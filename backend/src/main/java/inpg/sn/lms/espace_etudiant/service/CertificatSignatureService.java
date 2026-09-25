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

    private final String cle;

    public CertificatSignatureService(@Value("${certificat.signature.secret}") String cle) {
        this.cle = cle;
    }

    public String signer(Certificat certificat) {
        return hmacHex(contenuCanonique(certificat));
    }

    /**
     * Un certificat sans signature (emis avant la mise en place de ce mecanisme)
     * n'est pas "invalide" : il est simplement non verifiable. Seule une
     * signature presente mais qui ne correspond plus au contenu actuel indique
     * une alteration.
     */
    public boolean estAlteree(Certificat certificat) {
        return certificat.getSignature() != null && !certificat.getSignature().equals(signer(certificat));
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

    private String hmacHex(String message) {
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
