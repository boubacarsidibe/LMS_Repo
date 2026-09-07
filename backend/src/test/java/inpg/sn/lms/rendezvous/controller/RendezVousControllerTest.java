package inpg.sn.lms.rendezvous.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import inpg.sn.lms.auth.entity.Role;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;
import inpg.sn.lms.rendezvous.dto.CreneauCreateRequest;
import inpg.sn.lms.rendezvous.dto.DemandeRendezVousRequest;
import inpg.sn.lms.rendezvous.dto.ReponsePropositionRequest;
import inpg.sn.lms.rendezvous.dto.ReponseRendezVousRequest;
import inpg.sn.lms.rendezvous.entity.CreneauDisponibilite;
import inpg.sn.lms.rendezvous.entity.RendezVous;
import inpg.sn.lms.rendezvous.enums.EnumStatutRendezVous;
import inpg.sn.lms.rendezvous.repository.CreneauDisponibiliteRepository;
import inpg.sn.lms.rendezvous.repository.RendezVousRepository;
import inpg.sn.lms.rendezvous.service.RendezVousService;

/**
 * {@code /api/rendez-vous} — gestion des rendez-vous (TDR §J).
 *
 * <p>Couvre la disponibilité d'un créneau (jamais double-réservable), l'auteur
 * de la demande toujours dérivé du jeton, et le double verrou de réponse : seul
 * le titulaire du créneau répond à une demande, seul le demandeur répond à une
 * contre-proposition.
 */
@ExtendWith(MockitoExtension.class)
class RendezVousControllerTest {

    @Mock
    private CreneauDisponibiliteRepository creneauRepository;
    @Mock
    private RendezVousRepository rendezVousRepository;
    @Mock
    private RendezVousService rendezVousService;
    @Mock
    private UtilisateurCourantService utilisateurCourant;
    @Mock
    private Authentication authentication;

    private RendezVousController controller;

    private User personnel(long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("p" + id);
        Role role = new Role();
        role.setCode("ROLE_FORMATEUR");
        user.setRoles(Set.of(role));
        return user;
    }

    private User demandeur(long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("d" + id);
        Role role = new Role();
        role.setCode("ROLE_ETUDIANT");
        user.setRoles(Set.of(role));
        return user;
    }

    @BeforeEach
    void setUp() {
        controller = new RendezVousController(creneauRepository, rendezVousRepository, rendezVousService,
                utilisateurCourant);
        lenient().when(rendezVousService.toCreneauDto(any())).thenReturn(null);
        lenient().when(rendezVousService.toRendezVousDto(any())).thenReturn(null);
    }

    @Test
    @DisplayName("refuse un créneau dont la fin précède le début")
    void creerCreneau_horaire_invalide_refuse() {
        ResponseEntity<?> reponse = controller.creerCreneau(
                new CreneauCreateRequest(LocalDateTime.now().plusDays(1), LocalDateTime.now()), authentication);
        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(creneauRepository, never()).save(any());
    }

    @Test
    @DisplayName("refuse un créneau dans le passé")
    void creerCreneau_passe_refuse() {
        ResponseEntity<?> reponse = controller.creerCreneau(
                new CreneauCreateRequest(LocalDateTime.now().minusDays(1), LocalDateTime.now()), authentication);
        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(creneauRepository, never()).save(any());
    }

