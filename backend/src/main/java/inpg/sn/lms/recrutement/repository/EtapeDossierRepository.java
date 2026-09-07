package inpg.sn.lms.recrutement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import inpg.sn.lms.recrutement.entity.EtapeDossier;
import inpg.sn.lms.recrutement.enums.EnumEtapeQms;

public interface EtapeDossierRepository extends JpaRepository<EtapeDossier, Long> {

    List<EtapeDossier> findByCandidatureIdOrderByDateEtapeAsc(Long candidatureId);

    /** Sert à savoir si une étape a déjà été franchie — par exemple si l'inscription a déjà été faite. */
    boolean existsByCandidatureIdAndEtape(Long candidatureId, EnumEtapeQms etape);
}
