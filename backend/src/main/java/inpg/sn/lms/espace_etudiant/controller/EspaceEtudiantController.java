package inpg.sn.lms.espace_etudiant.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
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
import inpg.sn.lms.espace_etudiant.dto.CoursCreateRequest;
import inpg.sn.lms.espace_etudiant.dto.CoursDto;
import inpg.sn.lms.espace_etudiant.dto.EtudiantSuiviDto;
import inpg.sn.lms.espace_etudiant.dto.FormateurProfileDto;
import inpg.sn.lms.espace_etudiant.dto.FormationDto;
import inpg.sn.lms.espace_etudiant.dto.PromotionDto;
import inpg.sn.lms.espace_etudiant.dto.SupportDto;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.SupportCours;
import inpg.sn.lms.espace_etudiant.service.CoursService;
import inpg.sn.lms.espace_etudiant.service.FormationService;
import inpg.sn.lms.espace_etudiant.service.PromotionService;
import inpg.sn.lms.espace_etudiant.service.SupportService;

@RestController
@RequestMapping("/api/espace-etudiant")
@CrossOrigin(origins = "http://localhost:4200")
public class EspaceEtudiantController {

    private static final String DEFAULT_COURSE_COLOR = "bg-secondary-container";

    private final UserRepository userRepository;
    private final EtudiantRepository etudiantRepository;
    private final FormateurRepository formateurRepository;
    private final FormationService formationService;
    private final CoursService coursService;
    private final PromotionService promotionService;
    private final SupportService supportService;
    private final Path uploadDirectory;

    public EspaceEtudiantController(
            UserRepository userRepository,
            EtudiantRepository etudiantRepository,
            FormateurRepository formateurRepository,
            FormationService formationService,
            CoursService coursService,
            PromotionService promotionService,
            SupportService supportService,
            @Value("${app.upload.dir:uploads}") String uploadDirectory) {
        this.userRepository = userRepository;
        this.etudiantRepository = etudiantRepository;
        this.formateurRepository = formateurRepository;
        this.formationService = formationService;
        this.coursService = coursService;
        this.promotionService = promotionService;
        this.supportService = supportService;
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

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
                    "telephone", etudiant.getTelephone()));
        }
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/formations")
    public ResponseEntity<List<FormationDto>> listFormations() {
        List<FormationDto> list = formationService.getAll().stream()
                .map(f -> new FormationDto(f.getId(), f.getCode(), f.getIntitule()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/cours")
    public ResponseEntity<List<CoursDto>> listCours() {
        return ResponseEntity.ok(coursService.getAll().stream().map(this::toCoursDto).toList());
    }

    @GetMapping("/promotions")
    public ResponseEntity<List<PromotionDto>> listPromotions() {
        List<PromotionDto> list = promotionService.getAll().stream()
                .map(p -> new PromotionDto(p.getId(), p.getCode(), p.getLibelle()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/supports")
    public ResponseEntity<List<SupportDto>> listAllSupports() {
        List<SupportDto> supports = supportService.getAllCourseSupports().stream()
                .map(this::toSupportDto)
                .collect(Collectors.toList());
        supports.addAll(supportService.getAllPedagogicalSupports().stream()
                .map(s -> new SupportDto(s.getId().toString(), s.getTitre(), null))
                .toList());
        return ResponseEntity.ok(supports);
    }

    @GetMapping("/supports/cours")
    public ResponseEntity<List<SupportDto>> listCourseSupports() {
        return ResponseEntity.ok(supportService.getAllCourseSupports().stream().map(this::toSupportDto).toList());
    }

    @GetMapping("/supports/pedagogiques")
    public ResponseEntity<List<SupportDto>> listPedagogicalSupports() {
        return ResponseEntity.ok(supportService.getAllPedagogicalSupports().stream()
                .map(s -> new SupportDto(s.getId().toString(), s.getTitre(), null))
                .toList());
    }

    @GetMapping("/supports/pedagogiques/{id}")
    public ResponseEntity<SupportDto> getPedagogicalSupport(@PathVariable UUID id) {
        var support = supportService.findPedagogicalById(id);
        if (support == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new SupportDto(support.getId().toString(), support.getTitre(), null));
    }

    @GetMapping("/supports/cours/{id}/download")
    public ResponseEntity<Resource> downloadCourseSupport(@PathVariable Long id) {
        SupportCours support = supportService.getAllCourseSupports().stream()
                .filter(item -> item.getId().equals(id))
                .findFirst()
                .orElse(null);
        if (support == null || support.getFichierStocke() == null) {
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

    @GetMapping("/formateur/cours")
    public ResponseEntity<?> listMyCourses(Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        return ResponseEntity.ok(coursService.getByFormateurId(formateur.getId()).stream()
                .map(this::toCoursDto)
                .toList());
    }

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

        return ResponseEntity.status(HttpStatus.CREATED).body(toCoursDto(coursService.save(cours)));
    }

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

    @GetMapping("/formateur/etudiants")
    public ResponseEntity<?> listStudents(Authentication authentication) {
        if (findAuthenticatedFormateur(authentication) == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        return ResponseEntity.ok(etudiantRepository.findAll().stream().map(this::toEtudiantSuiviDto).toList());
    }

    @GetMapping("/formateur/etudiants/{id}")
    public ResponseEntity<?> getStudent(@PathVariable Long id, Authentication authentication) {
        if (findAuthenticatedFormateur(authentication) == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profil formateur introuvable."));
        }
        return etudiantRepository.findById(id)
                .<ResponseEntity<?>>map(etudiant -> ResponseEntity.ok(toEtudiantSuiviDto(etudiant)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private CoursDto toCoursDto(Cours cours) {
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
                DEFAULT_COURSE_COLOR);
    }

    private SupportDto toSupportDto(SupportCours support) {
        return new SupportDto(
                support.getId().toString(),
                support.getTitre(),
                support.getUrl());
    }

    private EtudiantSuiviDto toEtudiantSuiviDto(Etudiant etudiant) {
        String nomComplet = String.join(" ", etudiant.getPrenom(), etudiant.getNom()).trim();
        return new EtudiantSuiviDto(
                etudiant.getId(),
                nomComplet,
                "Non affectée",
                "Non renseignée",
                0,
                0,
                "À risque",
                initials(etudiant.getPrenom(), etudiant.getNom()),
                "bg-primary-container");
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
