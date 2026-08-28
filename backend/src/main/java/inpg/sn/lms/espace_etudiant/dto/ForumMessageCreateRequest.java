package inpg.sn.lms.espace_etudiant.dto;

/**
 * Réponse postée dans un sujet du forum.
 *
 * @param citeIds    personnes citées dans le message. Recoupées côté serveur avec le
 *                    périmètre du fil : un identifiant glissé à la main dans la requête
 *                    notifierait sinon n'importe qui, en lui montrant au passage un
 *                    extrait d'un fil auquel il n'a pas accès.
 * @param reponseAId identifiant du message précis auquel celui-ci répond (citation façon
 *                    WhatsApp), facultatif. Recoupé côté serveur avec le sujet courant.
 */
public record ForumMessageCreateRequest(String contenu, java.util.List<Long> citeIds, Long reponseAId) {
}
