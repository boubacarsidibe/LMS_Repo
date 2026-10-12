package inpg.sn.lms.espace_etudiant.service;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.espace_etudiant.entity.Formation;
import inpg.sn.lms.espace_etudiant.entity.Promotion;
import inpg.sn.lms.espace_etudiant.entity.ReleveLigne;
import inpg.sn.lms.espace_etudiant.entity.ReleveNotes;
import inpg.sn.lms.pdf.RapportPdf;

/**
 * Génère le PDF d'un relevé de notes <strong>délivré</strong> (TDR §3.x).
 *
 * <p>Met en page les valeurs <strong>figées</strong> de {@link ReleveNotes} et
 * {@link ReleveLigne} : code, titre, coefficient et moyenne y sont recopiés à la
 * délivrance (migration V9), jamais recalculés. Deux relevés d'un même étudiant à
 * deux dates peuvent donc différer — c'est la définition d'un document.
 *
 * <p>Mise en page de {@link RapportPdf} : papier à en-tête de l'INPG (logo, pied de page, pagination), tableau des
 * cours à intitulés qui passent à la ligne, moyenne générale pondérée et mention, et filigrane léger anti-contrefaçon
 * répété sur <em>chaque</em> page (l'ancienne version ne le posait que sur la première).
 */
@Service
public class RelevePdfService {

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Mentions demandées par l'INPG pour le relevé (questionnaire de collecte du 2026-09-30) : ministère de tutelle
     * « MEP » — sigle repris tel que fourni, son intitulé complet reste à confirmer — et signature du Directeur
     * Pédagogique Industriel (DPI), dont le nom n'a pas été communiqué : seule la fonction est imprimée.
     */
    static final String MINISTERE_TUTELLE = "MEP";

    /** Place nécessaire à « Synthèse » (titre, trois lignes, mention) et au bloc de signature, en points PDF. */
    private static final float HAUTEUR_SYNTHESE_ET_SIGNATURE = 250f;
    static final String SIGNATAIRE = "Le Directeur Pédagogique Industriel";

    /** Génère le PDF et l'emballe en réponse HTTP « pièce jointe » au nom {@code INPG-releve-<référence>.pdf}. */
    public ResponseEntity<byte[]> reponsePdf(ReleveNotes releve) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("INPG-releve-" + releve.getReference() + ".pdf").build().toString())
                .body(genererPdf(releve));
    }

    public byte[] genererPdf(ReleveNotes releve) {
        try (RapportPdf pdf = new RapportPdf("Relevé de notes " + releve.getReference())) {
            pdf.filigrane(releve.getReference());

            pdf.titre("RELEVÉ DE NOTES");
            pdf.texte("Institut National du Pétrole et du Gaz");
            pdf.texte("Ministère de tutelle : " + MINISTERE_TUTELLE);
            pdf.meta("Référence : " + releve.getReference()
                    + "   -   Émis le " + (releve.getDateEmission() == null ? "-"
                            : releve.getDateEmission().format(FORMAT_DATE))
                    + "   -   " + nn(releve.getDelivrePar()));
            pdf.espace(10);

            Etudiant etudiant = releve.getEtudiant();
            Promotion promotion = releve.getPromotion() != null ? releve.getPromotion()
                    : (etudiant != null ? etudiant.getPromotion() : null);
            Formation formation = promotion != null ? promotion.getFormation() : null;

            pdf.section("Étudiant");
            if (etudiant != null) {
                pdf.ligneCle("Nom et prénom", nn(etudiant.getPrenom()) + " " + nn(etudiant.getNom()));
                pdf.ligneCle("Matricule", etudiant.getMatricule() == null ? "-" : etudiant.getMatricule());
            }
            pdf.ligneCle("Promotion", promotion == null ? "-" : promotion.getLibelle());
            pdf.ligneCle("Formation", formation == null ? "-" : formation.getIntitule());
            pdf.ligneCle("Période couverte", nn(releve.getPeriode()));
            pdf.espace(10);

            pdf.section("Résultats par cours");
            if (releve.getLignes().isEmpty()) {
                pdf.texteItalique("Aucune note figée sur ce relevé.");
            } else {
                List<String[]> lignes = new ArrayList<>();
                for (ReleveLigne ligne : releve.getLignes()) {
                    lignes.add(new String[] {
                            nn(ligne.getCoursCode()),
                            nn(ligne.getCoursTitre()),
                            nombre(ligne.getCoefficient()),
                            formatNote(ligne.getMoyenne()) });
                }
                pdf.tableau(new String[] { "Code", "Intitulé du cours", "Coef.", "Moyenne" },
                        new float[] { 1.3f, 4.3f, 0.8f, 2.6f }, lignes);
            }
            pdf.espace(10);

            // La synthèse et la signature restent ensemble : une signature seule sur une page ne prouve rien.
            pdf.reserver(HAUTEUR_SYNTHESE_ET_SIGNATURE);
            pdf.section("Synthèse");
            pdf.ligneCle("Moyenne générale (pondérée par les coefficients)", formatNote(releve.getMoyenneGenerale()));
            pdf.ligneCle("Nombre de notes prises en compte", String.valueOf(releve.getNombreNotes()));
            String mention = mention(releve.getMoyenneGenerale());
            if (mention != null) {
                pdf.ligneCle("Mention", mention);
            }
            pdf.espace(12);
            pdf.texteItalique("Document figé à sa date d'émission : ses valeurs ne sont pas recalculées.");
            pdf.blocSignature("Fait à Dakar" + (releve.getDateEmission() == null ? ""
                    : ", le " + releve.getDateEmission().format(FORMAT_DATE)), SIGNATAIRE);

            return pdf.generer();
        } catch (IOException | RuntimeException exception) {
            throw new RuntimeException("La génération du relevé PDF a échoué.", exception);
        }
    }

    private String mention(Double moyenneSur100) {
        if (moyenneSur100 == null) {
            return null;
        }
        if (moyenneSur100 >= 80) {
            return "Très bien";
        }
        if (moyenneSur100 >= 70) {
            return "Bien";
        }
        if (moyenneSur100 >= 60) {
            return "Assez bien";
        }
        return moyenneSur100 >= 50 ? "Passable" : "Insuffisant";
    }

    private static String formatNote(Double valeurSur100) {
        return valeurSur100 == null ? "-"
                : nombre(valeurSur100) + " / 100 (" + nombre(valeurSur100 / 5) + " / 20)";
    }

    private static String nombre(double valeur) {
        return String.valueOf(Math.round(valeur * 100) / 100.0);
    }

    private static String nn(String valeur) {
        return valeur == null ? "-" : valeur;
    }
}
