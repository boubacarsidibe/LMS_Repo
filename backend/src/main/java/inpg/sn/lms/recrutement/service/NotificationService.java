package inpg.sn.lms.recrutement.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final JavaMailSender mailSender;

    public NotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void envoyerEmail(String destinataire, String sujet, String contenu) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(destinataire);
        message.setSubject("[INPG Recrutement] " + sujet);
        message.setText(contenu);
        mailSender.send(message);
    }

    public void notifierValidationDossier(String emailCandidat, String numeroDossier) {
        envoyerEmail(emailCandidat,
                "Dossier validé",
                "Bonjour,\n\nVotre dossier de candidature n°" + numeroDossier
                        + " a été validé et est maintenant en cours de présélection.\n\nCordialement,\nL'équipe INPG");
    }

    public void notifierRejetDossier(String emailCandidat, String numeroDossier, String motif) {
        envoyerEmail(emailCandidat,
                "Dossier rejeté",
                "Bonjour,\n\nVotre dossier de candidature n°" + numeroDossier
                        + " a été rejeté.\nMotif : " + (motif != null ? motif : "Non spécifié")
                        + "\n\nCordialement,\nL'équipe INPG");
    }

    public void notifierPaiementSucces(String emailCandidat, String numeroDossier) {
        envoyerEmail(emailCandidat,
                "Paiement confirmé",
                "Bonjour,\n\nLe paiement des frais de dossier pour la candidature n°" + numeroDossier
                        + " a été confirmé.\n\nCordialement,\nL'équipe INPG");
    }

    public void notifierConvocationTest(String emailCandidat, String nomEpreuve, String date) {
        envoyerEmail(emailCandidat,
                "Convocation au test académique",
                "Bonjour,\n\nVous êtes convoqué(e) au test \"" + nomEpreuve + "\" le " + date
                        + ".\n\nCordialement,\nL'équipe INPG");
    }

    public void notifierConvocationEntretien(String emailCandidat, String date, String lieuOuLien) {
        envoyerEmail(emailCandidat,
                "Convocation à l'entretien",
                "Bonjour,\n\nVous êtes convoqué(e) à l'entretien le " + date + ".\nLieu/Lien : " + lieuOuLien
                        + "\n\nCordialement,\nL'équipe INPG");
    }

    public void notifierAdmission(String emailCandidat, String numeroDossier) {
        envoyerEmail(emailCandidat,
                "Félicitations - Admission",
                "Bonjour,\n\nFélicitations ! Votre candidature n°" + numeroDossier
                        + " a été retenue. Vous êtes admis(e) à l'INPG.\n\nCordialement,\nL'équipe INPG");
    }

    public void notifierNonAdmission(String emailCandidat, String numeroDossier) {
        envoyerEmail(emailCandidat,
                "Résultat de candidature",
                "Bonjour,\n\nNous vous informons que votre candidature n°" + numeroDossier
                        + " n'a malheureusement pas été retenue.\n\nCordialement,\nL'équipe INPG");
    }
}