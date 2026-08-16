package inpg.sn.lms.espace_etudiant.dto;

import java.time.LocalDateTime;

/** Séance du calendrier académique de l'étudiant. */
public record SeanceEtudiantDto(
        Long id,
        String titre,
        String type,
        LocalDateTime dateHeure,
        Integer dureeMinutes,
        String lieuOuLien,
        Long coursId,
        String coursTitre) {
}
