package inpg.sn.lms.recrutement.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.exception.BusinessException;
import inpg.sn.lms.exception.ResourceNotFoundException;
import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.Paiement;
import inpg.sn.lms.recrutement.enums.EnumStatutPaiement;
import inpg.sn.lms.recrutement.repository.CandidatureRepository;
import inpg.sn.lms.recrutement.repository.PaiementRepository;

/**
 * Règles d'accès au reçu de paiement : seul le propriétaire du dossier peut le
 * télécharger, et seulement si le paiement est réellement validé.
 */
@ExtendWith(MockitoExtension.class)
class PaiementServiceRecuTest {

    private static final byte[] PDF = "%PDF-faux".getBytes();

    @Mock
    private PaiementRepository paiementRepository;
    @Mock
    private CandidatureRepository candidatureRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private NotificationCandidatService notificationCandidatService;
    @Mock
    private RecuPaiementService recuPaiementService;

    @InjectMocks
    private PaiementService paiementService;

    private Paiement paiement;

    @BeforeEach
    void preparerPaiement() {
        Candidat candidat = new Candidat();
        candidat.setId(7L);

        Candidature candidature = new Candidature();
        candidature.setNumeroDossier("DOS-2026-0042");
        candidature.setCandidat(candidat);

        paiement = new Paiement();
        paiement.setId(1L);
        paiement.setStatut(EnumStatutPaiement.SUCCES);
        paiement.setCandidature(candidature);
    }

    @Test
    void le_proprietaire_recupere_son_recu() {
        when(paiementRepository.findById(1L)).thenReturn(Optional.of(paiement));
        when(recuPaiementService.genererRecu(paiement)).thenReturn(PDF);

        PaiementService.Recu recu = paiementService.genererRecu(1L, 7L);

        assertArrayEquals(PDF, recu.contenu());
        assertEquals("recu-DOS-2026-0042.pdf", recu.nomFichier());
    }

    @Test
    void un_autre_candidat_ne_peut_pas_telecharger_le_recu() {
        when(paiementRepository.findById(1L)).thenReturn(Optional.of(paiement));

        assertThrows(AccessDeniedException.class, () -> paiementService.genererRecu(1L, 99L));
        verify(recuPaiementService, never()).genererRecu(any());
    }

    /** Le back-office (candidatId null) n'est pas soumis au contrôle de propriété. */
    @Test
    void le_back_office_recupere_le_recu_sans_controle_de_propriete() {
        when(paiementRepository.findById(1L)).thenReturn(Optional.of(paiement));
        when(recuPaiementService.genererRecu(paiement)).thenReturn(PDF);

        assertArrayEquals(PDF, paiementService.genererRecu(1L, null).contenu());
    }

    @Test
    void aucun_recu_tant_que_le_paiement_n_est_pas_valide() {
        paiement.setStatut(EnumStatutPaiement.EN_ATTENTE);
        when(paiementRepository.findById(1L)).thenReturn(Optional.of(paiement));

        assertThrows(BusinessException.class, () -> paiementService.genererRecu(1L, 7L));
        verify(recuPaiementService, never()).genererRecu(any());
    }

    @Test
    void paiement_inexistant() {
        when(paiementRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paiementService.genererRecu(404L, 7L));
    }
}
