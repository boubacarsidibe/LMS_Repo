package inpg.sn.lms.espace_alumni.dto;

import java.time.LocalDate;

import inpg.sn.lms.espace_alumni.enums.EnumStatutEmploi;

public record SituationProfessionnelleRequest(
        String intitulePoste,
        String entreprise,
        LocalDate dateDebut,
        LocalDate dateFin,
        EnumStatutEmploi statutEmploi) {
}
