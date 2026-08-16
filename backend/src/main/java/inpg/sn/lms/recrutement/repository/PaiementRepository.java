package inpg.sn.lms.recrutement.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.recrutement.entity.Paiement;
import inpg.sn.lms.recrutement.enums.EnumStatutPaiement;

@Repository
public interface PaiementRepository extends JpaRepository<Paiement, Long> {

    Optional<Paiement> findByCandidatureId(Long candidatureId);

    Optional<Paiement> findByReferenceTransaction(String referenceTransaction);

    Optional<Paiement> findByCandidatureIdAndStatut(Long candidatureId, EnumStatutPaiement statut);
}