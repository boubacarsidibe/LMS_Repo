package inpg.sn.lms.espace_etudiant.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.espace_etudiant.dto.AlbumCreateRequest;
import inpg.sn.lms.espace_etudiant.dto.AlbumPhotoDto;
import inpg.sn.lms.espace_etudiant.dto.PhotoDto;
import inpg.sn.lms.espace_etudiant.entity.AlbumPhoto;
import inpg.sn.lms.espace_etudiant.entity.Photo;
import inpg.sn.lms.espace_etudiant.entity.Promotion;
import inpg.sn.lms.espace_etudiant.repository.AlbumPhotoRepository;
import inpg.sn.lms.espace_etudiant.repository.PhotoRepository;
import inpg.sn.lms.espace_etudiant.repository.PromotionRepository;
import inpg.sn.lms.espace_etudiant.service.PhotoStorageService;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;

/**
 * Photothèque de l'institut (TDR §3.2) : albums d'événements, sorties et visites.
 *
 * <p>Un album rattaché à une promotion n'est visible que de ses étudiants ; sans
 * promotion, il est institutionnel et visible de tous. La consultation vit sous
 * {@code /photheque/**} (étudiants et formateurs) et l'alimentation sous
 * {@code /formateur/**}, déjà réservé au rôle FORMATEUR par SecurityConfig.
 */
@RestController
@RequestMapping("/api/espace-etudiant")
/*
 * Autorisation rendue explicite : elle ne reposait que sur le prefixe d'URL filtre
 * dans SecurityConfig. Un endpoint ajoute hors de ce prefixe aurait ete ouvert sans
 * que rien ne le signale. Les regles d'URL restent en place et s'appliquent en plus.
 */
@PreAuthorize("hasAnyRole('ETUDIANT','FORMATEUR')")
public class PhotothequeController {

    private final AlbumPhotoRepository albumRepository;
    private final PhotoRepository photoRepository;
    private final PromotionRepository promotionRepository;
    private final PhotoStorageService photoStorage;
    private final UtilisateurCourantService utilisateurCourant;

    public PhotothequeController(AlbumPhotoRepository albumRepository, PhotoRepository photoRepository,
            PromotionRepository promotionRepository, PhotoStorageService photoStorage,
            UtilisateurCourantService utilisateurCourant) {
        this.albumRepository = albumRepository;
        this.photoRepository = photoRepository;
        this.promotionRepository = promotionRepository;
        this.photoStorage = photoStorage;
        this.utilisateurCourant = utilisateurCourant;
    }

    /** Albums visibles par le connecté. */
    @GetMapping("/photheque/albums")
    @Transactional(readOnly = true)
    public ResponseEntity<?> albums(Authentication authentication) {
        return ResponseEntity.ok(albumsVisibles(authentication).stream().map(this::toAlbumDto).toList());
    }

    /** Contenu d'un album. */
    @GetMapping("/photheque/albums/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> album(@PathVariable Long id, Authentication authentication) {
        AlbumPhoto album = albumRepository.findById(id).orElse(null);
        if (album == null) {
            return ResponseEntity.notFound().build();
        }
        if (!estVisible(album, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Cet album ne concerne pas votre promotion."));
        }
        List<PhotoDto> photos = photoRepository.findByAlbumIdOrderByDateAjoutAsc(id).stream()
                .map(this::toPhotoDto)
                .toList();
        return ResponseEntity.ok(Map.of("album", toAlbumDto(album), "photos", photos));
    }

    /** Sert le fichier d'un cliché. */
    @GetMapping("/photheque/photos/{id}/fichier")
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> fichier(@PathVariable Long id, Authentication authentication) {
        Photo photo = photoRepository.findById(id).orElse(null);
        if (photo == null) {
            return ResponseEntity.notFound().build();
        }
        if (!estVisible(photo.getAlbum(), authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Path chemin = photoStorage.cheminDe(photo);
        if (chemin == null || !Files.exists(chemin)) {
            return ResponseEntity.notFound().build();
        }
        MediaType type = MediaType.APPLICATION_OCTET_STREAM;
        try {
            String detecte = Files.probeContentType(chemin);
            if (detecte != null) {
                type = MediaType.parseMediaType(detecte);
            }
        } catch (IOException | org.springframework.http.InvalidMediaTypeException ignore) {
            // Type indéterminé : le flux binaire générique fait l'affaire.
        }
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(photo.getNomOriginal()).toString())
                .body(new FileSystemResource(chemin));
    }

    /** Crée un album. */
    @PostMapping("/formateur/albums")
    @Transactional
    public ResponseEntity<?> creerAlbum(@RequestBody AlbumCreateRequest request) {
        if (request.titre() == null || request.titre().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le titre de l'album est obligatoire."));
        }
        Promotion promotion = null;
        if (request.promotionId() != null) {
            promotion = promotionRepository.findById(request.promotionId()).orElse(null);
            if (promotion == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Promotion introuvable."));
            }
        }
        AlbumPhoto album = albumRepository.save(new AlbumPhoto(
                request.titre().trim(),
                request.description(),
                promotion,
                request.dateEvenement()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toAlbumDto(album));
    }

    /** Ajoute un cliché à un album. */
    @PostMapping(value = "/formateur/albums/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<?> ajouterPhoto(@PathVariable Long id,
            @RequestParam("fichier") MultipartFile fichier,
            @RequestParam(value = "legende", required = false) String legende) {
        AlbumPhoto album = albumRepository.findById(id).orElse(null);
        if (album == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            Photo photo = photoRepository.save(photoStorage.enregistrer(album, fichier, legende));
            return ResponseEntity.status(HttpStatus.CREATED).body(toPhotoDto(photo));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "L'enregistrement du fichier a échoué."));
        }
    }

    // ===================== Visibilité =====================

    private List<AlbumPhoto> albumsVisibles(Authentication authentication) {
        Etudiant etudiant = utilisateurCourant.etudiant(authentication);
        if (etudiant == null) {
            return albumRepository.findAll();
        }
        Promotion promotion = etudiant.getPromotion();
        return promotion == null
                ? albumRepository.findGeneraux()
                : albumRepository.findVisiblesPour(promotion.getId());
    }

    private boolean estVisible(AlbumPhoto album, Authentication authentication) {
        if (album == null) {
            return false;
        }
        if (album.getPromotion() == null) {
            return true;
        }
        Etudiant etudiant = utilisateurCourant.etudiant(authentication);
        if (etudiant == null) {
            return true;
        }
        Promotion promotion = etudiant.getPromotion();
        return promotion != null && promotion.getId().equals(album.getPromotion().getId());
    }

    // ===================== Correspondances =====================

    private AlbumPhotoDto toAlbumDto(AlbumPhoto album) {
        List<Photo> photos = photoRepository.findByAlbumIdOrderByDateAjoutAsc(album.getId());
        return new AlbumPhotoDto(
                album.getId(),
                album.getTitre(),
                album.getDescription(),
                album.getDateEvenement(),
                album.getPromotion() == null ? null : album.getPromotion().getLibelle(),
                photos.size(),
                photos.isEmpty() ? null : urlDe(photos.get(0)));
    }

    private PhotoDto toPhotoDto(Photo photo) {
        return new PhotoDto(photo.getId(), urlDe(photo), photo.getLegende(), photo.getDateAjout());
    }

    private String urlDe(Photo photo) {
        return "/api/espace-etudiant/photheque/photos/" + photo.getId() + "/fichier";
    }
}
