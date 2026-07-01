OWASP Demo App

Run:
1. mvn -f owasp-app\pom.xml spring-boot:run
2. Open http://localhost:8080

Implemented security measures:
- Form login with CAPTCHA
- Credential-stuffing protection (lockout after failed attempts)
- CSRF enabled (Spring default)
- Roles (ROLE_USER, ROLE_ADMIN)
- SLF4J + Logback logging with sensitive data excluded
- Thymeleaf escaping to mitigate XSS

See docs/owasp-docs.md for risk descriptions and how they were demonstrated.