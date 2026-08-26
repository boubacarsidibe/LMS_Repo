package inpg.sn.lms.espace_alumni.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.espace_alumni.entity.ReponseEnqueteSuivi;

@Repository
public interface ReponseEnqueteSuiviRepository extends JpaRepository<ReponseEnqueteSuivi, Long> {

    List<ReponseEnqueteSuivi> findByAlumniProfilIdOrderByDateReponseDesc(Long alumniProfilId);

    Optional<ReponseEnqueteSuivi> findByEnqueteIdAndAlumniProfilId(Long enqueteId, Long alumniProfilId);

    List<ReponseEnqueteSuivi> findByEnqueteIdOrderByDateReponseDesc(Long enqueteId);

    long countByEnqueteId(Long enqueteId);

    @Query("SELECT AVG(r.noteApplicationCompetences) FROM ReponseEnqueteSuivi r WHERE r.enquete.id = :enqueteId")
    Double moyenneApplicationCompetences(@Param("enqueteId") Long enqueteId);

    @Query("SELECT AVG(r.noteImpactCarriere) FROM ReponseEnqueteSuivi r WHERE r.enquete.id = :enqueteId")
    Double moyenneImpactCarriere(@Param("enqueteId") Long enqueteId);
}
