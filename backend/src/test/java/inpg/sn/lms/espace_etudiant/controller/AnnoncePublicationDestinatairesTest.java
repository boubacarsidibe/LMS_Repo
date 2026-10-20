package inpg.sn.lms.espace_etudiant.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.espace_etudiant.dto.AnnonceCreateRequest;
import inpg.sn.lms.espace_etudiant.entity.Annonce;
import inpg.sn.lms.espace_etudiant.entity.Promotion;
import inpg.sn.lms.espace_etudiant.repository.AnnonceLectureRepository;
import inpg.sn.lms.espace_etudiant.repository.AnnonceRepository;
import inpg.sn.lms.espace_etudiant.repository.PromotionRepository;
import inpg.sn.lms.espace_etudiant.service.PerimetreFormateurService;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;

/**
 * Le formulaire du formateur propose des « destinataires » (les promotions qu'il enseigne) et envoyait leur liste sous la
 * clé {@code cohorteIds} ; le contrat serveur n'attendait que {@code promotionId}. Le choix était ignoré sans la moindre
 * erreur : toute annonce partait à « toutes les promotions ».
 */
@ExtendWith(MockitoExtension.class)
class AnnoncePublicationDestinatairesTest {

    @Mock private AnnonceRepository annonceRepository;
    @Mock private AnnonceLectureRepository lectureRepository;
    @Mock private PromotionRepository promotionRepository;
    @Mock private UtilisateurCourantService utilisateurCourant;
    @Mock private PerimetreFormateurService perimetreFormateur;

    private AnnonceController controller;
    private Authentication authentication;
    private User formateur;

    @BeforeEach
    void preparer() {
        controller = new AnnonceController(annonceRepository, lectureRepository, promotionRepository, utilisateurCourant,
                perimetreFormateur);
        authentication = new UsernamePasswordAuthenticationToken("moussa", "n/a", List.of());
        formateur = new User();
        formateur.setId(2L);
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(formateur);
        org.mockito.Mockito.lenient().when(annonceRepository.save(any(Annonce.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Promotion promotion(long id, String libelle) {
        Promotion p = new Promotion();
        ReflectionTestUtils.setField(p, "id", id);
        p.setLibelle(libelle);
        return p;
    }

    private AnnonceCreateRequest requete(List<Long> promotions) {
        return new AnnonceCreateRequest("Révision", "Séance en ligne", null, null, promotions, false);
    }

    @Test
    @SuppressWarnings("unchecked")
    @org.junit.jupiter.api.DisplayName("Une annonce est créée pour chaque promotion cochée — pas une seule annonce générale")
    void une_annonce_par_promotion_designee() {
        when(perimetreFormateur.cohorteIdsVisiblesPour(formateur)).thenReturn(List.of(4L, 5L));
        when(promotionRepository.findById(4L)).thenReturn(Optional.of(promotion(4L, "Master Pétrole & Gaz")));
        when(promotionRepository.findById(5L)).thenReturn(Optional.of(promotion(5L, "Master Finance")));

        ResponseEntity<?> reponse = controller.publier(requete(List.of(4L, 5L)), authentication);

        assertThat(reponse.getStatusCode().value()).isEqualTo(201);
        ArgumentCaptor<Annonce> captees = ArgumentCaptor.forClass(Annonce.class);
        verify(annonceRepository, times(2)).save(captees.capture());
        assertThat(captees.getAllValues()).extracting(a -> a.getPromotion().getLibelle())
                .containsExactly("Master Pétrole & Gaz", "Master Finance");
        assertThat((List<?>) reponse.getBody()).hasSize(2);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Sans destinataire, l'annonce est générale (toutes les promotions)")
    void sans_destinataire_annonce_generale() {
        ResponseEntity<?> reponse = controller.publier(requete(List.of()), authentication);

        assertThat(reponse.getStatusCode().value()).isEqualTo(201);
        ArgumentCaptor<Annonce> captee = ArgumentCaptor.forClass(Annonce.class);
        verify(annonceRepository).save(captee.capture());
        assertThat(captee.getValue().getPromotion()).isNull();
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Une promotion qui n'est pas celle de ses cours est refusée, et rien n'est enregistré")
    void promotion_hors_perimetre_refusee_sans_rien_ecrire() {
        when(perimetreFormateur.cohorteIdsVisiblesPour(formateur)).thenReturn(List.of(4L));
        when(promotionRepository.findById(4L)).thenReturn(Optional.of(promotion(4L, "Master Pétrole & Gaz")));
        when(promotionRepository.findById(9L)).thenReturn(Optional.of(promotion(9L, "Promotion d'un collègue")));

        // La première promotion est valide : sans contrôle préalable de TOUTES, elle serait déjà enregistrée.
        ResponseEntity<?> reponse = controller.publier(requete(List.of(4L, 9L)), authentication);

        assertThat(reponse.getStatusCode().value()).isEqualTo(403);
        verify(annonceRepository, never()).save(any(Annonce.class));
    }

    @Test
    void promotion_inexistante_refusee_sans_rien_ecrire() {
        when(perimetreFormateur.cohorteIdsVisiblesPour(formateur)).thenReturn(List.of(4L));
        when(promotionRepository.findById(404L)).thenReturn(Optional.empty());

        ResponseEntity<?> reponse = controller.publier(requete(List.of(404L)), authentication);

        assertThat(reponse.getStatusCode().value()).isEqualTo(400);
        verify(annonceRepository, never()).save(any(Annonce.class));
    }
}
