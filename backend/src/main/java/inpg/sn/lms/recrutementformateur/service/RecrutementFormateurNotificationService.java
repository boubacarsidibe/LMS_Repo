package inpg.sn.lms.recrutementformateur.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Notifications par e-mail du recrutement des formateurs (procédure QMS-007 :
 * « informer le candidat à chaque étape »).
 *
 * <p>Même patron qu'{@code AccreditationNotificationService} /
 * {@code EntrepriseNotificationService} : pas de modèle configurable en base,
 * un préfixe d'e-mail en dur, et un envoi <strong>best-effort</strong> — une
 * panne SMTP ne doit jamais faire échouer l'acte métier (dépôt, changement de
 * statut, décision) qui déclenche la notification.
 */
@Service
public class RecrutementFormateurNotificationService {

    private static final Logger log = LoggerFactory.getLogger(RecrutementFormateurNotificationService.class);

    private static final String SIGNATURE = "\n\nService recrutement des formateurs — INPG";

    private final JavaMailSender mailSender;
    private final String emailAdmission;

    public RecrutementFormateurNotificationService(JavaMailSender mailSender,
            @Value("${app.recrutement-formateurs.email:recrutement@inpg.sn}") String emailAdmission) {
        this.mailSender = mailSender;
        this.emailAdmission = emailAdmission;
    }

    private void envoyerEmail(String destinataire, String sujet, String contenu) {
        if (destinataire == null || destinataire.isBlank()) {
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(destinataire);
        message.setSubject("[INPG Recrutement Formateurs] " + sujet);
        message.setText(contenu);
        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Échec de l'envoi de l'e-mail de recrutement formateur à {} : {}", destinataire, e.getMessage(), e);
        }
    }

    /** Alerte l'équipe admission qu'une nouvelle candidature attend d'être triée. */
    public void notifierNouvelleCandidature(String intituleOffre) {
        envoyerEmail(emailAdmission, "Nouvelle candidature de formateur reçue",
                "Une nouvelle candidature vient d'être déposée pour l'offre « " + intituleOffre + " ».");
    }

    /** Accusé de réception au candidat. */
    public void notifierDossierRecu(String emailCandidat, String prenom, String intituleOffre) {
        envoyerEmail(emailCandidat, "Votre candidature a bien été reçue",
                "Bonjour " + prenom + ",\n\nVotre candidature pour l'offre « " + intituleOffre
                        + " » a bien été reçue et sera examinée par nos équipes." + SIGNATURE);
    }

    /** Le dossier passe en cours de traitement. */
    public void notifierPriseEnCharge(String emailCandidat, String prenom, String intituleOffre) {
        envoyerEmail(emailCandidat, "Votre candidature est en cours de traitement",
                "Bonjour " + prenom + ",\n\nVotre candidature pour l'offre « " + intituleOffre
                        + " » est désormais en cours de traitement." + SIGNATURE);
    }

    /** Candidat retenu : le compte formateur est activé. */
    public void notifierRetenu(String emailCandidat, String prenom, String intituleOffre) {
        envoyerEmail(emailCandidat, "Votre candidature a été retenue",
                "Bonjour " + prenom + ",\n\nFélicitations : votre candidature pour l'offre « " + intituleOffre
                        + " » a été retenue. Votre espace formateur est activé — connectez-vous avec les identifiants "
                        + "de votre compte de candidature." + SIGNATURE);
    }

    /** Candidat non retenu, avec le motif. */
    public void notifierNonRetenu(String emailCandidat, String prenom, String intituleOffre, String motif) {
        envoyerEmail(emailCandidat, "Suite donnée à votre candidature",
                "Bonjour " + prenom + ",\n\nVotre candidature pour l'offre « " + intituleOffre
                        + " » n'a pas été retenue.\n\nMotif : " + (motif != null ? motif : "Non précisé") + SIGNATURE);
    }
}
