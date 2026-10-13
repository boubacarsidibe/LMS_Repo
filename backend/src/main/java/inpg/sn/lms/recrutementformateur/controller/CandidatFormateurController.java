package inpg.sn.lms.recrutementformateur.controller;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;

import inpg.sn.lms.auth.entity.Role;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.RoleRepository;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;
import inpg.sn.lms.recrutement.entity.FichierStocke;
import inpg.sn.lms.recrutement.service.FichierStockeService;
import inpg.sn.lms.recrutementformateur.dto.CandidatureFormateurCreateRequest;
import inpg.sn.lms.recrutementformateur.dto.CandidatureFormateurCreateRequest.CertificationRequest;
import inpg.sn.lms.recrutementformateur.dto.CandidatureFormateurCreateRequest.ExperienceRequest;
import inpg.sn.lms.recrutementformateur.dto.CandidatureFormateurCreateRequest.FormationRequest;
import inpg.sn.lms.recrutementformateur.dto.CandidatureFormateurCreateRequest.LangueRequest;
import inpg.sn.lms.recrutementformateur.dto.CandidatureFormateurDto;
import inpg.sn.lms.recrutementformateur.dto.PieceCandidatureFormateurDto;
import inpg.sn.lms.recrutementformateur.entity.CandidatureFormateur;
import inpg.sn.lms.recrutementformateur.entity.CertificationCandidatureFormateur;
import inpg.sn.lms.recrutementformateur.entity.ExperienceCandidatureFormateur;
import inpg.sn.lms.recrutementformateur.entity.FormationCandidatureFormateur;
import inpg.sn.lms.recrutementformateur.entity.LangueCandidatureFormateur;
import inpg.sn.lms.recrutementformateur.entity.OffreRecrutementFormateur;
import inpg.sn.lms.recrutementformateur.entity.PieceCandidatureFormateur;
import inpg.sn.lms.recrutementformateur.enums.EnumNiveauLangue;
import inpg.sn.lms.recrutementformateur.enums.EnumStatutCandidatureFormateur;
import inpg.sn.lms.recrutementformateur.enums.EnumStatutOffreFormateur;
import inpg.sn.lms.recrutementformateur.enums.EnumTypeDocumentCandidatureFormateur;
import inpg.sn.lms.recrutementformateur.repository.CandidatureFormateurRepository;
import inpg.sn.lms.recrutementformateur.repository.CertificationCandidatureFormateurRepository;
import inpg.sn.lms.recrutementformateur.repository.ExperienceCandidatureFormateurRepository;
import inpg.sn.lms.recrutementformateur.repository.FormationCandidatureFormateurRepository;
import inpg.sn.lms.recrutementformateur.repository.LangueCandidatureFormateurRepository;
import inpg.sn.lms.recrutementformateur.repository.OffreRecrutementFormateurRepository;
import inpg.sn.lms.recrutementformateur.repository.PieceCandidatureFormateurRepository;
import inpg.sn.lms.recrutementformateur.service.RecrutementFormateurNotificationService;

/**
 * Espace du candidat formateur (procédure QMS-007 §3) : dépôt d'une candidature
 * à une offre publiée, pièces jointes, suivi de l'état.
 *
 * <p>Vit sous {@code /api/recrutement-formateurs/**}, {@code authenticated()}
 * dans {@code SecurityConfig} — pas {@code hasRole}, car
 * {@code ROLE_CANDIDAT_FORMATEUR} n'est accordé qu'à la première candidature
 * réussie, même patron que {@code EcoleAccreditationController}.
 */
@RestController
@RequestMapping("/api/recrutement-formateurs")
public class CandidatFormateurController {

    /** Un compte ne détient qu'un seul rôle ouvrant un espace à la fois (règle du dépôt). */
    private static final Set<String> ROLES_OUVRANT_UN_ESPACE = Set.of(
            "ROLE_ETUDIANT", "ROLE_FORMATEUR", "ROLE_RESPONSABLE_PEDAGOGIQUE", "ROLE_RESPONSABLE_ADMISSION",
            "ROLE_ALUMNI", "ROLE_EVALUATEUR_ACCREDITATION", "ROLE_ECOLE_FORMATION", "ROLE_ENTREPRISE", "ROLE_CANDIDAT");

    private final OffreRecrutementFormateurRepository offreRepository;
    private final CandidatureFormateurRepository candidatureRepository;
    private final PieceCandidatureFormateurRepository pieceRepository;
    private final FormationCandidatureFormateurRepository formationRepository;
    private final ExperienceCandidatureFormateurRepository experienceRepository;
    private final LangueCandidatureFormateurRepository langueRepository;
    private final CertificationCandidatureFormateurRepository certificationRepository;
    private final RoleRepository roleRepository;
    private final FichierStockeService fichierStockeService;
    private final UtilisateurCourantService utilisateurCourant;
    private final RecrutementFormateurNotificationService notificationService;

