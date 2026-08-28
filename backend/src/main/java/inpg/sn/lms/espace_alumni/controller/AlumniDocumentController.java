package inpg.sn.lms.espace_alumni.controller;

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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.espace_alumni.dto.DocumentPartageDto;
import inpg.sn.lms.espace_alumni.entity.ConsultationDocumentPartage;
import inpg.sn.lms.espace_alumni.entity.DocumentPartage;
import inpg.sn.lms.espace_alumni.repository.ConsultationDocumentPartageRepository;
import inpg.sn.lms.espace_alumni.repository.DocumentPartageRepository;
import inpg.sn.lms.espace_alumni.service.AlumniDocumentStorageService;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;

/**
 * Partage de documents entre alumni (TDR §C), migration V14.
 *
 * <p>Réutilise le mécanisme de dépôt de fichiers déjà éprouvé pour la photothèque
 * et les pièces jointes du forum ({@link AlumniDocumentStorageService}, patron de
 * {@code PhotoStorageService}) plutôt que d'en inventer un nouveau.
 *
 * <p>Vit sous {@code /api/espace-alumni/**}, déjà protégé par {@code hasRole
 * ('ALUMNI')} dans {@code SecurityConfig} ; double protection par
 * {@code @PreAuthorize} de classe, même patron que {@code AlumniController}.
 */
@RestController
@RequestMapping("/api/espace-alumni/documents")
@PreAuthorize("hasRole('ALUMNI')")
public class AlumniDocumentController {

    private final DocumentPartageRepository documentRepository;
    private final AlumniDocumentStorageService storage;
    private final UtilisateurCourantService utilisateurCourant;
    private final ConsultationDocumentPartageRepository consultationRepository;

    public AlumniDocumentController(DocumentPartageRepository documentRepository,
            AlumniDocumentStorageService storage, UtilisateurCourantService utilisateurCourant,
            ConsultationDocumentPartageRepository consultationRepository) {
        this.documentRepository = documentRepository;
        this.storage = storage;
        this.utilisateurCourant = utilisateurCourant;
        this.consultationRepository = consultationRepository;
    }

    /** Documents partagés, les plus récents en tête — visibles de tous les alumni. */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> lister(Authentication authentication) {
        User utilisateur = utilisateurCourant.utilisateur(authentication);
        List<DocumentPartageDto> documents = documentRepository
                .findAllByOrderByDateAjoutDesc().stream()
                .map(document -> toDto(document, utilisateur))
                .toList();
        return ResponseEntity.ok(documents);
    }

    /** Dépose un nouveau document. */
    @PostMapping
    @Transactional
    public ResponseEntity<?> ajouter(
            @RequestParam String titre,
            @RequestParam(required = false) String description,
            @RequestParam("fichier") MultipartFile fichier,
            Authentication authentication) {
        User auteur = utilisateurCourant.utilisateur(authentication);
        if (auteur == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (estVide(titre)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le titre du document est obligatoire."));
        }
        DocumentPartage document;
        try {
            document = storage.enregistrer(auteur, fichier, titre.trim(),
                    description == null || description.isBlank() ? null : description.trim());
        } catch (IllegalArgumentException invalide) {
            return ResponseEntity.badRequest().body(Map.of("message", invalide.getMessage()));
        } catch (IOException erreur) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Le fichier n'a pas pu être enregistré."));
        }
        documentRepository.save(document);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(document, auteur));
    }

    /**
     * Télécharge un document — toujours en pièce jointe, quel que soit son type.
     *
     * <p>C'est le seul geste qui prouve qu'un document a vraiment été ouvert : à la
     * différence de {@code ProgressionSupport} (support de cours), il n'y a pas de
     * bouton « marquer consulté » séparé côté client, l'ouverture du fichier fait
     * foi. Idempotent, même patron que {@code marquerConsulte} de
     * {@code ScolariteController}.
     */
    @GetMapping("/{id}/fichier")
    @Transactional
    public ResponseEntity<Resource> telecharger(@PathVariable Long id, Authentication authentication) {
        DocumentPartage document = documentRepository.findById(id).orElse(null);
        if (document == null) {
            return ResponseEntity.notFound().build();
        }
        Path chemin = storage.cheminDe(document);
        if (chemin == null || !Files.isRegularFile(chemin)) {
            return ResponseEntity.notFound().build();
        }
        User lecteur = utilisateurCourant.utilisateur(authentication);
        if (lecteur != null && !consultationRepository.existsByDocumentIdAndLecteurId(id, lecteur.getId())) {
            consultationRepository.save(new ConsultationDocumentPartage(document, lecteur));
        }
        MediaType type = document.getTypeContenu() != null
                ? MediaType.parseMediaType(document.getTypeContenu())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(document.getNomFichierOriginal()).build().toString())
                .body(new FileSystemResource(chemin));
    }

    /** Supprime son propre document. */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> supprimer(@PathVariable Long id, Authentication authentication) {
        User utilisateur = utilisateurCourant.utilisateur(authentication);
        DocumentPartage document = documentRepository.findById(id).orElse(null);
        if (document == null) {
            return ResponseEntity.notFound().build();
        }
        if (utilisateur == null || document.getAuteur() == null
                || !document.getAuteur().getId().equals(utilisateur.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Vous ne pouvez supprimer que vos propres documents."));
        }
        documentRepository.delete(document);
        return ResponseEntity.noContent().build();
    }

    // ===================== Correspondances =====================

    private DocumentPartageDto toDto(DocumentPartage document, User lecteur) {
        boolean estAuteur = lecteur != null && document.getAuteur() != null
                && document.getAuteur().getId().equals(lecteur.getId());
        return new DocumentPartageDto(
                document.getId(),
                document.getTitre(),
                document.getDescription(),
                utilisateurCourant.nomAffiche(document.getAuteur()),
                document.getNomFichierOriginal(),
                document.getDateAjout(),
                estAuteur);
    }

    private boolean estVide(String valeur) {
        return valeur == null || valeur.isBlank();
    }
}
