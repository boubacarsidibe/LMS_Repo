package inpg.sn.lms.recrutement.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import inpg.sn.lms.recrutement.entity.NotificationTemplate;
import inpg.sn.lms.recrutement.enums.EnumTypeNotification;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;
    private final NotificationTemplateService templateService;

    // Sans expéditeur explicite, l'envoi part avec un From vide : certains
    // serveurs SMTP (dont Brevo) le rejettent silencieusement ou le remplacent,
    // et les relances collectives n'arrivaient jamais malgré des identifiants
    // correctement transmis.
    @Value("${app.mail.from}")
    private String expediteur;

    public NotificationService(JavaMailSender mailSender, NotificationTemplateService templateService) {
        this.mailSender = mailSender;
        this.templateService = templateService;
    }

    // Best-effort : cette methode est appelee depuis de tres nombreuses actions
    // metier (validation/rejet de dossier, convocations, decisions d'admission,
    // relances...). Une panne SMTP ne doit jamais faire echouer l'action qui
    // declenche la notification.
    public void envoyerEmail(String destinataire, String sujet, String contenu) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(expediteur);
        message.setTo(destinataire);
        message.setSubject("[INPG Recrutement] " + sujet);
        message.setText(contenu);
        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Échec de l'envoi de l'email de recrutement à {} : {}", destinataire, e.getMessage(), e);
        }
    }

    /**
     * Envoie l'email correspondant à un modèle personnalisable (TDR : modèles
     * configurables + activation/désactivation par type). N'envoie rien si le
     * type de notification a été désactivé par l'admin.
     */
    private void envoyerNotification(EnumTypeNotification type, String emailCandidat, Map<String, String> variables) {
        NotificationTemplate template = templateService.getTemplate(type);
        if (!template.isActif()) {
            return;
        }
        envoyerEmail(emailCandidat, remplacerVariables(template.getSujet(), variables),
                remplacerVariables(template.getCorps(), variables));
    }

    private String remplacerVariables(String texte, Map<String, String> variables) {
        String resultat = texte;
        for (Map.Entry<String, String> entree : variables.entrySet()) {
            resultat = resultat.replace("{{" + entree.getKey() + "}}", entree.getValue() != null ? entree.getValue() : "");
        }
        return resultat;
    }

    public void notifierValidationDossier(String emailCandidat, String numeroDossier) {
        envoyerNotification(EnumTypeNotification.VALIDATION_DOSSIER, emailCandidat,
                Map.of("numeroDossier", numeroDossier));
    }

    public void notifierRejetDossier(String emailCandidat, String numeroDossier, String motif) {
        envoyerNotification(EnumTypeNotification.REJET_DOSSIER, emailCandidat,
                Map.of("numeroDossier", numeroDossier, "motif", motif != null ? motif : "Non spécifié"));
    }

    public void notifierPaiementSucces(String emailCandidat, String numeroDossier) {
        envoyerNotification(EnumTypeNotification.PAIEMENT_SUCCES, emailCandidat,
                Map.of("numeroDossier", numeroDossier));
    }

    public void notifierConvocationTest(String emailCandidat, String nomEpreuve, String date, String lieuOuLien) {
        envoyerNotification(EnumTypeNotification.CONVOCATION_TEST, emailCandidat,
                Map.of("nomEpreuve", nomEpreuve, "date", date, "lieuOuLien", lieuOuLien != null ? lieuOuLien : ""));
    }

    public void notifierConvocationEntretien(String emailCandidat, String date, String lieuOuLien) {
        envoyerNotification(EnumTypeNotification.CONVOCATION_ENTRETIEN, emailCandidat,
                Map.of("date", date, "lieuOuLien", lieuOuLien));
    }

    public void notifierAdmissionTest(String emailCandidat, String numeroDossier) {
        envoyerNotification(EnumTypeNotification.ADMISSION_TEST, emailCandidat,
                Map.of("numeroDossier", numeroDossier));
    }

    public void notifierAdmissionEntretien(String emailCandidat, String numeroDossier) {
        envoyerNotification(EnumTypeNotification.ADMISSION_ENTRETIEN, emailCandidat,
                Map.of("numeroDossier", numeroDossier));
    }

    public void notifierAdmissionFinale(String emailCandidat, String numeroDossier) {
        envoyerNotification(EnumTypeNotification.ADMISSION_FINALE, emailCandidat,
                Map.of("numeroDossier", numeroDossier));
    }

    public void notifierNonAdmission(String emailCandidat, String numeroDossier) {
        envoyerNotification(EnumTypeNotification.NON_ADMISSION, emailCandidat,
                Map.of("numeroDossier", numeroDossier));
    }

    public void notifierDocumentConforme(String emailCandidat, String numeroDossier, String typeDocument) {
        envoyerNotification(EnumTypeNotification.DOCUMENT_CONFORME, emailCandidat,
                Map.of("numeroDossier", numeroDossier, "typeDocument", typeDocument));
    }

    public void notifierDocumentNonConforme(String emailCandidat, String numeroDossier, String typeDocument) {
        envoyerNotification(EnumTypeNotification.DOCUMENT_NON_CONFORME, emailCandidat,
                Map.of("numeroDossier", numeroDossier, "typeDocument", typeDocument));
    }

    public void notifierRappelDossierIncomplet(String emailCandidat, String numeroDossier, String piecesManquantes) {
        envoyerNotification(EnumTypeNotification.RAPPEL_DOSSIER_INCOMPLET, emailCandidat,
                Map.of("numeroDossier", numeroDossier, "piecesManquantes", piecesManquantes));
    }

    public void notifierRappelPaiement(String emailCandidat, String numeroDossier) {
        envoyerNotification(EnumTypeNotification.RAPPEL_PAIEMENT, emailCandidat,
                Map.of("numeroDossier", numeroDossier));
    }
}
