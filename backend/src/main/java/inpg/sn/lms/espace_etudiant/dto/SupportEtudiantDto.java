package inpg.sn.lms.espace_etudiant.dto;

import java.time.LocalDate;

/** Support de cours accompagné de l'état de consultation de l'étudiant. */
public record SupportEtudiantDto(
        Long id,
        String titre,
        String typeRessource,
        String url,
        LocalDate datePublication,
        boolean consulte) {
}
