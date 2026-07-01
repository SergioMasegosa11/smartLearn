package bbw.lernkarten.service;

import bbw.lernkarten.model.User;
import bbw.lernkarten.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository repo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final LoginAttemptService loginAttemptService;

    public UserService(UserRepository repo,
                        PasswordEncoder passwordEncoder,
                        AuthenticationManager authenticationManager,
                        LoginAttemptService loginAttemptService) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.loginAttemptService = loginAttemptService;
    }

    /*
     * ================================================================
     * VORHER (Schwachstelle - OWASP A07):
     *
     *   public boolean register(String username, String password) {
     *       ...
     *       String hash = hash(password);              // SHA-256, KEIN Salt
     *       users.add(new User(username, hash));
     *       ...
     *   }
     *
     *   private String hash(String input) {
     *       MessageDigest md = MessageDigest.getInstance("SHA-256");
     *       return new String(md.digest(input.getBytes()));
     *   }
     *
     * Warum problematisch:
     * - SHA-256 ist ein schneller, generischer Hash-Algorithmus. Er ist
     *   NICHT fuer Passwoerter gedacht: GPUs koennen Milliarden SHA-256-
     *   Hashes pro Sekunde berechnen -> Offline-Brute-Force ist trivial.
     * - Es wird kein Salt verwendet. Identische Passwoerter erzeugen
     *   identische Hashes (siehe users.json: "smas" und "testt" haben
     *   denselben Hash -> beide nutzen offensichtlich dasselbe
     *   Passwort). Damit sind vorgerechnete Rainbow-Tables wirksam.
     * - new String(byte[]) interpretiert die rohen Hash-Bytes als Text
     *   in der Plattform-Kodierung -> kann sogar Informationen verlieren
     *   oder ungueltige Zeichen erzeugen (siehe kryptische Zeichen in
     *   users.json).
     *
     * Wie demonstrieren: zwei Benutzer mit demselben Passwort registrieren
     * und users.json vergleichen -> identische passwordHash-Werte.
     * ================================================================
     */

    public boolean register(String username, String password) {
        List<User> users = repo.getUsers();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return false;
        }

        if (users.stream().anyMatch(u -> u.getUsername().equals(username))) {
            log.info("Registrierung abgelehnt: Benutzername bereits vergeben");
            return false;
        }

        // NACHHER: BCrypt statt SHA-256 - automatischer, individueller
        // Salt pro Passwort + bewusst hoher Rechenaufwand.
        String hash = passwordEncoder.encode(password);

        // Least Privilege: neue Konten erhalten standardmaessig die
        // niedrigste Berechtigungsstufe (ROLE_USER), nie automatisch Admin.
        users.add(new User(username, hash, "ROLE_USER"));
        repo.saveUsers(users);

        log.info("Neuer Benutzer registriert");
        return true;
    }

    /*
     * ================================================================
     * VORHER (Schwachstelle - OWASP A07):
     *
     *   public boolean login(String username, String password) {
     *       List<User> users = repo.getUsers();
     *       String hash = hash(password);
     *       return users.stream()
     *               .anyMatch(u -> u.getUsername().equals(username) &&
     *                              u.getPasswordHash().equals(hash));
     *   }
     *
     * Warum problematisch:
     * - Keine Begrenzung der Versuche -> Brute-Force & Credential
     *   Stuffing moeglich (siehe LoginAttemptService).
     * - Kein Logging eines fehlgeschlagenen oder erfolgreichen Logins
     *   (siehe OWASP A09) -> Angriffe bleiben unbemerkt.
     * - String.equals() fuer Hash-Vergleich ist nicht zeitkonstant,
     *   wodurch theoretisch Timing-Angriffe moeglich sind. BCrypt's
     *   matches()-Methode ist dagegen darauf ausgelegt.
     *
     * NACHHER: Authentifizierung wird komplett an Spring Security
     * delegiert (AuthenticationManager -> DaoAuthenticationProvider ->
     * CustomUserDetailsService + BCryptPasswordEncoder). Zusaetzlich:
     * Account-Lockout nach mehreren Fehlversuchen + Security-Logging.
     * ================================================================
     */
    public boolean login(String username, String password) {

        if (loginAttemptService.isLocked(username)) {
            log.warn("Login verweigert: Konto ist temporaer gesperrt");
            return false;
        }

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            loginAttemptService.loginSucceeded(username);
            return true;

        } catch (LockedException e) {
            log.warn("Login verweigert: Konto wurde waehrend der Pruefung gesperrt");
            return false;
        } catch (BadCredentialsException e) {
            loginAttemptService.loginFailed(username);
            return false;
        }
    }
}
