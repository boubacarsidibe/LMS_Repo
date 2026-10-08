package inpg.sn.lms.espace_etudiant.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import inpg.sn.lms.espace_etudiant.dto.CertificatLotRequest;
import inpg.sn.lms.espace_etudiant.service.CertificatLotService;

/**
 * Délivrance de certificats par promotion.
 *
 * <p>Contrôleur à part, sur le même préfixe que {@code PedagogieScolariteController}
 * (comme {@code PedagogieEntrepriseController}) : les routes ci-dessous
 * ({@code /certificats/apercu}, {@code /certificats/lot}) n'existent pas dans l'autre,
 * qui garde {@code /certificats} et {@code /certificats/{id}/pdf}. Ne jamais déclarer
 * la même route des deux côtés — Spring refuse de démarrer.
 *
 * <p>Aucun {@code @Transactional} ici, volontairement : la transaction est celle du
 * service. Une transaction englobante ici, avec le {@code catch} ci-dessous, marquerait
 * la transaction « rollback-only » dès la première {@link IllegalArgumentException} du
 * service, et le commit final lèverait une 500 au lieu de renvoyer le 400 voulu.
 */
@RestController
@RequestMapping("/api/pedagogie/certificats")
@PreAuthorize("hasRole('RESPONSABLE_PEDAGOGIQUE')")
public class PedagogieCertificatLotController {

    private final CertificatLotService certificatLotService;

    public PedagogieCertificatLotController(CertificatLotService certificatLotService) {
        this.certificatLotService = certificatLotService;
    }

    /** Formation, période probable et éligibilité de chaque étudiant de la promotion. */
    @GetMapping("/apercu")
    public ResponseEntity<?> apercu(@RequestParam Long promotionId) {
        return certificatLotService.apercu(promotionId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Promotion introuvable.")));
    }

    /** Délivre à toute la promotion, ou à toute la promotion sauf les étudiants exclus. */
    @PostMapping("/lot")
    public ResponseEntity<?> delivrer(@RequestBody CertificatLotRequest requete) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(certificatLotService.delivrer(requete));
        } catch (IllegalArgumentException invalide) {
            return ResponseEntity.badRequest().body(Map.of("message", invalide.getMessage()));
        }
    }
}
