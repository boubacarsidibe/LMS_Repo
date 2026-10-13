package inpg.sn.lms.recrutementformateur.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import inpg.sn.lms.auth.entity.Formateur;
import inpg.sn.lms.auth.entity.Role;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.FormateurRepository;
import inpg.sn.lms.auth.repository.RoleRepository;
import inpg.sn.lms.auth.service.UserService;
import inpg.sn.lms.espace_etudiant.entity.Formation;
import inpg.sn.lms.espace_etudiant.repository.FormationRepository;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;
import inpg.sn.lms.fichier.ValidationFichier;
import inpg.sn.lms.recrutement.entity.FichierStocke;
import inpg.sn.lms.recrutement.service.FichierStockeService;
import inpg.sn.lms.recrutementformateur.dto.CandidatureFormateurDto;
import inpg.sn.lms.recrutementformateur.dto.CandidatureFormateurDto.CertificationDto;
import inpg.sn.lms.recrutementformateur.dto.CandidatureFormateurDto.ExperienceDto;
import inpg.sn.lms.recrutementformateur.dto.CandidatureFormateurDto.FormationDto;
import inpg.sn.lms.recrutementformateur.dto.CandidatureFormateurDto.LangueDto;
import inpg.sn.lms.recrutementformateur.dto.ChangementStatutRequest;
import inpg.sn.lms.recrutementformateur.dto.DecisionCandidatureFormateurRequest;
import inpg.sn.lms.recrutementformateur.dto.OffreFormateurDto;
import inpg.sn.lms.recrutementformateur.dto.OffreFormateurRequest;
import inpg.sn.lms.recrutementformateur.dto.PieceCandidatureFormateurDto;
import inpg.sn.lms.recrutementformateur.entity.CandidatureFormateur;
import inpg.sn.lms.recrutementformateur.entity.CertificationCandidatureFormateur;
import inpg.sn.lms.recrutementformateur.entity.ExperienceCandidatureFormateur;
import inpg.sn.lms.recrutementformateur.entity.FormationCandidatureFormateur;
import inpg.sn.lms.recrutementformateur.entity.LangueCandidatureFormateur;
import inpg.sn.lms.recrutementformateur.entity.OffreRecrutementFormateur;
import inpg.sn.lms.recrutementformateur.entity.PieceCandidatureFormateur;
import inpg.sn.lms.recrutementformateur.enums.EnumDecisionCandidatureFormateur;
import inpg.sn.lms.recrutementformateur.enums.EnumStatutCandidatureFormateur;
import inpg.sn.lms.recrutementformateur.enums.EnumStatutOffreFormateur;
import inpg.sn.lms.recrutementformateur.enums.EnumTypeContratFormateur;
import inpg.sn.lms.recrutementformateur.repository.CandidatureFormateurRepository;
import inpg.sn.lms.recrutementformateur.repository.CertificationCandidatureFormateurRepository;
import inpg.sn.lms.recrutementformateur.repository.ExperienceCandidatureFormateurRepository;
import inpg.sn.lms.recrutementformateur.repository.FormationCandidatureFormateurRepository;
import inpg.sn.lms.recrutementformateur.repository.LangueCandidatureFormateurRepository;
import inpg.sn.lms.recrutementformateur.repository.OffreRecrutementFormateurRepository;
import inpg.sn.lms.recrutementformateur.repository.PieceCandidatureFormateurRepository;
import inpg.sn.lms.recrutementformateur.service.RecrutementFormateurNotificationService;

/**
 * Volet responsable admission du recrutement des formateurs (procédure QMS-007
 * §1, §3, §4). Pilote les offres et l'instruction des candidatures ; la décision
 * {@code RETENU} crée le compte formateur.
 *
 * <p>Sous {@code /api/admin/recrutement-formateurs/**}, couvert par le joker
 * {@code /api/admin/** -> hasRole("RESPONSABLE_ADMISSION")} de
 * {@code SecurityConfig} ; l'annotation ci-dessous ajoute la protection au
 * niveau méthode (double protection volontaire, patron
 * {@code EvaluateurAccreditationController}).
 */
@RestController
@RequestMapping("/api/admin/recrutement-formateurs")
@PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
public class AdminRecrutementFormateurController {

    /** Une fiche de poste est un document de quelques pages ; le plafond général d'envoi (500 Mo) est bien trop large. */
    static final long TAILLE_MAX_FICHE_POSTE = 10L * 1024 * 1024;

