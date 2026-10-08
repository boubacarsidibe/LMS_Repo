package inpg.sn.lms.espace_etudiant.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Ce que l'écran de délivrance montre avant d'engager quoi que ce soit : la formation
 * de la promotion, sa période probable, et pour chaque étudiant s'il peut recevoir son
 * certificat dès maintenant.
 *
 * <p>{@code formationIntitule} est {@code null} quand la promotion n'est rattachée à
 * aucune formation : rien ne peut alors être délivré, et l'écran doit le dire.
 */
public record CertificatApercuDto(Long promotionId, String promotionLibelle, String formationIntitule,
        LocalDate dateDebut, LocalDate dateFin, List<EtudiantApercu> etudiants) {

    /**
     * {@code coursNonTermines} est vide quand l'étudiant est éligible ; sinon il porte les
     * motifs, tels que {@code CompletionProgrammeService} les produit.
     */
    public record EtudiantApercu(Long id, String nom, String matricule, String civilite,
            LocalDate dateNaissance, boolean eligible, List<String> coursNonTermines) {
    }
}
