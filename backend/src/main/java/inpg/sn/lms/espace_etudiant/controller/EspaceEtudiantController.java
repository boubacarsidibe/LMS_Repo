package inpg.sn.lms.espace_etudiant.controller;

import java.io.IOException;
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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.entity.Formateur;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.auth.repository.FormateurRepository;
import inpg.sn.lms.auth.repository.UserRepository;
import inpg.sn.lms.espace_etudiant.dto.AnnonceCreateRequest;
import inpg.sn.lms.espace_etudiant.dto.AnnonceDto;
import inpg.sn.lms.espace_etudiant.dto.CertificatCreateRequest;
import inpg.sn.lms.espace_etudiant.dto.CertificatDto;
import inpg.sn.lms.espace_etudiant.dto.CohorteDto;
import inpg.sn.lms.espace_etudiant.dto.CompteurParId;
import inpg.sn.lms.espace_etudiant.dto.ContactEtudiantRequest;
import inpg.sn.lms.espace_etudiant.dto.CoursCreateRequest;
import inpg.sn.lms.espace_etudiant.dto.CoursDto;
import inpg.sn.lms.espace_etudiant.dto.EtudiantSuiviDto;
import inpg.sn.lms.espace_etudiant.dto.EvenementCreateRequest;
import inpg.sn.lms.espace_etudiant.dto.EvenementDto;
import inpg.sn.lms.espace_etudiant.dto.FormateurProfileDto;
import inpg.sn.lms.espace_etudiant.dto.FormationDto;
import inpg.sn.lms.espace_etudiant.dto.MessageDto;
import inpg.sn.lms.espace_etudiant.dto.NoteModuleCreateRequest;
import inpg.sn.lms.espace_etudiant.dto.NoteModuleDto;
import inpg.sn.lms.espace_etudiant.dto.PresenceBulkUpdateRequest;
import inpg.sn.lms.espace_etudiant.dto.PresenceEtudiantDto;
import inpg.sn.lms.espace_etudiant.dto.PresenceHistoriqueDto;
import inpg.sn.lms.espace_etudiant.dto.SeanceCreateRequest;
import inpg.sn.lms.espace_etudiant.dto.SeanceDto;
import inpg.sn.lms.espace_etudiant.dto.PhotoDto;
import inpg.sn.lms.espace_etudiant.dto.RelanceCollectiveRequest;
import inpg.sn.lms.espace_etudiant.dto.RetourCreateRequest;
import inpg.sn.lms.espace_etudiant.dto.RetourEvaluationDto;
import inpg.sn.lms.espace_etudiant.dto.SujetDto;
import inpg.sn.lms.espace_etudiant.dto.SupportDto;
import inpg.sn.lms.espace_etudiant.entity.Annonce;
import inpg.sn.lms.espace_etudiant.entity.Certificat;
import inpg.sn.lms.espace_etudiant.entity.Cohorte;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.Evenement;
import inpg.sn.lms.espace_etudiant.entity.Formation;
import inpg.sn.lms.espace_etudiant.entity.Message;
import inpg.sn.lms.espace_etudiant.entity.NoteModule;
import inpg.sn.lms.espace_etudiant.entity.Presence;
import inpg.sn.lms.espace_etudiant.entity.Seance;
import inpg.sn.lms.espace_etudiant.entity.Photo;
import inpg.sn.lms.espace_etudiant.entity.RetourEvaluation;
import inpg.sn.lms.espace_etudiant.entity.Sujet;
import inpg.sn.lms.espace_etudiant.entity.SupportCours;
import inpg.sn.lms.espace_etudiant.enums.EnumStatutRetour;
import inpg.sn.lms.espace_etudiant.enums.EnumStatutSujet;
import inpg.sn.lms.espace_etudiant.enums.EnumTypeEvenement;
import inpg.sn.lms.espace_etudiant.service.AnnonceService;
import inpg.sn.lms.espace_etudiant.service.CertificatPdfService;
import inpg.sn.lms.espace_etudiant.service.CertificatService;
import inpg.sn.lms.espace_etudiant.service.CohorteService;
import inpg.sn.lms.espace_etudiant.service.CoursService;
import inpg.sn.lms.espace_etudiant.service.EvenementService;
import inpg.sn.lms.espace_etudiant.service.FormationService;
import inpg.sn.lms.espace_etudiant.service.PerimetreFormateurService;
import inpg.sn.lms.espace_etudiant.service.PhotoService;
import inpg.sn.lms.espace_etudiant.service.RetourEvaluationService;
import inpg.sn.lms.espace_etudiant.service.SujetService;
import inpg.sn.lms.espace_etudiant.service.NoteModuleService;
import inpg.sn.lms.espace_etudiant.service.PresenceService;
import inpg.sn.lms.espace_etudiant.service.SupportService;

@RestController
@RequestMapping("/api/espace-etudiant")
@CrossOrigin(origins = "http://localhost:4200")
public class EspaceEtudiantController {

