package inpg.sn.lms.recrutementformateur.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

import inpg.sn.lms.auth.entity.Formateur;
import inpg.sn.lms.auth.entity.Role;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.FormateurRepository;
import inpg.sn.lms.auth.repository.RoleRepository;
import inpg.sn.lms.auth.service.UserService;
import inpg.sn.lms.espace_etudiant.repository.FormationRepository;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;
import inpg.sn.lms.recrutement.entity.FichierStocke;
import inpg.sn.lms.recrutement.service.FichierStockeService;
import inpg.sn.lms.recrutementformateur.dto.OffreFormateurDto;
import inpg.sn.lms.recrutementformateur.dto.ChangementStatutRequest;
import inpg.sn.lms.recrutementformateur.dto.DecisionCandidatureFormateurRequest;
import inpg.sn.lms.recrutementformateur.dto.OffreFormateurRequest;
import inpg.sn.lms.recrutementformateur.entity.CandidatureFormateur;
import inpg.sn.lms.recrutementformateur.entity.OffreRecrutementFormateur;
import inpg.sn.lms.recrutementformateur.enums.EnumStatutCandidatureFormateur;
import inpg.sn.lms.recrutementformateur.enums.EnumStatutOffreFormateur;
import inpg.sn.lms.recrutementformateur.enums.EnumTypeContratFormateur;
import inpg.sn.lms.recrutementformateur.repository.CandidatureFormateurRepository;
import inpg.sn.lms.recrutementformateur.repository.CertificationCandidatureFormateurRepository;
import inpg.sn.lms.recrutementformateur.repository.ExperienceCandidatureFormateurRepository;
import inpg.sn.lms.recrutementformateur.repository.FormationCandidatureFormateurRepository;
import inpg.sn.lms.recrutementformateur.repository.LangueCandidatureFormateurRepository;
import inpg.sn.lms.recrutementformateur.repository.OffreRecrutementFormateurRepository;
import inpg.sn.lms.recrutementformateur.repository.PieceCandidatureFormateurRepository;
import inpg.sn.lms.recrutementformateur.service.RecrutementFormateurNotificationService;

/**
 * Volet responsable admission : cycle de vie des offres, avancement du suivi
 * et décision — dont la bascule du compte candidat en formateur au RETENU.
 */
@ExtendWith(MockitoExtension.class)
class AdminRecrutementFormateurControllerTest {

    @Mock private OffreRecrutementFormateurRepository offreRepository;
    @Mock private CandidatureFormateurRepository candidatureRepository;
    @Mock private PieceCandidatureFormateurRepository pieceRepository;
    @Mock private FormationCandidatureFormateurRepository formationCandidatureRepository;
    @Mock private ExperienceCandidatureFormateurRepository experienceRepository;
    @Mock private LangueCandidatureFormateurRepository langueRepository;
    @Mock private CertificationCandidatureFormateurRepository certificationRepository;
    @Mock private FormationRepository formationRepository;
    @Mock private FormateurRepository formateurRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserService userService;
    @Mock private UtilisateurCourantService utilisateurCourant;
    @Mock private RecrutementFormateurNotificationService notificationService;
    @Mock private FichierStockeService fichierStockeService;
    @Mock private Authentication authentication;

