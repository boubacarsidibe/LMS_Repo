package inpg.sn.lms.personnel.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import inpg.sn.lms.personnel.entity.ActivitePersonnel;

public interface ActivitePersonnelRepository extends JpaRepository<ActivitePersonnel, Long> {

    @EntityGraph(attributePaths = { "departement", "auteur" })
    List<ActivitePersonnel> findByDepartementIdAndAnneeAndMoisOrderByDateCreationDesc(
            Long departementId, int annee, int mois);

    @EntityGraph(attributePaths = { "departement", "auteur" })
    List<ActivitePersonnel> findByAnneeAndMoisOrderByDepartement_LibelleAscDateCreationDesc(int annee, int mois);

    @EntityGraph(attributePaths = { "departement", "auteur" })
    List<ActivitePersonnel> findByAuteurIdOrderByAnneeDescMoisDescDateCreationDesc(Long auteurId);

    long countByDepartementIdAndAnneeAndMois(Long departementId, int annee, int mois);
}
