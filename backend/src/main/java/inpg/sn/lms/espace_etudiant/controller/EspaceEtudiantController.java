package inpg.sn.lms.espace_etudiant.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.LinkedHashMap;
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
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
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
import inpg.sn.lms.auth.repository.FormateurRepository;
import inpg.sn.lms.auth.repository.UserRepository;
import inpg.sn.lms.espace_etudiant.dto.CoursCreateRequest;
import inpg.sn.lms.espace_etudiant.dto.CoursDto;
import inpg.sn.lms.espace_etudiant.dto.EtudiantSuiviDto;
import inpg.sn.lms.espace_etudiant.dto.FormateurProfileDto;
import inpg.sn.lms.espace_etudiant.dto.FormationDto;
import inpg.sn.lms.espace_etudiant.dto.PromotionDto;
import inpg.sn.lms.espace_etudiant.dto.SupportDetailDto;
import inpg.sn.lms.espace_etudiant.dto.SupportDto;
import inpg.sn.lms.espace_etudiant.dto.SeanceDto;
import inpg.sn.lms.espace_etudiant.dto.SeanceRequest;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.Formation;
import inpg.sn.lms.espace_etudiant.entity.SupportCours;
import inpg.sn.lms.espace_etudiant.entity.Seance;
import inpg.sn.lms.espace_etudiant.service.CoursService;
import inpg.sn.lms.espace_etudiant.service.FormationService;
import inpg.sn.lms.espace_etudiant.service.PromotionService;
import inpg.sn.lms.espace_etudiant.service.SupportCoursStorageService;
import inpg.sn.lms.espace_etudiant.service.SupportService;
import inpg.sn.lms.espace_etudiant.service.SeanceService;

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
    private final SeanceService seanceService;
    private final SupportCoursStorageService supportCoursStorageService;
    private final Path uploadDirectory;

    public EspaceEtudiantController(
            UserRepository userRepository,
            EtudiantRepository etudiantRepository,
            FormateurRepository formateurRepository,
            FormationService formationService,
            CoursService coursService,
            PromotionService promotionService,
            SupportService supportService,
            SeanceService seanceService,
            SupportCoursStorageService supportCoursStorageService,
            @Value("${app.upload.dir:uploads}") String uploadDirectory) {
        this.userRepository = userRepository;
        this.etudiantRepository = etudiantRepository;
        this.formateurRepository = formateurRepository;
        this.formationService = formationService;
        this.coursService = coursService;
        this.promotionService = promotionService;
        this.supportService = supportService;
        this.seanceService = seanceService;
        this.supportCoursStorageService = supportCoursStorageService;
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    /**
     * Les favoris sont une association LAZY et open-in-view est désactivé : sans
     * transaction ouverte sur toute la méthode, l'entité User est détachée dès le
     * retour du repository et la lecture de la collection lève une
     * LazyInitializationException.
     */
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
                    "telephone", etudiant.getTelephone()));
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

    /**
     * Contenu d'un cours vu par l'étudiant : la fiche du cours et ses supports.
     * Les endpoints /formateur/cours/{id}/supports existants sont réservés au
     * propriétaire du cours et ne conviennent donc pas ici.
     */
    @GetMapping("/cours/{id}")
    public ResponseEntity<?> getCours(@PathVariable Long id) {
        return coursService.findById(id)
                .<ResponseEntity<?>>map(cours -> ResponseEntity.ok(toCoursDto(cours)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/cours/{id}/supports")
    public ResponseEntity<List<SupportDetailDto>> listSupportsOfCours(@PathVariable Long id) {
        if (coursService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(supportService.getCourseSupports(id).stream()
                .map(this::toSupportDetailDto)
                .toList());
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
    @GetMapping("/formateur/seances")
    public ResponseEntity<?> listMySessions(Authentication authentication) {
        Formateur formateur = findAuthenticatedFormateur(authentication);
        if (formateur == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Profil formateur introuvable."));
        return ResponseEntity.ok(seanceService.getByFormateur(formateur.getId()).stream().map(this::toSeanceDto).toList());
    }

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

    @DeleteMapping("/formateur/seances/{id}")
    public ResponseEntity<?> deleteSession(@PathVariable Long id, Authentication authentication) {
        Seance seance = findOwnedSession(id, authentication);
        if (seance == null) return ResponseEntity.notFound().build();
        seanceService.delete(seance);
        return ResponseEntity.noContent().build();
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

    @GetMapping("/formateur/cours/{id}")
    public ResponseEntity<?> getMyCourse(@PathVariable Long id, Authentication authentication) {
        Cours cours = findOwnedCourse(id, authentication);
        return cours == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(toCoursDto(cours));
    }

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
            return ResponseEntity.badRequest().body(Map.of("message", "Le volume horaire ne peut pas etre negatif."));
        }
        cours.setNom(request.code().trim());
        cours.setTitre(request.titre().trim());
        cours.setDescription(request.description() == null ? "" : request.description().trim());
        cours.setSemestre(request.semestre() == null ? "" : request.semestre().trim());
        cours.setVolumeHoraire(request.volumeHoraire() == null ? 0L : request.volumeHoraire());
        return ResponseEntity.ok(toCoursDto(coursService.save(cours)));
    }

    @DeleteMapping("/formateur/cours/{id}")
    @Transactional
    public ResponseEntity<?> deleteCourse(@PathVariable Long id, Authentication authentication) {
        Cours cours = findOwnedCourse(id, authentication);
        if (cours == null) {
            return ResponseEntity.notFound().build();
        }
        supportService.deleteCourseSupports(cours.getId());
        coursService.delete(cours);
        return ResponseEntity.noContent().build();
    }

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

    String contentType = image.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
        return ResponseEntity.badRequest().body(Map.of("message", "Le fichier doit être une image."));
    }

    String originalName = Path.of(image.getOriginalFilename() == null ? "image" : image.getOriginalFilename())
            .getFileName().toString();
    String storedFileName = UUID.randomUUID() + "-" + originalName;
    try {
        Files.createDirectories(uploadDirectory);
        Files.copy(image.getInputStream(), uploadDirectory.resolve(storedFileName), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "L'image n'a pas pu être enregistrée."));
    }

    cours.setImageUrl("/uploads/" + storedFileName);
    Cours sauvegarde = coursService.save(cours);
    return ResponseEntity.ok(toCoursDto(sauvegarde));
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
            DEFAULT_COURSE_COLOR,
            cours.getImageUrl());
}

    private SupportDto toSupportDto(SupportCours support) {
        return new SupportDto(
                support.getId().toString(),
                support.getTitre(),
                support.getUrl());
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
