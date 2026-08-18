package inpg.sn.lms.espace_etudiant.service;

import static org.assertj.core.api.Assertions.assertThat;
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

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.NoteModule;
import inpg.sn.lms.espace_etudiant.entity.ReleveNotes;
import inpg.sn.lms.espace_etudiant.repository.ReleveNotesRepository;

/**
 * Délivrance d'un relevé de notes.
 *
 * <p>Ce qui est vérifié ici tient en une propriété : la délivrance <strong>fige</strong>
 * les valeurs. C'est elle qui distingue un document d'un écran — un relevé remis en
 * janvier ne doit pas changer parce qu'un formateur corrige une note en février.
 *
 * <p>Sans ces tests, la régression serait invisible : il suffirait qu'une ligne
 * référence le cours au lieu d'en recopier les valeurs pour que tous les relevés déjà
 * remis se réécrivent en silence, des mois plus tard, sans qu'aucune erreur ne soit
 * levée. Personne ne s'en apercevrait avant qu'un étudiant ne compare deux tirages.
 */
@ExtendWith(MockitoExtension.class)
class ReleveDelivranceServiceTest {

    @Mock
    private ReleveNotesRepository releveRepository;

    @Mock
    private NoteModuleService noteModuleService;

    private ReleveDelivranceService service;
    private Etudiant etudiant;

    @BeforeEach
    void preparer() {
        // Reel, pas simule : c'est le calcul que la delivrance doit figer.
        service = new ReleveDelivranceService(releveRepository, new ReleveNotesService(noteModuleService));
        etudiant = new Etudiant();
        etudiant.setId(38L);
        // Le depot rend l'objet tel quel : on inspecte ce qui allait etre ecrit.
        when(releveRepository.save(any(ReleveNotes.class))).thenAnswer(appel -> appel.getArgument(0));
    }

    /** Le groupement par cours passe par l'identifiant : un cours sans id serait ignoré. */
    private Cours cours(Long id, String code, String titre, double coefficient) {
        Cours cours = new Cours();
        setId(cours, id);
        cours.setNom(code);
        cours.setTitre(titre);
        cours.setCoefficient(coefficient);
        return cours;
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

    private NoteModule note(Cours cours, String module, double valeur) {
        NoteModule note = new NoteModule();
        note.setCours(cours);
        note.setModule(module);
        note.setNote(valeur);
        note.setDateEvaluation(LocalDate.of(2026, 8, 12));
        note.setFormateurNom("Moussa Ndiaye");
        return note;
    }

    @Test
    @DisplayName("Le relevé délivré recopie les moyennes et les coefficients")
    void recopie_les_valeurs_du_releve_courant() {
        Cours ml = cours(9L, "DS-101", "Introduction au Machine Learning", 3);
        Cours eco = cours(16L, "ECO-203", "Économie pétrolière", 1);
        when(noteModuleService.getByEtudiantId(38L)).thenReturn(List.of(
                note(ml, "Fondamentaux", 8),
                note(ml, "Supervisé", 10),
                note(eco, "Marchés", 18)));

        ReleveNotes releve = service.delivrer(etudiant, "Semestre 1 — 2025/2026");

        // (9 x 3 + 18 x 1) / 4 = 11,25 -> 11,3
        assertThat(releve.getMoyenneGenerale()).isEqualTo(11.3);
        assertThat(releve.getNombreNotes()).isEqualTo(3);
        assertThat(releve.getLignes()).hasSize(2);
        assertThat(releve.getLignes())
                .extracting("coursCode", "coefficient", "moyenne")
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("DS-101", 3.0, 9.0),
                        org.assertj.core.groups.Tuple.tuple("ECO-203", 1.0, 18.0));
    }

    /**
     * Le cœur du sujet. Une note corrigée après la délivrance ne doit rien changer au
     * document déjà remis — sinon ce n'est pas un relevé, c'est une vue.
     */
    @Test
    @DisplayName("Une note corrigée après coup ne modifie pas le relevé déjà délivré")
    void le_releve_delivre_ne_suit_pas_les_notes_ulterieures() {
        Cours ml = cours(9L, "DS-101", "Introduction au Machine Learning", 1);
        NoteModule saisie = note(ml, "Fondamentaux", 8);
        when(noteModuleService.getByEtudiantId(38L)).thenReturn(List.of(saisie));

        ReleveNotes releve = service.delivrer(etudiant, "Semestre 1 — 2025/2026");
        assertThat(releve.getMoyenneGenerale()).isEqualTo(8.0);

        // Le formateur corrige, et le cours change de coefficient l'annee suivante.
        saisie.setNote(15);
        ml.setCoefficient(4);
        ml.setTitre("Machine Learning avancé");

        assertThat(releve.getMoyenneGenerale()).isEqualTo(8.0);
        assertThat(releve.getLignes().get(0).getMoyenne()).isEqualTo(8.0);
        assertThat(releve.getLignes().get(0).getCoefficient()).isEqualTo(1.0);
        assertThat(releve.getLignes().get(0).getCoursTitre())
                .isEqualTo("Introduction au Machine Learning");
    }

    @Test
    @DisplayName("Un cours sans note ne figure pas au relevé")
    void un_cours_sans_note_est_ecarte() {
        Cours ml = cours(9L, "DS-101", "Introduction au Machine Learning", 1);
        when(noteModuleService.getByEtudiantId(38L)).thenReturn(List.of(note(ml, "Fondamentaux", 12)));

        ReleveNotes releve = service.delivrer(etudiant, "Semestre 1 — 2025/2026");

        // Un cours affiche avec un tiret laisserait croire a un echec la ou le
        // formateur n'a simplement rien corrige.
        assertThat(releve.getLignes()).hasSize(1);
        assertThat(releve.getLignes().get(0).getCoursCode()).isEqualTo("DS-101");
    }

    /**
     * Réémettre est légitime — duplicata, relevé corrigé — et on ne supprime pas un
     * document délivré. Mais un double clic produit le même résultat qu'une réémission
     * voulue : le service doit pouvoir signaler le cas, sans jamais le bloquer.
     */
    @Test
    @DisplayName("Un relevé déjà délivré pour la même période est signalé, pas refusé")
    void une_reemission_est_signalee_sans_etre_bloquee() {
        when(releveRepository.existsByEtudiant_IdAndPeriode(38L, "Semestre 1 — 2025/2026"))
                .thenReturn(true);

        assertThat(service.dejaDelivre(38L, "Semestre 1 — 2025/2026")).isTrue();

        // Et la delivrance reste possible : c'est un avertissement, pas un verrou.
        Cours ml = cours(9L, "DS-101", "Introduction au Machine Learning", 1);
        when(noteModuleService.getByEtudiantId(38L)).thenReturn(List.of(note(ml, "Fondamentaux", 12)));
        assertThat(service.delivrer(etudiant, "Semestre 1 — 2025/2026")).isNotNull();
    }

    @Test
    @DisplayName("Le relevé est délivré au nom de l'établissement, pas de l'agent connecté")
    void delivre_au_nom_de_l_etablissement() {
        Cours ml = cours(9L, "DS-101", "Introduction au Machine Learning", 1);
        when(noteModuleService.getByEtudiantId(38L)).thenReturn(List.of(note(ml, "Fondamentaux", 12)));

        ReleveNotes releve = service.delivrer(etudiant, "Semestre 1 — 2025/2026");

        assertThat(releve.getDelivrePar()).isEqualTo("INPG — Direction pédagogique");
    }
}
