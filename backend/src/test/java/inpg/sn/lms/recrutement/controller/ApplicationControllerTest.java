package inpg.sn.lms.recrutement.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.PieceJustificative;
import inpg.sn.lms.recrutement.enums.EnumTypeDocument;
import inpg.sn.lms.recrutement.repository.CandidatureRepository;
import inpg.sn.lms.recrutement.service.NotificationCandidatService;
import inpg.sn.lms.recrutement.service.NotificationService;

/**
 * Relance des dossiers incomplets (TDR : « aux candidats n'ayant pas un dossier complet,
 * de pouvoir revenir plus tard » — encore fallait-il les prévenir). L'endpoint ne faisait
 * jusqu'ici que compter les dossiers concernés sans jamais rien envoyer.
 */
@ExtendWith(MockitoExtension.class)
class ApplicationControllerTest {

    @Mock private CandidatureRepository candidatureRepo;
    @Mock private NotificationService notificationService;
    @Mock private NotificationCandidatService notificationCandidatService;

    @InjectMocks private ApplicationController controller;

    private Candidature candidature(long id, String email, List<PieceJustificative> pieces) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        Candidat candidat = new Candidat();
        candidat.setId(id);
        candidat.setUser(user);

        Candidature c = new Candidature();
        c.setId(id);
        c.setNumeroDossier("INPG-TEST" + id);
        c.setCandidat(candidat);
        c.setPiecesJustificatives(pieces);
        return c;
    }

    private PieceJustificative piece(EnumTypeDocument type, boolean conforme) {
        PieceJustificative p = new PieceJustificative();
        p.setTypeDocument(type);
        p.setEstConforme(conforme);
        return p;
    }

    @Test
    void un_dossier_complet_nest_pas_relance() {
        when(candidatureRepo.findAll()).thenReturn(List.of(
                candidature(1L, "a@example.sn", List.of(piece(EnumTypeDocument.CV, true)))));

        ResponseEntity<Map<String, Object>> reponse = controller.sendReminder();

        assertThat(reponse.getBody().get("envoyes")).isEqualTo(0);
        verify(notificationService, never()).notifierRappelDossierIncomplet(anyString(), anyString(), anyString());
    }

    @Test
    void un_dossier_sans_aucune_piece_est_relance() {
        when(candidatureRepo.findAll()).thenReturn(List.of(candidature(1L, "a@example.sn", List.of())));

        ResponseEntity<Map<String, Object>> reponse = controller.sendReminder();

        assertThat(reponse.getBody().get("envoyes")).isEqualTo(1);
        verify(notificationService).notifierRappelDossierIncomplet("a@example.sn", "INPG-TEST1", "aucune pièce déposée");
        verify(notificationCandidatService).creerNotification(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void un_dossier_avec_une_piece_non_conforme_nomme_la_piece_dans_la_relance() {
        when(candidatureRepo.findAll()).thenReturn(List.of(candidature(1L, "a@example.sn", List.of(
                piece(EnumTypeDocument.CV, true),
                piece(EnumTypeDocument.DIPLOME, false)))));

        controller.sendReminder();

        verify(notificationService).notifierRappelDossierIncomplet("a@example.sn", "INPG-TEST1", "Diplôme");
    }

    @Test
    void un_candidat_sans_adresse_email_est_compte_en_echec_pas_en_exception() {
        when(candidatureRepo.findAll()).thenReturn(List.of(candidature(1L, null, List.of())));

        ResponseEntity<Map<String, Object>> reponse = controller.sendReminder();

        assertThat(reponse.getBody().get("envoyes")).isEqualTo(0);
        @SuppressWarnings("unchecked")
        List<String> echecs = (List<String>) reponse.getBody().get("echecs");
        assertThat(echecs).hasSize(1);
        verify(notificationService, never()).notifierRappelDossierIncomplet(anyString(), anyString(), anyString());
    }

    @Test
    void plusieurs_dossiers_incomplets_sont_tous_relances() {
        when(candidatureRepo.findAll()).thenReturn(List.of(
                candidature(1L, "a@example.sn", List.of()),
                candidature(2L, "b@example.sn", List.of(piece(EnumTypeDocument.CV, false))),
                candidature(3L, "c@example.sn", List.of(piece(EnumTypeDocument.CV, true)))));

        ResponseEntity<Map<String, Object>> reponse = controller.sendReminder();

        assertThat(reponse.getBody().get("envoyes")).isEqualTo(2);
        verify(notificationService, times(2))
                .notifierRappelDossierIncomplet(anyString(), anyString(), anyString());
    }
}
