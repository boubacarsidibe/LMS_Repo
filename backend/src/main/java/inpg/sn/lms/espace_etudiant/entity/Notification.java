package inpg.sn.lms.espace_etudiant.entity;

import java.time.LocalDateTime;

import inpg.sn.lms.auth.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Notification déposée dans l'application, adossée à l'utilisateur.
 *
 * <p>Une cloche existait déjà, mais liée au <strong>candidat</strong>
 * ({@code NotificationCandidat}) : inutilisable pour un étudiant ou un formateur. Les
 * relances collectives du responsable pédagogique partaient donc uniquement par
 * courriel, faute de canal interne.
 *
 * <p>Le courriel n'a pas été retenu pour les mentions : elles sont fréquentes et de
 * faible enjeu, une par message inonderait la boîte, l'utilisateur couperait la
 * notification, et elle cesserait de servir. L'action est d'ailleurs dans
 * l'application — on clique, on arrive sur le fil.
 */
@Entity
@Table(name = "notification")
public class Notification {

    public static final String MENTION_FORUM = "MENTION_FORUM";

    /** Réponse à un message précis du forum (citation façon WhatsApp). */
    public static final String REPONSE_FORUM = "REPONSE_FORUM";

    /** Nouveau message reçu en messagerie privée (apprenant ↔ formateur). */
    public static final String MESSAGE_PRIVE = "MESSAGE_PRIVE";

    /** Nouvelle campagne d'enquête de suivi (TDR §G) ouverte aux alumni. */
    public static final String ENQUETE_SUIVI = "ENQUETE_SUIVI";

    /** Rappel automatique de dépôt d'une évaluation de cours (TDR §G, besoin B4). */
    public static final String EVALUATION_RAPPEL = "EVALUATION_RAPPEL";

    /** Nouvelle demande de rendez-vous reçue par un membre du personnel (TDR §J). */
    public static final String RENDEZ_VOUS_DEMANDE = "RENDEZ_VOUS_DEMANDE";

    /** Réponse (acceptation, refus ou contre-proposition) à une demande de rendez-vous (TDR §J). */
    public static final String RENDEZ_VOUS_REPONSE = "RENDEZ_VOUS_REPONSE";

    /** Un devoir ou une évaluation vient d'être publié dans un cours suivi. */
    public static final String ACTIVITE_COURS = "ACTIVITE_COURS";

    /** Une correction est disponible pour une remise de devoir ou d'évaluation. */
    public static final String ACTIVITE_CORRIGEE = "ACTIVITE_CORRIGEE";

    /** Un candidat déclare avoir réglé ses frais de dossier : le règlement attend la validation de l'admission. */
    public static final String PAIEMENT_A_VALIDER = "PAIEMENT_A_VALIDER";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User destinataire;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false, length = 64)
    private String type;

    @Column(nullable = false)
    private boolean lu = false;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    /** Où mène la notification. Sans lien, elle informe sans permettre d'agir. */
    @Column
    private String lien;

    protected Notification() {
        // requis par JPA
    }

    public Notification(User destinataire, String message, String type, String lien) {
        this.destinataire = destinataire;
        this.message = message;
        this.type = type;
        this.lien = lien;
        this.dateCreation = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getDestinataire() {
        return destinataire;
    }

    public String getMessage() {
        return message;
    }

    public String getType() {
        return type;
    }

    public boolean isLu() {
        return lu;
    }

    public void setLu(boolean lu) {
        this.lu = lu;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public String getLien() {
        return lien;
    }
}
