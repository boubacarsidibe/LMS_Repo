package inpg.sn.lms.personnel.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;
import inpg.sn.lms.personnel.dto.ActiviteCreateRequest;
import inpg.sn.lms.personnel.dto.ActiviteDto;
import inpg.sn.lms.personnel.dto.DepartementCreateRequest;
import inpg.sn.lms.personnel.dto.DepartementDto;
import inpg.sn.lms.personnel.entity.ActivitePersonnel;
import inpg.sn.lms.personnel.entity.Departement;
import inpg.sn.lms.personnel.enums.EnumTypeActivite;
import inpg.sn.lms.personnel.repository.ActivitePersonnelRepository;
import inpg.sn.lms.personnel.repository.DepartementRepository;
import inpg.sn.lms.personnel.service.RapportActivitePdfService;

/**
 * Suivi des activités du personnel INPG (TDR §I) : « chaque département de
 * l'INPG [peut] saisir ses activités mensuelles (formations, projets,
 * recherches, etc.) » avec génération de rapports mensuels par département,
 * consolidés en un rapport global.
 *
 * <p>Le TDR ne fournit ni organigramme ni notion d'affectation d'un compte à
 * un département : la saisie porte sur le catalogue de départements, pas sur
 * une hiérarchie inventée. Ouvert aux trois rôles internes déjà établis dans
 * ce dépôt comme « personnel » (par opposition aux rôles candidats/étudiants/
 * externes) : formateur, responsable pédagogique, responsable admission.
 * Le catalogue des départements reste géré par la pédagogie, qui gère déjà
 * d'autres référentiels de ce type (formations, promotions).
 */
@RestController
@RequestMapping("/api/personnel")
@PreAuthorize("hasAnyRole('FORMATEUR', 'RESPONSABLE_PEDAGOGIQUE', 'RESPONSABLE_ADMISSION')")
public class PersonnelActiviteController {

    private final DepartementRepository departementRepository;
    private final ActivitePersonnelRepository activiteRepository;
    private final UtilisateurCourantService utilisateurCourant;
    private final RapportActivitePdfService rapportPdfService;

    public PersonnelActiviteController(DepartementRepository departementRepository,
            ActivitePersonnelRepository activiteRepository, UtilisateurCourantService utilisateurCourant,
            RapportActivitePdfService rapportPdfService) {
        this.departementRepository = departementRepository;
        this.activiteRepository = activiteRepository;
        this.utilisateurCourant = utilisateurCourant;
        this.rapportPdfService = rapportPdfService;
    }

    // ===================== DÉPARTEMENTS =====================

    @GetMapping("/departements")
    @Transactional(readOnly = true)
    public ResponseEntity<List<DepartementDto>> listerDepartements() {
        return ResponseEntity.ok(departementRepository.findByActifTrueOrderByLibelleAsc().stream()
                .map(this::toDepartementDto)
                .toList());
    }

    /** Réservé à la pédagogie, qui gère déjà les autres référentiels de l'offre de formation. */
    @PostMapping("/departements")
    @PreAuthorize("hasRole('RESPONSABLE_PEDAGOGIQUE')")
    @Transactional
    public ResponseEntity<?> creerDepartement(@RequestBody DepartementCreateRequest requete) {
        if (estVide(requete.code()) || estVide(requete.libelle())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le code et le libellé sont requis."));
        }
        if (departementRepository.findByCode(requete.code().trim()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Ce code de département existe déjà."));
        }
        Departement departement = new Departement();
        departement.setCode(requete.code().trim());
        departement.setLibelle(requete.libelle().trim());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toDepartementDto(departementRepository.save(departement)));
    }

    /** Désactive un département plutôt que de le supprimer : ses activités passées restent consultables. */
    @PutMapping("/departements/{id}/desactiver")
    @PreAuthorize("hasRole('RESPONSABLE_PEDAGOGIQUE')")
    @Transactional
    public ResponseEntity<?> desactiverDepartement(@PathVariable Long id) {
        Departement departement = departementRepository.findById(id).orElse(null);
        if (departement == null) {
            return ResponseEntity.notFound().build();
        }
        departement.setActif(false);
        return ResponseEntity.ok(toDepartementDto(departementRepository.save(departement)));
    }

    // ===================== ACTIVITÉS =====================

