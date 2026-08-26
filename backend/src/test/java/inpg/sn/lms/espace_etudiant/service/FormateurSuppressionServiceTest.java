package inpg.sn.lms.espace_etudiant.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import inpg.sn.lms.auth.entity.AlumniProfil;
import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.auth.entity.Formateur;
import inpg.sn.lms.auth.repository.AlumniProfilRepository;
import inpg.sn.lms.auth.repository.CandidatRepository;
import inpg.sn.lms.auth.repository.FormateurRepository;
import inpg.sn.lms.auth.repository.UserRepository;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.repository.CoursRepository;
import inpg.sn.lms.espace_etudiant.repository.ForumMessageRepository;
import inpg.sn.lms.espace_etudiant.repository.ForumSujetRepository;

/**
 * {@code DELETE /api/pedagogie/formateurs/{id}} — voir FormateurSuppressionService.
 *
 * <p>Couvre le refus propre au formateur (seul titulaire d'un cours — {@code
 * cours_formateur} commande tout le cloisonnement enseignant, CLAUDE.md §3), les
 * trois refus partagés avec l'étudiant, et le chemin nominal qui doit détacher le
 * formateur des cours qu'il partage avec d'autres avant de le supprimer.
 */
@ExtendWith(MockitoExtension.class)
class FormateurSuppressionServiceTest {

    private static final Long ID = 7L;

    @Mock
    private FormateurRepository formateurRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CoursRepository coursRepository;
    @Mock
    private CandidatRepository candidatRepository;
    @Mock
    private AlumniProfilRepository alumniProfilRepository;
    @Mock
    private ForumSujetRepository forumSujetRepository;
    @Mock
    private ForumMessageRepository forumMessageRepository;

    private FormateurSuppressionService service;

    private Formateur formateur(long id) {
        Formateur f = new Formateur();
        f.setId(id);
        return f;
    }

    private Cours cours(String titre, Formateur... titulaires) {
        Cours c = new Cours();
        c.setTitre(titre);
        c.getFormateurs().addAll(List.of(titulaires));
        return c;
    }

    @BeforeEach
    void setUp() {
        service = new FormateurSuppressionService(formateurRepository, userRepository, coursRepository,
                candidatRepository, alumniProfilRepository, forumSujetRepository, forumMessageRepository);
        lenient().when(coursRepository.findByFormateurs_Id(ID)).thenReturn(List.of());
        lenient().when(candidatRepository.findByUserId(ID)).thenReturn(Optional.empty());
        lenient().when(alumniProfilRepository.findByUserId(ID)).thenReturn(Optional.empty());
        lenient().when(forumSujetRepository.existsByAuteurId(ID)).thenReturn(false);
        lenient().when(forumMessageRepository.existsByAuteurId(ID)).thenReturn(false);
    }

    @Test
    @DisplayName("refuse s'il est seul titulaire d'un cours")
    void refuse_si_seul_titulaire() {
        Formateur lui = formateur(ID);
        when(coursRepository.findByFormateurs_Id(ID)).thenReturn(List.of(cours("Thermodynamique", lui)));

        assertThatThrownBy(() -> service.supprimer(ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Thermodynamique");
        verify(formateurRepository, never()).deleteById(ID);
        verify(userRepository, never()).deleteById(ID);
    }

    @Test
    @DisplayName("refuse si un dossier de candidature existe encore")
    void refuse_si_dossier_candidature() {
        when(candidatRepository.findByUserId(ID)).thenReturn(Optional.of(new Candidat()));

        assertThatThrownBy(() -> service.supprimer(ID)).isInstanceOf(IllegalStateException.class);
        verify(formateurRepository, never()).deleteById(ID);
    }

    @Test
    @DisplayName("refuse si le compte est passé côté alumni")
    void refuse_si_alumni() {
        when(alumniProfilRepository.findByUserId(ID)).thenReturn(Optional.of(new AlumniProfil()));

        assertThatThrownBy(() -> service.supprimer(ID)).isInstanceOf(IllegalStateException.class);
        verify(formateurRepository, never()).deleteById(ID);
    }

    @Test
    @DisplayName("refuse s'il a publié dans le forum")
    void refuse_si_publication_forum() {
        when(forumSujetRepository.existsByAuteurId(ID)).thenReturn(true);

        assertThatThrownBy(() -> service.supprimer(ID)).isInstanceOf(IllegalStateException.class);
        verify(formateurRepository, never()).deleteById(ID);
    }

    @Test
    @DisplayName("détache des cours partagés puis supprime le formateur et l'utilisateur")
    void supprime_et_detache_des_cours_partages() {
        Formateur lui = formateur(ID);
        Formateur collegue = formateur(9L);
        Cours partage = cours("Mécanique des fluides", lui, collegue);
        when(coursRepository.findByFormateurs_Id(ID)).thenReturn(List.of(partage));

        assertThatCode(() -> service.supprimer(ID)).doesNotThrowAnyException();

        org.assertj.core.api.Assertions.assertThat(partage.getFormateurs()).containsExactly(collegue);
        verify(coursRepository).save(partage);
        verify(formateurRepository).deleteById(ID);
        verify(userRepository).deleteById(ID);
    }
}
