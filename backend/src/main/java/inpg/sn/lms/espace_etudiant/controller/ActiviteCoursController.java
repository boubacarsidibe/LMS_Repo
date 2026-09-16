package inpg.sn.lms.espace_etudiant.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.entity.Formateur;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.espace_etudiant.entity.ActiviteCours;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.Notification;
import inpg.sn.lms.espace_etudiant.entity.RemiseActivite;
import inpg.sn.lms.espace_etudiant.repository.ActiviteCoursRepository;
import inpg.sn.lms.espace_etudiant.repository.CoursRepository;
import inpg.sn.lms.espace_etudiant.repository.NotificationRepository;
import inpg.sn.lms.espace_etudiant.repository.RemiseActiviteRepository;
import inpg.sn.lms.espace_etudiant.service.HabilitationCoursService;
import inpg.sn.lms.espace_etudiant.service.PerimetreFormateurService;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;

/**
 * Devoirs et évaluations des cours. Le responsable pédagogique attribue le cours
 * et ses étudiants ; le formateur publie puis corrige uniquement dans son périmètre.
 */
@RestController
@RequestMapping("/api/espace-etudiant")
public class ActiviteCoursController {
    private final ActiviteCoursRepository activites;
    private final RemiseActiviteRepository remises;
    private final CoursRepository coursRepository;
    private final EtudiantRepository etudiantRepository;
    private final NotificationRepository notifications;
    private final UtilisateurCourantService utilisateurCourant;
    private final PerimetreFormateurService perimetre;
    private final HabilitationCoursService habilitation;

    public ActiviteCoursController(ActiviteCoursRepository activites, RemiseActiviteRepository remises,
            CoursRepository coursRepository, EtudiantRepository etudiantRepository,
            NotificationRepository notifications, UtilisateurCourantService utilisateurCourant,
            PerimetreFormateurService perimetre, HabilitationCoursService habilitation) {
        this.activites = activites;
        this.remises = remises;
        this.coursRepository = coursRepository;
        this.etudiantRepository = etudiantRepository;
        this.notifications = notifications;
        this.utilisateurCourant = utilisateurCourant;
        this.perimetre = perimetre;
        this.habilitation = habilitation;
    }

    @GetMapping("/formateur/cours/{coursId}/activites")
    @PreAuthorize("hasRole('FORMATEUR')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> activitesDuFormateur(@PathVariable Long coursId, Authentication authentication) {
        Cours cours = coursDuFormateur(coursId, authentication);
        return cours == null ? ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cours non attribué.")
                : ResponseEntity.ok(activites.findByCoursIdOrderByDatePublicationDesc(cours.getId())
                        .stream().map(this::dto).toList());
    }

    @PostMapping("/formateur/cours/{coursId}/activites")
    @PreAuthorize("hasRole('FORMATEUR')")
    @Transactional
    public ResponseEntity<?> creer(@PathVariable Long coursId, @RequestBody ActiviteRequest request,
            Authentication authentication) {
        Cours cours = coursDuFormateur(coursId, authentication);
        Formateur formateur = utilisateurCourant.formateur(authentication);
        if (cours == null || formateur == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        String erreur = valider(request);
        if (erreur != null) return ResponseEntity.badRequest().body(java.util.Map.of("message", erreur));
        ActiviteCours activite = new ActiviteCours();
        activite.setCours(cours);
        activite.setAuteur(formateur);
        appliquer(activite, request);
        ActiviteCours creee = activites.save(activite);
        if (!creee.getDatePublication().isAfter(LocalDateTime.now())) notifierPublication(creee);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto(creee));
    }

