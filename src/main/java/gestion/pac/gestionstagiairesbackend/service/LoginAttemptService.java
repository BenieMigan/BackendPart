package gestion.pac.gestionstagiairesbackend.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {
    private final Map<String, AttemptInfo> attemptStore = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 3;
    private static final long ATTEMPT_RESET_TIME = 30 * 60 * 1000; // 30 minutes

    public void recordFailedAttempt(String email) {
        AttemptInfo info = attemptStore.computeIfAbsent(email, k -> new AttemptInfo());
        info.incrementAttempts();
        info.setLastAttemptTime(System.currentTimeMillis());
    }

    public void resetAttempts(String email) {
        attemptStore.remove(email);
    }

    public boolean requiresCaptcha(String email) {
        AttemptInfo info = attemptStore.get(email);
        if (info == null) {
            return false;
        }

        // Réinitialiser si trop de temps a passé
        if (System.currentTimeMillis() - info.getLastAttemptTime() > ATTEMPT_RESET_TIME) {
            attemptStore.remove(email);
            return false;
        }

        return info.getAttempts() >= MAX_ATTEMPTS;
    }

    private static class AttemptInfo {
        private int attempts;
        private long lastAttemptTime;

        public void incrementAttempts() { this.attempts++; }
        public int getAttempts() { return attempts; }
        public long getLastAttemptTime() { return lastAttemptTime; }
        public void setLastAttemptTime(long time) { this.lastAttemptTime = time; }
    }
}