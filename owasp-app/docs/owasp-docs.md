OWASP risks demonstrated (short)

1) Broken Authentication / Credential Stuffing
- Demonstration: login attempts tracked; repeated failed attempts lock account.
- Mitigation: LoginAttemptService enforces lockout; passwords stored hashed via BCrypt.

2) Cross-Site Scripting (XSS)
- Demonstration: user-supplied card content is rendered escaped by Thymeleaf, preventing script execution.
- Mitigation: input validation and relying on template escaping.

3) SQL Injection
- Demonstration: repository uses JPA with parameter binding, preventing injection.
- Mitigation: no raw concatenated SQL; use JPA/Hibernate.

Additional:
- CSRF: Spring Security CSRF protection is enabled for state-changing requests.
- Logging: SLF4J used; avoid logging passwords or tokens.
- Architecture: Spring Security Filter Chain used; least privilege applied via roles.

Documentation for each risk with steps, screenshots and test commands should be added for submission.