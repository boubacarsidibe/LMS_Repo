package inpg.sn.lms.recrutement.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import inpg.sn.lms.recrutement.entity.NotificationTemplate;
import inpg.sn.lms.recrutement.enums.EnumTypeNotification;
import inpg.sn.lms.recrutement.repository.NotificationTemplateRepository;

/**
 * Modèles d'emails candidats, éditables depuis le back-office (TDR : "configurer
 * les modèles d'emails" + "activer les notifications automatiques"). Tant qu'un
 * type n'a jamais été personnalisé, un modèle par défaut raisonnable est
 * renvoyé (non persisté), pour que la page d'admin ait toujours un contenu
 * cohérent à afficher/éditer.
 */
@Service
@Transactional
public class NotificationTemplateService {

    private final NotificationTemplateRepository repository;

    public NotificationTemplateService(NotificationTemplateRepository repository) {
        this.repository = repository;
    }

    public NotificationTemplate getTemplate(EnumTypeNotification type) {
        return repository.findById(type).orElseGet(() -> genererParDefaut(type));
    }

    public List<NotificationTemplate> getAllTemplates() {
        return Arrays.stream(EnumTypeNotification.values())
                .map(this::getTemplate)
                .collect(Collectors.toList());
    }

    public NotificationTemplate mettreAJour(EnumTypeNotification type, NotificationTemplate request) {
        NotificationTemplate template = getTemplate(type);
        template.setSujet(request.getSujet());
        template.setCorps(request.getCorps());
        template.setActif(request.isActif());
        return repository.save(template);
    }

    private NotificationTemplate genererParDefaut(EnumTypeNotification type) {
        NotificationTemplate t = new NotificationTemplate();
        t.setType(type);
        t.setActif(true);
        switch (type) {
            case VALIDATION_DOSSIER -> {
                t.setSujet("Dossier validé");
                t.setCorps("Bonjour,\n\nVotre dossier de candidature n°{{numeroDossier}} a été validé et est "
                        + "maintenant en cours de présélection. Merci de procéder au paiement des frais de dossier "
                        + "depuis votre espace candidat.\n\nCordialement,\nL'équipe INPG");
            }
            case REJET_DOSSIER -> {
                t.setSujet("Dossier rejeté");
                t.setCorps("Bonjour,\n\nVotre dossier de candidature n°{{numeroDossier}} a été rejeté.\nMotif : "
                        + "{{motif}}\n\nCordialement,\nL'équipe INPG");
            }
            case PAIEMENT_SUCCES -> {
                t.setSujet("Paiement confirmé");
                t.setCorps("Bonjour,\n\nLe paiement des frais de dossier pour la candidature n°{{numeroDossier}} a "
                        + "été confirmé.\n\nCordialement,\nL'équipe INPG");
            }
            case CONVOCATION_TEST -> {
                t.setSujet("Convocation au test académique");
                t.setCorps("Bonjour,\n\nVous êtes convoqué(e) au test \"{{nomEpreuve}}\" le {{date}}."
                        + "\nLieu/Lien : {{lieuOuLien}}"
                        + "\n\nCordialement,\nL'équipe INPG");
            }
            case CONVOCATION_ENTRETIEN -> {
                t.setSujet("Convocation à l'entretien");
                t.setCorps("Bonjour,\n\nVous êtes convoqué(e) à l'entretien le {{date}}.\nLieu/Lien : {{lieuOuLien}}"
                        + "\n\nCordialement,\nL'équipe INPG");
            }
            case ADMISSION_TEST -> {
                t.setSujet("Admission au Test Académique");
                t.setCorps("Bonjour,\n\nFélicitations ! Votre dossier n°{{numeroDossier}} a été retenu. Vous êtes "
                        + "convoqué(e) au Test Académique.\n\nCordialement,\nL'équipe INPG");
            }
            case ADMISSION_ENTRETIEN -> {
                t.setSujet("Admission à l'Entretien");
                t.setCorps("Bonjour,\n\nFélicitations ! Vous avez réussi le Test Académique. Votre dossier "
                        + "n°{{numeroDossier}} passe à l'étape de l'Entretien.\n\nCordialement,\nL'équipe INPG");
            }
            case ADMISSION_FINALE -> {
                t.setSujet("Félicitations - Admission Définitive");
                t.setCorps("Bonjour,\n\nFélicitations ! Votre candidature n°{{numeroDossier}} a été définitivement "
                        + "retenue. Vous êtes admis(e) à l'INPG.\n\nCordialement,\nL'équipe INPG");
            }
            case NON_ADMISSION -> {
                t.setSujet("Résultat de candidature");
                t.setCorps("Bonjour,\n\nNous vous informons que votre candidature n°{{numeroDossier}} n'a "
                        + "malheureusement pas été retenue.\n\nCordialement,\nL'équipe INPG");
            }
            case DOCUMENT_CONFORME -> {
                t.setSujet("Document validé");
                t.setCorps("Bonjour,\n\nLe document \"{{typeDocument}}\" de votre dossier n°{{numeroDossier}} a été "
                        + "vérifié et validé comme conforme.\n\nCordialement,\nL'équipe INPG");
            }
            case DOCUMENT_NON_CONFORME -> {
                t.setSujet("Document à corriger");
                t.setCorps("Bonjour,\n\nLe document \"{{typeDocument}}\" de votre dossier n°{{numeroDossier}} n'a "
                        + "pas été jugé conforme. Merci de vous connecter à votre espace candidat pour le remplacer "
                        + "au plus vite.\n\nCordialement,\nL'équipe INPG");
            }
            case RAPPEL_DOSSIER_INCOMPLET -> {
                t.setSujet("Votre dossier de candidature est incomplet");
                t.setCorps("Bonjour,\n\nVotre dossier de candidature n°{{numeroDossier}} est encore incomplet : "
                        + "{{piecesManquantes}}. Merci de vous connecter à votre espace candidat pour compléter ou "
                        + "remplacer les pièces concernées dès que possible.\n\nCordialement,\nL'équipe INPG");
            }
            case RAPPEL_PAIEMENT -> {
                t.setSujet("Frais de dossier en attente de règlement");
                t.setCorps("Bonjour,\n\nVotre dossier de candidature n°{{numeroDossier}} est retenu, mais les frais "
                        + "de dossier n'ont pas encore été réglés. Merci de procéder au paiement depuis votre espace "
                        + "candidat pour poursuivre votre parcours d'admission.\n\nCordialement,\nL'équipe INPG");
            }
        }
        return t;
    }
}
