package bbw.lernkarten.service;

import bbw.lernkarten.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * NEU (OWASP A07 - Identification and Authentication Failures):
 * Bindet unser eigenes JSON-basiertes UserRepository in Spring Security
 * ein. Spring Security ruft loadUserByUsername() beim Login automatisch
 * auf und vergleicht das zurueckgegebene Passwort (BCrypt-Hash) selbst
 * -- wir muessen den Vergleich nicht mehr manuell programmieren.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository repo;

    public CustomUserDetailsService(UserRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return repo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Benutzer nicht gefunden: " + username));
    }
}
