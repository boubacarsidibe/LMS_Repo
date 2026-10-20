package inpg.sn.lms.recrutement.service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.Entretien;
import inpg.sn.lms.recrutement.entity.TestAcademique;
import inpg.sn.lms.recrutement.enums.EnumEtapeQms;
import inpg.sn.lms.recrutement.enums.EnumStatutCandidature;
import inpg.sn.lms.recrutement.repository.CandidatureRepository;
import inpg.sn.lms.recrutement.repository.EntretienRepository;
import inpg.sn.lms.recrutement.repository.TestAcademiqueRepository;

/**
 * Fait avancer le statut d'un dossier d'après les <strong>résultats</strong> saisis : notes des épreuves écrites,
 * note d'entretien.
 *
 * <p>Depuis la migration du 22/09 vers vingt statuts, {@code TEST_REUSSI}, {@code TEST_ECHOUE} et
 * {@code ENTRETIEN_EFFECTUE} figuraient dans l'énumération et dans la table des transitions, mais plus aucun code ne
 * les attribuait. Un dossier « admis au test » ({@code CONVOQUE_TEST}) ne pouvait donc <em>jamais</em> être convoqué à
 * l'entretien ni admis : « Admettre à l'entretien » répondait « Transition de statut non autorisée »
 * (CONVOQUE_TEST → CONVOQUE_ENTRETIEN), et le pipeline s'arrêtait net à l'étape du test. Saisir ou corriger une note
 * n'avait aucun effet sur le dossier.
 *
 * <p>Règle (TDR §A, tests académiques : « détermination de pass/fail selon le seuil défini par l'INPG ») :
 * <ul>
 *   <li>tant qu'une épreuve du candidat n'a pas de note, le dossier reste {@code CONVOQUE_TEST} ;</li>
 *   <li>quand toutes sont notées, la moyenne pondérée par les coefficients est comparée au seuil de la session :
 *       {@code TEST_REUSSI} au seuil ou au-dessus, {@code TEST_ECHOUE} en dessous ;</li>
 *   <li>corriger une note peut faire basculer le résultat, tant que le dossier n'est pas passé à l'étape suivante ;</li>
 *   <li>la note d'entretien saisie fait passer le dossier en {@code ENTRETIEN_EFFECTUE}, son effacement le remet en
 *       {@code CONVOQUE_ENTRETIEN}.</li>
 * </ul>
 *
 * <p>Un dossier plus avancé n'est jamais touché : modifier après coup une note de test d'un candidat déjà admis ne
 * doit pas le faire reculer.
 */
@Service
@Transactional
public class ProgressionDossierService {

    /** Seuil de repli si le dossier n'est rattaché à aucune session — la valeur par défaut de la session. */
    private static final float SEUIL_PAR_DEFAUT = 10f;

    private static final Set<EnumStatutCandidature> ETATS_TEST = Set.of(
            EnumStatutCandidature.CONVOQUE_TEST, EnumStatutCandidature.TEST_REUSSI, EnumStatutCandidature.TEST_ECHOUE);
    private static final Set<EnumStatutCandidature> ETATS_ENTRETIEN = Set.of(
            EnumStatutCandidature.CONVOQUE_ENTRETIEN, EnumStatutCandidature.ENTRETIEN_EFFECTUE);

    private final CandidatureRepository candidatureRepository;
    private final TestAcademiqueRepository testAcademiqueRepository;
    private final EntretienRepository entretienRepository;
    private final JournalEtapeService journalEtapeService;

    public ProgressionDossierService(CandidatureRepository candidatureRepository,
            TestAcademiqueRepository testAcademiqueRepository,
            EntretienRepository entretienRepository,
            JournalEtapeService journalEtapeService) {
        this.candidatureRepository = candidatureRepository;
        this.testAcademiqueRepository = testAcademiqueRepository;
        this.entretienRepository = entretienRepository;
        this.journalEtapeService = journalEtapeService;
    }

