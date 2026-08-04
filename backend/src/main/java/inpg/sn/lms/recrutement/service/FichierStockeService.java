package inpg.sn.lms.recrutement.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import inpg.sn.lms.recrutement.entity.FichierStocke;
import inpg.sn.lms.recrutement.repository.FichierStockeRepository;

@Service
public class FichierStockeService {

    private final FichierStockeRepository fichierStockeRepository;
    private final Path uploadDir;

    public FichierStockeService(FichierStockeRepository fichierStockeRepository,
            @Value("${app.upload.dir:uploads}") String uploadDirPath) {
        this.fichierStockeRepository = fichierStockeRepository;
        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de créer le dossier d'upload: " + uploadDirPath, e);
        }
    }

    public FichierStocke enregistrerFichier(MultipartFile file) {
        String nomUnique = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path cheminFichier = uploadDir.resolve(nomUnique);

        try {
            Files.copy(file.getInputStream(), cheminFichier, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Échec de l'enregistrement du fichier: " + file.getOriginalFilename(), e);
        }

        FichierStocke fichier = new FichierStocke();
        fichier.setNomOriginal(file.getOriginalFilename());
        fichier.setCheminStockage(cheminFichier.toString());
        fichier.setMimeType(file.getContentType());
        fichier.setTailleOctets(file.getSize());
        fichier.setDateUpload(LocalDateTime.now());

        return fichierStockeRepository.save(fichier);
    }

    public FichierStocke trouverParId(Long id) {
        return fichierStockeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fichier non trouvé avec l'ID: " + id));
    }

    public void supprimerFichier(Long id) {
        FichierStocke fichier = trouverParId(id);
        try {
            Files.deleteIfExists(Paths.get(fichier.getCheminStockage()));
        } catch (IOException e) {
            // Log l'erreur mais ne pas bloquer
        }
        fichierStockeRepository.deleteById(id);
    }
}