package inpg.sn.lms.espace_etudiant.dto;

import java.util.List;

/**
 * Fiche complète d'un formateur, telle que consultée par le responsable
 * pédagogique en ouvrant sa ligne dans le répertoire.
 *
 * <p>Distinct de {@link FormateurBrefDto}, qui alimente le sélecteur d'équipe
 * pédagogique d'un cours : l'email et le téléphone n'ont pas à y circuler pour
 * composer une équipe, seule cette fiche en a besoin.
 */
public record FormateurDetailDto(
        Long id,
        String nom,
        String prenom,
        String email,
        String telephone,
        String username,
        List<String> coursEnseignes) {
}
