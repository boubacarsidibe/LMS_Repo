package inpg.sn.lms.espace_etudiant.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import inpg.sn.lms.espace_etudiant.entity.AlbumPhoto;
import inpg.sn.lms.espace_etudiant.entity.Photo;
import inpg.sn.lms.fichier.ValidationFichier;

/**
 * Stockage des clichés de la photothèque.
 *
 * <p>Les contrôles (nom réduit à son dernier segment, extension et type vérifiés
 * contre une liste blanche, destination confinée au répertoire) sont désormais
 * mutualisés dans {@link ValidationFichier} et appliqués à tous les dépôts de
 * fichiers du projet.
 */
@Service
public class PhotoStorageService {


    private final Path repertoire;

    public PhotoStorageService(@Value("${app.upload.dir:uploads}") String uploadDirectory) {
        this.repertoire = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    /** Écrit le fichier sur disque et construit la Photo associée (non persistée). */
    public Photo enregistrer(AlbumPhoto album, MultipartFile fichier, String legende) throws IOException {
        // Le contrôle du seul type déclaré ne suffisait pas : c'est l'extension qui
        // détermine le Content-Type renvoyé quand le cliché est re-servi.
        ValidationFichier.exiger(fichier,
                ValidationFichier.EXTENSIONS_IMAGE,
                ValidationFichier.TYPES_IMAGE,
                "JPEG, PNG, WEBP, GIF");

        String nomOriginal = nomDeFichier(fichier);
        String nomStocke = UUID.randomUUID() + "-" + nomOriginal;

        Files.createDirectories(repertoire);
        Path destination = ValidationFichier.destinationSure(repertoire, nomStocke);
        try (InputStream flux = fichier.getInputStream()) {
            Files.copy(flux, destination, StandardCopyOption.REPLACE_EXISTING);
        }
        return new Photo(album, nomStocke, nomOriginal, legende);
    }

    /** Chemin sur disque d'un cliché, ou {@code null} s'il sort du répertoire. */
    public Path cheminDe(Photo photo) {
        Path chemin = repertoire.resolve(photo.getFichierStocke()).normalize();
        return chemin.startsWith(repertoire) ? chemin : null;
    }

    /** Ne conserve que le dernier segment du nom soumis par le client. */
    private String nomDeFichier(MultipartFile fichier) {
        String original = fichier.getOriginalFilename();
        if (original == null || original.isBlank()) {
            return "photo";
        }
        Path nom = Path.of(original.replace('\\', '/')).getFileName();
        return nom == null ? "photo" : nom.toString();
    }
}
