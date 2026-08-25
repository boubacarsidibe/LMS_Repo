package inpg.sn.lms.espace_etudiant.dto;

import java.util.List;

/**
 * Équipe pédagogique d'un cours, remplacée en bloc.
 *
 * <p>La liste reçue est la liste finale, non un ajout : c'est ce que produit une
 * sélection multiple à l'écran, et cela évite un second endpoint de retrait. Une
 * liste vide détache donc tous les formateurs — le serveur le refuse, un cours sans
 * formateur n'étant visible d'aucun espace enseignant.
 */
public record AffectationFormateursRequest(
        List<Long> formateurIds) {
}