    /** Activités de la période, tous départements confondus si {@code departementId} est absent. */
    @GetMapping("/activites")
    @Transactional(readOnly = true)
    public ResponseEntity<?> listerActivites(
            @RequestParam(required = false) Long departementId,
            @RequestParam int mois,
            @RequestParam int annee) {
        if (mois < 1 || mois > 12) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le mois doit être compris entre 1 et 12."));
        }
        List<ActivitePersonnel> activites = departementId != null
                ? activiteRepository.findByDepartementIdAndAnneeAndMoisOrderByDateCreationDesc(departementId, annee, mois)
                : activiteRepository.findByAnneeAndMoisOrderByDepartement_LibelleAscDateCreationDesc(annee, mois);
        return ResponseEntity.ok(activites.stream().map(this::toActiviteDto).toList());
    }

    /** Activités saisies par la personne connectée, toutes périodes confondues. */
    @GetMapping("/activites/mes-activites")
    @Transactional(readOnly = true)
    public ResponseEntity<?> mesActivites(Authentication authentication) {
        User utilisateur = utilisateurCourant.utilisateur(authentication);
        if (utilisateur == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(activiteRepository.findByAuteurIdOrderByAnneeDescMoisDescDateCreationDesc(
                utilisateur.getId()).stream().map(this::toActiviteDto).toList());
    }

    @PostMapping("/activites")
    @Transactional
    public ResponseEntity<?> saisirActivite(@RequestBody ActiviteCreateRequest requete, Authentication authentication) {
        if (requete.departementId() == null || estVide(requete.titre())
                || requete.mois() == null || requete.annee() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Le département, le titre, le mois et l'année sont requis."));
        }
        if (requete.mois() < 1 || requete.mois() > 12) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le mois doit être compris entre 1 et 12."));
        }
        Departement departement = departementRepository.findById(requete.departementId()).orElse(null);
        if (departement == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Département introuvable."));
        }
        EnumTypeActivite type;
        try {
            type = requete.typeActivite() == null ? EnumTypeActivite.AUTRE
                    : EnumTypeActivite.valueOf(requete.typeActivite());
        } catch (IllegalArgumentException invalide) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    "Le type d'activité doit valoir FORMATION, PROJET, RECHERCHE ou AUTRE."));
        }
        User auteur = utilisateurCourant.utilisateur(authentication);
        if (auteur == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        ActivitePersonnel activite = new ActivitePersonnel(departement, auteur, type, requete.titre().trim(),
                estVide(requete.description()) ? null : requete.description().trim(), requete.mois(), requete.annee());
        return ResponseEntity.status(HttpStatus.CREATED).body(toActiviteDto(activiteRepository.save(activite)));
    }

    /** L'auteur peut retirer sa propre saisie ; la pédagogie et l'admission peuvent modérer celle de tiers. */
    @DeleteMapping("/activites/{id}")
    @Transactional
    public ResponseEntity<?> supprimerActivite(@PathVariable Long id, Authentication authentication) {
        ActivitePersonnel activite = activiteRepository.findById(id).orElse(null);
        if (activite == null) {
            return ResponseEntity.notFound().build();
        }
        User utilisateur = utilisateurCourant.utilisateur(authentication);
        boolean estAuteur = utilisateur != null && activite.getAuteur() != null
                && activite.getAuteur().getId().equals(utilisateur.getId());
        boolean estModerateur = utilisateur != null && utilisateur.getRoles().stream()
                .anyMatch(r -> "ROLE_RESPONSABLE_PEDAGOGIQUE".equals(r.getCode())
                        || "ROLE_RESPONSABLE_ADMISSION".equals(r.getCode()));
        if (!estAuteur && !estModerateur) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Seul l'auteur ou la direction peut retirer cette activité."));
        }
        activiteRepository.delete(activite);
        return ResponseEntity.noContent().build();
    }

    // ===================== RAPPORTS =====================

    @GetMapping("/rapports/departement/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> rapportDepartement(@PathVariable Long id, @RequestParam int mois, @RequestParam int annee) {
        Departement departement = departementRepository.findById(id).orElse(null);
        if (departement == null) {
            return ResponseEntity.notFound().build();
        }
        List<ActivitePersonnel> activites = activiteRepository
                .findByDepartementIdAndAnneeAndMoisOrderByDateCreationDesc(id, annee, mois);
        byte[] pdf = rapportPdfService.genererRapportDepartement(departement.getLibelle(), mois, annee, activites);
        return pdfReponse(pdf, "rapport-" + departement.getCode() + "-" + annee + "-" + mois + ".pdf");
    }

    @GetMapping("/rapports/global")
    @Transactional(readOnly = true)
    public ResponseEntity<?> rapportGlobal(@RequestParam int mois, @RequestParam int annee) {
        List<ActivitePersonnel> activites = activiteRepository
                .findByAnneeAndMoisOrderByDepartement_LibelleAscDateCreationDesc(annee, mois);
        byte[] pdf = rapportPdfService.genererRapportGlobal(mois, annee, activites);
        return pdfReponse(pdf, "rapport-global-inpg-" + annee + "-" + mois + ".pdf");
    }

    private ResponseEntity<byte[]> pdfReponse(byte[] pdf, String nomFichier) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(nomFichier).build().toString())
                .body(pdf);
    }

    // ===================== Correspondances =====================

    private DepartementDto toDepartementDto(Departement departement) {
        return new DepartementDto(departement.getId(), departement.getCode(), departement.getLibelle(),
                departement.isActif());
    }

    private ActiviteDto toActiviteDto(ActivitePersonnel activite) {
        return new ActiviteDto(
                activite.getId(),
                activite.getDepartement() == null ? null : activite.getDepartement().getId(),
                activite.getDepartement() == null ? null : activite.getDepartement().getLibelle(),
                utilisateurCourant.nomAffiche(activite.getAuteur()),
                activite.getTypeActivite().name(),
                activite.getTitre(),
                activite.getDescription(),
                activite.getMois(),
                activite.getAnnee(),
                activite.getDateCreation());
    }

    private boolean estVide(String valeur) {
        return valeur == null || valeur.trim().isEmpty();
    }
}
