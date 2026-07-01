package bbw.lernkarten.controller;

import bbw.lernkarten.model.Flashcard;
import bbw.lernkarten.service.FlashcardService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
public class FlashcardController {

    private static final Logger log = LoggerFactory.getLogger(FlashcardController.class);

    private final FlashcardService service;

    public FlashcardController(FlashcardService service)
    {
        this.service = service;
    }

    /*
     * NEU: Authentication statt HttpSession-Attribut.
     * Da SecurityConfig anyRequest().authenticated() durchsetzt, ist an
     * dieser Stelle immer ein gueltig authentifizierter Benutzer
     * vorhanden -> kein NullPointerException-Risiko mehr wie vorher bei
     * "user == null" (z.B. wenn man die Karten-API ohne vorherigen
     * Login direkt aufgerufen hat).
     */
    @GetMapping
    public List<Flashcard> getCards(Authentication auth)
    {
        String user = auth.getName();
        return service.getAll().stream().filter(c -> c.getOwner().equals(user)).toList();
    }

    @PostMapping
    public Flashcard addCard(@RequestBody Flashcard card, Authentication auth)
    {
        String user = auth.getName();
        service.add(card, user);
        return card;
    }

    /*
     * VORHER (OWASP A01 - Broken Access Control):
     *   public void update(@PathVariable int id, @RequestBody Flashcard card) {
     *       service.update(id, card);
     *   }
     * -> kein Bezug zum eingeloggten Benutzer, jeder konnte jede Karte
     *    bearbeiten (siehe Erklaerung in FlashcardService.update()).
     *
     * NACHHER: Owner wird mitgegeben und im Service durchgesetzt;
     * bei verweigertem Zugriff liefern wir 403 Forbidden statt
     * stillschweigend nichts zu tun bzw. 200 OK vorzugaukeln.
     */
    @PutMapping("/{id}")
    public ResponseEntity<String> update(@PathVariable int id, @RequestBody Flashcard card, Authentication auth)
    {
        String user = auth.getName();
        boolean ok = service.update(id, card, user);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Kein Zugriff auf diese Karte");
        }
        return ResponseEntity.ok("OK");
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable int id, Authentication auth)
    {
        String user = auth.getName();
        try
        {
            service.delete(id, user);
            return "OK";
        }
        catch(Exception e) {
            log.error("Fehler beim Loeschen der Karte (id={})", id, e);
            return "FEHLER";
        }
    }


    @GetMapping("/categories")
    public List<String> getCategories() {
        return List.of("Mathe", "Deutsch", "Englisch");
    }

}
