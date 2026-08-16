package inpg.sn.lms.auth.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.espace_etudiant.dto.CompteurParId;

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

	/**
	 * Un formateur peut enseigner plusieurs promotions à la fois : variante
	 * multi-id de findByPromotionId. Le contrôleur lit ensuite
	 * {@code etudiant.getPromotion().getLibelle()} hors session (open-in-view
	 * désactivé) : sans ce graphe, promotion reste un proxy non résolu et lève
	 * une LazyInitializationException dès qu'au moins un étudiant est trouvé.
	 */
	@EntityGraph(attributePaths = "promotion")
	List<Etudiant> findByPromotion_IdIn(List<Long> promotionIds);

	/**
	 * Compte les etudiants de plusieurs promotions en UNE seule requete.
	 * Les promotions sans aucun etudiant sont absentes du resultat (COUNT ... GROUP BY
	 * ne produit pas de ligne a zero) : cote appelant, utiliser getOrDefault(id, 0L).
	 */
	@Query("SELECT new inpg.sn.lms.espace_etudiant.dto.CompteurParId(e.promotion.id, COUNT(e)) "
			+ "FROM Etudiant e WHERE e.promotion.id IN :promotionIds GROUP BY e.promotion.id")
	List<CompteurParId> compterParPromotion(@Param("promotionIds") List<Long> promotionIds);
}
