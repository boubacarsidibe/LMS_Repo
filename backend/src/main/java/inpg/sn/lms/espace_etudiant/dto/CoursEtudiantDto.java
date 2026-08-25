package inpg.sn.lms.espace_etudiant.dto;

import java.util.List;

/**
 * Cours tel que vu par un étudiant : uniquement ceux de sa formation, avec sa
 * progression personnelle.
 *
 * <p>{@code progression} est calculée à la volée (supports consultés / supports
 * publiés) et n'est jamais stockée.
 */
public record CoursEtudiantDto(
        Long id,
        String code,
        String titre,
        String description,
        String semestre,
        Long volumeHoraire,
        String imageUrl,
        List<String> formateurs,
        long nbSupports,
        long nbSupportsConsultes,
        int progression,
        Integer maNote) {
}
