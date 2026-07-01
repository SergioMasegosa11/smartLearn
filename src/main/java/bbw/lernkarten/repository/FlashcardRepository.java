package bbw.lernkarten.repository;
import bbw.lernkarten.model.Flashcard;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import java.io.File;
import java.util.ArrayList;
import java.util.List;


@Repository
public class FlashcardRepository 
{
    private static final Logger log = LoggerFactory.getLogger(FlashcardRepository.class);

    private final String FILE = "src/main/resources/cards.json";
    private final ObjectMapper mapper = new ObjectMapper();

    public List<Flashcard> getCards(){
        try
        {
            File file = new File(FILE);

            if(!file.exists()){
                return new ArrayList<>();
            }
            return mapper.readValue(file, new TypeReference<List<Flashcard>>() {});
        }catch(Exception e)
        {
            log.error("Fehler beim Lesen von cards.json", e);
            return new ArrayList<>();
        }
    }

    public void saveCards(List<Flashcard> cards)
    {
        try
        {
            mapper.writerWithDefaultPrettyPrinter()
                .writeValue(
                        new File(FILE),
                        cards
                );

        }catch(Exception e)
        {
            log.error("Fehler beim Schreiben von cards.json", e);
        }
    }
}