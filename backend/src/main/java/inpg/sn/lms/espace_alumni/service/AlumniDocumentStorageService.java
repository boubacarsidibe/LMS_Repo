package inpg.sn.lms.espace_alumni.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import inpg.sn.lms.espace_alumni.entity.DocumentPartage;
import inpg.sn.lms.fichier.ValidationFichier;

/**
 * Stockage des documents partagés dans l'espace Alumni, même patron que
 * {@code PhotoStorageService}.
 *
 * <p>Rangés dans un sous-dossier dédié ({@code alumni-documents}), à l'écart de
 * {@code public/} (seul sous-dossier servi sans authentification par
 * {@code WebConfig}) — exactement comme les pièces jointes du forum, servies par
 * leur propre endpoint authentifié plutôt que statiquement.
 */
@Service
public class AlumniDocumentStorageService {

    private final Path repertoire;

    public AlumniDocumentStorageService(@Value("${app.upload.dir:uploads}") String uploadDirectory) {
        this.repertoire = Path.of(uploadDirectory).toAbsolutePath().normalize().resolve("alumni-documents");
    }

    /** Écrit le fichier sur disque et construit le DocumentPartage (non persisté). */
    public DocumentPartage enregistrer(inpg.sn.lms.auth.entity.User auteur, MultipartFile fichier, String titre,
            String description) throws IOException {
        ValidationFichier.exiger(fichier,
                ValidationFichier.EXTENSIONS_SUPPORT_COURS,
                ValidationFichier.TYPES_SUPPORT_COURS,
                "PDF, Word, PowerPoint, Excel, images, archives ZIP, audio et vidéo");

        String nomOriginal = ValidationFichier.nomSur(fichier, "document");
        String nomStocke = UUID.randomUUID() + "-" + nomOriginal;

        Files.createDirectories(repertoire);
        Path destination = ValidationFichier.destinationSure(repertoire, nomStocke);
        try (InputStream flux = fichier.getInputStream()) {
            Files.copy(flux, destination, StandardCopyOption.REPLACE_EXISTING);
        }
        return new DocumentPartage(auteur, titre, description, nomOriginal, nomStocke, fichier.getContentType());
    }

    /** Chemin sur disque d'un document, ou {@code null} s'il sort du répertoire. */
    public Path cheminDe(DocumentPartage document) {
        Path chemin = repertoire.resolve(document.getNomFichierStocke()).normalize();
        return chemin.startsWith(repertoire) ? chemin : null;
    }
}
