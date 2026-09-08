package inpg.sn.lms.recrutementformateur.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.recrutementformateur.entity.OffreRecrutementFormateur;
import inpg.sn.lms.recrutementformateur.enums.EnumStatutOffreFormateur;

@Repository
public interface OffreRecrutementFormateurRepository extends JpaRepository<OffreRecrutementFormateur, Long> {

    List<OffreRecrutementFormateur> findByStatutOrderByDatePublicationDesc(EnumStatutOffreFormateur statut);

    List<OffreRecrutementFormateur> findAllByOrderByDateCreationDesc();
}
