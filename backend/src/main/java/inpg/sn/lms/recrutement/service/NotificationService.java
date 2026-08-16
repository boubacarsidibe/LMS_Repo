package inpg.sn.lms.recrutement.service;

import java.util.Map;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import inpg.sn.lms.recrutement.entity.NotificationTemplate;
import inpg.sn.lms.recrutement.enums.EnumTypeNotification;

@Service
public class NotificationService {

    private final JavaMailSender mailSender;
    private final NotificationTemplateService templateService;

    public NotificationService(JavaMailSender mailSender, NotificationTemplateService templateService) {
        this.mailSender = mailSender;
        this.templateService = templateService;
    }

    public void envoyerEmail(String destinataire, String sujet, String contenu) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(destinataire);
        message.setSubject("[INPG Recrutement] " + sujet);
        message.setText(contenu);
        mailSender.send(message);
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

    public void notifierConvocationTest(String emailCandidat, String nomEpreuve, String date) {
        envoyerNotification(EnumTypeNotification.CONVOCATION_TEST, emailCandidat,
                Map.of("nomEpreuve", nomEpreuve, "date", date));
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
}