    private AdminRecrutementFormateurController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminRecrutementFormateurController(offreRepository, candidatureRepository, pieceRepository,
                formationCandidatureRepository, experienceRepository, langueRepository, certificationRepository,
                formationRepository, formateurRepository, roleRepository, userService, utilisateurCourant,
                notificationService, fichierStockeService);
    }

    private OffreRecrutementFormateur offre(EnumStatutOffreFormateur statut) {
        OffreRecrutementFormateur o = new OffreRecrutementFormateur();
        o.setId(1L);
        o.setIntitule("Formateur HSE");
        o.setTypeContrat(EnumTypeContratFormateur.CDD);
        o.setStatut(statut);
        return o;
    }

    // ---------- Fiche de poste jointe ----------

    private static FichierStocke fichier(long id, String nom) {
        FichierStocke f = new FichierStocke();
        f.setId(id);
        f.setNomOriginal(nom);
        return f;
    }

    private static MockMultipartFile pdf(String nom) {
        return new MockMultipartFile("fichier", nom, "application/pdf", "%PDF-1.4 contenu".getBytes());
    }

    @Test
    @DisplayName("Joindre un PDF : le fichier est rattaché à l'offre et son nom est exposé")
    void joindre_fiche_de_poste_pdf() {
        OffreRecrutementFormateur o = offre(EnumStatutOffreFormateur.BROUILLON);
        when(offreRepository.findById(1L)).thenReturn(Optional.of(o));
        when(offreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(fichierStockeService.enregistrerFichier(any())).thenReturn(fichier(11L, "fiche-hse.pdf"));

        ResponseEntity<?> reponse = controller.joindreFichePoste(1L, pdf("fiche-hse.pdf"));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(o.getFichePoste().getId()).isEqualTo(11L);
        assertThat(((OffreFormateurDto) reponse.getBody()).fichePosteNom()).isEqualTo("fiche-hse.pdf");
        verify(fichierStockeService, never()).supprimerFichier(any());
    }

    @Test
    @DisplayName("Joindre un fichier qui n'est pas un PDF (Word, image…) : 400, rien n'est enregistré")
    void joindre_fiche_de_poste_non_pdf_est_refuse() {
        when(offreRepository.findById(1L)).thenReturn(Optional.of(offre(EnumStatutOffreFormateur.BROUILLON)));
        MockMultipartFile word = new MockMultipartFile("fichier", "fiche.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", new byte[] { 1, 2, 3 });

        ResponseEntity<?> reponse = controller.joindreFichePoste(1L, word);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(fichierStockeService, never()).enregistrerFichier(any());
        verify(offreRepository, never()).save(any());
    }

    @Test
    @DisplayName("Une fiche de plus de 10 Mo est refusée avant tout stockage")
    void joindre_fiche_trop_lourde_est_refuse() {
        when(offreRepository.findById(1L)).thenReturn(Optional.of(offre(EnumStatutOffreFormateur.BROUILLON)));
        byte[] trop = new byte[(int) AdminRecrutementFormateurController.TAILLE_MAX_FICHE_POSTE + 1];
        MockMultipartFile gros = new MockMultipartFile("fichier", "gros.pdf", "application/pdf", trop);

        ResponseEntity<?> reponse = controller.joindreFichePoste(1L, gros);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(fichierStockeService, never()).enregistrerFichier(any());
    }

    @Test
    @DisplayName("Un faux PDF (contenu ≠ signature) refusé par le stockage : 400, l'offre reste inchangée")
    void joindre_faux_pdf_est_refuse() {
        OffreRecrutementFormateur o = offre(EnumStatutOffreFormateur.BROUILLON);
        when(offreRepository.findById(1L)).thenReturn(Optional.of(o));
        when(fichierStockeService.enregistrerFichier(any()))
                .thenThrow(new IllegalArgumentException("Le contenu du fichier ne correspond pas au format annoncé."));

        ResponseEntity<?> reponse = controller.joindreFichePoste(1L, pdf("faux.pdf"));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(o.getFichePoste()).isNull();
        verify(offreRepository, never()).save(any());
    }

    @Test
    @DisplayName("Remplacer la fiche : l'ancien fichier est supprimé, pas laissé orphelin")
    void remplacer_fiche_de_poste_supprime_l_ancienne() {
        OffreRecrutementFormateur o = offre(EnumStatutOffreFormateur.PUBLIEE);
        o.setFichePoste(fichier(10L, "ancienne.pdf"));
        when(offreRepository.findById(1L)).thenReturn(Optional.of(o));
        when(offreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(fichierStockeService.enregistrerFichier(any())).thenReturn(fichier(11L, "nouvelle.pdf"));

        controller.joindreFichePoste(1L, pdf("nouvelle.pdf"));

        assertThat(o.getFichePoste().getId()).isEqualTo(11L);
        verify(fichierStockeService).supprimerFichier(10L);
    }

    @Test
    @DisplayName("Retirer la fiche : l'offre n'en porte plus et le fichier est supprimé")
    void retirer_fiche_de_poste() {
        OffreRecrutementFormateur o = offre(EnumStatutOffreFormateur.PUBLIEE);
        o.setFichePoste(fichier(10L, "fiche.pdf"));
        when(offreRepository.findById(1L)).thenReturn(Optional.of(o));
        when(offreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> reponse = controller.retirerFichePoste(1L);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(o.getFichePoste()).isNull();
        assertThat(((OffreFormateurDto) reponse.getBody()).fichePosteNom()).isNull();
        verify(fichierStockeService).supprimerFichier(10L);
    }

    @Test
    @DisplayName("Supprimer une offre emporte sa fiche de poste")
    void supprimer_offre_supprime_sa_fiche() {
        OffreRecrutementFormateur o = offre(EnumStatutOffreFormateur.BROUILLON);
        o.setFichePoste(fichier(10L, "fiche.pdf"));
        when(offreRepository.findById(1L)).thenReturn(Optional.of(o));
        when(candidatureRepository.countByOffreId(1L)).thenReturn(0L);

        ResponseEntity<?> reponse = controller.supprimerOffre(1L);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(offreRepository).delete(o);
        verify(fichierStockeService).supprimerFichier(10L);
    }

    @Test
    @DisplayName("Télécharger la fiche d'une offre qui n'en a pas : 404")
    void telecharger_fiche_absente() {
        when(offreRepository.findById(1L)).thenReturn(Optional.of(offre(EnumStatutOffreFormateur.PUBLIEE)));

        assertThat(controller.telechargerFichePoste(1L).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private CandidatureFormateur candidature(EnumStatutCandidatureFormateur statut) {
        User candidat = new User();
        candidat.setId(7L);
        candidat.setEmail("c@c.sn");
        CandidatureFormateur c = new CandidatureFormateur(offre(EnumStatutOffreFormateur.PUBLIEE), candidat,
                "Sow", "Modou", "c@c.sn", "770123456");
        c.setId(5L);
        c.setStatut(statut);
        return c;
    }

    // ---------- Offres ----------

    @Test
    @DisplayName("Publication d'une offre brouillon : PUBLIEE + date de publication posée")
    void publier_offre_brouillon() {
        OffreRecrutementFormateur o = offre(EnumStatutOffreFormateur.BROUILLON);
        when(offreRepository.findById(1L)).thenReturn(Optional.of(o));
        when(offreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(candidatureRepository.countByOffreId(1L)).thenReturn(0L);

        ResponseEntity<?> reponse = controller.publierOffre(1L);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(o.getStatut()).isEqualTo(EnumStatutOffreFormateur.PUBLIEE);
        assertThat(o.getDatePublication()).isNotNull();
    }

    @Test
    @DisplayName("Publication d'une offre clôturée : 409")
    void publier_offre_cloturee_est_refuse() {
        when(offreRepository.findById(1L)).thenReturn(Optional.of(offre(EnumStatutOffreFormateur.CLOTUREE)));

        ResponseEntity<?> reponse = controller.publierOffre(1L);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(offreRepository, never()).save(any());
    }

    @Test
    @DisplayName("Suppression d'une offre avec candidatures : 409")
    void supprimer_offre_avec_candidatures_est_refuse() {
        when(offreRepository.findById(1L)).thenReturn(Optional.of(offre(EnumStatutOffreFormateur.PUBLIEE)));
        when(candidatureRepository.countByOffreId(1L)).thenReturn(3L);

        ResponseEntity<?> reponse = controller.supprimerOffre(1L);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(offreRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Création d'offre sans intitulé : 400")
    void creer_offre_sans_intitule_est_refuse() {
        ResponseEntity<?> reponse = controller.creerOffre(
                new OffreFormateurRequest("  ", null, null, null, "CDD", null, 1, null, null), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(offreRepository, never()).save(any());
    }

    // ---------- Suivi ----------

    @Test
    @DisplayName("Changement de statut : saut DOSSIER_RECU → TERMINE interdit (400)")
    void changer_statut_saut_interdit_est_refuse() {
        when(candidatureRepository.findById(5L))
                .thenReturn(Optional.of(candidature(EnumStatutCandidatureFormateur.DOSSIER_RECU)));

        ResponseEntity<?> reponse = controller.changerStatut(5L, new ChangementStatutRequest("TERMINE"));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(candidatureRepository, never()).save(any());
    }

    @Test
    @DisplayName("Changement de statut valide : EN_COURS_DE_TRAITEMENT + candidat notifié")
    void changer_statut_valide_notifie() {
        CandidatureFormateur c = candidature(EnumStatutCandidatureFormateur.DOSSIER_RECU);
        when(candidatureRepository.findById(5L)).thenReturn(Optional.of(c));
        when(candidatureRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pieceRepository.findByCandidatureId(5L)).thenReturn(java.util.List.of());

        ResponseEntity<?> reponse = controller.changerStatut(5L,
                new ChangementStatutRequest("EN_COURS_DE_TRAITEMENT"));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(c.getStatut()).isEqualTo(EnumStatutCandidatureFormateur.EN_COURS_DE_TRAITEMENT);
        verify(notificationService).notifierPriseEnCharge(anyString(), anyString(), anyString());
    }

    // ---------- Décision ----------

    @Test
    @DisplayName("Décision NON_RETENU sans motif : 400")
    void decider_non_retenu_sans_motif_est_refuse() {
        when(candidatureRepository.findById(5L))
                .thenReturn(Optional.of(candidature(EnumStatutCandidatureFormateur.EN_COURS_DE_TRAITEMENT)));

        ResponseEntity<?> reponse = controller.decider(5L,
                new DecisionCandidatureFormateurRequest("NON_RETENU", "  "), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(candidatureRepository, never()).save(any());
    }

    @Test
    @DisplayName("Décision RETENU : TERMINE, compte basculé en formateur, fiche créée, école notifiée")
    void decider_retenu_cree_le_formateur() {
        CandidatureFormateur c = candidature(EnumStatutCandidatureFormateur.EN_COURS_DE_TRAITEMENT);
        when(candidatureRepository.findById(5L)).thenReturn(Optional.of(c));
        when(candidatureRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pieceRepository.findByCandidatureId(5L)).thenReturn(java.util.List.of());
        when(formateurRepository.findByTelephone("770123456")).thenReturn(Optional.empty());
        when(formateurRepository.findByUserId(7L)).thenReturn(Optional.empty());
        when(roleRepository.findByCode("ROLE_FORMATEUR"))
                .thenReturn(new Role(4, "ROLE_FORMATEUR", "Formateur", null));
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(new User());

        ResponseEntity<?> reponse = controller.decider(5L,
                new DecisionCandidatureFormateurRequest("RETENU", null), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(c.getStatut()).isEqualTo(EnumStatutCandidatureFormateur.TERMINE);
        assertThat(c.isFormateurCree()).isTrue();
        assertThat(c.getCandidat().getRoles()).extracting(Role::getCode).containsExactly("ROLE_FORMATEUR");
        verify(formateurRepository).save(any(Formateur.class));
        verify(notificationService).notifierRetenu(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Décision RETENU avec téléphone déjà pris : 400, rien n'est créé")
    void decider_retenu_telephone_deja_pris_est_refuse() {
        CandidatureFormateur c = candidature(EnumStatutCandidatureFormateur.EN_COURS_DE_TRAITEMENT);
        when(candidatureRepository.findById(5L)).thenReturn(Optional.of(c));
        when(formateurRepository.findByTelephone("770123456"))
                .thenReturn(Optional.of(new Formateur()));

        ResponseEntity<?> reponse = controller.decider(5L,
                new DecisionCandidatureFormateurRequest("RETENU", null), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(formateurRepository, never()).save(any());
        verify(candidatureRepository, never()).save(any());
    }

    @Test
    @DisplayName("Décision sur une candidature ayant déjà donné un compte formateur : 409")
    void decider_sur_formateur_deja_cree_est_refuse() {
        CandidatureFormateur c = candidature(EnumStatutCandidatureFormateur.TERMINE);
        c.setFormateurCree(true);
        when(candidatureRepository.findById(5L)).thenReturn(Optional.of(c));

        ResponseEntity<?> reponse = controller.decider(5L,
                new DecisionCandidatureFormateurRequest("RETENU", null), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(candidatureRepository, never()).save(any());
    }
}
