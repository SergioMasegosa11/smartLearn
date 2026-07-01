package bbw.lernkarten.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * NEU (OWASP A01 / A07):
 * - "role" wurde ergaenzt -> Grundlage fuer rollenbasierte Autorisierung
 *   (ROLE_USER / ROLE_ADMIN), siehe SecurityConfig + @PreAuthorize.
 * - Die Klasse implementiert UserDetails, damit Spring Security das
 *   Objekt direkt fuer Authentifizierung/Autorisierung nutzen kann
 *   (statt einer eigenen, fehleranfaelligen Session-Logik).
 * - "passwordHash" enthaelt neu einen BCrypt-Hash (siehe SecurityConfig,
 *   PasswordEncoder) statt eines ungesalzenen SHA-256-Hashes.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class User implements UserDetails {

    private String username;
    private String passwordHash;
    private String role; // "ROLE_USER" oder "ROLE_ADMIN"

    // Konto-Sperre nach zu vielen Fehlversuchen (OWASP A07 - Brute-Force-Schutz)
    private int failedLoginAttempts = 0;
    private long lockedUntil = 0; // Timestamp in ms, 0 = nicht gesperrt

    public User() {}

    public User(String username, String passwordHash, String role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }

    public long getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(long lockedUntil) { this.lockedUntil = lockedUntil; }

    // ---- UserDetails-Schnittstelle (von Spring Security benoetigt) ----
    // @JsonIgnore auf allen Interface-Methoden: verhindert, dass Jackson
    // beim Schreiben von users.json zusaetzliche Felder wie "authorities",
    // "accountNonLocked" etc. (abgeleitet aus den UserDetails-Gettern)
    // in die JSON-Datei mit aufnimmt.

    @Override
    @com.fasterxml.jackson.annotation.JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role != null ? role : "ROLE_USER"));
    }

    @Override
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getPassword() { return passwordHash; }

    @Override
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isAccountNonLocked() {
        return lockedUntil == 0 || System.currentTimeMillis() > lockedUntil;
    }

    @Override
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isAccountNonExpired() { return true; }

    @Override
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isEnabled() { return true; }
}
