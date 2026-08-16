package inpg.sn.lms.espace_etudiant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.entity.Formateur;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.auth.repository.FormateurRepository;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.Promotion;
import inpg.sn.lms.espace_etudiant.entity.SupportCours;

/**
 * Ces tests protegent le cloisonnement des donnees entre formateurs : un
 * formateur ne doit voir que les etudiants des promotions rattachees a SES cours.
 * Une regression sur ces regles rouvrirait une fuite de donnees, d'ou leur
 * presence ici.
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

    @InjectMocks
    private PerimetreFormateurService perimetreFormateurService;

    // ---------- Outils de construction d'objets de test ----------

    private Promotion promotion(Long id) {
        Promotion promotion = new Promotion();
        setId(promotion, id);
        return promotion;
    }

    private Formateur formateur(Long id) {
        Formateur formateur = new Formateur();
        formateur.setId(id);
        return formateur;
    }

    private Cours cours(Promotion... promotions) {
        Cours cours = new Cours();
        cours.getPromotions().addAll(List.of(promotions));
        return cours;
    }

    private Etudiant etudiant(Promotion promotion) {
        Etudiant etudiant = new Etudiant();
        etudiant.setPromotion(promotion);
        return etudiant;
    }

    /** Promotion n'expose pas de setId (id genere par la base) : on le force par reflexion. */
    private void setId(Promotion promotion, Long id) {
        try {
            var champ = Promotion.class.getDeclaredField("id");
            champ.setAccessible(true);
            champ.set(promotion, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Impossible de fixer l'id de la promotion de test", e);
        }
    }

    // ---------- cohorteIdsDuFormateur ----------

    @Test
    @DisplayName("Retourne les promotions de tous les cours du formateur, sans doublon")
    void promotionsDuFormateur_dedoublonne() {
        Promotion dataScience = promotion(1L);
        Promotion finance = promotion(2L);
        Formateur formateur = formateur(10L);
        // Deux cours partagent la promotion Data Science : elle ne doit apparaitre qu'une fois.
        when(coursService.getByFormateurId(10L))
                .thenReturn(List.of(cours(dataScience), cours(dataScience, finance)));

        List<Long> resultat = perimetreFormateurService.cohorteIdsDuFormateur(formateur);

        assertThat(resultat).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("Un formateur sans cours ne voit aucune promotion")
    void promotionsDuFormateur_sansCours() {
        when(coursService.getByFormateurId(anyLong())).thenReturn(List.of());

        assertThat(perimetreFormateurService.cohorteIdsDuFormateur(formateur(10L))).isEmpty();
    }

    @Test
    @DisplayName("Un cours sans promotion rattachee ne donne acces a aucun etudiant")
    void promotionsDuFormateur_coursSansPromotion() {
        when(coursService.getByFormateurId(anyLong())).thenReturn(List.of(cours()));

        assertThat(perimetreFormateurService.cohorteIdsDuFormateur(formateur(10L))).isEmpty();
    }

    // ---------- cohorteIdsVisiblesPour ----------

    @Test
    @DisplayName("Un formateur voit les promotions de ses cours")
    void visiblesPour_formateur() {
        User user = new User();
        user.setId(100L);
        when(formateurRepository.findByUserId(100L)).thenReturn(Optional.of(formateur(10L)));
        when(coursService.getByFormateurId(10L)).thenReturn(List.of(cours(promotion(5L))));

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
    @DisplayName("Un etudiant d'une promotion rattachee suit bien le cours")
    void etudiantSuitLeCours_memePromotion() {
        Cours cours = cours(promotion(1L), promotion(2L));

        assertThat(perimetreFormateurService.etudiantSuitLeCours(etudiant(promotion(2L)), cours)).isTrue();
    }

    @Test
    @DisplayName("Un etudiant d'une autre promotion ne suit pas le cours")
    void etudiantSuitLeCours_autrePromotion() {
        Cours cours = cours(promotion(1L));

        assertThat(perimetreFormateurService.etudiantSuitLeCours(etudiant(promotion(2L)), cours)).isFalse();
    }

    @Test
    @DisplayName("Un etudiant sans promotion ne suit aucun cours")
    void etudiantSuitLeCours_sansPromotion() {
        Cours cours = cours(promotion(1L));

        assertThat(perimetreFormateurService.etudiantSuitLeCours(etudiant(null), cours)).isFalse();
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
        SupportCours support = support(cours(promotion(1L)), null);

        assertThat(perimetreFormateurService.coursDuSupportEstAccessible(support, List.of(1L), null)).isTrue();
    }

    @Test
    @DisplayName("Un etudiant d'une autre promotion ne voit pas le support d'un cours qui ne le concerne pas")
    void coursDuSupportEstAccessible_etudiantEtranger() {
        SupportCours support = support(cours(promotion(1L)), null);

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

        assertThat(perimetreFormateurService.coursDuSupportEstAccessible(support, List.of(), formateur(10L))).isFalse();
    }
}
