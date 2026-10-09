package inpg.sn.lms.recrutement.service;

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

import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.Paiement;
import inpg.sn.lms.recrutement.enums.EnumStatutCandidature;
import inpg.sn.lms.recrutement.enums.EnumStatutPaiement;
import inpg.sn.lms.recrutement.repository.CandidatureRepository;
import inpg.sn.lms.recrutement.repository.PaiementRepository;

/**
 * Relance des dossiers validés mais impayés (TDR : suivi des paiements).
 *
 * <p>{@code getAllPaiements()} seul ne peut pas servir de base à cette relance : il ne
 * liste que les {@link Paiement} déjà créés, jamais les candidatures n'ayant fait aucune
 * tentative. Ces tests figent le vrai périmètre : quels statuts de dossier sont concernés,
 * et que l'absence de paiement compte comme impayé au même titre qu'un échec.
 */
@ExtendWith(MockitoExtension.class)
class PaiementServiceRelanceTest {

    @Mock private PaiementRepository paiementRepository;
    @Mock private CandidatureRepository candidatureRepository;
    @Mock private NotificationService notificationService;
    @Mock private NotificationCandidatService notificationCandidatService;
    @Mock private RecuPaiementService recuPaiementService;

    @InjectMocks private PaiementService paiementService;

    private Candidature candidature(long id, EnumStatutCandidature statut, Paiement paiement, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        Candidat candidat = new Candidat();
        candidat.setId(id);
        candidat.setUser(user);

        Candidature c = new Candidature();
        c.setId(id);
        c.setNumeroDossier("INPG-TEST" + id);
        c.setStatut(statut);
        c.setCandidat(candidat);
        if (paiement != null) {
            paiement.setCandidature(c);
            c.setPaiement(paiement);
        }
        return c;
    }

    private Paiement paiement(EnumStatutPaiement statut) {
        Paiement p = new Paiement();
        p.setStatut(statut);
        return p;
    }

    @Test
    void un_dossier_encore_soumis_nest_pas_relance() {
        when(candidatureRepository.findAll()).thenReturn(
                List.of(candidature(1L, EnumStatutCandidature.SOUMIS, null, "a@example.sn")));

        Map<String, Object> resultat = paiementService.relancerImpayes();

        assertThat(resultat.get("envoyes")).isEqualTo(0);
        verify(notificationService, never()).notifierRappelPaiement(anyString(), anyString());
    }

    @Test
    void un_dossier_rejete_nest_pas_relance_meme_impaye() {
        when(candidatureRepository.findAll()).thenReturn(
                List.of(candidature(1L, EnumStatutCandidature.DOSSIER_REJETE, null, "a@example.sn")));

        Map<String, Object> resultat = paiementService.relancerImpayes();

        assertThat(resultat.get("envoyes")).isEqualTo(0);
    }

    @Test
    void un_dossier_deja_paye_nest_pas_relance() {
        when(candidatureRepository.findAll()).thenReturn(List.of(
                candidature(1L, EnumStatutCandidature.CONVOQUE_TEST, paiement(EnumStatutPaiement.SUCCES), "a@example.sn")));

        Map<String, Object> resultat = paiementService.relancerImpayes();

        assertThat(resultat.get("envoyes")).isEqualTo(0);
        verify(notificationService, never()).notifierRappelPaiement(anyString(), anyString());
    }

    /**
     * Les frais ne se paient qu'après la présélection, et la convocation au test vient après le
     * paiement confirmé : un dossier déjà convoqué ou admis n'est jamais « impayé », même sans
     * ligne de paiement (donnée héritée). Le relancer enverrait un rappel absurde.
     */
    @Test
    void un_dossier_au_dela_du_paiement_nest_jamais_relance() {
        when(candidatureRepository.findAll()).thenReturn(List.of(
                candidature(1L, EnumStatutCandidature.CONVOQUE_TEST, null, "a@example.sn"),
                candidature(2L, EnumStatutCandidature.CONVOQUE_ENTRETIEN, null, "b@example.sn"),
                candidature(3L, EnumStatutCandidature.ADMIS, null, "c@example.sn")));

        Map<String, Object> resultat = paiementService.relancerImpayes();

        assertThat(resultat.get("envoyes")).isEqualTo(0);
        verify(notificationService, never()).notifierRappelPaiement(anyString(), anyString());
    }

    /** Un candidat n'ayant jamais initié de paiement (aucune ligne Paiement) est bien impayé. */
    @Test
    void un_dossier_valide_sans_aucune_tentative_de_paiement_est_relance() {
        Candidature c = candidature(1L, EnumStatutCandidature.PRESELECTIONNE, null, "a@example.sn");
        when(candidatureRepository.findAll()).thenReturn(List.of(c));

        Map<String, Object> resultat = paiementService.relancerImpayes();

        assertThat(resultat.get("envoyes")).isEqualTo(1);
        verify(notificationService).notifierRappelPaiement("a@example.sn", "INPG-TEST1");
        verify(notificationCandidatService).creerNotification(anyLong(), anyString(), anyString(), anyString());
    }

    /** Après une tentative échouée, le dossier reste en PAIEMENT_EN_ATTENTE : il doit être relancé. */
    @Test
    void un_dossier_en_attente_de_paiement_avec_tentative_en_echec_est_relance() {
        when(candidatureRepository.findAll()).thenReturn(List.of(
                candidature(2L, EnumStatutCandidature.PAIEMENT_EN_ATTENTE, paiement(EnumStatutPaiement.ECHEC), "b@example.sn")));

        Map<String, Object> resultat = paiementService.relancerImpayes();

        assertThat(resultat.get("envoyes")).isEqualTo(1);
        verify(notificationService).notifierRappelPaiement("b@example.sn", "INPG-TEST2");
    }

    @Test
    void un_candidat_sans_adresse_email_est_compte_en_echec_pas_en_exception() {
        when(candidatureRepository.findAll()).thenReturn(
                List.of(candidature(3L, EnumStatutCandidature.PRESELECTIONNE, null, null)));

        Map<String, Object> resultat = paiementService.relancerImpayes();

        assertThat(resultat.get("envoyes")).isEqualTo(0);
        @SuppressWarnings("unchecked")
        List<String> echecs = (List<String>) resultat.get("echecs");
        assertThat(echecs).hasSize(1);
        verify(notificationService, never()).notifierRappelPaiement(anyString(), anyString());
    }

    @Test
    void plusieurs_dossiers_eligibles_sont_tous_relances() {
        when(candidatureRepository.findAll()).thenReturn(List.of(
                candidature(1L, EnumStatutCandidature.PRESELECTIONNE, null, "a@example.sn"),
                candidature(2L, EnumStatutCandidature.PAIEMENT_EN_ATTENTE, paiement(EnumStatutPaiement.EN_ATTENTE), "b@example.sn"),
                candidature(3L, EnumStatutCandidature.PAIEMENT_CONFIRME, paiement(EnumStatutPaiement.SUCCES), "c@example.sn"),
                candidature(4L, EnumStatutCandidature.SOUMIS, null, "d@example.sn")));

        Map<String, Object> resultat = paiementService.relancerImpayes();

        assertThat(resultat.get("envoyes")).isEqualTo(2);
        verify(notificationService, times(2)).notifierRappelPaiement(anyString(), anyString());
    }
}
