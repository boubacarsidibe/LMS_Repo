package inpg.sn.lms.recrutement.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import inpg.sn.lms.recrutement.enums.EnumStatutCandidature;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidatureResponse {

    private Long id;
    private String numeroDossier;
    private EnumStatutCandidature statut;
    private EnumStatutCandidature statutPrecedent;
    private LocalDateTime dateSoumission;
    private String motifRejet;
    private Integer rangListeAttente;
    private Long candidatId;
    private String candidatNom;
    private String candidatPrenom;
    private String candidatEmail;

    /** Champs démographiques du candidat, exposés uniquement pour les statistiques du back-office. */
    private String candidatGenre;
    private String candidatProfession;
    private Integer candidatAnneesExperience;
    
    private LocalDate candidatDateNaissance;
    private String candidatNiveauEtude;
    private String candidatNationalite;

    /** Coordonnées saisies au dépôt, pour l'examen du dossier par l'admission (et la relecture par le candidat). */
    private String candidatTelephone;
    private String candidatAdresse;

    /** Instant où le candidat a certifié sur l'honneur l'exactitude de son dossier ; nul s'il n'a pas été enregistré. */
    private LocalDateTime consentementLe;
    private Long sessionCandidatureId;
    private String sessionCandidatureLibelle;

    /**
     * Informations "Prochaines étapes" saisies par le responsable admission sur la
     * session (délai de paiement, disponibilité de la carte étudiante, date de
     * rentrée) — affichées au candidat une fois admis. Nulles si non renseignées :
     * l'écran l'indique alors plutôt que d'inventer une date.
     */
    private String sessionInfoFraisScolarite;
    private String sessionInfoCarteEtudiante;
    private String sessionInfoRentree;

    private String informationsAcademiques;
    private String informationsProfessionnelles;
    private List<PieceJustificativeResponse> piecesJustificatives;

    /** Pièces exigées et leur état, calculés par le serveur : seule définition, lue par le suivi et l'admission. */
    private List<DocumentAttenduDto> documentsAttendus;
    private PaiementResponse paiement;
    private List<ReponseCritereResponse> reponsesCriteres;

    /**
     * Ce que l'admission peut faire de ce dossier <strong>maintenant</strong>, décidé par le serveur (voir
     * {@code CandidatureService#actionsPossibles}) : les écrans n'ont plus à recopier la table des transitions,
     * dont les anciennes copies avaient cessé de correspondre aux statuts réels.
     */
    private List<String> actionsPossibles;
    /** Statuts antérieurs vers lesquels l'admission peut faire reculer ce dossier (correction, mot de passe exigé). */
    private List<EnumStatutCandidature> reculsPossibles;
}
