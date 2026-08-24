package inpg.sn.lms.accreditation.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Notifications par email du module d'accréditation (TDR §F : « informer
 * automatiquement les écoles par mail à chaque étape »).
 *
 * <p>Même patron que {@code recrutement.service.NotificationService}, en plus
 * simple : pas de modèle configurable en base — le TDR ne demande ici que des
 * emails automatiques, pas un écran d'administration de leurs textes. Un
 * service distinct plutôt qu'une extension de {@code NotificationService} :
 * celui-ci préfixe ses emails « [INPG Recrutement] » en dur, propre à son
 * module.
 */
@Service
public class AccreditationNotificationService {

    private final JavaMailSender mailSender;
    private final String emailAccreditation;

    public AccreditationNotificationService(JavaMailSender mailSender,
            @Value("${app.accreditation.email:accreditation@inpg.sn}") String emailAccreditation) {
        this.mailSender = mailSender;
        this.emailAccreditation = emailAccreditation;
    }

    private void envoyerEmail(String destinataire, String sujet, String contenu) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(destinataire);
        message.setSubject("[INPG Accréditation] " + sujet);
        message.setText(contenu);
        mailSender.send(message);
    }

    /** Alerte l'équipe d'accréditation qu'une nouvelle demande attend d'être évaluée. */
    public void notifierNouvelleDemande(String nomEcole) {
        envoyerEmail(emailAccreditation, "Nouvelle demande d'accréditation reçue",
                "L'école « " + nomEcole + " » vient de soumettre une demande d'accréditation.");
    }

    public void notifierPriseEnCharge(String emailEcole, String nomEcole) {
        envoyerEmail(emailEcole, "Votre demande d'accréditation est en cours d'évaluation",
                "Bonjour,\n\nVotre demande d'accréditation pour « " + nomEcole
                        + " » est désormais en cours d'évaluation par un évaluateur de l'INPG.");
    }

    public void notifierAccreditation(String emailEcole, String nomEcole) {
        envoyerEmail(emailEcole, "Votre école est accréditée",
                "Bonjour,\n\nFélicitations : « " + nomEcole
                        + " » est désormais accréditée par l'INPG pour dispenser des formations dans le "
                        + "domaine du pétrole et du gaz.");
    }

    public void notifierRejet(String emailEcole, String nomEcole, String motif) {
        envoyerEmail(emailEcole, "Votre demande d'accréditation n'a pas été retenue",
                "Bonjour,\n\nLa demande d'accréditation de « " + nomEcole + " » n'a pas été retenue.\n\n"
                        + "Motif : " + (motif != null ? motif : "Non spécifié"));
    }
}