    /** À appeler après toute création, saisie, correction ou suppression d'une épreuve du candidat. */
    public void apresTests(Long candidatId) {
        Candidature candidature = dossierDe(candidatId);
        if (candidature == null || !ETATS_TEST.contains(candidature.getStatut())) {
            return;
        }
        Double moyenne = moyennePonderee(testAcademiqueRepository.findByCandidatId(candidatId));
        if (moyenne == null) {
            appliquer(candidature, EnumStatutCandidature.CONVOQUE_TEST, EnumEtapeQms.EVALUATION,
                    "Notes des épreuves incomplètes : le dossier reste en attente des résultats du test.");
            return;
        }
        float seuil = candidature.getSessionCandidature() != null
                ? candidature.getSessionCandidature().getSeuilReussiteTest() : SEUIL_PAR_DEFAUT;
        boolean reussi = moyenne >= seuil;
        appliquer(candidature,
                reussi ? EnumStatutCandidature.TEST_REUSSI : EnumStatutCandidature.TEST_ECHOUE,
                EnumEtapeQms.EVALUATION,
                String.format(Locale.FRENCH, "Moyenne aux épreuves : %.2f/20 (seuil de réussite : %.1f/20) — test %s.",
                        moyenne, seuil, reussi ? "réussi" : "échoué"));
    }

    /** À appeler après toute création, saisie de note ou suppression d'un entretien du candidat. */
    public void apresEntretien(Long candidatId) {
        Candidature candidature = dossierDe(candidatId);
        if (candidature == null || !ETATS_ENTRETIEN.contains(candidature.getStatut())) {
            return;
        }
        List<Entretien> entretiens = entretienRepository.findByCandidatId(candidatId);
        boolean tousNotes = !entretiens.isEmpty() && entretiens.stream().allMatch(e -> e.getNoteEntretien() != null);
        if (tousNotes) {
            appliquer(candidature, EnumStatutCandidature.ENTRETIEN_EFFECTUE, EnumEtapeQms.EVALUATION,
                    "Note d'entretien saisie : entretien effectué, dossier prêt pour la délibération.");
        } else {
            appliquer(candidature, EnumStatutCandidature.CONVOQUE_ENTRETIEN, EnumEtapeQms.EVALUATION,
                    "Note d'entretien absente : le dossier reste en attente de l'entretien.");
        }
    }

    /**
     * Moyenne des épreuves pondérée par leur coefficient, sur 20.
     *
     * @return {@code null} s'il n'y a aucune épreuve ou si l'une d'elles n'a pas encore de note : un résultat ne se
     *         décide qu'une fois toutes les notes connues
     */
    static Double moyennePonderee(List<TestAcademique> tests) {
        if (tests == null || tests.isEmpty() || tests.stream().anyMatch(t -> t.getNoteObtenue() == null)) {
            return null;
        }
        double poids = 0;
        double total = 0;
        for (TestAcademique t : tests) {
            double coefficient = t.getCoefficient() == null || t.getCoefficient() <= 0 ? 1 : t.getCoefficient();
            total += t.getNoteObtenue() * coefficient;
            poids += coefficient;
        }
        return total / poids;
    }

    /** Un candidat n'a normalement qu'un dossier actif (garde-fou de CandidatController) : le premier suffit. */
    private Candidature dossierDe(Long candidatId) {
        return candidatureRepository.findByCandidatId(candidatId).stream().findFirst().orElse(null);
    }

    private void appliquer(Candidature candidature, EnumStatutCandidature cible, EnumEtapeQms etape, String commentaire) {
        if (candidature.getStatut() == cible) {
            return;
        }
        candidature.setStatutPrecedent(candidature.getStatut());
        candidature.setStatut(cible);
        candidatureRepository.save(candidature);
        journalEtapeService.enregistrer(candidature, etape, cible, null, commentaire);
    }
}