    @Test
    @DisplayName("crée un créneau valide pour le personnel connecté, jamais pour un tiers transmis par le client")
    void creerCreneau_valide_enregistre_avec_personnel_du_jeton() {
        User moi = personnel(9L);
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(moi);
        when(creneauRepository.save(any(CreneauDisponibilite.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> reponse = controller.creerCreneau(
                new CreneauCreateRequest(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1)),
                authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ArgumentCaptor<CreneauDisponibilite> captureur = ArgumentCaptor.forClass(CreneauDisponibilite.class);
        verify(creneauRepository).save(captureur.capture());
        assertThat(captureur.getValue().getPersonnel()).isEqualTo(moi);
    }

    @Test
    @DisplayName("refuse de retirer un créneau déjà réservé")
    void supprimerCreneau_reserve_refuse() {
        User moi = personnel(9L);
        CreneauDisponibilite creneau = new CreneauDisponibilite(moi, LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1));
        creneau.setReserve(true);
        when(creneauRepository.findById(5L)).thenReturn(Optional.of(creneau));
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(moi);

        ResponseEntity<?> reponse = controller.supprimerCreneau(5L, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(creneauRepository, never()).delete(any());
    }

    @Test
    @DisplayName("refuse de retirer le créneau d'un tiers")
    void supprimerCreneau_tiers_refuse() {
        User proprietaire = personnel(9L);
        User autre = personnel(10L);
        CreneauDisponibilite creneau = new CreneauDisponibilite(proprietaire, LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1));
        when(creneauRepository.findById(5L)).thenReturn(Optional.of(creneau));
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(autre);

        ResponseEntity<?> reponse = controller.supprimerCreneau(5L, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(creneauRepository, never()).delete(any());
    }

    @Test
    @DisplayName("refuse une demande sur un créneau déjà réservé")
    void demanderRendezVous_creneau_reserve_refuse() {
        User titulaire = personnel(9L);
        CreneauDisponibilite creneau = new CreneauDisponibilite(titulaire, LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1));
        creneau.setReserve(true);
        when(creneauRepository.findById(3L)).thenReturn(Optional.of(creneau));

        ResponseEntity<?> reponse = controller.demanderRendezVous(
                new DemandeRendezVousRequest(3L, "EN_LIGNE", null, "Question"), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(rendezVousRepository, never()).save(any());
    }

    @Test
    @DisplayName("refuse une demande de rendez-vous avec soi-même")
    void demanderRendezVous_avec_soi_meme_refuse() {
        User moi = personnel(9L);
        CreneauDisponibilite creneau = new CreneauDisponibilite(moi, LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1));
        when(creneauRepository.findById(3L)).thenReturn(Optional.of(creneau));
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(moi);

        ResponseEntity<?> reponse = controller.demanderRendezVous(
                new DemandeRendezVousRequest(3L, "EN_LIGNE", null, null), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(rendezVousRepository, never()).save(any());
    }

    @Test
    @DisplayName("dépose la demande, réserve le créneau et notifie le personnel, auteur dérivé du jeton")
    void demanderRendezVous_valide_reserve_le_creneau() {
        User titulaire = personnel(9L);
        User moi = demandeur(20L);
        CreneauDisponibilite creneau = new CreneauDisponibilite(titulaire, LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1));
        when(creneauRepository.findById(3L)).thenReturn(Optional.of(creneau));
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(moi);
        when(rendezVousRepository.save(any(RendezVous.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> reponse = controller.demanderRendezVous(
                new DemandeRendezVousRequest(3L, "EN_LIGNE", null, "Question sur le programme"), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(creneau.isReserve()).isTrue();
        ArgumentCaptor<RendezVous> captureur = ArgumentCaptor.forClass(RendezVous.class);
        verify(rendezVousRepository).save(captureur.capture());
        assertThat(captureur.getValue().getDemandeur()).isEqualTo(moi);
        verify(rendezVousService).notifier(any(), any(), any());
    }

    @Test
    @DisplayName("école / entreprise : une demande vers un créneau non tenu par un responsable pédagogique est refusée")
    void demanderRendezVous_externe_hors_pedagogie_refuse() {
        User titulaire = personnel(9L); // ROLE_FORMATEUR
        User ecole = demandeur(30L);
        CreneauDisponibilite creneau = new CreneauDisponibilite(titulaire, LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1));
        when(creneauRepository.findById(3L)).thenReturn(Optional.of(creneau));
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(ecole);
        when(rendezVousService.estDemandeurExterne(ecole)).thenReturn(true);
        when(rendezVousService.estResponsablePedagogique(titulaire)).thenReturn(false);

        ResponseEntity<?> reponse = controller.demanderRendezVous(
                new DemandeRendezVousRequest(3L, "EN_LIGNE", null, null), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(creneau.isReserve()).isFalse();
        verify(rendezVousRepository, never()).save(any());
    }

    @Test
    @DisplayName("seul le titulaire du créneau peut répondre à une demande")
    void repondre_tiers_refuse() {
        User titulaire = personnel(9L);
        User autre = personnel(10L);
        CreneauDisponibilite creneau = new CreneauDisponibilite(titulaire, LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1));
        RendezVous rdv = new RendezVous(creneau, demandeur(20L), inpg.sn.lms.rendezvous.enums.EnumModaliteRendezVous.EN_LIGNE, null, null);
        when(rendezVousRepository.findById(7L)).thenReturn(Optional.of(rdv));
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(autre);

        ResponseEntity<?> reponse = controller.repondre(7L, new ReponseRendezVousRequest("ACCEPTER", null, null, null),
                authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(rendezVousRepository, never()).save(any());
    }

    @Test
    @DisplayName("le refus du personnel libère le créneau et notifie le demandeur")
    void repondre_refuser_libere_creneau() {
        User titulaire = personnel(9L);
        CreneauDisponibilite creneau = new CreneauDisponibilite(titulaire, LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1));
        creneau.setReserve(true);
        RendezVous rdv = new RendezVous(creneau, demandeur(20L), inpg.sn.lms.rendezvous.enums.EnumModaliteRendezVous.EN_LIGNE, null, null);
        when(rendezVousRepository.findById(7L)).thenReturn(Optional.of(rdv));
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(titulaire);
        when(rendezVousRepository.save(any(RendezVous.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> reponse = controller.repondre(7L, new ReponseRendezVousRequest("REFUSER", null, null, "Indisponible"),
                authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rdv.getStatut()).isEqualTo(EnumStatutRendezVous.REFUSE);
        assertThat(creneau.isReserve()).isFalse();
        verify(rendezVousService).notifier(any(), any(), any());
    }

    @Test
    @DisplayName("une proposition sans nouvelle date est refusée")
    void repondre_proposer_sans_date_refuse() {
        User titulaire = personnel(9L);
        CreneauDisponibilite creneau = new CreneauDisponibilite(titulaire, LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1));
        RendezVous rdv = new RendezVous(creneau, demandeur(20L), inpg.sn.lms.rendezvous.enums.EnumModaliteRendezVous.EN_LIGNE, null, null);
        when(rendezVousRepository.findById(7L)).thenReturn(Optional.of(rdv));
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(titulaire);

        ResponseEntity<?> reponse = controller.repondre(7L, new ReponseRendezVousRequest("PROPOSER", null, null, null),
                authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(rendezVousRepository, never()).save(any());
    }

    @Test
    @DisplayName("seul le demandeur peut répondre à une contre-proposition")
    void repondreProposition_tiers_refuse() {
        User titulaire = personnel(9L);
        User leDemandeur = demandeur(20L);
        User autre = demandeur(21L);
        CreneauDisponibilite creneau = new CreneauDisponibilite(titulaire, LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1));
        RendezVous rdv = new RendezVous(creneau, leDemandeur, inpg.sn.lms.rendezvous.enums.EnumModaliteRendezVous.EN_LIGNE, null, null);
        rdv.setStatut(EnumStatutRendezVous.PROPOSITION_AUTRE_DATE);
        when(rendezVousRepository.findById(7L)).thenReturn(Optional.of(rdv));
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(autre);

        ResponseEntity<?> reponse = controller.repondreProposition(7L, new ReponsePropositionRequest(true), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(rendezVousRepository, never()).save(any());
    }

    @Test
    @DisplayName("le refus d'une contre-proposition par le demandeur libère le créneau")
    void repondreProposition_refus_libere_creneau() {
        User titulaire = personnel(9L);
        User leDemandeur = demandeur(20L);
        CreneauDisponibilite creneau = new CreneauDisponibilite(titulaire, LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1));
        creneau.setReserve(true);
        RendezVous rdv = new RendezVous(creneau, leDemandeur, inpg.sn.lms.rendezvous.enums.EnumModaliteRendezVous.EN_LIGNE, null, null);
        rdv.setStatut(EnumStatutRendezVous.PROPOSITION_AUTRE_DATE);
        when(rendezVousRepository.findById(7L)).thenReturn(Optional.of(rdv));
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(leDemandeur);
        when(rendezVousRepository.save(any(RendezVous.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> reponse = controller.repondreProposition(7L, new ReponsePropositionRequest(false), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rdv.getStatut()).isEqualTo(EnumStatutRendezVous.REFUSE);
        assertThat(creneau.isReserve()).isFalse();
    }

    @Test
    @DisplayName("seul le demandeur peut annuler sa propre demande, et seulement si elle est encore en attente")
    void annulerDemande_deja_repondue_refuse() {
        User titulaire = personnel(9L);
        User leDemandeur = demandeur(20L);
        CreneauDisponibilite creneau = new CreneauDisponibilite(titulaire, LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1));
        RendezVous rdv = new RendezVous(creneau, leDemandeur, inpg.sn.lms.rendezvous.enums.EnumModaliteRendezVous.EN_LIGNE, null, null);
        rdv.setStatut(EnumStatutRendezVous.ACCEPTE);
        when(rendezVousRepository.findById(7L)).thenReturn(Optional.of(rdv));
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(leDemandeur);

        ResponseEntity<?> reponse = controller.annulerDemande(7L, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(rendezVousRepository, never()).save(any());
    }
}
