package inpg.sn.lms.accreditation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.accreditation.entity.DemandeAccreditation;
import inpg.sn.lms.accreditation.enums.EnumStatutDemandeAccreditation;

@Repository
public interface DemandeAccreditationRepository extends JpaRepository<DemandeAccreditation, Long> {

    List<DemandeAccreditation> findByEcoleIdOrderByDateSoumissionDesc(Long ecoleId);

    List<DemandeAccreditation> findAllByOrderByDateSoumissionDesc();

    List<DemandeAccreditation> findByStatutOrderByDateSoumissionDesc(EnumStatutDemandeAccreditation statut);

    /** Programmes tranchés favorablement (pleine ou sous réserve), plus récents d'abord. */
    List<DemandeAccreditation> findByStatutInOrderByDateDecisionDesc(List<EnumStatutDemandeAccreditation> statuts);

    /** Une demande active (non tranchée) pour cette école, s'il y en a une. */
    List<DemandeAccreditation> findByEcoleIdAndStatutIn(Long ecoleId, List<EnumStatutDemandeAccreditation> statuts);

    /** Vérification publique de l'attestation d'accréditation par sa référence (voir migration V202609271700). */
    java.util.Optional<DemandeAccreditation> findByReferenceAttestation(String referenceAttestation);
}
