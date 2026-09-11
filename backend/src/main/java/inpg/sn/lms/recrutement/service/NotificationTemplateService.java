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
        final String signature = "\n\nCordialement,\nL'équipe des admissions\nInstitut National du Pétrole et du Gaz (INPG)";
        switch (type) {
            case VALIDATION_DOSSIER -> {
                t.setSujet("Votre dossier de candidature a été validé");
                t.setCorps("Bonjour {{nomCandidat}},\n\nNous vous informons que votre dossier de candidature n°{{numeroDossier}} a "
                        + "été validé avec succès et qu'il est désormais en cours de présélection. Merci de bien "
                        + "vouloir procéder au règlement des frais de dossier depuis votre espace candidat afin de "
                        + "poursuivre votre parcours d'admission." + signature);
            }
            case REJET_DOSSIER -> {
                t.setSujet("Votre dossier de candidature n'a pas été retenu");
                t.setCorps("Bonjour {{nomCandidat}},\n\nAprès examen de votre dossier de candidature n°{{numeroDossier}}, nous "
                        + "sommes au regret de vous informer qu'il n'a pas été retenu.\n\nMotif : {{motif}}\n\nNous "
                        + "vous remercions de l'intérêt que vous avez porté à notre établissement et vous souhaitons "
                        + "plein succès dans la suite de vos démarches." + signature);
            }
            case PAIEMENT_SUCCES -> {
                t.setSujet("Confirmation de paiement des frais de dossier");
                t.setCorps("Bonjour {{nomCandidat}},\n\nNous vous confirmons la bonne réception du paiement des frais de dossier "
                        + "pour votre candidature n°{{numeroDossier}}. Votre dossier peut désormais être traité dans "
                        + "le cadre du processus de présélection." + signature);
            }
            case CONVOCATION_TEST -> {
                t.setSujet("Convocation au test académique");
                t.setCorps("Bonjour {{nomCandidat}},\n\nNous avons le plaisir de vous convoquer au test académique "
                        + "\"{{nomEpreuve}}\", qui se tiendra le {{date}}.\n\nLieu / Lien de connexion : "
                        + "{{lieuOuLien}}\n\nNous vous remercions de vous présenter quelques minutes avant l'heure "
                        + "prévue et de vous munir d'une pièce d'identité." + signature);
            }
            case CONVOCATION_ENTRETIEN -> {
                t.setSujet("Convocation à l'entretien");
                t.setCorps("Bonjour {{nomCandidat}},\n\nNous avons le plaisir de vous convoquer à un entretien dans le cadre de "
                        + "votre candidature, qui se tiendra le {{date}}.\n\nLieu / Lien de connexion : "
                        + "{{lieuOuLien}}\n\nNous vous remercions de vous présenter quelques minutes avant l'heure "
                        + "prévue et de vous munir d'une pièce d'identité." + signature);
            }
            case ADMISSION_TEST -> {
                t.setSujet("Admission au Test Académique");
                t.setCorps("Bonjour {{nomCandidat}},\n\nNous avons le plaisir de vous informer que votre dossier de candidature "
                        + "n°{{numeroDossier}} a été retenu à l'issue de la présélection. Vous êtes à présent "
                        + "convoqué(e) au Test Académique ; les modalités vous seront communiquées séparément."
                        + signature);
            }
            case ADMISSION_ENTRETIEN -> {
                t.setSujet("Admission à l'Entretien");
                t.setCorps("Bonjour {{nomCandidat}},\n\nNous avons le plaisir de vous informer que vous avez été admis(e) à l'issue "
                        + "du Test Académique. Votre dossier de candidature n°{{numeroDossier}} passe désormais à "
                        + "l'étape de l'entretien ; les modalités vous seront communiquées séparément." + signature);
            }
            case ADMISSION_FINALE -> {
                t.setSujet("Félicitations - Admission définitive à l'INPG");
                t.setCorps("Bonjour {{nomCandidat}},\n\nNous avons le plaisir de vous annoncer que votre candidature "
                        + "n°{{numeroDossier}} a été définitivement retenue. Toute l'équipe de l'INPG vous félicite "
                        + "pour votre admission et se réjouit de vous accueillir prochainement." + signature);
            }
            case LISTE_ATTENTE -> {
                t.setSujet("Votre candidature est placée sur liste complémentaire");
                t.setCorps("Bonjour {{nomCandidat}},\n\nÀ l'issue de l'entretien, votre candidature n°{{numeroDossier}} a été "
                        + "retenue par le jury, mais le nombre de places disponibles ne permet pas de vous admettre "
                        + "immédiatement. Vous êtes donc placé(e) sur liste complémentaire.\n\nVotre dossier reste "
                        + "actif : nous reviendrons vers vous dès qu'une place se libère. Aucune démarche n'est "
                        + "attendue de votre part d'ici là." + signature);
            }
            case NON_ADMISSION -> {
                t.setSujet("Résultat de votre candidature");
                t.setCorps("Bonjour {{nomCandidat}},\n\nAprès un examen attentif de votre candidature n°{{numeroDossier}}, nous "
                        + "sommes au regret de vous informer qu'elle n'a pas été retenue à l'issue du processus "
                        + "d'admission.\n\nNous vous remercions sincèrement de l'intérêt que vous avez porté à notre "
                        + "établissement et vous souhaitons plein succès dans la suite de votre parcours." + signature);
            }
            case DOCUMENT_CONFORME -> {
                t.setSujet("Document validé");
                t.setCorps("Bonjour {{nomCandidat}},\n\nNous vous informons que le document « {{typeDocument}} » transmis dans le "
                        + "cadre de votre dossier de candidature n°{{numeroDossier}} a été vérifié et jugé conforme."
                        + signature);
            }
            case DOCUMENT_NON_CONFORME -> {
                t.setSujet("Document à corriger");
                t.setCorps("Bonjour {{nomCandidat}},\n\nNous vous informons que le document « {{typeDocument}} » transmis dans le "
                        + "cadre de votre dossier de candidature n°{{numeroDossier}} n'a pas été jugé conforme. "
                        + "Merci de bien vouloir vous connecter à votre espace candidat afin de le remplacer dans "
                        + "les meilleurs délais." + signature);
            }
            case RAPPEL_DOSSIER_INCOMPLET -> {
                t.setSujet("Rappel : votre dossier de candidature est incomplet");
                t.setCorps("Bonjour {{nomCandidat}},\n\nNous vous rappelons que votre dossier de candidature n°{{numeroDossier}} "
                        + "est actuellement incomplet : {{piecesManquantes}}.\n\nMerci de bien vouloir vous "
                        + "connecter à votre espace candidat afin de compléter ou de remplacer les pièces "
                        + "manquantes dans les meilleurs délais, sous peine de retarder le traitement de votre "
                        + "dossier." + signature);
            }
            case RAPPEL_PAIEMENT -> {
                t.setSujet("Rappel : frais de dossier en attente de règlement");
                t.setCorps("Bonjour {{nomCandidat}},\n\nNous vous rappelons que votre dossier de candidature n°{{numeroDossier}} a "
                        + "été retenu, mais que les frais de dossier restent à ce jour impayés. Merci de bien "
                        + "vouloir procéder à leur règlement depuis votre espace candidat afin de poursuivre votre "
                        + "parcours d'admission." + signature);
            }
        }
        return t;
    }
}
