package inpg.sn.lms.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.auth.entity.Etudiant;
import java.util.List;
import java.util.Optional;


@Repository
public interface EtudiantRepository extends JpaRepository<Etudiant, Long> {
	Optional<Etudiant> findByUserId(Long userId);

	/** Le téléphone est unique en base : sert à détecter un conflit avant l'écriture. */
	Optional<Etudiant> findByTelephone(String telephone);

	Optional<Etudiant> findByMatricule(String matricule);

	List<Etudiant> findByPromotionId(Long promotionId);

	long countByPromotionId(Long promotionId);

	/** Étudiants sans matricule : cible du rattrapage au démarrage. */
	List<Etudiant> findByMatriculeIsNull();

	/** Sert à dériver le prochain numéro d'ordre d'un matricule pour une année. */
	long countByMatriculeStartingWith(String prefixe);

	/**
	 * Recherche demandée au TDR §3.2 : retrouver un dossier « en tapant le nom de
	 * l'étudiant ou le nom de sa promotion ».
	 */
	@Query("""
			SELECT e FROM Etudiant e
			LEFT JOIN e.promotion p
			WHERE LOWER(e.nom) LIKE LOWER(CONCAT('%', :terme, '%'))
			   OR LOWER(e.prenom) LIKE LOWER(CONCAT('%', :terme, '%'))
			   OR LOWER(COALESCE(e.matricule, '')) LIKE LOWER(CONCAT('%', :terme, '%'))
			   OR LOWER(COALESCE(p.libelle, '')) LIKE LOWER(CONCAT('%', :terme, '%'))
			   OR LOWER(COALESCE(p.code, '')) LIKE LOWER(CONCAT('%', :terme, '%'))
			""")
	List<Etudiant> rechercher(@Param("terme") String terme);
}
