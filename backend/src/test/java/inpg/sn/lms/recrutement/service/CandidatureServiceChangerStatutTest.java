package inpg.sn.lms.recrutement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.auth.repository.CandidatRepository;
import inpg.sn.lms.exception.BusinessException;
import inpg.sn.lms.exception.ResourceNotFoundException;
import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.PieceJustificative;
import inpg.sn.lms.recrutement.entity.SessionCandidature;
import inpg.sn.lms.recrutement.enums.EnumStatutCandidature;
import inpg.sn.lms.recrutement.enums.EnumStatutVerificationPiece;
import inpg.sn.lms.recrutement.enums.EnumTypeDocument;
import inpg.sn.lms.recrutement.repository.CandidatureRepository;
import inpg.sn.lms.recrutement.repository.SessionCandidatureRepository;

/**
 * Machine à états d'un dossier ({@link CandidatureService#changerStatut}) : transitions
 * autorisées, motif de rejet, et surtout la garde « dossier vide » — un dossier sans
 * aucune pièce justificative ne doit jamais atteindre la présélection, sans quoi le
 * responsable admission évalue un dossier qu'il n'a pas pu lire.
 */
@ExtendWith(MockitoExtension.class)
class CandidatureServiceChangerStatutTest {

    private static final long ID = 42L;

    @Mock private CandidatureRepository candidatureRepository;
    @Mock private SessionCandidatureRepository sessionCandidatureRepository;
    @Mock private CandidatRepository candidatRepository;
    @Mock private JournalEtapeService journalEtapeService;

    @InjectMocks private CandidatureService service;

