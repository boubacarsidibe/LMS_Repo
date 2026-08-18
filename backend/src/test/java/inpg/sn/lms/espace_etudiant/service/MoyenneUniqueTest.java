package inpg.sn.lms.espace_etudiant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.NoteModule;

/**
 * Une seule moyenne académique, pour les trois rôles.
 *
 * <p>Trois écrans affichaient trois chiffres différents pour le même étudiant :
 *
 * <ul>
 *   <li>l'étudiant, sur son relevé, une moyenne pondérée par les coefficients ;</li>
 *   <li>le formateur, dans « Mes étudiants », une moyenne à plat — un cours y comptait
 *       autant de fois qu'il avait de devoirs ;</li>
 *   <li>le responsable pédagogique, dans sa liste, un <strong>zéro codé en dur</strong>.</li>
 * </ul>
 *
 * <p>C'est le responsable pédagogique qui délivre les relevés et relance les étudiants
 * en difficulté : il décidait donc sur la seule donnée qui ne voulait rien dire. Et une
 * divergence entre deux chiffres académiques ne lève aucune erreur — elle se découvre
 * quand un étudiant conteste sa moyenne.
 *
 * <p>Ce test fige l'accord : {@link ReleveNotesService} est la seule définition, et le
 * chiffre du relevé est celui des écrans de suivi.
 */
@ExtendWith(MockitoExtension.class)
class MoyenneUniqueTest {

    @Mock
    private NoteModuleService noteModuleService;

    private ReleveNotesService service;

    @BeforeEach
    void preparer() {
        service = new ReleveNotesService(noteModuleService);
    }

    private Cours cours(Long id, String code, double coefficient) {
        Cours cours = new Cours();
        try {
            var champ = Cours.class.getDeclaredField("id");
            champ.setAccessible(true);
            champ.set(cours, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Impossible de fixer l'id", e);
        }
        cours.setNom(code);
        cours.setTitre(code);
        cours.setCoefficient(coefficient);
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

    /**
     * Le cas qui séparait les deux chiffres : un cours évalué deux fois, l'autre une
     * seule. La moyenne à plat pesait alors le premier cours double, sans que personne
     * ne l'ait décidé.
     */
    @Test
    @DisplayName("La moyenne de suivi est celle du relevé, pas une moyenne à plat")
    void la_moyenne_de_suivi_est_celle_du_releve() {
        Cours ml = cours(9L, "DS-101", 1);
        Cours eco = cours(16L, "ECO-203", 1);
        when(noteModuleService.getByEtudiantId(38L)).thenReturn(List.of(
                note(ml, "Fondamentaux", 8),
                note(ml, "Supervisé", 10),
                note(eco, "Marchés", 18)));

        Double suivi = service.moyenneDeLEtudiant(38L);

        // Relevé : (9 + 18) / 2 = 13,5
        // À plat  : (8 + 10 + 18) / 3 = 12,0  <- l'ancien chiffre du formateur
        assertThat(suivi).isEqualTo(13.5);
        assertThat(suivi).isEqualTo(service.duEtudiant(38L).moyenneGenerale());
    }

    /**
     * Sans note, la moyenne est {@code null}, pas zéro. Un zéro se lit comme un
     * résultat catastrophique là où il n'y a simplement rien à afficher — c'est
     * exactement ce que montrait la liste du responsable pédagogique.
     */
    @Test
    @DisplayName("Aucune note donne null, jamais zéro")
    void aucune_note_ne_vaut_pas_zero() {
        when(noteModuleService.getByEtudiantId(38L)).thenReturn(List.of());

        assertThat(service.moyenneDeLEtudiant(38L)).isNull();
        assertThat(service.statutDeSuivi(null)).isEqualTo("À surveiller");
    }

    @Test
    @DisplayName("Le statut de suivi bascule à 12")
    void le_statut_bascule_a_douze() {
        assertThat(service.statutDeSuivi(11.9)).isEqualTo("À risque");
        assertThat(service.statutDeSuivi(12.0)).isEqualTo("Actif");
        assertThat(service.statutDeSuivi(18.0)).isEqualTo("Actif");
    }
}
