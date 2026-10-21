package inpg.sn.lms.espace_etudiant.service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.repository.ProgressionSupportRepository;

/**
 * Ce que l'étudiant a réellement fait sur la plateforme, tel que le voient son formateur et le
 * responsable pédagogique.
 *
 * <p>Les écrans de suivi affichaient « 0 % » de progression pour tout le monde et « Non renseignée » en
 * guise de dernière connexion : deux valeurs écrites en dur dans les contrôleurs. Le même étudiant voyait
 * pourtant, de son côté, « Progression 100 % » — l'écran de celui qui doit décider qui relancer disait le
 * contraire de celui de l'intéressé. Ce service est l'unique source de ces deux chiffres.
 *
 * <p><strong>La progression suit la définition de l'espace étudiant, pas une nouvelle.</strong> Pour
 * chaque cours autorisé : supports consultés ÷ supports publiés (0 % si le cours n'a aucun support) ;
 * l'indicateur global est la moyenne arrondie de ces pourcentages. Sa dérivation « cours autorisés »
 * passe par {@link HabilitationCoursService}, comme l'écran de l'étudiant.
 *
 * <p><strong>Pas de « dernière connexion ».</strong> La plateforme n'enregistre nulle part l'heure d'une
 * connexion : afficher ce libellé promettait une donnée qui n'existait pas. On expose à la place le seul
 * fait daté dont on dispose — la date du dernier support ouvert pour la première fois (voir
 * {@link ProgressionSupportRepository}) — sous son vrai nom, « dernière activité ».
 */
@Service
public class ActiviteEtudiantService {

    /** Valeur affichée quand l'étudiant n'a encore ouvert aucun support. */
    public static final String AUCUNE_ACTIVITE = "Aucune activité";

    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRANCE);

    private final HabilitationCoursService habilitation;
    private final SupportService supportService;
    private final ProgressionSupportRepository progressionRepository;

    public ActiviteEtudiantService(HabilitationCoursService habilitation, SupportService supportService,
            ProgressionSupportRepository progressionRepository) {
        this.habilitation = habilitation;
        this.supportService = supportService;
        this.progressionRepository = progressionRepository;
    }

    /** Progression globale en pourcentage entier ; 0 si l'étudiant n'a aucun cours autorisé. */
    @Transactional(readOnly = true)
    public int progression(Etudiant etudiant) {
        List<Cours> cours = habilitation.coursAutorises(etudiant);
        if (cours.isEmpty()) {
            return 0;
        }
        long somme = 0;
        for (Cours c : cours) {
            somme += progressionDuCours(c.getId(), etudiant.getId());
        }
        return (int) Math.round((double) somme / cours.size());
    }

    /** « 19 oct. 2026 », ou {@link #AUCUNE_ACTIVITE} si aucun support n'a encore été ouvert. */
    @Transactional(readOnly = true)
    public String derniereActivite(Etudiant etudiant) {
        return progressionRepository.findFirstByEtudiantIdOrderByDateConsultationDesc(etudiant.getId())
                .map(p -> JOUR.format(p.getDateConsultation()))
                .orElse(AUCUNE_ACTIVITE);
    }

    private int progressionDuCours(Long coursId, Long etudiantId) {
        long total = supportService.countCourseSupports(coursId);
        if (total == 0) {
            return 0;
        }
        long consultes = progressionRepository.countByEtudiantIdAndSupport_Cours_Id(etudiantId, coursId);
        return (int) Math.round(100.0 * consultes / total);
    }
}
