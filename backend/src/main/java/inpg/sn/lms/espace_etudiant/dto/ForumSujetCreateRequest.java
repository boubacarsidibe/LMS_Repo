package inpg.sn.lms.espace_etudiant.dto;

/**
 * Ouverture d'un sujet. {@code coursId} et {@code pourMaPromotion} sont
 * mutuellement exclusifs ; les deux absents crée un sujet général, visible de
 * tout l'espace étudiant.
 *
 * <p>{@code pourMaPromotion} n'est qu'un drapeau, jamais un identifiant : la
 * promotion effectivement utilisée est toujours celle de l'auteur, lue côté
 * serveur depuis {@code Etudiant.getPromotion()} — jamais une valeur envoyée
 * par le client, qui pourrait sinon rattacher son sujet à la promotion de
 * n'importe qui (voir {@code ForumController#ouvrirSujet}).
 */
public record ForumSujetCreateRequest(
        String titre,
        Long coursId,
        boolean pourMaPromotion,
        String premierMessage) {
}
