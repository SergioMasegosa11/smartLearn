package bbw.lernkarten.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NEU (OWASP A07 - Identification and Authentication Failures /
 * Aufgabenstellung: "CAPTCHA einbinden zum Schutz vor automatisierten
 * Angriffen").
 *
 * Einfaches, selbst gehostetes Rechen-CAPTCHA (kein externer Dienst wie
 * Google reCAPTCHA notwendig -> funktioniert offline und ohne API-Key,
 * ideal fuer dieses Schulprojekt). Es demonstriert das Prinzip:
 *
 * 1. GET  /api/users/captcha       -> Server erzeugt eine einfache
 *    Rechenaufgabe (z.B. "4 + 7"), merkt sich die Loesung serverseitig
 *    (gebunden an eine zufaellige captchaId) und schickt NUR die Frage
 *    + captchaId an den Client.
 * 2. Beim Login/Register muss der Client captchaId + die Loesung
 *    mitschicken. Der Server prueft sie serverseitig.
 *
 * Wichtig: Die Loesung wird NIE an den Client gesendet und nur einmal
 * verwendet (wird nach Pruefung sofort entfernt) -> verhindert simples
 * Skripten/Wiederverwenden durch einen Bot.
 */
@Service
public class CaptchaService {

    private final Map<String, Integer> activeCaptchas = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public record Captcha(String id, String question) {}

    public Captcha generate() {
        int a = random.nextInt(10) + 1;
        int b = random.nextInt(10) + 1;
        String id = UUID.randomUUID().toString();
        activeCaptchas.put(id, a + b);
        return new Captcha(id, a + " + " + b + " = ?");
    }

    public boolean verify(String id, Integer answer) {
        if (id == null || answer == null) return false;
        Integer solution = activeCaptchas.remove(id); // Einmal-Verwendung
        return solution != null && solution.equals(answer);
    }
}
