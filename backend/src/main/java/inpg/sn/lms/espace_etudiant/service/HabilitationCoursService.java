package inpg.sn.lms.espace_etudiant.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.Formation;
import inpg.sn.lms.espace_etudiant.entity.Promotion;
import inpg.sn.lms.espace_etudiant.repository.CoursRepository;

/**
 * Détermine les « cours autorisés » d'un étudiant au sens du §3.2 du cahier des
 * charges.
 *
 * <p>La règle — Étudiant → Promotion → Formation → Cours — commande l'accès aux
 * cours, à leurs supports, au calendrier, aux salons du forum et aux évaluations.
 * Elle est centralisée ici pour qu'il n'existe qu'une seule définition de ce à
 * quoi un étudiant a droit.
 */
@Service
public class HabilitationCoursService {

    private final CoursRepository coursRepository;

    public HabilitationCoursService(CoursRepository coursRepository) {
        this.coursRepository = coursRepository;
    }

    /**
     * Cours au programme de la formation suivie par la promotion de l'étudiant.
     *
     * <p>Renvoie une liste vide — et non le catalogue complet — tant que l'étudiant
     * n'est rattaché ni à une promotion ni à une formation : un espace vide est
     * préférable à un accès non fondé.
     */
    @Transactional(readOnly = true)
    public List<Cours> coursAutorises(Etudiant etudiant) {
        if (etudiant == null) {
            return List.of();
        }
        Promotion promotion = etudiant.getPromotion();
        if (promotion == null) {
            return List.of();
        }
        Formation formation = promotion.getFormation();
        if (formation == null) {
            return List.of();
        }
        return coursRepository.findByFormations_Id(formation.getId());
    }

    @Transactional(readOnly = true)
    public List<Long> idsCoursAutorises(Etudiant etudiant) {
        return coursAutorises(etudiant).stream().map(Cours::getId).toList();
    }

    /** Le cours demandé s'il est autorisé, {@code null} sinon. */
    @Transactional(readOnly = true)
    public Cours trouverAutorise(Long coursId, Etudiant etudiant) {
        return coursAutorises(etudiant).stream()
                .filter(cours -> cours.getId().equals(coursId))
                .findFirst()
                .orElse(null);
    }

    public boolean estAutorise(Long coursId, Etudiant etudiant) {
        return trouverAutorise(coursId, etudiant) != null;
    }
}
