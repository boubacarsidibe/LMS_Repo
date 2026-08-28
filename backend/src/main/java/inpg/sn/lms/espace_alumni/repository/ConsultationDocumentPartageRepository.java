package inpg.sn.lms.espace_alumni.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.espace_alumni.entity.ConsultationDocumentPartage;

@Repository
public interface ConsultationDocumentPartageRepository extends JpaRepository<ConsultationDocumentPartage, Long> {

    boolean existsByDocumentIdAndLecteurId(Long documentId, Long lecteurId);

    /** Nombre de documents distincts consultés par un lecteur — profil enrichi. */
    long countByLecteurId(Long lecteurId);
}
