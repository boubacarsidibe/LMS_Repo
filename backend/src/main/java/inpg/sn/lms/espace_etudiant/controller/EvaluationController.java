package inpg.sn.lms.espace_etudiant.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.espace_etudiant.dto.EvaluationCoursDto;
import inpg.sn.lms.espace_etudiant.dto.EvaluationRequest;
import inpg.sn.lms.espace_etudiant.dto.SyntheseEvaluationDto;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.EvaluationCours;
import inpg.sn.lms.espace_etudiant.repository.CoursRepository;
import inpg.sn.lms.espace_etudiant.repository.EvaluationCoursRepository;
import inpg.sn.lms.espace_etudiant.service.HabilitationCoursService;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;

/**
 * Évaluations de satisfaction (TDR §3.7 « Processus qualité »).
 *
 * <p>Un étudiant ne peut évaluer qu'un cours de sa propre formation, une seule
 * fois : une nouvelle soumission met à jour son avis plutôt que d'en empiler un
 * second. La synthèse par cours est ouverte aux formateurs comme aux étudiants,
 * mais reste anonyme — elle ne renvoie qu'une moyenne et un volume.
 */
@RestController
@RequestMapping("/api/espace-etudiant/evaluations")
/*
 * Autorisation rendue explicite : elle ne reposait que sur le prefixe d'URL filtre
 * dans SecurityConfig. Un endpoint ajoute hors de ce prefixe aurait ete ouvert sans
 * que rien ne le signale. Les regles d'URL restent en place et s'appliquent en plus.
 */
@PreAuthorize("hasAnyRole('ETUDIANT','FORMATEUR')")
public class EvaluationController {

    private static final int NOTE_MIN = 1;
    private static final int NOTE_MAX = 5;

    private final EvaluationCoursRepository evaluationRepository;
    private final CoursRepository coursRepository;
    private final UtilisateurCourantService utilisateurCourant;
    private final HabilitationCoursService habilitation;

    public EvaluationController(EvaluationCoursRepository evaluationRepository, CoursRepository coursRepository,
            UtilisateurCourantService utilisateurCourant, HabilitationCoursService habilitation) {
        this.evaluationRepository = evaluationRepository;
        this.coursRepository = coursRepository;
        this.utilisateurCourant = utilisateurCourant;
        this.habilitation = habilitation;
    }

    /** Les évaluations déjà déposées par l'étudiant connecté. */
    @GetMapping("/mes-evaluations")
    @Transactional(readOnly = true)
    public ResponseEntity<?> mesEvaluations(Authentication authentication) {
        Etudiant etudiant = utilisateurCourant.etudiant(authentication);
        if (etudiant == null) {
            return profilIntrouvable();
        }
        return ResponseEntity.ok(evaluationRepository.findByEtudiantIdOrderByDateSoumissionDesc(etudiant.getId())
                .stream().map(this::toDto).toList());
    }

    /** Dépose ou met à jour l'évaluation d'un cours. */
    @PostMapping("/cours/{coursId}")
    @Transactional
    public ResponseEntity<?> evaluer(@PathVariable Long coursId, @RequestBody EvaluationRequest request,
            Authentication authentication) {
        Etudiant etudiant = utilisateurCourant.etudiant(authentication);
        if (etudiant == null) {
            return profilIntrouvable();
        }
        if (request.note() == null || request.note() < NOTE_MIN || request.note() > NOTE_MAX) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "La note doit être comprise entre " + NOTE_MIN + " et " + NOTE_MAX + "."));
        }
        Cours cours = habilitation.trouverAutorise(coursId, etudiant);
        if (cours == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Vous ne pouvez évaluer que les cours de votre formation."));
        }
        String commentaire = request.commentaire() == null ? null : request.commentaire().trim();

        EvaluationCours evaluation = evaluationRepository
                .findByEtudiantIdAndCoursId(etudiant.getId(), coursId)
                .orElseGet(() -> new EvaluationCours(etudiant, cours, request.note(), commentaire));
        evaluation.setNote(request.note());
        evaluation.setCommentaire(commentaire);
        evaluation = evaluationRepository.save(evaluation);

        return ResponseEntity.ok(toDto(evaluation));
    }

    /** Synthèse anonyme des retours sur un cours. */
    @GetMapping("/cours/{coursId}/synthese")
    @Transactional(readOnly = true)
    public ResponseEntity<?> synthese(@PathVariable Long coursId, Authentication authentication) {
        Cours cours = coursRepository.findById(coursId).orElse(null);
        if (cours == null) {
            return ResponseEntity.notFound().build();
        }
        Etudiant etudiant = utilisateurCourant.etudiant(authentication);
        // Un étudiant ne consulte la synthèse que des cours qu'il suit ; un
        // formateur (sans profil étudiant) accède à toutes les synthèses.
        if (etudiant != null && !habilitation.estAutorise(coursId, etudiant)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Ce cours ne fait pas partie de votre formation."));
        }
        return ResponseEntity.ok(new SyntheseEvaluationDto(
                cours.getId(),
                cours.getTitre(),
                evaluationRepository.moyenneParCours(coursId),
                evaluationRepository.countByCoursId(coursId)));
    }

    private EvaluationCoursDto toDto(EvaluationCours evaluation) {
        Cours cours = evaluation.getCours();
        return new EvaluationCoursDto(
                evaluation.getId(),
                cours == null ? null : cours.getId(),
                cours == null ? null : cours.getTitre(),
                evaluation.getNote(),
                evaluation.getCommentaire(),
                evaluation.getDateSoumission());
    }

    private ResponseEntity<?> profilIntrouvable() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Aucun profil étudiant n'est associé à ce compte."));
    }
}
