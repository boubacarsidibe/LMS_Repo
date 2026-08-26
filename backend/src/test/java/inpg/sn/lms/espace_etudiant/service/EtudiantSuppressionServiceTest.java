package inpg.sn.lms.espace_etudiant.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import inpg.sn.lms.auth.entity.AlumniProfil;
import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.auth.repository.AlumniProfilRepository;
import inpg.sn.lms.auth.repository.CandidatRepository;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.auth.repository.UserRepository;
import inpg.sn.lms.espace_etudiant.repository.AnnonceLectureRepository;
import inpg.sn.lms.espace_etudiant.repository.CertificatRepository;
import inpg.sn.lms.espace_etudiant.repository.ForumMessageRepository;
import inpg.sn.lms.espace_etudiant.repository.ForumSujetRepository;
import inpg.sn.lms.espace_etudiant.repository.NoteModuleRepository;
import inpg.sn.lms.espace_etudiant.repository.PresenceRepository;
import inpg.sn.lms.espace_etudiant.repository.ProgressionSupportRepository;
import inpg.sn.lms.espace_etudiant.repository.ReleveNotesRepository;
import inpg.sn.lms.espace_etudiant.repository.RetourEvaluationRepository;

/**
 * {@code DELETE /api/pedagogie/etudiants/{id}} — voir EtudiantSuppressionService.
 *
 * <p>Couvre les trois refus (dossier de candidature, historique alumni, publication
 * au forum) et le chemin nominal, qui doit vider chaque table dépendante avant de
 * retirer les lignes {@code etudiants} puis {@code users} : aucune de ces tables
 * n'a de suppression en cascade côté base (V1__baseline.sql), un maillon oublié
 * ferait échouer la suppression avec une violation de clé étrangère brute.
 */
@ExtendWith(MockitoExtension.class)
class EtudiantSuppressionServiceTest {

    private static final Long ID = 42L;

    @Mock
    private EtudiantRepository etudiantRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CandidatRepository candidatRepository;
    @Mock
    private AlumniProfilRepository alumniProfilRepository;
    @Mock
    private ForumSujetRepository forumSujetRepository;
    @Mock
    private ForumMessageRepository forumMessageRepository;
    @Mock
    private CertificatRepository certificatRepository;
    @Mock
    private ReleveNotesRepository releveNotesRepository;
    @Mock
    private RetourEvaluationRepository retourEvaluationRepository;
    @Mock
    private NoteModuleRepository noteModuleRepository;
    @Mock
    private PresenceRepository presenceRepository;
    @Mock
    private ProgressionSupportRepository progressionSupportRepository;
    @Mock
    private AnnonceLectureRepository annonceLectureRepository;

    private EtudiantSuppressionService service;

    @BeforeEach
    void setUp() {
        service = new EtudiantSuppressionService(etudiantRepository, userRepository, candidatRepository,
                alumniProfilRepository, forumSujetRepository, forumMessageRepository, certificatRepository,
                releveNotesRepository, retourEvaluationRepository, noteModuleRepository, presenceRepository,
                progressionSupportRepository, annonceLectureRepository);
        lenient().when(candidatRepository.findByUserId(ID)).thenReturn(Optional.empty());
        lenient().when(alumniProfilRepository.findByUserId(ID)).thenReturn(Optional.empty());
        lenient().when(forumSujetRepository.existsByAuteurId(ID)).thenReturn(false);
        lenient().when(forumMessageRepository.existsByAuteurId(ID)).thenReturn(false);
    }

    @Test
    @DisplayName("refuse si un dossier de candidature existe encore")
    void refuse_si_dossier_candidature() {
        when(candidatRepository.findByUserId(ID)).thenReturn(Optional.of(new Candidat()));

        assertThatThrownBy(() -> service.supprimer(ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("dossier de candidature");
        verify(etudiantRepository, never()).deleteById(ID);
        verify(userRepository, never()).deleteById(ID);
    }

    @Test
    @DisplayName("refuse si le compte est passé côté alumni")
    void refuse_si_alumni() {
        when(alumniProfilRepository.findByUserId(ID)).thenReturn(Optional.of(new AlumniProfil()));

        assertThatThrownBy(() -> service.supprimer(ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("alumni");
        verify(etudiantRepository, never()).deleteById(ID);
    }

    @Test
    @DisplayName("refuse si l'étudiant a publié dans le forum")
    void refuse_si_publication_forum() {
        when(forumMessageRepository.existsByAuteurId(ID)).thenReturn(true);

        assertThatThrownBy(() -> service.supprimer(ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("forum");
        verify(etudiantRepository, never()).deleteById(ID);
    }

    @Test
    @DisplayName("vide chaque table dépendante puis supprime l'étudiant et l'utilisateur")
    void supprime_dans_le_bon_ordre() {
        assertThatCode(() -> service.supprimer(ID)).doesNotThrowAnyException();

        verify(certificatRepository).deleteByEtudiant_Id(ID);
        verify(releveNotesRepository).deleteByEtudiant_Id(ID);
        verify(retourEvaluationRepository).deleteByEtudiantId(ID);
        verify(noteModuleRepository).deleteByEtudiantId(ID);
        verify(presenceRepository).deleteByEtudiantId(ID);
        verify(progressionSupportRepository).deleteByEtudiantId(ID);
        verify(annonceLectureRepository).deleteByEtudiantId(ID);
        verify(etudiantRepository).deleteById(ID);
        verify(userRepository).deleteById(ID);
    }
}
