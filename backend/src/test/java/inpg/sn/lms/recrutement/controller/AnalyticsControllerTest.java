package inpg.sn.lms.recrutement.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.Paiement;
import inpg.sn.lms.recrutement.enums.EnumStatutCandidature;
import inpg.sn.lms.recrutement.enums.EnumStatutPaiement;
import inpg.sn.lms.recrutement.repository.CandidatureRepository;
import inpg.sn.lms.recrutement.repository.EntretienRepository;
import inpg.sn.lms.recrutement.repository.PaiementRepository;

/**
 * Avant cette story, plusieurs endpoints renvoyaient des valeurs codées en
 * dur, et {@code getKpis} comparait le statut d'un paiement à la chaîne
 * {@code "SUCCESS"} — qui ne correspond à aucune valeur de {@link
 * EnumStatutPaiement} ({@code SUCCES}) — donc les frais collectés valaient
 * toujours 0. Ces tests vérifient que les montants et l'entonnoir sont bien
 * calculés depuis les repositories, avec la bonne valeur d'enum.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    @Mock
    private CandidatureRepository candidatureRepo;
    @Mock
    private PaiementRepository paiementRepo;
    @Mock
    private EntretienRepository entretienRepo;

    private AnalyticsController controller;

    @BeforeEach
    void setUp() {
        controller = new AnalyticsController(candidatureRepo, paiementRepo, entretienRepo);
    }

    private Candidature candidature(EnumStatutCandidature statut, String nationalite, Paiement paiement) {
        Candidature c = new Candidature();
        c.setStatut(statut);
        c.setDateSoumission(LocalDateTime.now());
        Candidat candidat = new Candidat();
        candidat.setNationalite(nationalite);
        c.setCandidat(candidat);
        c.setPaiement(paiement);
        return c;
    }

    private Paiement paiement(EnumStatutPaiement statut) {
        Paiement p = new Paiement();
        p.setStatut(statut);
        p.setMontant(BigDecimal.valueOf(75000));
        p.setDevise("XOF");
        return p;
    }

    @Test
    @DisplayName("Frais collectés : seuls les paiements SUCCES comptent, pas de valeur fictive de repli")
    void kpis_somme_uniquement_les_paiements_reussis() {
        when(candidatureRepo.findAll()).thenReturn(List.of(
                candidature(EnumStatutCandidature.SOUMIS, "Sénégal", paiement(EnumStatutPaiement.SUCCES)),
                candidature(EnumStatutCandidature.SOUMIS, "Mali", paiement(EnumStatutPaiement.EN_ATTENTE))));
        when(paiementRepo.findAll()).thenReturn(List.of(
                paiement(EnumStatutPaiement.SUCCES), paiement(EnumStatutPaiement.EN_ATTENTE)));

        Map<String, Object> kpis = controller.getKpis().getBody();

        assertThat(kpis).isNotNull();
        assertThat((Number) kpis.get("fraisCollectes")).isEqualTo(75000L);
    }

    @Test
    @DisplayName("Entonnoir de conversion : décompte réel par statut, jamais de nombre fixe")
    void entonnoir_reflete_les_statuts_reels() {
        when(candidatureRepo.findAll()).thenReturn(List.of(
                candidature(EnumStatutCandidature.BROUILLON, "Sénégal", null),
                candidature(EnumStatutCandidature.SOUMIS, "Sénégal", paiement(EnumStatutPaiement.SUCCES)),
                candidature(EnumStatutCandidature.ADMIS1, "Sénégal", paiement(EnumStatutPaiement.SUCCES)),
                candidature(EnumStatutCandidature.ADMIS2, "Sénégal", paiement(EnumStatutPaiement.SUCCES)),
                candidature(EnumStatutCandidature.ADMIS3, "Sénégal", paiement(EnumStatutPaiement.SUCCES))));

        List<Map<String, Object>> funnel = controller.conversionFunnel().getBody();

        assertThat(funnel).isNotNull();
        // BROUILLON exclu : 4 dossiers réellement déposés.
        assertThat(funnel.get(0).get("nombre")).isEqualTo(4L);
        assertThat(funnel.get(1).get("nombre")).isEqualTo(4L); // Payés
        assertThat(funnel.get(2).get("nombre")).isEqualTo(3L); // Admissibles (ADMIS1+)
        assertThat(funnel.get(3).get("nombre")).isEqualTo(2L); // Entretiens (ADMIS2+)
        assertThat(funnel.get(4).get("nombre")).isEqualTo(1L); // Inscrits (ADMIS3)
    }

    @Test
    @DisplayName("Répartition géographique : regroupée par nationalité réelle, au-delà de 4 pays regroupés en Autres")
    void repartition_geographique_regroupe_au_dela_de_quatre_pays() {
        when(candidatureRepo.findAll()).thenReturn(List.of(
                candidature(EnumStatutCandidature.SOUMIS, "Sénégal", null),
                candidature(EnumStatutCandidature.SOUMIS, "Sénégal", null),
                candidature(EnumStatutCandidature.SOUMIS, "Mali", null),
                candidature(EnumStatutCandidature.SOUMIS, "Côte d'Ivoire", null),
                candidature(EnumStatutCandidature.SOUMIS, "Bénin", null),
                candidature(EnumStatutCandidature.SOUMIS, "Togo", null)));

        List<Map<String, Object>> repartition = controller.geographicDistribution().getBody();

        assertThat(repartition).isNotNull();
        assertThat(repartition).hasSize(5); // 4 pays + "Autres"
        assertThat(repartition.get(0)).containsEntry("pays", "Sénégal").containsEntry("effectif", 2L);
        assertThat(repartition.stream().anyMatch(e -> "Autres".equals(e.get("pays")) && e.get("effectif").equals(1L)))
                .isTrue();
    }

    @Test
    @DisplayName("Aucune candidature : pas de donnée fictive, listes/valeurs vides ou nulles")
    void sans_donnees_aucune_valeur_fictive() {
        when(candidatureRepo.findAll()).thenReturn(List.of());
        when(paiementRepo.findAll()).thenReturn(List.of());

        Map<String, Object> kpis = controller.getKpis().getBody();
        List<Map<String, Object>> repartition = controller.geographicDistribution().getBody();

        assertThat(kpis).isNotNull();
        assertThat((Number) kpis.get("fraisCollectes")).isEqualTo(0L);
        assertThat((Number) kpis.get("tauxAdmission")).isEqualTo(0.0);
        assertThat(repartition).isEmpty();
    }
}
