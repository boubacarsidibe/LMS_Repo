package inpg.sn.lms.fichier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

/**
 * Dépôt et service d'une pièce jointe de message (forum formateur, forum étudiant,
 * messagerie privée) : validation, nommage préfixé d'UUID et protection contre la
 * traversée de chemin sont communs aux trois contrôleurs qui en ont besoin — les
 * dupliquer trois fois aurait fini par en faire diverger un des trois, sur du code
 * qui touche justement à la sécurité (extensions autorisées, chemin de destination).
 */
public final class PieceJointeStorage {

    private PieceJointeStorage() {
    }

    public record Enregistree(String nomOriginal, String nomStocke, String typeContenu) {
    }

    /**
     * Valide puis enregistre {@code fichier} sous {@code uploadDirectory/sousDossier}.
     *
     * @throws IllegalArgumentException si le fichier est refusé (format, taille…)
     * @throws IOException              si l'écriture sur disque échoue
     */
    public static Enregistree enregistrer(MultipartFile fichier, Path uploadDirectory, String sousDossier)
            throws IOException {
        ValidationFichier.exiger(fichier,
                ValidationFichier.EXTENSIONS_SUPPORT_COURS,
                ValidationFichier.TYPES_SUPPORT_COURS,
                "PDF, Word, PowerPoint, Excel, images, archives ZIP, audio et vidéo");

        String nomOriginal = ValidationFichier.nomSur(fichier, "fichier");
        String nomStocke = UUID.randomUUID() + "-" + nomOriginal;
        Path dossier = uploadDirectory.resolve(sousDossier);
        Files.createDirectories(dossier);
        Files.copy(fichier.getInputStream(),
                ValidationFichier.destinationSure(dossier, nomStocke),
                StandardCopyOption.REPLACE_EXISTING);
        return new Enregistree(nomOriginal, nomStocke, fichier.getContentType());
    }

    /** Sert en téléchargement le fichier déposé par {@link #enregistrer}, ou 404 s'il est introuvable. */
    public static ResponseEntity<Resource> servir(Path uploadDirectory, String sousDossier,
            String nomStocke, String nomOriginal, String typeContenu) {
        if (nomStocke == null) {
            return ResponseEntity.notFound().build();
        }
        Path filePath = uploadDirectory.resolve(sousDossier).resolve(nomStocke).normalize();
        if (!filePath.startsWith(uploadDirectory) || !Files.isRegularFile(filePath)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(filePath);
        MediaType mediaType = typeContenu != null
                ? MediaType.parseMediaType(typeContenu)
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nomOriginal).build().toString())
                .body(resource);
    }
}
