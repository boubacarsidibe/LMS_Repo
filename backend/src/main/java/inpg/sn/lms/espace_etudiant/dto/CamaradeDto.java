package inpg.sn.lms.espace_etudiant.dto;

/**
 * Camarade de promotion, tel que vu par un étudiant sur sa propre fiche
 * promotion — nom et matricule seulement, jamais l'email ni le téléphone
 * (principe de minimisation déjà appliqué à la vérification de certificat).
 */
public record CamaradeDto(Long id, String nom, String matricule) {
}
