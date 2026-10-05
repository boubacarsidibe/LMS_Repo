package inpg.sn.lms.espace_etudiant.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Rapport des comptes formateurs, en PDF (demande responsable pédagogique).
 * Il n'existe pas de parcours de candidature pour un formateur — l'ouverture
 * d'un compte se fait directement (voir {@code PedagogieController#creerFormateur})
 * — ce rapport reste donc un état des lieux de l'effectif plutôt qu'un
 * entonnoir de recrutement comme celui des candidatures étudiantes.
 */
public record RapportFormateursDto(
        int totalFormateurs,
        int formateursSansCours,
        List<FormateurRapport> formateurs) {

    public record FormateurRapport(
            String nom,
            String prenom,
            String email,
            String telephone,
            String identifiant,
            LocalDateTime dateCreationCompte,
            List<String> coursEnseignes) {
    }
}
