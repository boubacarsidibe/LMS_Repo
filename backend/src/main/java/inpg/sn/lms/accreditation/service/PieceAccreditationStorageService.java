package inpg.sn.lms.accreditation.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import inpg.sn.lms.fichier.ValidationFichier;
import inpg.sn.lms.recrutement.entity.FichierStocke;

/**
 * Stockage des pièces d'un dossier d'accréditation, même patron que
 * {@code AlumniDocumentStorageService}.
 *
 * <p>Réutilise l'entité {@link FichierStocke} (module recrutement, déjà
 * réutilisée par {@code PieceJustificative}) plutôt qu'un nouveau type
 * d'enregistrement de fichier — mais pas {@code FichierStockeService} : celui-ci
 * valide contre {@code EXTENSIONS_DOCUMENT_CANDIDAT} (PDF/JPEG/PNG/WEBP
 * seulement), trop étroit pour un curriculum ou une politique qualité, souvent
 * fournis en Word/PowerPoint/Excel. {@code EXTENSIONS_SUPPORT_COURS} (déjà
 * utilisée pour les supports de cours) convient mieux à ces documents
 * institutionnels.
 *
 * <p>Rangées dans un sous-dossier dédié ({@code accreditation}), à l'écart de
 * {@code public/} (seul sous-dossier servi sans authentification par
 * {@code WebConfig}) — le téléchargement passe par un endpoint authentifié.
 */
@Service
public class PieceAccreditationStorageService {

    private final Path repertoire;

    public PieceAccreditationStorageService(@Value("${app.upload.dir:uploads}") String uploadDirectory) {
        this.repertoire = Path.of(uploadDirectory).toAbsolutePath().normalize().resolve("accreditation");
    }

    /** Écrit le fichier sur disque et construit le FichierStocke associé (non persisté). */
    public FichierStocke enregistrer(MultipartFile fichier) throws IOException {
        ValidationFichier.exiger(fichier,
                ValidationFichier.EXTENSIONS_SUPPORT_COURS,
                ValidationFichier.TYPES_SUPPORT_COURS,
                "PDF, Word, PowerPoint, Excel, images");

        String nomOriginal = ValidationFichier.nomSur(fichier, "document");
        String nomStocke = UUID.randomUUID() + "-" + nomOriginal;

        Files.createDirectories(repertoire);
        Path destination = ValidationFichier.destinationSure(repertoire, nomStocke);
        try (InputStream flux = fichier.getInputStream()) {
            Files.copy(flux, destination, StandardCopyOption.REPLACE_EXISTING);
        }

        FichierStocke fichierStocke = new FichierStocke();
        fichierStocke.setNomOriginal(nomOriginal);
        fichierStocke.setCheminStockage(destination.toString());
        fichierStocke.setMimeType(fichier.getContentType());
        fichierStocke.setTailleOctets(fichier.getSize());
        fichierStocke.setDateUpload(LocalDateTime.now());
        return fichierStocke;
    }

    /** Chemin sur disque d'un fichier stocké, ou {@code null} s'il sort du répertoire. */
    public Path cheminDe(FichierStocke fichierStocke) {
        Path chemin = Path.of(fichierStocke.getCheminStockage()).normalize();
        return chemin.startsWith(repertoire) ? chemin : null;
    }
}
