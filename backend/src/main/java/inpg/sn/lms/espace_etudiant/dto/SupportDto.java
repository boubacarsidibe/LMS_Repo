package inpg.sn.lms.espace_etudiant.dto;

/**
 * @param typeRessource    pdf, lien, video... (sert a choisir l'icone cote interface)
 * @param datePublication  date a partir de laquelle la ressource est visible des etudiants
 * @param programme        vrai si cette date est encore dans le futur (ressource pas
 *                         encore diffusee ; seuls les formateurs du cours la voient)
 * @param coursId          cours auquel la ressource est rattachee (null pour un support pedagogique)
 * @param coursCode        code du cours, pour l'affichage
 */
public record SupportDto(
        String id,
        String titre,
        String url,
        String typeRessource,
        String datePublication,
        boolean programme,
        Long coursId,
        String coursCode) {

}
