package inpg.sn.lms.recrutement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Réponse d'un candidat à un critère personnalisé, telle qu'affichée côté responsable admission. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReponseCritereResponse {

    private Long critereId;
    private String libelle;
    private String valeur;
}
