package inpg.sn.lms.espace_etudiant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.entity.Formateur;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.auth.repository.FormateurRepository;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.Formation;
import inpg.sn.lms.espace_etudiant.entity.Promotion;
import inpg.sn.lms.espace_etudiant.entity.SupportCours;

/**
 * Ces tests protegent le cloisonnement des donnees entre formateurs : un
 * formateur ne doit voir que les etudiants des promotions rattachees a SES cours.
 * Une regression sur ces regles rouvrirait une fuite de donnees, d'ou leur
 * presence ici.
 *
 * <p>Un cours ne se rattache plus directement a une promotion : il appartient a une
 * formation, et une promotion suit une formation. La descente formation -> promotions
 * est une requete, portee par {@link PromotionService} et couverte par son propre
 * test ; elle est simulee ici. Ce qui reste verifie sur ce service est ce qu'il
 * calcule lui-meme : l'extraction des formations des cours du formateur, le
 * dedoublonnage, les cas vides, et les regles qui ne dependent d'aucune requete
 * (appartenance d'un etudiant a un cours, propriete d'un cours, publication
 * programmee).
 *
 * <p>L'entite Cohorte a ete renommee Promotion lors de la fusion avec la branche
 * pedagogie. Les methodes du service portent encore le prefixe {@code cohorteIds}
 * : le renommage cote service touche le controleur et n'a pas ete fait ici pour
 * garder ce correctif limite au test.
 */
@ExtendWith(MockitoExtension.class)
class PerimetreFormateurServiceTest {

    @Mock
    private CoursService coursService;

    @Mock
    private FormateurRepository formateurRepository;

    @Mock
    private EtudiantRepository etudiantRepository;

    @Mock
    private PromotionService promotionService;

    @InjectMocks
    private PerimetreFormateurService perimetreFormateurService;

    // ---------- Outils de construction d'objets de test ----------

    /**
     * Promotion dotee de son propre programme, portant le meme identifiant.
     * Reproduit la structure reelle : chaque promotion suit une formation.
     */
    private Promotion promotion(Long id) {
        Promotion promotion = new Promotion();
        setId(promotion, id);
        Formation formation = new Formation();
        setId(formation, id);
        formation.setCode("F-" + id);
        formation.setIntitule("Formation " + id);
        promotion.setFormation(formation);
        return promotion;
    }

    private Formateur formateur(Long id) {
        Formateur formateur = new Formateur();
        formateur.setId(id);
        return formateur;
    }

    /**
     * Cours inscrit au programme de ces promotions. Les deux cotes de la relation
     * sont cables, comme le ferait la base.
     */
    private Cours cours(Promotion... promotions) {
        Cours cours = new Cours();
        for (Promotion promotion : promotions) {
            Formation formation = promotion.getFormation();
            formation.getCours().add(cours);
            cours.getFormations().add(formation);
        }
        return cours;
    }

    private Etudiant etudiant(Promotion promotion) {
        Etudiant etudiant = new Etudiant();
        etudiant.setPromotion(promotion);
        return etudiant;
    }

