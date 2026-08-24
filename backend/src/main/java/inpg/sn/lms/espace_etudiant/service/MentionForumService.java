package inpg.sn.lms.espace_etudiant.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.entity.Formateur;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.espace_etudiant.dto.MentionnableDto;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.ForumMention;
import inpg.sn.lms.espace_etudiant.entity.ForumMessage;
import inpg.sn.lms.espace_etudiant.entity.ForumSujet;
import inpg.sn.lms.espace_etudiant.entity.Notification;
import inpg.sn.lms.espace_etudiant.entity.PorteeForumSujet;
import inpg.sn.lms.espace_etudiant.entity.Promotion;
import inpg.sn.lms.espace_etudiant.repository.ForumMentionRepository;
import inpg.sn.lms.espace_etudiant.repository.NotificationRepository;

/**
 * Mentions au forum : qui peut être cité, et ce qu'il advient d'une citation.
 *
 * <p>Le périmètre est la question sensible. Pouvoir citer n'importe qui reviendrait à
 * publier l'annuaire de l'établissement dans une autocomplétion : chacun découvrirait
 * les noms de toutes les promotions. Les personnes proposées sont donc exactement
 * celles qui <strong>participent déjà au fil</strong> — les étudiants qui y ont accès
 * et les formateurs qui y enseignent. Citer quelqu'un ne lui donne aucun droit
 * nouveau : la mention suit l'accès, elle ne l'ouvre pas.
 */
@Service
public class MentionForumService {

    private final EtudiantRepository etudiantRepository;
    private final PromotionService promotionService;
    private final ForumMentionRepository mentionRepository;
    private final NotificationRepository notificationRepository;

