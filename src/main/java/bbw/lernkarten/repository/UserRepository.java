package bbw.lernkarten.repository;

import bbw.lernkarten.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private static final Logger log = LoggerFactory.getLogger(UserRepository.class);

    private final String FILE = "src/main/resources/users.json";
    private final ObjectMapper mapper = new ObjectMapper();

    public List<User> getUsers() {
        try {
            File file = new File(FILE);
            if (!file.exists()) return new ArrayList<>();
            return mapper.readValue(file, new TypeReference<List<User>>() {});
        } catch (Exception e) {
            log.error("Fehler beim Lesen von users.json", e);
            return new ArrayList<>();
        }
    }

    // NEU: wird von CustomUserDetailsService (Spring Security) benoetigt,
    // um einen Benutzer beim Login nachzuschlagen.
    public Optional<User> findByUsername(String username) {
        return getUsers().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst();
    }

    public void saveUsers(List<User> users) {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE), users);
        } catch (Exception e) {
            log.error("Fehler beim Schreiben von users.json", e);
        }
    }

    // NEU: aktualisiert einen einzelnen Benutzer (z.B. failedLoginAttempts,
    // lockedUntil) ohne die ganze Liste manuell durchzugehen.
    public void update(User updated) {
        List<User> users = getUsers();
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getUsername().equals(updated.getUsername())) {
                users.set(i, updated);
                break;
            }
        }
        saveUsers(users);
    }
}
