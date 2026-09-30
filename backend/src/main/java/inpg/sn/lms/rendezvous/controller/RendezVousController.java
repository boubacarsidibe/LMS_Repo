package inpg.sn.lms.rendezvous.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.espace_etudiant.entity.Notification;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;
import inpg.sn.lms.rendezvous.dto.CreneauCreateRequest;
import inpg.sn.lms.rendezvous.dto.DemandeRendezVousRequest;
import inpg.sn.lms.rendezvous.dto.ReponsePropositionRequest;
import inpg.sn.lms.rendezvous.dto.ReponseRendezVousRequest;
import inpg.sn.lms.rendezvous.entity.CreneauDisponibilite;
import inpg.sn.lms.rendezvous.entity.RendezVous;
import inpg.sn.lms.rendezvous.enums.EnumModaliteRendezVous;
import inpg.sn.lms.rendezvous.enums.EnumStatutRendezVous;
import inpg.sn.lms.rendezvous.repository.CreneauDisponibiliteRepository;
import inpg.sn.lms.rendezvous.repository.RendezVousRepository;
import inpg.sn.lms.rendezvous.service.RendezVousService;

/**
 * Gestion des rendez-vous (TDR §J) : « Chaque utilisateur pourra consulter le
 * profil des membres du personnel de l'INPG et envoyer une demande de
 * rendez-vous en choisissant un créneau disponible dans leur emploi du
 * temps. [...] Les membres du personnel auront la possibilité d'accepter,
 * de refuser ou de proposer une autre date. »
 *
 * <p>Ouvert à tout utilisateur connecté (voir {@code SecurityConfig}) pour la
 * consultation et la demande — sauf au personnel de l'INPG lui-même
 * ({@link RendezVousService#ROLES_PERSONNEL}), qui reçoit les demandes plutôt
 * que d'en émettre : {@code demanderRendezVous} le refuse explicitement. La
 * création/gestion de créneaux et la réponse aux demandes restent, elles,
 * réservées à ces trois rôles internes.
 */
@RestController
@RequestMapping("/api/rendez-vous")
public class RendezVousController {

    private final CreneauDisponibiliteRepository creneauRepository;
    private final RendezVousRepository rendezVousRepository;
    private final RendezVousService rendezVousService;
    private final UtilisateurCourantService utilisateurCourant;

    public RendezVousController(CreneauDisponibiliteRepository creneauRepository,
            RendezVousRepository rendezVousRepository, RendezVousService rendezVousService,
            UtilisateurCourantService utilisateurCourant) {
        this.creneauRepository = creneauRepository;
        this.rendezVousRepository = rendezVousRepository;
        this.rendezVousService = rendezVousService;
        this.utilisateurCourant = utilisateurCourant;
    }

    // ===================== PERSONNEL & CRÉNEAUX =====================

    /**
     * Liste des membres du personnel consultables pour une prise de rendez-vous.
     *
     * <p>{@code scope=EXTERNE} restreint aux responsables pédagogique et
     * admission : ce sont les deux seuls interlocuteurs des écoles partenaires
     * et des entreprises (décision client), qui n'ont donc pas à voir tout
     * l'annuaire du personnel, formateurs compris.
     */
    @GetMapping("/personnel")
    @Transactional(readOnly = true)
    public ResponseEntity<?> listerPersonnel(@RequestParam(required = false) String scope) {
        if ("EXTERNE".equalsIgnoreCase(scope)) {
            return ResponseEntity.ok(rendezVousService.listerInterlocuteursExternes());
        }
        return ResponseEntity.ok(rendezVousService.listerPersonnel());
    }

    @GetMapping("/personnel/{id}/creneaux")
    @Transactional(readOnly = true)
    public ResponseEntity<?> creneauxLibres(@PathVariable Long id) {
        return ResponseEntity.ok(rendezVousService.creneauxLibres(id));
    }

