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
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.ForumSujet;
import inpg.sn.lms.espace_etudiant.entity.PorteeForumSujet;
import inpg.sn.lms.espace_etudiant.repository.ForumSujetRepository;

/**
 * Visibilité des sujets du forum de l'espace étudiant.
 *
 * <p>Deux risques justifient ces tests, tous deux introduits par la migration V13
 * (portée du forum) ou déjà latents avant elle.
 *
 * <p><strong>Fuite entre espaces.</strong> Un sujet de l'espace alumni
 * ({@code portee = ALUMNI}) ne doit jamais apparaître dans la liste d'un étudiant
 * ou d'un formateur, ni être atteignable en devinant son identifiant.
 *
 * <p><strong>Énumération non filtrée.</strong> {@code EspaceEtudiantController}
 * appelait {@code findAll()} sans aucun filtre — un étudiant pouvait lister tous
 * les sujets de tous les cours. La branche « formateur » de cette classe doit
 * rester une vue large (« l'ensemble des salons de l'espace étudiant »), mais
 * jamais un {@code findAll()} brut qui inclurait l'espace alumni.
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
    private Authentication authentication;

    private ForumVisibiliteService service;

    @BeforeEach
    void setUp() {
        service = new ForumVisibiliteService(sujetRepository, utilisateurCourant, habilitation);
    }

    private Etudiant etudiant() {
        Etudiant etudiant = new Etudiant();
        setId(etudiant, 7L);
        return etudiant;
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
    @DisplayName("Un formateur voit la vue filtrée par portée, jamais un findAll() brut")
    void formateur_voit_la_vue_filtree_par_portee() {
        when(utilisateurCourant.etudiant(authentication)).thenReturn(null);
        List<ForumSujet> attendus = List.of(new ForumSujet());
        when(sujetRepository.findVisiblesEtudiantFormateur()).thenReturn(attendus);

        List<ForumSujet> visibles = service.sujetsVisibles(authentication);

        assertThat(visibles).isSameAs(attendus);
    }

    @Test
    @DisplayName("Un étudiant sans cours autorisé se replie sur les sujets généraux de l'espace étudiant")
    void etudiant_sans_cours_voit_les_generaux() {
        Etudiant etudiant = etudiant();
        when(utilisateurCourant.etudiant(authentication)).thenReturn(etudiant);
        when(habilitation.idsCoursAutorises(etudiant)).thenReturn(List.of());
        List<ForumSujet> attendus = List.of(new ForumSujet());
        when(sujetRepository.findGeneraux()).thenReturn(attendus);

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
}
