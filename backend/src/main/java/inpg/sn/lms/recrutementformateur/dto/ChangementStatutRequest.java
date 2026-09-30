package inpg.sn.lms.recrutementformateur.dto;

/** Fait avancer le suivi d'une candidature : {@code DOSSIER_RECU} → {@code EN_COURS_DE_TRAITEMENT} → {@code TERMINE}. */
public record ChangementStatutRequest(String statut) {
}
