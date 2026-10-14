package inpg.sn.lms.espace_etudiant.service;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.espace_etudiant.entity.Certificat;

/**
 * Signature electronique des certificats : un HMAC-SHA256 du contenu figé du
 * certificat (reference, etudiant, formation, date, emetteur), calcule avec une
 * cle connue uniquement du serveur.
 *
 * <p>Ce n'est pas une signature au sens PKI/PAdES (pas de certificat X.509, pas
 * de tiers de confiance) : c'est un scellement d'integrite verifiable par
 * quiconque possede la reference, via {@code GET /api/certificats/public/verifier/{reference}},
 * sans jamais exposer la cle. Recalculer le HMAC et le comparer a la valeur
 * stockee suffit a detecter une donnee modifiee apres la delivrance — c'est le
 * seul besoin exprime par le cahier des charges ("signature electronique reelle").
 */
@Service
public class CertificatSignatureService {

    private static final String ALGORITHME = "HmacSHA256";

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CertificatSignatureService.class);

    private final String cle;
    /** Clés précédentes, acceptées pour vérifier un certificat déjà délivré, jamais pour signer. */
    private final java.util.List<String> clesAnciennes;

    /**
     * La clé est obligatoire et n'a plus de repli sur {@code jwt.secret} (audit du 2026-10-14) : un même secret servait à
     * signer les jetons de connexion et les certificats, si bien que changer le premier (rotation, fuite) aurait
     * invalidé tous les certificats imprimés. Pour changer de clé sans perdre les certificats existants : nouvelle valeur
     * dans {@code CERTIFICAT_SIGNATURE_SECRET}, ancienne dans {@code CERTIFICAT_SIGNATURE_SECRETS_ANCIENS} (séparées
     * par des virgules).
     */
    @org.springframework.beans.factory.annotation.Autowired
    public CertificatSignatureService(@Value("${certificat.signature.secret:}") String cle,
            @Value("${certificat.signature.secrets-anciens:}") String clesAnciennes,
            @Value("${jwt.secret:}") String cleJwt) {
        if (cle == null || cle.isBlank()) {
            throw new IllegalStateException("CERTIFICAT_SIGNATURE_SECRET est obligatoire (clé de signature des certificats). "
                    + "Pour garder valides les certificats déjà délivrés, reprenez la valeur utilisée jusqu'ici "
                    + "(celle de JWT_SECRET si la variable était recopiée).");
        }
        this.cle = cle;
        this.clesAnciennes = java.util.Arrays.stream(clesAnciennes == null ? new String[0] : clesAnciennes.split(","))
                .map(String::trim).filter(c -> !c.isEmpty()).toList();
        if (cle.equals(cleJwt)) {
            log.warn("CERTIFICAT_SIGNATURE_SECRET est identique à JWT_SECRET : changer l'un invaliderait l'autre usage. "
                    + "Donnez-lui une valeur propre et passez l'ancienne dans CERTIFICAT_SIGNATURE_SECRETS_ANCIENS.");
        }
    }

    /** Pour les tests : une seule clé, aucune ancienne. */
    public CertificatSignatureService(String cle) {
        this(cle, "", "");
    }

    public String signer(Certificat certificat) {
        return hmacHex(contenuCanonique(certificat), cle);
    }

    /**
     * Un certificat sans signature (emis avant la mise en place de ce mecanisme)
     * n'est pas "invalide" : il est simplement non verifiable. Seule une
     * signature presente mais qui ne correspond plus au contenu actuel indique
     * une alteration.
     */
    public boolean estAlteree(Certificat certificat) {
        if (certificat.getSignature() == null) {
            return false;
        }
        String contenu = contenuCanonique(certificat);
        if (certificat.getSignature().equals(hmacHex(contenu, cle))) {
            return false;
        }
        // Signé avant un changement de clé : toujours authentique.
        return clesAnciennes.stream().noneMatch(ancienne -> certificat.getSignature().equals(hmacHex(contenu, ancienne)));
    }

    private String contenuCanonique(Certificat certificat) {
        Etudiant etudiant = certificat.getEtudiant();
        String nomEtudiant = etudiant == null ? "" : (nullToVide(etudiant.getPrenom()) + " " + nullToVide(etudiant.getNom())).trim();
        String contenu = String.join("|",
                nullToVide(certificat.getReference()),
                nomEtudiant,
                nullToVide(certificat.getIntituleFormation()),
                certificat.getDateEmission() == null ? "" : certificat.getDateEmission().toString(),
                nullToVide(certificat.getFormateurNom()));
        // Les champs du modèle INPG ne sont scellés que lorsqu'ils existent : un certificat délivré
        // avant leur ajout garde exactement le contenu canonique d'origine, donc sa signature reste valide.
        if (aChampsModeleInpg(certificat)) {
            contenu += "|" + String.join("|",
                    nullToVide(certificat.getCivilite()),
                    certificat.getDateNaissance() == null ? "" : certificat.getDateNaissance().toString(),
                    certificat.getDateDebut() == null ? "" : certificat.getDateDebut().toString(),
                    certificat.getDateFin() == null ? "" : certificat.getDateFin().toString(),
                    nullToVide(certificat.getCodeCertificat()));
        }
        return contenu;
    }

    private boolean aChampsModeleInpg(Certificat certificat) {
        return certificat.getCivilite() != null || certificat.getDateNaissance() != null
                || certificat.getDateDebut() != null || certificat.getDateFin() != null
                || certificat.getCodeCertificat() != null;
    }

    private String nullToVide(String valeur) {
        return valeur == null ? "" : valeur;
    }

    private static String hmacHex(String message, String cle) {
        try {
            Mac mac = Mac.getInstance(ALGORITHME);
            mac.init(new SecretKeySpec(cle.getBytes(StandardCharsets.UTF_8), ALGORITHME));
            byte[] brut = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(brut);
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("Impossible de signer le certificat.", exception);
        }
    }
}
