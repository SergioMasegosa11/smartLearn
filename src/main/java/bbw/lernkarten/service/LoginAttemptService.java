package bbw.lernkarten.service;

import bbw.lernkarten.model.User;
import bbw.lernkarten.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * NEU (OWASP A07 - Identification and Authentication Failures):
 *
 * VORHER (Schwachstelle): UserService.login() hat Benutzername/Passwort
 * unbegrenzt oft pruefen lassen. Ein Angreifer konnte beliebig viele
 * Passwoerter automatisiert durchprobieren (Brute-Force) oder geleakte
 * Listen aus anderen Datenlecks durchtesten (Credential Stuffing) - ohne
 * jede Bremse oder Sperre.
 *
 * Wie demonstrieren: vor der Aenderung mit einem Skript/Postman 20x
 * hintereinander /api/users/login mit falschem Passwort aufrufen ->
 * jede Anfrage wird in gleicher Geschwindigkeit weiterverarbeitet,
 * keine Sperre, keine Verzoegerung, kein Log-Eintrag.
 *
 * NACHHER (Gegenmassnahme):
 * - Nach MAX_ATTEMPTS Fehlversuchen wird das Konto fuer LOCK_TIME_MS
 *   gesperrt (Account Lockout).
 * - Jeder Fehlversuch wird sicherheitsrelevant geloggt (OWASP A09).
 * - Zusaetzlich verlangt das Frontend nach mehreren Fehlversuchen ein
 *   einfaches CAPTCHA (siehe login.html / SecurityConfig).
 */
@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_TIME_MS = 2 * 60 * 1000L; // 2 Minuten

    private final UserRepository repo;

    public LoginAttemptService(UserRepository repo) {
        this.repo = repo;
    }

    public boolean isLocked(String username) {
        Optional<User> userOpt = repo.findByUsername(username);
        if (userOpt.isEmpty()) return false;
        User user = userOpt.get();
        return user.getLockedUntil() > System.currentTimeMillis();
    }

    public void loginFailed(String username) {
        Optional<User> userOpt = repo.findByUsername(username);
        if (userOpt.isEmpty()) {
            // Keine Benutzer-Enumeration: gleiche Verzoegerung/Loggingform
            // unabhaengig davon, ob der Benutzername existiert.
            log.warn("Fehlgeschlagener Login-Versuch fuer unbekannten Benutzer");
            return;
        }
        User user = userOpt.get();
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_ATTEMPTS) {
            user.setLockedUntil(System.currentTimeMillis() + LOCK_TIME_MS);
            log.warn("Konto '{}' nach {} Fehlversuchen gesperrt", maskUsername(username), attempts);
        } else {
            log.info("Fehlgeschlagener Login-Versuch {} von {} fuer Konto '{}'",
                    attempts, MAX_ATTEMPTS, maskUsername(username));
        }
        repo.update(user);
    }

    public void loginSucceeded(String username) {
        Optional<User> userOpt = repo.findByUsername(username);
        if (userOpt.isEmpty()) return;
        User user = userOpt.get();
        if (user.getFailedLoginAttempts() > 0 || user.getLockedUntil() > 0) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(0);
            repo.update(user);
        }
        log.info("Erfolgreicher Login fuer Konto '{}'", maskUsername(username));
    }

    // OWASP A09: niemals vollstaendige PII/Benutzernamen ungeschuetzt loggen.
    private String maskUsername(String username) {
        if (username == null || username.isBlank()) return "?";
        if (username.length() <= 2) return username.charAt(0) + "*";
        return username.charAt(0) + "***" + username.charAt(username.length() - 1);
    }
}
