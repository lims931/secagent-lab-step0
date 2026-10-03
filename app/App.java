import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Mini site web Java (zero dependance, zero build) pour lab AWS Security Agent.
 * Lancement : java App.java   (Java 11+ "source launcher")
 * Ecoute sur 127.0.0.1:8080 ; Caddy fait le HTTPS devant.
 * DEMO_VULN=true active volontairement une XSS reflechie sur /search (lab uniquement).
 */
public class App {

    static final boolean DEMO_VULN = "true".equalsIgnoreCase(System.getenv("DEMO_VULN"));

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        String bind = System.getenv().getOrDefault("BIND", "127.0.0.1");
        HttpServer server = HttpServer.create(new InetSocketAddress(bind, port), 0);

        server.createContext("/", ex -> {
            if (!ex.getRequestURI().getPath().equals("/")) { send(ex, 404, "text/plain", "Not found"); return; }
            send(ex, 200, "text/html", page("Accueil",
                "<p>Mini site Java deploye sur AWS avec Terraform.</p>"
              + "<ul><li><a href=\"/hello?name=Formation\">/hello?name=...</a></li>"
              + "<li><a href=\"/search?q=test\">/search?q=...</a></li>"
              + "<li><a href=\"/health\">/health</a></li></ul>"));
        });

        server.createContext("/hello", ex -> {
            String name = query(ex).getOrDefault("name", "inconnu");
            send(ex, 200, "text/html", page("Hello", "<p>Bonjour " + escape(name) + " !</p>"));
        });

        server.createContext("/search", ex -> {
            String q = query(ex).getOrDefault("q", "");
            // Volontaire si DEMO_VULN=true : sortie non echappee => XSS reflechie
            String shown = DEMO_VULN ? q : escape(q);
            send(ex, 200, "text/html", page("Recherche",
                "<form action=\"/search\"><input name=\"q\"><button>OK</button></form>"
              + "<p>Resultats pour : " + shown + "</p><p>Aucun resultat.</p>"));
        });

        server.createContext("/health", ex ->
            send(ex, 200, "application/json", "{\"status\":\"UP\",\"time\":\"" + Instant.now() + "\"}"));

        server.start();
        System.out.println("App demarree sur " + bind + ":" + port + " (DEMO_VULN=" + DEMO_VULN + ")");
    }

    static Map<String, String> query(HttpExchange ex) {
        Map<String, String> m = new HashMap<>();
        String raw = ex.getRequestURI().getRawQuery();
        if (raw == null) return m;
        for (String kv : raw.split("&")) {
            String[] p = kv.split("=", 2);
            m.put(URLDecoder.decode(p[0], StandardCharsets.UTF_8),
                  p.length > 1 ? URLDecoder.decode(p[1], StandardCharsets.UTF_8) : "");
        }
        return m;
    }

    static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#x27;");
    }

    static String page(String title, String body) {
        return "<!doctype html><html lang=\"fr\"><head><meta charset=\"utf-8\"><title>" + title
             + "</title></head><body style=\"font-family:sans-serif;max-width:640px;margin:40px auto\">"
             + "<h1>" + title + "</h1>" + body + "</body></html>";
    }

    static void send(HttpExchange ex, int code, String type, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }
}
