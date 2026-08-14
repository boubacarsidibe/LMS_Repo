package inpg.sn.lms.espace_etudiant.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import inpg.sn.lms.espace_etudiant.entity.AlbumPhoto;
import inpg.sn.lms.espace_etudiant.entity.Photo;

/**
 * Stockage des clichés de la photothèque.
 *
 * <p>Deux précautions, absentes du stockage des supports de cours et pourtant
 * nécessaires ici : le nom d'origine est réduit à son dernier segment avant
 * d'être préfixé d'un UUID — un client peut soumettre {@code ../../etc/passwd},
 * et sans cela les {@code ..} annuleraient le préfixe et feraient sortir du
 * répertoire — et le type de média est vérifié contre une liste blanche, pour
 * qu'un exécutable ne soit pas déposé et re-servi sous couvert d'image.
 */
@Service
public class PhotoStorageService {

    private static final Set<String> TYPES_AUTORISES = Set.of(
            "image/jpeg", "image/pjpeg", "image/png", "image/webp", "image/gif");

    private final Path repertoire;

    public PhotoStorageService(@Value("${app.upload.dir:uploads}") String uploadDirectory) {
        this.repertoire = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    /** Écrit le fichier sur disque et construit la Photo associée (non persistée). */
    public Photo enregistrer(AlbumPhoto album, MultipartFile fichier, String legende) throws IOException {
        if (fichier == null || fichier.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide.");
        }
        String typeMedia = fichier.getContentType();
        if (typeMedia == null || !TYPES_AUTORISES.contains(typeMedia.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Format non pris en charge. Formats acceptés : JPEG, PNG, WEBP, GIF.");
        }

        String nomOriginal = nomDeFichier(fichier);
        String nomStocke = UUID.randomUUID() + "-" + nomOriginal;

        Files.createDirectories(repertoire);
        Path destination = repertoire.resolve(nomStocke).normalize();
        // Ceinture et bretelles : même après nettoyage du nom, on refuse toute
        // destination qui sortirait du répertoire d'upload.
        if (!destination.startsWith(repertoire)) {
            throw new IllegalArgumentException("Nom de fichier invalide.");
        }
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
