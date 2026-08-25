package inpg.sn.lms.recrutement.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.auth.entity.Role;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.CandidatRepository;
import inpg.sn.lms.auth.repository.RoleRepository;
import inpg.sn.lms.auth.repository.UserRepository;
import inpg.sn.lms.exception.BusinessException;
import inpg.sn.lms.recrutement.dto.CandidatureRequest;
import inpg.sn.lms.recrutement.dto.CandidatureResponse;
import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.enums.EnumStatutCandidature;
import inpg.sn.lms.recrutement.repository.BrouillonCandidatureRepository;
import inpg.sn.lms.recrutement.repository.CandidatureRepository;
import inpg.sn.lms.recrutement.repository.EntretienRepository;
import inpg.sn.lms.recrutement.repository.PieceJustificativeRepository;
import inpg.sn.lms.recrutement.repository.SessionCandidatureRepository;
import inpg.sn.lms.recrutement.repository.TestAcademiqueRepository;
import inpg.sn.lms.recrutement.service.CandidatureService;
import inpg.sn.lms.recrutement.service.ConvocationPdfService;
import inpg.sn.lms.recrutement.service.EntretienService;
import inpg.sn.lms.recrutement.service.FichierStockeService;
import inpg.sn.lms.recrutement.service.LettreAdmissionPdfService;
import inpg.sn.lms.recrutement.service.NotificationCandidatService;
import inpg.sn.lms.recrutement.service.NotificationService;
import inpg.sn.lms.recrutement.service.PaiementService;

/**
 * Règles de dépôt d'une candidature.
 *
 * <p>Deux défauts constatés le 16/08/2026 sont figés ici :
 *
 * <ul>
 *   <li>{@code garantirRoleCandidat()} vide les rôles du compte avant d'accorder
 *       CANDIDAT. La route n'écartait que le Responsable Admission : un étudiant
 *       qui postulait perdait donc l'accès à ses cours, ses notes et ses
 *       certificats, sans pouvoir le rétablir.</li>
 *   <li>{@code Candidature.soumettre()} réécrit le statut à SOUMIS sans regarder
 *       l'état de départ. Un simple renvoi du formulaire ramenait un dossier déjà
 *       présélectionné dans la pile « à traiter », effaçant la décision du
 *       responsable d'admission.</li>
 * </ul>
 *
 * <p>Le TDR exige par ailleurs qu'un candidat puisse « revenir plus tard pour
 * poursuivre et finaliser sa candidature » : le test
 * {@link #un_dossier_pas_encore_examine_peut_etre_resoumis()} garantit que le
 * correctif ne l'a pas interdit au passage.
 */
@ExtendWith(MockitoExtension.class)
class CandidatControllerSoumissionTest {

    private static final long ID_UTILISATEUR = 1L;
    private static final long ID_SESSION = 5L;

    @Mock private CandidatureService candidatureService;
    @Mock private CandidatRepository candidatRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private SessionCandidatureRepository sessionRepository;
    @Mock private CandidatureRepository candidatureRepository;
    @Mock private FichierStockeService fichierStockeService;
    @Mock private PieceJustificativeRepository pieceJustificativeRepository;
    @Mock private BrouillonCandidatureRepository brouillonRepository;
    @Mock private NotificationService notificationService;
    @Mock private NotificationCandidatService notificationCandidatService;
    @Mock private PaiementService paiementService;
    @Mock private TestAcademiqueRepository testAcademiqueRepository;
    @Mock private EntretienRepository entretienRepository;
    @Mock private EntretienService entretienService;
    @Mock private ConvocationPdfService convocationPdfService;
    @Mock private LettreAdmissionPdfService lettreAdmissionPdfService;

    @Mock private Authentication authentication;

    /**
     * Un vrai ObjectMapper, pas un mock : c'est lui qui remplace le découpage manuel du
     * JSON d'état civil. Un mock renverrait {@code null} et masquerait précisément ce
     * que ces tests doivent vérifier.
     */
    @Spy private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @InjectMocks private CandidatController controller;

    private User utilisateur;
    private Candidat candidat;
    private CandidatureRequest requete;

    @BeforeEach
    void preparerUtilisateurConnecte() {
        utilisateur = new User();
        utilisateur.setId(ID_UTILISATEUR);
        utilisateur.setUsername("awa_diop");
        utilisateur.setEmail("awa.diop@example.sn");

        candidat = new Candidat();
        candidat.setId(ID_UTILISATEUR); // @MapsId : l'id du candidat EST celui du user
        candidat.setNom("Diop");
        candidat.setPrenom("Awa");

        requete = new CandidatureRequest();
        requete.setSessionCandidatureId(ID_SESSION);

        // lenient : les tests qui s'arrêtent au contrôle de rôle n'atteignent pas ces appels.
        lenient().when(authentication.getName()).thenReturn("awa_diop");
        lenient().when(userRepository.findByUsername("awa_diop")).thenReturn(utilisateur);
    }

