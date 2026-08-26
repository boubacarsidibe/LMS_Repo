package inpg.sn.lms.espace_etudiant.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
 * Suppression d'un compte étudiant par le responsable pédagogique.
 *
 * <p>Aucune des tables qui référencent {@code etudiants(id_user)} n'a de suppression
 * en cascade côté base (voir V1__baseline.sql) : il faut donc les vider dans l'ordre
 * avant de retirer la ligne {@code etudiants} puis la ligne {@code users} qui la
 * porte, sur le même principe que {@code PedagogieController.supprimerCours} et
 * {@code PedagogieScolariteController.supprimerPromotion}. Un maillon oublié se
 * traduit par une violation de clé étrangère remontée en 500 brut — c'est le piège
 * déjà rencontré deux fois dans ce module (voir CLAUDE.md §3).
 *
 * <p>Trois cas sont volontairement refusés plutôt que traités par cascade, parce que
 * la suppression y déborderait du mandat de cet écran ou effacerait un contenu dont
 * d'autres personnes dépendent :
 * <ul>
 *   <li>un dossier de candidature existe encore : son cycle de vie appartient à
 *       l'espace admission (« l'admission ouvre l'accès, la pédagogie inscrit »,
 *       CLAUDE.md §3) ;</li>
 *   <li>le compte est passé côté alumni : sa fiche et son historique professionnel
 *       relèvent de l'espace alumni ;</li>
 *   <li>l'étudiant a publié dans le forum : supprimer ses messages les effacerait
 *       aussi pour les autres participants du fil, qui n'ont rien demandé.</li>
 * </ul>
 */
@Service
public class EtudiantSuppressionService {

    private final EtudiantRepository etudiantRepository;
    private final UserRepository userRepository;
    private final CandidatRepository candidatRepository;
    private final AlumniProfilRepository alumniProfilRepository;
    private final ForumSujetRepository forumSujetRepository;
    private final ForumMessageRepository forumMessageRepository;
    private final CertificatRepository certificatRepository;
    private final ReleveNotesRepository releveNotesRepository;
    private final RetourEvaluationRepository retourEvaluationRepository;
    private final NoteModuleRepository noteModuleRepository;
    private final PresenceRepository presenceRepository;
    private final ProgressionSupportRepository progressionSupportRepository;
    private final AnnonceLectureRepository annonceLectureRepository;

    public EtudiantSuppressionService(EtudiantRepository etudiantRepository, UserRepository userRepository,
            CandidatRepository candidatRepository, AlumniProfilRepository alumniProfilRepository,
            ForumSujetRepository forumSujetRepository, ForumMessageRepository forumMessageRepository,
            CertificatRepository certificatRepository, ReleveNotesRepository releveNotesRepository,
            RetourEvaluationRepository retourEvaluationRepository, NoteModuleRepository noteModuleRepository,
            PresenceRepository presenceRepository, ProgressionSupportRepository progressionSupportRepository,
            AnnonceLectureRepository annonceLectureRepository) {
        this.etudiantRepository = etudiantRepository;
        this.userRepository = userRepository;
        this.candidatRepository = candidatRepository;
        this.alumniProfilRepository = alumniProfilRepository;
        this.forumSujetRepository = forumSujetRepository;
        this.forumMessageRepository = forumMessageRepository;
        this.certificatRepository = certificatRepository;
        this.releveNotesRepository = releveNotesRepository;
        this.retourEvaluationRepository = retourEvaluationRepository;
        this.noteModuleRepository = noteModuleRepository;
        this.presenceRepository = presenceRepository;
        this.progressionSupportRepository = progressionSupportRepository;
        this.annonceLectureRepository = annonceLectureRepository;
    }

    /** @throws IllegalStateException si la suppression doit être refusée — le message est déjà destiné à l'écran. */
    @Transactional
    public void supprimer(Long id) {
        if (candidatRepository.findByUserId(id).isPresent()) {
            throw new IllegalStateException(
                    "Ce compte porte un dossier de candidature : sa suppression relève de l'espace admission.");
        }
        if (alumniProfilRepository.findByUserId(id).isPresent()) {
            throw new IllegalStateException(
                    "Ce compte est passé côté alumni : sa suppression n'est pas prise en charge depuis cet écran.");
        }
        if (forumSujetRepository.existsByAuteurId(id) || forumMessageRepository.existsByAuteurId(id)) {
            throw new IllegalStateException(
                    "Cet étudiant a publié dans le forum : le supprimer effacerait ses messages pour les autres participants.");
        }

        certificatRepository.deleteByEtudiant_Id(id);
        releveNotesRepository.deleteByEtudiant_Id(id);   // releve_ligne suit en cascade (ON DELETE CASCADE)
        retourEvaluationRepository.deleteByEtudiantId(id);
        noteModuleRepository.deleteByEtudiantId(id);
        presenceRepository.deleteByEtudiantId(id);
        progressionSupportRepository.deleteByEtudiantId(id);
        annonceLectureRepository.deleteByEtudiantId(id);
        etudiantRepository.deleteById(id);
        // notifications et forum_mention suivent en cascade (ON DELETE CASCADE, migration V10).
        userRepository.deleteById(id);
    }
}
