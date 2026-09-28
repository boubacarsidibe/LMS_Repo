package inpg.sn.lms.accreditation.service;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import inpg.sn.lms.accreditation.entity.DemandeAccreditation;

/**
 * Signature électronique de l'attestation d'accréditation : même principe que
 * {@code CertificatSignatureService} (HMAC-SHA256 du contenu figé de la
 * décision), et même clé — un seul secret serveur pour deux domaines de
 * documents scellés, pas de raison de les séparer.
 */
@Service
public class AttestationAccreditationSignatureService {

    private static final String ALGORITHME = "HmacSHA256";

    /**
     * {@code LocalDateTime.toString()} varie selon que les secondes/nanosecondes
     * sont nulles ou non (« 2026-09-27T10:00 » vs « 2026-09-27T10:00:00.123 »).
     * La valeur signée juste après {@code LocalDateTime.now()} n'a donc pas
     * forcément la même représentation que la même donnée relue après un
     * aller-retour en base (arrondi de précision côté colonne/pilote JDBC) — un
     * faux positif d'altération détecté en vérifiant réellement contre la base,
     * pas seulement avec un objet gardé en mémoire par un test unitaire. Un
     * format explicite à la minute élimine cette variation ; la minute suffit
     * amplement pour une date de décision.
     */
    private static final DateTimeFormatter DATE_DECISION_CANONIQUE = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final String cle;

    public AttestationAccreditationSignatureService(@Value("${certificat.signature.secret}") String cle) {
        this.cle = cle;
    }

    public String signer(DemandeAccreditation demande) {
        return hmacHex(contenuCanonique(demande));
    }

    public boolean estAlteree(DemandeAccreditation demande) {
        return demande.getSignatureAttestation() != null
                && !demande.getSignatureAttestation().equals(signer(demande));
    }

    private String contenuCanonique(DemandeAccreditation demande) {
        return String.join("|",
                nullToVide(demande.getReferenceAttestation()),
                demande.getEcole() == null ? "" : nullToVide(demande.getEcole().getNomEcole()),
                nullToVide(demande.getIntituleProgramme()),
                demande.getNiveauProgramme() == null ? "" : demande.getNiveauProgramme().name(),
                demande.getStatut() == null ? "" : demande.getStatut().name(),
                demande.getDateDecision() == null ? "" : demande.getDateDecision().format(DATE_DECISION_CANONIQUE),
                demande.getDateExpiration() == null ? "" : demande.getDateExpiration().toString());
    }

    private String nullToVide(String valeur) {
        return valeur == null ? "" : valeur;
    }

    private String hmacHex(String message) {
        try {
            Mac mac = Mac.getInstance(ALGORITHME);
            mac.init(new SecretKeySpec(cle.getBytes(StandardCharsets.UTF_8), ALGORITHME));
            byte[] brut = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(brut);
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("Impossible de signer l'attestation d'accréditation.", exception);
        }
    }
}
