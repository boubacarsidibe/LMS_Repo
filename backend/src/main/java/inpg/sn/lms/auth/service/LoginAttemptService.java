package inpg.sn.lms.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Limite locale par adresse, y compris pour les comptes inexistants. */
@Service
public class LoginAttemptService {
    private static final int MAX_TENTATIVES = 10;
    private static final int CAPACITE = 10000;
    private static final Duration FENETRE = Duration.ofMinutes(15);
    private final Clock clock;
    private final Map<String, Tentatives> tentatives = new HashMap<>();
    private record Tentatives(Instant expiration, int nombre) { }

    @Autowired
    public LoginAttemptService() { this(Clock.systemUTC()); }

    LoginAttemptService(Clock clock) { this.clock = clock; }

    public synchronized boolean autoriser(String email) {
        Instant maintenant = clock.instant();
        tentatives.entrySet().removeIf(entry -> !maintenant.isBefore(entry.getValue().expiration()));
        String cle = email.trim().toLowerCase(Locale.ROOT);
        Tentatives precedent = tentatives.get(cle);
        if (precedent == null) {
            if (tentatives.size() >= CAPACITE) return false;
            tentatives.put(cle, new Tentatives(maintenant.plus(FENETRE), 1));
            return true;
        }
        if (precedent.nombre() >= MAX_TENTATIVES) return false;
        tentatives.put(cle, new Tentatives(precedent.expiration(), precedent.nombre() + 1));
        return true;
    }
}
