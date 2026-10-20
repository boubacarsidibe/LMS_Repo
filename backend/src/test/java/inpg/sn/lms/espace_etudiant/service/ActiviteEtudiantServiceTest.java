package inpg.sn.lms.espace_etudiant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.ProgressionSupport;
import inpg.sn.lms.espace_etudiant.repository.ProgressionSupportRepository;

/**
 * Les écrans de suivi du formateur et du responsable pédagogique affichaient « 0 % » de progression pour
 * tout le monde et « Non renseignée » en dernière connexion — deux valeurs codées en dur — alors que
 * l'étudiant voyait, de son côté, sa progression réelle.
 */
@ExtendWith(MockitoExtension.class)
class ActiviteEtudiantServiceTest {

    private static final long ETUDIANT = 38L;

    @Mock private HabilitationCoursService habilitation;
    @Mock private SupportService supportService;
    @Mock private ProgressionSupportRepository progressionRepository;

    private ActiviteEtudiantService service;
    private Etudiant etudiant;

    @BeforeEach
    void preparer() {
        service = new ActiviteEtudiantService(habilitation, supportService, progressionRepository);
        etudiant = new Etudiant();
        etudiant.setId(ETUDIANT);
    }

    private static Cours cours(long id) {
        Cours c = new Cours();
        // Pas de setter public sur l'identifiant : même patron que les autres tests de ce module.
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    private void supports(long coursId, long publies, long consultes) {
        when(supportService.countCourseSupports(coursId)).thenReturn(publies);
        if (publies > 0) {
            when(progressionRepository.countByEtudiantIdAndSupport_Cours_Id(ETUDIANT, coursId)).thenReturn(consultes);
        }
    }

    @Test
    @DisplayName("La progression est la moyenne des pourcentages par cours, comme sur l'écran de l'étudiant")
    void progression_moyenne_des_cours() {
        when(habilitation.coursAutorises(etudiant)).thenReturn(List.of(cours(1), cours(2), cours(3)));
        supports(1, 2, 1);   //  50 %
        supports(2, 2, 2);   // 100 %
        supports(3, 4, 0);   //   0 %

        assertThat(service.progression(etudiant)).isEqualTo(50);
    }

    @Test
    @DisplayName("Un cours sans aucun support compte pour 0 %, comme dans l'espace étudiant")
    void cours_sans_support_vaut_zero() {
        when(habilitation.coursAutorises(etudiant)).thenReturn(List.of(cours(1), cours(2)));
        supports(1, 3, 3);   // 100 %
        supports(2, 0, 0);   //   0 %

        assertThat(service.progression(etudiant)).isEqualTo(50);
    }

    @Test
    @DisplayName("Tous les supports consultés donnent 100 %, jamais 0")
    void tout_consulte_vaut_cent() {
        when(habilitation.coursAutorises(etudiant)).thenReturn(List.of(cours(1)));
        supports(1, 2, 2);

        assertThat(service.progression(etudiant)).isEqualTo(100);
    }

    @Test
    @DisplayName("Aucun cours autorisé : 0 %, sans diviser par zéro")
    void aucun_cours_autorise() {
        when(habilitation.coursAutorises(etudiant)).thenReturn(List.of());

        assertThat(service.progression(etudiant)).isZero();
    }

    @Test
    @DisplayName("La dernière activité est la date du dernier support ouvert")
    void derniere_activite_datee() {
        ProgressionSupport dernier = new ProgressionSupport();
        dernier.setDateConsultation(LocalDateTime.of(2026, 10, 19, 16, 13));
        when(progressionRepository.findFirstByEtudiantIdOrderByDateConsultationDesc(ETUDIANT))
                .thenReturn(Optional.of(dernier));

        assertThat(service.derniereActivite(etudiant)).isEqualTo("19 oct. 2026");
    }

    @Test
    @DisplayName("Sans support ouvert : « Aucune activité » — jamais une fausse « dernière connexion »")
    void derniere_activite_absente() {
        when(progressionRepository.findFirstByEtudiantIdOrderByDateConsultationDesc(ETUDIANT))
                .thenReturn(Optional.empty());

        assertThat(service.derniereActivite(etudiant)).isEqualTo(ActiviteEtudiantService.AUCUNE_ACTIVITE);
    }
}
