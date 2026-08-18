package inpg.sn.lms.espace_etudiant.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import inpg.sn.lms.auth.repository.FormateurRepository;
import inpg.sn.lms.espace_etudiant.dto.CoursCreateRequest;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.service.CoursService;
import inpg.sn.lms.espace_etudiant.service.FormationService;
import inpg.sn.lms.espace_etudiant.service.PromotionService;
import inpg.sn.lms.espace_etudiant.service.SupportService;

/**
 * Rattachement d'un cours a son programme, depuis l'espace pedagogique.
 *
 * <p>Un cours tient sa place dans le cursus d'une formation, et les promotions en
 * heritent. Sans formation, aucune promotion ne le recoit : le cours existe en base
 * et n'est visible d'aucun etudiant. Le formulaire ne demandait pas la formation, si
 * bien que tout cours cree ici naissait dans cet etat — six cours sur neuf l'etaient.
 *
 * <p>Le piege est la distinction entre <strong>champ absent</strong> et
 * <strong>liste vide</strong>. Les confondre produit l'un ou l'autre de ces defauts,
 * tous deux silencieux :
 *
 * <ul>
 *   <li>traiter {@code null} comme une liste vide : modifier le titre d'un cours le
 *       retirerait de tous ses programmes ;</li>
 *   <li>traiter une liste vide comme {@code null} : on ne pourrait plus jamais
 *       retirer un cours d'un programme.</li>
 * </ul>
 *
 * <p>Aucun des deux ne leve d'erreur ni ne laisse de trace dans les journaux : seul
 * un ecran vide, plus tard, le revele. D'ou ces tests.
 */
@ExtendWith(MockitoExtension.class)
class PedagogieCoursProgrammeTest {

    @Mock
    private FormationService formationService;

    @Mock
    private PromotionService promotionService;

    @Mock
    private CoursService coursService;

    @Mock
    private SupportService supportService;

    @Mock
    private FormateurRepository formateurRepository;

    @InjectMocks
    private PedagogieController controleur;

    private static final Long COURS_ID = 7L;

    private Cours coursExistant() {
        Cours cours = new Cours();
        cours.setNom("GEO-101");
        cours.setTitre("Geologie des reservoirs");
        cours.setDescription("");
        cours.setSemestre("S1");
        cours.setVolumeHoraire(30L);
        return cours;
    }

    private CoursCreateRequest requete(List<Long> formationIds) {
        return new CoursCreateRequest("GEO-101", "Geologie des reservoirs", "S1", "", 30L, null, formationIds);
    }

    private void simulerCoursEnBase() {
        Cours cours = coursExistant();
        when(coursService.findById(COURS_ID)).thenReturn(Optional.of(cours));
        when(coursService.save(any(Cours.class))).thenReturn(cours);
    }

    @Test
    @DisplayName("les formations demandees sont transmises telles quelles au rattachement")
    void les_formations_demandees_sont_rattachees() {
        simulerCoursEnBase();

        controleur.modifierCours(COURS_ID, requete(List.of(1L, 4L)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        verify(formationService).rattacherAuxFormations(any(Cours.class), captor.capture());
        assertThat(captor.getValue()).containsExactly(1L, 4L);
    }

    @Test
    @DisplayName("une liste vide retire le cours de tous ses programmes")
    void une_liste_vide_detache_le_cours() {
        simulerCoursEnBase();

        controleur.modifierCours(COURS_ID, requete(List.of()));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        verify(formationService).rattacherAuxFormations(any(Cours.class), captor.capture());
        assertThat(captor.getValue()).isEmpty();
    }

    /**
     * Le cas qui coute le plus cher : l'espace formateur envoie {@code formationIds}
     * absent. Le prendre pour une liste vide detacherait le cours a chaque
     * enregistrement, sans que rien ne le signale.
     */
    @Test
    @DisplayName("aucun champ de programme renseigne : le rattachement n'est pas touche")
    void champ_absent_ne_touche_pas_au_programme() {
        simulerCoursEnBase();

        controleur.modifierCours(COURS_ID, requete(null));

        verify(formationService, never()).rattacherAuxFormations(any(Cours.class), any());
        verify(formationService, never()).rattacherAuxFormationsDe(any(Cours.class), any());
    }

    @Test
    @DisplayName("un cours cree sans programme est bien cree, mais hors programme")
    void creation_sans_programme_reste_possible() {
        Cours cours = coursExistant();
        when(coursService.save(any(Cours.class))).thenReturn(cours);
        // any() et non anyLong() : le cours tout juste cree n'a pas encore d'identifiant,
        // et rattacher() relit donc findById(null).
        when(coursService.findById(any())).thenReturn(Optional.of(cours));

        controleur.creerCours(requete(List.of()));

        verify(formationService).rattacherAuxFormations(any(Cours.class), any());
        // L'ecran signale ce cours en rouge : le serveur ne l'interdit pas, car un
        // cours peut legitimement etre prepare avant d'etre inscrit a un cursus.
        assertThat(cours.getFormations()).isEmpty();
    }
}
