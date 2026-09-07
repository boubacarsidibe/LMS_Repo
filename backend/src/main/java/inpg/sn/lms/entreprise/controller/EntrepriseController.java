package inpg.sn.lms.entreprise.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import inpg.sn.lms.auth.entity.Entreprise;
import inpg.sn.lms.auth.entity.Role;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.EntrepriseRepository;
import inpg.sn.lms.auth.repository.RoleRepository;
import inpg.sn.lms.entreprise.dto.DemandeEntrepriseCreateRequest;
import inpg.sn.lms.entreprise.dto.DemandeEntrepriseDto;
import inpg.sn.lms.entreprise.dto.MessageEntrepriseDto;
import inpg.sn.lms.entreprise.entity.DemandeEntreprise;
import inpg.sn.lms.entreprise.entity.FicheTechniqueModule;
import inpg.sn.lms.entreprise.entity.MessageEntreprise;
import inpg.sn.lms.entreprise.enums.EnumStatutDemandeEntreprise;
import inpg.sn.lms.entreprise.enums.EnumStatutFicheModule;
import inpg.sn.lms.entreprise.enums.EnumTypeDemandeEntreprise;
import inpg.sn.lms.entreprise.repository.DemandeEntrepriseRepository;
import inpg.sn.lms.entreprise.repository.FicheTechniqueModuleRepository;
import inpg.sn.lms.entreprise.repository.MessageEntrepriseRepository;
import inpg.sn.lms.entreprise.service.EntrepriseNotificationService;
import inpg.sn.lms.entreprise.service.FicheTechniqueModuleService;
import inpg.sn.lms.espace_etudiant.entity.Formation;
import inpg.sn.lms.espace_etudiant.repository.FormationRepository;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;

/**
 * Espace Entreprise (TDR §H) : soumission de besoins de formation et de
 * demandes de devis, consultation du catalogue, messagerie privée avec la
 * pédagogie, suivi de ses propres demandes.
 *
 * <p>Vit sous {@code /api/entreprise/**}, {@code authenticated()} dans
 * {@code SecurityConfig} — pas {@code hasRole}, car {@code ROLE_ENTREPRISE}
 * n'est accordé qu'à la première soumission réussie, même patron que
 * {@code EcoleAccreditationController}.
 */
@RestController
@RequestMapping("/api/entreprise")
public class EntrepriseController {

    /** Même règle qu'ailleurs : un compte ne détient qu'un seul rôle ouvrant un espace à la fois. */
    private static final Set<String> ROLES_OUVRANT_UN_ESPACE = Set.of(
            "ROLE_ETUDIANT", "ROLE_FORMATEUR", "ROLE_RESPONSABLE_PEDAGOGIQUE",
            "ROLE_RESPONSABLE_ADMISSION", "ROLE_ALUMNI", "ROLE_EVALUATEUR_ACCREDITATION",
            "ROLE_CANDIDAT", "ROLE_ECOLE_FORMATION");

    private final EntrepriseRepository entrepriseRepository;
    private final DemandeEntrepriseRepository demandeRepository;
    private final MessageEntrepriseRepository messageRepository;
    private final FormationRepository formationRepository;
    private final FicheTechniqueModuleRepository ficheRepository;
    private final FicheTechniqueModuleService ficheService;
    private final RoleRepository roleRepository;
    private final UtilisateurCourantService utilisateurCourant;
    private final EntrepriseNotificationService notificationService;

    public EntrepriseController(EntrepriseRepository entrepriseRepository,
            DemandeEntrepriseRepository demandeRepository, MessageEntrepriseRepository messageRepository,
            FormationRepository formationRepository, FicheTechniqueModuleRepository ficheRepository,
            FicheTechniqueModuleService ficheService, RoleRepository roleRepository,
            UtilisateurCourantService utilisateurCourant, EntrepriseNotificationService notificationService) {
        this.entrepriseRepository = entrepriseRepository;
        this.demandeRepository = demandeRepository;
        this.messageRepository = messageRepository;
        this.formationRepository = formationRepository;
        this.ficheRepository = ficheRepository;
        this.ficheService = ficheService;
        this.roleRepository = roleRepository;
        this.utilisateurCourant = utilisateurCourant;
        this.notificationService = notificationService;
    }

    /** Catalogue des formations publiées — même source que la vitrine publique. */
    @GetMapping("/catalogue")
    @Transactional(readOnly = true)
    public ResponseEntity<?> catalogue() {
        return ResponseEntity.ok(formationRepository.findByPublieeTrueOrderByIntituleAsc().stream()
                .map(f -> Map.of("id", f.getId(), "intitule", f.getIntitule(),
                        "description", f.getDescription() == null ? "" : f.getDescription(),
                        "domaine", f.getDomaine() == null ? "" : f.getDomaine()))
                .toList());
    }

