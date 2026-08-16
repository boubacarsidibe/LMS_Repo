package inpg.sn.lms.espace_etudiant.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;

import inpg.sn.lms.espace_etudiant.dto.CoursCreateRequest;
import inpg.sn.lms.espace_etudiant.dto.CoursDto;
import inpg.sn.lms.espace_etudiant.dto.FormationDto;
import inpg.sn.lms.espace_etudiant.dto.PromotionDto;
import inpg.sn.lms.espace_etudiant.dto.SupportDetailDto;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.SupportCours;
import inpg.sn.lms.espace_etudiant.service.CoursService;
import inpg.sn.lms.espace_etudiant.service.FormationService;
import inpg.sn.lms.espace_etudiant.service.PromotionService;
import inpg.sn.lms.espace_etudiant.service.SupportCoursStorageService;
import inpg.sn.lms.espace_etudiant.service.SupportService;

/**
 * Espace du responsable pédagogique : pilotage de l'offre d'enseignement.
 *
 * <p>Se distingue des endpoints /api/espace-etudiant/formateur/**, qui sont
 * cadrés par la propriété du cours : le responsable pédagogique intervient sur
 * l'ensemble des cours, quel que soit le formateur rattaché. L'accès est
 * restreint à ROLE_RESPONSABLE_PEDAGOGIQUE dans SecurityConfig.
 */
@RestController
@RequestMapping("/api/pedagogie")
@CrossOrigin(origins = "http://localhost:4200")
/*
 * Autorisation rendue explicite : elle ne reposait que sur le prefixe d'URL filtre
 * dans SecurityConfig. Un endpoint ajoute hors de ce prefixe aurait ete ouvert sans
 * que rien ne le signale. Les regles d'URL restent en place et s'appliquent en plus.
 */
@PreAuthorize("hasRole('RESPONSABLE_PEDAGOGIQUE')")
public class PedagogieController {

    private static final String COULEUR_BANDEAU_PAR_DEFAUT = "bg-secondary-container";

    private final FormationService formationService;
    private final PromotionService promotionService;
    private final CoursService coursService;
    private final SupportService supportService;
    private final SupportCoursStorageService supportCoursStorageService;

    public PedagogieController(
            FormationService formationService,
            PromotionService promotionService,
            CoursService coursService,
            SupportService supportService,
            SupportCoursStorageService supportCoursStorageService) {
        this.formationService = formationService;
        this.promotionService = promotionService;
        this.coursService = coursService;
        this.supportService = supportService;
        this.supportCoursStorageService = supportCoursStorageService;
    }

    // ===================== OFFRE DE FORMATION =====================

    @GetMapping("/formations")
    public ResponseEntity<List<FormationDto>> listFormations() {
        return ResponseEntity.ok(formationService.getAll().stream()
                .map(f -> new FormationDto(f.getId(), f.getCode(), f.getIntitule()))
                .toList());
    }

    @GetMapping("/promotions")
    public ResponseEntity<List<PromotionDto>> listPromotions() {
        return ResponseEntity.ok(promotionService.getAll().stream()
                .map(p -> new PromotionDto(p.getId(), p.getCode(), p.getLibelle(),
                        p.getDateDebut() != null ? p.getDateDebut().getYear() : null))
                .toList());
    }

    // ===================== COURS =====================

    @GetMapping("/cours")
    public ResponseEntity<List<CoursDto>> listCours() {
        return ResponseEntity.ok(coursService.getAll().stream().map(this::toCoursDto).toList());
    }

