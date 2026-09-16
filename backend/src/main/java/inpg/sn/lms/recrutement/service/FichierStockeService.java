package inpg.sn.lms.recrutement.service;

import inpg.sn.lms.exception.ResourceNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import inpg.sn.lms.fichier.ValidationFichier;
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
        // Un dossier de candidature ne contient que des documents et des scans :
        // tout le reste est refusé, en particulier les formats exécutables par un
        // navigateur, qui seraient ensuite re-servis depuis l'origine du site.
        ValidationFichier.exiger(file,
                ValidationFichier.EXTENSIONS_DOCUMENT_CANDIDAT,
                ValidationFichier.TYPES_DOCUMENT_CANDIDAT,
                "PDF, JPEG, PNG, WEBP");

        String nomOriginal = ValidationFichier.nomSur(file, "document");
        String nomUnique = UUID.randomUUID().toString() + "_" + nomOriginal;
        Path cheminFichier = ValidationFichier.destinationSure(uploadDir, nomUnique);

        try {
            Files.copy(file.getInputStream(), cheminFichier, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Échec de l'enregistrement du fichier: " + nomOriginal, e);
        }

        // Le disque ne participe pas à la transaction SQL. Lorsque l'appelant
        // annule la création de la pièce, cette compensation supprime le fichier
        // qui aurait autrement survécu sans ligne de base associée.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                        supprimerSilencieusement(cheminFichier);
                    }
                }
            });
        }

        FichierStocke fichier = new FichierStocke();
        fichier.setNomOriginal(nomOriginal);
        fichier.setCheminStockage(cheminFichier.toString());
        fichier.setMimeType(file.getContentType());
        fichier.setTailleOctets(file.getSize());
        fichier.setDateUpload(LocalDateTime.now());

        try {
            return fichierStockeRepository.save(fichier);
        } catch (RuntimeException e) {
            supprimerSilencieusement(cheminFichier);
            throw e;
        }
    }

    public FichierStocke trouverParId(Long id) {
        return fichierStockeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fichier non trouvé avec l'ID: " + id));
    }

    public byte[] lireContenu(FichierStocke fichier) {
        try {
            return Files.readAllBytes(Paths.get(fichier.getCheminStockage()));
        } catch (IOException e) {
            throw new RuntimeException("Impossible de lire le fichier: " + fichier.getNomOriginal(), e);
        }
    }

    @Transactional
    public void supprimerFichier(Long id) {
        FichierStocke fichier = trouverParId(id);
        fichierStockeRepository.deleteById(id);

        Path chemin = Paths.get(fichier.getCheminStockage());
        // Supprimer le fichier seulement après un commit effectif : dans l'ordre
        // inverse, une erreur SQL laisserait une ligne pointant vers un fichier
        // déjà disparu. Un échec disque après commit produit au pire un orphelin,
        // récupérable par une tâche de réconciliation.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    supprimerSilencieusement(chemin);
                }
            });
        } else {
            supprimerSilencieusement(chemin);
        }
    }

    private void supprimerSilencieusement(Path chemin) {
        try {
            Files.deleteIfExists(chemin);
        } catch (IOException e) {
            // La suppression est rejouable par le contrôle d'exploitation ; elle
            // ne doit pas masquer l'erreur métier initiale.
        }
    }
}
