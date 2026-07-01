package bbw.lernkarten.service;

import bbw.lernkarten.model.Flashcard;
import bbw.lernkarten.repository.FlashcardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FlashcardService
{
    private static final Logger log = LoggerFactory.getLogger(FlashcardService.class);

    private final FlashcardRepository repository;

    public FlashcardService(FlashcardRepository repository){
        this.repository = repository;
    }

    public List<Flashcard> getAll(){
        return repository.getCards();
    }

    public void add(Flashcard card, String username){

        if(card.getQuestion().isEmpty() || card.getAnswer().isEmpty() || card.getCategory().isEmpty()){
            return;
        }

        List<Flashcard> cards = repository.getCards();

        int newId = cards.stream()
                .mapToInt(Flashcard::getId)
                .max()
                .orElse(0) + 1;

        card.setId(newId);
        card.setOwner(username);
        cards.add(card);
        repository.saveCards(cards);
        log.info("Neue Lernkarte (id={}) angelegt", newId);
    }

    public void delete(int id, String username)
    {
        List<Flashcard> cards = repository.getCards();
        boolean existed = cards.stream().anyMatch(c -> c.getId() == id);
        boolean removed = cards.removeIf(card -> card.getId() == id && card.getOwner().equals(username));
        repository.saveCards(cards);

        if (existed && !removed) {
            // Karte existiert, gehoert aber jemand anderem -> Zugriffsversuch
            log.warn("Zugriff verweigert: Versuch, fremde Karte (id={}) zu loeschen", id);
        } else if (removed) {
            log.info("Lernkarte (id={}) geloescht", id);
        }
    }

    /*
     * ================================================================
     * VORHER (Schwachstelle - OWASP A01: Broken Access Control):
     *
     *   public void update(int id, Flashcard updated){
     *       List<Flashcard> cards = repository.getCards();
     *       for(Flashcard card : cards){
     *           if(card.getId()==id) {
     *               card.setQuestion(updated.getQuestion());
     *               card.setAnswer(updated.getAnswer());
     *               card.setCategory(updated.getCategory());
     *           }
     *       }
     *       repository.saveCards(cards);
     *   }
     *
     * Warum problematisch:
     * - Es wird NIRGENDS geprueft, ob die anfragende Person der "owner"
     *   der Karte ist. Im Unterschied zu add()/delete() fehlte hier der
     *   Owner-Check komplett (IDOR - Insecure Direct Object Reference).
     * - Jeder eingeloggte Benutzer A konnte per
     *   PUT /api/cards/{id-von-Benutzer-B}
     *   beliebige Lernkarten anderer Benutzer veraendern, einfach indem
     *   er die ID erraet/durchzaehlt (IDs sind einfache Integer 1,2,3...).
     *
     * Wie demonstrieren: Als Benutzer A einloggen, dann mit Postman/curl
     * PUT /api/cards/{id} senden, wobei {id} einer Karte von Benutzer B
     * gehoert (z.B. id=2, Owner "smas", waehrend man selbst "testt" ist).
     * Vor der Korrektur: Karte wird trotzdem veraendert (200 OK).
     *
     * NACHHER: Owner-Check analog zu delete(); Versuch wird zusaetzlich
     * sicherheitsrelevant geloggt (OWASP A09).
     * ================================================================
     */
    public boolean update(int id, Flashcard updated, String username) {
        List<Flashcard> cards = repository.getCards();
        boolean changed = false;

        for (Flashcard card : cards) {
            if (card.getId() == id) {
                if (!card.getOwner().equals(username)) {
                    log.warn("Zugriff verweigert: Versuch, fremde Karte (id={}) zu bearbeiten", id);
                    return false; // Broken Access Control behoben
                }
                card.setQuestion(updated.getQuestion());
                card.setAnswer(updated.getAnswer());
                card.setCategory(updated.getCategory());
                changed = true;
            }
        }
        repository.saveCards(cards);
        if (changed) {
            log.info("Lernkarte (id={}) aktualisiert", id);
        }
        return changed;
    }
}
