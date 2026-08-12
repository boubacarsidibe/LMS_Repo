package inpg.sn.lms.recrutement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.enums.EnumStatutCandidature;

@Repository
public interface CandidatureRepository extends JpaRepository<Candidature, Long> {

    Optional<Candidature> findByNumeroDossier(String numeroDossier);

    List<Candidature> findByCandidatId(Long candidatId);

    List<Candidature> findByStatut(EnumStatutCandidature statut);

    List<Candidature> findBySessionCandidatureId(Long sessionId);

    List<Candidature> findByCandidatIdAndStatut(Long candidatId, EnumStatutCandidature statut);

    List<Candidature> findTop5ByOrderByDateSoumissionDesc();

    @Query("SELECT c FROM Candidature c LEFT JOIN c.candidat ca LEFT JOIN ca.user u " +
           "WHERE LOWER(c.numeroDossier) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(ca.nom) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(ca.prenom) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Candidature> searchByQuery(@Param("q") String q);
}
