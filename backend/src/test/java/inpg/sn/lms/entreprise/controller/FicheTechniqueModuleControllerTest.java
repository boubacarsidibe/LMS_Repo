package inpg.sn.lms.entreprise.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import inpg.sn.lms.auth.entity.Entreprise;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.EntrepriseRepository;
import inpg.sn.lms.auth.repository.RoleRepository;
import inpg.sn.lms.entreprise.dto.FicheTechniqueModuleDto;
import inpg.sn.lms.entreprise.dto.FicheTechniqueModuleRequest;
import inpg.sn.lms.entreprise.entity.DemandeEntreprise;
import inpg.sn.lms.entreprise.entity.FicheTechniqueModule;
import inpg.sn.lms.entreprise.enums.EnumStatutDemandeEntreprise;
import inpg.sn.lms.entreprise.enums.EnumStatutFicheModule;
import inpg.sn.lms.entreprise.enums.EnumTypeDemandeEntreprise;
import inpg.sn.lms.entreprise.repository.DemandeEntrepriseRepository;
import inpg.sn.lms.entreprise.repository.FicheTechniqueModuleRepository;
import inpg.sn.lms.entreprise.repository.MessageEntrepriseRepository;
import inpg.sn.lms.entreprise.service.EntrepriseNotificationService;
import inpg.sn.lms.entreprise.service.FicheTechniqueModuleService;
import inpg.sn.lms.espace_etudiant.repository.FormationRepository;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;

/**
 * Cycle de vie de la Fiche Technique de Module : upsert et transmission côté
 * pédagogie, consultation et décision côté entreprise, et cloisonnement — une
 * entreprise ne doit jamais accéder à la fiche d'une demande qui n'est pas la
 * sienne. Le service {@link FicheTechniqueModuleService} est instancié
 * réellement (aucune dépendance externe) plutôt que mocké, pour exercer la
 * vraie règle de transmissibilité.
 */
@ExtendWith(MockitoExtension.class)
class FicheTechniqueModuleControllerTest {

    @Mock
    private DemandeEntrepriseRepository demandeRepository;
    @Mock
    private MessageEntrepriseRepository messageRepository;
    @Mock
    private EntrepriseRepository entrepriseRepository;
    @Mock
    private FormationRepository formationRepository;
    @Mock
    private FicheTechniqueModuleRepository ficheRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UtilisateurCourantService utilisateurCourant;
    @Mock
    private EntrepriseNotificationService notificationService;
    @Mock
    private Authentication authentication;

    private final FicheTechniqueModuleService ficheService = new FicheTechniqueModuleService();

    private EntrepriseController entrepriseController;
    private PedagogieEntrepriseController pedagogieController;

    private Entreprise entrepriseA;
    private DemandeEntreprise demandeA;

    @BeforeEach
    void setUp() {
        entrepriseController = new EntrepriseController(entrepriseRepository, demandeRepository, messageRepository,
                formationRepository, ficheRepository, ficheService, roleRepository, utilisateurCourant,
                notificationService);
        pedagogieController = new PedagogieEntrepriseController(demandeRepository, messageRepository,
                entrepriseRepository, ficheRepository, ficheService, notificationService, utilisateurCourant);

        User userA = new User();
        userA.setId(1L);
        entrepriseA = new Entreprise(userA, "Entreprise A", null, "Adresse A", "77000000");
        entrepriseA.setId(1L);
        demandeA = new DemandeEntreprise(entrepriseA, EnumTypeDemandeEntreprise.DEVIS_SESSION, null,
                "Besoin de formation sur mesure", 12, "Q1 2027");
        demandeA.setId(5L);
    }

    private FicheTechniqueModuleRequest requeteInstruisable() {
        return new FicheTechniqueModuleRequest(null, "Lubrification des machines tournantes", null, null, null,
                null, null, null, null, null, null, null, null, null, null, null,
                "Maîtriser les bases de la lubrification industrielle", null, null, null, null, null, null, null,
                null, null,
                List.of(new FicheTechniqueModuleRequest.JourRequest(1, "Théorie", 7, "Présentiel", "Salle")));
    }

    // ===================== Upsert (pédagogie) =====================

