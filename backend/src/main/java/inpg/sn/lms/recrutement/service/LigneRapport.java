package inpg.sn.lms.recrutement.service;

/**
 * Une ligne d'un rapport de synthèse recrutement (candidatures, tests,
 * entretiens) : regroupée par {@code section} à l'affichage (PDF) et au
 * classeur (Excel), dans l'ordre où les sections sont ajoutées.
 */
public record LigneRapport(String section, String libelle, String valeur) {
}