    private final OffreRecrutementFormateurRepository offreRepository;
    private final CandidatureFormateurRepository candidatureRepository;
    private final PieceCandidatureFormateurRepository pieceRepository;
    private final FormationCandidatureFormateurRepository formationCandidatureRepository;
    private final ExperienceCandidatureFormateurRepository experienceRepository;
    private final LangueCandidatureFormateurRepository langueRepository;
    private final CertificationCandidatureFormateurRepository certificationRepository;
    private final FormationRepository formationRepository;
    private final FormateurRepository formateurRepository;
    private final RoleRepository roleRepository;
    private final UserService userService;
    private final UtilisateurCourantService utilisateurCourant;
    private final RecrutementFormateurNotificationService notificationService;
    private final FichierStockeService fichierStockeService;

    public AdminRecrutementFormateurController(OffreRecrutementFormateurRepository offreRepository,
            CandidatureFormateurRepository candidatureRepository, PieceCandidatureFormateurRepository pieceRepository,
            FormationCandidatureFormateurRepository formationCandidatureRepository,
            ExperienceCandidatureFormateurRepository experienceRepository,
            LangueCandidatureFormateurRepository langueRepository,
            CertificationCandidatureFormateurRepository certificationRepository,
            FormationRepository formationRepository, FormateurRepository formateurRepository,
            RoleRepository roleRepository, UserService userService, UtilisateurCourantService utilisateurCourant,
            RecrutementFormateurNotificationService notificationService, FichierStockeService fichierStockeService) {
        this.offreRepository = offreRepository;
        this.candidatureRepository = candidatureRepository;
        this.pieceRepository = pieceRepository;
        this.formationCandidatureRepository = formationCandidatureRepository;
        this.experienceRepository = experienceRepository;
        this.langueRepository = langueRepository;
        this.certificationRepository = certificationRepository;
        this.formationRepository = formationRepository;
        this.formateurRepository = formateurRepository;
        this.roleRepository = roleRepository;
        this.userService = userService;
        this.utilisateurCourant = utilisateurCourant;
        this.notificationService = notificationService;
        this.fichierStockeService = fichierStockeService;
    }

    // ===================== Offres (fiche de poste, QMS-007 §1) =====================

    @GetMapping("/offres")
    @Transactional(readOnly = true)
    public ResponseEntity<?> offres() {
        return ResponseEntity.ok(offreRepository.findAllByOrderByDateCreationDesc().stream()
                .map(this::toOffreDto).toList());
    }

    @GetMapping("/offres/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> offre(@PathVariable Long id) {
        OffreRecrutementFormateur offre = offreRepository.findById(id).orElse(null);
        return offre == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(toOffreDto(offre));
    }

