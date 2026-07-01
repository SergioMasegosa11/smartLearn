package bbw.lernkarten.controller;

import bbw.lernkarten.service.CaptchaService;
import bbw.lernkarten.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    // NEU (OWASP A09): dedizierter Logger statt System.out/e.printStackTrace().
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService service;
    private final CaptchaService captchaService;
    private final CsrfTokenRepository csrfTokenRepository;

    public UserController(UserService service,
                          CaptchaService captchaService,
                          CsrfTokenRepository csrfTokenRepository) {
        this.service = service;
        this.captchaService = captchaService;
        this.csrfTokenRepository = csrfTokenRepository;
    }

    /*
     * NEU (OWASP A07 / Aufgabenstellung "CAPTCHA einbinden"):
     * Liefert eine einfache Rechenaufgabe, die das Frontend (login.html)
     * vor dem Absenden von Login/Register anzeigen muss. Schuetzt vor
     * vollautomatisierten Bot-Anfragen (Credential Stuffing, Brute-Force-
     * Skripte), da ein Skript ohne Bildschirm die Aufgabe nicht "sieht".
     */
    @GetMapping("/captcha")
    public CaptchaService.Captcha captcha() {
        return captchaService.generate();
    }

    @PostMapping("/register")
    public String register(@RequestBody Map<String, String> body) {
        // NEU: serverseitige Validierung statt blindem Vertrauen ins Frontend.
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || username.isBlank() || password == null || password.length() < 8) {
            return "INVALID";
        }
        if (!checkCaptcha(body)) {
            return "CAPTCHA_FAIL";
        }
        boolean ok = service.register(username, password);
        return ok ? "OK" : "EXISTS";
    }

    /*
     * VORHER (OWASP A09 - Security Logging and Monitoring Failures):
     * Es gab UEBERHAUPT kein Logging beim Login. Weder erfolgreiche noch
     * fehlgeschlagene Versuche wurden irgendwo erfasst. Im Ernstfall
     * (z.B. Brute-Force-Angriff oder kompromittiertes Konto) gibt es
     * keine Spur, anhand der man das erkennen oder nachvollziehen koennte.
     *
     * NACHHER: UserService/LoginAttemptService loggen jeden Versuch
     * (Erfolg/Fehlschlag/Sperre) ueber SLF4J. WICHTIG: Es wird niemals
     * das Passwort selbst geloggt, und der Benutzername wird maskiert
     * (siehe LoginAttemptService.maskUsername), um keine vollstaendigen
     * PII in Klartext in Logdateien zu schreiben.
     */
    @PostMapping("/login")
    public String login(@RequestBody Map<String, String> body, HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        String username = body.get("username");
        String password = body.get("password");

        if (!checkCaptcha(body)) {
            return "CAPTCHA_FAIL";
        }

        boolean ok = service.login(username, password);

        if(ok){

            request.changeSessionId();
            session.setAttribute("user", username);

            var csrfToken = csrfTokenRepository.generateToken(request);
            csrfTokenRepository.saveToken(csrfToken, request, response);

            log.info("Login erfolgreich: {}", username);
        }

        return ok ? "OK" : "FAIL";
    }

    private boolean checkCaptcha(Map<String, String> body) {
        String captchaId = body.get("captchaId");
        String captchaAnswerRaw = body.get("captchaAnswer");
        try {
            return captchaService.verify(captchaId, Integer.parseInt(captchaAnswerRaw));
        } catch (Exception e) {
            return false;
        }
    }

    @GetMapping("/current")
    public String current(HttpSession session) {
        Object user = session.getAttribute("user");
        if (user != null) {
            return user.toString();
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
            return auth.getName();
        }

        return "";
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(HttpServletRequest request, HttpServletResponse response) {
        CsrfToken token = csrfTokenRepository.generateToken(request);
        csrfTokenRepository.saveToken(token, request, response);
        return Map.of("token", token.getToken());
    }

    // NEU: aktuelle Rolle fuers Frontend (z.B. um Admin-Menues ein-/auszublenden)
    @GetMapping("/role")
    public String role() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return "";
        return auth.getAuthorities().toString();
    }

    @PostMapping("/logout")
    public void logout(HttpSession session) {
        log.info("Benutzer hat sich abgemeldet");
        session.invalidate();
        SecurityContextHolder.clearContext();
    }
}
