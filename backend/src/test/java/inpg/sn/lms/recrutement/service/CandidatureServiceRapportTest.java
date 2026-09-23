package inpg.sn.lms.recrutement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.CandidatRepository;
import inpg.sn.lms.recrutement.dto.RapportRecrutementDto;
import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.SessionCandidature;
import inpg.sn.lms.recrutement.enums.EnumStatutCandidature;
import inpg.sn.lms.recrutement.repository.CandidatureRepository;
import inpg.sn.lms.recrutement.repository.SessionCandidatureRepository;

/**
 * Rapport qualité du recrutement : répartition des candidatures par étape du
 * workflow, avec liste nominative — sert à la fois l'écran et le PDF
 * téléchargeable (voir {@code RapportRecrutementPdfService}).
 */
@ExtendWith(MockitoExtension.class)
class CandidatureServiceRapportTest {

    @Mock private CandidatureRepository candidatureRepository;
    @Mock private SessionCandidatureRepository sessionCandidatureRepository;
    @Mock private CandidatRepository candidatRepository;

    @InjectMocks private CandidatureService service;

    private SessionCandidature session;

    private SessionCandidature session() {
        if (session == null) {
            session = new SessionCandidature();
            session.setId(1L);
            session.setLibelle("Rentrée 2026");
        }
        return session;
    }

    private Candidature candidature(String numeroDossier, EnumStatutCandidature statut, String motifRejet) {
        User user = new User();
        user.setEmail(numeroDossier.toLowerCase() + "@example.com");
        Candidat candidat = new Candidat();
        candidat.setNom("Nom-" + numeroDossier);
        candidat.setPrenom("Prenom-" + numeroDossier);
        candidat.setUser(user);

        Candidature c = new Candidature();
        c.setNumeroDossier(numeroDossier);
        c.setStatut(statut);
        c.setDateSoumission(LocalDateTime.now());
        c.setMotifRejet(motifRejet);
        c.setCandidat(candidat);
        c.setSessionCandidature(session());
        return c;
    }

    @Test
    @DisplayName("Sans aucune candidature : total à zéro, les 6 étapes présentes mais vides")
    void sans_candidature_les_etapes_sont_vides() {
        when(candidatureRepository.findAll()).thenReturn(List.of());

        RapportRecrutementDto rapport = service.genererRapportParEtape();

        assertThat(rapport.totalCandidatures()).isZero();
        assertThat(rapport.etapes()).hasSize(11);
        assertThat(rapport.etapes()).allSatisfy(e -> {
            assertThat(e.nombre()).isZero();
            assertThat(e.candidats()).isEmpty();
        });
    }

    @Test
    @DisplayName("Regroupe correctement par étape, BROUILLON exclu du total")
    void regroupe_par_etape_avec_les_bons_comptes() {
        when(candidatureRepository.findAll()).thenReturn(List.of(
                candidature("D1", EnumStatutCandidature.SOUMIS, null),
                candidature("D2", EnumStatutCandidature.SOUMIS, null),
                candidature("D3", EnumStatutCandidature.PRESELECTIONNE, null),
                candidature("D4", EnumStatutCandidature.CONVOQUE_TEST, null),
                candidature("D5", EnumStatutCandidature.ADMIS, null),
                candidature("D6", EnumStatutCandidature.NON_ADMIS, "Dossier incomplet")));

        RapportRecrutementDto rapport = service.genererRapportParEtape();

        assertThat(rapport.totalCandidatures()).isEqualTo(6);
        assertThat(etape(rapport, "SOUMIS").nombre()).isEqualTo(2);
        assertThat(etape(rapport, "PRESELECTIONNE").nombre()).isEqualTo(1);
        assertThat(etape(rapport, "CONVOQUE_TEST").nombre()).isEqualTo(1);
        assertThat(etape(rapport, "CONVOQUE_ENTRETIEN").nombre()).isZero();
        assertThat(etape(rapport, "ADMIS").nombre()).isEqualTo(1);
        assertThat(etape(rapport, "NON_ADMIS").nombre()).isEqualTo(1);
        assertThat(etape(rapport, "ADMIS").candidats().get(0).numeroDossier()).isEqualTo("D5");
    }

    @Test
    @DisplayName("Les étapes apparaissent dans l'ordre du workflow, pas l'ordre d'insertion en base")
    void ordonne_les_etapes_dans_l_ordre_du_workflow() {
        when(candidatureRepository.findAll()).thenReturn(List.of(
                candidature("D1", EnumStatutCandidature.NON_ADMIS, "Non retenu"),
                candidature("D2", EnumStatutCandidature.SOUMIS, null)));

        RapportRecrutementDto rapport = service.genererRapportParEtape();

        assertThat(rapport.etapes()).extracting(RapportRecrutementDto.EtapeRapport::statut)
                .containsExactly("SOUMIS", "PRESELECTIONNE", "PAIEMENT_EN_ATTENTE", "PAIEMENT_CONFIRME",
                        "CONVOQUE_TEST", "TEST_REUSSI", "CONVOQUE_ENTRETIEN", "ENTRETIEN_EFFECTUE",
                        "ADMIS", "LISTE_ATTENTE", "NON_ADMIS");
    }

    @Test
    @DisplayName("Le motif de rejet n'apparaît que sur l'étape REJETE")
    void motif_de_rejet_uniquement_sur_l_etape_rejetee() {
        when(candidatureRepository.findAll()).thenReturn(List.of(
                candidature("D1", EnumStatutCandidature.NON_ADMIS, "Critères non remplis"),
                candidature("D2", EnumStatutCandidature.SOUMIS, null)));

        RapportRecrutementDto rapport = service.genererRapportParEtape();

        assertThat(etape(rapport, "NON_ADMIS").candidats().get(0).motifRejet()).isEqualTo("Critères non remplis");
        assertThat(etape(rapport, "SOUMIS").candidats().get(0).motifRejet()).isNull();
    }

    private RapportRecrutementDto.EtapeRapport etape(RapportRecrutementDto rapport, String statut) {
        return rapport.etapes().stream().filter(e -> e.statut().equals(statut)).findFirst().orElseThrow();
    }
}
