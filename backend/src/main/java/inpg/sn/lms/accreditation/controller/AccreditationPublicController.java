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
 * Liste des écoles accréditées (TDR §F : « base de données des écoles
 * accréditées », processus « transparent »).
 *
 * <p>Réservée aux comptes connectés — tout rôle confondu, aucune restriction
 * par rôle — depuis la décision produit du 2026-08-25 (voir
 * {@code LOG_BLOQUANT.md}) : le TDR demandait une liste réellement publique,
 * mais l'accès a été resserré à la demande de l'utilisateur. Réponse toujours
 * restreinte (voir {@link EcoleAccrediteeDto}), jamais l'entité ni les
 * coordonnées de l'école.
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
                .findByStatutInOrderByDateDecisionDesc(List.of(
                        EnumStatutDemandeAccreditation.ACCREDITE,
                        EnumStatutDemandeAccreditation.ACCREDITE_SOUS_RESERVE))
                .stream()
                .map(demande -> new EcoleAccrediteeDto(
                        demande.getEcole().getNomEcole(),
                        demande.getIntituleProgramme(),
                        demande.getNiveauProgramme() != null ? demande.getNiveauProgramme().name() : null,
                        demande.getDateDecision(),
                        demande.getDateExpiration(),
                        demande.getStatut() == EnumStatutDemandeAccreditation.ACCREDITE_SOUS_RESERVE))
                .toList();
        return ResponseEntity.ok(ecoles);
    }
}