    @Test
    @DisplayName("Upsert sur une demande sans fiche : crée une fiche en BROUILLON")
    void enregistrerFiche_sans_fiche_existante_en_cree_une_en_brouillon() {
        when(demandeRepository.findById(5L)).thenReturn(Optional.of(demandeA));
        when(ficheRepository.findByDemandeId(5L)).thenReturn(Optional.empty());
        when(ficheRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> reponse = pedagogieController.enregistrerFiche(5L, requeteInstruisable());

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        FicheTechniqueModuleDto dto = (FicheTechniqueModuleDto) reponse.getBody();
        assertThat(dto.statut()).isEqualTo("BROUILLON");
        assertThat(dto.intitule()).isEqualTo("Lubrification des machines tournantes");
        assertThat(dto.jours()).hasSize(1);
    }

    @Test
    @DisplayName("Upsert sur une demande inexistante : 404")
    void enregistrerFiche_sur_demande_inexistante_est_refuse() {
        when(demandeRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<?> reponse = pedagogieController.enregistrerFiche(99L, requeteInstruisable());

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Upsert sur une fiche déjà acceptée : 409, plus modifiable")
    void enregistrerFiche_sur_fiche_acceptee_est_refuse() {
        FicheTechniqueModule fiche = new FicheTechniqueModule(demandeA);
        fiche.setStatut(EnumStatutFicheModule.ACCEPTEE);
        when(demandeRepository.findById(5L)).thenReturn(Optional.of(demandeA));
        when(ficheRepository.findByDemandeId(5L)).thenReturn(Optional.of(fiche));

        ResponseEntity<?> reponse = pedagogieController.enregistrerFiche(5L, requeteInstruisable());

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(ficheRepository, never()).save(any());
    }

    // ===================== Transmission (pédagogie) =====================

    @Test
    @DisplayName("Transmettre une fiche vide : 400, aucune notification")
    void transmettreFiche_sur_fiche_vide_est_refuse() {
        FicheTechniqueModule fiche = new FicheTechniqueModule(demandeA);
        when(ficheRepository.findByDemandeId(5L)).thenReturn(Optional.of(fiche));

        ResponseEntity<?> reponse = pedagogieController.transmettreFiche(5L);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(fiche.getStatut()).isEqualTo(EnumStatutFicheModule.BROUILLON);
        verify(notificationService, never()).notifierFicheTransmise(any(), any(), any());
    }

    @Test
    @DisplayName("Transmettre une fiche instruisable : TRANSMISE, entreprise notifiée, demande passe EN_COURS")
    void transmettreFiche_sur_fiche_instruisable_la_transmet() {
        FicheTechniqueModule fiche = new FicheTechniqueModule(demandeA);
        ficheService.appliquer(fiche, requeteInstruisable());
        when(ficheRepository.findByDemandeId(5L)).thenReturn(Optional.of(fiche));
        when(ficheRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(demandeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> reponse = pedagogieController.transmettreFiche(5L);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fiche.getStatut()).isEqualTo(EnumStatutFicheModule.TRANSMISE);
        assertThat(fiche.getDateTransmission()).isNotNull();
        assertThat(demandeA.getStatut()).isEqualTo(EnumStatutDemandeEntreprise.EN_COURS);
        verify(notificationService).notifierFicheTransmise(null, "Entreprise A", fiche.getIntitule());
    }

    // ===================== Consultation (entreprise) =====================

    @Test
    @DisplayName("Fiche en BROUILLON : invisible pour l'entreprise, 404")
    void fiche_en_brouillon_est_invisible_pour_l_entreprise() {
        FicheTechniqueModule fiche = new FicheTechniqueModule(demandeA);
        when(utilisateurCourant.entreprise(authentication)).thenReturn(entrepriseA);
        when(ficheRepository.findByDemandeId(5L)).thenReturn(Optional.of(fiche));

        ResponseEntity<?> reponse = entrepriseController.fiche(5L, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Fiche TRANSMISE : visible pour l'entreprise propriétaire")
    void fiche_transmise_est_visible_pour_sa_propre_entreprise() {
        FicheTechniqueModule fiche = new FicheTechniqueModule(demandeA);
        fiche.setStatut(EnumStatutFicheModule.TRANSMISE);
        when(utilisateurCourant.entreprise(authentication)).thenReturn(entrepriseA);
        when(ficheRepository.findByDemandeId(5L)).thenReturn(Optional.of(fiche));

        ResponseEntity<?> reponse = entrepriseController.fiche(5L, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Cloisonnement : une autre entreprise ne voit pas la fiche de la demande de l'entreprise A")
    void fiche_est_cloisonnee_a_l_entreprise_proprietaire() {
        FicheTechniqueModule fiche = new FicheTechniqueModule(demandeA);
        fiche.setStatut(EnumStatutFicheModule.TRANSMISE);
        User userB = new User();
        userB.setId(2L);
        Entreprise entrepriseB = new Entreprise(userB, "Entreprise B", null, "Adresse B", "78000000");
        entrepriseB.setId(2L);
        when(utilisateurCourant.entreprise(authentication)).thenReturn(entrepriseB);
        when(ficheRepository.findByDemandeId(5L)).thenReturn(Optional.of(fiche));

        ResponseEntity<?> reponse = entrepriseController.fiche(5L, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ===================== Décision (entreprise) =====================

    @Test
    @DisplayName("Accepter la fiche : ACCEPTEE, demande CLOTUREE, équipe notifiée")
    void accepterFiche_cloture_la_demande() {
        FicheTechniqueModule fiche = new FicheTechniqueModule(demandeA);
        fiche.setStatut(EnumStatutFicheModule.TRANSMISE);
        when(utilisateurCourant.entreprise(authentication)).thenReturn(entrepriseA);
        when(ficheRepository.findByDemandeId(5L)).thenReturn(Optional.of(fiche));
        when(ficheRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(demandeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> reponse = entrepriseController.accepterFiche(5L, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fiche.getStatut()).isEqualTo(EnumStatutFicheModule.ACCEPTEE);
        assertThat(demandeA.getStatut()).isEqualTo(EnumStatutDemandeEntreprise.CLOTUREE);
        verify(notificationService).notifierDecisionFiche("Entreprise A", true, null);
    }

    @Test
    @DisplayName("Demander des ajustements sans commentaire : 400, rien n'est enregistré")
    void demanderAjustements_sans_commentaire_est_refuse() {
        ResponseEntity<?> reponse = entrepriseController.demanderAjustements(5L, Map.of(), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(ficheRepository, never()).save(any());
    }

    @Test
    @DisplayName("Demander des ajustements avec commentaire : AJUSTEMENTS_DEMANDES, équipe notifiée")
    void demanderAjustements_avec_commentaire_est_enregistre() {
        FicheTechniqueModule fiche = new FicheTechniqueModule(demandeA);
        fiche.setStatut(EnumStatutFicheModule.TRANSMISE);
        when(utilisateurCourant.entreprise(authentication)).thenReturn(entrepriseA);
        when(ficheRepository.findByDemandeId(5L)).thenReturn(Optional.of(fiche));
        when(ficheRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> reponse = entrepriseController.demanderAjustements(5L,
                Map.of("commentaire", "Merci de préciser le planning"), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fiche.getStatut()).isEqualTo(EnumStatutFicheModule.AJUSTEMENTS_DEMANDES);
        assertThat(fiche.getCommentaireEntreprise()).isEqualTo("Merci de préciser le planning");
        verify(notificationService).notifierDecisionFiche("Entreprise A", false, "Merci de préciser le planning");
    }

    @Test
    @DisplayName("Accepter une fiche déjà acceptée : idempotent, 200 sans double notification")
    void accepterFiche_deja_acceptee_est_idempotent() {
        FicheTechniqueModule fiche = new FicheTechniqueModule(demandeA);
        fiche.setStatut(EnumStatutFicheModule.ACCEPTEE);
        when(utilisateurCourant.entreprise(authentication)).thenReturn(entrepriseA);
        when(ficheRepository.findByDemandeId(5L)).thenReturn(Optional.of(fiche));

        ResponseEntity<?> reponse = entrepriseController.accepterFiche(5L, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(notificationService, never()).notifierDecisionFiche(any(), anyBoolean(), any());
        verify(demandeRepository, never()).save(any());
    }
}
