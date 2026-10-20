package inpg.sn.lms.espace_etudiant.dto;

import java.util.List;

import inpg.sn.lms.espace_etudiant.enums.EnumTypeAnnonce;

/**
 * Publication d'une annonce, d'un message ou d'un mémo.
 *
 * <p>Destinataires : aucune promotion indiquée vaut diffusion à tous les étudiants ; sinon, une annonce par promotion
 * désignée. {@code cohorteIds} est la clé qu'envoie le formulaire du formateur (la même que la création de cours) ;
 * {@code promotionId} reste accepté pour les anciens clients.
 *
 * <p>Le formulaire envoyait {@code cohorteIds} alors que ce contrat n'attendait que {@code promotionId} : les
 * promotions cochées par le formateur étaient ignorées sans erreur, et toute annonce partait à « toutes les promotions ».
 */
public record AnnonceCreateRequest(
        String titre,
        String contenu,
        EnumTypeAnnonce type,
        Long promotionId,
        List<Long> cohorteIds,
        Boolean epinglee) {
}
