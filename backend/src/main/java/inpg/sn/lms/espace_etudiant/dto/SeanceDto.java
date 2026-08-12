package inpg.sn.lms.espace_etudiant.dto;

import java.time.LocalDateTime;

public record SeanceDto(Long id, Long coursId, String coursTitre, String titre, String type,
        LocalDateTime dateHeure, Integer dureeMinutes, String lieuOuLien) {}