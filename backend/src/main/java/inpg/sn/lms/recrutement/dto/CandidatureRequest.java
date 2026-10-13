package inpg.sn.lms.recrutement.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CandidatureRequest {

    private Long sessionCandidatureId;
    private String informationsPersonnelles;
    private String informationsAcademiques;
    private String informationsProfessionnelles;

    /** Réponses aux critères personnalisés de la session : clé = id du critère, valeur = réponse saisie. */
    private Map<Long, String> reponsesCriteres;

    /**
     * Certification sur l'honneur cochée par le candidat. Volontairement non exigée côté serveur : un client plus
     * ancien ne l'envoie pas, et refuser son dépôt serait pire que de ne pas dater la certification.
     */
    private Boolean consentement;
}