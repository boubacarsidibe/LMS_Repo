package inpg.sn.lms.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Régression : les photos de démonstration étaient enregistrées en base avec leur seul nom de fichier
 * (« demo-photo-ds.png ») alors que le fichier vit dans « uploads/photos/ » et que le service de lecture résout le
 * chemin depuis « uploads/ ». Résultat : 404 sur chaque photo, et une photothèque d'images cassées.
 */
class DataInitializerPhotoDemoTest {

    @TempDir
    Path uploads;

    @Test
    @DisplayName("Le chemin renvoyé, résolu depuis la racine des dépôts, désigne un fichier qui existe")
    void cheminEnregistreDesigneLeFichierDepose() throws IOException {
        String chemin = DataInitializer.deposerPhotoDemo(uploads, "demo-photo-ds.png", new byte[] {1, 2, 3});

        // C'est exactement la résolution de PhotoStorageService : repertoire.resolve(fichierStocke)
        assertThat(uploads.resolve(chemin)).exists().hasBinaryContent(new byte[] {1, 2, 3});
        assertThat(chemin).isEqualTo("photos/demo-photo-ds.png");
    }

    @Test
    @DisplayName("Le sous-dossier « photos » est créé au besoin, et un fichier déjà présent n'est pas écrasé")
    void creeLeSousDossierEtNEcrasePasUnFichierExistant() throws IOException {
        DataInitializer.deposerPhotoDemo(uploads, "demo-photo-fin.png", new byte[] {9});
        DataInitializer.deposerPhotoDemo(uploads, "demo-photo-fin.png", new byte[] {7, 7});

        assertThat(Files.readAllBytes(uploads.resolve("photos/demo-photo-fin.png"))).containsExactly(9);
    }
}
