package inpg.sn.lms.accreditation.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(AccreditationNotificationService.class);

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String SIGNATURE = "\n\nDirection de la Régulation et du Contrôle Qualité (DRCQ) — INPG";

    private final JavaMailSender mailSender;
    private final String emailAccreditation;

    public AccreditationNotificationService(JavaMailSender mailSender,
            @Value("${app.accreditation.email:accreditation@inpg.sn}") String emailAccreditation) {
        this.mailSender = mailSender;
        this.emailAccreditation = emailAccreditation;
    }

    // Best-effort : une panne SMTP ne doit jamais faire echouer la decision
    // metier (prise en charge, accreditation, rejet...) qui declenche l'email.
    private void envoyerEmail(String destinataire, String sujet, String contenu) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(destinataire);
        message.setSubject("[INPG Accréditation] " + sujet);
        message.setText(contenu);
        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Échec de l'envoi de l'email d'accréditation à {} : {}", destinataire, e.getMessage(), e);
        }
    }

    /** Alerte l'équipe d'accréditation qu'une nouvelle demande attend d'être évaluée. */
    public void notifierNouvelleDemande(String nomEcole) {
        envoyerEmail(emailAccreditation, "Nouvelle demande d'accréditation reçue",
                "L'école « " + nomEcole + " » vient de soumettre une demande d'accréditation.");
    }

    public void notifierPriseEnCharge(String emailEcole, String nomEcole) {
        envoyerEmail(emailEcole, "Votre demande d'accréditation est en cours d'évaluation",
                "Bonjour,\n\nVotre demande d'accréditation pour « " + nomEcole
                        + " » est désormais en cours d'évaluation par un évaluateur de l'INPG." + SIGNATURE);
    }

    public void notifierAccreditation(String emailEcole, String nomEcole, LocalDate dateExpiration) {
        envoyerEmail(emailEcole, "Votre programme est accrédité",
                "Bonjour,\n\nFélicitations : le programme « " + nomEcole
                        + " » est accrédité par l'INPG pour dispenser des formations dans le domaine du "
                        + "pétrole et du gaz." + validiteLigne(dateExpiration) + SIGNATURE);
    }

    public void notifierAccreditationSousReserve(String emailEcole, String nomEcole, String planActions,
            LocalDate dateExpiration) {
        envoyerEmail(emailEcole, "Votre programme est accrédité sous réserve",
                "Bonjour,\n\nLe programme « " + nomEcole + " » bénéficie d'une accréditation conditionnelle."
                        + validiteLigne(dateExpiration)
                        + "\n\nPlan d'actions à mettre en œuvre :\n" + (planActions != null ? planActions : "Non spécifié")
                        + SIGNATURE);
    }

    public void notifierRejet(String emailEcole, String nomEcole, String motif) {
        envoyerEmail(emailEcole, "Votre demande d'accréditation n'a pas été retenue",
                "Bonjour,\n\nLa demande d'accréditation du programme « " + nomEcole + " » n'a pas été retenue.\n\n"
                        + "Motif : " + (motif != null ? motif : "Non spécifié") + SIGNATURE);
    }

    private String validiteLigne(LocalDate dateExpiration) {
        return dateExpiration == null ? "" : "\n\nCette accréditation est valable jusqu'au " + dateExpiration.format(DATE)
                + " (3 ans).";
    }
}
