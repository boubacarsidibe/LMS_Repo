package inpg.sn.lms.entreprise.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(EntrepriseNotificationService.class);

    private final JavaMailSender mailSender;
    private final String emailEntreprise;

    public EntrepriseNotificationService(JavaMailSender mailSender,
            @Value("${app.entreprise.email:entreprise@inpg.sn}") String emailEntreprise) {
        this.mailSender = mailSender;
        this.emailEntreprise = emailEntreprise;
    }

    // Best-effort : une panne SMTP ne doit jamais faire echouer l'action metier
    // (soumission de demande, message...) qui declenche cette notification.
    private void envoyerEmail(String destinataire, String sujet, String contenu) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(destinataire);
        message.setSubject("[INPG Entreprise] " + sujet);
        message.setText(contenu);
        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Échec de l'envoi de l'email entreprise à {} : {}", destinataire, e.getMessage(), e);
        }
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

    /** Prévient l'entreprise que sa fiche technique de module est disponible pour revue. */
    public void notifierFicheTransmise(String emailDestinataire, String raisonSociale, String intituleModule) {
        if (emailDestinataire == null || emailDestinataire.isBlank()) {
            return;
        }
        envoyerEmail(emailDestinataire, "Votre fiche technique de module est disponible",
                "Bonjour,\n\nLa fiche technique du module « "
                        + (intituleModule == null || intituleModule.isBlank() ? "formation sur mesure" : intituleModule)
                        + " » proposé à « " + raisonSociale
                        + " » est disponible dans votre espace. Vous pouvez l'accepter ou demander des ajustements.");
    }

    /** Prévient l'équipe pédagogique de la décision de l'entreprise sur la fiche. */
    public void notifierDecisionFiche(String raisonSociale, boolean acceptee, String commentaire) {
        if (acceptee) {
            envoyerEmail(emailEntreprise, "Fiche technique acceptée",
                    "L'entreprise « " + raisonSociale + " » a accepté la fiche technique du module. "
                            + "La demande est clôturée.");
        } else {
            envoyerEmail(emailEntreprise, "Ajustements demandés sur la fiche technique",
                    "L'entreprise « " + raisonSociale + " » demande des ajustements sur la fiche technique du module.\n\n"
                            + "Commentaire : " + (commentaire != null ? commentaire : "Non précisé"));
        }
    }
}
