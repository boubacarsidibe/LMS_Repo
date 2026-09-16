package inpg.sn.lms.espace_etudiant.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.SupportCours;
import inpg.sn.lms.fichier.ValidationFichier;

/**
 * Écriture d'un support de cours sur le disque et création de l'entité
 * associée. Extrait du contrôleur pour être partagé par les deux profils
 * autorisés à publier : le formateur sur ses propres cours, le responsable
 * pédagogique sur n'importe quel cours. L'autorisation reste à l'appelant.
 */
@Service
public class SupportCoursStorageService {

    private final SupportService supportService;
    private final Path uploadDirectory;

    public SupportCoursStorageService(SupportService supportService,
            @Value("${app.upload.dir:uploads}") String uploadDirectory) {
        this.supportService = supportService;
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    /**
     * Enregistre le fichier puis le support. L'URL de téléchargement contient
     * l'identifiant généré : le support est donc sauvegardé une première fois
     * pour obtenir cet identifiant, puis complété.
     */
    public SupportCours publier(Cours cours, MultipartFile fichier, String typeRessource, LocalDate datePublication)
            throws IOException {
        ValidationFichier.exiger(fichier,
                ValidationFichier.EXTENSIONS_SUPPORT_COURS,
                ValidationFichier.TYPES_SUPPORT_COURS,
                "PDF, Word, PowerPoint, Excel, images, archives ZIP, audio et vidéo");

        String nomOriginal = nomDeFichier(fichier);
        String nomStocke = UUID.randomUUID() + "-" + nomOriginal;

        Files.createDirectories(uploadDirectory);
        Path destination = ValidationFichier.destinationSure(uploadDirectory, nomStocke);
        Files.copy(fichier.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

        SupportCours support = new SupportCours();
        support.setCours(cours);
        support.setTitre(nomOriginal);
        support.setTypeRessource(typeRessource);
        support.setFichierStocke(nomStocke);
        support.setDatePublication(datePublication);

        SupportCours enregistre = supportService.saveCourseSupport(support);
        enregistre.setUrl("/api/espace-etudiant/supports/cours/" + enregistre.getId() + "/download");
        return supportService.saveCourseSupport(enregistre);
    }

    /** Enregistre une ressource web : elle ne possède volontairement aucun fichier local. */
    public SupportCours publierLien(Cours cours, String titre, String url, LocalDate datePublication) {
        SupportCours support = new SupportCours();
        support.setCours(cours);
        support.setTitre(titre);
        support.setTypeRessource("lien");
        support.setUrl(url);
        support.setDatePublication(datePublication);
        return supportService.saveCourseSupport(support);
    }

    /** Ne conserve que le nom du fichier : un chemin fourni par le client ne doit pas être suivi. */
    private String nomDeFichier(MultipartFile fichier) {
        String original = fichier.getOriginalFilename() == null ? "ressource" : fichier.getOriginalFilename();
        String nom = Path.of(original).getFileName().toString();
        return nom.isBlank() ? "ressource" : nom;
    }
}
