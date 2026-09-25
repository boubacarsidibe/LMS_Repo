package inpg.sn.lms.espace_etudiant.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.entity.Formateur;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.auth.repository.FormateurRepository;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.Formation;
import inpg.sn.lms.espace_etudiant.entity.Promotion;
import inpg.sn.lms.espace_etudiant.entity.SupportCours;

/**
 * Regroupe les regles de cloisonnement des donnees : qui a le droit de voir quoi.
 *
 * <p>Ces regles etaient auparavant dispersees (et dupliquees) dans
 * EspaceEtudiantController. Les centraliser ici garantit qu'un correctif de
 * securite s'applique partout d'un coup, et rend la logique testable isolement.
 *
 * <p><strong>Un seul chemin fait autorite :</strong> Cours -> Formation -> Promotion
 * -> Etudiant. Ce service repondait auparavant a partir de la table
 * {@code cours_promotion}, tandis que {@link HabilitationCoursService} repondait a
 * partir de {@code formation_cours}. Les deux chemins avaient diverge au point de ne
 * plus designer aucun cours commun : un etudiant recevait 403 sur le cours portant
 * ses propres notes, et le meme PDF etait refuse par une route et servi par l'autre.
 * Les deux services lisent desormais la meme relation.
 */
@Service
public class PerimetreFormateurService {

    private final CoursService coursService;
    private final FormateurRepository formateurRepository;
    private final EtudiantRepository etudiantRepository;
    private final PromotionService promotionService;

    public PerimetreFormateurService(CoursService coursService, FormateurRepository formateurRepository,
            EtudiantRepository etudiantRepository, PromotionService promotionService) {
        this.coursService = coursService;
        this.formateurRepository = formateurRepository;
        this.etudiantRepository = etudiantRepository;
        this.promotionService = promotionService;
    }

    private static List<Long> idsDesFormations(java.util.stream.Stream<Formation> formations) {
        return formations.map(Formation::getId).filter(Objects::nonNull).distinct().sorted().toList();
    }

    /**
     * Promotions qui suivent ce cours, deduites de la ou des formations dont il fait
     * partie. Un cours hors de tout programme ne touche aucune promotion : la liste
     * est vide, ce qui est le comportement voulu et non un defaut.
     */
    @Transactional(readOnly = true)
    public List<Promotion> promotionsDuCours(Cours cours) {
        return promotionService.desFormationsDuCours(cours);
    }

    /** Variante identifiants de {@link #promotionsDuCours}. */
    @Transactional(readOnly = true)
    public List<Long> promotionIdsDuCours(Cours cours) {
        return promotionsDuCours(cours).stream().map(Promotion::getId).toList();
    }

    /** Identifiants des cours que ce formateur enseigne. */
    @Transactional(readOnly = true)
    public List<Long> coursIdsDuFormateur(Formateur formateur) {
        return coursService.getByFormateurId(formateur.getId()).stream().map(Cours::getId).toList();
    }

    /**
     * Promotions que le formateur enseigne, deduites des formations auxquelles ses
     * cours appartiennent. Un formateur sans cours, ou dont aucun cours n'est
     * rattache a un programme, obtient une liste vide (et ne verra donc aucun
     * etudiant) : c'est le comportement voulu, pas un bug.
     */
    @Transactional(readOnly = true)
    public List<Long> cohorteIdsDuFormateur(Formateur formateur) {
        List<Long> formationIds = idsDesFormations(
                coursService.getByFormateurId(formateur.getId()).stream()
                        .flatMap(cours -> cours.getFormations().stream()));
        if (formationIds.isEmpty()) {
            return List.of();
        }
        return promotionService.promotionIdsDesFormations(formationIds);
    }

    /**
     * Promotions visibles par un utilisateur quelconque, selon son profil :
     * un formateur voit celles de ses cours, un etudiant uniquement la sienne.
     * Un utilisateur sans profil (ni formateur ni etudiant) ne voit rien.
     */
    @Transactional(readOnly = true)
    public List<Long> cohorteIdsVisiblesPour(User user) {
        Formateur formateur = formateurRepository.findByUserId(user.getId()).orElse(null);
        if (formateur != null) {
            return cohorteIdsDuFormateur(formateur);
        }
        Etudiant etudiant = etudiantRepository.findByUserId(user.getId()).orElse(null);
        if (etudiant != null && etudiant.getPromotion() != null) {
            return List.of(etudiant.getPromotion().getId());
        }
        return List.of();
    }

    /** Vrai si ce formateur fait partie des enseignants du cours (droit de modification). */
    public boolean estFormateurDuCours(Formateur formateur, Cours cours) {
        return cours.getFormateurs().stream()
                .anyMatch(f -> f.getId().equals(formateur.getId()));
    }

    /**
     * Vrai si le cours figure au programme de la formation suivie par l'etudiant.
     *
     * <p>Ne declenche aucune requete : la comparaison porte sur la formation de la
     * promotion de l'etudiant, deja chargee, et sur les formations du cours.
     */
    public boolean etudiantSuitLeCours(Etudiant etudiant, Cours cours) {
        if (etudiant == null || cours == null || etudiant.getPromotion() == null
                || etudiant.getPromotion().getFormation() == null) {
            return false;
        }
        Long formationId = etudiant.getPromotion().getFormation().getId();
        return formationId != null && cours.getFormations().stream()
                .anyMatch(formation -> formationId.equals(formation.getId()));
    }

    /** Vrai si la ressource a une date de publication encore dans le futur. */
    public boolean supportEstProgramme(SupportCours support) {
        return support.getDatePublication() != null
                && support.getDatePublication().isAfter(LocalDate.now());
    }

    /**
     * Une ressource programmee reste invisible tant que sa date n'est pas atteinte,
     * SAUF pour les formateurs du cours concerne : ils doivent pouvoir relire et
     * corriger ce qu'ils ont planifie avant sa mise en ligne.
     *
     * @param formateurConnecte le formateur authentifie, ou null si l'appelant est un etudiant
     */
    public boolean peutVoirSupport(SupportCours support, Formateur formateurConnecte) {
        if (formateurConnecte != null && support.getCours() != null
                && estFormateurDuCours(formateurConnecte, support.getCours())) {
            return true;
        }
        return !supportEstProgramme(support);
    }

    /**
     * Une ressource est liee a un cours precis : elle ne doit etre visible que par
     * les gens concernes par CE cours, pas par toute la plateforme. Sans ce controle,
     * un etudiant de Finance voyait (et pouvait telecharger) les PDF de Data Science.
     *
     * <p>Un support sans cours rattache (ressource pedagogique generale, hors du
     * perimetre "cours") n'est pas concerne par cette regle : toujours accessible.
     *
     * @param mesCohorteIds promotions visibles de l'appelant, voir {@link #cohorteIdsVisiblesPour}
     */
    @Transactional(readOnly = true)
    public boolean coursDuSupportEstAccessible(SupportCours support, List<Long> mesCohorteIds,
            Formateur formateurConnecte) {
        Cours cours = support.getCours();
        if (cours == null) {
            return true;
        }
        if (formateurConnecte != null && estFormateurDuCours(formateurConnecte, cours)) {
            return true;
        }
        return promotionIdsDuCours(cours).stream().anyMatch(mesCohorteIds::contains);
    }
}
