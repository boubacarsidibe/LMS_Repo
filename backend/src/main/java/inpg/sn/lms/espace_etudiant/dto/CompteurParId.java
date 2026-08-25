package inpg.sn.lms.espace_etudiant.dto;

/**
 * Resultat d'un "COUNT(...) GROUP BY id" : associe un identifiant a son total.
 * Permet de remplacer N requetes de comptage (une par ligne affichee) par une
 * seule requete groupee, dont le resultat est ensuite consulte en memoire.
 */
public record CompteurParId(Long id, Long total) {
}
