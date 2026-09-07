package inpg.sn.lms.accreditation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.accreditation.entity.CritereAccreditation;

@Repository
public interface CritereAccreditationRepository extends JpaRepository<CritereAccreditation, Long> {

    List<CritereAccreditation> findByActifTrue();

    /** Référentiel actif, dans l'ordre d'affichage de la grille d'évaluation. */
    List<CritereAccreditation> findByActifTrueOrderByOrdreAscIdAsc();

    List<CritereAccreditation> findAllByOrderByOrdreAscIdAsc();

    /** Clé d'idempotence de l'amorçage du référentiel DRCQ. */
    Optional<CritereAccreditation> findByCode(String code);
}
