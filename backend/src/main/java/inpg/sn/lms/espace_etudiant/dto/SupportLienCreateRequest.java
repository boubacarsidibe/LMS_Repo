package inpg.sn.lms.espace_etudiant.dto;

/** Création d'une ressource externe, sans dépôt de fichier sur le serveur. */
public record SupportLienCreateRequest(
        Long coursId,
        String titre,
        String url,
        String datePublication) {
}
