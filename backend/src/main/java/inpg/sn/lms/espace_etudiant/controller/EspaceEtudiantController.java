package inpg.sn.lms.espace_etudiant.controller;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.entity.Formateur;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.config.WebConfig;
import inpg.sn.lms.fichier.ValidationFichier;
import inpg.sn.lms.auth.repository.FormateurRepository;
import inpg.sn.lms.auth.repository.UserRepository;
import inpg.sn.lms.espace_etudiant.dto.CertificatCreateRequest;
import inpg.sn.lms.espace_etudiant.dto.CertificatDto;
import inpg.sn.lms.espace_etudiant.dto.CompteurParId;
import inpg.sn.lms.espace_etudiant.dto.ContactEtudiantRequest;
import inpg.sn.lms.espace_etudiant.dto.CoursCreateRequest;
import inpg.sn.lms.espace_etudiant.dto.CoursDto;
import inpg.sn.lms.espace_etudiant.dto.EtudiantSuiviDto;
import inpg.sn.lms.espace_etudiant.dto.FormateurProfileDto;
import inpg.sn.lms.espace_etudiant.dto.FormateurBrefDto;
import inpg.sn.lms.espace_etudiant.dto.FormationDto;
import inpg.sn.lms.espace_etudiant.dto.MessageDto;
import inpg.sn.lms.espace_etudiant.dto.NoteModuleCreateRequest;
import inpg.sn.lms.espace_etudiant.dto.NoteModuleDto;
import inpg.sn.lms.espace_etudiant.dto.PresenceBulkUpdateRequest;
import inpg.sn.lms.espace_etudiant.dto.PresenceEtudiantDto;
import inpg.sn.lms.espace_etudiant.dto.PresenceHistoriqueDto;
import inpg.sn.lms.espace_etudiant.dto.PromotionDto;
import inpg.sn.lms.espace_etudiant.dto.SupportDetailDto;
import inpg.sn.lms.espace_etudiant.dto.SupportDto;
import inpg.sn.lms.espace_etudiant.dto.SupportLienCreateRequest;
import inpg.sn.lms.espace_etudiant.dto.SeanceDto;
import inpg.sn.lms.espace_etudiant.dto.SeanceRequest;
import inpg.sn.lms.espace_etudiant.dto.RelanceCollectiveRequest;
import inpg.sn.lms.espace_etudiant.dto.RetourEvaluationDto;
import inpg.sn.lms.espace_etudiant.dto.SujetDto;
import inpg.sn.lms.espace_etudiant.entity.Certificat;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.Formation;
import inpg.sn.lms.espace_etudiant.entity.ForumMessage;
import inpg.sn.lms.espace_etudiant.entity.NoteModule;
import inpg.sn.lms.espace_etudiant.entity.Presence;
import inpg.sn.lms.espace_etudiant.entity.Promotion;
import inpg.sn.lms.espace_etudiant.entity.Seance;
import inpg.sn.lms.espace_etudiant.entity.RetourEvaluation;
import inpg.sn.lms.espace_etudiant.entity.ForumSujet;
import inpg.sn.lms.espace_etudiant.entity.SupportCours;
import inpg.sn.lms.espace_etudiant.enums.EnumStatutRetour;
import inpg.sn.lms.espace_etudiant.service.CertificatPdfService;
import inpg.sn.lms.espace_etudiant.service.CertificatService;
import inpg.sn.lms.espace_etudiant.service.CoursService;
import inpg.sn.lms.espace_etudiant.service.FormationService;
import inpg.sn.lms.espace_etudiant.service.ForumVisibiliteService;
import inpg.sn.lms.espace_etudiant.service.PerimetreFormateurService;
import inpg.sn.lms.espace_etudiant.service.RetourEvaluationService;
import inpg.sn.lms.espace_etudiant.repository.ForumMessageRepository;
import inpg.sn.lms.espace_etudiant.repository.ForumSujetRepository;
import inpg.sn.lms.espace_etudiant.service.NoteModuleService;
import inpg.sn.lms.espace_etudiant.service.PresenceService;
import inpg.sn.lms.espace_etudiant.service.ReleveNotesService;
import inpg.sn.lms.espace_etudiant.service.PromotionService;
import inpg.sn.lms.espace_etudiant.service.SupportCoursStorageService;
import inpg.sn.lms.espace_etudiant.service.SupportService;
import inpg.sn.lms.espace_etudiant.service.SeanceService;

@RestController
@RequestMapping("/api/espace-etudiant")
public class EspaceEtudiantController {

