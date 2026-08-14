package inpg.sn.lms.espace_etudiant.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.entity.Formateur;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.auth.repository.FormateurRepository;
import inpg.sn.lms.espace_etudiant.entity.Cohorte;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.SupportCours;

/**
 * Regroupe les regles de cloisonnement des donnees : qui a le droit de voir quoi.
 *
 * <p>Ces regles etaient auparavant dispersees (et dupliquees) dans
 * EspaceEtudiantController. Les centraliser ici garantit qu'un correctif de
 * securite s'applique partout d'un coup, et rend la logique testable isolement.
 */
@Service
public class PerimetreFormateurService {

    private final CoursService coursService;
    private final FormateurRepository formateurRepository;
    private final EtudiantRepository etudiantRepository;

    public PerimetreFormateurService(CoursService coursService, FormateurRepository formateurRepository,
            EtudiantRepository etudiantRepository) {
        this.coursService = coursService;
        this.formateurRepository = formateurRepository;
        this.etudiantRepository = etudiantRepository;
    }

    /**
     * Cohortes que le formateur enseigne, deduites des cohortes rattachees a ses cours.
     * Un formateur sans cours, ou dont aucun cours n'a de cohorte, obtient une liste vide
     * (et ne verra donc aucun etudiant) : c'est le comportement voulu, pas un bug.
     */
    public List<Long> cohorteIdsDuFormateur(Formateur formateur) {
        return coursService.getByFormateurId(formateur.getId()).stream()
                .flatMap(cours -> cours.getCohortes().stream())
                .map(Cohorte::getId)
                .distinct()
                .toList();
    }

    /**
     * Cohortes visibles par un utilisateur quelconque, selon son profil :
     * un formateur voit celles de ses cours, un etudiant uniquement la sienne.
     * Un utilisateur sans profil (ni formateur ni etudiant) ne voit rien.
     */
    public List<Long> cohorteIdsVisiblesPour(User user) {
        Formateur formateur = formateurRepository.findByUserId(user.getId()).orElse(null);
        if (formateur != null) {
            return cohorteIdsDuFormateur(formateur);
        }
        Etudiant etudiant = etudiantRepository.findByUserId(user.getId()).orElse(null);
        if (etudiant != null && etudiant.getCohorte() != null) {
            return List.of(etudiant.getCohorte().getId());
        }
        return List.of();
    }

    /** Vrai si ce formateur fait partie des enseignants du cours (droit de modification). */
    public boolean estFormateurDuCours(Formateur formateur, Cours cours) {
        return cours.getFormateurs().stream()
                .anyMatch(f -> f.getId().equals(formateur.getId()));
    }

    /** Vrai si l'etudiant appartient a l'une des cohortes rattachees au cours. */
    public boolean etudiantSuitLeCours(Etudiant etudiant, Cours cours) {
        return etudiant.getCohorte() != null
                && cours.getCohortes().stream()
                        .anyMatch(c -> c.getId().equals(etudiant.getCohorte().getId()));
    }

    /** Vrai si la ressource a une date de publication encore dans le futur. */
    public boolean supportEstProgramme(SupportCours support) {
        return support.getDatePublication() != null
                && support.getDatePublication().isAfter(LocalDate.now());
    }

    /**
     * Une ressource programmee reste invisible tant que sa date n'est pas atteinte,
     * SAUF pour les formateurs du cours concerne : ils doivent pouvoir relire et
     * corriger ce qu'ils ont planifie avant sa mise en ligne.
     *
     * @param formateurConnecte le formateur authentifie, ou null si l'appelant est un etudiant
     */
    public boolean peutVoirSupport(SupportCours support, Formateur formateurConnecte) {
        if (formateurConnecte != null && support.getCours() != null
                && estFormateurDuCours(formateurConnecte, support.getCours())) {
            return true;
        }
        return !supportEstProgramme(support);
    }

    /**
     * Une ressource est liee a un cours precis : elle ne doit etre visible que par
     * les gens concernes par CE cours, pas par toute la plateforme. Sans ce controle,
     * un etudiant de Finance voyait (et pouvait telecharger) les PDF de Data Science.
     *
     * <p>Un support sans cours rattache (ressource pedagogique generale, hors du
     * perimetre "cours") n'est pas concerne par cette regle : toujours accessible.
     *
     * @param mesCohorteIds cohortes visibles de l'appelant, voir {@link #cohorteIdsVisiblesPour}
     */
    public boolean coursDuSupportEstAccessible(SupportCours support, List<Long> mesCohorteIds, Formateur formateurConnecte) {
        Cours cours = support.getCours();
        if (cours == null) {
            return true;
        }
        if (formateurConnecte != null && estFormateurDuCours(formateurConnecte, cours)) {
            return true;
        }
        return cours.getCohortes().stream().anyMatch(c -> mesCohorteIds.contains(c.getId()));
    }
}