    /**
     * Crée (si besoin) le profil entreprise et soumet une demande — besoin de
     * formation ou devis/session sur mesure selon {@code type}.
     */
    @PostMapping("/demandes")
    @Transactional
    public ResponseEntity<?> soumettreDemande(@RequestBody DemandeEntrepriseCreateRequest requete,
            Authentication authentication) {
        User user = utilisateurCourant.utilisateur(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (requete.entreprise() == null || estVide(requete.entreprise().raisonSociale())
                || estVide(requete.entreprise().adresse()) || estVide(requete.entreprise().telephone())) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "La raison sociale, l'adresse et le téléphone de l'entreprise sont obligatoires."));
        }
        if (estVide(requete.description())) {
            return ResponseEntity.badRequest().body(Map.of("message", "La description de la demande est obligatoire."));
        }
        EnumTypeDemandeEntreprise type;
        try {
            type = EnumTypeDemandeEntreprise.valueOf(requete.type());
        } catch (IllegalArgumentException | NullPointerException invalide) {
            return ResponseEntity.badRequest().body(Map.of("message", "Type de demande invalide."));
        }

        boolean dejaEntreprise = user.getRoles().stream().anyMatch(r -> "ROLE_ENTREPRISE".equals(r.getCode()));
        if (!dejaEntreprise) {
            Optional<Role> roleOuvrant = user.getRoles().stream()
                    .filter(r -> ROLES_OUVRANT_UN_ESPACE.contains(r.getCode()))
                    .findFirst();
            if (roleOuvrant.isPresent()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "message", "Ce compte donne déjà accès à l'espace « " + roleOuvrant.get().getLibelle()
                                + " ». Pour soumettre une demande entreprise, utilisez un compte distinct."));
            }
        }

        Entreprise entreprise = entrepriseRepository.findByUserId(user.getId()).orElse(null);
        var infos = requete.entreprise();
        if (entreprise == null) {
            entreprise = entrepriseRepository.save(new Entreprise(user, infos.raisonSociale().trim(),
                    estVide(infos.secteur()) ? null : infos.secteur().trim(), infos.adresse().trim(),
                    infos.telephone().trim()));
        } else {
            entreprise.setRaisonSociale(infos.raisonSociale().trim());
            entreprise.setSecteur(estVide(infos.secteur()) ? null : infos.secteur().trim());
            entreprise.setAdresse(infos.adresse().trim());
            entreprise.setTelephone(infos.telephone().trim());
            entrepriseRepository.save(entreprise);
        }

        Formation formation = requete.formationId() != null
                ? formationRepository.findById(requete.formationId()).orElse(null)
                : null;

        DemandeEntreprise demande = demandeRepository.save(new DemandeEntreprise(entreprise, type, formation,
                requete.description().trim(), requete.effectifConcerne(),
                estVide(requete.datesSouhaitees()) ? null : requete.datesSouhaitees().trim()));
        garantirRoleEntreprise(user);
        notificationService.notifierNouvelleDemande(entreprise.getRaisonSociale(), type);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(demande));
    }

    @GetMapping("/demandes")
    @Transactional(readOnly = true)
    public ResponseEntity<?> mesDemandes(Authentication authentication) {
        Entreprise entreprise = utilisateurCourant.entreprise(authentication);
        if (entreprise == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(demandeRepository.findByEntrepriseIdOrderByDateSoumissionDesc(entreprise.getId())
                .stream().map(EntrepriseController::toDto).toList());
    }

    // ===================== Fiche technique de module =====================

    /** Fiche technique transmise pour une de ses demandes (jamais un brouillon). */
    @GetMapping("/demandes/{id}/fiche")
    @Transactional(readOnly = true)
    public ResponseEntity<?> fiche(@PathVariable Long id, Authentication authentication) {
        FicheTechniqueModule fiche = ficheVisible(id, authentication);
        if (fiche == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ficheService.toDto(fiche));
    }

    /** L'entreprise accepte la fiche : la demande est clôturée. */
    @PutMapping("/demandes/{id}/fiche/accepter")
    @Transactional
    public ResponseEntity<?> accepterFiche(@PathVariable Long id, Authentication authentication) {
        FicheTechniqueModule fiche = ficheVisible(id, authentication);
        if (fiche == null) {
            return ResponseEntity.notFound().build();
        }
        if (fiche.getStatut() == EnumStatutFicheModule.ACCEPTEE) {
            return ResponseEntity.ok(ficheService.toDto(fiche));
        }
        fiche.setStatut(EnumStatutFicheModule.ACCEPTEE);
        fiche.setDateDecisionEntreprise(java.time.LocalDateTime.now());
        DemandeEntreprise demande = fiche.getDemande();
        demande.setStatut(EnumStatutDemandeEntreprise.CLOTUREE);
        demandeRepository.save(demande);
        ficheRepository.save(fiche);
        notificationService.notifierDecisionFiche(demande.getEntreprise().getRaisonSociale(), true, null);
        return ResponseEntity.ok(ficheService.toDto(fiche));
    }

    /** L'entreprise demande des ajustements : commentaire obligatoire. */
    @PutMapping("/demandes/{id}/fiche/ajustements")
    @Transactional
    public ResponseEntity<?> demanderAjustements(@PathVariable Long id, @RequestBody Map<String, String> corps,
            Authentication authentication) {
        String commentaire = corps.get("commentaire");
        if (estVide(commentaire)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Merci de préciser les ajustements souhaités."));
        }
        FicheTechniqueModule fiche = ficheVisible(id, authentication);
        if (fiche == null) {
            return ResponseEntity.notFound().build();
        }
        if (fiche.getStatut() == EnumStatutFicheModule.ACCEPTEE) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Cette fiche a déjà été acceptée."));
        }
        fiche.setStatut(EnumStatutFicheModule.AJUSTEMENTS_DEMANDES);
        fiche.setCommentaireEntreprise(commentaire.trim());
        fiche.setDateDecisionEntreprise(java.time.LocalDateTime.now());
        ficheRepository.save(fiche);
        notificationService.notifierDecisionFiche(fiche.getDemande().getEntreprise().getRaisonSociale(), false,
                commentaire.trim());
        return ResponseEntity.ok(ficheService.toDto(fiche));
    }

    /** Fiche d'une demande appartenant à l'entreprise connectée, si elle n'est plus un brouillon. */
    private FicheTechniqueModule ficheVisible(Long demandeId, Authentication authentication) {
        Entreprise entreprise = utilisateurCourant.entreprise(authentication);
        if (entreprise == null) {
            return null;
        }
        FicheTechniqueModule fiche = ficheRepository.findByDemandeId(demandeId).orElse(null);
        if (fiche == null || fiche.getStatut() == EnumStatutFicheModule.BROUILLON) {
            return null;
        }
        DemandeEntreprise demande = fiche.getDemande();
        if (demande == null || !demande.getEntreprise().getId().equals(entreprise.getId())) {
            return null;
        }
        return fiche;
    }

    /** Historique de la messagerie privée de l'entreprise connectée. */
    @GetMapping("/messages")
    @Transactional(readOnly = true)
    public ResponseEntity<?> mesMessages(Authentication authentication) {
        Entreprise entreprise = utilisateurCourant.entreprise(authentication);
        if (entreprise == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(messageRepository.findByEntrepriseIdOrderByDateCreationAsc(entreprise.getId())
                .stream().map(m -> toMessageDto(m, entreprise)).toList());
    }

    @PostMapping("/messages")
    @Transactional
    public ResponseEntity<?> envoyerMessage(@RequestBody Map<String, String> corps, Authentication authentication) {
        Entreprise entreprise = utilisateurCourant.entreprise(authentication);
        User user = utilisateurCourant.utilisateur(authentication);
        if (entreprise == null || user == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Aucun profil entreprise associé à ce compte."));
        }
        String contenu = corps.get("contenu");
        if (estVide(contenu)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le message ne peut pas être vide."));
        }
        MessageEntreprise message = messageRepository.save(new MessageEntreprise(entreprise, user, contenu.trim()));
        notificationService.notifierNouveauMessage(entreprise.getRaisonSociale());
        return ResponseEntity.status(HttpStatus.CREATED).body(toMessageDto(message, entreprise));
    }

    // ===================== Utilitaires =====================

    private void garantirRoleEntreprise(User user) {
        boolean dejaEntreprise = user.getRoles().stream().anyMatch(r -> "ROLE_ENTREPRISE".equals(r.getCode()));
        if (dejaEntreprise) {
            return;
        }
        Role role = roleRepository.findByCode("ROLE_ENTREPRISE");
        if (role == null) {
            throw new IllegalStateException("Le rôle ROLE_ENTREPRISE est introuvable.");
        }
        user.getRoles().clear();
        user.addRole(role);
    }

    static DemandeEntrepriseDto toDto(DemandeEntreprise demande) {
        return new DemandeEntrepriseDto(
                demande.getId(),
                demande.getType().name(),
                demande.getStatut().name(),
                demande.getEntreprise().getId(),
                demande.getEntreprise().getRaisonSociale(),
                demande.getFormation() != null ? demande.getFormation().getId() : null,
                demande.getFormation() != null ? demande.getFormation().getIntitule() : null,
                demande.getDescription(),
                demande.getEffectifConcerne(),
                demande.getDatesSouhaitees(),
                demande.getDateSoumission(),
                demande.getDateReponse(),
                demande.getReponseEquipe(),
                demande.getFiche() != null ? demande.getFiche().getStatut().name() : null);
    }

    static MessageEntrepriseDto toMessageDto(MessageEntreprise message, Entreprise entreprise) {
        boolean auteurEntreprise = message.getAuteur().getId().equals(entreprise.getUser().getId());
        return new MessageEntrepriseDto(
                message.getId(),
                message.getEntreprise().getId(),
                message.getAuteur().getId(),
                auteurEntreprise ? entreprise.getRaisonSociale() : "Équipe pédagogique INPG",
                auteurEntreprise,
                message.getContenu(),
                message.getDateCreation());
    }

    private boolean estVide(String valeur) {
        return valeur == null || valeur.isBlank();
    }
}