    @GetMapping("/cours/{id}")
    public ResponseEntity<?> getCours(@PathVariable Long id) {
        return coursService.findById(id)
                .<ResponseEntity<?>>map(cours -> ResponseEntity.ok(toCoursDto(cours)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/cours")
    public ResponseEntity<?> creerCours(@RequestBody CoursCreateRequest request) {
        String erreur = validerCours(request);
        if (erreur != null) {
            return ResponseEntity.badRequest().body(Map.of("message", erreur));
        }
        Cours cours = new Cours();
        appliquer(request, cours);
        return ResponseEntity.status(HttpStatus.CREATED).body(toCoursDto(coursService.save(cours)));
    }

    @PutMapping("/cours/{id}")
    public ResponseEntity<?> modifierCours(@PathVariable Long id, @RequestBody CoursCreateRequest request) {
        Cours cours = coursService.findById(id).orElse(null);
        if (cours == null) {
            return ResponseEntity.notFound().build();
        }
        String erreur = validerCours(request);
        if (erreur != null) {
            return ResponseEntity.badRequest().body(Map.of("message", erreur));
        }
        appliquer(request, cours);
        return ResponseEntity.ok(toCoursDto(coursService.save(cours)));
    }

    /** Les supports sont supprimés avec le cours : sans cela ils resteraient orphelins. */
    @DeleteMapping("/cours/{id}")
    @Transactional
    public ResponseEntity<?> supprimerCours(@PathVariable Long id) {
        Cours cours = coursService.findById(id).orElse(null);
        if (cours == null) {
            return ResponseEntity.notFound().build();
        }
        supportService.deleteCourseSupports(cours.getId());
        coursService.delete(cours);
        return ResponseEntity.noContent().build();
    }

    // ===================== SUPPORTS =====================

    @GetMapping("/cours/{id}/supports")
    public ResponseEntity<List<SupportDetailDto>> listSupports(@PathVariable Long id) {
        if (coursService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(supportService.getCourseSupports(id).stream()
                .map(this::toSupportDetailDto)
                .toList());
    }

    @PostMapping(value = "/supports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> publierSupport(
            @RequestParam Long coursId,
            @RequestParam String typeRessource,
            @RequestParam(required = false) String datePublication,
            @RequestParam("fichier") MultipartFile fichier) {
        Cours cours = coursService.findById(coursId).orElse(null);
        if (cours == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Cours introuvable."));
        }
        if (fichier.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Un fichier est requis."));
        }

        LocalDate publication;
        try {
            publication = datePublication == null || datePublication.isBlank()
                    ? LocalDate.now()
                    : LocalDate.parse(datePublication);
        } catch (DateTimeParseException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", "La date de publication est invalide."));
        }

        try {
            SupportCours support = supportCoursStorageService.publier(cours, fichier, typeRessource, publication);
            return ResponseEntity.status(HttpStatus.CREATED).body(toSupportDetailDto(support));
        } catch (IOException exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Le fichier n'a pas pu être enregistré."));
        }
    }

    @DeleteMapping("/supports/{id}")
    public ResponseEntity<?> supprimerSupport(@PathVariable Long id) {
        SupportCours support = supportService.findCourseSupportById(id).orElse(null);
        if (support == null) {
            return ResponseEntity.notFound().build();
        }
        supportService.deleteCourseSupport(support);
        return ResponseEntity.noContent().build();
    }

    // ===================== Méthodes privées =====================

    private String validerCours(CoursCreateRequest request) {
        if (estVide(request.code()) || estVide(request.titre())) {
            return "Le code et le titre du cours sont requis.";
        }
        if (request.volumeHoraire() != null && request.volumeHoraire() < 0) {
            return "Le volume horaire ne peut pas être négatif.";
        }
        return null;
    }

    /** Le code du cours est stocké dans le champ {@code nom}, comme côté formateur. */
    private void appliquer(CoursCreateRequest request, Cours cours) {
        cours.setNom(request.code().trim());
        cours.setTitre(request.titre().trim());
        cours.setDescription(request.description() == null ? "" : request.description().trim());
        cours.setSemestre(request.semestre() == null ? "" : request.semestre().trim());
        cours.setVolumeHoraire(request.volumeHoraire() == null ? 0L : request.volumeHoraire());
    }

    private boolean estVide(String valeur) {
        return valeur == null || valeur.trim().isEmpty();
    }

    private CoursDto toCoursDto(Cours cours) {
        List<PromotionDto> promotions = cours.getPromotions().stream()
                .map(p -> new PromotionDto(p.getId(), p.getCode(), p.getLibelle(),
                        p.getDateDebut() != null ? p.getDateDebut().getYear() : null))
                .toList();
        return new CoursDto(
                cours.getId(),
                cours.getNom(),
                cours.getTitre(),
                cours.getDescription(),
                cours.getSemestre(),
                cours.getVolumeHoraire(),
                0,
                supportService.countCourseSupports(cours.getId()),
                0,
                COULEUR_BANDEAU_PAR_DEFAUT,
                promotions,
                cours.getImageUrl());
    }

    private SupportDetailDto toSupportDetailDto(SupportCours support) {
        return new SupportDetailDto(
                support.getId(),
                support.getTitre(),
                support.getTypeRessource(),
                support.getUrl(),
                support.getDatePublication(),
                support.getFichierStocke() != null && !support.getFichierStocke().isBlank());
    }
}
