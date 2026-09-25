package inpg.sn.lms.espace_etudiant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.entity.Formateur;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.ForumSujet;
import inpg.sn.lms.espace_etudiant.entity.PorteeForumSujet;
import inpg.sn.lms.espace_etudiant.entity.Promotion;
import inpg.sn.lms.espace_etudiant.repository.ForumSujetRepository;

/**
 * Visibilité des sujets du forum de l'espace étudiant.
 *
 * <p>Trois risques justifient ces tests.
 *
 * <p><strong>Fuite entre espaces.</strong> Un sujet de l'espace alumni
 * ({@code portee = ALUMNI}) ne doit jamais apparaître dans la liste d'un étudiant
 * ou d'un formateur, ni être atteignable en devinant son identifiant.
 *
 * <p><strong>Énumération non filtrée.</strong> {@code EspaceEtudiantController}
 * appelait {@code findAll()} sans aucun filtre — un étudiant pouvait lister tous
 * les sujets de tous les cours.
 *
 * <p><strong>Cloisonnement formateur manquant.</strong> Une version antérieure de
 * cette classe donnait à tout formateur une vue sur l'intégralité du forum
 * étudiant (« l'ensemble des salons »), y compris à un formateur sans le moindre
 * cours assigné dans {@code cours_formateur} — à rebours de la règle appliquée
 * partout ailleurs dans l'espace formateur (cf. CLAUDE.md §3). Un formateur ne
 * doit voir que les sujets généraux, ceux de ses propres cours, et ceux des
 * promotions desservies par ces cours.
 */
@ExtendWith(MockitoExtension.class)
class ForumVisibiliteServiceTest {

    @Mock
    private ForumSujetRepository sujetRepository;
    @Mock
    private UtilisateurCourantService utilisateurCourant;
    @Mock
    private HabilitationCoursService habilitation;
    @Mock
    private PerimetreFormateurService perimetreFormateur;
    @Mock
    private Authentication authentication;

    private ForumVisibiliteService service;

    @BeforeEach
    void setUp() {
        service = new ForumVisibiliteService(sujetRepository, utilisateurCourant, habilitation, perimetreFormateur);
    }

    private Etudiant etudiant() {
        Etudiant etudiant = new Etudiant();
        setId(etudiant, 7L);
        return etudiant;
    }

