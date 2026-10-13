package inpg.sn.lms.recrutement.dto;

import inpg.sn.lms.recrutement.enums.EnumTypeDocument;

/**
 * Un document exigé d'un candidat, et où il en est.
 *
 * <p>Calculé par le serveur ({@code CompletudeDossier}) et lu tel quel par le suivi du candidat comme par la fiche
 * d'admission : la liste des pièces exigées existait en trois copies (formulaire, page de suivi, relance), qui ont
 * fini par diverger — le suivi n'affichait pas le diplôme que la relance réclamait.
 */
public record DocumentAttenduDto(EnumTypeDocument type, String libelle, Etat etat) {

    /** Ce qu'il reste à faire pour ce document. */
    public enum Etat {
        /** Au moins une pièce déposée, aucune refusée. Elle peut encore attendre son contrôle. */
        FOURNI,
        /** Aucune pièce de ce type. */
        A_FOURNIR,
        /** Une pièce a été refusée par l'admission : le candidat doit la remplacer. */
        A_REMPLACER
    }
}
