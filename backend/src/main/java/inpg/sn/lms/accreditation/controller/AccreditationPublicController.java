package inpg.sn.lms.accreditation.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import inpg.sn.lms.accreditation.dto.EcoleAccrediteeDto;
import inpg.sn.lms.accreditation.enums.EnumStatutDemandeAccreditation;
import inpg.sn.lms.accreditation.repository.DemandeAccreditationRepository;

/**
 * Liste publique des écoles accréditées (TDR §F : « base de données des
 * écoles accréditées », processus « transparent »).
 *
 * <p>Sans authentification — {@code permitAll()} dans {@code SecurityConfig},
 * même patron que {@code CertificatPublicController} : réponse restreinte
 * (voir {@link EcoleAccrediteeDto}), jamais l'entité ni les coordonnées de
 * l'école.
 */
@RestController
@RequestMapping("/api/accreditation/public")
public class AccreditationPublicController {

    private final DemandeAccreditationRepository demandeRepository;

    public AccreditationPublicController(DemandeAccreditationRepository demandeRepository) {
        this.demandeRepository = demandeRepository;
    }

    @GetMapping("/ecoles-accreditees")
    @Transactional(readOnly = true)
    public ResponseEntity<List<EcoleAccrediteeDto>> ecolesAccreditees() {
        List<EcoleAccrediteeDto> ecoles = demandeRepository
                .findByStatutOrderByDateSoumissionDesc(EnumStatutDemandeAccreditation.ACCREDITE).stream()
                .map(demande -> new EcoleAccrediteeDto(demande.getEcole().getNomEcole(), demande.getDateDecision()))
                .toList();
        return ResponseEntity.ok(ecoles);
    }
}
