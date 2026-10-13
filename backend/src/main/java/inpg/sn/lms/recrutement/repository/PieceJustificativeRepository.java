package inpg.sn.lms.recrutement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.recrutement.entity.PieceJustificative;
import inpg.sn.lms.recrutement.enums.EnumTypeDocument;

@Repository
public interface PieceJustificativeRepository extends JpaRepository<PieceJustificative, Long> {

    List<PieceJustificative> findByCandidatureId(Long candidatureId);

    void deleteByCandidatureId(Long candidatureId);

    /** Sert a plafonner le nombre de releves de notes deposes par candidature (au plus 9). */
    long countByCandidatureIdAndTypeDocument(Long candidatureId, EnumTypeDocument typeDocument);

    /** Sert à reconnaître un envoi répété du même document (nouvel essai après une réponse perdue). */
    List<PieceJustificative> findByCandidatureIdAndTypeDocument(Long candidatureId, EnumTypeDocument typeDocument);
}