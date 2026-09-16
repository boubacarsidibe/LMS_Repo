package inpg.sn.lms.recrutement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
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
import inpg.sn.lms.recrutement.dto.RapportStatistiqueDto;
import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.SessionCandidature;
import inpg.sn.lms.recrutement.enums.EnumStatutCandidature;
import inpg.sn.lms.recrutement.repository.CandidatureRepository;
import inpg.sn.lms.recrutement.repository.SessionCandidatureRepository;

/**
 * Rapport statistique du recrutement : doit recalculer exactement les mêmes
 * agrégats que la page Statistiques du portail admission
 * ({@code responsable-admission-statistiques.component.ts}), afin que le PDF
 * téléchargé corresponde toujours à ce que l'écran affiche.
 */
@ExtendWith(MockitoExtension.class)
class CandidatureServiceRapportStatistiqueTest {

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

    private Candidature candidature(String numeroDossier, EnumStatutCandidature statut, String genre,
            String profession, LocalDate dateNaissance, String nationalite) {
        User user = new User();
        user.setEmail(numeroDossier.toLowerCase() + "@example.com");
        Candidat candidat = new Candidat();
        candidat.setNom("Nom-" + numeroDossier);
        candidat.setPrenom("Prenom-" + numeroDossier);
        candidat.setGenre(genre);
        candidat.setProfession(profession);
        candidat.setDateNaissance(dateNaissance);
        candidat.setNationalite(nationalite);
        candidat.setUser(user);

        Candidature c = new Candidature();
        c.setNumeroDossier(numeroDossier);
        c.setStatut(statut);
        c.setDateSoumission(LocalDateTime.now());
        c.setCandidat(candidat);
        c.setSessionCandidature(session());
        return c;
    }

    @Test
    @DisplayName("Sans aucune candidature : tous les compteurs à zéro, pas de division par zéro")
    void sans_candidature_tout_est_a_zero() {
        when(candidatureRepository.findAll()).thenReturn(List.of());

        RapportStatistiqueDto rapport = service.genererRapportStatistique();

        assertThat(rapport.totalCandidats()).isZero();
        assertThat(rapport.tauxAdmission()).isZero();
        assertThat(rapport.repartitionStatuts()).hasSize(6);
        assertThat(rapport.repartitionStatuts()).allSatisfy(s -> assertThat(s.pourcentage()).isZero());
    }

    @Test
    @DisplayName("Répartition par étape : mêmes compteurs et pourcentages que le rapport qualité")
    void repartition_par_etape_correcte() {
        when(candidatureRepository.findAll()).thenReturn(List.of(
                candidature("D1", EnumStatutCandidature.SOUMIS, "Homme", null, null, null),
                candidature("D2", EnumStatutCandidature.SOUMIS, "Homme", null, null, null),
                candidature("D3", EnumStatutCandidature.EN_COURS_DE_PRESELECTION, "Femme", null, null, null),
                candidature("D4", EnumStatutCandidature.REJETE, "Femme", null, null, null)));

        RapportStatistiqueDto rapport = service.genererRapportStatistique();

        assertThat(rapport.totalCandidats()).isEqualTo(4);
        assertThat(rapport.validationEnCours()).isEqualTo(2);
        assertThat(rapport.preselectionnes()).isEqualTo(1);
        assertThat(rapport.rejetes()).isEqualTo(1);
        assertThat(segment(rapport, rapport.repartitionStatuts(), "Soumis").count()).isEqualTo(2);
        assertThat(segment(rapport, rapport.repartitionStatuts(), "Soumis").pourcentage()).isEqualTo(50);
    }

    @Test
    @DisplayName("Taux d'admission : calculé sur les seuls dossiers tranchés (ADMIS3 + REJETE), pas sur le total")
    void taux_admission_calcule_sur_les_dossiers_tranches() {
        when(candidatureRepository.findAll()).thenReturn(List.of(
                candidature("D1", EnumStatutCandidature.ADMIS3, "Homme", null, null, null),
                candidature("D2", EnumStatutCandidature.REJETE, "Homme", null, null, null),
                candidature("D3", EnumStatutCandidature.SOUMIS, "Femme", null, null, null)));

        RapportStatistiqueDto rapport = service.genererRapportStatistique();

        // 1 admis sur 2 dossiers tranchés (D3 encore en cours n'entre pas dans le calcul) = 50 %, pas 33 %.
        assertThat(rapport.candidaturesDecidees()).isEqualTo(2);
        assertThat(rapport.tauxAdmission()).isEqualTo(50);
    }

