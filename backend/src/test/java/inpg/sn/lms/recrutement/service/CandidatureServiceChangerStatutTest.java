package inpg.sn.lms.recrutement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
                () -> service.changerStatut(ID, EnumStatutCandidature.EN_COURS_DE_PRESELECTION, null));

        assertEquals("Ce dossier ne contient aucune pièce justificative : impossible de le présélectionner.",
                erreur.getMessage());
        assertEquals(EnumStatutCandidature.SOUMIS, vide.getStatut(), "le statut ne doit pas avoir bougé");
        verify(candidatureRepository, never()).save(any());
    }

    @Test
    @DisplayName("Une seule pièce jointe suffit à autoriser la présélection")
    void preselection_acceptee_des_qu_une_piece_existe() {
        Candidature dossier = candidature(EnumStatutCandidature.SOUMIS, true);
        when(candidatureRepository.findById(ID)).thenReturn(Optional.of(dossier));

        service.changerStatut(ID, EnumStatutCandidature.EN_COURS_DE_PRESELECTION, null);

        assertEquals(EnumStatutCandidature.EN_COURS_DE_PRESELECTION, dossier.getStatut());
        assertEquals(EnumStatutCandidature.SOUMIS, dossier.getStatutPrecedent(),
                "le statut précédent est mémorisé pour permettre l'annulation");
    }

    @Test
    @DisplayName("La garde ne s'applique qu'à la présélection : un dossier vide reste rejetable")
    void rejet_possible_meme_avec_un_dossier_vide() {
        Candidature vide = candidature(EnumStatutCandidature.SOUMIS, false);
        when(candidatureRepository.findById(ID)).thenReturn(Optional.of(vide));

        service.changerStatut(ID, EnumStatutCandidature.REJETE, "Dossier jamais complété.");

        assertEquals(EnumStatutCandidature.REJETE, vide.getStatut());
        assertEquals("Dossier jamais complété.", vide.getMotifRejet());
    }

    @Test
    @DisplayName("Une transition qui saute une étape est refusée")
    void transition_hors_parcours_est_refusee() {
        Candidature dossier = candidature(EnumStatutCandidature.SOUMIS, true);
        when(candidatureRepository.findById(ID)).thenReturn(Optional.of(dossier));

        assertThrows(BusinessException.class,
                () -> service.changerStatut(ID, EnumStatutCandidature.ADMIS2, null));
        assertEquals(EnumStatutCandidature.SOUMIS, dossier.getStatut());
    }

    @Test
    @DisplayName("ADMIS3 est terminal : plus aucune transition n'en part")
    void statut_terminal_n_evolue_plus() {
        Candidature dossier = candidature(EnumStatutCandidature.ADMIS3, true);
        when(candidatureRepository.findById(ID)).thenReturn(Optional.of(dossier));

        assertThrows(BusinessException.class,
                () -> service.changerStatut(ID, EnumStatutCandidature.REJETE, "trop tard"));
        assertEquals(EnumStatutCandidature.ADMIS3, dossier.getStatut());
    }

    @Test
    @DisplayName("Un motif n'est retenu que pour un rejet")
    void motif_ignore_hors_rejet() {
        Candidature dossier = candidature(EnumStatutCandidature.SOUMIS, true);
        when(candidatureRepository.findById(ID)).thenReturn(Optional.of(dossier));

        service.changerStatut(ID, EnumStatutCandidature.EN_COURS_DE_PRESELECTION, "commentaire quelconque");

        assertNull(dossier.getMotifRejet());
    }

    @Test
    void candidature_introuvable_est_signalee() {
        when(candidatureRepository.findById(ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.changerStatut(ID, EnumStatutCandidature.EN_COURS_DE_PRESELECTION, null));
    }

    /** @param avecPiece un dossier « non vide » au sens de la garde : au moins une pièce jointe. */
    private Candidature candidature(EnumStatutCandidature statut, boolean avecPiece) {
        Candidature candidature = new Candidature();
        candidature.setStatut(statut);
        candidature.setCandidat(new Candidat());
        candidature.setSessionCandidature(new SessionCandidature());
        if (avecPiece) {
            candidature.getPiecesJustificatives().add(new PieceJustificative());
        }
        return candidature;
    }
}
