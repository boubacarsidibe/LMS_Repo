package inpg.sn.lms.espace_etudiant.controller;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import inpg.sn.lms.auth.entity.Formateur;
import inpg.sn.lms.auth.repository.FormateurRepository;
import inpg.sn.lms.espace_etudiant.dto.AffectationFormateursRequest;
import inpg.sn.lms.espace_etudiant.dto.CoursCreateRequest;
import inpg.sn.lms.espace_etudiant.dto.CoursDto;
import inpg.sn.lms.espace_etudiant.dto.FormateurBrefDto;
import inpg.sn.lms.espace_etudiant.dto.FormationDto;
import inpg.sn.lms.espace_etudiant.dto.PromotionDto;
import inpg.sn.lms.espace_etudiant.dto.SupportDetailDto;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.Promotion;
import inpg.sn.lms.espace_etudiant.entity.SupportCours;
import inpg.sn.lms.espace_etudiant.service.CoursService;
import inpg.sn.lms.espace_etudiant.service.FormationService;
import inpg.sn.lms.espace_etudiant.service.PromotionService;
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
    private final FormateurRepository formateurRepository;

    public PedagogieController(
            FormationService formationService,
            PromotionService promotionService,
            CoursService coursService,
            SupportService supportService,
            FormateurRepository formateurRepository) {
        this.formationService = formationService;
        this.promotionService = promotionService;
        this.coursService = coursService;
        this.supportService = supportService;
        this.formateurRepository = formateurRepository;
    }

    // ===================== OFFRE DE FORMATION =====================

    @Transactional(readOnly = true)
    @GetMapping("/formations")
    public ResponseEntity<List<FormationDto>> listFormations() {
        return ResponseEntity.ok(formationService.getAll().stream()
                .map(f -> new FormationDto(f.getId(), f.getCode(), f.getIntitule()))
                .toList());
    }

    /**
     * Formateurs disponibles pour l'affectation à un cours.
     *
     * <p>Ne renvoie que l'identité : le téléphone porté par l'entité n'a pas à
     * circuler pour composer une équipe pédagogique.
     */
    @Transactional(readOnly = true)
    @GetMapping("/formateurs")
    public ResponseEntity<List<FormateurBrefDto>> listFormateurs() {
        return ResponseEntity.ok(formateurRepository.findAll().stream()
                .map(PedagogieController::toFormateurDto)
                .sorted(Comparator.comparing(FormateurBrefDto::nomComplet))
                .toList());
    }

    // ===================== COURS =====================

    @Transactional(readOnly = true)
    @GetMapping("/cours")
    public ResponseEntity<List<CoursDto>> listCours() {
        return ResponseEntity.ok(coursService.getAll().stream().map(this::toCoursDto).toList());
    }

    @Transactional(readOnly = true)
    @GetMapping("/cours/{id}")
    public ResponseEntity<?> getCours(@PathVariable Long id) {
        return coursService.findById(id)
                .<ResponseEntity<?>>map(cours -> ResponseEntity.ok(toCoursDto(cours)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Transactional
    @PostMapping("/cours")
    public ResponseEntity<?> creerCours(@RequestBody CoursCreateRequest request) {
        String erreur = validerCours(request);
        if (erreur != null) {
            return ResponseEntity.badRequest().body(Map.of("message", erreur));
        }
        Cours cours = new Cours();
        appliquer(request, cours);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toCoursDto(rattacher(request, coursService.save(cours))));
    }

    @Transactional
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
        return ResponseEntity.ok(toCoursDto(rattacher(request, coursService.save(cours))));
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
        // La table formation_cours reference le cours : sans ce detachement,
        // la suppression echoue sur une violation de cle etrangere.
        formationService.detacherDeToutProgramme(cours);
        coursService.delete(cours);
        return ResponseEntity.noContent().build();
    }

    /**
     * Remplace l'équipe pédagogique d'un cours.
     *
     * <p>La table {@code cours_formateur} commande tout le cloisonnement enseignant :
     * {@code PerimetreFormateurService} y lit « ce cours est-il le mien ? ». Elle
     * n'était pourtant écrite que par le formateur créant son propre cours — un cours
     * créé ici naissait donc orphelin, invisible de tout espace formateur, donc sans
     * support, sans séance et sans note possibles. Six des neuf cours en base étaient
     * dans ce cas.
     *
     * <p>Une liste vide est refusée : elle rendrait le cours orphelin, ce que rien à
     * l'écran ne signalerait ensuite.
     */
    @Transactional
    @PutMapping("/cours/{id}/formateurs")
    public ResponseEntity<?> affecterFormateurs(
            @PathVariable Long id,
            @RequestBody AffectationFormateursRequest request) {
        Cours cours = coursService.findById(id).orElse(null);
        if (cours == null) {
            return ResponseEntity.notFound().build();
        }
        if (request.formateurIds() == null || request.formateurIds().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    "Un cours doit compter au moins un formateur : sans cela il n'apparaît dans aucun espace enseignant."));
        }

        List<Formateur> formateurs = request.formateurIds().stream()
                .distinct()
                .map(formateurRepository::findById)
                .flatMap(Optional::stream)
                .toList();
        if (formateurs.size() != request.formateurIds().stream().distinct().count()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Un formateur demandé est introuvable."));
        }

        cours.getFormateurs().clear();
        cours.getFormateurs().addAll(formateurs);
        return ResponseEntity.ok(toCoursDto(coursService.save(cours)));
    }

    // ===================== SUPPORTS =====================

    /**
     * Supports d'un cours, en consultation seule.
     *
     * <p>La publication et le retrait ont été rendus au formateur, à qui ils
     * reviennent : c'est lui qui conçoit le contenu de son cours. Le responsable
     * pédagogique garde la vue — vérifier qu'un cours dispose bien de supports fait
     * partie du suivi — mais n'écrit plus à la place de l'enseignant.
     */
    @Transactional(readOnly = true)
    @GetMapping("/cours/{id}/supports")
    public ResponseEntity<List<SupportDetailDto>> listSupports(@PathVariable Long id) {
        if (coursService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(supportService.getCourseSupports(id).stream()
                .map(this::toSupportDetailDto)
                .toList());
    }

    // ===================== Méthodes privées =====================

    private static FormateurBrefDto toFormateurDto(Formateur formateur) {
        return new FormateurBrefDto(formateur.getId(),
                (formateur.getPrenom() + " " + formateur.getNom()).trim());
    }

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

    /**
     * Inscrit le cours au programme des promotions demandees.
     *
     * <p>{@code cohorteIds} etait recu puis purement ignore : un cours cree ici
     * n'etait rattache a rien, donc invisible pour tous les etudiants et tous les
     * formateurs. C'est pourtant le chemin officiel de creation de l'offre.
     *
     * <p>Le cours doit deja etre enregistre : c'est la formation qui porte la table
     * de jointure et elle a besoin de l'identifiant.
     */
    private Cours rattacher(CoursCreateRequest request, Cours coursEnregistre) {
        if (request.cohorteIds() == null) {
            return coursEnregistre;
        }
        List<Promotion> promotions = request.cohorteIds().stream()
                .map(promotionService::findById)
                .flatMap(Optional::stream)
                .toList();
        formationService.rattacherAuxFormationsDe(coursEnregistre, promotions);
        return coursService.findById(coursEnregistre.getId()).orElse(coursEnregistre);
    }

    private boolean estVide(String valeur) {
        return valeur == null || valeur.trim().isEmpty();
    }

    private CoursDto toCoursDto(Cours cours) {
        List<PromotionDto> promotions = promotionService.desFormationsDuCours(cours).stream()
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
                cours.getFormateurs().stream().map(PedagogieController::toFormateurDto).toList(),
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