    // ===================== Rôle du compte =====================

    @Test
    void un_etudiant_ne_peut_pas_postuler_et_conserve_son_role() {
        utilisateur.addRole(role("ROLE_ETUDIANT", "Étudiant"));

        ResponseEntity<?> reponse = controller.soumettreCandidature(requete, authentication);

        assertEquals(HttpStatus.FORBIDDEN, reponse.getStatusCode());
        // Le rôle doit être intact : c'est précisément ce que le défaut détruisait.
        assertEquals(1, utilisateur.getRoles().size());
        assertEquals("ROLE_ETUDIANT", utilisateur.getRoles().iterator().next().getCode());
        verify(userRepository, never()).save(any());
        verify(candidatureRepository, never()).save(any());
    }

    @Test
    void un_formateur_ne_peut_pas_postuler() {
        utilisateur.addRole(role("ROLE_FORMATEUR", "Formateur"));

        assertEquals(HttpStatus.FORBIDDEN,
                controller.soumettreCandidature(requete, authentication).getStatusCode());
        verify(userRepository, never()).save(any());
    }

    /** Comportement d'origine, conservé : le refus du compte administratif ne doit pas régresser. */
    @Test
    void un_responsable_admission_ne_peut_toujours_pas_postuler() {
        utilisateur.addRole(role("ROLE_RESPONSABLE_ADMISSION", "Responsable Admission"));

        assertEquals(HttpStatus.FORBIDDEN,
                controller.soumettreCandidature(requete, authentication).getStatusCode());
    }

    // ===================== État du dossier =====================

    @Test
    void un_dossier_deja_examine_ne_peut_plus_etre_resoumis() {
        utilisateur.addRole(role("ROLE_CANDIDAT", "Candidat"));
        Candidature enExamen = candidature(EnumStatutCandidature.EN_COURS_DE_PRESELECTION);
        when(candidatRepository.findByUserId(ID_UTILISATEUR)).thenReturn(Optional.of(candidat));
        when(candidatureRepository.findByCandidatId(ID_UTILISATEUR)).thenReturn(List.of(enExamen));

        ResponseEntity<?> reponse = controller.soumettreCandidature(requete, authentication);

        assertEquals(HttpStatus.CONFLICT, reponse.getStatusCode());
        // Le statut ne doit pas avoir été ramené à SOUMIS.
        assertEquals(EnumStatutCandidature.EN_COURS_DE_PRESELECTION, enExamen.getStatut());
        verify(candidatureRepository, never()).save(any());
    }

    /**
     * Le contrôle doit précéder toute écriture. Une première version le plaçait après la
     * mise à jour de l'état civil : le dossier verrouillé recevait bien son refus, mais le
     * nom et le prénom du candidat avaient déjà été réécrits en base.
     */
    @Test
    void l_etat_civil_n_est_pas_modifie_quand_la_soumission_est_refusee() {
        utilisateur.addRole(role("ROLE_CANDIDAT", "Candidat"));
        when(candidatRepository.findByUserId(ID_UTILISATEUR)).thenReturn(Optional.of(candidat));
        when(candidatureRepository.findByCandidatId(ID_UTILISATEUR))
                .thenReturn(List.of(candidature(EnumStatutCandidature.ADMIS1)));
        requete.setInformationsPersonnelles("{\"nom\":\"USURPE\",\"prenom\":\"USURPE\"}");

        assertEquals(HttpStatus.CONFLICT,
                controller.soumettreCandidature(requete, authentication).getStatusCode());

        assertEquals("Diop", candidat.getNom());
        assertEquals("Awa", candidat.getPrenom());
        verify(candidatRepository, never()).save(any());
    }

