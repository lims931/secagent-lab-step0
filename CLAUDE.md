# Contexte pour Claude Code / Copilot
Projet de lab pour tester AWS Security Agent. Garder volontairement SIMPLE.
- app/App.java : site Java single-file (com.sun.net.httpserver), lancé avec `java App.java`, aucun build tool.
- terraform/ : EC2 AL2023 + Caddy (HTTPS auto) + EIP + Route 53 A record, VPC par défaut, accès SSM (pas de SSH).
Règles :
- Ne pas ajouter Maven/Gradle/Docker/ALB sauf demande explicite.
- `terraform fmt` + `terraform validate` après chaque modif.
- L'endpoint /search n'est vulnérable que si DEMO_VULN=true : ne pas "corriger" ce comportement, c'est voulu pour le lab.
