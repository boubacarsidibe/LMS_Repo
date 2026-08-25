package inpg.sn.lms.espace_etudiant.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import inpg.sn.lms.espace_etudiant.entity.Promotion;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    /**
     * Promotions rattachées à l'une de ces formations.
     *
     * <p>C'est le maillon qui permet de descendre d'un cours vers ses étudiants :
     * un cours appartient à une formation, et une promotion suit une formation.
     * Avant l'unification, ce trajet passait par la table {@code cours_promotion},
     * qui dupliquait — et contredisait — ce chemin.
     */
    List<Promotion> findByFormation_IdIn(Collection<Long> formationIds);
}
