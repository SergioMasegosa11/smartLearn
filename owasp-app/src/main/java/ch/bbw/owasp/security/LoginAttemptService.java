package ch.bbw.owasp.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {
    private final Map<String, Integer> attempts = new ConcurrentHashMap<>();
    private final Map<String, Instant> lockUntil = new ConcurrentHashMap<>();
    private final int MAX_ATTEMPTS = 5;
    private final long LOCK_MILLIS = 15 * 60 * 1000; // 15 minutes

    public void loginSucceeded(String key) {
        attempts.remove(key);
        lockUntil.remove(key);
    }

    public void loginFailed(String key) {
        int v = attempts.getOrDefault(key, 0) + 1;
        attempts.put(key, v);
        if (v >= MAX_ATTEMPTS) {
            lockUntil.put(key, Instant.now().plusMillis(LOCK_MILLIS));
        }
    }

    public boolean isBlocked(String key) {
        Instant until = lockUntil.get(key);
        if (until == null) return false;
        if (Instant.now().isAfter(until)) {
            lockUntil.remove(key);
            attempts.remove(key);
            return false;
        }
        return true;
    }
}
