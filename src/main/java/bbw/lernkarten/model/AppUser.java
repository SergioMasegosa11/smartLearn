package bbw.lernkarten.model;

import java.util.HashSet;
import java.util.Set;

@Entity
public class AppUser {

    @Id @GeneratedValue
    private Long id;

    private String username;
    private String password;

    private int failedLoginAttempts;
    private boolean accountLocked;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> roles = new HashSet<>();
}