    @Test
    @DisplayName("Genre : segments Homme/Femme/Autre toujours présents, « Non renseigné » seulement si nécessaire")
    void repartition_genre_avec_non_renseigne() {
        when(candidatureRepository.findAll()).thenReturn(List.of(
                candidature("D1", EnumStatutCandidature.SOUMIS, "Homme", null, null, null),
                candidature("D2", EnumStatutCandidature.SOUMIS, null, null, null, null)));

        RapportStatistiqueDto rapport = service.genererRapportStatistique();

        assertThat(rapport.repartitionGenre()).extracting(RapportStatistiqueDto.Segment::label)
                .containsExactly("Homme", "Femme", "Autre", "Non renseigné");
        assertThat(segment(rapport, rapport.repartitionGenre(), "Homme").count()).isEqualTo(1);
        assertThat(segment(rapport, rapport.repartitionGenre(), "Non renseigné").count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Âge : calculé depuis la date de naissance, candidats sans date ignorés sans erreur")
    void repartition_age_calculee_depuis_la_date_de_naissance() {
        LocalDate ilYa22Ans = LocalDate.now().minusYears(22);
        LocalDate ilYa45Ans = LocalDate.now().minusYears(45);
        when(candidatureRepository.findAll()).thenReturn(List.of(
                candidature("D1", EnumStatutCandidature.SOUMIS, "Homme", null, ilYa22Ans, null),
                candidature("D2", EnumStatutCandidature.SOUMIS, "Femme", null, ilYa45Ans, null),
                candidature("D3", EnumStatutCandidature.SOUMIS, "Femme", null, null, null)));

        RapportStatistiqueDto rapport = service.genererRapportStatistique();

        assertThat(segment(rapport, rapport.repartitionAge(), "20-25 ans").count()).isEqualTo(1);
        assertThat(segment(rapport, rapport.repartitionAge(), "> 40 ans").count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Professions : regroupées sans tenir compte de la casse, réaffichées avec une seule majuscule")
    void professions_regroupees_sans_tenir_compte_de_la_casse() {
        when(candidatureRepository.findAll()).thenReturn(List.of(
                candidature("D1", EnumStatutCandidature.SOUMIS, "Homme", "Ingénieur", null, null),
                candidature("D2", EnumStatutCandidature.SOUMIS, "Femme", "ingénieur", null, null),
                candidature("D3", EnumStatutCandidature.SOUMIS, "Femme", "  ", null, null)));

        RapportStatistiqueDto rapport = service.genererRapportStatistique();

        assertThat(rapport.topProfessions()).hasSize(1);
        assertThat(rapport.topProfessions().get(0).label()).isEqualTo("Ingénieur");
        assertThat(rapport.topProfessions().get(0).count()).isEqualTo(2);
        assertThat(rapport.nbSansProfessionDeclaree()).isEqualTo(1);
    }

    @Test
    @DisplayName("Nationalités : jamais transformées, contrairement aux professions")
    void nationalites_jamais_transformees() {
        when(candidatureRepository.findAll()).thenReturn(List.of(
                candidature("D1", EnumStatutCandidature.SOUMIS, "Homme", null, null, "Sénégalaise"),
                candidature("D2", EnumStatutCandidature.SOUMIS, "Femme", null, null, "Sénégalaise")));

        RapportStatistiqueDto rapport = service.genererRapportStatistique();

        assertThat(rapport.topNationalites().get(0).label()).isEqualTo("Sénégalaise");
        assertThat(rapport.topNationalites().get(0).count()).isEqualTo(2);
    }

    private RapportStatistiqueDto.Segment segment(RapportStatistiqueDto rapport,
            List<RapportStatistiqueDto.Segment> segments, String label) {
        return segments.stream().filter(s -> s.label().equals(label)).findFirst()
                .orElseThrow(() -> new AssertionError("Segment introuvable : " + label));
    }
}