    private static final String DEFAULT_COURSE_COLOR = "bg-secondary-container";
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EspaceEtudiantController.class);

    private final UserRepository userRepository;
    private final EtudiantRepository etudiantRepository;
    private final FormateurRepository formateurRepository;
    private final FormationService formationService;
    private final CoursService coursService;
    private final CohorteService cohorteService;
    private final SupportService supportService;
    private final EvenementService evenementService;
    private final SujetService sujetService;
    private final AnnonceService annonceService;
    private final PhotoService photoService;
    private final CertificatService certificatService;
    private final CertificatPdfService certificatPdfService;
    private final inpg.sn.lms.recrutement.service.NotificationService notificationService;
    private final RetourEvaluationService retourEvaluationService;
    private final PerimetreFormateurService perimetreFormateurService;
    private final NoteModuleService noteModuleService;
    private final PresenceService presenceService;
    private final Path uploadDirectory;

    public EspaceEtudiantController(
            UserRepository userRepository,
            EtudiantRepository etudiantRepository,
            FormateurRepository formateurRepository,
            FormationService formationService,
            CoursService coursService,
            CohorteService cohorteService,
            SupportService supportService,
            EvenementService evenementService,
            SujetService sujetService,
            AnnonceService annonceService,
            PhotoService photoService,
            CertificatService certificatService,
            CertificatPdfService certificatPdfService,
            inpg.sn.lms.recrutement.service.NotificationService notificationService,
            RetourEvaluationService retourEvaluationService,
            PerimetreFormateurService perimetreFormateurService,
            NoteModuleService noteModuleService,
            PresenceService presenceService,
            @Value("${app.upload.dir:uploads}") String uploadDirectory) {
        this.userRepository = userRepository;
        this.etudiantRepository = etudiantRepository;
        this.formateurRepository = formateurRepository;
        this.formationService = formationService;
        this.coursService = coursService;
        this.cohorteService = cohorteService;
        this.supportService = supportService;
        this.evenementService = evenementService;
        this.sujetService = sujetService;
        this.annonceService = annonceService;
        this.photoService = photoService;
        this.certificatService = certificatService;
        this.certificatPdfService = certificatPdfService;
        this.notificationService = notificationService;
        this.retourEvaluationService = retourEvaluationService;
        this.perimetreFormateurService = perimetreFormateurService;
        this.noteModuleService = noteModuleService;
        this.presenceService = presenceService;
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    /**
     * Les favoris sont une association LAZY et open-in-view est désactivé : sans
     * transaction ouverte sur toute la méthode, l'entité User est détachée dès le
     * retour du repository et la lecture de la collection lève une
     * LazyInitializationException.
     */
    @PreAuthorize("hasAnyRole('ETUDIANT','FORMATEUR')")
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
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        User user = findAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Etudiant etudiant = etudiantRepository.findByUserId(user.getId()).orElse(null);
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
        return ResponseEntity.ok(profile);
    }

    @PreAuthorize("hasAnyRole('ETUDIANT','FORMATEUR')")
    @GetMapping("/formations")
    public ResponseEntity<List<FormationDto>> listFormations() {
        List<FormationDto> list = formationService.getAll().stream()
                .map(f -> new FormationDto(f.getId(), f.getCode(), f.getIntitule()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PreAuthorize("hasAnyRole('ETUDIANT','FORMATEUR')")
    @GetMapping("/cours")
    public ResponseEntity<List<CoursDto>> listCours() {
        return ResponseEntity.ok(toCoursDtoList(coursService.getAll()));
    }

    @PreAuthorize("hasAnyRole('ETUDIANT','FORMATEUR')")
    @GetMapping("/cohortes")
    public ResponseEntity<List<CohorteDto>> listCohortes() {
        List<CohorteDto> list = cohorteService.getAll().stream()
                .map(this::toCohorteDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PreAuthorize("hasAnyRole('ETUDIANT','FORMATEUR')")
    @GetMapping("/annonces")
    public ResponseEntity<List<AnnonceDto>> listAnnonces(Authentication authentication) {
        User user = findAuthenticatedUser(authentication);
        List<Long> mesCohorteIds = user != null ? cohorteIdsVisiblesPour(user) : List.of();
        return ResponseEntity.ok(annonceService.getAll().stream()
                .filter(a -> a.getCohortes().isEmpty() || a.getCohortes().stream().anyMatch(c -> mesCohorteIds.contains(c.getId())))
                .map(this::toAnnonceDto).toList());
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @GetMapping("/formateur/cohortes")
    public ResponseEntity<?> listCohortesDuFormateur(Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        List<Long> mesCohorteIds = cohorteIdsDuFormateur(formateur);
        return ResponseEntity.ok(cohorteService.getAll().stream()
                .filter(c -> mesCohorteIds.contains(c.getId()))
                .map(this::toCohorteDto)
                .toList());
    }

    private List<Long> cohorteIdsVisiblesPour(User user) {
        return perimetreFormateurService.cohorteIdsVisiblesPour(user);
    }

    @PreAuthorize("hasAnyRole('ETUDIANT','FORMATEUR')")
    @GetMapping("/photos")
    public ResponseEntity<List<PhotoDto>> listPhotos() {
        return ResponseEntity.ok(photoService.getAll().stream().map(this::toPhotoDto).toList());
    }

    /**
     * Un formateur voit tous les certificats émis ; un étudiant ne voit que
     * les siens (l'émission reste réservée au formateur, cf TDR §B).
     */
    @PreAuthorize("hasAnyRole('ETUDIANT','FORMATEUR')")
    @GetMapping("/certificats")
    public ResponseEntity<?> listCertificats(Authentication authentication) {
        User user = findAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (formateurRepository.findByUserId(user.getId()).isPresent()) {
            return ResponseEntity.ok(certificatService.getAll().stream().map(this::toCertificatDto).toList());
        }
        Etudiant etudiant = etudiantRepository.findByUserId(user.getId()).orElse(null);
        if (etudiant == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(certificatService.getByEtudiant(etudiant.getId()).stream().map(this::toCertificatDto).toList());
    }

    @PreAuthorize("hasAnyRole('ETUDIANT','FORMATEUR')")
    @GetMapping("/evenements")
    public ResponseEntity<List<EvenementDto>> listEvenements() {
        return ResponseEntity.ok(evenementService.getAll().stream().map(this::toEvenementDto).toList());
    }

    @PreAuthorize("hasAnyRole('ETUDIANT','FORMATEUR')")
    @GetMapping("/sujets")
    public ResponseEntity<List<SujetDto>> listSujets() {
        return ResponseEntity.ok(sujetService.getAll().stream().map(this::toSujetDto).toList());
    }

    @PreAuthorize("hasAnyRole('ETUDIANT','FORMATEUR')")
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
        Sujet sujet = sujetService.findById(id).orElse(null);
        if (sujet == null) {
            return ResponseEntity.notFound().build();
        }

        String pieceJointeNomOriginal = null;
        String pieceJointeNomStocke = null;
        String pieceJointeTypeContenu = null;
        if (fichier != null && !fichier.isEmpty()) {
            pieceJointeNomOriginal = Path.of(
                    fichier.getOriginalFilename() == null ? "fichier" : fichier.getOriginalFilename())
                    .getFileName().toString();
            if (pieceJointeNomOriginal.isBlank()) {
                pieceJointeNomOriginal = "fichier";
            }
            pieceJointeNomStocke = UUID.randomUUID() + "-" + pieceJointeNomOriginal;
            pieceJointeTypeContenu = fichier.getContentType();
            try {
                Path messagesDirectory = uploadDirectory.resolve("messages");
                Files.createDirectories(messagesDirectory);
                Files.copy(fichier.getInputStream(), messagesDirectory.resolve(pieceJointeNomStocke),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException exception) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Le fichier n'a pas pu être enregistré."));
            }
        }

        Sujet updated = sujetService.addMessage(sujet, displayName(user), contenu.trim(), marquerCommeResolu,
                pieceJointeNomOriginal, pieceJointeNomStocke, pieceJointeTypeContenu);
        return ResponseEntity.status(HttpStatus.CREATED).body(toSujetDto(updated));
    }

    @PreAuthorize("hasAnyRole('ETUDIANT','FORMATEUR')")
    @GetMapping("/sujets/messages/{messageId}/piece-jointe")
    public ResponseEntity<Resource> downloadMessageAttachment(@PathVariable Long messageId) {
        Message message = sujetService.findMessageById(messageId).orElse(null);
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
    @PatchMapping("/formateur/sujets/{id}/urgence")
    public ResponseEntity<?> toggleUrgenceSujet(@PathVariable Long id, Authentication authentication) {
        if (findAuthenticatedFormateur(authentication) == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        Sujet sujet = sujetService.findById(id).orElse(null);
        if (sujet == null) {
            return ResponseEntity.notFound().build();
        }
        sujet.setStatut(sujet.getStatut() == EnumStatutSujet.URGENT ? EnumStatutSujet.NOUVEAU : EnumStatutSujet.URGENT);
        return ResponseEntity.ok(toSujetDto(sujetService.save(sujet)));
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @PatchMapping("/formateur/sujets/{id}/resoudre")
    public ResponseEntity<?> marquerSujetResolu(@PathVariable Long id, Authentication authentication) {
        if (findAuthenticatedFormateur(authentication) == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        Sujet sujet = sujetService.findById(id).orElse(null);
        if (sujet == null) {
            return ResponseEntity.notFound().build();
        }
        sujet.setStatut(EnumStatutSujet.RESOLU);
        return ResponseEntity.ok(toSujetDto(sujetService.save(sujet)));
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @DeleteMapping("/formateur/sujets/{id}")
    public ResponseEntity<?> supprimerSujet(@PathVariable Long id, Authentication authentication) {
        if (findAuthenticatedFormateur(authentication) == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        if (sujetService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        sujetService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ETUDIANT','FORMATEUR')")
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

    @PreAuthorize("hasAnyRole('ETUDIANT','FORMATEUR')")
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

    @PreAuthorize("hasAnyRole('ETUDIANT','FORMATEUR')")
    @GetMapping("/supports/pedagogiques")
    public ResponseEntity<List<SupportDto>> listPedagogicalSupports() {
        return ResponseEntity.ok(supportService.getAllPedagogicalSupports().stream()
                .map(s -> new SupportDto(s.getId().toString(), s.getTitre(), null, null, null, false, null, null))
                .toList());
    }

    @PreAuthorize("hasAnyRole('ETUDIANT','FORMATEUR')")
    @GetMapping("/supports/pedagogiques/{id}")
    public ResponseEntity<SupportDto> getPedagogicalSupport(@PathVariable UUID id) {
        var support = supportService.findPedagogicalById(id);
        if (support == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new SupportDto(support.getId().toString(), support.getTitre(), null, null, null, false, null, null));
    }

    @PreAuthorize("hasAnyRole('ETUDIANT','FORMATEUR')")
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

    @PreAuthorize("hasAnyRole('ETUDIANT','FORMATEUR')")
    @GetMapping("/photos/{id}/fichier")
    public ResponseEntity<Resource> getPhotoFile(@PathVariable Long id) {
        Photo photo = photoService.findById(id).orElse(null);
        if (photo == null) {
            return ResponseEntity.notFound().build();
        }

        Path filePath = uploadDirectory.resolve("photos").resolve(photo.getNomFichier()).normalize();
        if (!filePath.startsWith(uploadDirectory) || !Files.isRegularFile(filePath)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(filePath);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getTypeContenu()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(photo.getTitre()).build().toString())
                .body(resource);
    }

    @PreAuthorize("hasAnyRole('ETUDIANT','FORMATEUR')")
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

        boolean estFormateur = formateurRepository.findByUserId(user.getId()).isPresent();
        boolean estProprietaire = certificat.getEtudiant().getUser() != null
                && certificat.getEtudiant().getUser().getId().equals(user.getId());
        if (!estFormateur && !estProprietaire) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Vous n'avez pas accès à ce certificat."));
        }

        byte[] pdf = certificatPdfService.genererPdf(certificat);
        String nomFichier = "certificat-" + certificat.getReference() + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(nomFichier).build().toString())
                .body(pdf);
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @GetMapping("/formateur/profile")
    public ResponseEntity<?> getFormateurProfile(Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        long numberOfCourses = coursService.getByFormateurId(formateur.getId()).size();
        String fullName = String.join(" ", formateur.getPrenom(), formateur.getNom()).trim();
        return ResponseEntity.ok(new FormateurProfileDto(
                formateur.getId(), fullName, formateur.getUser().getEmail(), numberOfCourses));
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @GetMapping("/formateur/cours")
    public ResponseEntity<?> listMyCourses(Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        return ResponseEntity.ok(toCoursDtoList(coursService.getByFormateurId(formateur.getId())));
    }

    @PreAuthorize("hasRole('FORMATEUR')")
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
        if (request.cohorteIds() != null) {
            for (Long cohorteId : request.cohorteIds()) {
                cohorteService.findById(cohorteId).ifPresent(cours.getCohortes()::add);
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(toCoursDto(coursService.save(cours)));
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @PatchMapping("/formateur/cours/{id}")
    public ResponseEntity<?> updateCourse(
            @PathVariable Long id,
            @RequestBody CoursCreateRequest request,
            Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        Cours cours = coursService.findById(id).orElse(null);
        if (cours == null) {
            return ResponseEntity.notFound().build();
        }
        if (!perimetreFormateurService.estFormateurDuCours(formateur, cours)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Vous n'êtes pas formateur de ce cours."));
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

    @PreAuthorize("hasRole('FORMATEUR')")
    @PatchMapping("/formateur/cours/{id}/cohortes")
    public ResponseEntity<?> updateCoursCohortes(
            @PathVariable Long id,
            @RequestBody Map<String, List<Long>> body,
            Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        Cours cours = coursService.findById(id).orElse(null);
        if (cours == null) {
            return ResponseEntity.notFound().build();
        }
        if (!perimetreFormateurService.estFormateurDuCours(formateur, cours)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Vous n'êtes pas formateur de ce cours."));
        }
        List<Long> cohorteIds = body.getOrDefault("cohorteIds", List.of());
        Set<Cohorte> cohortes = new LinkedHashSet<>();
        for (Long cohorteId : cohorteIds) {
            Cohorte cohorte = cohorteService.findById(cohorteId).orElse(null);
            if (cohorte == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Cohorte introuvable : " + cohorteId));
            }
            cohortes.add(cohorte);
        }
        cours.setCohortes(cohortes);
        return ResponseEntity.ok(toCoursDto(coursService.save(cours)));
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @PostMapping("/formateur/certificats")
    public ResponseEntity<?> creerCertificat(
            @RequestBody CertificatCreateRequest request,
            Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        if (request.etudiantId() == null || isBlank(request.intituleFormation())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "L'étudiant et l'intitulé de la formation sont requis."));
        }
        Etudiant etudiant = etudiantRepository.findById(request.etudiantId()).orElse(null);
        if (etudiant == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Étudiant introuvable."));
        }
        Cohorte cohorte = null;
        if (request.cohorteId() != null) {
            cohorte = cohorteService.findById(request.cohorteId()).orElse(null);
            if (cohorte == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Cohorte introuvable."));
            }
        }

        Certificat certificat = new Certificat();
        certificat.setEtudiant(etudiant);
        certificat.setCohorte(cohorte);
        certificat.setIntituleFormation(request.intituleFormation().trim());
        certificat.setFormateurNom(displayName(formateur.getUser()));

        return ResponseEntity.status(HttpStatus.CREATED).body(toCertificatDto(certificatService.save(certificat)));
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @PostMapping("/formateur/annonces")
    public ResponseEntity<?> creerAnnonce(
            @RequestBody AnnonceCreateRequest request,
            Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        if (isBlank(request.titre()) || isBlank(request.contenu())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le titre et le contenu sont requis."));
        }

        List<Cohorte> cohortesCiblees = new ArrayList<>();
        if (request.cohorteIds() != null && !request.cohorteIds().isEmpty()) {
            List<Long> mesCohorteIds = cohorteIdsDuFormateur(formateur);
            for (Long cohorteId : request.cohorteIds()) {
                if (!mesCohorteIds.contains(cohorteId)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "Vous ne pouvez cibler que les cohortes de vos propres cours."));
                }
                cohorteService.findById(cohorteId).ifPresent(cohortesCiblees::add);
            }
        }

        Annonce annonce = new Annonce();
        annonce.setTitre(request.titre().trim());
        annonce.setContenu(request.contenu().trim());
        annonce.setAuteurNom(displayName(formateur.getUser()));
        annonce.setDateCreation(java.time.LocalDateTime.now());
        annonce.setEpinglee(request.epinglee());
        annonce.setCohortes(cohortesCiblees);

        return ResponseEntity.status(HttpStatus.CREATED).body(toAnnonceDto(annonceService.save(annonce)));
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @PostMapping("/formateur/evenements")
    public ResponseEntity<?> createEvenement(
            @RequestBody EvenementCreateRequest request,
            Authentication authentication) {
        if (findAuthenticatedFormateur(authentication) == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        if (isBlank(request.titre()) || isBlank(request.date()) || isBlank(request.heure()) || isBlank(request.type())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Titre, date, heure et type sont requis."));
        }

        LocalDate date;
        EnumTypeEvenement type;
        try {
            date = LocalDate.parse(request.date());
        } catch (java.time.format.DateTimeParseException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", "La date est invalide."));
        }
        try {
            type = EnumTypeEvenement.valueOf(request.type().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le type d'événement est invalide."));
        }

        Evenement evenement = new Evenement();
        evenement.setTitre(request.titre().trim());
        evenement.setDate(date);
        evenement.setHeure(request.heure().trim());
        evenement.setType(type);
        evenement.setLienVisio(isBlank(request.lienVisio()) ? null : request.lienVisio().trim());

        return ResponseEntity.status(HttpStatus.CREATED).body(toEvenementDto(evenementService.save(evenement)));
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @PatchMapping("/formateur/evenements/{id}")
    public ResponseEntity<?> updateEvenement(
            @PathVariable Long id,
            @RequestBody EvenementCreateRequest request,
            Authentication authentication) {
        if (findAuthenticatedFormateur(authentication) == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        Evenement evenement = evenementService.findById(id).orElse(null);
        if (evenement == null) {
            return ResponseEntity.notFound().build();
        }
        if (isBlank(request.titre()) || isBlank(request.date()) || isBlank(request.heure()) || isBlank(request.type())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Titre, date, heure et type sont requis."));
        }

        LocalDate date;
        EnumTypeEvenement type;
        try {
            date = LocalDate.parse(request.date());
        } catch (java.time.format.DateTimeParseException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", "La date est invalide."));
        }
        try {
            type = EnumTypeEvenement.valueOf(request.type().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le type d'événement est invalide."));
        }

        evenement.setTitre(request.titre().trim());
        evenement.setDate(date);
        evenement.setHeure(request.heure().trim());
        evenement.setType(type);
        evenement.setLienVisio(isBlank(request.lienVisio()) ? null : request.lienVisio().trim());

        return ResponseEntity.ok(toEvenementDto(evenementService.save(evenement)));
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @PostMapping(value = "/formateur/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadPhoto(
            @RequestParam String titre,
            @RequestParam(required = false) Long cohorteId,
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
        if (isBlank(titre)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le titre est requis."));
        }
        String contentType = fichier.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Seuls les fichiers image sont acceptés."));
        }

        Cohorte cohorte = null;
        if (cohorteId != null) {
            cohorte = cohorteService.findById(cohorteId).orElse(null);
            if (cohorte == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Cohorte introuvable."));
            }
        }

        String storedFileName = UUID.randomUUID() + "-" + Path.of(
                fichier.getOriginalFilename() == null ? "photo" : fichier.getOriginalFilename())
                .getFileName().toString();
        try {
            Path photosDirectory = uploadDirectory.resolve("photos");
            Files.createDirectories(photosDirectory);
            Files.copy(fichier.getInputStream(), photosDirectory.resolve(storedFileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Le fichier n'a pas pu être enregistré."));
        }

        Photo photo = new Photo();
        photo.setTitre(titre.trim());
        photo.setNomFichier(storedFileName);
        photo.setTypeContenu(contentType);
        photo.setAuteurNom(displayName(formateur.getUser()));
        photo.setDateAjout(java.time.LocalDateTime.now());
        photo.setCohorte(cohorte);

        return ResponseEntity.status(HttpStatus.CREATED).body(toPhotoDto(photoService.save(photo)));
    }

    @PreAuthorize("hasRole('FORMATEUR')")
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

        String originalName = Path.of(fichier.getOriginalFilename() == null ? "ressource" : fichier.getOriginalFilename())
                .getFileName().toString();
        if (originalName.isBlank()) {
            originalName = "ressource";
        }
        String storedFileName = UUID.randomUUID() + "-" + originalName;
        try {
            Files.createDirectories(uploadDirectory);
            Files.copy(fichier.getInputStream(), uploadDirectory.resolve(storedFileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Le fichier n'a pas pu être enregistré."));
        }

        SupportCours support = new SupportCours();
        support.setCours(cours);
        support.setTitre(originalName);
        support.setTypeRessource(typeRessource);
        support.setFichierStocke(storedFileName);
        support.setDatePublication(publicationDate);
        SupportCours savedSupport = supportService.saveCourseSupport(support);
        savedSupport.setUrl("/api/espace-etudiant/supports/cours/" + savedSupport.getId() + "/download");
        savedSupport = supportService.saveCourseSupport(savedSupport);
        return ResponseEntity.status(HttpStatus.CREATED).body(toSupportDto(savedSupport));
    }

    /**
     * Ressources des cours du formateur connecte uniquement — contrairement a
     * /supports/cours qui expose celles de tous les cours de la plateforme.
     */
    @PreAuthorize("hasRole('FORMATEUR')")
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
        return ResponseEntity.ok(etudiantRepository.findByCohorte_IdIn(cohorteIds).stream()
                .map(this::toEtudiantSuiviDto).toList());
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @GetMapping("/formateur/etudiants/{id}")
    public ResponseEntity<?> getStudent(@PathVariable Long id, Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        List<Long> cohorteIds = cohorteIdsDuFormateur(formateur);
        return etudiantRepository.findById(id)
                .filter(etudiant -> etudiant.getCohorte() != null && cohorteIds.contains(etudiant.getCohorte().getId()))
                .<ResponseEntity<?>>map(etudiant -> ResponseEntity.ok(toEtudiantSuiviDto(etudiant)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @GetMapping("/formateur/etudiants/{id}/notes")
    public ResponseEntity<?> listNotesEtudiant(@PathVariable Long id, Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        List<Long> cohorteIds = cohorteIdsDuFormateur(formateur);
        Etudiant etudiant = etudiantRepository.findById(id)
                .filter(e -> e.getCohorte() != null && cohorteIds.contains(e.getCohorte().getId()))
                .orElse(null);
        if (etudiant == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(noteModuleService.getByEtudiantId(id).stream().map(this::toNoteModuleDto).toList());
    }

    @PreAuthorize("hasRole('FORMATEUR')")
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
                .filter(e -> e.getCohorte() != null && cohorteIds.contains(e.getCohorte().getId()))
                .orElse(null);
        if (etudiant == null) {
            return ResponseEntity.notFound().build();
        }
        if (isBlank(request.module())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le nom du module est requis."));
        }
        if (request.note() < 0 || request.note() > 20) {
            return ResponseEntity.badRequest().body(Map.of("message", "La note doit être comprise entre 0 et 20."));
        }
        Cours cours = coursService.getByFormateurId(formateur.getId()).stream()
                .filter(c -> c.getId().equals(request.coursId()))
                .findFirst()
                .orElse(null);
        if (cours == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Vous n'enseignez pas ce cours."));
        }
        if (cours.getCohortes().stream().noneMatch(c -> c.getId().equals(etudiant.getCohorte().getId()))) {
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

    @PreAuthorize("hasRole('FORMATEUR')")
    @PostMapping("/formateur/cours/{coursId}/seances")
    public ResponseEntity<?> creerSeance(
            @PathVariable Long coursId,
            @RequestBody SeanceCreateRequest request,
            Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        if (isBlank(request.titre())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le titre de la séance est requis."));
        }
        Cours cours = coursService.getByFormateurId(formateur.getId()).stream()
                .filter(c -> c.getId().equals(coursId))
                .findFirst()
                .orElse(null);
        if (cours == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Vous n'enseignez pas ce cours."));
        }
        LocalDate date;
        try {
            date = isBlank(request.date()) ? LocalDate.now() : LocalDate.parse(request.date());
        } catch (java.time.format.DateTimeParseException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", "La date est invalide."));
        }

        Seance seance = new Seance();
        seance.setCours(cours);
        seance.setTitre(request.titre().trim());
        seance.setDate(date);
        return ResponseEntity.status(HttpStatus.CREATED).body(toSeanceDto(presenceService.saveSeance(seance)));
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @GetMapping("/formateur/cours/{coursId}/seances")
    public ResponseEntity<?> listSeances(@PathVariable Long coursId, Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        boolean enseigneLeCours = coursService.getByFormateurId(formateur.getId()).stream()
                .anyMatch(c -> c.getId().equals(coursId));
        if (!enseigneLeCours) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Vous n'enseignez pas ce cours."));
        }
        return ResponseEntity.ok(presenceService.getSeancesByCoursId(coursId).stream().map(this::toSeanceDto).toList());
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @GetMapping("/formateur/seances/{id}/presences")
    public ResponseEntity<?> listPresencesSeance(@PathVariable Long id, Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        Seance seance = presenceService.findSeanceById(id).orElse(null);
        if (seance == null) {
            return ResponseEntity.notFound().build();
        }
        if (!perimetreFormateurService.estFormateurDuCours(formateur, seance.getCours())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Vous n'êtes pas formateur de ce cours."));
        }
        List<Long> cohorteIds = seance.getCours().getCohortes().stream().map(Cohorte::getId).toList();
        List<Etudiant> etudiants = etudiantRepository.findByCohorte_IdIn(cohorteIds);
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
        Seance seance = presenceService.findSeanceById(id).orElse(null);
        if (seance == null) {
            return ResponseEntity.notFound().build();
        }
        if (!perimetreFormateurService.estFormateurDuCours(formateur, seance.getCours())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Vous n'êtes pas formateur de ce cours."));
        }
        List<Long> cohorteIds = seance.getCours().getCohortes().stream().map(Cohorte::getId).toList();
        for (PresenceBulkUpdateRequest.Entree entree : request.presences()) {
            Etudiant etudiant = etudiantRepository.findById(entree.etudiantId()).orElse(null);
            if (etudiant == null || etudiant.getCohorte() == null || !cohorteIds.contains(etudiant.getCohorte().getId())) {
                continue; // etudiant hors perimetre de ce cours : ignore plutot que planter tout le lot
            }
            presenceService.marquerPresence(seance, etudiant, entree.present());
        }
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('FORMATEUR')")
    @GetMapping("/formateur/etudiants/{id}/presences")
    public ResponseEntity<?> listPresencesEtudiant(@PathVariable Long id, Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        List<Long> cohorteIds = cohorteIdsDuFormateur(formateur);
        Etudiant etudiant = etudiantRepository.findById(id)
                .filter(e -> e.getCohorte() != null && cohorteIds.contains(e.getCohorte().getId()))
                .orElse(null);
        if (etudiant == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(presenceService.getPresencesByEtudiantId(id).stream()
                .map(this::toPresenceHistoriqueDto).toList());
    }

    private SeanceDto toSeanceDto(Seance seance) {
        return new SeanceDto(
                seance.getId(),
                seance.getTitre(),
                seance.getDate().toString(),
                seance.getCours().getId(),
                seance.getCours().getNom());
    }

    private PresenceHistoriqueDto toPresenceHistoriqueDto(Presence presence) {
        Seance seance = presence.getSeance();
        return new PresenceHistoriqueDto(
                presence.getId(),
                seance.getDate().toString(),
                seance.getCours().getNom(),
                seance.getCours().getTitre(),
                seance.getTitre(),
                presence.isPresent());
    }

    private List<Long> cohorteIdsDuFormateur(Formateur formateur) {
        return perimetreFormateurService.cohorteIdsDuFormateur(formateur);
    }

    @PreAuthorize("hasRole('FORMATEUR')")
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

    @PreAuthorize("hasRole('ETUDIANT')")
    @PostMapping("/retours")
    public ResponseEntity<?> creerRetour(@RequestBody RetourCreateRequest request, Authentication authentication) {
        User user = findAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Etudiant etudiant = etudiantRepository.findByUserId(user.getId()).orElse(null);
        if (etudiant == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Seul un étudiant peut laisser un avis."));
        }
        if (request.coursId() == null || request.note() == null || isBlank(request.commentaire())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le cours, la note et le commentaire sont requis."));
        }
        if (request.note() < 1 || request.note() > 5) {
            return ResponseEntity.badRequest().body(Map.of("message", "La note doit être comprise entre 1 et 5."));
        }
        Cours cours = coursService.findById(request.coursId()).orElse(null);
        if (cours == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cours introuvable."));
        }
        if (!perimetreFormateurService.etudiantSuitLeCours(etudiant, cours)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Vous ne suivez pas ce cours."));
        }

        RetourEvaluation retour = new RetourEvaluation();
        retour.setCours(cours);
        retour.setEtudiant(etudiant);
        retour.setNote(request.note());
        retour.setCommentaire(request.commentaire().trim());
        retour.setDateCreation(LocalDateTime.now());
        retour.setStatut(request.note() <= 3 ? EnumStatutRetour.ACTION_REQUISE : EnumStatutRetour.VALIDE);
        retour.setEpingle(false);

        return ResponseEntity.status(HttpStatus.CREATED).body(toRetourEvaluationDto(retourEvaluationService.save(retour)));
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
                .filter(e -> e.getCohorte() != null && cohorteIds.contains(e.getCohorte().getId()))
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
        List<Etudiant> destinataires = etudiantRepository.findByCohorte_IdIn(cohorteIds).stream()
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

    @PreAuthorize("hasRole('FORMATEUR')")
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
        Cohorte cohorte = cohorteService.findById(cohorteId).orElse(null);
        if (cohorte == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cohorte introuvable."));
        }

        etudiant.setCohorte(cohorte);
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
                .flatMap(c -> c.getCohortes().stream())
                .map(Cohorte::getId)
                .distinct()
                .toList();

        Map<Long, Long> etudiantsParCohorte = cohorteIds.isEmpty()
                ? Map.of()
                : etudiantRepository.compterParCohorte(cohorteIds).stream()
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
        long nbEtudiants = cours.getCohortes().stream()
                .mapToLong(cohorte -> etudiantsParCohorte.getOrDefault(cohorte.getId(), 0L))
                .sum();
        List<CohorteDto> cohortes = cours.getCohortes().stream()
                .map(this::toCohorteDto)
                .toList();
        return new CoursDto(
                cours.getId(),
                cours.getNom(),
                cours.getTitre(),
                cours.getDescription(),
                cours.getSemestre(),
                cours.getVolumeHoraire(),
                nbEtudiants,
                supportsParCours.getOrDefault(cours.getId(), 0L),
                0,
                DEFAULT_COURSE_COLOR,
                cohortes);
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
                certificat.getCohorte() != null ? certificat.getCohorte().getLibelle() : null,
                certificat.getDateEmission().toString(),
                certificat.getFormateurNom());
    }

    private PhotoDto toPhotoDto(Photo photo) {
        return new PhotoDto(
                photo.getId(),
                photo.getTitre(),
                "/api/espace-etudiant/photos/" + photo.getId() + "/fichier",
                photo.getAuteurNom(),
                photo.getDateAjout().toString(),
                photo.getCohorte() != null ? photo.getCohorte().getId() : null,
                photo.getCohorte() != null ? photo.getCohorte().getLibelle() : null);
    }

    private AnnonceDto toAnnonceDto(Annonce annonce) {
        List<CohorteDto> cohortes = annonce.getCohortes().stream()
                .map(this::toCohorteDto)
                .toList();
        return new AnnonceDto(
                annonce.getId(),
                annonce.getTitre(),
                annonce.getContenu(),
                annonce.getAuteurNom(),
                annonce.getDateCreation().toString(),
                annonce.isEpinglee(),
                cohortes);
    }

    private EvenementDto toEvenementDto(Evenement evenement) {
        return new EvenementDto(
                evenement.getId(),
                evenement.getTitre(),
                evenement.getDate().toString(),
                evenement.getHeure(),
                evenement.getType().name().toLowerCase(),
                evenement.getLienVisio());
    }

    private static final DateTimeFormatter DATE_HEURE_FORUM = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String[] AVATAR_BACKGROUNDS = {
            "bg-secondary-container", "bg-error-container/20", "bg-surface-container-highest", "bg-primary-container"
    };
    private static final String[] AVATAR_TEXTS = {
            "text-on-secondary-container", "text-error", "text-on-surface-variant", "text-on-primary-container"
    };

    private CohorteDto toCohorteDto(Cohorte cohorte) {
        Integer annee = cohorte.getDateDebut() != null ? cohorte.getDateDebut().getYear() : null;
        return new CohorteDto(cohorte.getId(), cohorte.getCode(), cohorte.getLibelle(), annee);
    }

    private SujetDto toSujetDto(Sujet sujet) {
        List<MessageDto> messages = sujet.getMessages().stream().map(this::toMessageDto).toList();
        String apercu = messages.isEmpty() ? "" : messages.get(0).contenu();
        if (apercu.length() > 140) {
            apercu = apercu.substring(0, 140) + "…";
        }
        int paletteIndex = (int) Math.floorMod(sujet.getId(), AVATAR_BACKGROUNDS.length);
        return new SujetDto(
                sujet.getId(),
                sujet.getAuteurNom(),
                initialsFromFullName(sujet.getAuteurNom()),
                AVATAR_BACKGROUNDS[paletteIndex],
                AVATAR_TEXTS[paletteIndex],
                sujet.getCoursNom(),
                sujet.getModule(),
                sujet.getTitre(),
                apercu,
                sujet.getDateCreation().format(DATE_HEURE_FORUM),
                sujet.getStatut().name().toLowerCase(),
                messages);
    }

    private MessageDto toMessageDto(Message message) {
        return new MessageDto(
                message.getId(),
                message.getAuteurNom(),
                initialsFromFullName(message.getAuteurNom()),
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

    private EtudiantSuiviDto toEtudiantSuiviDto(Etudiant etudiant) {
        String nomComplet = String.join(" ", etudiant.getPrenom(), etudiant.getNom()).trim();
        String cohorte = etudiant.getCohorte() != null ? etudiant.getCohorte().getLibelle() : "Non affectée";
        double noteMoyenne = noteModuleService.getByEtudiantId(etudiant.getId()).stream()
                .mapToDouble(NoteModule::getNote)
                .average()
                .orElse(0);
        return new EtudiantSuiviDto(
                etudiant.getId(),
                nomComplet,
                cohorte,
                "Non renseignée",
                0,
                Math.round(noteMoyenne * 10) / 10.0,
                "À risque",
                initials(etudiant.getPrenom(), etudiant.getNom()),
                "bg-primary-container",
                etudiant.getMatricule());
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