    @PostMapping("/offres")
    @Transactional
    public ResponseEntity<?> creerOffre(@RequestBody OffreFormateurRequest requete, Authentication authentication) {
        if (estVide(requete.intitule()) || estVide(requete.typeContrat())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "L'intitulé et le type de contrat sont obligatoires."));
        }
        EnumTypeContratFormateur typeContrat;
        try {
            typeContrat = EnumTypeContratFormateur.valueOf(requete.typeContrat());
        } catch (IllegalArgumentException invalide) {
            return ResponseEntity.badRequest().body(Map.of("message", "Type de contrat invalide."));
        }
        LocalDate dateLimite = parseDate(requete.dateLimiteCandidature());
        if (requete.dateLimiteCandidature() != null && !requete.dateLimiteCandidature().isBlank() && dateLimite == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "La date limite de candidature est invalide (format AAAA-MM-JJ)."));
        }
        OffreRecrutementFormateur offre = new OffreRecrutementFormateur();
        appliquer(offre, requete, typeContrat, dateLimite);
        offre.setCreePar(utilisateurCourant.utilisateur(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(toOffreDto(offreRepository.save(offre)));
    }

    @PutMapping("/offres/{id}")
    @Transactional
    public ResponseEntity<?> modifierOffre(@PathVariable Long id, @RequestBody OffreFormateurRequest requete) {
        OffreRecrutementFormateur offre = offreRepository.findById(id).orElse(null);
        if (offre == null) {
            return ResponseEntity.notFound().build();
        }
        if (estVide(requete.intitule()) || estVide(requete.typeContrat())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "L'intitulé et le type de contrat sont obligatoires."));
        }
        EnumTypeContratFormateur typeContrat;
        try {
            typeContrat = EnumTypeContratFormateur.valueOf(requete.typeContrat());
        } catch (IllegalArgumentException invalide) {
            return ResponseEntity.badRequest().body(Map.of("message", "Type de contrat invalide."));
        }
        LocalDate dateLimite = parseDate(requete.dateLimiteCandidature());
        appliquer(offre, requete, typeContrat, dateLimite);
        return ResponseEntity.ok(toOffreDto(offreRepository.save(offre)));
    }

    @PutMapping("/offres/{id}/publier")
    @Transactional
    public ResponseEntity<?> publierOffre(@PathVariable Long id) {
        OffreRecrutementFormateur offre = offreRepository.findById(id).orElse(null);
        if (offre == null) {
            return ResponseEntity.notFound().build();
        }
        if (offre.getStatut() == EnumStatutOffreFormateur.CLOTUREE) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Une offre clôturée ne peut plus être publiée."));
        }
        offre.setStatut(EnumStatutOffreFormateur.PUBLIEE);
        if (offre.getDatePublication() == null) {
            offre.setDatePublication(LocalDate.now());
        }
        return ResponseEntity.ok(toOffreDto(offreRepository.save(offre)));
    }

    @PutMapping("/offres/{id}/cloturer")
    @Transactional
    public ResponseEntity<?> cloturerOffre(@PathVariable Long id) {
        OffreRecrutementFormateur offre = offreRepository.findById(id).orElse(null);
        if (offre == null) {
            return ResponseEntity.notFound().build();
        }
        offre.setStatut(EnumStatutOffreFormateur.CLOTUREE);
        return ResponseEntity.ok(toOffreDto(offreRepository.save(offre)));
    }

    @DeleteMapping("/offres/{id}")
    @Transactional
    public ResponseEntity<?> supprimerOffre(@PathVariable Long id) {
        OffreRecrutementFormateur offre = offreRepository.findById(id).orElse(null);
        if (offre == null) {
            return ResponseEntity.notFound().build();
        }
        if (candidatureRepository.countByOffreId(id) > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "message", "Cette offre porte des candidatures : elle peut être clôturée, pas supprimée."));
        }
        FichierStocke fichePoste = offre.getFichePoste();
        offreRepository.delete(offre);
        if (fichePoste != null) {
            fichierStockeService.supprimerFichier(fichePoste.getId());
        }
        return ResponseEntity.noContent().build();
    }

    // ===================== Fiche de poste jointe =====================

    /**
     * Joint (ou remplace) la fiche de poste d'une offre. PDF uniquement : c'est le format que
     * les candidats peuvent tous ouvrir, et celui dont {@code FichierStockeService} vérifie la
     * signature binaire — un fichier renommé en {@code .pdf} est refusé.
     */
    @PostMapping(value = "/offres/{id}/fiche-poste", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<?> joindreFichePoste(@PathVariable Long id, @RequestParam("fichier") MultipartFile fichier) {
        OffreRecrutementFormateur offre = offreRepository.findById(id).orElse(null);
        if (offre == null) {
            return ResponseEntity.notFound().build();
        }
        if (!ValidationFichier.nomSur(fichier, "").toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")) {
            return ResponseEntity.badRequest().body(Map.of("message", "La fiche de poste doit être un fichier PDF."));
        }
        if (fichier.getSize() > TAILLE_MAX_FICHE_POSTE) {
            return ResponseEntity.badRequest().body(Map.of("message", "La fiche de poste ne doit pas dépasser 10 Mo."));
        }
        FichierStocke nouvelle;
        try {
            nouvelle = fichierStockeService.enregistrerFichier(fichier);
        } catch (IllegalArgumentException invalide) {
            return ResponseEntity.badRequest().body(Map.of("message", invalide.getMessage()));
        } catch (RuntimeException erreur) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Le fichier n'a pas pu être enregistré."));
        }
        FichierStocke ancienne = offre.getFichePoste();
        offre.setFichePoste(nouvelle);
        OffreRecrutementFormateur enregistree = offreRepository.save(offre);
        if (ancienne != null) {
            fichierStockeService.supprimerFichier(ancienne.getId());
        }
        return ResponseEntity.ok(toOffreDto(enregistree));
    }

    @GetMapping("/offres/{id}/fiche-poste")
    @Transactional(readOnly = true)
    public ResponseEntity<?> telechargerFichePoste(@PathVariable Long id) {
        OffreRecrutementFormateur offre = offreRepository.findById(id).orElse(null);
        if (offre == null || offre.getFichePoste() == null) {
            return ResponseEntity.notFound().build();
        }
        return CandidatFormateurController.servirFichier(offre.getFichePoste());
    }

    @DeleteMapping("/offres/{id}/fiche-poste")
    @Transactional
    public ResponseEntity<?> retirerFichePoste(@PathVariable Long id) {
        OffreRecrutementFormateur offre = offreRepository.findById(id).orElse(null);
        if (offre == null) {
            return ResponseEntity.notFound().build();
        }
        FichierStocke ancienne = offre.getFichePoste();
        if (ancienne != null) {
            offre.setFichePoste(null);
            offreRepository.save(offre);
            fichierStockeService.supprimerFichier(ancienne.getId());
        }
        return ResponseEntity.ok(toOffreDto(offre));
    }

    // ===================== Candidatures (tri et sélection, QMS-007 §3-4) =====================

    @GetMapping("/candidatures")
    @Transactional(readOnly = true)
    public ResponseEntity<?> candidatures(@RequestParam(required = false) Long offreId,
            @RequestParam(required = false) String statut) {
        List<CandidatureFormateur> liste;
        if (offreId != null) {
            liste = candidatureRepository.findByOffreIdOrderByDateSoumissionDesc(offreId);
        } else if (statut != null && !statut.isBlank()) {
            try {
                liste = candidatureRepository.findByStatutOrderByDateSoumissionDesc(
                        EnumStatutCandidatureFormateur.valueOf(statut));
            } catch (IllegalArgumentException invalide) {
                return ResponseEntity.badRequest().body(Map.of("message", "Statut invalide."));
            }
        } else {
            liste = candidatureRepository.findAllByOrderByDateSoumissionDesc();
        }
        return ResponseEntity.ok(liste.stream().map(this::toCandidatureDto).toList());
    }

    @GetMapping("/candidatures/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> candidature(@PathVariable Long id) {
        CandidatureFormateur c = candidatureRepository.findById(id).orElse(null);
        return c == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(toCandidatureDto(c));
    }

    @GetMapping("/candidatures/{id}/pieces/{pieceId}/fichier")
    @Transactional(readOnly = true)
    public ResponseEntity<?> telechargerPiece(@PathVariable Long id, @PathVariable Long pieceId) {
        if (candidatureRepository.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return CandidatFormateurController.servirPiece(pieceRepository.findById(pieceId).orElse(null), id);
    }

    /** Fait avancer le suivi : DOSSIER_RECU → EN_COURS_DE_TRAITEMENT. Le passage en TERMINE se fait par la décision. */
    @PutMapping("/candidatures/{id}/statut")
    @Transactional
    public ResponseEntity<?> changerStatut(@PathVariable Long id, @RequestBody ChangementStatutRequest requete) {
        CandidatureFormateur c = candidatureRepository.findById(id).orElse(null);
        if (c == null) {
            return ResponseEntity.notFound().build();
        }
        EnumStatutCandidatureFormateur cible;
        try {
            cible = EnumStatutCandidatureFormateur.valueOf(requete.statut());
        } catch (IllegalArgumentException | NullPointerException invalide) {
            return ResponseEntity.badRequest().body(Map.of("message", "Statut invalide."));
        }
        if (cible != EnumStatutCandidatureFormateur.EN_COURS_DE_TRAITEMENT
                || c.getStatut() != EnumStatutCandidatureFormateur.DOSSIER_RECU) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    "Seul le passage « dossier reçu » → « en cours de traitement » est permis ici ; "
                            + "la clôture se fait par la décision."));
        }
        c.setStatut(EnumStatutCandidatureFormateur.EN_COURS_DE_TRAITEMENT);
        candidatureRepository.save(c);
        notificationService.notifierPriseEnCharge(c.getEmail(), c.getPrenom(), c.getOffre().getIntitule());
        return ResponseEntity.ok(toCandidatureDto(c));
    }

    /**
     * Décision finale (QMS-007 §4) : passe la candidature en TERMINE.
     * {@code NON_RETENU} exige un motif. {@code RETENU} bascule le compte du
     * candidat en formateur (une seule fois, garde-fou {@code formateurCree}).
     */
    @PutMapping("/candidatures/{id}/decision")
    @Transactional
    public ResponseEntity<?> decider(@PathVariable Long id, @RequestBody DecisionCandidatureFormateurRequest requete,
            Authentication authentication) {
        CandidatureFormateur c = candidatureRepository.findById(id).orElse(null);
        if (c == null) {
            return ResponseEntity.notFound().build();
        }
        EnumDecisionCandidatureFormateur decision;
        try {
            decision = EnumDecisionCandidatureFormateur.valueOf(requete == null ? "" : String.valueOf(requete.decision()));
        } catch (IllegalArgumentException invalide) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "La décision doit valoir RETENU ou NON_RETENU."));
        }
        String motif = requete.motif() == null || requete.motif().isBlank() ? null : requete.motif().trim();
        if (decision == EnumDecisionCandidatureFormateur.NON_RETENU && motif == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le motif est obligatoire pour un refus."));
        }
        if (c.isFormateurCree()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Cette candidature a déjà donné lieu à un compte formateur."));
        }

        if (decision == EnumDecisionCandidatureFormateur.RETENU
                && formateurRepository.findByTelephone(c.getTelephone().trim()).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Ce numéro de téléphone est déjà associé à un formateur."));
        }

        c.setStatut(EnumStatutCandidatureFormateur.TERMINE);
        c.setDecision(decision);
        c.setMotifDecision(motif);
        c.setDateDecision(LocalDateTime.now());
        c.setTraitePar(utilisateurCourant.utilisateur(authentication));

        if (decision == EnumDecisionCandidatureFormateur.RETENU) {
            promouvoirEnFormateur(c);
            c.setFormateurCree(true);
            candidatureRepository.save(c);
            notificationService.notifierRetenu(c.getEmail(), c.getPrenom(), c.getOffre().getIntitule());
        } else {
            candidatureRepository.save(c);
            notificationService.notifierNonRetenu(c.getEmail(), c.getPrenom(), c.getOffre().getIntitule(), motif);
        }
        return ResponseEntity.ok(toCandidatureDto(c));
    }

    // ===================== Utilitaires =====================

    /** Bascule le compte du candidat en formateur et crée sa fiche (patron garantirRoleEcole / inscrireCommeEtudiant). */
    private void promouvoirEnFormateur(CandidatureFormateur c) {
        User user = c.getCandidat();
        Role roleFormateur = roleRepository.findByCode("ROLE_FORMATEUR");
        if (roleFormateur == null) {
            throw new IllegalStateException("Le rôle ROLE_FORMATEUR est introuvable.");
        }
        boolean dejaFormateur = user.getRoles().stream().anyMatch(r -> "ROLE_FORMATEUR".equals(r.getCode()));
        if (!dejaFormateur) {
            user.getRoles().clear();
            user.addRole(roleFormateur);
            userService.save(user);
        }
        if (formateurRepository.findByUserId(user.getId()).isEmpty()) {
            formateurRepository.save(new Formateur(user, c.getNom(), c.getPrenom(), c.getTelephone().trim()));
        }
    }

    private void appliquer(OffreRecrutementFormateur offre, OffreFormateurRequest r,
            EnumTypeContratFormateur typeContrat, LocalDate dateLimite) {
        offre.setIntitule(r.intitule().trim());
        offre.setDomaine(vide(r.domaine()));
        offre.setDescriptionMissions(vide(r.descriptionMissions()));
        offre.setProfilRecherche(vide(r.profilRecherche()));
        offre.setTypeContrat(typeContrat);
        offre.setLieu(vide(r.lieu()));
        offre.setNbPostes(r.nbPostes() != null && r.nbPostes() > 0 ? r.nbPostes() : 1);
        offre.setDateLimiteCandidature(dateLimite);
        Formation formation = r.formationId() != null ? formationRepository.findById(r.formationId()).orElse(null) : null;
        offre.setFormation(formation);
    }

    private OffreFormateurDto toOffreDto(OffreRecrutementFormateur o) {
        return new OffreFormateurDto(
                o.getId(), o.getIntitule(), o.getDomaine(), o.getDescriptionMissions(), o.getProfilRecherche(),
                o.getTypeContrat() != null ? o.getTypeContrat().name() : null, o.getLieu(), o.getNbPostes(),
                o.getFormation() != null ? o.getFormation().getId() : null,
                o.getFormation() != null ? o.getFormation().getIntitule() : null,
                o.getDatePublication(), o.getDateLimiteCandidature(), o.getStatut().name(), o.getDateCreation(),
                candidatureRepository.countByOffreId(o.getId()),
                o.getFichePoste() != null ? o.getFichePoste().getNomOriginal() : null);
    }

    private CandidatureFormateurDto toCandidatureDto(CandidatureFormateur c) {
        return candidatureDto(c, pieceRepository.findByCandidatureId(c.getId()),
                formationCandidatureRepository.findByCandidatureId(c.getId()),
                experienceRepository.findByCandidatureId(c.getId()),
                langueRepository.findByCandidatureId(c.getId()),
                certificationRepository.findByCandidatureId(c.getId()));
    }

    /** Partagé avec {@link CandidatFormateurController}. */
    static CandidatureFormateurDto candidatureDto(CandidatureFormateur c, List<PieceCandidatureFormateur> pieces,
            List<FormationCandidatureFormateur> formations, List<ExperienceCandidatureFormateur> experiences,
            List<LangueCandidatureFormateur> langues, List<CertificationCandidatureFormateur> certifications) {
        List<PieceCandidatureFormateurDto> piecesDto = pieces.stream()
                .map(p -> new PieceCandidatureFormateurDto(p.getId(), p.getTypeDocument().name(),
                        p.getFichierStocke() != null ? p.getFichierStocke().getNomOriginal() : null))
                .toList();
        List<FormationDto> formationsDto = formations.stream()
                .map(f -> new FormationDto(f.getId(), f.getNiveauEtude(), f.getPays(), f.getVille(),
                        f.getEtablissement(), f.getIntituleDiplome(), f.getMatierePrincipale(), f.getDateDebut(),
                        f.getDateObtention()))
                .toList();
        List<ExperienceDto> experiencesDto = experiences.stream()
                .map(e -> new ExperienceDto(e.getId(), e.getEmployeur(), e.getPays(), e.getVille(), e.isPosteActuel(),
                        e.getIntitulePoste(), e.getDateDebut(), e.getDateFin(), e.getNomSuperviseur(),
                        e.getTitreSuperviseur(), e.getTelephoneSuperviseur(), e.getEmailSuperviseur(),
                        e.getContactAutorise(), e.getMotifNonContact(), e.getMotifDepart()))
                .toList();
        List<LangueDto> languesDto = langues.stream()
                .map(l -> new LangueDto(l.getId(), l.getLangue(), l.getNiveau().name()))
                .toList();
        List<CertificationDto> certificationsDto = certifications.stream()
                .map(cert -> new CertificationDto(cert.getId(), cert.getIntitule(), cert.getOrganisme(),
                        cert.getDateObtention(), cert.getDateExpiration()))
                .toList();
        OffreRecrutementFormateur o = c.getOffre();
        return new CandidatureFormateurDto(
                c.getId(),
                o != null ? o.getId() : null,
                o != null ? o.getIntitule() : null,
                o != null && o.getTypeContrat() != null ? o.getTypeContrat().name() : null,
                c.getNom(), c.getPrenom(), c.getEmail(), c.getTelephone(),
                c.getDiplomePrincipal(), c.getAnneesExperience(),
                c.getStatut().name(),
                c.getDecision() != null ? c.getDecision().name() : null,
                c.getMotifDecision(),
                c.getDateSoumission(), c.getDateDecision(), c.isFormateurCree(),
                piecesDto,
                c.getCivilite(), c.getGenre(), c.getDateNaissance(), c.getPaysNaissance(), c.getLieuNaissance(),
                c.getNationalite(), c.getNationaliteSecondaire(), c.getNiveauEtude(), c.getTelephoneSecondaire(),
                c.getEmailAlternatif(), c.getAdresseLigne1(), c.getAdresseLigne2(), c.getVille(),
                c.getPaysResidence(),
                formationsDto, experiencesDto, languesDto, certificationsDto,
                // Copie matérialisée : open-in-view=false, la collection paresseuse de
                // l'entité ne doit pas fuir hors de cette méthode transactionnelle.
                java.util.Set.copyOf(c.getDomainesExpertise()));
    }

    private LocalDate parseDate(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(v.trim());
        } catch (DateTimeParseException invalide) {
            return null;
        }
    }

    private boolean estVide(String v) {
        return v == null || v.isBlank();
    }

    private String vide(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
