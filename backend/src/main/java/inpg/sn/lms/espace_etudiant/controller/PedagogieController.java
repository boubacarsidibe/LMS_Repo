package inpg.sn.lms.espace_etudiant.controller;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

import inpg.sn.lms.auth.dto.FormateurDto;
import inpg.sn.lms.auth.entity.Formateur;
import inpg.sn.lms.auth.repository.FormateurRepository;
import inpg.sn.lms.auth.service.FormateurService;
import inpg.sn.lms.espace_etudiant.dto.AffectationFormateursRequest;
import inpg.sn.lms.espace_etudiant.dto.CoursCreateRequest;
import inpg.sn.lms.espace_etudiant.dto.CoursDto;
import inpg.sn.lms.espace_etudiant.dto.FormateurBrefDto;
import inpg.sn.lms.espace_etudiant.dto.FormateurDetailDto;
import inpg.sn.lms.espace_etudiant.dto.FormationDto;
import inpg.sn.lms.espace_etudiant.dto.PromotionDto;
import inpg.sn.lms.espace_etudiant.dto.RapportFormateursDto;
import inpg.sn.lms.espace_etudiant.dto.SupportDetailDto;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.Promotion;
import inpg.sn.lms.espace_etudiant.entity.SupportCours;
import inpg.sn.lms.espace_etudiant.repository.ForumMentionRepository;
import inpg.sn.lms.espace_etudiant.repository.ForumMessageRepository;
import inpg.sn.lms.espace_etudiant.repository.ForumSujetRepository;
import inpg.sn.lms.espace_etudiant.repository.NoteModuleRepository;
import inpg.sn.lms.espace_etudiant.repository.PresenceRepository;
import inpg.sn.lms.espace_etudiant.repository.RetourEvaluationRepository;
import inpg.sn.lms.espace_etudiant.repository.SeanceRepository;
import inpg.sn.lms.espace_etudiant.service.CoursService;
import inpg.sn.lms.espace_etudiant.service.FormateurSuppressionService;
import inpg.sn.lms.espace_etudiant.service.RapportFormateursPdfService;
import inpg.sn.lms.espace_etudiant.service.FormationService;
import inpg.sn.lms.espace_etudiant.service.PromotionService;
import inpg.sn.lms.espace_etudiant.service.SupportService;
import inpg.sn.lms.recrutement.service.NotificationService;

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

    private static final Logger log = LoggerFactory.getLogger(PedagogieController.class);
    private static final String COULEUR_BANDEAU_PAR_DEFAUT = "bg-secondary-container";

    private final FormationService formationService;
    private final PromotionService promotionService;
    private final CoursService coursService;
    private final SupportService supportService;
    private final FormateurRepository formateurRepository;
    private final FormateurService formateurService;
    private final FormateurSuppressionService formateurSuppressionService;
    private final NotificationService notificationService;
    private final PresenceRepository presenceRepository;
    private final SeanceRepository seanceRepository;
    private final ForumMentionRepository forumMentionRepository;
    private final ForumMessageRepository forumMessageRepository;
    private final ForumSujetRepository forumSujetRepository;
    private final NoteModuleRepository noteModuleRepository;
    private final RetourEvaluationRepository retourEvaluationRepository;
    private final RapportFormateursPdfService rapportFormateursPdfService;

    public PedagogieController(
            FormationService formationService,
            PromotionService promotionService,
            CoursService coursService,
            SupportService supportService,
            FormateurRepository formateurRepository,
            FormateurService formateurService,
            FormateurSuppressionService formateurSuppressionService,
            NotificationService notificationService,
            PresenceRepository presenceRepository,
            SeanceRepository seanceRepository,
            ForumMentionRepository forumMentionRepository,
            ForumMessageRepository forumMessageRepository,
            ForumSujetRepository forumSujetRepository,
            NoteModuleRepository noteModuleRepository,
            RetourEvaluationRepository retourEvaluationRepository,
            RapportFormateursPdfService rapportFormateursPdfService) {
        this.formationService = formationService;
        this.promotionService = promotionService;
        this.coursService = coursService;
        this.supportService = supportService;
        this.formateurRepository = formateurRepository;
        this.formateurService = formateurService;
        this.formateurSuppressionService = formateurSuppressionService;
        this.notificationService = notificationService;
        this.presenceRepository = presenceRepository;
        this.seanceRepository = seanceRepository;
        this.forumMentionRepository = forumMentionRepository;
        this.forumMessageRepository = forumMessageRepository;
        this.forumSujetRepository = forumSujetRepository;
        this.noteModuleRepository = noteModuleRepository;
        this.retourEvaluationRepository = retourEvaluationRepository;
        this.rapportFormateursPdfService = rapportFormateursPdfService;
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

    /**
     * Fiche complète d'un formateur : coordonnées et cours enseignés, à la
     * différence de {@link #listFormateurs} qui n'expose que l'identité.
     */
    @Transactional(readOnly = true)
    @GetMapping("/formateurs/{id}")
    public ResponseEntity<?> detailFormateur(@PathVariable Long id) {
        return formateurRepository.findById(id)
                .<ResponseEntity<?>>map(f -> ResponseEntity.ok(toFormateurDetailDto(f)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Rapport des comptes formateurs, en PDF : effectif complet avec, pour
     * chacun, ses coordonnées et ses cours affectés — signale distinctement les
     * formateurs sans aucun cours, invisibles dans leur propre espace tant que
     * {@code cours_formateur} ne les y rattache pas (voir CLAUDE.md §3).
     */
    @Transactional(readOnly = true)
    @GetMapping("/formateurs/rapport/pdf")
    public ResponseEntity<byte[]> genererRapportFormateurs() {
        byte[] pdf = rapportFormateursPdfService.genererRapport(construireRapportFormateurs());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("rapport_formateurs_" + java.time.LocalDate.now() + ".pdf", java.nio.charset.StandardCharsets.UTF_8)
                .build());
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).headers(headers).body(pdf);
    }

    private RapportFormateursDto construireRapportFormateurs() {
        List<Cours> tousCours = coursService.getAll();
        List<RapportFormateursDto.FormateurRapport> formateurs = formateurRepository.findAll().stream()
                .sorted(Comparator.comparing(f -> (f.getPrenom() + " " + f.getNom()).trim()))
                .map(f -> new RapportFormateursDto.FormateurRapport(
                        f.getNom(),
                        f.getPrenom(),
                        f.getUser() == null ? null : f.getUser().getEmail(),
                        f.getTelephone(),
                        f.getUser() == null ? null : f.getUser().getUsername(),
                        f.getUser() == null ? null : f.getUser().getDateCreation(),
                        tousCours.stream()
                                .filter(c -> c.getFormateurs().stream().anyMatch(ft -> ft.getId().equals(f.getId())))
                                .map(Cours::getTitre)
                                .toList()))
                .toList();
        int sansCours = (int) formateurs.stream().filter(f -> f.coursEnseignes().isEmpty()).count();
        return new RapportFormateursDto(formateurs.size(), sansCours, formateurs);
    }

    /**
     * Supprime un compte formateur. Refusée s'il est seul titulaire d'un cours, s'il
     * a un dossier de candidature ou un historique alumni, ou s'il a publié dans le
     * forum — voir {@link FormateurSuppressionService}.
     */
    @Transactional
    @DeleteMapping("/formateurs/{id}")
    public ResponseEntity<?> supprimerFormateur(@PathVariable Long id) {
        if (!formateurRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        try {
            formateurSuppressionService.supprimer(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException erreur) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", erreur.getMessage()));
        }
    }

    /**
     * Ouvre un compte formateur. Jusqu'ici, seul {@code POST /api/auth/formateurs}
     * existait, réservé à RESPONSABLE_ADMISSION — la pédagogie, qui affecte les
     * formateurs aux cours, ne pouvait pas ouvrir leur compte elle-même.
     *
     * <p>Le mot de passe est optionnel : laissé vide, {@link FormateurService} en
     * génère un et le réécrit dans {@code requete}, envoyé par courriel ci-dessous.
     * Le compte créé n'apparaît dans aucun espace formateur tant qu'il n'est pas
     * rattaché à au moins un cours ({@code PUT /cours/{id}/formateurs}) — le message
     * de succès le rappelle.
     */
    @Transactional
    @PostMapping("/formateurs")
    public ResponseEntity<?> creerFormateur(@RequestBody FormateurDto requete) {
        if (estVide(requete.getUsername()) || estVide(requete.getEmail())
                || estVide(requete.getNom()) || estVide(requete.getPrenom()) || estVide(requete.getTelephone())) {
            // Le telephone n'est pas facultatif : Formateur.telephone est NOT NULL UNIQUE
            // en base. Sans ce garde-fou, une omission se solde par une violation de
            // contrainte non rattrapee, remontee en 500 sans message (meme defaut que
            // PedagogieScolariteController.creerEtudiant, voir EtudiantImportService).
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Le nom, le prénom, l'identifiant, l'email et le téléphone sont requis."));
        }
        // Formateur.telephone est NOT NULL UNIQUE : un doublon lèverait une
        // DataIntegrityViolationException au commit, hors de portée du catch
        // ci-dessous, et remonterait en 500 « La création du compte a échoué »
        // sans dire pourquoi. On constate le conflit avant d'écrire.
        if (formateurRepository.findByTelephone(requete.getTelephone().trim()).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Ce numéro de téléphone est déjà associé à un formateur."));
        }
        boolean motDePasseGenere = estVide(requete.getPassword());
        try {
            Formateur formateur = formateurService.createFormateur(requete);
            if (motDePasseGenere) {
                envoyerIdentifiants(requete.getEmail(), requete.getUsername(), requete.getPassword());
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "id", formateur.getId(),
                    "message",
                    "Formateur créé avec succès. Rattachez-le à au moins un cours pour qu'il apparaisse dans son espace."));
        } catch (IllegalStateException erreur) {
            return ResponseEntity.badRequest().body(Map.of("message", erreur.getMessage()));
        }
    }

    /**
     * Transmet les identifiants d'un compte formateur nouvellement créé. Une panne
     * SMTP ne doit pas faire échouer la création elle-même — le compte reste
     * utilisable, seul l'envoi manque.
     */
    private void envoyerIdentifiants(String email, String identifiant, String motDePasse) {
        try {
            notificationService.envoyerEmail(email, "Votre compte INPG LMS",
                    "Un compte vient de vous être créé sur la plateforme INPG LMS.\n\n"
                            + "Identifiant : " + identifiant + "\n"
                            + "Mot de passe provisoire : " + motDePasse + "\n\n"
                            + "Merci de le modifier dès votre première connexion.");
        } catch (Exception exception) {
            log.error("Échec de l'envoi des identifiants à {} : {}", email, exception.getMessage());
        }
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

    /**
     * Le cours est référencé par sept tables (notes, séances, présences, avis,
     * fils de forum...) toutes en {@code ON DELETE} restrictif : la ligne
     * {@code cours} ne peut disparaître tant qu'une seule d'entre elles pointe
     * encore vers elle. L'oubli d'une seule faisait échouer la suppression avec
     * une violation de clé étrangère, sans qu'aucun message ne le précise —
     * l'écran affichait juste « la suppression du cours a échoué ».
     */
    @DeleteMapping("/cours/{id}")
    @Transactional
    public ResponseEntity<?> supprimerCours(@PathVariable Long id) {
        Cours cours = coursService.findById(id).orElse(null);
        if (cours == null) {
            return ResponseEntity.notFound().build();
        }
        presenceRepository.deleteBySeance_CoursId(cours.getId());
        seanceRepository.deleteByCoursId(cours.getId());
        forumMentionRepository.deleteByMessage_Sujet_CoursId(cours.getId());
        forumMessageRepository.deleteBySujet_CoursId(cours.getId());
        forumSujetRepository.deleteByCoursId(cours.getId());
        noteModuleRepository.deleteByCoursId(cours.getId());
        retourEvaluationRepository.deleteByCoursId(cours.getId());
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

    /** Trié : {@code Cours.formations} est un HashSet, dont l'ordre varie d'un appel à l'autre. */
    private static List<FormationDto> toFormationDtos(Cours cours) {
        return cours.getFormations().stream()
                .map(f -> new FormationDto(f.getId(), f.getCode(), f.getIntitule()))
                .sorted(Comparator.comparing(FormationDto::intitule,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private static FormateurBrefDto toFormateurDto(Formateur formateur) {
        return new FormateurBrefDto(formateur.getId(),
                (formateur.getPrenom() + " " + formateur.getNom()).trim());
    }

    private FormateurDetailDto toFormateurDetailDto(Formateur formateur) {
        return new FormateurDetailDto(
                formateur.getId(),
                formateur.getNom(),
                formateur.getPrenom(),
                formateur.getUser() == null ? null : formateur.getUser().getEmail(),
                formateur.getTelephone(),
                formateur.getUser() == null ? null : formateur.getUser().getUsername(),
                coursService.getAll().stream()
                        .filter(c -> c.getFormateurs().stream().anyMatch(f -> f.getId().equals(formateur.getId())))
                        .map(Cours::getTitre)
                        .toList());
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
        // Absent ou invalide : on garde la valeur en place plutot que d'imposer 1, sans
        // quoi une modification du titre remettrait le coefficient a plat.
        if (request.coefficient() != null && request.coefficient() > 0) {
            cours.setCoefficient(request.coefficient());
        }
    }

    /**
     * Inscrit le cours au programme demande.
     *
     * <p>Le responsable pedagogique designe des formations : c'est le rattachement
     * reel, et il vaut pour toutes les promotions a venir. La voie par promotions
     * reste acceptee pour l'espace formateur, qui raisonne ainsi.
     *
     * <p>Aucun des deux champs renseigne : le programme n'est pas touche. C'est la
     * difference avec une liste vide, qui retire le cours de tout programme — les
     * confondre effacerait le rattachement a chaque modification du titre.
     *
     * <p>Le cours doit deja etre enregistre : c'est la formation qui porte la table
     * de jointure et elle a besoin de l'identifiant.
     */
    private Cours rattacher(CoursCreateRequest request, Cours coursEnregistre) {
        if (request.formationIds() != null) {
            formationService.rattacherAuxFormations(coursEnregistre, request.formationIds());
        } else if (request.cohorteIds() != null) {
            List<Promotion> promotions = request.cohorteIds().stream()
                    .map(promotionService::findById)
                    .flatMap(Optional::stream)
                    .toList();
            formationService.rattacherAuxFormationsDe(coursEnregistre, promotions);
        } else {
            return coursEnregistre;
        }
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
                cours.getCoefficient(),
                0,
                supportService.countCourseSupports(cours.getId()),
                0,
                COULEUR_BANDEAU_PAR_DEFAUT,
                promotions,
                toFormationDtos(cours),
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