    @BeforeEach
    void setUp() {
        lenient().when(candidatureRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Un dossier sans aucune pièce jointe ne peut pas passer en présélection")
    void preselection_refusee_si_dossier_vide() {
        Candidature vide = candidature(EnumStatutCandidature.SOUMIS, false);
        when(candidatureRepository.findById(ID)).thenReturn(Optional.of(vide));

        BusinessException erreur = assertThrows(BusinessException.class,
                () -> service.changerStatut(ID, EnumStatutCandidature.PRESELECTIONNE, null));

        assertTrue(erreur.getMessage().startsWith("Dossier incomplet : "), erreur.getMessage());
        assertTrue(erreur.getMessage().contains("Pièce d'identité (non fournie)"), erreur.getMessage());
        assertTrue(erreur.getMessage().contains("Demandez un complément"), erreur.getMessage());
        assertEquals(EnumStatutCandidature.SOUMIS, vide.getStatut(), "le statut ne doit pas avoir bougé");
        verify(candidatureRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un dossier complet peut être présélectionné")
    void preselection_acceptee_si_le_dossier_est_complet() {
        Candidature dossier = candidature(EnumStatutCandidature.SOUMIS, true);
        when(candidatureRepository.findById(ID)).thenReturn(Optional.of(dossier));

        service.changerStatut(ID, EnumStatutCandidature.PRESELECTIONNE, null);

        assertEquals(EnumStatutCandidature.PRESELECTIONNE, dossier.getStatut());
        assertEquals(EnumStatutCandidature.SOUMIS, dossier.getStatutPrecedent(),
                "le statut précédent est mémorisé pour permettre l'annulation");
    }

    @Test
    @DisplayName("La garde ne s'applique qu'à la décision favorable : un dossier vide reste rejetable")
    void rejet_possible_meme_avec_un_dossier_vide() {
        Candidature vide = candidature(EnumStatutCandidature.SOUMIS, false);
        when(candidatureRepository.findById(ID)).thenReturn(Optional.of(vide));

        service.changerStatut(ID, EnumStatutCandidature.DOSSIER_REJETE, "Dossier jamais complété.");

        assertEquals(EnumStatutCandidature.DOSSIER_REJETE, vide.getStatut());
        assertEquals("Dossier jamais complété.", vide.getMotifRejet());
    }

    @Test
    @DisplayName("Une transition qui saute une étape est refusée")
    void transition_hors_parcours_est_refusee() {
        Candidature dossier = candidature(EnumStatutCandidature.SOUMIS, true);
        when(candidatureRepository.findById(ID)).thenReturn(Optional.of(dossier));

        assertThrows(BusinessException.class,
                () -> service.changerStatut(ID, EnumStatutCandidature.CONVOQUE_ENTRETIEN, null));
        assertEquals(EnumStatutCandidature.SOUMIS, dossier.getStatut());
    }

    @Test
    @DisplayName("ADMIS3 est terminal : plus aucune transition n'en part")
    void statut_terminal_n_evolue_plus() {
        Candidature dossier = candidature(EnumStatutCandidature.ADMIS, true);
        when(candidatureRepository.findById(ID)).thenReturn(Optional.of(dossier));

        assertThrows(BusinessException.class,
                () -> service.changerStatut(ID, EnumStatutCandidature.DOSSIER_REJETE, "trop tard"));
        assertEquals(EnumStatutCandidature.ADMIS, dossier.getStatut());
    }

    @Test
    @DisplayName("Un motif n'est retenu que pour un rejet")
    void motif_ignore_hors_rejet() {
        Candidature dossier = candidature(EnumStatutCandidature.SOUMIS, true);
        when(candidatureRepository.findById(ID)).thenReturn(Optional.of(dossier));

        service.changerStatut(ID, EnumStatutCandidature.PRESELECTIONNE, "commentaire quelconque");

        assertNull(dossier.getMotifRejet());
    }

    @Test
    void candidature_introuvable_est_signalee() {
        when(candidatureRepository.findById(ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.changerStatut(ID, EnumStatutCandidature.PRESELECTIONNE, null));
    }

    // ===================== Complétude : la présélection n'est accordée qu'à un dossier complet =====================

    @Test
    @DisplayName("Un dossier auquel il manque son diplôme n'est pas présélectionnable, même avec d'autres pièces")
    void preselection_refusee_si_un_document_exige_manque() {
        Candidature dossier = candidature(EnumStatutCandidature.SOUMIS, true);
        dossier.getPiecesJustificatives().removeIf(p -> p.getTypeDocument() == EnumTypeDocument.DIPLOME);
        when(candidatureRepository.findById(ID)).thenReturn(Optional.of(dossier));

        BusinessException erreur = assertThrows(BusinessException.class,
                () -> service.changerStatut(ID, EnumStatutCandidature.PRESELECTIONNE, null));

        assertTrue(erreur.getMessage().contains("Diplôme (non fournie)"), erreur.getMessage());
        assertEquals(EnumStatutCandidature.SOUMIS, dossier.getStatut());
        verify(candidatureRepository, never()).save(any());
    }

    @Test
    @DisplayName("La validation du dossier (DOSSIER_VALIDE) est soumise à la même garde")
    void validation_refusee_si_le_dossier_est_incomplet() {
        Candidature dossier = candidature(EnumStatutCandidature.SOUMIS, false);
        when(candidatureRepository.findById(ID)).thenReturn(Optional.of(dossier));

        assertThrows(BusinessException.class,
                () -> service.changerStatut(ID, EnumStatutCandidature.DOSSIER_VALIDE, null));
    }

    @Test
    @DisplayName("Une pièce refusée et pas encore remplacée bloque la présélection")
    void preselection_refusee_tant_qu_une_piece_refusee_n_est_pas_remplacee() {
        Candidature dossier = candidature(EnumStatutCandidature.SOUMIS, true);
        dossier.getPiecesJustificatives().removeIf(p -> p.getTypeDocument() == EnumTypeDocument.CV);
        dossier.getPiecesJustificatives().add(DossiersDeTest.piece(EnumTypeDocument.CV,
                EnumStatutVerificationPiece.A_REMPLACER, LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1)));
        when(candidatureRepository.findById(ID)).thenReturn(Optional.of(dossier));

        BusinessException erreur = assertThrows(BusinessException.class,
                () -> service.changerStatut(ID, EnumStatutCandidature.PRESELECTIONNE, null));

        assertTrue(erreur.getMessage().contains("CV (à remplacer : non conforme)"), erreur.getMessage());
    }

    @Test
    @DisplayName("Une fois la pièce refusée remplacée par un nouveau dépôt, le dossier est de nouveau présélectionnable")
    void preselection_acceptee_une_fois_la_piece_refusee_remplacee() {
        Candidature dossier = candidature(EnumStatutCandidature.SOUMIS, true);
        dossier.getPiecesJustificatives().removeIf(p -> p.getTypeDocument() == EnumTypeDocument.CV);
        dossier.getPiecesJustificatives().add(DossiersDeTest.piece(EnumTypeDocument.CV,
                EnumStatutVerificationPiece.A_REMPLACER, LocalDateTime.now().minusDays(3), LocalDateTime.now().minusDays(2)));
        dossier.getPiecesJustificatives().add(DossiersDeTest.piece(EnumTypeDocument.CV,
                EnumStatutVerificationPiece.A_VERIFIER, LocalDateTime.now().minusDays(1), null));
        when(candidatureRepository.findById(ID)).thenReturn(Optional.of(dossier));

        service.changerStatut(ID, EnumStatutCandidature.PRESELECTIONNE, null);

        assertEquals(EnumStatutCandidature.PRESELECTIONNE, dossier.getStatut());
    }

    // ===================== Demander un complément =====================

    @Test
    void demander_un_complement_met_le_dossier_incomplet_en_attente_du_candidat() {
        Candidature dossier = candidature(EnumStatutCandidature.SOUMIS, false);
        when(candidatureRepository.findById(ID)).thenReturn(Optional.of(dossier));

        service.demanderComplement(ID);

        assertEquals(EnumStatutCandidature.INCOMPLET, dossier.getStatut());
        assertEquals(EnumStatutCandidature.SOUMIS, dossier.getStatutPrecedent());
    }

    @Test
    void on_ne_demande_pas_de_complement_pour_un_dossier_complet() {
        Candidature dossier = candidature(EnumStatutCandidature.SOUMIS, true);
        when(candidatureRepository.findById(ID)).thenReturn(Optional.of(dossier));

        BusinessException erreur = assertThrows(BusinessException.class, () -> service.demanderComplement(ID));

        assertTrue(erreur.getMessage().contains("complet"), erreur.getMessage());
        assertEquals(EnumStatutCandidature.SOUMIS, dossier.getStatut());
    }

    @Test
    void on_ne_demande_de_complement_qu_a_un_dossier_soumis() {
        for (EnumStatutCandidature statut : new EnumStatutCandidature[] {
                EnumStatutCandidature.INCOMPLET, EnumStatutCandidature.PRESELECTIONNE, EnumStatutCandidature.ADMIS }) {
            Candidature dossier = candidature(statut, false);
            when(candidatureRepository.findById(ID)).thenReturn(Optional.of(dossier));

            assertThrows(BusinessException.class, () -> service.demanderComplement(ID), statut.name());
            assertEquals(statut, dossier.getStatut());
        }
    }

    // ===================== Résultat du test et de l'entretien =====================

    @Test
    @DisplayName("Le résultat du test fait avancer un dossier convoqué — il restait bloqué faute de route qui pose ces statuts")
    void resultat_du_test() {
        for (Object[] cas : new Object[][] { { "REUSSI", EnumStatutCandidature.TEST_REUSSI },
                { "ECHOUE", EnumStatutCandidature.TEST_ECHOUE }, { "absent", EnumStatutCandidature.ABSENT_TEST } }) {
            Candidature dossier = candidature(EnumStatutCandidature.CONVOQUE_TEST, true);
            when(candidatureRepository.findById(ID)).thenReturn(Optional.of(dossier));

            service.enregistrerResultatTest(ID, (String) cas[0]);

            assertEquals(cas[1], dossier.getStatut(), (String) cas[0]);
        }
    }

    @Test
    void resultat_du_test_refuse_hors_convocation_ou_inconnu() {
        Candidature soumis = candidature(EnumStatutCandidature.SOUMIS, true);
        when(candidatureRepository.findById(ID)).thenReturn(Optional.of(soumis));
        assertThrows(BusinessException.class, () -> service.enregistrerResultatTest(ID, "REUSSI"));

        // Un résultat inconnu est refusé avant même de lire le dossier.
        assertThrows(BusinessException.class, () -> service.enregistrerResultatTest(ID, "PEUT-ETRE"));
        assertThrows(BusinessException.class, () -> service.enregistrerResultatTest(ID, null));
        assertEquals(EnumStatutCandidature.SOUMIS, soumis.getStatut());
    }

    @Test
    void resultat_de_l_entretien() {
        Candidature effectue = candidature(EnumStatutCandidature.CONVOQUE_ENTRETIEN, true);
        when(candidatureRepository.findById(ID)).thenReturn(Optional.of(effectue));
        service.enregistrerResultatEntretien(ID, "EFFECTUE");
        assertEquals(EnumStatutCandidature.ENTRETIEN_EFFECTUE, effectue.getStatut());

        Candidature absent = candidature(EnumStatutCandidature.CONVOQUE_ENTRETIEN, true);
        when(candidatureRepository.findById(ID)).thenReturn(Optional.of(absent));
        service.enregistrerResultatEntretien(ID, "ABSENT");
        assertEquals(EnumStatutCandidature.ABSENT_ENTRETIEN, absent.getStatut());

        Candidature test = candidature(EnumStatutCandidature.CONVOQUE_TEST, true);
        when(candidatureRepository.findById(ID)).thenReturn(Optional.of(test));
        assertThrows(BusinessException.class, () -> service.enregistrerResultatEntretien(ID, "EFFECTUE"));
        assertThrows(BusinessException.class, () -> service.enregistrerResultatEntretien(ID, "REUSSI"));
    }

    // ===================== Actions possibles : une seule source pour tous les écrans =====================

    private List<String> actions(EnumStatutCandidature statut, boolean complet) {
        return service.actionsPossibles(candidature(statut, complet));
    }

    @Test
    @DisplayName("Chaque statut du workflow propose les actions que le serveur acceptera")
    void actions_possibles_par_statut() {
        assertEquals(List.of("PRESELECTIONNER", "REJETER"), actions(EnumStatutCandidature.SOUMIS, true));
        assertEquals(List.of("DEMANDER_COMPLEMENT", "REJETER"), actions(EnumStatutCandidature.SOUMIS, false));
        assertEquals(List.of("REJETER"), actions(EnumStatutCandidature.INCOMPLET, false));
        assertEquals(List.of("ENREGISTRER_REGLEMENT", "REJETER"), actions(EnumStatutCandidature.PRESELECTIONNE, true));
        assertEquals(List.of("ENREGISTRER_REGLEMENT"), actions(EnumStatutCandidature.PAIEMENT_EN_ATTENTE, true));
        assertEquals(List.of("CONVOQUER_TEST"), actions(EnumStatutCandidature.PAIEMENT_CONFIRME, true));
        assertEquals(List.of("ENREGISTRER_RESULTAT_TEST"), actions(EnumStatutCandidature.CONVOQUE_TEST, true));
        assertEquals(List.of("ADMETTRE_ENTRETIEN", "REJETER"), actions(EnumStatutCandidature.TEST_REUSSI, true));
        assertEquals(List.of("REJETER"), actions(EnumStatutCandidature.TEST_ECHOUE, true));
        assertEquals(List.of("CONVOQUER_TEST", "REJETER"), actions(EnumStatutCandidature.ABSENT_TEST, true));
        assertEquals(List.of("ENREGISTRER_RESULTAT_ENTRETIEN"), actions(EnumStatutCandidature.CONVOQUE_ENTRETIEN, true));
        assertEquals(List.of("ADMETTRE_ENTRETIEN", "REJETER"), actions(EnumStatutCandidature.ABSENT_ENTRETIEN, true));
        assertEquals(List.of("ADMETTRE_FINAL", "PLACER_LISTE_ATTENTE", "REJETER"),
                actions(EnumStatutCandidature.ENTRETIEN_EFFECTUE, true));
        assertEquals(List.of("ADMETTRE_FINAL", "REJETER"), actions(EnumStatutCandidature.LISTE_ATTENTE, true));
        assertEquals(List.of("FINALISER_INSCRIPTION"), actions(EnumStatutCandidature.ADMIS, true));
    }

    @Test
    void un_dossier_decide_ou_ferme_ne_propose_plus_rien() {
        for (EnumStatutCandidature statut : new EnumStatutCandidature[] { EnumStatutCandidature.DOSSIER_REJETE,
                EnumStatutCandidature.NON_ADMIS, EnumStatutCandidature.INSCRIPTION_FINALISEE,
                EnumStatutCandidature.DESISTEMENT, EnumStatutCandidature.BROUILLON }) {
            assertEquals(List.of(), actions(statut, true), statut.name());
        }
    }

    /**
     * Garde-fou contre la dérive : une action que le serveur propose doit être une transition qu'il accepte. C'est
     * l'écart entre ces deux listes, recopiées dans les écrans avec l'ancien vocabulaire, qui laissait des dossiers
     * sans aucun bouton.
     */
    @Test
    void toute_action_proposee_correspond_a_une_transition_acceptee() {
        java.util.Map<String, EnumStatutCandidature[]> cibles = java.util.Map.ofEntries(
                java.util.Map.entry("PRESELECTIONNER", new EnumStatutCandidature[] { EnumStatutCandidature.PRESELECTIONNE }),
                java.util.Map.entry("DEMANDER_COMPLEMENT", new EnumStatutCandidature[] { EnumStatutCandidature.INCOMPLET }),
                java.util.Map.entry("ENREGISTRER_REGLEMENT", new EnumStatutCandidature[] { EnumStatutCandidature.PAIEMENT_CONFIRME }),
                java.util.Map.entry("CONVOQUER_TEST", new EnumStatutCandidature[] { EnumStatutCandidature.CONVOQUE_TEST }),
                java.util.Map.entry("ENREGISTRER_RESULTAT_TEST", new EnumStatutCandidature[] {
                        EnumStatutCandidature.TEST_REUSSI, EnumStatutCandidature.TEST_ECHOUE, EnumStatutCandidature.ABSENT_TEST }),
                java.util.Map.entry("ADMETTRE_ENTRETIEN", new EnumStatutCandidature[] { EnumStatutCandidature.CONVOQUE_ENTRETIEN }),
                java.util.Map.entry("ENREGISTRER_RESULTAT_ENTRETIEN", new EnumStatutCandidature[] {
                        EnumStatutCandidature.ENTRETIEN_EFFECTUE, EnumStatutCandidature.ABSENT_ENTRETIEN }),
                java.util.Map.entry("ADMETTRE_FINAL", new EnumStatutCandidature[] { EnumStatutCandidature.ADMIS }),
                java.util.Map.entry("PLACER_LISTE_ATTENTE", new EnumStatutCandidature[] { EnumStatutCandidature.LISTE_ATTENTE }),
                java.util.Map.entry("FINALISER_INSCRIPTION", new EnumStatutCandidature[] { EnumStatutCandidature.INSCRIPTION_FINALISEE }),
                java.util.Map.entry("REJETER", new EnumStatutCandidature[] {
                        EnumStatutCandidature.DOSSIER_REJETE, EnumStatutCandidature.NON_ADMIS }));

        for (EnumStatutCandidature statut : EnumStatutCandidature.values()) {
            for (boolean complet : new boolean[] { true, false }) {
                for (String action : actions(statut, complet)) {
                    boolean accepte = false;
                    for (EnumStatutCandidature cible : cibles.get(action)) {
                        Candidature dossier = candidature(statut, complet);
                        when(candidatureRepository.findById(ID)).thenReturn(Optional.of(dossier));
                        try {
                            service.changerStatut(ID, cible, "motif");
                            accepte = true;
                        } catch (BusinessException e) {
                            // Refus d'une garde métier (complétude) ou transition : seul « non autorisée » est une dérive.
                            if (!e.getMessage().startsWith("Transition de statut non autorisée")) {
                                accepte = true;
                            }
                        }
                    }
                    assertTrue(accepte, "action " + action + " proposée au statut " + statut
                            + " sans aucune transition valide derrière");
                }
            }
        }
    }

    // ===================== Recul =====================

    @Test
    void recul_possible_uniquement_vers_une_etape_anterieure() {
        assertEquals(List.of(EnumStatutCandidature.SOUMIS, EnumStatutCandidature.INCOMPLET),
                service.reculsPossibles(candidature(EnumStatutCandidature.PRESELECTIONNE, true)));
        assertEquals(List.of(), service.reculsPossibles(candidature(EnumStatutCandidature.SOUMIS, true)));
    }

    @Test
    void un_dossier_ecarte_ou_en_liste_d_attente_peut_etre_ramene_a_n_importe_quelle_etape_du_parcours() {
        List<EnumStatutCandidature> depuisRejet = service.reculsPossibles(candidature(EnumStatutCandidature.DOSSIER_REJETE, true));
        assertTrue(depuisRejet.contains(EnumStatutCandidature.SOUMIS));
        assertTrue(depuisRejet.contains(EnumStatutCandidature.CONVOQUE_TEST));
        assertTrue(depuisRejet.contains(EnumStatutCandidature.ENTRETIEN_EFFECTUE));
        assertEquals(depuisRejet, service.reculsPossibles(candidature(EnumStatutCandidature.NON_ADMIS, true)));
        assertEquals(depuisRejet, service.reculsPossibles(candidature(EnumStatutCandidature.LISTE_ATTENTE, true)));
    }

    @Test
    void un_dossier_dont_l_inscription_est_finalisee_ne_recule_plus() {
        assertEquals(List.of(), service.reculsPossibles(candidature(EnumStatutCandidature.INSCRIPTION_FINALISEE, true)));
    }

    /** @param complet un dossier complet au sens de {@link CompletudeDossier} (quatre pièces), sinon un dossier vide. */
    private Candidature candidature(EnumStatutCandidature statut, boolean complet) {
        Candidature candidature = new Candidature();
        candidature.setStatut(statut);
        candidature.setCandidat(new Candidat());
        candidature.setSessionCandidature(new SessionCandidature());
        if (complet) {
            DossiersDeTest.completer(candidature);
        }
        return candidature;
    }
}
