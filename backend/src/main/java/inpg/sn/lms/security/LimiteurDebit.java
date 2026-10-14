package inpg.sn.lms.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalLong;

/**
 * Limiteur de débit à fenêtre glissante : au plus {@code maximum} requêtes par clé et par règle sur la durée donnée.
 *
 * <p>La mémoire est bornée : quand la table est pleine, la clé la plus anciennement utilisée est oubliée. Un attaquant qui
 * multiplie les adresses ne peut donc pas faire grossir le serveur indéfiniment ; il retrouve seulement, pour une adresse
 * oubliée, un compteur à zéro. Local à une instance du serveur (comme {@code LoginAttemptService}) : derrière plusieurs
 * instances, la limite réelle est multipliée par leur nombre.
 */
public class LimiteurDebit {

    private static final int CAPACITE = 50_000;

    private final Clock clock;
    private final Map<String, Deque<Instant>> passages = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Deque<Instant>> eldest) {
            return size() > CAPACITE;
        }
    };

    public LimiteurDebit() {
        this(Clock.systemUTC());
    }

    LimiteurDebit(Clock clock) {
        this.clock = clock;
    }

    /**
     * Enregistre un passage si la limite n'est pas atteinte.
     *
     * @return vide si la requête est autorisée ; sinon le nombre de secondes à attendre avant de pouvoir réessayer
     */
    public synchronized OptionalLong essayer(String regle, String cle, int maximum, Duration fenetre) {
        Instant maintenant = clock.instant();
        Deque<Instant> file = passages.computeIfAbsent(regle + '|' + cle, k -> new ArrayDeque<>());
        while (!file.isEmpty() && !maintenant.isBefore(file.peekFirst().plus(fenetre))) {
            file.removeFirst();
        }
        if (file.size() >= maximum) {
            long attente = Duration.between(maintenant, file.peekFirst().plus(fenetre)).toSeconds() + 1;
            return OptionalLong.of(Math.max(1, attente));
        }
        file.addLast(maintenant);
        return OptionalLong.empty();
    }
}
