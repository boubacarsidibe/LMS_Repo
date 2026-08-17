package inpg.sn.lms.espace_etudiant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.Formation;
import inpg.sn.lms.espace_etudiant.entity.Promotion;
import inpg.sn.lms.espace_etudiant.repository.PromotionRepository;

/**
 * Descente d'un cours vers les promotions qui le suivent.
 *
 * <p>C'est le maillon qui a motive tout ce correctif. Deux chemins repondaient
 * autrefois a « cet etudiant suit-il ce cours ? » : l'un partait de la table
 * {@code cours_promotion}, l'autre de {@code formation_cours}. Ils avaient diverge
 * au point de ne plus designer aucun cours commun — un etudiant recevait 403 sur le
 * cours portant ses propres notes. Il ne reste qu'un chemin, teste ici :
 * <strong>Cours -> Formation -> Promotion</strong>.
 */
@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

    @Mock
    private PromotionRepository promotionRepository;

    @InjectMocks
    private PromotionService promotionService;

    private Formation formation(Long id) {
        Formation formation = new Formation();
        setId(formation, id);
        formation.setCode("F-" + id);
        formation.setIntitule("Formation " + id);
        return formation;
    }

    private Promotion promotion(Long id) {
        Promotion promotion = new Promotion();
        setId(promotion, id);
        return promotion;
    }

    private Cours coursDesFormations(Formation... formations) {
        Cours cours = new Cours();
        cours.getFormations().addAll(List.of(formations));
        return cours;
    }

    /** Ni Promotion ni Formation n'exposent setId (id genere par la base). */
    private void setId(Object entite, Long id) {
        try {
            var champ = entite.getClass().getDeclaredField("id");
            champ.setAccessible(true);
            champ.set(entite, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Impossible de fixer l'id de " + entite.getClass().getSimpleName(), e);
        }
    }

    @Test
    @DisplayName("Les promotions d'un cours sont celles des programmes qui le portent")
    void desFormationsDuCours_remonteLesProgrammes() {
        Cours cours = coursDesFormations(formation(1L), formation(2L));
        when(promotionRepository.findByFormation_IdIn(List.of(1L, 2L)))
                .thenReturn(List.of(promotion(10L), promotion(11L)));

        assertThat(promotionService.desFormationsDuCours(cours))
                .extracting(Promotion::getId)
                .containsExactlyInAnyOrder(10L, 11L);
    }

    /**
     * Cas rencontre en production : un cours cree par le responsable pedagogique
     * n'etait rattache a aucun programme. Il ne doit donner acces a personne — surtout
     * pas au catalogue entier.
     */
    @Test
    @DisplayName("Un cours hors de tout programme ne touche aucune promotion")
    void desFormationsDuCours_coursOrphelin() {
        assertThat(promotionService.desFormationsDuCours(coursDesFormations())).isEmpty();
        verify(promotionRepository, never()).findByFormation_IdIn(anyCollection());
    }

    @Test
    @DisplayName("Un cours absent ne fait pas planter le calcul")
    void desFormationsDuCours_coursNull() {
        assertThat(promotionService.desFormationsDuCours(null)).isEmpty();
        verify(promotionRepository, never()).findByFormation_IdIn(anyCollection());
    }

    @Test
    @DisplayName("Les promotions de plusieurs programmes sont dedoublonnees")
    void promotionIdsDesFormations_dedoublonne() {
        Promotion partagee = promotion(10L);
        when(promotionRepository.findByFormation_IdIn(List.of(1L, 2L)))
                .thenReturn(List.of(partagee, partagee, promotion(11L)));

        assertThat(promotionService.promotionIdsDesFormations(List.of(1L, 2L)))
                .containsExactlyInAnyOrder(10L, 11L);
    }

    @Test
    @DisplayName("Une liste de programmes vide n'interroge pas la base")
    void promotionIdsDesFormations_listeVide() {
        assertThat(promotionService.promotionIdsDesFormations(List.of())).isEmpty();
        assertThat(promotionService.promotionIdsDesFormations(null)).isEmpty();
        verify(promotionRepository, never()).findByFormation_IdIn(anyCollection());
    }
}
