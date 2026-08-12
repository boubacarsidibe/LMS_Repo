package inpg.sn.lms.espace_etudiant.dto;

import java.time.LocalDateTime;

public record SeanceRequest(Long coursId, String titre, String type, LocalDateTime dateHeure,
        Integer dureeMinutes, String lieuOuLien) {}