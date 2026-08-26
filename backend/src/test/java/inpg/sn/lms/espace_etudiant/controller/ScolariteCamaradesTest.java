package inpg.sn.lms.espace_etudiant.controller;

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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.espace_etudiant.dto.CamaradeDto;
import inpg.sn.lms.espace_etudiant.entity.Promotion;
import inpg.sn.lms.espace_etudiant.repository.CoursRepository;
import inpg.sn.lms.espace_etudiant.repository.ProgressionSupportRepository;
import inpg.sn.lms.espace_etudiant.repository.RetourEvaluationRepository;
import inpg.sn.lms.espace_etudiant.repository.SeanceRepository;
import inpg.sn.lms.espace_etudiant.service.HabilitationCoursService;
import inpg.sn.lms.espace_etudiant.service.MatriculeService;
import inpg.sn.lms.espace_etudiant.service.NoteModuleService;
import inpg.sn.lms.espace_etudiant.service.ReleveDelivranceService;
import inpg.sn.lms.espace_etudiant.service.ReleveNotesService;
import inpg.sn.lms.espace_etudiant.service.SupportService;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;

/**
 * {@code GET /api/espace-etudiant/scolarite/ma-promotion/etudiants} (roster de
 * promotion, souhait utilisateur adjacent au TDR §B).
 *
 * <p>Le risque à couvrir : la promotion utilisée pour la requête doit toujours
 * être celle de l'étudiant connecté, jamais une valeur qu'un client pourrait
 * fournir — il n'y a d'ailleurs aucun paramètre d'entrée à ce endpoint, la
 * seule source possible est {@code Etudiant.getPromotion()} côté serveur.
 */
@ExtendWith(MockitoExtension.class)
class ScolariteCamaradesTest {

    @Mock
    private UtilisateurCourantService utilisateurCourant;
    @Mock
    private MatriculeService matriculeService;
    @Mock
    private CoursRepository coursRepository;
    @Mock
    private SeanceRepository seanceRepository;
    @Mock
    private SupportService supportService;
    @Mock
    private ProgressionSupportRepository progressionRepository;
    @Mock
    private RetourEvaluationRepository retourRepository;
    @Mock
    private EtudiantRepository etudiantRepository;
    @Mock
    private HabilitationCoursService habilitation;
    @Mock
    private NoteModuleService noteModuleService;
    @Mock
    private ReleveNotesService releveNotesService;
    @Mock
    private ReleveDelivranceService releveDelivranceService;
    @Mock
    private Authentication authentication;

    private ScolariteController controller;

    @BeforeEach
    void setUp() {
        controller = new ScolariteController(utilisateurCourant, matriculeService, coursRepository, seanceRepository,
                supportService, progressionRepository, retourRepository, etudiantRepository, habilitation,
                noteModuleService, releveNotesService, releveDelivranceService);
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
    @DisplayName("Sans profil étudiant : 404")
    void sans_profil_404() {
        lenient().when(utilisateurCourant.etudiant(authentication)).thenReturn(null);

        ResponseEntity<?> reponse = controller.camaradesDePromotion(authentication);

        assertThat(reponse.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    @DisplayName("Sans promotion : liste vide, jamais d'erreur")
    void sans_promotion_liste_vide() {
        Etudiant etudiant = new Etudiant();
        setId(etudiant, 1L);
        when(utilisateurCourant.etudiant(authentication)).thenReturn(etudiant);

        ResponseEntity<?> reponse = controller.camaradesDePromotion(authentication);

        assertThat(reponse.getBody()).isEqualTo(List.of());
    }

    @Test
    @DisplayName("Renvoie les camarades de la promotion de l'étudiant connecté, jamais une autre")
    void renvoie_les_camarades_de_sa_propre_promotion() {
        Etudiant etudiant = new Etudiant();
        setId(etudiant, 1L);
        Promotion promotion = new Promotion();
        setId(promotion, 42L);
        etudiant.setPromotion(promotion);
        when(utilisateurCourant.etudiant(authentication)).thenReturn(etudiant);

        Etudiant camarade = new Etudiant(null, "Diallo", "Awa", null);
        setId(camarade, 2L);
        camarade.setMatricule("INPG-2026-002");
        when(etudiantRepository.findByPromotionId(42L)).thenReturn(List.of(camarade));

        ResponseEntity<?> reponse = controller.camaradesDePromotion(authentication);

        @SuppressWarnings("unchecked")
        List<CamaradeDto> corps = (List<CamaradeDto>) reponse.getBody();
        assertThat(corps).hasSize(1);
        assertThat(corps.get(0).matricule()).isEqualTo("INPG-2026-002");
        // Ni email ni téléphone dans le DTO : seuls id/nom/matricule sont exposés.
        assertThat(CamaradeDto.class.getRecordComponents()).extracting("name")
                .containsExactlyInAnyOrder("id", "nom", "matricule");
    }
}
