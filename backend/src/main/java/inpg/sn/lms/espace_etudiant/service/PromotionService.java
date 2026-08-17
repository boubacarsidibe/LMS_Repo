package inpg.sn.lms.espace_etudiant.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.Formation;
import inpg.sn.lms.espace_etudiant.entity.Promotion;
import inpg.sn.lms.espace_etudiant.repository.PromotionRepository;

@Service
public class PromotionService {

    private final PromotionRepository promotionRepository;

    public PromotionService(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    public List<Promotion> getAll() {
        return promotionRepository.findAll();
    }

    public Optional<Promotion> findById(Long id) {
        return promotionRepository.findById(id);
    }

    /**
     * Promotions qui suivent ce cours, déduites de la ou des formations dont il fait
     * partie : un cours appartient à un programme, et une promotion suit ce programme.
     *
     * <p>C'est le seul chemin qui fasse autorité. Un champ {@code Cours.promotions},
     * adossé à la table {@code cours_promotion}, répondait autrefois à la même question
     * par un rattachement manuel ; les deux réponses avaient divergé au point de ne plus
     * désigner aucun cours commun.
     *
     * <p>Un cours hors de tout programme ne touche aucune promotion — liste vide, ce qui
     * est le comportement voulu et non un défaut.
     */
    @Transactional(readOnly = true)
    public List<Promotion> desFormationsDuCours(Cours cours) {
        if (cours == null) {
            return List.of();
        }
        // Trie : Cours.formations est un HashSet, dont l'ordre d'iteration n'est pas
        // garanti. Sans cela, deux appels identiques produiraient des requetes
        // differant par l'ordre des parametres — indeterminisme inutile.
        List<Long> formationIds = cours.getFormations().stream()
                .map(Formation::getId)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        return formationIds.isEmpty() ? List.of() : promotionRepository.findByFormation_IdIn(formationIds);
    }

    /** Variante identifiants de {@link #desFormationsDuCours}. */
    @Transactional(readOnly = true)
    public List<Long> idsDesFormationsDuCours(Cours cours) {
        return desFormationsDuCours(cours).stream().map(Promotion::getId).toList();
    }

    /**
     * Identifiants des promotions rattachées à ces formations. Sert à établir le
     * périmètre d'un formateur : les promotions de tous les programmes où figurent
     * ses cours.
     */
    @Transactional(readOnly = true)
    public List<Long> promotionIdsDesFormations(List<Long> formationIds) {
        if (formationIds == null || formationIds.isEmpty()) {
            return List.of();
        }
        return promotionRepository.findByFormation_IdIn(formationIds).stream()
                .map(Promotion::getId)
                .distinct()
                .toList();
    }
}