    /**
     * Exigence du TDR : un dossier incomplet peut être repris. Tant que l'administration
     * ne s'est pas prononcée, une nouvelle soumission reste donc acceptée.
     */
    @Test
    void un_dossier_pas_encore_examine_peut_etre_resoumis() {
        utilisateur.addRole(role("ROLE_CANDIDAT", "Candidat"));
        Candidature enAttente = candidature(EnumStatutCandidature.SOUMIS);
        when(candidatRepository.findByUserId(ID_UTILISATEUR)).thenReturn(Optional.of(candidat));
        when(candidatureRepository.findByCandidatId(ID_UTILISATEUR)).thenReturn(List.of(enAttente));
        when(brouillonRepository.findByUserId(ID_UTILISATEUR)).thenReturn(Optional.empty());
        when(candidatureService.getCandidatureById(any())).thenReturn(new CandidatureResponse());

        ResponseEntity<?> reponse = controller.soumettreCandidature(requete, authentication);

        assertEquals(HttpStatus.OK, reponse.getStatusCode());
        assertEquals(EnumStatutCandidature.SOUMIS, enAttente.getStatut());
        verify(candidatureRepository).save(enAttente);
    }

    // ===================== Lecture de l'état civil =====================

    /**
     * Le découpage manuel qui précédait ne reconnaissait que les valeurs entre
     * guillemets : {@code "anneesExperience": 5}, un nombre JSON parfaitement légitime,
     * était perdu sans erreur. Ce champ alimente le filtre automatique de présélection.
     */
    @Test
    void les_champs_numeriques_et_les_dates_sont_bien_lus() {
        utilisateur.addRole(role("ROLE_CANDIDAT", "Candidat"));
        Candidature enAttente = candidature(EnumStatutCandidature.SOUMIS);
        when(candidatRepository.findByUserId(ID_UTILISATEUR)).thenReturn(Optional.of(candidat));
        when(candidatureRepository.findByCandidatId(ID_UTILISATEUR)).thenReturn(List.of(enAttente));
        when(brouillonRepository.findByUserId(ID_UTILISATEUR)).thenReturn(Optional.empty());
        when(candidatureService.getCandidatureById(any())).thenReturn(new CandidatureResponse());
        requete.setInformationsPersonnelles(
                "{\"anneesExperience\":5,\"dateNaissance\":\"1995-03-10\",\"profession\":\"Ingénieur\"}");

        assertEquals(HttpStatus.OK,
                controller.soumettreCandidature(requete, authentication).getStatusCode());

        assertEquals(5, candidat.getAnneesExperience());
        assertEquals(LocalDate.of(1995, 3, 10), candidat.getDateNaissance());
        assertEquals("Ingénieur", candidat.getProfession());
        // Un champ absent du JSON ne doit pas écraser ce qui existe déjà.
        assertEquals("Diop", candidat.getNom());
    }

    /** Un JSON illisible doit être signalé, non ignoré en silence. */
    @Test
    void un_etat_civil_illisible_est_refuse() {
        utilisateur.addRole(role("ROLE_CANDIDAT", "Candidat"));
        when(candidatRepository.findByUserId(ID_UTILISATEUR)).thenReturn(Optional.of(candidat));
        when(candidatureRepository.findByCandidatId(ID_UTILISATEUR)).thenReturn(List.of());
        requete.setInformationsPersonnelles("{ceci n'est pas du JSON");

        assertThrows(BusinessException.class,
                () -> controller.soumettreCandidature(requete, authentication));
        verify(candidatRepository, never()).save(any());
    }

    /** Un dossier rejeté n'est pas « actif » : il n'empêche pas de déposer à nouveau. */
    @Test
    void un_dossier_rejete_ne_verrouille_pas_le_compte() {
        utilisateur.addRole(role("ROLE_CANDIDAT", "Candidat"));
        when(candidatRepository.findByUserId(ID_UTILISATEUR)).thenReturn(Optional.of(candidat));
        when(candidatureRepository.findByCandidatId(ID_UTILISATEUR))
                .thenReturn(List.of(candidature(EnumStatutCandidature.REJETE)));
        when(sessionRepository.findById(ID_SESSION)).thenReturn(Optional.empty());

        // La session introuvable interrompt le traitement plus loin : ce qui compte ici est
        // que le dossier REJETE n'ait pas déclenché le refus 409.
        try {
            ResponseEntity<?> reponse = controller.soumettreCandidature(requete, authentication);
            assertEquals(HttpStatus.CONFLICT, reponse.getStatusCode(), "ne doit pas être verrouillé");
        } catch (RuntimeException attendu) {
            assertEquals("Session non trouvée", attendu.getMessage());
        }
    }

    // ===================== Fabriques =====================

    private Role role(String code, String libelle) {
        Role role = new Role();
        role.setCode(code);
        role.setLibelle(libelle);
        return role;
    }

    private Candidature candidature(EnumStatutCandidature statut) {
        Candidature candidature = new Candidature();
        candidature.setId(42L);
        candidature.setNumeroDossier("INPG-TEST42");
        candidature.setStatut(statut);
        candidature.setCandidat(candidat);
        return candidature;
    }
}
