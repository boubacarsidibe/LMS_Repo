package inpg.sn.lms.espace_etudiant.dto;

import java.time.LocalDate;

/**
 * Support d'un cours tel que présenté à l'étudiant. Plus complet que
 * {@link SupportDto}, qui se limite à l'identifiant, au titre et à l'URL et
 * reste utilisé par les listes existantes : ici il faut aussi savoir si un
 * fichier est réellement téléchargeable, et depuis quand.
 */
public record SupportDetailDto(
        Long id,
        String titre,
        String typeRessource,
        String url,
        LocalDate datePublication,
        boolean telechargeable) {

}
