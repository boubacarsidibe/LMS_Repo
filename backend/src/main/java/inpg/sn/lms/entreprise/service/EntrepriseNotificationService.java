package inpg.sn.lms.entreprise.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import inpg.sn.lms.entreprise.enums.EnumTypeDemandeEntreprise;

/**
 * Notifications par email du module Espace Entreprise (TDR §H).
 *
 * <p>Même patron qu'{@code AccreditationNotificationService} : pas de modèle
 * configurable en base, un préfixe d'email en dur propre au module.
 */
@Service
public class EntrepriseNotificationService {

    private final JavaMailSender mailSender;
    private final String emailEntreprise;

    public EntrepriseNotificationService(JavaMailSender mailSender,
            @Value("${app.entreprise.email:entreprise@inpg.sn}") String emailEntreprise) {
        this.mailSender = mailSender;
        this.emailEntreprise = emailEntreprise;
    }

    private void envoyerEmail(String destinataire, String sujet, String contenu) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(destinataire);
        message.setSubject("[INPG Entreprise] " + sujet);
        message.setText(contenu);
        mailSender.send(message);
    }

    /** Alerte l'équipe pédagogique qu'une nouvelle demande entreprise attend une réponse. */
    public void notifierNouvelleDemande(String raisonSociale, EnumTypeDemandeEntreprise type) {
        String nature = type == EnumTypeDemandeEntreprise.DEVIS_SESSION
                ? "une demande de devis pour une session sur mesure"
                : "un besoin de formation";
        envoyerEmail(emailEntreprise, "Nouvelle demande entreprise reçue",
                "L'entreprise « " + raisonSociale + " » vient de soumettre " + nature + ".");
    }

    /** Alerte l'équipe pédagogique qu'un nouveau message attend une réponse dans la messagerie. */
    public void notifierNouveauMessage(String raisonSociale) {
        envoyerEmail(emailEntreprise, "Nouveau message entreprise",
                "L'entreprise « " + raisonSociale + " » a envoyé un nouveau message dans sa messagerie privée.");
    }
}