    public MentionForumService(EtudiantRepository etudiantRepository, PromotionService promotionService,
            ForumMentionRepository mentionRepository, NotificationRepository notificationRepository) {
        this.etudiantRepository = etudiantRepository;
        this.promotionService = promotionService;
        this.mentionRepository = mentionRepository;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Personnes citables dans ce fil.
     *
     * <p>Fil rattaché à un cours : les étudiants des promotions qui le suivent, et ses
     * formateurs. Fil du salon général : la promotion de l'auteur de la demande, car
     * c'est là que se tient la conversation entre camarades.
     *
     * <p>Le demandeur est écarté de la liste : se citer soi-même n'a pas de sens, et
     * cela s'enverrait une notification.
     */
    @Transactional(readOnly = true)
    public List<MentionnableDto> citablesDans(ForumSujet sujet, User demandeur) {
        return citablesParId(sujet, demandeur).values().stream()
                .map(Citable::versDto)
                .sorted(Comparator.comparing(MentionnableDto::nom, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /**
     * Les mêmes personnes, mais en gardant l'utilisateur sous la main.
     *
     * <p>Une première version rappelait le dépôt des étudiants pour retrouver chaque
     * personne citée, puis retombait sur les formateurs quand la recherche échouait —
     * un formateur n'ayant pas de fiche étudiant. Deux chemins pour une même question,
     * et une requête par citation : la personne est ici retenue dès sa découverte.
     *
     * <p>Un fil de l'espace alumni ({@code portee = ALUMNI}) est traité à part : la
     * branche « salon général » ci-dessous retomberait sur la promotion de
     * l'auteur — or l'auteur est un alumni dont la ligne {@code Etudiant} existe
     * toujours (conversion sans suppression, cf. migration V12), ce qui proposerait
     * à tort ses anciens camarades encore étudiants comme citables. Ceux-ci n'ont
     * pourtant pas accès au fil : la citation ouvrirait un accès qu'elle ne fait que
     * suivre en principe (voir le javadoc de classe). Les citables y sont donc les
     * seuls participants déjà présents dans le fil, comme le principe l'exige.
     */
    private Map<Long, Citable> citablesParId(ForumSujet sujet, User demandeur) {
        if (sujet != null && sujet.getPortee() == PorteeForumSujet.ALUMNI) {
            return participantsDuFil(sujet, demandeur);
        }
        Map<Long, Citable> parId = new LinkedHashMap<>();
        Cours cours = sujet == null ? null : sujet.getCours();

        List<Promotion> promotions = cours != null
                ? promotionService.desFormationsDuCours(cours)
                : promotionsDe(demandeur);

        List<Long> promotionIds = promotions.stream()
                .map(Promotion::getId)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (!promotionIds.isEmpty()) {
            for (Etudiant etudiant : etudiantRepository.findByPromotion_IdIn(promotionIds)) {
                ajouter(parId, etudiant.getUser(),
                        (etudiant.getPrenom() + " " + etudiant.getNom()).trim(), "Étudiant");
            }
        }

        if (cours != null) {
            for (Formateur formateur : cours.getFormateurs()) {
                ajouter(parId, formateur.getUser(),
                        (formateur.getPrenom() + " " + formateur.getNom()).trim(), "Formateur");
            }
        }

        if (demandeur != null) {
            parId.remove(demandeur.getId());
        }
        return parId;
    }

    /**
     * Citables d'un fil alumni : les auteurs des messages déjà postés, à
     * l'exclusion du demandeur. Pas d'annuaire alumni consulté ici — seulement
     * ceux qui participent déjà, exactement le principe de cette classe.
     */
    private Map<Long, Citable> participantsDuFil(ForumSujet sujet, User demandeur) {
        Map<Long, Citable> parId = new LinkedHashMap<>();
        for (ForumMessage message : sujet.getMessages()) {
            User auteur = message.getAuteur();
            if (auteur == null) {
                continue;
            }
            // La ligne Etudiant d'un alumni n'est pas supprimée à la conversion
            // (migration V12) : elle reste la source du nom affiché, comme pour un
            // étudiant actif.
            String nom = etudiantRepository.findByUserId(auteur.getId())
                    .map(etudiant -> (etudiant.getPrenom() + " " + etudiant.getNom()).trim())
                    .orElseGet(() -> nomDe(auteur));
            ajouter(parId, auteur, nom, "Alumni");
        }
        if (demandeur != null) {
            parId.remove(demandeur.getId());
        }
        return parId;
    }

    /** Une personne citable, avec l'utilisateur qu'il faudra notifier. */
    private record Citable(User user, String nom, String role) {
        MentionnableDto versDto() {
            return new MentionnableDto(user.getId(), nom, role);
        }
    }

    /** Promotion de l'étudiant qui demande ; vide pour un formateur, qui n'en a pas. */
    private List<Promotion> promotionsDe(User demandeur) {
        if (demandeur == null) {
            return List.of();
        }
        return etudiantRepository.findByUserId(demandeur.getId())
                .map(Etudiant::getPromotion)
                .map(List::of)
                .orElse(List.of());
    }

    private void ajouter(Map<Long, Citable> parId, User user, String nom, String role) {
        if (user == null || user.getId() == null || parId.containsKey(user.getId())) {
            return;
        }
        parId.put(user.getId(), new Citable(user, nom, role));
    }

    /**
     * Enregistre les citations d'un message et prévient les personnes visées.
     *
     * <p>Les identifiants reçus sont <strong>recoupés avec le périmètre du fil</strong> :
     * sans cela, un identifiant glissé à la main dans la requête notifierait n'importe
     * quel utilisateur de l'établissement, en lui montrant au passage un extrait d'un
     * fil auquel il n'a pas accès.
     *
     * <p>L'auteur est écarté : personne n'a besoin d'être prévenu de son propre message.
     */
    @Transactional
    public List<ForumMention> enregistrer(ForumMessage message, List<Long> citesDemandes, User auteur) {
        if (citesDemandes == null || citesDemandes.isEmpty()) {
            return List.of();
        }
        Map<Long, Citable> autorises = citablesParId(message.getSujet(), auteur);

        List<ForumMention> posees = new ArrayList<>();
        for (Long id : citesDemandes.stream().distinct().toList()) {
            Citable citable = id == null ? null : autorises.get(id);
            if (citable == null) {
                continue;
            }
            User cite = citable.user();
            posees.add(mentionRepository.save(new ForumMention(message, cite)));
            notificationRepository.save(new Notification(
                    cite,
                    nomDe(auteur) + " vous a cité dans « " + message.getSujet().getTitre() + " »",
                    Notification.MENTION_FORUM,
                    "/espace-etudiant/forum/" + message.getSujet().getId()));
        }
        return posees;
    }


    private String nomDe(User user) {
        if (user == null) {
            return "Quelqu'un";
        }
        return user.getUsername() == null ? "Quelqu'un" : user.getUsername().replace('_', ' ');
    }
}