    private Formateur formateur() {
        Formateur formateur = new Formateur();
        formateur.setId(11L);
        return formateur;
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

    @Test
    @DisplayName("Un formateur sans cours se replie sur les sujets généraux, jamais une vue large")
    void formateur_sans_cours_voit_les_generaux() {
        Formateur formateur = formateur();
        when(utilisateurCourant.etudiant(authentication)).thenReturn(null);
        when(utilisateurCourant.formateur(authentication)).thenReturn(formateur);
        when(perimetreFormateur.coursIdsDuFormateur(formateur)).thenReturn(List.of());
        List<ForumSujet> attendus = List.of(new ForumSujet());
        when(sujetRepository.findGeneraux(null)).thenReturn(attendus);

        assertThat(service.sujetsVisibles(authentication)).isSameAs(attendus);
    }

    @Test
    @DisplayName("Un formateur avec des cours ne voit que ses cours et les promotions qu'ils desservent")
    void formateur_avec_cours_voit_son_perimetre() {
        Formateur formateur = formateur();
        when(utilisateurCourant.etudiant(authentication)).thenReturn(null);
        when(utilisateurCourant.formateur(authentication)).thenReturn(formateur);
        when(perimetreFormateur.coursIdsDuFormateur(formateur)).thenReturn(List.of(1L, 2L));
        when(perimetreFormateur.cohorteIdsDuFormateur(formateur)).thenReturn(List.of(5L));
        List<ForumSujet> attendus = List.of(new ForumSujet());
        when(sujetRepository.findVisiblesPourFormateur(List.of(1L, 2L), List.of(5L))).thenReturn(attendus);

        assertThat(service.sujetsVisibles(authentication)).isSameAs(attendus);
    }

    @Test
    @DisplayName("Un connecté sans profil étudiant ni formateur ne voit aucun sujet")
    void sans_profil_ne_voit_rien() {
        when(utilisateurCourant.etudiant(authentication)).thenReturn(null);
        when(utilisateurCourant.formateur(authentication)).thenReturn(null);

        assertThat(service.sujetsVisibles(authentication)).isEmpty();
    }

    @Test
    @DisplayName("Un étudiant sans cours autorisé ni promotion se replie sur les sujets généraux")
    void etudiant_sans_cours_voit_les_generaux() {
        Etudiant etudiant = etudiant();
        when(utilisateurCourant.etudiant(authentication)).thenReturn(etudiant);
        when(habilitation.idsCoursAutorises(etudiant)).thenReturn(List.of());
        List<ForumSujet> attendus = List.of(new ForumSujet());
        when(sujetRepository.findGeneraux(null)).thenReturn(attendus);

        assertThat(service.sujetsVisibles(authentication)).isSameAs(attendus);
    }

    @Test
    @DisplayName("Un étudiant sans cours mais avec une promotion voit aussi les sujets de sa promotion")
    void etudiant_sans_cours_mais_avec_promotion() {
        Etudiant etudiant = etudiant();
        Promotion promotion = new Promotion();
        setId(promotion, 3L);
        etudiant.setPromotion(promotion);
        when(utilisateurCourant.etudiant(authentication)).thenReturn(etudiant);
        when(habilitation.idsCoursAutorises(etudiant)).thenReturn(List.of());
        List<ForumSujet> attendus = List.of(new ForumSujet());
        when(sujetRepository.findGeneraux(3L)).thenReturn(attendus);

        assertThat(service.sujetsVisibles(authentication)).isSameAs(attendus);
    }

    @Test
    @DisplayName("Un étudiant avec des cours transmet aussi sa promotion à la requête de visibilité")
    void etudiant_avec_cours_transmet_sa_promotion() {
        Etudiant etudiant = etudiant();
        Promotion promotion = new Promotion();
        setId(promotion, 3L);
        etudiant.setPromotion(promotion);
        when(utilisateurCourant.etudiant(authentication)).thenReturn(etudiant);
        when(habilitation.idsCoursAutorises(etudiant)).thenReturn(List.of(1L, 2L));
        List<ForumSujet> attendus = List.of(new ForumSujet());
        when(sujetRepository.findVisiblesPour(List.of(1L, 2L), 3L)).thenReturn(attendus);

        assertThat(service.sujetsVisibles(authentication)).isSameAs(attendus);
    }

    @Test
    @DisplayName("Un sujet de l'espace alumni n'est jamais visible d'ici, même sans cours")
    void sujet_alumni_jamais_visible() {
        ForumSujet sujetAlumni = new ForumSujet("Retrouvailles", null, PorteeForumSujet.ALUMNI);

        assertThat(service.estVisible(sujetAlumni, authentication)).isFalse();
    }

    @Test
    @DisplayName("Un sujet général de l'espace étudiant reste visible de tous")
    void sujet_general_etudiant_visible() {
        ForumSujet sujetGeneral = new ForumSujet("Question logistique", null, PorteeForumSujet.ETUDIANT);

        assertThat(service.estVisible(sujetGeneral, authentication)).isTrue();
    }

    @Test
    @DisplayName("Un sujet de cours n'est visible que si l'étudiant y est habilité")
    void sujet_de_cours_verifie_l_habilitation() {
        Etudiant etudiant = etudiant();
        Cours cours = new Cours();
        setId(cours, 9L);
        ForumSujet sujet = new ForumSujet("Devoir", cours, null);

        lenient().when(utilisateurCourant.etudiant(authentication)).thenReturn(etudiant);
        when(habilitation.estAutorise(9L, etudiant)).thenReturn(false);

        assertThat(service.estVisible(sujet, authentication)).isFalse();
    }

    @Test
    @DisplayName("Un sujet de promotion est visible d'un étudiant de cette promotion")
    void sujet_de_promotion_visible_pour_un_membre() {
        Etudiant etudiant = etudiant();
        Promotion promotion = new Promotion();
        setId(promotion, 3L);
        etudiant.setPromotion(promotion);
        ForumSujet sujet = new ForumSujet("Sortie de promotion", promotion, null);

        when(utilisateurCourant.etudiant(authentication)).thenReturn(etudiant);

        assertThat(service.estVisible(sujet, authentication)).isTrue();
    }

    @Test
    @DisplayName("Un sujet de promotion n'est jamais visible d'un étudiant d'une autre promotion")
    void sujet_de_promotion_invisible_pour_un_tiers() {
        Etudiant etudiant = etudiant();
        Promotion maPromotion = new Promotion();
        setId(maPromotion, 5L);
        etudiant.setPromotion(maPromotion);
        Promotion autrePromotion = new Promotion();
        setId(autrePromotion, 3L);
        ForumSujet sujet = new ForumSujet("Sortie de promotion", autrePromotion, null);

        when(utilisateurCourant.etudiant(authentication)).thenReturn(etudiant);

        assertThat(service.estVisible(sujet, authentication)).isFalse();
    }

    @Test
    @DisplayName("Un sujet de cours est visible du formateur qui enseigne ce cours")
    void sujet_de_cours_visible_pour_son_formateur() {
        Formateur formateur = formateur();
        Cours cours = new Cours();
        setId(cours, 9L);
        ForumSujet sujet = new ForumSujet("Devoir", cours, null);

        when(utilisateurCourant.etudiant(authentication)).thenReturn(null);
        when(utilisateurCourant.formateur(authentication)).thenReturn(formateur);
        when(perimetreFormateur.estFormateurDuCours(formateur, cours)).thenReturn(true);

        assertThat(service.estVisible(sujet, authentication)).isTrue();
    }

    @Test
    @DisplayName("Un sujet de cours n'est jamais visible d'un formateur étranger à ce cours")
    void sujet_de_cours_invisible_pour_un_formateur_etranger() {
        Formateur formateur = formateur();
        Cours cours = new Cours();
        setId(cours, 9L);
        ForumSujet sujet = new ForumSujet("Devoir", cours, null);

        when(utilisateurCourant.etudiant(authentication)).thenReturn(null);
        when(utilisateurCourant.formateur(authentication)).thenReturn(formateur);
        when(perimetreFormateur.estFormateurDuCours(formateur, cours)).thenReturn(false);

        assertThat(service.estVisible(sujet, authentication)).isFalse();
    }

    @Test
    @DisplayName("Un sujet de promotion est visible du formateur dont un cours dessert cette promotion")
    void sujet_de_promotion_visible_pour_le_formateur_concerne() {
        Formateur formateur = formateur();
        Promotion promotion = new Promotion();
        setId(promotion, 3L);
        ForumSujet sujet = new ForumSujet("Sortie de promotion", promotion, null);

        when(utilisateurCourant.etudiant(authentication)).thenReturn(null);
        when(utilisateurCourant.formateur(authentication)).thenReturn(formateur);
        when(perimetreFormateur.cohorteIdsDuFormateur(formateur)).thenReturn(List.of(3L));

        assertThat(service.estVisible(sujet, authentication)).isTrue();
    }

    @Test
    @DisplayName("Un sujet de promotion n'est jamais visible d'un formateur dont aucun cours ne la dessert")
    void sujet_de_promotion_invisible_pour_un_formateur_etranger() {
        Formateur formateur = formateur();
        Promotion promotion = new Promotion();
        setId(promotion, 3L);
        ForumSujet sujet = new ForumSujet("Sortie de promotion", promotion, null);

        when(utilisateurCourant.etudiant(authentication)).thenReturn(null);
        when(utilisateurCourant.formateur(authentication)).thenReturn(formateur);
        when(perimetreFormateur.cohorteIdsDuFormateur(formateur)).thenReturn(List.of());

        assertThat(service.estVisible(sujet, authentication)).isFalse();
    }

    @Test
    @DisplayName("Un sujet de cours n'est jamais visible d'un connecté sans profil étudiant ni formateur")
    void sujet_de_cours_invisible_sans_profil() {
        Cours cours = new Cours();
        setId(cours, 9L);
        ForumSujet sujet = new ForumSujet("Devoir", cours, null);

        when(utilisateurCourant.etudiant(authentication)).thenReturn(null);
        when(utilisateurCourant.formateur(authentication)).thenReturn(null);

        assertThat(service.estVisible(sujet, authentication)).isFalse();
    }
}
