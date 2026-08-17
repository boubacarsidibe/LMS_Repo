package inpg.sn.lms.espace_etudiant.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.espace_etudiant.dto.ReleveNotesDto;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.NoteModule;
import inpg.sn.lms.espace_etudiant.repository.CoursRepository;
import inpg.sn.lms.espace_etudiant.repository.RetourEvaluationRepository;
import inpg.sn.lms.espace_etudiant.repository.ProgressionSupportRepository;
import inpg.sn.lms.espace_etudiant.repository.SeanceRepository;
import inpg.sn.lms.espace_etudiant.service.HabilitationCoursService;
import inpg.sn.lms.espace_etudiant.service.MatriculeService;
import inpg.sn.lms.espace_etudiant.service.NoteModuleService;
import inpg.sn.lms.espace_etudiant.service.SupportService;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;

/**
 * Relevé de notes de l'étudiant.
 *
 * <p>Ces notes existaient en base depuis l'origine mais n'étaient exposées que par
 * des endpoints réservés au formateur : l'étudiant n'avait aucun moyen de consulter
 * ses propres résultats. Le champ {@code maNote} de la liste des cours ne s'y
 * substituait pas — il porte l'appréciation que l'étudiant donne au cours.
 *
 * <p>Le piège figé ici est la distinction entre <strong>« aucune note »</strong> et
 * <strong>« moyenne de zéro »</strong> : renvoyer 0 dans le premier cas afficherait
 * un échec à un étudiant qui n'a simplement pas encore été évalué.
 */
@ExtendWith(MockitoExtension.class)
class ScolariteReleveNotesTest {

    @Mock private UtilisateurCourantService utilisateurCourant;
    @Mock private MatriculeService matriculeService;
    @Mock private CoursRepository coursRepository;
    @Mock private SeanceRepository seanceRepository;
    @Mock private SupportService supportService;
    @Mock private ProgressionSupportRepository progressionRepository;
    @Mock private RetourEvaluationRepository retourRepository;
    @Mock private EtudiantRepository etudiantRepository;
    @Mock private HabilitationCoursService habilitation;
    @Mock private NoteModuleService noteModuleService;

    private ScolariteController controller;
    private Etudiant etudiant;
    private Authentication authentication;

    @BeforeEach
    void preparer() {
        controller = new ScolariteController(utilisateurCourant, matriculeService, coursRepository,
                seanceRepository, supportService, progressionRepository, retourRepository,
                etudiantRepository, habilitation, noteModuleService);

        etudiant = new Etudiant();
        etudiant.setId(38L);
        authentication = null; // utilisateurCourant est simulé : la valeur n'est jamais lue
    }

    private Cours cours(Long id, String code, String titre) {
        Cours cours = new Cours();
        setId(cours, id);
        cours.setNom(code);
        cours.setTitre(titre);
        return cours;
    }

    private NoteModule note(Cours cours, String module, double valeur) {
        NoteModule note = new NoteModule();
        note.setCours(cours);
        note.setModule(module);
        note.setNote(valeur);
        note.setDateEvaluation(LocalDate.of(2026, 8, 12));
        note.setFormateurNom("Moussa Ndiaye");
        return note;
    }

    private void setId(Object entite, Long id) {
        try {
            var champ = entite.getClass().getDeclaredField("id");
            champ.setAccessible(true);
            champ.set(entite, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Impossible de fixer l'id", e);
        }
    }

    private ReleveNotesDto releve() {
        ResponseEntity<?> reponse = controller.mesNotes(authentication);
        assertEquals(HttpStatus.OK, reponse.getStatusCode());
        return assertInstanceOf(ReleveNotesDto.class, reponse.getBody());
    }

    @Test
    @DisplayName("Les notes sont groupées par cours, avec une moyenne par cours et une moyenne générale")
    void groupe_par_cours_et_calcule_les_moyennes() {
        Cours ml = cours(9L, "DS-101", "Introduction au Machine Learning");
        Cours finance = cours(10L, "FIN-201", "Analyse Financiere Avancee");
        when(utilisateurCourant.etudiant(any())).thenReturn(etudiant);
        when(noteModuleService.getByEtudiantId(38L)).thenReturn(List.of(
                note(ml, "Fondamentaux du ML", 16),
                note(ml, "Apprentissage supervisé", 15),
                note(finance, "Analyse de bilan", 13)));

        ReleveNotesDto releve = releve();

        assertEquals(3, releve.nombreNotes());
        // (16 + 15 + 13) / 3 = 14,666… arrondi au dixième
        assertEquals(14.7, releve.moyenneGenerale());
        assertEquals(2, releve.parCours().size());

        ReleveNotesDto.NotesCoursDto premier = releve.parCours().get(0);
        assertEquals("DS-101", premier.coursCode());
        assertEquals(15.5, premier.moyenne());
        assertEquals(2, premier.notes().size());
    }

    /**
     * Un étudiant pas encore évalué ne doit pas voir « 0 » : ce serait lui annoncer un
     * échec. La distinction se fait sur {@code null}.
     */
    @Test
    @DisplayName("Sans aucune note, la moyenne est absente — pas égale à zéro")
    void aucune_note_ne_vaut_pas_zero() {
        when(utilisateurCourant.etudiant(any())).thenReturn(etudiant);
        when(noteModuleService.getByEtudiantId(38L)).thenReturn(List.of());

        ReleveNotesDto releve = releve();

        assertNull(releve.moyenneGenerale());
        assertEquals(0, releve.nombreNotes());
        assertTrue(releve.parCours().isEmpty());
    }

    /** Une vraie moyenne de zéro reste, elle, bien renvoyée. */
    @Test
    @DisplayName("Une moyenne réellement nulle est distinguée de l'absence de note")
    void une_moyenne_de_zero_est_renvoyee() {
        Cours ml = cours(9L, "DS-101", "Introduction au Machine Learning");
        when(utilisateurCourant.etudiant(any())).thenReturn(etudiant);
        when(noteModuleService.getByEtudiantId(38L)).thenReturn(List.of(note(ml, "Rattrapage", 0)));

        ReleveNotesDto releve = releve();

        assertEquals(0.0, releve.moyenneGenerale());
        assertEquals(1, releve.nombreNotes());
    }

    /**
     * Une note dont le cours a été supprimé ne doit pas faire échouer le relevé.
     * Elle reste comptée dans la moyenne : elle a bien été obtenue.
     */
    @Test
    @DisplayName("Une note orpheline ne casse pas le relevé")
    void note_sans_cours() {
        when(utilisateurCourant.etudiant(any())).thenReturn(etudiant);
        when(noteModuleService.getByEtudiantId(38L)).thenReturn(List.of(note(null, "Module retiré", 12)));

        ReleveNotesDto releve = releve();

        assertEquals(1, releve.nombreNotes());
        assertEquals(12.0, releve.moyenneGenerale());
        assertTrue(releve.parCours().isEmpty());
    }

    @Test
    @DisplayName("Un compte sans fiche étudiant reçoit une erreur explicite, pas un relevé vide")
    void compte_sans_fiche_etudiant() {
        when(utilisateurCourant.etudiant(any())).thenReturn(null);

        assertEquals(HttpStatus.NOT_FOUND, controller.mesNotes(authentication).getStatusCode());
    }
}
