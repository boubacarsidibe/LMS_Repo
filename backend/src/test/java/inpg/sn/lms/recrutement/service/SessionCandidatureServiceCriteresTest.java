package inpg.sn.lms.recrutement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
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

import inpg.sn.lms.exception.BusinessException;
import inpg.sn.lms.exception.ResourceNotFoundException;
import inpg.sn.lms.recrutement.dto.CritereAdmissionRequest;
import inpg.sn.lms.recrutement.dto.CritereAdmissionResponse;
import inpg.sn.lms.recrutement.entity.CritereAdmissionPersonnalise;
import inpg.sn.lms.recrutement.entity.SessionCandidature;
import inpg.sn.lms.recrutement.enums.EnumTypeCritere;
import inpg.sn.lms.recrutement.repository.CritereAdmissionPersonnaliseRepository;
import inpg.sn.lms.recrutement.repository.SessionCandidatureRepository;

/**
 * CRUD des critères personnalisés — en particulier le retrait, qui doit toujours être
 * logique (voir migration V30) : un critère déjà répondu par des candidats ne perd
 * jamais ses réponses parce que l'admin a cessé de le proposer.
 */
@ExtendWith(MockitoExtension.class)
class SessionCandidatureServiceCriteresTest {

    private static final long ID_SESSION = 1L;
    private static final long ID_CRITERE = 42L;

    @Mock private SessionCandidatureRepository sessionRepository;
    @Mock private CritereAdmissionPersonnaliseRepository critereRepository;

    @InjectMocks private SessionCandidatureService service;

    @BeforeEach
    void setUp() {
        lenient().when(critereRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void creer_un_critere_sans_libelle_est_refuse() {
        when(sessionRepository.findById(ID_SESSION)).thenReturn(Optional.of(new SessionCandidature()));
        CritereAdmissionRequest request = new CritereAdmissionRequest();
        request.setType(EnumTypeCritere.TEXTE);
        request.setLibelle("  ");

        assertThrows(BusinessException.class, () -> service.creerCritere(ID_SESSION, request));
        verify(critereRepository, never()).save(any());
    }

    @Test
    void creer_un_critere_sur_une_session_introuvable_est_signale() {
        when(sessionRepository.findById(ID_SESSION)).thenReturn(Optional.empty());
        CritereAdmissionRequest request = new CritereAdmissionRequest();
        request.setLibelle("Diplôme d'ingénieur");
        request.setType(EnumTypeCritere.BOOLEEN);

        assertThrows(ResourceNotFoundException.class, () -> service.creerCritere(ID_SESSION, request));
    }

    /** Le retrait est logique : jamais de {@code deleteById}, seulement {@code actif = false}. */
    @Test
    void retirer_un_critere_le_desactive_sans_le_supprimer() {
        CritereAdmissionPersonnalise critere = new CritereAdmissionPersonnalise();
        critere.setId(ID_CRITERE);
        critere.setActif(true);
        when(critereRepository.findById(ID_CRITERE)).thenReturn(Optional.of(critere));

        CritereAdmissionResponse reponse = service.retirerCritere(ID_CRITERE);

        assertFalse(reponse.isActif());
        assertFalse(critere.isActif());
        verify(critereRepository, never()).delete(any());
        verify(critereRepository, never()).deleteById(any());
    }

    @Test
    void reactiver_un_critere_le_remet_visible() {
        CritereAdmissionPersonnalise critere = new CritereAdmissionPersonnalise();
        critere.setId(ID_CRITERE);
        critere.setActif(false);
        when(critereRepository.findById(ID_CRITERE)).thenReturn(Optional.of(critere));

        CritereAdmissionResponse reponse = service.reactiverCritere(ID_CRITERE);

        assertTrue(reponse.isActif());
    }

    @Test
    void modifier_un_critere_inexistant_est_signale() {
        when(critereRepository.findById(ID_CRITERE)).thenReturn(Optional.empty());
        CritereAdmissionRequest request = new CritereAdmissionRequest();
        request.setLibelle("X");
        request.setType(EnumTypeCritere.TEXTE);

        assertThrows(ResourceNotFoundException.class, () -> service.modifierCritere(ID_CRITERE, request));
    }

    @Test
    void modifier_un_critere_met_a_jour_ses_champs() {
        CritereAdmissionPersonnalise critere = new CritereAdmissionPersonnalise();
        critere.setId(ID_CRITERE);
        when(critereRepository.findById(ID_CRITERE)).thenReturn(Optional.of(critere));
        CritereAdmissionRequest request = new CritereAdmissionRequest();
        request.setLibelle("Note minimale au concours");
        request.setType(EnumTypeCritere.NUMERIQUE);
        request.setObligatoire(true);
        request.setValeurMinimale(12);
        request.setOrdre(2);

        CritereAdmissionResponse reponse = service.modifierCritere(ID_CRITERE, request);

        assertEquals("Note minimale au concours", reponse.getLibelle());
        assertEquals(EnumTypeCritere.NUMERIQUE, reponse.getType());
        assertEquals(12, reponse.getValeurMinimale());
        assertEquals(2, reponse.getOrdre());
    }
}
