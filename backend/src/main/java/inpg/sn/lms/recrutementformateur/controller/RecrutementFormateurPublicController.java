package inpg.sn.lms.recrutementformateur.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import inpg.sn.lms.recrutementformateur.dto.OffreFormateurDto;
import inpg.sn.lms.recrutementformateur.entity.OffreRecrutementFormateur;
import inpg.sn.lms.recrutementformateur.enums.EnumStatutOffreFormateur;
import inpg.sn.lms.recrutementformateur.repository.OffreRecrutementFormateurRepository;

/**
 * Vitrine publique des offres de recrutement de formateurs (QMS-007 §2,
 * « diffusion de l'information »). Seules les offres {@code PUBLIEE} sont
 * exposées ; {@code permitAll} dans {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/recrutement-formateurs/public")
public class RecrutementFormateurPublicController {

    private final OffreRecrutementFormateurRepository offreRepository;

    public RecrutementFormateurPublicController(OffreRecrutementFormateurRepository offreRepository) {
        this.offreRepository = offreRepository;
    }

    @GetMapping("/offres")
    @Transactional(readOnly = true)
    public ResponseEntity<List<OffreFormateurDto>> offres() {
        return ResponseEntity.ok(
                offreRepository.findByStatutOrderByDatePublicationDesc(EnumStatutOffreFormateur.PUBLIEE).stream()
                        .map(RecrutementFormateurPublicController::toDto)
                        .toList());
    }

    @GetMapping("/offres/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> offre(@PathVariable Long id) {
        OffreRecrutementFormateur offre = offreRepository.findById(id).orElse(null);
        if (offre == null || offre.getStatut() != EnumStatutOffreFormateur.PUBLIEE) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toDto(offre));
    }

    /** Fiche de poste d'une offre publiée : un candidat doit pouvoir la lire avant de postuler. */
    @GetMapping("/offres/{id}/fiche-poste")
    @Transactional(readOnly = true)
    public ResponseEntity<?> fichePoste(@PathVariable Long id) {
        OffreRecrutementFormateur offre = offreRepository.findById(id).orElse(null);
        if (offre == null || offre.getStatut() != EnumStatutOffreFormateur.PUBLIEE || offre.getFichePoste() == null) {
            return ResponseEntity.notFound().build();
        }
        return CandidatFormateurController.servirFichier(offre.getFichePoste());
    }

    /** Vue publique : pas de comptage de candidatures (0), rien de sensible dans une offre d'emploi. */
    static OffreFormateurDto toDto(OffreRecrutementFormateur o) {
        return new OffreFormateurDto(
                o.getId(),
                o.getIntitule(),
                o.getDomaine(),
                o.getDescriptionMissions(),
                o.getProfilRecherche(),
                o.getTypeContrat() != null ? o.getTypeContrat().name() : null,
                o.getLieu(),
                o.getNbPostes(),
                o.getFormation() != null ? o.getFormation().getId() : null,
                o.getFormation() != null ? o.getFormation().getIntitule() : null,
                o.getDatePublication(),
                o.getDateLimiteCandidature(),
                o.getStatut().name(),
                o.getDateCreation(),
                0L,
                o.getFichePoste() != null ? o.getFichePoste().getNomOriginal() : null);
    }
}
