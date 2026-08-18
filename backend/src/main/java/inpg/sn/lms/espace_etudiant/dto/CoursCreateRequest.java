package inpg.sn.lms.espace_etudiant.dto;

import java.util.List;

/**
 * Cours créé ou modifié, depuis l'espace formateur comme depuis l'espace pédagogique.
 *
 * <p>Deux façons de désigner le programme, parce que les deux espaces ne raisonnent
 * pas pareil. Le formateur pense en promotions — c'est ce que son écran affiche — et
 * le serveur remonte à leur formation. Le responsable pédagogique, lui, pilote l'offre
 * et désigne directement les <strong>formations</strong> : c'est le rattachement réel,
 * et il vaut pour toutes les promotions à venir sans avoir à refaire le geste chaque
 * rentrée.
 *
 * <p>{@code null} sur les deux champs signifie « ne pas toucher au programme ». Une
 * liste vide signifie « le retirer de tous les programmes » : ce n'est pas la même
 * chose, et confondre les deux effacerait un rattachement à chaque modification.
 */
public record CoursCreateRequest(
        String code,
        String titre,
        String semestre,
        String description,
        Long volumeHoraire,
        List<Long> cohorteIds,
        List<Long> formationIds) {
}
