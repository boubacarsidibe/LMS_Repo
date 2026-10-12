package inpg.sn.lms.recrutementformateur.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Offre de recrutement de formateur, vue complète (espace admission). */
public record OffreFormateurDto(
        Long id,
        String intitule,
        String domaine,
        String descriptionMissions,
        String profilRecherche,
        String typeContrat,
        String lieu,
        int nbPostes,
        Long formationId,
        String intituleFormation,
        LocalDate datePublication,
        LocalDate dateLimiteCandidature,
        String statut,
        LocalDateTime dateCreation,
        long nombreCandidatures,
        /** Nom du fichier de la fiche de poste jointe ; {@code null} si l'offre n'en a pas. */
        String fichePosteNom) {
}