    public CandidatFormateurController(OffreRecrutementFormateurRepository offreRepository,
            CandidatureFormateurRepository candidatureRepository, PieceCandidatureFormateurRepository pieceRepository,
            FormationCandidatureFormateurRepository formationRepository,
            ExperienceCandidatureFormateurRepository experienceRepository,
            LangueCandidatureFormateurRepository langueRepository,
            CertificationCandidatureFormateurRepository certificationRepository,
            RoleRepository roleRepository, FichierStockeService fichierStockeService,
            UtilisateurCourantService utilisateurCourant,
            RecrutementFormateurNotificationService notificationService) {
        this.offreRepository = offreRepository;
        this.candidatureRepository = candidatureRepository;
        this.pieceRepository = pieceRepository;
        this.formationRepository = formationRepository;
        this.experienceRepository = experienceRepository;
        this.langueRepository = langueRepository;
        this.certificationRepository = certificationRepository;
        this.roleRepository = roleRepository;
        this.fichierStockeService = fichierStockeService;
        this.utilisateurCourant = utilisateurCourant;
        this.notificationService = notificationService;
    }

    // ===================== Dépôt et suivi =====================

    @PostMapping("/candidatures")
    @Transactional
    public ResponseEntity<?> postuler(@RequestBody CandidatureFormateurCreateRequest requete,
            Authentication authentication) {
        User user = utilisateurCourant.utilisateur(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (requete.offreId() == null || estVide(requete.nom()) || estVide(requete.prenom())
                || estVide(requete.telephone())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "L'offre, le nom, le prénom et le téléphone sont obligatoires."));
        }
        OffreRecrutementFormateur offre = offreRepository.findById(requete.offreId()).orElse(null);
        if (offre == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Offre introuvable."));
        }
        if (offre.getStatut() != EnumStatutOffreFormateur.PUBLIEE) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Cette offre n'est pas ouverte aux candidatures."));
        }
        if (offre.getDateLimiteCandidature() != null && LocalDate.now().isAfter(offre.getDateLimiteCandidature())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "La date limite de candidature pour cette offre est dépassée."));
        }

        boolean dejaCandidatFormateur = user.getRoles().stream()
                .anyMatch(r -> "ROLE_CANDIDAT_FORMATEUR".equals(r.getCode()));
        if (!dejaCandidatFormateur) {
            Optional<Role> roleOuvrant = user.getRoles().stream()
                    .filter(r -> ROLES_OUVRANT_UN_ESPACE.contains(r.getCode()))
                    .findFirst();
            if (roleOuvrant.isPresent()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "message", "Ce compte donne déjà accès à l'espace « " + roleOuvrant.get().getLibelle()
                                + " ». Pour postuler comme formateur, utilisez un compte distinct."));
            }
        }

        boolean candidatureActive = !candidatureRepository.findByOffreIdAndCandidatIdAndStatutNot(
                offre.getId(), user.getId(), EnumStatutCandidatureFormateur.TERMINE).isEmpty();
        if (candidatureActive) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Vous avez déjà une candidature en cours pour cette offre."));
        }

        CandidatureFormateur candidature = new CandidatureFormateur(offre, user, requete.nom().trim(),
                requete.prenom().trim(), user.getEmail(), requete.telephone().trim());
        candidature.setDiplomePrincipal(vide(requete.diplomePrincipal()));
        candidature.setAnneesExperience(requete.anneesExperience());

        candidature.setCivilite(vide(requete.civilite()));
        candidature.setGenre(vide(requete.genre()));
        candidature.setDateNaissance(parseDate(requete.dateNaissance()));
        candidature.setPaysNaissance(vide(requete.paysNaissance()));
        candidature.setLieuNaissance(vide(requete.lieuNaissance()));
        candidature.setNationalite(vide(requete.nationalite()));
        candidature.setNationaliteSecondaire(vide(requete.nationaliteSecondaire()));
        candidature.setNiveauEtude(vide(requete.niveauEtude()));
        candidature.setTelephoneSecondaire(vide(requete.telephoneSecondaire()));
        candidature.setEmailAlternatif(vide(requete.emailAlternatif()));
        candidature.setAdresseLigne1(vide(requete.adresseLigne1()));
        candidature.setAdresseLigne2(vide(requete.adresseLigne2()));
        candidature.setVille(vide(requete.ville()));
        candidature.setPaysResidence(vide(requete.paysResidence()));
        if (requete.domainesExpertise() != null) {
            for (String d : requete.domainesExpertise()) {
                if (d != null && !d.isBlank()) {
                    candidature.getDomainesExpertise().add(d.trim());
                }
            }
        }

        candidature = candidatureRepository.save(candidature);
        enregistrerDossierEnrichi(candidature, requete);

        garantirRoleCandidatFormateur(user);
        notificationService.notifierNouvelleCandidature(offre.getIntitule());
        notificationService.notifierDossierRecu(user.getEmail(), candidature.getPrenom(), offre.getIntitule());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(candidature));
    }

    @GetMapping("/mes-candidatures")
    @Transactional(readOnly = true)
    public ResponseEntity<?> mesCandidatures(Authentication authentication) {
        User user = utilisateurCourant.utilisateur(authentication);
        if (user == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(candidatureRepository.findByCandidatIdOrderByDateSoumissionDesc(user.getId()).stream()
                .map(this::toDto)
                .toList());
    }

    @GetMapping("/candidatures/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> candidature(@PathVariable Long id, Authentication authentication) {
        CandidatureFormateur candidature = candidaturePropriete(id, authentication);
        if (candidature == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toDto(candidature));
    }

    // ===================== Pièces =====================

    @PostMapping("/candidatures/{id}/pieces")
    @Transactional
    public ResponseEntity<?> televerserPiece(@PathVariable Long id,
            @RequestParam("typeDocument") String typeDocumentBrut,
            @RequestParam("fichier") MultipartFile fichier, Authentication authentication) {
        CandidatureFormateur candidature = candidaturePropriete(id, authentication);
        if (candidature == null) {
            return ResponseEntity.notFound().build();
        }
        if (candidature.getStatut() == EnumStatutCandidatureFormateur.TERMINE) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Cette candidature est close : elle n'accepte plus de nouvelle pièce."));
        }
        EnumTypeDocumentCandidatureFormateur typeDocument;
        try {
            typeDocument = EnumTypeDocumentCandidatureFormateur.valueOf(typeDocumentBrut);
        } catch (IllegalArgumentException invalide) {
            return ResponseEntity.badRequest().body(Map.of("message", "Type de document invalide."));
        }
        try {
            FichierStocke fichierStocke = fichierStockeService.enregistrerFichier(fichier);
            PieceCandidatureFormateur piece = pieceRepository.save(
                    new PieceCandidatureFormateur(candidature, typeDocument, fichierStocke));
            return ResponseEntity.status(HttpStatus.CREATED).body(toPieceDto(piece));
        } catch (IllegalArgumentException invalide) {
            return ResponseEntity.badRequest().body(Map.of("message", invalide.getMessage()));
        } catch (RuntimeException erreur) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Le fichier n'a pas pu être enregistré."));
        }
    }

    @DeleteMapping("/candidatures/{id}/pieces/{pieceId}")
    @Transactional
    public ResponseEntity<?> supprimerPiece(@PathVariable Long id, @PathVariable Long pieceId,
            Authentication authentication) {
        CandidatureFormateur candidature = candidaturePropriete(id, authentication);
        if (candidature == null) {
            return ResponseEntity.notFound().build();
        }
        if (candidature.getStatut() == EnumStatutCandidatureFormateur.TERMINE) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Cette candidature est close : ses pièces ne peuvent plus être modifiées."));
        }
        PieceCandidatureFormateur piece = pieceRepository.findById(pieceId).orElse(null);
        if (piece == null || piece.getCandidature() == null
                || !piece.getCandidature().getId().equals(candidature.getId())) {
            return ResponseEntity.notFound().build();
        }
        supprimerFichierDisque(piece);
        pieceRepository.delete(piece); // orphanRemoval retire le FichierStocke associé
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/candidatures/{id}/pieces/{pieceId}/fichier")
    @Transactional(readOnly = true)
    public ResponseEntity<?> telechargerPiece(@PathVariable Long id, @PathVariable Long pieceId,
            Authentication authentication) {
        CandidatureFormateur candidature = candidaturePropriete(id, authentication);
        if (candidature == null) {
            return ResponseEntity.notFound().build();
        }
        return servirPiece(pieceRepository.findById(pieceId).orElse(null), candidature.getId());
    }

    // ===================== Utilitaires =====================

    static ResponseEntity<?> servirPiece(PieceCandidatureFormateur piece, Long candidatureId) {
        if (piece == null || piece.getCandidature() == null
                || !piece.getCandidature().getId().equals(candidatureId) || piece.getFichierStocke() == null) {
            return ResponseEntity.notFound().build();
        }
        return servirFichier(piece.getFichierStocke());
    }

    /** Envoie un fichier stocké en téléchargement ; 404 s'il n'existe plus sur le disque. */
    static ResponseEntity<?> servirFichier(FichierStocke f) {
        var chemin = Paths.get(f.getCheminStockage());
        if (!Files.isRegularFile(chemin)) {
            return ResponseEntity.notFound().build();
        }
        byte[] contenu;
        try {
            contenu = Files.readAllBytes(chemin);
        } catch (java.io.IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        MediaType type = f.getMimeType() != null && !f.getMimeType().isBlank()
                ? MediaType.parseMediaType(f.getMimeType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        org.springframework.http.ContentDisposition.attachment().filename(f.getNomOriginal()).build()
                                .toString())
                .body(contenu);
    }

    static void supprimerFichierDisque(PieceCandidatureFormateur piece) {
        if (piece.getFichierStocke() == null || piece.getFichierStocke().getCheminStockage() == null) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(piece.getFichierStocke().getCheminStockage()));
        } catch (java.io.IOException ignore) {
            // Best-effort : la ligne en base part de toute façon (orphanRemoval), le fichier
            // orphelin sur disque n'a pas d'impact fonctionnel.
        }
    }

    private void garantirRoleCandidatFormateur(User user) {
        boolean deja = user.getRoles().stream().anyMatch(r -> "ROLE_CANDIDAT_FORMATEUR".equals(r.getCode()));
        if (deja) {
            return;
        }
        Role role = roleRepository.findByCode("ROLE_CANDIDAT_FORMATEUR");
        if (role == null) {
            throw new IllegalStateException("Le rôle ROLE_CANDIDAT_FORMATEUR est introuvable.");
        }
        user.getRoles().clear();
        user.addRole(role);
    }

    private CandidatureFormateur candidaturePropriete(Long id, Authentication authentication) {
        User user = utilisateurCourant.utilisateur(authentication);
        if (user == null) {
            return null;
        }
        CandidatureFormateur candidature = candidatureRepository.findById(id).orElse(null);
        if (candidature == null || candidature.getCandidat() == null
                || !candidature.getCandidat().getId().equals(user.getId())) {
            return null;
        }
        return candidature;
    }

    /** Persiste les blocs répétables du dossier enrichi (formations, expériences, langues, certifications). */
    private void enregistrerDossierEnrichi(CandidatureFormateur candidature, CandidatureFormateurCreateRequest requete) {
        if (requete.formations() != null) {
            for (FormationRequest f : requete.formations()) {
                if (f == null) {
                    continue;
                }
                formationRepository.save(new FormationCandidatureFormateur(candidature, vide(f.niveauEtude()),
                        vide(f.pays()), vide(f.ville()), vide(f.etablissement()), vide(f.intituleDiplome()),
                        vide(f.matierePrincipale()), parseDate(f.dateDebut()), parseDate(f.dateObtention())));
            }
        }
        if (requete.experiences() != null) {
            for (ExperienceRequest e : requete.experiences()) {
                if (e == null) {
                    continue;
                }
                experienceRepository.save(new ExperienceCandidatureFormateur(candidature, vide(e.employeur()),
                        vide(e.pays()), vide(e.ville()), e.posteActuel(), vide(e.intitulePoste()),
                        parseDate(e.dateDebut()), parseDate(e.dateFin()), vide(e.nomSuperviseur()),
                        vide(e.titreSuperviseur()), vide(e.telephoneSuperviseur()), vide(e.emailSuperviseur()),
                        e.contactAutorise(), vide(e.motifNonContact()), vide(e.motifDepart())));
            }
        }
        if (requete.langues() != null) {
            for (LangueRequest l : requete.langues()) {
                if (l == null || estVide(l.langue()) || estVide(l.niveau())) {
                    continue;
                }
                try {
                    langueRepository.save(new LangueCandidatureFormateur(candidature, l.langue().trim(),
                            EnumNiveauLangue.valueOf(l.niveau())));
                } catch (IllegalArgumentException niveauInvalide) {
                    // Niveau inconnu : ligne ignorée plutôt que de faire échouer tout le dépôt.
                }
            }
        }
        if (requete.certifications() != null) {
            for (CertificationRequest c : requete.certifications()) {
                if (c == null || estVide(c.intitule())) {
                    continue;
                }
                certificationRepository.save(new CertificationCandidatureFormateur(candidature, c.intitule().trim(),
                        vide(c.organisme()), parseDate(c.dateObtention()), parseDate(c.dateExpiration())));
            }
        }
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

    CandidatureFormateurDto toDto(CandidatureFormateur c) {
        return AdminRecrutementFormateurController.candidatureDto(c,
                pieceRepository.findByCandidatureId(c.getId()),
                formationRepository.findByCandidatureId(c.getId()),
                experienceRepository.findByCandidatureId(c.getId()),
                langueRepository.findByCandidatureId(c.getId()),
                certificationRepository.findByCandidatureId(c.getId()));
    }

    private PieceCandidatureFormateurDto toPieceDto(PieceCandidatureFormateur piece) {
        return new PieceCandidatureFormateurDto(piece.getId(), piece.getTypeDocument().name(),
                piece.getFichierStocke() != null ? piece.getFichierStocke().getNomOriginal() : null);
    }

    private boolean estVide(String v) {
        return v == null || v.isBlank();
    }

    private String vide(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