    @PutMapping("/formateur/activites/{id}")
    @PreAuthorize("hasRole('FORMATEUR')")
    @Transactional
    public ResponseEntity<?> modifier(@PathVariable Long id, @RequestBody ActiviteRequest request,
            Authentication authentication) {
        ActiviteCours activite = activites.findById(id).orElse(null);
        if (activite == null || coursDuFormateur(activite.getCours().getId(), authentication) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String erreur = valider(request);
        if (erreur != null) return ResponseEntity.badRequest().body(java.util.Map.of("message", erreur));
        boolean devientPubliee = activite.getDatePublication().isAfter(LocalDateTime.now())
                && !request.datePublication().isAfter(LocalDateTime.now());
        appliquer(activite, request);
        ActiviteCours enregistree = activites.save(activite);
        if (devientPubliee) notifierPublication(enregistree);
        return ResponseEntity.ok(dto(enregistree));
    }

    @GetMapping("/cours/{coursId}/activites")
    @PreAuthorize("hasRole('ETUDIANT')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> activitesEtudiant(@PathVariable Long coursId, Authentication authentication) {
        Etudiant etudiant = utilisateurCourant.etudiant(authentication);
        if (etudiant == null || !habilitation.estAutorise(coursId, etudiant)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(activites.findByCoursIdAndDatePublicationLessThanEqualOrderByDatePublicationDesc(
                coursId, LocalDateTime.now()).stream().map(this::dto).toList());
    }

    @PostMapping("/activites/{id}/remises")
    @PreAuthorize("hasRole('ETUDIANT')")
    @Transactional
    public ResponseEntity<?> remettre(@PathVariable Long id, @RequestBody RemiseRequest request,
            Authentication authentication) {
        Etudiant etudiant = utilisateurCourant.etudiant(authentication);
        ActiviteCours activite = activites.findById(id).orElse(null);
        if (etudiant == null || activite == null || !habilitation.estAutorise(activite.getCours().getId(), etudiant)) {
            return ResponseEntity.notFound().build();
        }
        if (!"EN_LIGNE".equals(activite.getModalite())) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "Cette activité est prévue en présentiel."));
        }
        if (activite.getDateLimite() != null && LocalDateTime.now().isAfter(activite.getDateLimite())) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "La date limite de remise est dépassée."));
        }
        if (estVide(request.contenu())) return ResponseEntity.badRequest().body(java.util.Map.of("message", "La remise ne peut pas être vide."));
        RemiseActivite remise = remises.findByActiviteIdAndEtudiantId(id, etudiant.getId()).orElseGet(RemiseActivite::new);
        remise.setActivite(activite);
        remise.setEtudiant(etudiant);
        remise.setContenu(request.contenu().trim());
        remise.setRemiseLe(LocalDateTime.now());
        return ResponseEntity.ok(remiseDto(remises.save(remise)));
    }

    @GetMapping("/formateur/activites/{id}/remises")
    @PreAuthorize("hasRole('FORMATEUR')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> remises(@PathVariable Long id, Authentication authentication) {
        ActiviteCours activite = activites.findById(id).orElse(null);
        if (activite == null || coursDuFormateur(activite.getCours().getId(), authentication) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(remises.findByActiviteIdOrderByRemiseLeDesc(id).stream().map(this::remiseDto).toList());
    }

    @PutMapping("/formateur/remises/{id}/note")
    @PreAuthorize("hasRole('FORMATEUR')")
    @Transactional
    public ResponseEntity<?> corriger(@PathVariable Long id, @RequestBody CorrectionRequest request,
            Authentication authentication) {
        RemiseActivite remise = remises.findById(id).orElse(null);
        if (remise == null || coursDuFormateur(remise.getActivite().getCours().getId(), authentication) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (request.note() == null || request.note() < 0 || request.note() > 100) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "La note doit être comprise entre 0 et 100."));
        }
        remise.setNote(request.note());
        remise.setCommentaireCorrection(estVide(request.commentaire()) ? null : request.commentaire().trim());
        remise.setCorrigeLe(LocalDateTime.now());
        RemiseActivite corrigee = remises.save(remise);
        notifications.save(new Notification(corrigee.getEtudiant().getUser(),
                "Votre activité « " + corrigee.getActivite().getTitre() + " » a été corrigée.",
                Notification.ACTIVITE_CORRIGEE,
                "/espace-etudiant/mes-cours/" + corrigee.getActivite().getCours().getId()));
        return ResponseEntity.ok(remiseDto(corrigee));
    }

    private Cours coursDuFormateur(Long coursId, Authentication authentication) {
        Formateur formateur = utilisateurCourant.formateur(authentication);
        Cours cours = coursRepository.findById(coursId).orElse(null);
        return formateur != null && cours != null && perimetre.estFormateurDuCours(formateur, cours) ? cours : null;
    }

    private void notifierPublication(ActiviteCours activite) {
        List<Long> promotionIds = perimetre.promotionIdsDuCours(activite.getCours());
        if (promotionIds.isEmpty()) return;
        etudiantRepository.findByPromotion_IdIn(promotionIds).forEach(etudiant -> notifications.save(new Notification(
                etudiant.getUser(), "Nouvelle activité dans « " + activite.getCours().getTitre() + " » : " + activite.getTitre(),
                Notification.ACTIVITE_COURS, "/espace-etudiant/mes-cours/" + activite.getCours().getId())));
    }

    private static String valider(ActiviteRequest request) {
        if (request == null || estVide(request.module()) || estVide(request.titre())) return "Le module et le titre sont requis.";
        if (!List.of("DEVOIR", "EVALUATION").contains(request.type())) return "Le type doit être DEVOIR ou EVALUATION.";
        if (!List.of("EN_LIGNE", "PRESENTIEL").contains(request.modalite())) return "La modalité est invalide.";
        if (request.datePublication() == null) return "La date de publication est requise.";
        if ("EN_LIGNE".equals(request.modalite()) && request.dateLimite() != null
                && request.dateLimite().isBefore(request.datePublication())) {
            return "La date limite doit suivre la publication.";
        }
        if (request.bareme() != null && request.bareme() != 100) return "Les évaluations sont notées sur 100.";
        return null;
    }
    private static boolean estVide(String value) { return value == null || value.trim().isEmpty(); }
    private static void appliquer(ActiviteCours activite, ActiviteRequest request) {
        activite.setModule(request.module().trim()); activite.setTitre(request.titre().trim());
        activite.setDescription(estVide(request.description()) ? null : request.description().trim());
        activite.setType(request.type()); activite.setModalite(request.modalite());
        activite.setDatePublication(request.datePublication());
        // Le présentiel se déroule à une date donnée : ce n'est pas une remise à fermer.
        // On efface donc toute ancienne valeur envoyée par un client avant la règle d'interface.
        activite.setDateLimite("EN_LIGNE".equals(request.modalite()) ? request.dateLimite() : null);
        activite.setBareme(100); activite.setModifieLe(LocalDateTime.now());
    }
    private ActiviteDto dto(ActiviteCours a) { return new ActiviteDto(a.getId(), a.getCours().getId(), a.getModule(), a.getTitre(),
            a.getDescription(), a.getType(), a.getModalite(), a.getDatePublication(), a.getDateLimite(), a.getBareme()); }
    private RemiseDto remiseDto(RemiseActivite r) { return new RemiseDto(r.getId(), r.getActivite().getId(), r.getEtudiant().getId(),
            r.getEtudiant().getPrenom() + " " + r.getEtudiant().getNom(), r.getContenu(), r.getRemiseLe(), r.getNote(), r.getCommentaireCorrection(), r.getCorrigeLe()); }
    public record ActiviteRequest(String module, String titre, String description, String type, String modalite,
            LocalDateTime datePublication, LocalDateTime dateLimite, Double bareme) { }
    public record RemiseRequest(String contenu) { }
    public record CorrectionRequest(Double note, String commentaire) { }
    public record ActiviteDto(Long id, Long coursId, String module, String titre, String description, String type,
            String modalite, LocalDateTime datePublication, LocalDateTime dateLimite, double bareme) { }
    public record RemiseDto(Long id, Long activiteId, Long etudiantId, String etudiant, String contenu,
            LocalDateTime remiseLe, Double note, String commentaireCorrection, LocalDateTime corrigeLe) { }
}
