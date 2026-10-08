package inpg.sn.lms.espace_etudiant.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Délivrance de certificats à une promotion entière, à toute la promotion sauf quelques
 * étudiants, ou à un seul de ses étudiants.
 *
 * <p>La formation n'est pas demandée : une promotion appartient à une seule formation,
 * c'est elle qui donne l'intitulé imprimé. {@code mode} vaut {@code TOUS}, {@code TOUS_SAUF}
 * ou {@code UN_SEUL} ; {@code exclusIds} n'a de sens que pour le deuxième, {@code etudiantId}
 * que pour le troisième. La liste des bénéficiaires est toujours recalculée côté serveur :
 * {@code identites} n'apporte que la civilité et la naissance, qui diffèrent d'un étudiant
 * à l'autre.
 *
 * <p>{@code codeSession} est facultatif. Pour plusieurs certificats, chacun reçoit ce code
 * suivi d'un numéro d'ordre ({@code CG1001260206/01}, {@code /02}…) ; pour {@code UN_SEUL},
 * il est imprimé tel quel, numéro compris.
 */
public record CertificatLotRequest(Long promotionId, String mode, List<Long> exclusIds, Long etudiantId,
        LocalDate dateDebut, LocalDate dateFin, String codeSession, List<Identite> identites) {

    /** Civilité et date de naissance d'un étudiant, saisies à l'écran. */
    public record Identite(Long etudiantId, String civilite, LocalDate dateNaissance) {
    }
}