    private static final String DEFAULT_COURSE_COLOR = "bg-secondary-container";
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EspaceEtudiantController.class);

    private final UserRepository userRepository;
    private final EtudiantRepository etudiantRepository;
    private final FormateurRepository formateurRepository;
    private final FormationService formationService;
    private final CoursService coursService;
    private final PromotionService promotionService;
    private final SupportService supportService;
    private final ForumSujetRepository forumSujetRepository;
    private final ForumMessageRepository forumMessageRepository;
    private final CertificatService certificatService;
    private final CertificatPdfService certificatPdfService;
    private final inpg.sn.lms.recrutement.service.NotificationService notificationService;
    private final RetourEvaluationService retourEvaluationService;
    private final PerimetreFormateurService perimetreFormateurService;
    private final NoteModuleService noteModuleService;
    private final ReleveNotesService releveNotesService;
    private final PresenceService presenceService;
    private final SeanceService seanceService;
    private final SupportCoursStorageService supportCoursStorageService;
    private final ForumVisibiliteService forumVisibilite;
    private final Path uploadDirectory;

    public EspaceEtudiantController(
            UserRepository userRepository,
            EtudiantRepository etudiantRepository,
            FormateurRepository formateurRepository,
            FormationService formationService,
            CoursService coursService,
            PromotionService promotionService,
            SupportService supportService,
            ForumSujetRepository forumSujetRepository,
            ForumMessageRepository forumMessageRepository,
            CertificatService certificatService,
            CertificatPdfService certificatPdfService,
            inpg.sn.lms.recrutement.service.NotificationService notificationService,
            RetourEvaluationService retourEvaluationService,
            PerimetreFormateurService perimetreFormateurService,
            NoteModuleService noteModuleService,
            ReleveNotesService releveNotesService,
            PresenceService presenceService,
            SeanceService seanceService,
            SupportCoursStorageService supportCoursStorageService,
            ForumVisibiliteService forumVisibilite,
            @Value("${app.upload.dir:uploads}") String uploadDirectory) {
        this.userRepository = userRepository;
        this.etudiantRepository = etudiantRepository;
        this.formateurRepository = formateurRepository;
        this.formationService = formationService;
        this.coursService = coursService;
        this.promotionService = promotionService;
        this.supportService = supportService;
        this.forumSujetRepository = forumSujetRepository;
        this.forumMessageRepository = forumMessageRepository;
        this.certificatService = certificatService;
        this.certificatPdfService = certificatPdfService;
        this.notificationService = notificationService;
        this.retourEvaluationService = retourEvaluationService;
        this.perimetreFormateurService = perimetreFormateurService;
        this.noteModuleService = noteModuleService;
        this.releveNotesService = releveNotesService;
        this.presenceService = presenceService;
        this.seanceService = seanceService;
        this.supportCoursStorageService = supportCoursStorageService;
        this.forumVisibilite = forumVisibilite;
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    /**
     * Les favoris sont une association LAZY et open-in-view est désactivé : sans
     * transaction ouverte sur toute la méthode, l'entité User est détachée dès le
     * retour du repository et la lecture de la collection lève une
     * LazyInitializationException.
     */
    // Le formateur dispose de /formateur/cohortes, filtre par les cours qui lui
    // sont attribués. Cette route générale ne doit pas exposer le référentiel de
    // toutes les promotions à un formateur.
    @PreAuthorize("hasRole('ETUDIANT')")
    @GetMapping("/favorites")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Long>> getFavorites(Authentication authentication) {
        User user = findAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<Long> list = user.getFavorites().stream()
                .map(Formation::getId)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PreAuthorize("hasAnyRole('ETUDIANT','FORMATEUR')")
    @PostMapping("/favorites/{id}")
    @Transactional
    public ResponseEntity<?> toggleFavorite(@PathVariable Long id, Authentication authentication) {
        User user = findAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Formation formation = formationService.findById(id).orElse(null);
        if (formation == null) {
            return ResponseEntity.notFound().build();
        }
        Set<Formation> favoris = user.getFavorites();
        boolean nowFavorite = !favoris.removeIf(f -> f.getId().equals(formation.getId()));
        if (nowFavorite) {
            favoris.add(formation);
        }
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("favorite", nowFavorite));
    }

    @PreAuthorize("hasAnyRole('ETUDIANT','FORMATEUR')")
    @Transactional(readOnly = true)
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        User user = findAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Etudiant etudiant = etudiantRepository.findByUserId(user.getId()).orElse(null);
        return ResponseEntity.ok(toProfilePayload(user, etudiant));
    }

    /**
     * Met à jour l'identité de l'étudiant connecté. L'email n'est pas modifiable
     * ici : il sert d'identifiant de connexion et relève de l'espace
     * d'authentification, pas du profil.
     */
    @PreAuthorize("hasAnyRole('ETUDIANT','FORMATEUR')")
    @PutMapping("/profile")
    @Transactional
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> request, Authentication authentication) {
        User user = findAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Etudiant etudiant = etudiantRepository.findByUserId(user.getId()).orElse(null);
        if (etudiant == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Aucune fiche étudiant n'est associée à ce compte."));
        }

        String nom = trimOrNull(request.get("nom"));
        String prenom = trimOrNull(request.get("prenom"));
        String telephone = trimOrNull(request.get("telephone"));

        if (nom == null || prenom == null || telephone == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Le nom, le prénom et le téléphone sont obligatoires."));
        }

        // Le téléphone porte une contrainte d'unicité : on détecte le conflit ici
        // pour renvoyer un message exploitable plutôt qu'une violation de contrainte.
        Etudiant proprietaireDuNumero = etudiantRepository.findByTelephone(telephone).orElse(null);
        if (proprietaireDuNumero != null && !proprietaireDuNumero.getId().equals(etudiant.getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Ce numéro de téléphone est déjà utilisé par un autre étudiant."));
        }

        etudiant.setNom(nom);
        etudiant.setPrenom(prenom);
        etudiant.setTelephone(telephone);
        etudiantRepository.save(etudiant);

        return ResponseEntity.ok(toProfilePayload(user, etudiant));
    }

    /** Format unique du profil, partagé par la lecture et la mise à jour. */
    private Map<String, Object> toProfilePayload(User user, Etudiant etudiant) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("userId", user.getId());
        profile.put("username", user.getUsername());
        profile.put("email", user.getEmail());
        if (etudiant != null) {
            profile.put("etudiant", Map.of(
                    "id", etudiant.getId(),
                    "nom", etudiant.getNom(),
                    "prenom", etudiant.getPrenom(),
                    "telephone", etudiant.getTelephone(),
                    "matricule", etudiant.getMatricule() != null ? etudiant.getMatricule() : ""));
        }
        return profile;
    }

    private String trimOrNull(String valeur) {
        if (valeur == null) {
            return null;
        }
        String trimmed = valeur.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @PreAuthorize("hasRole('ETUDIANT')")
    @Transactional(readOnly = true)
    @GetMapping("/formations")
    public ResponseEntity<List<FormationDto>> listFormations() {
        List<FormationDto> list = formationService.getAll().stream()
                .map(f -> new FormationDto(f.getId(), f.getCode(), f.getIntitule()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PreAuthorize("hasRole('ETUDIANT')")
    @Transactional(readOnly = true)
    @GetMapping("/cours")
    public ResponseEntity<List<CoursDto>> listCours() {
        return ResponseEntity.ok(toCoursDtoList(coursService.getAll()));
    }

    // Le formateur dispose de /formateur/cohortes, filtre par les cours qui lui
    // sont attribués. Cette route générale ne doit pas exposer le référentiel de
    // toutes les promotions à un formateur.
    @PreAuthorize("hasRole('ETUDIANT')")
    @Transactional(readOnly = true)
    @GetMapping("/cohortes")
    public ResponseEntity<List<PromotionDto>> listPromotions() {
        List<PromotionDto> list = promotionService.getAll().stream()
                .map(this::toPromotionDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @Transactional(readOnly = true)
    @GetMapping("/formateur/cohortes")
    public ResponseEntity<?> listPromotionsDuFormateur(Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        List<Long> mesPromotionIds = cohorteIdsDuFormateur(formateur);
        return ResponseEntity.ok(promotionService.getAll().stream()
                .filter(p -> mesPromotionIds.contains(p.getId()))
                .map(this::toPromotionDto)
                .toList());
    }

    private List<Long> cohorteIdsVisiblesPour(User user) {
        return perimetreFormateurService.cohorteIdsVisiblesPour(user);
    }

    /** Un étudiant consulte uniquement ses propres certificats émis. */
    @PreAuthorize("hasRole('ETUDIANT')")
    @Transactional(readOnly = true)
    @GetMapping("/certificats")
    public ResponseEntity<?> listCertificats(Authentication authentication) {
        User user = findAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Etudiant etudiant = etudiantRepository.findByUserId(user.getId()).orElse(null);
        if (etudiant == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(certificatService.getByEtudiant(etudiant.getId()).stream().map(this::toCertificatDto).toList());
    }

    /**
     * Répondait auparavant {@code forumSujetRepository.findAll()} : un étudiant
     * pouvait lister tous les sujets de tous les cours, y compris ceux hors de son
     * cursus, en appelant directement cet endpoint (le second des deux contrôleurs
     * qui servent {@code ForumSujet}, cf. {@code ForumVisibiliteService}). Utilise
     * désormais la même visibilité que {@code ForumController}.
     */
    @PreAuthorize("hasAnyRole('ETUDIANT','FORMATEUR')")
    @Transactional(readOnly = true)
    @GetMapping("/sujets")
    public ResponseEntity<List<SujetDto>> listSujets(Authentication authentication) {
        return ResponseEntity.ok(
                forumVisibilite.sujetsVisibles(authentication).stream().map(this::toSujetDto).toList());
    }

    @PreAuthorize("hasAnyRole('ETUDIANT','FORMATEUR')")
    @Transactional
    @PostMapping(value = "/sujets/{id}/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> repondreAuSujet(
            @PathVariable Long id,
            @RequestParam String contenu,
            @RequestParam(defaultValue = "false") boolean marquerCommeResolu,
            @RequestParam(value = "fichier", required = false) MultipartFile fichier,
            Authentication authentication) {
        User user = findAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (isBlank(contenu)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le message ne peut pas être vide."));
        }
        ForumSujet sujet = forumSujetRepository.findById(id).orElse(null);
        if (sujet == null || !forumVisibilite.estVisible(sujet, authentication)) {
            return ResponseEntity.notFound().build();
        }
        if (sujet.isClos() && !marquerCommeResolu) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Ce sujet est clos : il n'accepte plus de reponse."));
        }

        String pieceJointeNomOriginal = null;
        String pieceJointeNomStocke = null;
        String pieceJointeTypeContenu = null;
        if (fichier != null && !fichier.isEmpty()) {
            // Le dossier « messages » est sous /uploads, donc atteignable en statique :
            // une pièce jointe non filtrée y serait re-servie et exécutée.
            try {
                ValidationFichier.exiger(fichier,
                        ValidationFichier.EXTENSIONS_SUPPORT_COURS,
                        ValidationFichier.TYPES_SUPPORT_COURS,
                        "PDF, Word, PowerPoint, Excel, images, archives ZIP, audio et vidéo");
            } catch (IllegalArgumentException invalide) {
                return ResponseEntity.badRequest().body(Map.of("message", invalide.getMessage()));
            }

            pieceJointeNomOriginal = ValidationFichier.nomSur(fichier, "fichier");
            pieceJointeNomStocke = UUID.randomUUID() + "-" + pieceJointeNomOriginal;
            pieceJointeTypeContenu = fichier.getContentType();
            try {
                Path messagesDirectory = uploadDirectory.resolve("messages");
                Files.createDirectories(messagesDirectory);
                Files.copy(fichier.getInputStream(),
                        ValidationFichier.destinationSure(messagesDirectory, pieceJointeNomStocke),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException exception) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Le fichier n'a pas pu être enregistré."));
            }
        }

        ForumMessage message = new ForumMessage(sujet, user, contenu.trim());
        message.setPieceJointeNomOriginal(pieceJointeNomOriginal);
        message.setPieceJointeNomStocke(pieceJointeNomStocke);
        message.setPieceJointeTypeContenu(pieceJointeTypeContenu);
        forumMessageRepository.save(message);
        if (marquerCommeResolu) {
            sujet.setClos(true);
            forumSujetRepository.save(sujet);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(toSujetDto(sujet));
    }

    @PreAuthorize("hasAnyRole('ETUDIANT','FORMATEUR')")
    @Transactional(readOnly = true)
    @GetMapping("/sujets/messages/{messageId}/piece-jointe")
    public ResponseEntity<Resource> downloadMessageAttachment(@PathVariable Long messageId) {
        ForumMessage message = forumMessageRepository.findById(messageId).orElse(null);
        if (message == null || message.getPieceJointeNomStocke() == null) {
            return ResponseEntity.notFound().build();
        }
        Path filePath = uploadDirectory.resolve("messages").resolve(message.getPieceJointeNomStocke()).normalize();
        if (!filePath.startsWith(uploadDirectory) || !Files.isRegularFile(filePath)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(filePath);
        MediaType mediaType = message.getPieceJointeTypeContenu() != null
                ? MediaType.parseMediaType(message.getPieceJointeTypeContenu())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(message.getPieceJointeNomOriginal()).build().toString())
                .body(resource);
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @Transactional
    @PatchMapping("/formateur/sujets/{id}/urgence")
    public ResponseEntity<?> toggleUrgenceSujet(@PathVariable Long id, Authentication authentication) {
        if (findAuthenticatedFormateur(authentication) == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        ForumSujet sujet = forumSujetRepository.findById(id).orElse(null);
        if (sujet == null || !forumVisibilite.estVisible(sujet, authentication)) {
            return ResponseEntity.notFound().build();
        }
        // « Urgent » se materialise par l'epinglage : le sujet remonte en tete de liste,
        // ce qui est exactement l'effet recherche par le formateur.
        sujet.setEpingle(!sujet.isEpingle());
        return ResponseEntity.ok(toSujetDto(forumSujetRepository.save(sujet)));
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @Transactional
    @PatchMapping("/formateur/sujets/{id}/resoudre")
    public ResponseEntity<?> marquerSujetResolu(@PathVariable Long id, Authentication authentication) {
        if (findAuthenticatedFormateur(authentication) == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        ForumSujet sujet = forumSujetRepository.findById(id).orElse(null);
        if (sujet == null || !forumVisibilite.estVisible(sujet, authentication)) {
            return ResponseEntity.notFound().build();
        }
        // Un sujet resolu est clos : il reste consultable mais n'accepte plus de reponse.
        sujet.setClos(true);
        sujet.setEpingle(false);
        return ResponseEntity.ok(toSujetDto(forumSujetRepository.save(sujet)));
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @Transactional
    @DeleteMapping("/formateur/sujets/{id}")
    public ResponseEntity<?> supprimerSujet(@PathVariable Long id, Authentication authentication) {
        if (findAuthenticatedFormateur(authentication) == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        ForumSujet sujet = forumSujetRepository.findById(id).orElse(null);
        if (sujet == null || !forumVisibilite.estVisible(sujet, authentication)) {
            return ResponseEntity.notFound().build();
        }
        forumSujetRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Contenu d'un cours vu par l'étudiant : la fiche du cours et ses supports.
     * Les endpoints /formateur/cours/{id}/supports existants sont réservés au
     * propriétaire du cours et ne conviennent donc pas ici.
     */
    @PreAuthorize("hasRole('ETUDIANT')")
    @Transactional(readOnly = true)
    @GetMapping("/cours/{id}")
    public ResponseEntity<?> getCours(@PathVariable Long id) {
        return coursService.findById(id)
                .<ResponseEntity<?>>map(cours -> ResponseEntity.ok(toCoursDto(cours)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ETUDIANT')")
    @Transactional(readOnly = true)
    @GetMapping("/cours/{id}/supports")
    public ResponseEntity<List<SupportDetailDto>> listSupportsOfCours(@PathVariable Long id) {
        if (coursService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(supportService.getCourseSupports(id).stream()
                .map(this::toSupportDetailDto)
                .toList());
    }

    @PreAuthorize("hasRole('ETUDIANT')")
    @Transactional(readOnly = true)
    @GetMapping("/supports")
    public ResponseEntity<List<SupportDto>> listAllSupports(Authentication authentication) {
        User user = findAuthenticatedUser(authentication);
        Formateur formateur = findAuthenticatedFormateur(authentication);
        List<Long> mesCohorteIds = user != null ? cohorteIdsVisiblesPour(user) : List.of();
        List<SupportDto> supports = supportService.getAllCourseSupports().stream()
                .filter(s -> perimetreFormateurService.peutVoirSupport(s, formateur))
                .filter(s -> perimetreFormateurService.coursDuSupportEstAccessible(s, mesCohorteIds, formateur))
                .map(this::toSupportDto)
                .collect(Collectors.toList());
        supports.addAll(supportService.getAllPedagogicalSupports().stream()
                .map(s -> new SupportDto(s.getId().toString(), s.getTitre(), null, null, null, false, null, null))
                .toList());
        return ResponseEntity.ok(supports);
    }

    @PreAuthorize("hasRole('ETUDIANT')")
    @Transactional(readOnly = true)
    @GetMapping("/supports/cours")
    public ResponseEntity<List<SupportDto>> listCourseSupports(Authentication authentication) {
        User user = findAuthenticatedUser(authentication);
        Formateur formateur = findAuthenticatedFormateur(authentication);
        List<Long> mesCohorteIds = user != null ? cohorteIdsVisiblesPour(user) : List.of();
        return ResponseEntity.ok(supportService.getAllCourseSupports().stream()
                .filter(s -> perimetreFormateurService.peutVoirSupport(s, formateur))
                .filter(s -> perimetreFormateurService.coursDuSupportEstAccessible(s, mesCohorteIds, formateur))
                .map(this::toSupportDto)
                .toList());
    }

    @PreAuthorize("hasRole('ETUDIANT')")
    @Transactional(readOnly = true)
    @GetMapping("/supports/pedagogiques")
    public ResponseEntity<List<SupportDto>> listPedagogicalSupports() {
        return ResponseEntity.ok(supportService.getAllPedagogicalSupports().stream()
                .map(s -> new SupportDto(s.getId().toString(), s.getTitre(), null, null, null, false, null, null))
                .toList());
    }

    @PreAuthorize("hasRole('ETUDIANT')")
    @Transactional(readOnly = true)
    @GetMapping("/supports/pedagogiques/{id}")
    public ResponseEntity<SupportDto> getPedagogicalSupport(@PathVariable UUID id) {
        var support = supportService.findPedagogicalById(id);
        if (support == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new SupportDto(support.getId().toString(), support.getTitre(), null, null, null, false, null, null));
    }

    @PreAuthorize("hasAnyRole('ETUDIANT','FORMATEUR')")
    @Transactional(readOnly = true)
    @GetMapping("/supports/cours/{id}/download")
    public ResponseEntity<Resource> downloadCourseSupport(@PathVariable Long id, Authentication authentication) {
        SupportCours support = supportService.getAllCourseSupports().stream()
                .filter(item -> item.getId().equals(id))
                .findFirst()
                .orElse(null);
        if (support == null || support.getFichierStocke() == null) {
            return ResponseEntity.notFound().build();
        }
        // Sans ces controles, filtrer seulement la liste serait cosmetique : il
        // suffirait de deviner l'identifiant pour telecharger une ressource non
        // encore publiee, ou celle d'un cours suivi par d'autres etudiants/formateurs.
        User user = findAuthenticatedUser(authentication);
        Formateur formateur = findAuthenticatedFormateur(authentication);
        List<Long> mesCohorteIds = user != null ? cohorteIdsVisiblesPour(user) : List.of();
        if (!perimetreFormateurService.peutVoirSupport(support, formateur)
                || !perimetreFormateurService.coursDuSupportEstAccessible(support, mesCohorteIds, formateur)) {
            return ResponseEntity.notFound().build();
        }

        Path filePath = uploadDirectory.resolve(support.getFichierStocke()).normalize();
        if (!filePath.startsWith(uploadDirectory) || !Files.isRegularFile(filePath)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(filePath);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(support.getTitre()).build().toString())
                .body(resource);
    }

    @PreAuthorize("hasRole('ETUDIANT')")
    @Transactional(readOnly = true)
    @GetMapping("/certificats/{id}/pdf")
    public ResponseEntity<?> telechargerCertificat(@PathVariable Long id, Authentication authentication) {
        User user = findAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Certificat certificat = certificatService.findById(id).orElse(null);
        if (certificat == null) {
            return ResponseEntity.notFound().build();
        }

        boolean estProprietaire = certificat.getEtudiant().getUser() != null
                && certificat.getEtudiant().getUser().getId().equals(user.getId());
        if (!estProprietaire) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Vous n'avez pas accès à ce certificat."));
        }

        byte[] pdf = certificatPdfService.genererPdf(certificat);
        String nomFichier = "INPG-certificat-" + certificat.getReference() + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(nomFichier).build().toString())
                .body(pdf);
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @Transactional(readOnly = true)
    @GetMapping("/formateur/profile")
    public ResponseEntity<?> getFormateurProfile(Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        return ResponseEntity.ok(toFormateurProfileDto(formateur));
    }

    /**
     * Mise à jour de l'identité du formateur.
     *
     * <p>Même contrat que {@code PUT /profile} côté étudiant : champs obligatoires
     * contrôlés ici, conflit de téléphone traduit en 409 plutôt qu'en violation de
     * contrainte, et le profil complet est renvoyé pour que l'écran se rafraîchisse
     * sans second appel.
     *
     * <p>L'e-mail n'est volontairement pas modifiable : il sert d'identifiant de
     * connexion, le changer relève d'un parcours de vérification à part.
     */
    @PreAuthorize("hasRole('FORMATEUR')")
    @PutMapping("/formateur/profile")
    @Transactional
    public ResponseEntity<?> updateFormateurProfile(@RequestBody Map<String, String> request,
            Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }

        String nom = trimOrNull(request.get("nom"));
        String prenom = trimOrNull(request.get("prenom"));
        String telephone = trimOrNull(request.get("telephone"));

        if (nom == null || prenom == null || telephone == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Le nom, le prénom et le téléphone sont obligatoires."));
        }

        // Le téléphone est unique sur la table des formateurs : on détecte le conflit
        // pour renvoyer un message exploitable plutôt qu'une erreur de contrainte.
        Formateur proprietaireDuNumero = formateurRepository.findByTelephone(telephone).orElse(null);
        if (proprietaireDuNumero != null && !proprietaireDuNumero.getId().equals(formateur.getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Ce numéro de téléphone est déjà utilisé par un autre formateur."));
        }

        formateur.setNom(nom);
        formateur.setPrenom(prenom);
        formateur.setTelephone(telephone);
        formateurRepository.save(formateur);

        return ResponseEntity.ok(toFormateurProfileDto(formateur));
    }

    /** Format unique du profil formateur, partagé par la lecture et la mise à jour. */
    private FormateurProfileDto toFormateurProfileDto(Formateur formateur) {
        long nbCours = coursService.getByFormateurId(formateur.getId()).size();
        String nomComplet = String.join(" ", formateur.getPrenom(), formateur.getNom()).trim();
        return new FormateurProfileDto(
                formateur.getId(),
                nomComplet,
                formateur.getPrenom(),
                formateur.getNom(),
                formateur.getTelephone(),
                formateur.getUser().getEmail(),
                nbCours);
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @Transactional(readOnly = true)
    @GetMapping("/formateur/dashboard")
    public ResponseEntity<?> getFormateurDashboard(Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        List<Cours> cours = coursService.getByFormateurId(formateur.getId());
        long nbRessources = cours.stream()
                .mapToLong(item -> supportService.countCourseSupports(item.getId()))
                .sum();
        return ResponseEntity.ok(Map.of(
                "nbCours", cours.size(),
                "nbRessources", nbRessources));
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @Transactional(readOnly = true)
    @GetMapping("/formateur/seances")
    public ResponseEntity<?> listMySessions(Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Profil formateur introuvable."));
        return ResponseEntity.ok(seanceService.getByFormateur(formateur.getId()).stream().map(this::toSeanceDto).toList());
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @Transactional
    @PostMapping("/formateur/seances")
    public ResponseEntity<?> createSession(@RequestBody SeanceRequest request, Authentication authentication) {
        Cours cours = request.coursId() == null ? null : findOwnedCourse(request.coursId(), authentication);
        if (cours == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Cours introuvable ou non autorisé."));
        if (isBlank(request.titre()) || isBlank(request.type()) || request.dateHeure() == null || request.dureeMinutes() == null || request.dureeMinutes() <= 0)
            return ResponseEntity.badRequest().body(Map.of("message", "Titre, type, date et durée positive sont requis."));
        Seance seance = new Seance();
        applySession(seance, request, cours);
        return ResponseEntity.status(HttpStatus.CREATED).body(toSeanceDto(seanceService.save(seance)));
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @Transactional
    @PutMapping("/formateur/seances/{id}")
    public ResponseEntity<?> updateSession(@PathVariable Long id, @RequestBody SeanceRequest request, Authentication authentication) {
        Seance seance = findOwnedSession(id, authentication);
        Cours cours = request.coursId() == null ? null : findOwnedCourse(request.coursId(), authentication);
        if (seance == null || cours == null) return ResponseEntity.notFound().build();
        if (isBlank(request.titre()) || isBlank(request.type()) || request.dateHeure() == null || request.dureeMinutes() == null || request.dureeMinutes() <= 0)
            return ResponseEntity.badRequest().body(Map.of("message", "Titre, type, date et durée positive sont requis."));
        applySession(seance, request, cours);
        return ResponseEntity.ok(toSeanceDto(seanceService.save(seance)));
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @Transactional
    @DeleteMapping("/formateur/seances/{id}")
    public ResponseEntity<?> deleteSession(@PathVariable Long id, Authentication authentication) {
        Seance seance = findOwnedSession(id, authentication);
        if (seance == null) return ResponseEntity.notFound().build();
        seanceService.delete(seance);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @Transactional(readOnly = true)
    @GetMapping("/formateur/cours")
    public ResponseEntity<?> listMyCourses(Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        return ResponseEntity.ok(toCoursDtoList(coursService.getByFormateurId(formateur.getId())));
    }

    // Création, édition, suppression et affectation des promotions relèvent du
    // responsable pédagogique. Ces anciennes routes formateur sont conservées
    // temporairement pour répondre explicitement par 403 aux clients obsolètes.
    @PreAuthorize("denyAll()")
    @Transactional
    @PostMapping("/formateur/cours")
    public ResponseEntity<?> createCourse(
            @RequestBody CoursCreateRequest request,
            Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        if (isBlank(request.code()) || isBlank(request.titre())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le code et le titre du cours sont requis."));
        }
        if (request.volumeHoraire() != null && request.volumeHoraire() < 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le volume horaire ne peut pas être négatif."));
        }

        Cours cours = new Cours();
        cours.setNom(request.code().trim());
        cours.setTitre(request.titre().trim());
        cours.setDescription(request.description() == null ? "" : request.description().trim());
        cours.setSemestre(request.semestre() == null ? "" : request.semestre().trim());
        cours.setVolumeHoraire(request.volumeHoraire() == null ? 0L : request.volumeHoraire());
        cours.getFormateurs().add(formateur);

        // Le rattachement au programme exige un cours deja enregistre : c'est la
        // formation qui porte la table de jointure, et elle a besoin de l'identifiant.
        Cours enregistre = coursService.save(cours);
        if (request.cohorteIds() != null && !request.cohorteIds().isEmpty()) {
            List<Promotion> promotions = request.cohorteIds().stream()
                    .map(promotionService::findById)
                    .flatMap(Optional::stream)
                    .toList();
            formationService.rattacherAuxFormationsDe(enregistre, promotions);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(toCoursDto(coursService.findById(enregistre.getId()).orElse(enregistre)));
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @Transactional(readOnly = true)
    @GetMapping("/formateur/cours/{id}")
    public ResponseEntity<?> getMyCourse(@PathVariable Long id, Authentication authentication) {
        Cours cours = findOwnedCourse(id, authentication);
        return cours == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(toCoursDto(cours));
    }

    @PreAuthorize("denyAll()")
    @Transactional
    @PutMapping("/formateur/cours/{id}")
    public ResponseEntity<?> updateCourse(
            @PathVariable Long id,
            @RequestBody CoursCreateRequest request,
            Authentication authentication) {
        Cours cours = findOwnedCourse(id, authentication);
        if (cours == null) {
            return ResponseEntity.notFound().build();
        }
        if (isBlank(request.code()) || isBlank(request.titre())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le code et le titre du cours sont requis."));
        }
        if (request.volumeHoraire() != null && request.volumeHoraire() < 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le volume horaire ne peut pas être négatif."));
        }
        cours.setNom(request.code().trim());
        cours.setTitre(request.titre().trim());
        cours.setDescription(request.description() == null ? "" : request.description().trim());
        cours.setSemestre(request.semestre() == null ? "" : request.semestre().trim());
        cours.setVolumeHoraire(request.volumeHoraire() == null ? 0L : request.volumeHoraire());

        return ResponseEntity.ok(toCoursDto(coursService.save(cours)));
    }

    @PreAuthorize("denyAll()")
    @DeleteMapping("/formateur/cours/{id}")
    @Transactional
    public ResponseEntity<?> deleteCourse(@PathVariable Long id, Authentication authentication) {
        Cours cours = findOwnedCourse(id, authentication);
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

    @PreAuthorize("denyAll()")
    @Transactional
    @PatchMapping("/formateur/cours/{id}/cohortes")
    public ResponseEntity<?> updateCoursCohortes(
            @PathVariable Long id,
            @RequestBody Map<String, List<Long>> body,
            Authentication authentication) {
        Cours cours = findOwnedCourse(id, authentication);
        if (cours == null) {
            return ResponseEntity.notFound().build();
        }
        List<Long> cohorteIds = body.getOrDefault("cohorteIds", List.of());
        List<Promotion> cohortes = new ArrayList<>();
        for (Long cohorteId : cohorteIds) {
            Promotion cohorte = promotionService.findById(cohorteId).orElse(null);
            if (cohorte == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Cohorte introuvable : " + cohorteId));
            }
            cohortes.add(cohorte);
        }
        formationService.rattacherAuxFormationsDe(cours, cohortes);
        return ResponseEntity.ok(toCoursDto(coursService.findById(cours.getId()).orElse(cours)));
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @Transactional(readOnly = true)
    @GetMapping("/formateur/cours/{id}/supports")
    public ResponseEntity<?> listMyCourseSupports(@PathVariable Long id, Authentication authentication) {
        Cours cours = findOwnedCourse(id, authentication);
        if (cours == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(supportService.getCourseSupports(cours.getId()).stream()
                .map(this::toSupportDto)
                .toList());
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @Transactional
    @PostMapping(value = "/formateur/supports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadCourseSupport(
            @RequestParam Long coursId,
            @RequestParam String typeRessource,
            @RequestParam(required = false) String datePublication,
            @RequestParam("fichier") MultipartFile fichier,
            Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        if (fichier.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Un fichier est requis."));
        }

        LocalDate publicationDate;
        try {
            publicationDate = isBlank(datePublication) ? LocalDate.now() : LocalDate.parse(datePublication);
        } catch (java.time.format.DateTimeParseException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", "La date de publication est invalide."));
        }

        Cours cours = coursService.getByFormateurId(formateur.getId()).stream()
                .filter(item -> item.getId().equals(coursId))
                .findFirst()
                .orElse(null);
        if (cours == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Vous ne pouvez pas publier dans ce cours."));
        }

        try {
            SupportCours savedSupport = supportCoursStorageService.publier(cours, fichier, typeRessource,
                    publicationDate);
            return ResponseEntity.status(HttpStatus.CREATED).body(toSupportDto(savedSupport));
        } catch (IOException exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Le fichier n'a pas pu être enregistré."));
        }
    }

    /** Publie un lien HTTP(S) dans un cours du formateur connecté. */
    @PreAuthorize("hasRole('FORMATEUR')")
    @Transactional
    @PostMapping("/formateur/supports/lien")
    public ResponseEntity<?> publierLienExterne(@RequestBody SupportLienCreateRequest request,
            Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        if (request.coursId() == null || isBlank(request.titre()) || isBlank(request.url())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Le cours, le titre et le lien sont requis."));
        }
        if (!estLienHttpValide(request.url())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Le lien doit commencer par http:// ou https:// et désigner une adresse valide."));
        }

        LocalDate publicationDate;
        try {
            publicationDate = isBlank(request.datePublication()) ? LocalDate.now() : LocalDate.parse(request.datePublication());
        } catch (java.time.format.DateTimeParseException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", "La date de publication est invalide."));
        }

        Cours cours = coursService.getByFormateurId(formateur.getId()).stream()
                .filter(item -> item.getId().equals(request.coursId()))
                .findFirst()
                .orElse(null);
        if (cours == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Vous ne pouvez pas publier dans ce cours."));
        }

        SupportCours support = supportCoursStorageService.publierLien(cours, request.titre().trim(),
                request.url().trim(), publicationDate);
        return ResponseEntity.status(HttpStatus.CREATED).body(toSupportDto(support));
    }

    @PreAuthorize("denyAll()")
    @Transactional
    @PostMapping(value = "/formateur/cours/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadCourseImage(
        @PathVariable Long id,
        @RequestParam("image") MultipartFile image,
        Authentication authentication) {
    Cours cours = findOwnedCourse(id, authentication);
    if (cours == null) {
        return ResponseEntity.notFound().build();
    }
    if (image.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of("message", "Une image est requise."));
    }

    // L'image est ensuite servie en statique depuis /uploads : contrôler l'extension
    // et pas seulement le type déclaré, sinon un SVG porteur de script — qui passe
    // un test « commence par image/ » — serait exécuté depuis l'origine du site.
    try {
        ValidationFichier.exiger(image,
                ValidationFichier.EXTENSIONS_IMAGE,
                ValidationFichier.TYPES_IMAGE,
                "JPEG, PNG, WEBP, GIF");
    } catch (IllegalArgumentException invalide) {
        return ResponseEntity.badRequest().body(Map.of("message", invalide.getMessage()));
    }

    String originalName = ValidationFichier.nomSur(image, "image");
    String storedFileName = UUID.randomUUID() + "-" + originalName;
    // L'image d'un cours est affichée par une balise <img>, qui ne peut pas porter
    // d'en-tête d'authentification : c'est le seul contenu qui doit vivre dans le
    // sous-dossier public exposé en statique.
    Path repertoirePublic = uploadDirectory.resolve(WebConfig.DOSSIER_PUBLIC);
    try {
        Files.createDirectories(repertoirePublic);
        Files.copy(image.getInputStream(), ValidationFichier.destinationSure(repertoirePublic, storedFileName),
                StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "L'image n'a pas pu être enregistrée."));
    }

    cours.setImageUrl("/uploads/" + storedFileName);
    Cours sauvegarde = coursService.save(cours);
    return ResponseEntity.ok(toCoursDto(sauvegarde));
    }

    /**
     * Ressources des cours du formateur connecte uniquement — contrairement a
     * /supports/cours qui expose celles de tous les cours de la plateforme.
     */
    @PreAuthorize("hasRole('FORMATEUR')")
    @Transactional(readOnly = true)
    @GetMapping("/formateur/supports")
    public ResponseEntity<?> listMesSupports(Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        List<Long> mesCoursIds = coursService.getByFormateurId(formateur.getId()).stream()
                .map(Cours::getId)
                .toList();
        return ResponseEntity.ok(supportService.getAllCourseSupports().stream()
                .filter(s -> s.getCours() != null && mesCoursIds.contains(s.getCours().getId()))
                .sorted(Comparator.comparing(SupportCours::getId).reversed())
                .map(this::toSupportDto)
                .toList());
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @Transactional
    @DeleteMapping("/formateur/supports/{id}")
    public ResponseEntity<?> supprimerSupport(@PathVariable Long id, Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        SupportCours support = supportService.findCourseSupportById(id).orElse(null);
        if (support == null) {
            return ResponseEntity.notFound().build();
        }
        if (support.getCours() == null || !perimetreFormateurService.estFormateurDuCours(formateur, support.getCours())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Vous n'êtes pas formateur de ce cours."));
        }

        // Le fichier est supprime avant la ligne : si l'effacement disque echoue on
        // le journalise sans bloquer, pour ne pas laisser une ligne pointant vers
        // un fichier devenu inaccessible.
        if (support.getFichierStocke() != null) {
            Path filePath = uploadDirectory.resolve(support.getFichierStocke()).normalize();
            if (filePath.startsWith(uploadDirectory)) {
                try {
                    Files.deleteIfExists(filePath);
                } catch (IOException exception) {
                    log.error("Suppression du fichier {} impossible : {}", filePath, exception.getMessage());
                }
            }
        }
        supportService.deleteCourseSupport(support);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @Transactional(readOnly = true)
    @GetMapping("/formateur/etudiants")
    public ResponseEntity<?> listStudents(Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        List<Long> cohorteIds = cohorteIdsDuFormateur(formateur);
        if (cohorteIds.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(etudiantRepository.findByPromotion_IdIn(cohorteIds).stream()
                .map(this::toEtudiantSuiviDto).toList());
    }

    /**
     * Fiche de suivi d'un etudiant.
     *
     * <p>{@code @Transactional} est indispensable : avec {@code open-in-view=false} la
     * session Hibernate se ferme a la sortie du service, or {@link #toEtudiantSuiviDto}
     * lit la promotion, chargee paresseusement. Sans cette annotation l'ecran
     * « Suivi des etudiants » renvoyait systematiquement une erreur 500.
     */
    @PreAuthorize("hasRole('FORMATEUR')")
    @GetMapping("/formateur/etudiants/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getStudent(@PathVariable Long id, Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        List<Long> cohorteIds = cohorteIdsDuFormateur(formateur);
        return etudiantRepository.findById(id)
                .filter(etudiant -> etudiant.getPromotion() != null && cohorteIds.contains(etudiant.getPromotion().getId()))
                .<ResponseEntity<?>>map(etudiant -> ResponseEntity.ok(toEtudiantSuiviDto(etudiant)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @Transactional(readOnly = true)
    @GetMapping("/formateur/etudiants/{id}/notes")
    public ResponseEntity<?> listNotesEtudiant(@PathVariable Long id, Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        List<Long> cohorteIds = cohorteIdsDuFormateur(formateur);
        Etudiant etudiant = etudiantRepository.findById(id)
                .filter(e -> e.getPromotion() != null && cohorteIds.contains(e.getPromotion().getId()))
                .orElse(null);
        if (etudiant == null) {
            return ResponseEntity.notFound().build();
        }
        // L'étudiant peut être dans une cohorte du formateur sans que celui-ci
        // enseigne tous ses modules : ne renvoyer que les notes des cours que ce
        // formateur enseigne réellement, jamais celles d'un collègue.
        return ResponseEntity.ok(noteModuleService.getByEtudiantId(id).stream()
                .filter(note -> perimetreFormateurService.estFormateurDuCours(formateur, note.getCours()))
                .map(this::toNoteModuleDto)
                .toList());
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @Transactional
    @PostMapping("/formateur/etudiants/{id}/notes")
    public ResponseEntity<?> ajouterNoteEtudiant(
            @PathVariable Long id,
            @RequestBody NoteModuleCreateRequest request,
            Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        List<Long> cohorteIds = cohorteIdsDuFormateur(formateur);
        Etudiant etudiant = etudiantRepository.findById(id)
                .filter(e -> e.getPromotion() != null && cohorteIds.contains(e.getPromotion().getId()))
                .orElse(null);
        if (etudiant == null) {
            return ResponseEntity.notFound().build();
        }
        if (isBlank(request.module())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le nom du module est requis."));
        }
        if (request.note() < 0 || request.note() > 100) {
            return ResponseEntity.badRequest().body(Map.of("message", "La note doit être comprise entre 0 et 100."));
        }
        Cours cours = coursService.getByFormateurId(formateur.getId()).stream()
                .filter(c -> c.getId().equals(request.coursId()))
                .findFirst()
                .orElse(null);
        if (cours == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Vous n'enseignez pas ce cours."));
        }
        if (!perimetreFormateurService.etudiantSuitLeCours(etudiant, cours)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cet étudiant ne suit pas ce cours."));
        }

        NoteModule note = new NoteModule();
        note.setEtudiant(etudiant);
        note.setCours(cours);
        note.setModule(request.module().trim());
        note.setNote(request.note());
        note.setCommentaire(isBlank(request.commentaire()) ? null : request.commentaire().trim());
        note.setDateEvaluation(LocalDate.now());
        note.setFormateurNom(displayName(formateur.getUser()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toNoteModuleDto(noteModuleService.save(note)));
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @Transactional
    @DeleteMapping("/formateur/notes/{noteId}")
    public ResponseEntity<?> supprimerNote(@PathVariable Long noteId, Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        NoteModule note = noteModuleService.findById(noteId).orElse(null);
        if (note == null) {
            return ResponseEntity.notFound().build();
        }
        if (!perimetreFormateurService.estFormateurDuCours(formateur, note.getCours())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Vous n'êtes pas formateur de ce cours."));
        }
        noteModuleService.deleteById(noteId);
        return ResponseEntity.noContent().build();
    }

    private NoteModuleDto toNoteModuleDto(NoteModule note) {
        return new NoteModuleDto(
                note.getId(),
                note.getModule(),
                note.getNote(),
                note.getCommentaire(),
                note.getDateEvaluation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                note.getCours().getId(),
                note.getCours().getNom(),
                note.getCours().getTitre(),
                note.getFormateurNom());
    }

    /** Séances d'UN cours precis (utilise par la prise de presence formateur, cf. PresenceEtudiantDto). */
    @PreAuthorize("hasRole('FORMATEUR')")
    @Transactional(readOnly = true)
    @GetMapping("/formateur/cours/{coursId}/seances")
    public ResponseEntity<?> listSeances(@PathVariable Long coursId, Authentication authentication) {
        Cours cours = findOwnedCourse(coursId, authentication);
        if (cours == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Vous n'enseignez pas ce cours."));
        }
        return ResponseEntity.ok(seanceService.getByCoursId(coursId).stream().map(this::toSeanceDto).toList());
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @Transactional(readOnly = true)
    @GetMapping("/formateur/seances/{id}/presences")
    public ResponseEntity<?> listPresencesSeance(@PathVariable Long id, Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        Seance seance = seanceService.findById(id).orElse(null);
        if (seance == null) {
            return ResponseEntity.notFound().build();
        }
        if (!perimetreFormateurService.estFormateurDuCours(formateur, seance.getCours())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Vous n'êtes pas formateur de ce cours."));
        }
        List<Long> cohorteIds = perimetreFormateurService.promotionIdsDuCours(seance.getCours());
        List<Etudiant> etudiants = etudiantRepository.findByPromotion_IdIn(cohorteIds);
        Map<Long, Boolean> presencesParEtudiant = presenceService.getPresencesBySeanceId(id).stream()
                .collect(Collectors.toMap(p -> p.getEtudiant().getId(), Presence::isPresent));
        List<PresenceEtudiantDto> liste = etudiants.stream()
                .map(e -> new PresenceEtudiantDto(
                        e.getId(),
                        String.join(" ", e.getPrenom(), e.getNom()).trim(),
                        initials(e.getPrenom(), e.getNom()),
                        "bg-primary-container",
                        presencesParEtudiant.get(e.getId())))
                .toList();
        return ResponseEntity.ok(liste);
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @Transactional
    @PostMapping("/formateur/seances/{id}/presences")
    public ResponseEntity<?> enregistrerPresences(
            @PathVariable Long id,
            @RequestBody PresenceBulkUpdateRequest request,
            Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        Seance seance = seanceService.findById(id).orElse(null);
        if (seance == null) {
            return ResponseEntity.notFound().build();
        }
        if (!perimetreFormateurService.estFormateurDuCours(formateur, seance.getCours())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Vous n'êtes pas formateur de ce cours."));
        }
        List<Long> cohorteIds = perimetreFormateurService.promotionIdsDuCours(seance.getCours());
        for (PresenceBulkUpdateRequest.Entree entree : request.presences()) {
            Etudiant etudiant = etudiantRepository.findById(entree.etudiantId()).orElse(null);
            if (etudiant == null || etudiant.getPromotion() == null || !cohorteIds.contains(etudiant.getPromotion().getId())) {
                continue; // etudiant hors perimetre de ce cours : ignore plutot que planter tout le lot
            }
            presenceService.marquerPresence(seance, etudiant, entree.present());
        }
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @Transactional(readOnly = true)
    @GetMapping("/formateur/etudiants/{id}/presences")
    public ResponseEntity<?> listPresencesEtudiant(@PathVariable Long id, Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        List<Long> cohorteIds = cohorteIdsDuFormateur(formateur);
        Etudiant etudiant = etudiantRepository.findById(id)
                .filter(e -> e.getPromotion() != null && cohorteIds.contains(e.getPromotion().getId()))
                .orElse(null);
        if (etudiant == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(presenceService.getPresencesByEtudiantId(id).stream()
                .map(this::toPresenceHistoriqueDto).toList());
    }

    private PresenceHistoriqueDto toPresenceHistoriqueDto(Presence presence) {
        Seance seance = presence.getSeance();
        return new PresenceHistoriqueDto(
                presence.getId(),
                seance.getDateHeure().format(DATE_HEURE_FORUM),
                seance.getCours().getNom(),
                seance.getCours().getTitre(),
                seance.getTitre(),
                presence.isPresent());
    }

    private List<Long> cohorteIdsDuFormateur(Formateur formateur) {
        return perimetreFormateurService.cohorteIdsDuFormateur(formateur);
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @Transactional(readOnly = true)
    @GetMapping("/formateur/retours")
    public ResponseEntity<?> listRetours(Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        List<Long> coursIds = coursService.getByFormateurId(formateur.getId()).stream()
                .map(Cours::getId)
                .toList();
        return ResponseEntity.ok(retourEvaluationService.getByCoursIds(coursIds).stream()
                .map(this::toRetourEvaluationDto).toList());
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @Transactional
    @PatchMapping("/formateur/retours/{id}/epingle")
    public ResponseEntity<?> togglePinRetour(@PathVariable Long id, Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        RetourEvaluation retour = retourEvaluationService.findById(id).orElse(null);
        if (retour == null) {
            return ResponseEntity.notFound().build();
        }
        if (!perimetreFormateurService.estFormateurDuCours(formateur, retour.getCours())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Vous n'êtes pas formateur de ce cours."));
        }
        retour.setEpingle(!retour.isEpingle());
        return ResponseEntity.ok(toRetourEvaluationDto(retourEvaluationService.save(retour)));
    }

    private RetourEvaluationDto toRetourEvaluationDto(RetourEvaluation retour) {
        return new RetourEvaluationDto(
                retour.getId(),
                retour.getNote(),
                retour.getCommentaire(),
                retour.getDateCreation().toString(),
                retour.getCours().getId(),
                retour.getCours().getTitre(),
                retour.getStatut().name(),
                retour.isEpingle());
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @Transactional
    @PostMapping("/formateur/etudiants/{id}/contacter")
    public ResponseEntity<?> contacterEtudiant(
            @PathVariable Long id,
            @RequestBody ContactEtudiantRequest request,
            Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        if (isBlank(request.objet()) || isBlank(request.message())) {
            return ResponseEntity.badRequest().body(Map.of("message", "L'objet et le message sont requis."));
        }
        List<Long> cohorteIds = cohorteIdsDuFormateur(formateur);
        Etudiant etudiant = etudiantRepository.findById(id)
                .filter(e -> e.getPromotion() != null && cohorteIds.contains(e.getPromotion().getId()))
                .orElse(null);
        if (etudiant == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            notificationService.envoyerEmail(etudiant.getUser().getEmail(), request.objet(), request.message());
        } catch (Exception exception) {
            log.error("Échec de l'envoi d'email à {} : {}", etudiant.getUser().getEmail(), exception.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", "L'envoi de l'email a échoué. Vérifiez la configuration SMTP."));
        }
        return ResponseEntity.ok(Map.of("message", "Email envoyé à " + etudiant.getUser().getEmail() + "."));
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @Transactional
    @PostMapping("/formateur/etudiants/relance")
    public ResponseEntity<?> relancerEtudiants(
            @RequestBody RelanceCollectiveRequest request,
            Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        if (isBlank(request.objet()) || isBlank(request.message())) {
            return ResponseEntity.badRequest().body(Map.of("message", "L'objet et le message sont requis."));
        }
        if (request.etudiantIds() == null || request.etudiantIds().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Aucun étudiant sélectionné."));
        }
        List<Long> cohorteIds = cohorteIdsDuFormateur(formateur);
        List<Etudiant> destinataires = etudiantRepository.findByPromotion_IdIn(cohorteIds).stream()
                .filter(e -> request.etudiantIds().contains(e.getId()))
                .toList();
        int envoyes = 0;
        List<String> echecs = new ArrayList<>();
        for (Etudiant etudiant : destinataires) {
            try {
                notificationService.envoyerEmail(etudiant.getUser().getEmail(), request.objet(), request.message());
                envoyes++;
            } catch (Exception exception) {
                log.error("Échec de l'envoi d'email (relance) à {} : {}", etudiant.getUser().getEmail(), exception.getMessage());
                echecs.add(etudiant.getUser().getEmail());
            }
        }
        return ResponseEntity.ok(Map.of("envoyes", envoyes, "echecs", echecs));
    }

    @PreAuthorize("denyAll()")
    @Transactional
    @PostMapping("/formateur/etudiants/{id}/cohorte")
    public ResponseEntity<?> assignerCohorte(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body,
            Authentication authentication) {
        if (findAuthenticatedFormateur(authentication) == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        Etudiant etudiant = etudiantRepository.findById(id).orElse(null);
        if (etudiant == null) {
            return ResponseEntity.notFound().build();
        }
        Long cohorteId = body.get("cohorteId");
        if (cohorteId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "cohorteId est requis."));
        }
        Promotion cohorte = promotionService.findById(cohorteId).orElse(null);
        if (cohorte == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cohorte introuvable."));
        }

        etudiant.setPromotion(cohorte);
        etudiantRepository.save(etudiant);
        return ResponseEntity.ok(toEtudiantSuiviDto(etudiant));
    }

    /**
     * Convertit une liste de cours en DTO avec seulement DEUX requetes de comptage
     * au total (etudiants par cohorte, supports par cours), au lieu d'une requete
     * par cohorte et par cours — ce qui degenerait en N+1 sur les listes.
     */
    private List<CoursDto> toCoursDtoList(List<Cours> cours) {
        List<Long> coursIds = cours.stream().map(Cours::getId).toList();
        List<Long> cohorteIds = cours.stream()
                .flatMap(c -> perimetreFormateurService.promotionIdsDuCours(c).stream())
                .distinct()
                .toList();

        Map<Long, Long> etudiantsParCohorte = cohorteIds.isEmpty()
                ? Map.of()
                : etudiantRepository.compterParPromotion(cohorteIds).stream()
                        .collect(Collectors.toMap(CompteurParId::id, CompteurParId::total));
        Map<Long, Long> supportsParCours = supportService.compterSupportsParCours(coursIds).stream()
                .collect(Collectors.toMap(CompteurParId::id, CompteurParId::total));

        return cours.stream().map(c -> toCoursDto(c, etudiantsParCohorte, supportsParCours)).toList();
    }

    /** Cours seul (creation/edition) : passe par la version groupee, donc 2 requetes aussi. */
    private CoursDto toCoursDto(Cours cours) {
        return toCoursDtoList(List.of(cours)).get(0);
    }

    private CoursDto toCoursDto(Cours cours, Map<Long, Long> etudiantsParCohorte, Map<Long, Long> supportsParCours) {
        List<Promotion> promotions = perimetreFormateurService.promotionsDuCours(cours);
        long nbEtudiants = promotions.stream()
                .mapToLong(cohorte -> etudiantsParCohorte.getOrDefault(cohorte.getId(), 0L))
                .sum();
        List<PromotionDto> cohortes = promotions.stream()
                .map(this::toPromotionDto)
                .toList();
        return new CoursDto(
                cours.getId(),
                cours.getNom(),
                cours.getTitre(),
                cours.getDescription(),
                cours.getSemestre(),
                cours.getVolumeHoraire(),
                cours.getCoefficient(),
                nbEtudiants,
                supportsParCours.getOrDefault(cours.getId(), 0L),
                0,
                DEFAULT_COURSE_COLOR,
                cohortes,
                cours.getFormations().stream()
                        .map(f -> new FormationDto(f.getId(), f.getCode(), f.getIntitule()))
                        .sorted(Comparator.comparing(FormationDto::intitule,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .toList(),
                cours.getFormateurs().stream()
                        .map(f -> new FormateurBrefDto(f.getId(), f.getPrenom() + " " + f.getNom()))
                        .toList(),
                cours.getImageUrl());
    }

    private CertificatDto toCertificatDto(Certificat certificat) {
        Etudiant etudiant = certificat.getEtudiant();
        return new CertificatDto(
                certificat.getId(),
                certificat.getReference(),
                etudiant.getId(),
                String.join(" ", etudiant.getPrenom(), etudiant.getNom()).trim(),
                etudiant.getMatricule(),
                certificat.getIntituleFormation(),
                certificat.getPromotion() != null ? certificat.getPromotion().getLibelle() : null,
                certificat.getDateEmission().toString(),
                certificat.getFormateurNom(),
                certificat.getCivilite(),
                certificat.getDateNaissance() == null ? null : certificat.getDateNaissance().toString(),
                certificat.getDateDebut() == null ? null : certificat.getDateDebut().toString(),
                certificat.getDateFin() == null ? null : certificat.getDateFin().toString(),
                certificat.getCodeCertificat());
    }

    private static final DateTimeFormatter DATE_HEURE_FORUM = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String[] AVATAR_BACKGROUNDS = {
            "bg-secondary-container", "bg-error-container/20", "bg-surface-container-highest", "bg-primary-container"
    };
    private static final String[] AVATAR_TEXTS = {
            "text-on-secondary-container", "text-error", "text-on-surface-variant", "text-on-primary-container"
    };

    private PromotionDto toPromotionDto(Promotion cohorte) {
        Integer annee = cohorte.getDateDebut() != null ? cohorte.getDateDebut().getYear() : null;
        return new PromotionDto(cohorte.getId(), cohorte.getCode(), cohorte.getLibelle(), annee);
    }

    /**
     * Forme de reponse inchangee apres la fusion des deux forums (migration V5) :
     * l'ecran formateur consomme le meme SujetDto, desormais alimente par
     * forum_sujets. Les champs autrefois stockes en clair sont derives des vraies
     * associations — le cours par sa cle etrangere, l'auteur par son compte.
     */
    private SujetDto toSujetDto(ForumSujet sujet) {
        List<MessageDto> messages = forumMessageRepository.findBySujetIdOrderByDateCreationAsc(sujet.getId())
                .stream().map(this::toMessageDto).toList();
        String apercu = messages.isEmpty() ? "" : messages.get(0).contenu();
        if (apercu.length() > 140) {
            apercu = apercu.substring(0, 140) + "…";
        }
        int paletteIndex = (int) Math.floorMod(sujet.getId(), AVATAR_BACKGROUNDS.length);
        String auteurNom = sujet.getAuteur() != null ? displayName(sujet.getAuteur()) : "Auteur inconnu";
        return new SujetDto(
                sujet.getId(),
                auteurNom,
                initialsFromFullName(auteurNom),
                AVATAR_BACKGROUNDS[paletteIndex],
                AVATAR_TEXTS[paletteIndex],
                libelleRattachement(sujet),
                sujet.getCours() != null ? sujet.getCours().getNom() : "",
                sujet.getTitre(),
                apercu,
                sujet.getDateCreation().format(DATE_HEURE_FORUM),
                statutLisible(sujet),
                messages);
    }

    /** Rattachement affiché en tête de sujet : cours, promotion, ou salon général. */
    private String libelleRattachement(ForumSujet sujet) {
        if (sujet.getCours() != null) {
            return sujet.getCours().getTitre();
        }
        if (sujet.getPromotion() != null) {
            return "Promotion " + sujet.getPromotion().getLibelle();
        }
        return "Général";
    }

    /**
     * Traduit l'etat du sujet dans le vocabulaire attendu par l'ecran formateur.
     * L'ancien modele portait une enumeration NOUVEAU / URGENT / RESOLU ; le modele
     * conserve exprime la meme chose avec deux booleens, « clos » l'emportant sur
     * « epingle » puisqu'un sujet resolu n'est plus a traiter.
     */
    private String statutLisible(ForumSujet sujet) {
        if (sujet.isClos()) {
            return "resolu";
        }
        return sujet.isEpingle() ? "urgent" : "nouveau";
    }

    private MessageDto toMessageDto(ForumMessage message) {
        String auteurNom = message.getAuteur() != null ? displayName(message.getAuteur()) : "Auteur inconnu";
        return new MessageDto(
                message.getId(),
                auteurNom,
                initialsFromFullName(auteurNom),
                message.getContenu(),
                message.getDateCreation().format(DATE_HEURE_FORUM),
                message.getPieceJointeNomOriginal());
    }

    private String displayName(User user) {
        return etudiantRepository.findByUserId(user.getId())
                .map(etudiant -> String.join(" ", etudiant.getPrenom(), etudiant.getNom()).trim())
                .or(() -> formateurRepository.findByUserId(user.getId())
                        .map(formateur -> String.join(" ", formateur.getPrenom(), formateur.getNom()).trim()))
                .orElse(user.getUsername());
    }

    private String initialsFromFullName(String fullName) {
        if (isBlank(fullName)) {
            return "?";
        }
        String[] parts = fullName.trim().split("\\s+");
        String result = parts.length == 1
                ? parts[0].substring(0, 1)
                : parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1);
        return result.toUpperCase();
    }

    private Seance findOwnedSession(Long id, Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) return null;
        return seanceService.getByFormateur(formateur.getId()).stream().filter(seance -> seance.getId().equals(id)).findFirst().orElse(null);
    }

    private void applySession(Seance seance, SeanceRequest request, Cours cours) {
        seance.setCours(cours);
        seance.setTitre(request.titre().trim());
        seance.setType(request.type().trim().toLowerCase());
        seance.setDateHeure(request.dateHeure());
        seance.setDureeMinutes(request.dureeMinutes());
        seance.setLieuOuLien(request.lieuOuLien() == null ? "" : request.lieuOuLien().trim());
    }

    private SeanceDto toSeanceDto(Seance seance) {
        return new SeanceDto(seance.getId(), seance.getCours().getId(), seance.getCours().getTitre(), seance.getTitre(), seance.getType(), seance.getDateHeure(), seance.getDureeMinutes(), seance.getLieuOuLien());
    }

    private SupportDto toSupportDto(SupportCours support) {
        Cours cours = support.getCours();
        return new SupportDto(
                support.getId().toString(),
                support.getTitre(),
                support.getUrl(),
                support.getTypeRessource(),
                support.getDatePublication() != null ? support.getDatePublication().toString() : null,
                perimetreFormateurService.supportEstProgramme(support),
                cours != null ? cours.getId() : null,
                cours != null ? cours.getNom() : null);
    }

    private boolean estLienHttpValide(String valeur) {
        try {
            URI uri = new URI(valeur.trim());
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null;
        } catch (URISyntaxException exception) {
            return false;
        }
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

    private EtudiantSuiviDto toEtudiantSuiviDto(Etudiant etudiant) {
        String nomComplet = String.join(" ", etudiant.getPrenom(), etudiant.getNom()).trim();
        String promotion = etudiant.getPromotion() != null ? etudiant.getPromotion().getLibelle() : "Non affectée";
        // Passe par le service : la moyenne a plat qui vivait ici comptait un cours
        // autant de fois qu'il avait de devoirs, et ne correspondait donc pas au releve
        // que l'etudiant voit de son cote.
        Double noteMoyenne = releveNotesService.moyenneDeLEtudiant(etudiant.getId());
        return new EtudiantSuiviDto(
                etudiant.getId(),
                nomComplet,
                promotion,
                "Non renseignée",
                0,
                noteMoyenne,
                releveNotesService.statutDeSuivi(noteMoyenne),
                initials(etudiant.getPrenom(), etudiant.getNom()),
                "bg-primary-container",
                etudiant.getMatricule(),
                // Sans objet cote formateur : il enseigne a ses etudiants, il ne les inscrit pas.
                null,
                false);
    }


    private User findAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String username = authentication.getName();
        User user = userRepository.findByUsername(username);
        return user != null ? user : userRepository.findByEmail(username);
    }

    private Formateur findAuthenticatedFormateur(Authentication authentication) {
        User user = findAuthenticatedUser(authentication);
        return user == null ? null : formateurRepository.findByUserId(user.getId()).orElse(null);
    }

    private Cours findOwnedCourse(Long courseId, Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) {
            return null;
        }
        return coursService.getByFormateurId(formateur.getId()).stream()
                .filter(cours -> cours.getId().equals(courseId))
                .findFirst()
                .orElse(null);
    }
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String initials(String firstName, String lastName) {
        String firstInitial = isBlank(firstName) ? "" : firstName.trim().substring(0, 1);
        String lastInitial = isBlank(lastName) ? "" : lastName.trim().substring(0, 1);
        String result = (firstInitial + lastInitial).toUpperCase();
        return result.isBlank() ? "?" : result;
    }
}