    /** Ni Promotion ni Formation n'exposent setId (id genere par la base) : on le force par reflexion. */
    private void setId(Object entite, Long id) {
        try {
            var champ = entite.getClass().getDeclaredField("id");
            champ.setAccessible(true);
            champ.set(entite, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Impossible de fixer l'id de " + entite.getClass().getSimpleName(), e);
        }
    }

    // ---------- cohorteIdsDuFormateur ----------

    @Test
    @DisplayName("Interroge les formations de tous les cours du formateur, sans doublon")
    void promotionsDuFormateur_dedoublonneLesFormations() {
        Formateur formateur = formateur(10L);
        // Deux cours partagent le programme 1 : il ne doit etre demande qu'une fois.
        when(coursService.getByFormateurId(10L))
                .thenReturn(List.of(cours(promotion(1L)), cours(promotion(1L), promotion(2L))));
        when(promotionService.promotionIdsDesFormations(anyList())).thenReturn(List.of(1L, 2L));

        List<Long> resultat = perimetreFormateurService.cohorteIdsDuFormateur(formateur);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> formationsDemandees = ArgumentCaptor.forClass(List.class);
        verify(promotionService).promotionIdsDesFormations(formationsDemandees.capture());
        assertThat(formationsDemandees.getValue()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(resultat).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("Un formateur sans cours ne voit aucune promotion")
    void promotionsDuFormateur_sansCours() {
        when(coursService.getByFormateurId(anyLong())).thenReturn(List.of());

        assertThat(perimetreFormateurService.cohorteIdsDuFormateur(formateur(10L))).isEmpty();
    }

    /** Cas du cours cree hors de tout programme : il ne donne acces a personne. */
    @Test
    @DisplayName("Un cours hors programme ne donne acces a aucun etudiant")
    void promotionsDuFormateur_coursSansFormation() {
        when(coursService.getByFormateurId(anyLong())).thenReturn(List.of(cours()));

        assertThat(perimetreFormateurService.cohorteIdsDuFormateur(formateur(10L))).isEmpty();
    }

    // ---------- cohorteIdsVisiblesPour ----------

    @Test
    @DisplayName("Un formateur voit les promotions des programmes de ses cours")
    void visiblesPour_formateur() {
        User user = new User();
        user.setId(100L);
        when(formateurRepository.findByUserId(100L)).thenReturn(Optional.of(formateur(10L)));
        when(coursService.getByFormateurId(10L)).thenReturn(List.of(cours(promotion(5L))));
        when(promotionService.promotionIdsDesFormations(anyList())).thenReturn(List.of(5L));

        assertThat(perimetreFormateurService.cohorteIdsVisiblesPour(user)).containsExactly(5L);
    }

    @Test
    @DisplayName("Un etudiant ne voit que sa propre promotion")
    void visiblesPour_etudiant() {
        User user = new User();
        user.setId(200L);
        when(formateurRepository.findByUserId(200L)).thenReturn(Optional.empty());
        when(etudiantRepository.findByUserId(200L)).thenReturn(Optional.of(etudiant(promotion(7L))));

        assertThat(perimetreFormateurService.cohorteIdsVisiblesPour(user)).containsExactly(7L);
    }

    @Test
    @DisplayName("Un etudiant sans promotion affectee ne voit rien")
    void visiblesPour_etudiantSansPromotion() {
        User user = new User();
        user.setId(200L);
        when(formateurRepository.findByUserId(200L)).thenReturn(Optional.empty());
        when(etudiantRepository.findByUserId(200L)).thenReturn(Optional.of(etudiant(null)));

        assertThat(perimetreFormateurService.cohorteIdsVisiblesPour(user)).isEmpty();
    }

    @Test
    @DisplayName("Un utilisateur sans profil formateur ni etudiant ne voit rien")
    void visiblesPour_sansProfil() {
        User user = new User();
        user.setId(300L);
        when(formateurRepository.findByUserId(300L)).thenReturn(Optional.empty());
        when(etudiantRepository.findByUserId(300L)).thenReturn(Optional.empty());

        assertThat(perimetreFormateurService.cohorteIdsVisiblesPour(user)).isEmpty();
    }

    // ---------- estFormateurDuCours ----------

    @Test
    @DisplayName("Le formateur rattache au cours peut le modifier")
    void estFormateurDuCours_rattache() {
        Formateur formateur = formateur(10L);
        Cours cours = cours();
        cours.getFormateurs().add(formateur(10L)); // meme id, instance differente

        assertThat(perimetreFormateurService.estFormateurDuCours(formateur, cours)).isTrue();
    }

    @Test
    @DisplayName("Un formateur etranger au cours ne peut pas le modifier")
    void estFormateurDuCours_etranger() {
        Cours cours = cours();
        cours.getFormateurs().add(formateur(99L));

        assertThat(perimetreFormateurService.estFormateurDuCours(formateur(10L), cours)).isFalse();
    }

    @Test
    @DisplayName("Un cours sans formateur n'est modifiable par personne")
    void estFormateurDuCours_aucunFormateur() {
        assertThat(perimetreFormateurService.estFormateurDuCours(formateur(10L), cours())).isFalse();
    }

    // ---------- etudiantSuitLeCours ----------

    @Test
    @DisplayName("Un etudiant dont le programme porte le cours le suit bien")
    void etudiantSuitLeCours_memeProgramme() {
        Promotion visee = promotion(2L);
        Cours cours = cours(promotion(1L), visee);

        assertThat(perimetreFormateurService.etudiantSuitLeCours(etudiant(visee), cours)).isTrue();
    }

    @Test
    @DisplayName("Un etudiant d'un autre programme ne suit pas le cours")
    void etudiantSuitLeCours_autreProgramme() {
        Cours cours = cours(promotion(1L));

        assertThat(perimetreFormateurService.etudiantSuitLeCours(etudiant(promotion(2L)), cours)).isFalse();
    }

    @Test
    @DisplayName("Un etudiant sans promotion ne suit aucun cours")
    void etudiantSuitLeCours_sansPromotion() {
        Cours cours = cours(promotion(1L));

        assertThat(perimetreFormateurService.etudiantSuitLeCours(etudiant(null), cours)).isFalse();
    }

    /**
     * Une promotion non rattachee a un programme ne donne acces a rien : c'est le cas
     * d'une promotion creee sans formation, qu'il vaut mieux voir vide que trop large.
     */
    @Test
    @DisplayName("Un etudiant dont la promotion n'a pas de programme ne suit aucun cours")
    void etudiantSuitLeCours_promotionSansProgramme() {
        Promotion orpheline = promotion(3L);
        orpheline.setFormation(null);

        assertThat(perimetreFormateurService.etudiantSuitLeCours(etudiant(orpheline), cours(promotion(1L))))
                .isFalse();
    }

    // ---------- Publication programmee des ressources ----------

    private SupportCours support(Cours cours, LocalDate datePublication) {
        SupportCours support = new SupportCours();
        support.setCours(cours);
        support.setDatePublication(datePublication);
        return support;
    }

    @Test
    @DisplayName("Une ressource datee dans le futur est consideree comme programmee")
    void supportEstProgramme_dateFuture() {
        SupportCours support = support(cours(), LocalDate.now().plusDays(3));

        assertThat(perimetreFormateurService.supportEstProgramme(support)).isTrue();
    }

    @Test
    @DisplayName("Une ressource datee d'aujourd'hui est deja publiee (pas programmee)")
    void supportEstProgramme_aujourdhui() {
        SupportCours support = support(cours(), LocalDate.now());

        assertThat(perimetreFormateurService.supportEstProgramme(support)).isFalse();
    }

    @Test
    @DisplayName("Une ressource sans date est consideree comme publiee")
    void supportEstProgramme_sansDate() {
        assertThat(perimetreFormateurService.supportEstProgramme(support(cours(), null))).isFalse();
    }

    @Test
    @DisplayName("Un etudiant ne voit pas une ressource encore programmee")
    void peutVoirSupport_etudiantEtRessourceProgrammee() {
        SupportCours support = support(cours(), LocalDate.now().plusDays(3));

        // formateurConnecte = null represente un etudiant
        assertThat(perimetreFormateurService.peutVoirSupport(support, null)).isFalse();
    }

    @Test
    @DisplayName("Un etudiant voit une ressource dont la date est atteinte")
    void peutVoirSupport_etudiantEtRessourcePubliee() {
        SupportCours support = support(cours(), LocalDate.now().minusDays(1));

        assertThat(perimetreFormateurService.peutVoirSupport(support, null)).isTrue();
    }

    @Test
    @DisplayName("Le formateur du cours voit sa ressource meme avant la date prevue")
    void peutVoirSupport_formateurDuCours() {
        Cours cours = cours();
        cours.getFormateurs().add(formateur(10L));
        SupportCours support = support(cours, LocalDate.now().plusDays(3));

        assertThat(perimetreFormateurService.peutVoirSupport(support, formateur(10L))).isTrue();
    }

    @Test
    @DisplayName("Un formateur etranger au cours ne voit pas une ressource programmee")
    void peutVoirSupport_formateurEtranger() {
        Cours cours = cours();
        cours.getFormateurs().add(formateur(99L));
        SupportCours support = support(cours, LocalDate.now().plusDays(3));

        assertThat(perimetreFormateurService.peutVoirSupport(support, formateur(10L))).isFalse();
    }

    @Test
    @DisplayName("Une ressource sans cours rattache ne fait pas planter le controle")
    void peutVoirSupport_sansCours() {
        SupportCours support = support(null, LocalDate.now().plusDays(3));

        assertThat(perimetreFormateurService.peutVoirSupport(support, formateur(10L))).isFalse();
    }

    // ---------- Cloisonnement d'un support par cours/promotion ----------

    @Test
    @DisplayName("Un support sans cours (ressource generale) est toujours accessible")
    void coursDuSupportEstAccessible_sansCours() {
        SupportCours support = support(null, null);

        assertThat(perimetreFormateurService.coursDuSupportEstAccessible(support, List.of(), null)).isTrue();
    }

    @Test
    @DisplayName("Un etudiant de la bonne promotion accede au support de son cours")
    void coursDuSupportEstAccessible_etudiantConcerne() {
        Cours cours = cours(promotion(1L));
        SupportCours support = support(cours, null);
        when(promotionService.desFormationsDuCours(cours)).thenReturn(List.of(promotion(1L)));

        assertThat(perimetreFormateurService.coursDuSupportEstAccessible(support, List.of(1L), null)).isTrue();
    }

    @Test
    @DisplayName("Un etudiant d'une autre promotion ne voit pas le support d'un cours qui ne le concerne pas")
    void coursDuSupportEstAccessible_etudiantEtranger() {
        Cours cours = cours(promotion(1L));
        SupportCours support = support(cours, null);
        when(promotionService.desFormationsDuCours(cours)).thenReturn(List.of(promotion(1L)));

        assertThat(perimetreFormateurService.coursDuSupportEstAccessible(support, List.of(2L), null)).isFalse();
    }

    @Test
    @DisplayName("Le formateur du cours accede a son support meme sans promotion commune calculee")
    void coursDuSupportEstAccessible_formateurDuCours() {
        Cours cours = cours(promotion(1L));
        cours.getFormateurs().add(formateur(10L));
        SupportCours support = support(cours, null);

        assertThat(perimetreFormateurService.coursDuSupportEstAccessible(support, List.of(), formateur(10L))).isTrue();
    }

    @Test
    @DisplayName("Un formateur etranger au cours, sans promotion commune, n'y accede pas")
    void coursDuSupportEstAccessible_formateurEtranger() {
        Cours cours = cours(promotion(1L));
        cours.getFormateurs().add(formateur(99L));
        SupportCours support = support(cours, null);
        when(promotionService.desFormationsDuCours(cours)).thenReturn(List.of(promotion(1L)));

        assertThat(perimetreFormateurService.coursDuSupportEstAccessible(support, List.of(), formateur(10L)))
                .isFalse();
    }
}