    /** Créneaux (libres et réservés) du membre du personnel connecté — son propre agenda. */
    @GetMapping("/mes-creneaux")
    @PreAuthorize("hasAnyRole('FORMATEUR', 'RESPONSABLE_PEDAGOGIQUE', 'RESPONSABLE_ADMISSION')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> mesCreneaux(Authentication authentication) {
        User utilisateur = utilisateurCourant.utilisateur(authentication);
        if (utilisateur == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(rendezVousService.mesCreneaux(utilisateur.getId()));
    }

    @PostMapping("/creneaux")
    @PreAuthorize("hasAnyRole('FORMATEUR', 'RESPONSABLE_PEDAGOGIQUE', 'RESPONSABLE_ADMISSION')")
    @Transactional
    public ResponseEntity<?> creerCreneau(@RequestBody CreneauCreateRequest requete, Authentication authentication) {
        if (requete.dateHeureDebut() == null || requete.dateHeureFin() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le début et la fin du créneau sont requis."));
        }
        if (!requete.dateHeureFin().isAfter(requete.dateHeureDebut())) {
            return ResponseEntity.badRequest().body(Map.of("message", "La fin doit être après le début."));
        }
        if (requete.dateHeureDebut().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le créneau doit être dans le futur."));
        }
        User personnel = utilisateurCourant.utilisateur(authentication);
        if (personnel == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        CreneauDisponibilite creneau = new CreneauDisponibilite(personnel, requete.dateHeureDebut(), requete.dateHeureFin());
        return ResponseEntity.status(HttpStatus.CREATED).body(rendezVousService.toCreneauDto(creneauRepository.save(creneau)));
    }

    @DeleteMapping("/creneaux/{id}")
    @PreAuthorize("hasAnyRole('FORMATEUR', 'RESPONSABLE_PEDAGOGIQUE', 'RESPONSABLE_ADMISSION')")
    @Transactional
    public ResponseEntity<?> supprimerCreneau(@PathVariable Long id, Authentication authentication) {
        CreneauDisponibilite creneau = creneauRepository.findById(id).orElse(null);
        if (creneau == null) {
            return ResponseEntity.notFound().build();
        }
        User utilisateur = utilisateurCourant.utilisateur(authentication);
        if (utilisateur == null || !creneau.getPersonnel().getId().equals(utilisateur.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Seul le propriétaire du créneau peut le retirer."));
        }
        if (creneau.isReserve()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Ce créneau fait l'objet d'une demande de rendez-vous, il ne peut pas être retiré."));
        }
        creneauRepository.delete(creneau);
        return ResponseEntity.noContent().build();
    }

    // ===================== DEMANDES =====================

    @PostMapping("/demandes")
    @Transactional
    public ResponseEntity<?> demanderRendezVous(@RequestBody DemandeRendezVousRequest requete, Authentication authentication) {
        if (requete.creneauId() == null || estVide(requete.modalite())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le créneau et la modalité sont requis."));
        }
        EnumModaliteRendezVous modalite;
        try {
            modalite = EnumModaliteRendezVous.valueOf(requete.modalite());
        } catch (IllegalArgumentException invalide) {
            return ResponseEntity.badRequest().body(Map.of("message", "La modalité doit valoir EN_LIGNE ou PRESENTIEL."));
        }
        CreneauDisponibilite creneau = creneauRepository.findById(requete.creneauId()).orElse(null);
        if (creneau == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Créneau introuvable."));
        }
        if (creneau.isReserve()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Ce créneau n'est plus disponible."));
        }
        User demandeur = utilisateurCourant.utilisateur(authentication);
        if (demandeur == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // Le personnel de l'INPG gère les demandes, il ne les fait jamais : sans ce
        // garde-fou serveur, l'interface seule empêchait un formateur ou un
        // responsable de solliciter un collègue — un appel direct à l'API restait possible.
        if (rendezVousService.estPersonnel(demandeur)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message",
                    "Le personnel de l'INPG ne demande pas de rendez-vous : il gère les demandes reçues depuis « Mon agenda »."));
        }
        if (demandeur.getId().equals(creneau.getPersonnel().getId())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vous ne pouvez pas demander un rendez-vous avec vous-même."));
        }
        // Écoles partenaires et entreprises : leurs interlocuteurs sont les
        // responsables pédagogique et admission, jamais un formateur. On le
        // vérifie côté serveur, la liste envoyée par le navigateur ne faisant pas foi.
        if (rendezVousService.estDemandeurExterne(demandeur)
                && !rendezVousService.estInterlocuteurExterne(creneau.getPersonnel())) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    "Les demandes de rendez-vous des écoles et des entreprises sont adressées au responsable pédagogique ou au responsable admission."));
        }
        creneau.setReserve(true);
        creneauRepository.save(creneau);
        RendezVous rdv = new RendezVous(creneau, demandeur, modalite,
                estVide(requete.lieuOuLien()) ? null : requete.lieuOuLien().trim(),
                estVide(requete.motif()) ? null : requete.motif().trim());
        rdv = rendezVousRepository.save(rdv);
        rendezVousService.notifier(creneau.getPersonnel(),
                utilisateurCourant.nomAffiche(demandeur) + " a demandé un rendez-vous avec vous.",
                Notification.RENDEZ_VOUS_DEMANDE);
        return ResponseEntity.status(HttpStatus.CREATED).body(rendezVousService.toRendezVousDto(rdv));
    }

    @GetMapping("/mes-demandes")
    @Transactional(readOnly = true)
    public ResponseEntity<?> mesDemandes(Authentication authentication) {
        User utilisateur = utilisateurCourant.utilisateur(authentication);
        if (utilisateur == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(rendezVousRepository.findByDemandeurIdOrderByDateCreationDesc(utilisateur.getId())
                .stream().map(rendezVousService::toRendezVousDto).toList());
    }

    @GetMapping("/demandes-recues")
    @PreAuthorize("hasAnyRole('FORMATEUR', 'RESPONSABLE_PEDAGOGIQUE', 'RESPONSABLE_ADMISSION')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> demandesRecues(Authentication authentication) {
        User utilisateur = utilisateurCourant.utilisateur(authentication);
        if (utilisateur == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(rendezVousRepository.findByCreneau_Personnel_IdOrderByDateCreationDesc(utilisateur.getId())
                .stream().map(rendezVousService::toRendezVousDto).toList());
    }

    /** Le demandeur retire sa propre demande tant qu'elle est encore en attente de réponse. */
    @PostMapping("/{id}/annuler")
    @Transactional
    public ResponseEntity<?> annulerDemande(@PathVariable Long id, Authentication authentication) {
        RendezVous rdv = rendezVousRepository.findById(id).orElse(null);
        if (rdv == null) {
            return ResponseEntity.notFound().build();
        }
        User utilisateur = utilisateurCourant.utilisateur(authentication);
        if (utilisateur == null || !rdv.getDemandeur().getId().equals(utilisateur.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Seul le demandeur peut annuler sa demande."));
        }
        if (rdv.getStatut() != EnumStatutRendezVous.EN_ATTENTE) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Cette demande a déjà reçu une réponse."));
        }
        rdv.setStatut(EnumStatutRendezVous.ANNULE);
        liberer(rdv.getCreneau());
        return ResponseEntity.ok(rendezVousService.toRendezVousDto(rendezVousRepository.save(rdv)));
    }

    // ===================== RÉPONSES DU PERSONNEL =====================

    /** {@code action} : ACCEPTER, REFUSER ou PROPOSER — réservé au personnel titulaire du créneau. */
    @PostMapping("/{id}/reponse")
    @PreAuthorize("hasAnyRole('FORMATEUR', 'RESPONSABLE_PEDAGOGIQUE', 'RESPONSABLE_ADMISSION')")
    @Transactional
    public ResponseEntity<?> repondre(@PathVariable Long id, @RequestBody ReponseRendezVousRequest requete,
            Authentication authentication) {
        RendezVous rdv = rendezVousRepository.findById(id).orElse(null);
        if (rdv == null) {
            return ResponseEntity.notFound().build();
        }
        User utilisateur = utilisateurCourant.utilisateur(authentication);
        if (utilisateur == null || !rdv.getCreneau().getPersonnel().getId().equals(utilisateur.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Seul le titulaire du créneau peut répondre à cette demande."));
        }
        if (rdv.getStatut() != EnumStatutRendezVous.EN_ATTENTE) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Cette demande a déjà reçu une réponse."));
        }
        String commentaire = estVide(requete.commentaire()) ? null : requete.commentaire().trim();
        String messageDemandeur;
        switch (requete.action() == null ? "" : requete.action()) {
            case "ACCEPTER" -> {
                rdv.setStatut(EnumStatutRendezVous.ACCEPTE);
                messageDemandeur = "Votre demande de rendez-vous a été acceptée.";
            }
            case "REFUSER" -> {
                rdv.setStatut(EnumStatutRendezVous.REFUSE);
                liberer(rdv.getCreneau());
                messageDemandeur = "Votre demande de rendez-vous a été refusée.";
            }
            case "PROPOSER" -> {
                if (requete.nouvelleDateHeureDebut() == null || requete.nouvelleDateHeureFin() == null
                        || !requete.nouvelleDateHeureFin().isAfter(requete.nouvelleDateHeureDebut())) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("message", "La nouvelle date proposée est invalide."));
                }
                rdv.setStatut(EnumStatutRendezVous.PROPOSITION_AUTRE_DATE);
                rdv.setDateHeureProposeeDebut(requete.nouvelleDateHeureDebut());
                rdv.setDateHeureProposeeFin(requete.nouvelleDateHeureFin());
                messageDemandeur = "Une autre date vous a été proposée pour votre rendez-vous.";
            }
            default -> {
                return ResponseEntity.badRequest().body(Map.of("message", "L'action doit valoir ACCEPTER, REFUSER ou PROPOSER."));
            }
        }
        rdv.setCommentaireReponse(commentaire);
        rdv.setDateReponse(LocalDateTime.now());
        rdv = rendezVousRepository.save(rdv);
        rendezVousService.notifier(rdv.getDemandeur(), messageDemandeur, Notification.RENDEZ_VOUS_REPONSE);
        return ResponseEntity.ok(rendezVousService.toRendezVousDto(rdv));
    }

    /** Le demandeur accepte ou refuse la contre-proposition du personnel. */
    @PostMapping("/{id}/reponse-proposition")
    @Transactional
    public ResponseEntity<?> repondreProposition(@PathVariable Long id, @RequestBody ReponsePropositionRequest requete,
            Authentication authentication) {
        RendezVous rdv = rendezVousRepository.findById(id).orElse(null);
        if (rdv == null) {
            return ResponseEntity.notFound().build();
        }
        User utilisateur = utilisateurCourant.utilisateur(authentication);
        if (utilisateur == null || !rdv.getDemandeur().getId().equals(utilisateur.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Seul le demandeur peut répondre à cette proposition."));
        }
        if (rdv.getStatut() != EnumStatutRendezVous.PROPOSITION_AUTRE_DATE) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Aucune proposition en attente sur cette demande."));
        }
        rdv.setStatut(requete.accepter() ? EnumStatutRendezVous.ACCEPTE : EnumStatutRendezVous.REFUSE);
        rdv.setDateReponse(LocalDateTime.now());
        if (!requete.accepter()) {
            liberer(rdv.getCreneau());
        }
        rdv = rendezVousRepository.save(rdv);
        rendezVousService.notifier(rdv.getCreneau().getPersonnel(),
                requete.accepter() ? "Votre proposition de date a été acceptée." : "Votre proposition de date a été refusée.",
                Notification.RENDEZ_VOUS_REPONSE);
        return ResponseEntity.ok(rendezVousService.toRendezVousDto(rdv));
    }

    private void liberer(CreneauDisponibilite creneau) {
        creneau.setReserve(false);
        creneauRepository.save(creneau);
    }

    private boolean estVide(String valeur) {
        return valeur == null || valeur.trim().isEmpty();
    }
}
