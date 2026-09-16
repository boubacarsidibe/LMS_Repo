package inpg.sn.lms.fichier;

import java.nio.file.Path;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

import org.springframework.web.multipart.MultipartFile;

/**
 * Contrôles communs à tous les dépôts de fichiers.
 *
 * <p>Le point important : <strong>le type MIME est déclaré par le client</strong>
 * ({@code Content-Type} de la partie multipart) et se falsifie donc trivialement.
 * Ce qui décide réellement de la façon dont un fichier sera re-servi, c'est son
 * <strong>extension</strong> : le gestionnaire de ressources statiques de Spring
 * déduit le {@code Content-Type} de réponse du nom du fichier sur disque. Un
 * {@code .html} déposé sous couvert de {@code application/pdf} serait donc bien
 * renvoyé en {@code text/html} et exécuté par le navigateur.
 *
 * <p>C'est pourquoi l'extension est vérifiée en premier et fait foi ; le type MIME
 * n'est qu'un contrôle secondaire, utile contre les erreurs de manipulation, pas
 * contre un attaquant.
 */
public final class ValidationFichier {

    private ValidationFichier() {
    }

    /**
     * Pièces justificatives d'un candidat : un dossier d'admission se compose de
     * documents et de scans, rien d'autre.
     */
    public static final Set<String> EXTENSIONS_DOCUMENT_CANDIDAT = Set.of(
            "pdf", "jpg", "jpeg", "png", "webp");

    public static final Set<String> TYPES_DOCUMENT_CANDIDAT = Set.of(
            "application/pdf", "image/jpeg", "image/pjpeg", "image/png", "image/webp");

    /**
     * Images réellement affichables. {@code svg} en est volontairement exclu : un
     * SVG est un document XML qui peut embarquer du script, et il passe pourtant
     * un contrôle naïf du genre {@code contentType.startsWith("image/")}.
     */
    public static final Set<String> EXTENSIONS_IMAGE = Set.of(
            "jpg", "jpeg", "png", "webp", "gif");

    public static final Set<String> TYPES_IMAGE = Set.of(
            "image/jpeg", "image/pjpeg", "image/png", "image/webp", "image/gif");

    /**
     * Supports de cours : bureautique, images, archives et vidéo. Volontairement
     * absents de cette liste — et c'est tout l'objet du contrôle — {@code html},
     * {@code htm}, {@code svg}, {@code js} et {@code xhtml}, qui s'exécutent dans
     * le navigateur et permettraient un script persistant servi depuis l'origine
     * de l'application.
     */
    public static final Set<String> EXTENSIONS_SUPPORT_COURS = Set.of(
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "odt", "odp", "ods",
            "txt", "csv", "jpg", "jpeg", "png", "webp", "gif", "zip", "mp4", "mp3");

