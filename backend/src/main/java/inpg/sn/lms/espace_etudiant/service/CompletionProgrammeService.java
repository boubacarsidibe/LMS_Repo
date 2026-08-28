package inpg.sn.lms.espace_etudiant.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.Formation;
import inpg.sn.lms.espace_etudiant.entity.Promotion;
import inpg.sn.lms.espace_etudiant.repository.CoursRepository;
import inpg.sn.lms.espace_etudiant.repository.NoteModuleRepository;
import inpg.sn.lms.espace_etudiant.repository.ProgressionSupportRepository;
import inpg.sn.lms.espace_etudiant.repository.SupportCoursRepository;

/**
 * Détermine si un étudiant a réellement fini son programme : contenus
 * consultés à 100 % et au moins une évaluation par cours.
 *
 * <p>Un seul et même contrôle pour deux décisions qui engagent l'établissement :
 * délivrer un certificat, et faire basculer un étudiant en alumni. Les garder
 * séparés reproduirait le genre de divergence que ce projet a déjà eu ailleurs
 * (deux définitions concurrentes de « ce cours est-il suivi »).
 */
@Service
public class CompletionProgrammeService {

    private final CoursRepository coursRepository;
    private final SupportCoursRepository supportCoursRepository;
    private final ProgressionSupportRepository progressionSupportRepository;
    private final NoteModuleRepository noteModuleRepository;

    public CompletionProgrammeService(CoursRepository coursRepository, SupportCoursRepository supportCoursRepository,
            ProgressionSupportRepository progressionSupportRepository, NoteModuleRepository noteModuleRepository) {
        this.coursRepository = coursRepository;
        this.supportCoursRepository = supportCoursRepository;
        this.progressionSupportRepository = progressionSupportRepository;
        this.noteModuleRepository = noteModuleRepository;
    }

    /**
     * Cours du programme de l'étudiant qui ne sont pas achevés : contenus pas tous
     * consultés, ou aucune évaluation encore saisie. Liste vide = programme terminé.
     *
     * <p>Aucun cursus rattaché (pas de promotion, ou promotion sans formation, ou
     * formation sans cours) compte comme non terminé : il n'y a alors rien à
     * délivrer ou promouvoir de sérieux.
     */
    @Transactional(readOnly = true)
    public List<String> coursNonTermines(Etudiant etudiant) {
        Promotion promotion = etudiant.getPromotion();
        Formation formation = promotion == null ? null : promotion.getFormation();
        if (formation == null) {
            return List.of("aucune formation rattachée à la promotion de cet étudiant");
        }
        List<Cours> coursProgramme = coursRepository.findByFormations_Id(formation.getId());
        if (coursProgramme.isEmpty()) {
            return List.of("aucun cours au programme de cette formation");
        }
        List<String> nonTermines = new ArrayList<>();
        for (Cours cours : coursProgramme) {
            long totalSupports = supportCoursRepository.countByCoursId(cours.getId());
            long consultes = progressionSupportRepository.countByEtudiantIdAndSupport_Cours_Id(etudiant.getId(), cours.getId());
            boolean contenuTermine = totalSupports == 0 || consultes >= totalSupports;
            boolean evalue = noteModuleRepository.existsByEtudiantIdAndCoursId(etudiant.getId(), cours.getId());
            if (!contenuTermine || !evalue) {
                nonTermines.add(cours.getTitre());
            }
        }
        return nonTermines;
    }
}
