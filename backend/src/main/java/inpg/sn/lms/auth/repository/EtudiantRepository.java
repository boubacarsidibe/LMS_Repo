package inpg.sn.lms.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.espace_etudiant.dto.CompteurParId;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


@Repository
public interface EtudiantRepository extends JpaRepository<Etudiant, Long> {
	Optional<Etudiant> findByUserId(Long userId);

	List<Etudiant> findByCohorte_IdIn(List<Long> cohorteIds);

	long countByCohorte_Id(Long cohorteId);

	/**
	 * Compte les etudiants de plusieurs cohortes en UNE seule requete.
	 * Les cohortes sans aucun etudiant sont absentes du resultat (COUNT ... GROUP BY
	 * ne produit pas de ligne a zero) : cote appelant, utiliser getOrDefault(id, 0L).
	 */
	@Query("SELECT new inpg.sn.lms.espace_etudiant.dto.CompteurParId(e.cohorte.id, COUNT(e)) "
			+ "FROM Etudiant e WHERE e.cohorte.id IN :cohorteIds GROUP BY e.cohorte.id")
	List<CompteurParId> compterParCohorte(@Param("cohorteIds") List<Long> cohorteIds);

}
