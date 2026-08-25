package inpg.sn.lms.espace_etudiant.dto;

import java.time.LocalDateTime;

/**
 * Support récemment ouvert, proposé à l'étudiant pour reprendre son travail.
 *
 * <p>Un suivi équivalent existait auparavant dans le {@code localStorage} du
 * navigateur, faute de trace serveur à l'époque. Il enregistrait des <em>formations</em>
 * — les fiches du catalogue sur lesquelles on avait cliqué — et non le travail réel.
 * {@code progressions_support} horodate désormais les consultations côté serveur : la
 * reprise survit au changement d'appareil et désigne un vrai support de cours.
 */
public record RepriseDto(
        Long supportId,
        String supportTitre,
        String typeRessource,
        String url,
        Long coursId,
        String coursTitre,
        LocalDateTime dateConsultation) {
}
