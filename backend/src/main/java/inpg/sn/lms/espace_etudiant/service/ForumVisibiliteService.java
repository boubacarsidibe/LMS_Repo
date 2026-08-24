package inpg.sn.lms.espace_etudiant.service;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.espace_etudiant.entity.ForumSujet;
import inpg.sn.lms.espace_etudiant.entity.PorteeForumSujet;
import inpg.sn.lms.espace_etudiant.repository.ForumSujetRepository;

/**
 * Visibilité des sujets du forum de l'espace étudiant, pour un étudiant ou un
 * formateur.
 *
 * <p>Cette logique existait déjà, mais seulement dans {@code ForumController}
 * (méthode privée {@code sujetsVisibles}) : {@code EspaceEtudiantController}, qui
 * sert les mêmes entités sous un préfixe différent, listait tous les sujets sans
 * aucun filtre ({@code forumSujetRepository.findAll()}) — un étudiant pouvait donc
 * lire l'intégralité des sujets rattachés à des cours qui ne sont pas les siens en
 * appelant directement {@code GET /api/espace-etudiant/sujets}. Centraliser la
 * question ici, comme le reste du dépôt le fait déjà pour d'autres questions
 * métier (cf. CLAUDE.md §3), corrige ce défaut sans le recréer ailleurs.
 *
 * <p>Ne sert jamais de sujet de l'espace alumni ({@code portee = ALUMNI}) : cette
 * classe répond uniquement à « que voit un étudiant ou un formateur ? ». L'espace
 * alumni a son propre contrôleur et sa propre visibilité, plus simple (un seul
 * salon plat, pas de notion de cours).
 */
@Service
public class ForumVisibiliteService {

    private final ForumSujetRepository sujetRepository;
    private final UtilisateurCourantService utilisateurCourant;
    private final HabilitationCoursService habilitation;

    public ForumVisibiliteService(ForumSujetRepository sujetRepository,
            UtilisateurCourantService utilisateurCourant, HabilitationCoursService habilitation) {
        this.sujetRepository = sujetRepository;
        this.utilisateurCourant = utilisateurCourant;
        this.habilitation = habilitation;
    }

    /** Sujets accessibles au connecté, épinglés en tête. */
    @Transactional(readOnly = true)
    public List<ForumSujet> sujetsVisibles(Authentication authentication) {
        Etudiant etudiant = utilisateurCourant.etudiant(authentication);
        if (etudiant == null) {
            // Formateur ou personnel : accès à l'ensemble des salons de l'espace étudiant.
            return sujetRepository.findVisiblesEtudiantFormateur();
        }
        List<Long> coursIds = habilitation.idsCoursAutorises(etudiant);
        return coursIds.isEmpty() ? sujetRepository.findGeneraux() : sujetRepository.findVisiblesPour(coursIds);
    }

    /** Un sujet donné est-il accessible au connecté ? */
    public boolean estVisible(ForumSujet sujet, Authentication authentication) {
        if (sujet.getPortee() == PorteeForumSujet.ALUMNI) {
            // Défense en profondeur : un alumni ne peut de toute façon pas atteindre ce
            // contrôleur (SecurityConfig/@PreAuthorize le bloquent déjà), mais un étudiant
            // ou un formateur ne doit pas non plus pouvoir lire un sujet alumni en devinant
            // son identifiant.
            return false;
        }
        if (sujet.getCours() == null) {
            return true;
        }
        Etudiant etudiant = utilisateurCourant.etudiant(authentication);
        if (etudiant == null) {
            return true;
        }
        return habilitation.estAutorise(sujet.getCours().getId(), etudiant);
    }
}
