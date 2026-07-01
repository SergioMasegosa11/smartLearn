# SmartLearn – OWASP Security Umsetzung

Lernkarten-Webapplikation (Spring Boot) mit nachgeruesteter Spring Security
Absicherung im Rahmen von Modul 183.

## Installation & Start

Voraussetzungen: Java 17+, Maven.

```bash
mvn spring-boot:run
```

Anwendung erreichbar unter: http://localhost:8080/login.html

### Testbenutzer

| Benutzername | Passwort       | Rolle       |
|--------------|----------------|-------------|
| smas         | password123    | ROLE_USER   |
| testt        | password123    | ROLE_USER   |
| admin        | adminpass123   | ROLE_ADMIN  |

> Passwoerter sind nun mit BCrypt gehasht (siehe `users.json`). Die
> Klartext-Passwoerter oben dienen nur zum Testen in dieser Abgabe.

## Umgesetzte OWASP-Risiken

### 1) A07:2021 – Identification and Authentication Failures
- **Vorher:** Passwoerter wurden mit ungesalzenem SHA-256 gehasht
  (`UserService.hash()`), Login ohne Versuchsbegrenzung, kein CAPTCHA,
  kein Logging.
- **Nachher:**
  - BCrypt-Hashing (`SecurityConfig.passwordEncoder()`), automatischer
    Salt pro Passwort.
  - Account-Lockout nach 5 Fehlversuchen, 2 Min. Sperre
    (`LoginAttemptService`).
  - Einfaches Rechen-CAPTCHA bei Login/Registrierung
    (`CaptchaService`, `login.html`).
  - Mindestpasswortlaenge (8 Zeichen) bei Registrierung.
  - Session-ID-Wechsel nach Login (Schutz vor Session Fixation).

### 2) A01:2021 – Broken Access Control
- **Vorher:** `FlashcardService.update()` pruefte den Besitzer der
  Lernkarte NICHT -> jeder eingeloggte Benutzer konnte fremde Karten
  per `PUT /api/cards/{id}` veraendern (IDOR).
- **Nachher:**
  - Owner-Check in `update()` analog zu `delete()`, Antwort `403
    Forbidden` bei fremden Karten.
  - Rollenbasierte Autorisierung (`ROLE_USER` / `ROLE_ADMIN`) in
    `SecurityConfig`, Admin-Bereich (`/api/admin/**`) nur fuer
    `ROLE_ADMIN`.
  - Least Privilege: neue Konten erhalten standardmaessig nur
    `ROLE_USER`.

### 3) A09:2021 – Security Logging and Monitoring Failures
- **Vorher:** Kein Logging-Framework genutzt, lediglich
  `e.printStackTrace()` in den Repositories. Keine Security-Events
  (Logins, Zugriffsverweigerungen) wurden erfasst.
- **Nachher:**
  - SLF4J/Logback durchgaengig eingesetzt (`logback-spring.xml`),
    Konsole + rollierende Logdatei (`logs/smartlearn.log`).
  - Sinnvolle Log-Level: `INFO` (z.B. erfolgreicher Login, neue Karte),
    `WARN` (Fehlversuche, Konto-Sperre, verweigerter Fremdzugriff),
    `ERROR` (technische Fehler).
  - **Keine sensiblen Daten im Log:** Passwoerter werden nie geloggt,
    Benutzernamen werden maskiert (`LoginAttemptService.maskUsername`,
    z.B. `s***s`).

## Security-Architektur (Spring Security Filter Chain)

Siehe ausfuehrliche Kommentare in `SecurityConfig.java`. Kurzfassung:

1. CORS-Filter (eng eingeschraenkt auf `http://localhost:8080`)
2. CSRF-Filter (aktiv, Cookie-basiertes Token)
3. Authentication-Filter (Login via `AuthenticationManager` ->
   `DaoAuthenticationProvider` -> `CustomUserDetailsService` +
   `BCryptPasswordEncoder`)
4. Authorization-Filter (rollenbasiert, Least Privilege:
   `anyRequest().authenticated()` als Default-Deny)
5. Exception-Translation (401/403 statt Redirects, passend fuer eine
   REST-API)

## Hinweis zur Demonstration der Schwachstellen

Im Code sind alle drei behobenen Schwachstellen mit Kommentarbloecken
`VORHER (Schwachstelle - OWASP ...)` markiert, die den urspruenglichen,
unsicheren Code sowie eine Anleitung zur Reproduktion enthalten (siehe
`UserService.java`, `FlashcardService.java`, `LoginAttemptService.java`).