    public static final Set<String> TYPES_SUPPORT_COURS = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.oasis.opendocument.text",
            "application/vnd.oasis.opendocument.presentation",
            "application/vnd.oasis.opendocument.spreadsheet",
            "text/plain", "text/csv",
            "image/jpeg", "image/pjpeg", "image/png", "image/webp", "image/gif",
            "application/zip", "application/x-zip-compressed",
            "video/mp4", "audio/mpeg",
            "application/octet-stream");

    /**
     * Refuse un fichier vide, une extension hors liste blanche, ou un type MIME
     * hors liste blanche quand il est renseigné.
     *
     * @param formatsLisibles formats à citer dans le message d'erreur montré à
     *                        l'utilisateur, ex. « PDF, Word, PowerPoint, images »
     * @throws IllegalArgumentException si le fichier est refusé
     */
    public static void exiger(MultipartFile fichier, Set<String> extensionsAutorisees,
            Set<String> typesAutorises, String formatsLisibles) {
        if (fichier == null || fichier.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide.");
        }

        String extension = extensionDe(nomSur(fichier, ""));
        if (extension.isEmpty() || !extensionsAutorisees.contains(extension)) {
            throw new IllegalArgumentException(
                    "Format de fichier non autorisé. Formats acceptés : " + formatsLisibles + ".");
        }

        // Contrôle secondaire : un type MIME absent est toléré (certains clients ne
        // l'envoient pas), un type MIME présent mais hors liste est refusé.
        String type = fichier.getContentType();
        if (type != null && !type.isBlank()) {
            String typeNormalise = type.toLowerCase(Locale.ROOT).split(";")[0].trim();
            if (!typesAutorises.contains(typeNormalise)) {
                throw new IllegalArgumentException(
                        "Format de fichier non autorisé. Formats acceptés : " + formatsLisibles + ".");
            }
        }

        // Les pièces d'admission sont les fichiers les plus exposés à un contenu
        // renommé. On vérifie leur signature binaire, sans prétendre remplacer un
        // antivirus ou un service de désarmement documentaire.
        if (extensionsAutorisees.equals(EXTENSIONS_DOCUMENT_CANDIDAT)
                && !contenuCorrespondALaSignature(fichier, extension)) {
            throw new IllegalArgumentException("Le contenu du fichier ne correspond pas au format annoncé.");
        }
    }

    /**
     * Ne conserve que le dernier segment du nom soumis : un client peut envoyer
     * {@code ../../etc/passwd}, et les {@code ..} annuleraient le préfixe UUID
     * ajouté ensuite.
     */
    public static String nomSur(MultipartFile fichier, String defaut) {
        String original = fichier.getOriginalFilename();
        if (original == null || original.isBlank()) {
            return defaut;
        }
        Path nom = Path.of(original.replace('\\', '/')).getFileName();
        return nom == null || nom.toString().isBlank() ? defaut : nom.toString();
    }

    /**
     * Résout {@code nom} dans {@code repertoire} en garantissant que le résultat y
     * reste. Ceinture et bretelles : le nom a déjà été réduit à son dernier segment.
     *
     * @throws IllegalArgumentException si la destination sortirait du répertoire
     */
    public static Path destinationSure(Path repertoire, String nom) {
        Path destination = repertoire.resolve(nom).normalize();
        if (!destination.startsWith(repertoire)) {
            throw new IllegalArgumentException("Nom de fichier invalide.");
        }
        return destination;
    }

    /** Extension en minuscules, sans le point, ou chaîne vide s'il n'y en a pas. */
    private static String extensionDe(String nom) {
        int point = nom.lastIndexOf('.');
        if (point < 0 || point == nom.length() - 1) {
            return "";
        }
        return nom.substring(point + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean contenuCorrespondALaSignature(MultipartFile fichier, String extension) {
        byte[] entete = new byte[16];
        int lus;
        try (InputStream contenu = fichier.getInputStream()) {
            lus = contenu.read(entete);
        } catch (IOException e) {
            throw new IllegalArgumentException("Le fichier ne peut pas être lu.", e);
        }
        if (lus < 4) {
            return false;
        }
        return switch (extension) {
            case "pdf" -> commencePar(entete, lus, "%PDF-".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            case "jpg", "jpeg" -> commencePar(entete, lus, new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF });
            case "png" -> commencePar(entete, lus,
                    new byte[] { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', (byte) 0x1A, '\n' });
            case "gif" -> commencePar(entete, lus, "GIF87a".getBytes(java.nio.charset.StandardCharsets.US_ASCII))
                    || commencePar(entete, lus, "GIF89a".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            case "webp" -> commencePar(entete, lus, "RIFF".getBytes(java.nio.charset.StandardCharsets.US_ASCII))
                    && lus >= 12 && entete[8] == 'W' && entete[9] == 'E' && entete[10] == 'B' && entete[11] == 'P';
            default -> false;
        };
    }

    private static boolean commencePar(byte[] contenu, int taille, byte[] signature) {
        if (taille < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (contenu[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
