package inpg.sn.lms.espace_etudiant.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.espace_etudiant.entity.ProgressionSupport;

@Repository
public interface ProgressionSupportRepository extends JpaRepository<ProgressionSupport, Long> {

    boolean existsByEtudiantIdAndSupportId(Long etudiantId, Long supportId);

    /** Nombre de supports d'un cours déjà consultés par l'étudiant. */
    long countByEtudiantIdAndSupport_Cours_Id(Long etudiantId, Long coursId);

    long countByEtudiantId(Long etudiantId);

    /**
     * Derniers supports ouverts par l'étudiant, du plus récent au plus ancien.
     *
     * <p>{@code date_consultation} est posée à la première ouverture et n'est jamais
     * remise à jour — c'est voulu, un support est consulté ou il ne l'est pas. On lit
     * donc ici « les derniers supports découverts », ce qui reste le bon repère pour
     * reprendre son travail.
     */
    @EntityGraph(attributePaths = { "support", "support.cours" })
    List<ProgressionSupport> findTop5ByEtudiantIdOrderByDateConsultationDesc(Long etudiantId);

    /** Identifiants des supports consultés, pour cocher la liste côté client. */
    @Query("SELECT p.support.id FROM ProgressionSupport p WHERE p.etudiant.id = :etudiantId")
    List<Long> findSupportIdsConsultes(@Param("etudiantId") Long etudiantId);

    void deleteByEtudiantId(Long etudiantId);
}
