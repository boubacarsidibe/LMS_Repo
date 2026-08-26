package inpg.sn.lms.espace_etudiant.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import inpg.sn.lms.auth.repository.AlumniProfilRepository;
import inpg.sn.lms.auth.repository.CandidatRepository;
import inpg.sn.lms.auth.repository.FormateurRepository;
import inpg.sn.lms.auth.repository.UserRepository;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.repository.CoursRepository;
import inpg.sn.lms.espace_etudiant.repository.ForumMessageRepository;
import inpg.sn.lms.espace_etudiant.repository.ForumSujetRepository;

/**
 * Suppression d'un compte formateur par le responsable pédagogique.
 *
 * <p>Même principe que {@link EtudiantSuppressionService} : aucune suppression en
 * cascade côté base pour les tables qui référencent {@code formateurs(id_user)}, donc
 * détachement explicite avant de retirer les lignes {@code formateurs} puis
 * {@code users}.
 *
 * <p>Un refus supplémentaire, propre au formateur : {@code cours_formateur} commande
 * tout le cloisonnement enseignant (CLAUDE.md §3) — un cours sans aucun formateur
 * n'apparaît plus dans aucun espace enseignant, sans qu'aucune alerte ne le signale
 * ensuite. Si ce formateur est l'unique titulaire d'au moins un cours, la suppression
 * est refusée plutôt que de laisser ce cours orphelin.
 */
@Service
public class FormateurSuppressionService {

    private final FormateurRepository formateurRepository;
    private final UserRepository userRepository;
    private final CoursRepository coursRepository;
    private final CandidatRepository candidatRepository;
    private final AlumniProfilRepository alumniProfilRepository;
    private final ForumSujetRepository forumSujetRepository;
    private final ForumMessageRepository forumMessageRepository;

    public FormateurSuppressionService(FormateurRepository formateurRepository, UserRepository userRepository,
            CoursRepository coursRepository, CandidatRepository candidatRepository,
            AlumniProfilRepository alumniProfilRepository, ForumSujetRepository forumSujetRepository,
            ForumMessageRepository forumMessageRepository) {
        this.formateurRepository = formateurRepository;
        this.userRepository = userRepository;
        this.coursRepository = coursRepository;
        this.candidatRepository = candidatRepository;
        this.alumniProfilRepository = alumniProfilRepository;
        this.forumSujetRepository = forumSujetRepository;
        this.forumMessageRepository = forumMessageRepository;
    }

    /** @throws IllegalStateException si la suppression doit être refusée — le message est déjà destiné à l'écran. */
    @Transactional
    public void supprimer(Long id) {
        List<Cours> coursDuFormateur = coursRepository.findByFormateurs_Id(id);
        List<Cours> seulTitulaire = coursDuFormateur.stream()
                .filter(c -> c.getFormateurs().size() == 1)
                .toList();
        if (!seulTitulaire.isEmpty()) {
            String titres = seulTitulaire.stream().map(Cours::getTitre).collect(Collectors.joining(", "));
            throw new IllegalStateException(
                    "Ce formateur est seul titulaire de : " + titres
                            + ". Affectez-y un autre formateur avant de le supprimer.");
        }
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
                    "Ce formateur a publié dans le forum : le supprimer effacerait ses messages pour les autres participants.");
        }

        for (Cours cours : coursDuFormateur) {
            cours.getFormateurs().removeIf(f -> f.getId().equals(id));
            coursRepository.save(cours);
        }
        formateurRepository.deleteById(id);
        // notifications et forum_mention suivent en cascade (ON DELETE CASCADE, migration V10).
        userRepository.deleteById(id);
    }
}
