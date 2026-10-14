package inpg.sn.lms.security;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.OptionalLong;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Limite le débit des trois routes ouvertes à tous qui n'étaient protégées par rien (audit du 2026-10-14) : l'inscription
 * (création de comptes en masse), la question publique (chaque envoi écrit à l'INPG) et la vérification de certificat (balayage
 * des références, 8 caractères hexadécimaux). La connexion et le code de réinitialisation ont déjà leur propre limite.
 *
 * <p>L'adresse retenue est la <strong>dernière</strong> de {@code X-Forwarded-For} : le proxy (nginx, ou celui de l'hébergeur) y
 * ajoute celle qu'il voit réellement, alors que les précédentes sont fournies par le client et se falsifient. Un serveur joint
 * directement, sans proxy, laisse le client écrire tout l'en-tête : la limite n'y est alors qu'indicative.
 */
@Component
public class LimiteDebitFilter extends OncePerRequestFilter {

    /** Une règle : une méthode, un préfixe de chemin, un nombre de requêtes par fenêtre. */
    record Regle(String nom, String methode, String prefixe, int maximum, Duration fenetre) {
        boolean concerne(HttpServletRequest requete) {
            return methode.equals(requete.getMethod()) && requete.getRequestURI().startsWith(prefixe);
        }
    }

    private final LimiteurDebit limiteur = new LimiteurDebit();
    private final boolean actif;
    private final List<Regle> regles;

    public LimiteDebitFilter(
            @Value("${app.rate-limit.enabled:true}") boolean actif,
            @Value("${app.rate-limit.inscription:10}") int inscription,
            @Value("${app.rate-limit.question:5}") int question,
            @Value("${app.rate-limit.verification-certificat:30}") int verificationCertificat) {
        this.actif = actif;
        this.regles = List.of(
                new Regle("inscription", "POST", "/api/auth/register", inscription, Duration.ofHours(1)),
                new Regle("question", "POST", "/api/recrutement/public/question", question, Duration.ofMinutes(15)),
                new Regle("verification-certificat", "GET", "/api/certificats/public/verifier/", verificationCertificat,
                        Duration.ofMinutes(10)));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest requete) {
        return !actif || regles.stream().noneMatch(r -> r.concerne(requete));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requete, HttpServletResponse reponse, FilterChain chaine)
            throws ServletException, IOException {
        String adresse = adresseDuClient(requete);
        for (Regle regle : regles) {
            if (!regle.concerne(requete)) {
                continue;
            }
            OptionalLong attente = limiteur.essayer(regle.nom(), adresse, regle.maximum(), regle.fenetre());
            if (attente.isPresent()) {
                reponse.setStatus(429);
                reponse.setHeader("Retry-After", String.valueOf(attente.getAsLong()));
                reponse.setContentType("application/json;charset=UTF-8");
                reponse.getWriter().write("{\"message\":\"Trop de demandes depuis votre connexion. Réessayez dans "
                        + formater(attente.getAsLong()) + ".\"}");
                return;
            }
        }
        chaine.doFilter(requete, reponse);
    }

    static String adresseDuClient(HttpServletRequest requete) {
        String transmis = requete.getHeader("X-Forwarded-For");
        if (transmis != null && !transmis.isBlank()) {
            String[] adresses = transmis.split(",");
            return adresses[adresses.length - 1].trim();
        }
        return requete.getRemoteAddr();
    }

    private static String formater(long secondes) {
        if (secondes < 90) {
            return secondes + " secondes";
        }
        long minutes = (secondes + 59) / 60;
        return minutes < 90 ? minutes + " minutes" : ((minutes + 59) / 60) + " heures";
    }
}
