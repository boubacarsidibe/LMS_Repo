package inpg.sn.lms.recrutement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Bilan de l'application du filtre automatique de critères sur les dossiers SOUMIS d'une session. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FiltreCriteresResponse {

    private int total;
    private int preselectionnes;
    private int rejetes;
    /** Dossiers SOUMIS sans aucune pièce justificative, exclus du filtre : ils restent SOUMIS. */
    private int dossiersVides;
    /**
     * Dossiers SOUMIS qui ont des pièces mais auxquels il en manque (ou dont une pièce est à remplacer), exclus du
     * filtre eux aussi : on ne présélectionne pas, et on ne rejette pas, un dossier que le candidat peut encore compléter.
     */
    private int dossiersIncomplets;
}
