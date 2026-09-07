package inpg.sn.lms.accreditation.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import inpg.sn.lms.accreditation.dto.DemandeAccreditationCreateRequest;
import inpg.sn.lms.accreditation.dto.DemandeAccreditationDto;
import inpg.sn.lms.accreditation.dto.PieceAccreditationDto;
import inpg.sn.lms.accreditation.entity.DemandeAccreditation;
import inpg.sn.lms.accreditation.entity.PieceAccreditation;
import inpg.sn.lms.accreditation.enums.EnumNiveauProgramme;
import inpg.sn.lms.accreditation.enums.EnumStatutDemandeAccreditation;
import inpg.sn.lms.accreditation.enums.EnumTypeDocumentAccreditation;
import inpg.sn.lms.accreditation.enums.EnumTypeStructure;
import inpg.sn.lms.accreditation.repository.DemandeAccreditationRepository;
import inpg.sn.lms.accreditation.repository.PieceAccreditationRepository;
import inpg.sn.lms.accreditation.service.AccreditationNotificationService;
import inpg.sn.lms.accreditation.service.PieceAccreditationStorageService;
import inpg.sn.lms.auth.entity.EcoleFormation;
import inpg.sn.lms.auth.entity.Role;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.EcoleFormationRepository;
import inpg.sn.lms.auth.repository.RoleRepository;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;

/**
 * Espace École de formation (TDR §F) : création de compte, soumission d'une
 * demande d'accréditation, pièces jointes, suivi de l'état.
 *
 * <p>Vit sous {@code /api/accreditation/ecole/**}, {@code authenticated()}
 * dans {@code SecurityConfig} — pas {@code hasRole}, car {@code
 * ROLE_ECOLE_FORMATION} n'est accordé qu'à la première soumission réussie
 * (voir {@link #soumettreDemande}), même patron que {@code CandidatController
 * #garantirRoleCandidat}.
 */
@RestController
@RequestMapping("/api/accreditation/ecole")
public class EcoleAccreditationController {

    /**
     * Un compte ne détient qu'un seul rôle à la fois (règle en vigueur dans tout
     * le dépôt) : une école ne peut pas déposer une demande depuis un compte qui
     * donne déjà accès à un autre espace, sous peine de le lui faire perdre
     * silencieusement.
     */
    private static final Set<String> ROLES_OUVRANT_UN_ESPACE = Set.of(
            "ROLE_ETUDIANT", "ROLE_FORMATEUR", "ROLE_RESPONSABLE_PEDAGOGIQUE",
            "ROLE_RESPONSABLE_ADMISSION", "ROLE_ALUMNI", "ROLE_EVALUATEUR_ACCREDITATION", "ROLE_CANDIDAT");

    private final EcoleFormationRepository ecoleFormationRepository;
    private final DemandeAccreditationRepository demandeRepository;
    private final PieceAccreditationRepository pieceRepository;
    private final RoleRepository roleRepository;
    private final UtilisateurCourantService utilisateurCourant;
    private final PieceAccreditationStorageService storage;
    private final AccreditationNotificationService notificationService;

    public EcoleAccreditationController(EcoleFormationRepository ecoleFormationRepository,
            DemandeAccreditationRepository demandeRepository, PieceAccreditationRepository pieceRepository,
            RoleRepository roleRepository, UtilisateurCourantService utilisateurCourant,
            PieceAccreditationStorageService storage, AccreditationNotificationService notificationService) {
        this.ecoleFormationRepository = ecoleFormationRepository;
        this.demandeRepository = demandeRepository;
        this.pieceRepository = pieceRepository;
        this.roleRepository = roleRepository;
        this.utilisateurCourant = utilisateurCourant;
        this.storage = storage;
        this.notificationService = notificationService;
    }

    /**
     * Crée (si besoin) le profil école et soumet une nouvelle demande. Une école
     * ne peut avoir qu'une seule demande active — l'index partiel de la
     * migration V15 est le garde-fou réel, ce contrôle applicatif n'est qu'un
     * message d'erreur lisible avant d'atteindre la base.
     */
    @PostMapping("/demande")
    @Transactional
    public ResponseEntity<?> soumettreDemande(@RequestBody DemandeAccreditationCreateRequest requete,
            Authentication authentication) {
        User user = utilisateurCourant.utilisateur(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (estVide(requete.nomEcole()) || estVide(requete.adresse()) || estVide(requete.telephone())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Le nom de l'école, l'adresse et le téléphone sont obligatoires."));
        }
        if (estVide(requete.intituleProgramme()) || estVide(requete.niveauProgramme())
                || estVide(requete.typeStructure())) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    "L'intitulé du programme, son niveau et le type de structure sont obligatoires : "
                            + "l'accréditation porte sur un programme, pas sur l'établissement."));
        }
        EnumNiveauProgramme niveauProgramme;
        EnumTypeStructure typeStructure;
        try {
            niveauProgramme = EnumNiveauProgramme.valueOf(requete.niveauProgramme());
            typeStructure = EnumTypeStructure.valueOf(requete.typeStructure());
        } catch (IllegalArgumentException invalide) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Niveau de programme ou type de structure invalide."));
        }
        if (requete.effectifCohorte() != null && requete.effectifCohorte() <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "L'effectif de la cohorte doit être un nombre positif."));
        }
        LocalDate dateDebutCohorte = null;
        if (!estVide(requete.dateDebutCohorte())) {
            try {
                dateDebutCohorte = LocalDate.parse(requete.dateDebutCohorte().trim());
            } catch (DateTimeParseException invalide) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "La date de début de la cohorte est invalide (format attendu AAAA-MM-JJ)."));
            }
        }
        boolean dejaEcole = user.getRoles().stream().anyMatch(r -> "ROLE_ECOLE_FORMATION".equals(r.getCode()));
        if (!dejaEcole) {
            Optional<Role> roleOuvrant = user.getRoles().stream()
                    .filter(r -> ROLES_OUVRANT_UN_ESPACE.contains(r.getCode()))
                    .findFirst();
            if (roleOuvrant.isPresent()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "message", "Ce compte donne déjà accès à l'espace « " + roleOuvrant.get().getLibelle()
                                + " ». Pour déposer une demande d'accréditation, utilisez un compte distinct."));
            }
        }

        EcoleFormation ecole = ecoleFormationRepository.findByUserId(user.getId()).orElse(null);
        if (ecole == null) {
            ecole = ecoleFormationRepository.save(
                    new EcoleFormation(user, requete.nomEcole().trim(), requete.adresse().trim(),
                            requete.telephone().trim(), estVide(requete.siteWeb()) ? null : requete.siteWeb().trim()));
        } else {
            ecole.setNomEcole(requete.nomEcole().trim());
            ecole.setAdresse(requete.adresse().trim());
            ecole.setTelephone(requete.telephone().trim());
            ecole.setSiteWeb(estVide(requete.siteWeb()) ? null : requete.siteWeb().trim());
            ecoleFormationRepository.save(ecole);
        }

        List<DemandeAccreditation> actives = demandeRepository.findByEcoleIdAndStatutIn(ecole.getId(),
                List.of(EnumStatutDemandeAccreditation.SOUMISE, EnumStatutDemandeAccreditation.EN_COURS_EVALUATION));
        if (!actives.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "message", "Une demande est déjà en cours d'examen (n°" + actives.get(0).getId()
                            + ") : elle doit être tranchée avant tout nouveau dépôt."));
        }

        DemandeAccreditation demande = new DemandeAccreditation(ecole);
        demande.setIntituleProgramme(requete.intituleProgramme().trim());
        demande.setNiveauProgramme(niveauProgramme);
        demande.setTypeStructure(typeStructure);
        demande.setEffectifCohorte(requete.effectifCohorte());
        demande.setDateDebutCohorte(dateDebutCohorte);
        demande = demandeRepository.save(demande);
        garantirRoleEcole(user);
        notificationService.notifierNouvelleDemande(ecole.getNomEcole());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(demande));
    }

    @GetMapping("/mes-demandes")
    @Transactional(readOnly = true)
    public ResponseEntity<?> mesDemandes(Authentication authentication) {
        EcoleFormation ecole = utilisateurCourant.ecoleFormation(authentication);
        if (ecole == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(demandeRepository.findByEcoleIdOrderByDateSoumissionDesc(ecole.getId()).stream()
                .map(this::toDto)
                .toList());
    }

    @GetMapping("/demande/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> demande(@PathVariable Long id, Authentication authentication) {
        DemandeAccreditation demande = demandePropriete(id, authentication);
        if (demande == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toDto(demande));
    }

    /**
     * Dépose une pièce. Autorisé tant que la demande n'est pas tranchée : une
     * école qui a oublié un document peut le compléter pendant l'évaluation.
     */
    @PostMapping("/demande/{id}/pieces")
    @Transactional
    public ResponseEntity<?> televerserPiece(@PathVariable Long id,
            @RequestParam("typeDocument") String typeDocumentBrut,
            @RequestParam("fichier") MultipartFile fichier,
            Authentication authentication) {
        DemandeAccreditation demande = demandePropriete(id, authentication);
        if (demande == null) {
            return ResponseEntity.notFound().build();
        }
        if (demande.getStatut() == EnumStatutDemandeAccreditation.ACCREDITE
                || demande.getStatut() == EnumStatutDemandeAccreditation.REJETE) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Cette demande est déjà tranchée : elle n'accepte plus de nouvelle pièce."));
        }
        EnumTypeDocumentAccreditation typeDocument;
        try {
            typeDocument = EnumTypeDocumentAccreditation.valueOf(typeDocumentBrut);
        } catch (IllegalArgumentException invalide) {
            return ResponseEntity.badRequest().body(Map.of("message", "Type de document invalide."));
        }
        try {
            var fichierStocke = storage.enregistrer(fichier);
            PieceAccreditation piece = pieceRepository.save(
                    new PieceAccreditation(typeDocument, demande, fichierStocke));
            return ResponseEntity.status(HttpStatus.CREATED).body(toPieceDto(piece));
        } catch (IllegalArgumentException invalide) {
            return ResponseEntity.badRequest().body(Map.of("message", invalide.getMessage()));
        } catch (IOException erreur) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Le fichier n'a pas pu être enregistré."));
        }
    }

    /**
     * Retire une pièce, tant que la demande n'est pas tranchée. Permet à l'école
     * de corriger une pièce erronée (le « remplacement » côté client = retrait
     * puis nouveau dépôt du même type).
     */
    @org.springframework.web.bind.annotation.DeleteMapping("/demande/{id}/pieces/{pieceId}")
    @Transactional
    public ResponseEntity<?> supprimerPiece(@PathVariable Long id, @PathVariable Long pieceId,
            Authentication authentication) {
        DemandeAccreditation demande = demandePropriete(id, authentication);
        if (demande == null) {
            return ResponseEntity.notFound().build();
        }
        if (demande.getStatut() == EnumStatutDemandeAccreditation.ACCREDITE
                || demande.getStatut() == EnumStatutDemandeAccreditation.REJETE) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Cette demande est déjà tranchée : ses pièces ne peuvent plus être modifiées."));
        }
        PieceAccreditation piece = pieceRepository.findById(pieceId).orElse(null);
        if (piece == null || piece.getDemande() == null || !piece.getDemande().getId().equals(demande.getId())) {
            return ResponseEntity.notFound().build();
        }
        if (piece.getFichierStocke() != null) {
            var chemin = storage.cheminDe(piece.getFichierStocke());
            if (chemin != null) {
                try {
                    java.nio.file.Files.deleteIfExists(chemin);
                } catch (IOException ignore) {
                    // Best-effort : la ligne en base part de toute façon, le fichier orphelin sur
                    // disque n'a pas d'impact fonctionnel.
                }
            }
        }
        pieceRepository.delete(piece); // orphanRemoval retire le FichierStocke associé
        return ResponseEntity.noContent().build();
    }

    /** Relit une pièce déjà déposée. */
    @GetMapping("/demande/{id}/pieces/{pieceId}/fichier")
    @Transactional(readOnly = true)
    public ResponseEntity<org.springframework.core.io.Resource> telechargerPiece(@PathVariable Long id,
            @PathVariable Long pieceId, Authentication authentication) {
        DemandeAccreditation demande = demandePropriete(id, authentication);
        if (demande == null) {
            return ResponseEntity.notFound().build();
        }
        PieceAccreditation piece = pieceRepository.findById(pieceId).orElse(null);
        if (piece == null || piece.getDemande() == null || !piece.getDemande().getId().equals(demande.getId())
                || piece.getFichierStocke() == null) {
            return ResponseEntity.notFound().build();
        }
        return servirFichier(piece, storage);
    }

    // ===================== Utilitaires =====================

    static ResponseEntity<org.springframework.core.io.Resource> servirFichier(PieceAccreditation piece,
            PieceAccreditationStorageService storage) {
        var chemin = storage.cheminDe(piece.getFichierStocke());
        if (chemin == null || !java.nio.file.Files.isRegularFile(chemin)) {
            return ResponseEntity.notFound().build();
        }
        String type = piece.getFichierStocke().getMimeType();
        org.springframework.http.MediaType mediaType = type != null && !type.isBlank()
                ? org.springframework.http.MediaType.parseMediaType(type)
                : org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        org.springframework.http.ContentDisposition.attachment()
                                .filename(piece.getFichierStocke().getNomOriginal()).build().toString())
                .body(new org.springframework.core.io.FileSystemResource(chemin));
    }

    private void garantirRoleEcole(User user) {
        boolean dejaEcole = user.getRoles().stream().anyMatch(r -> "ROLE_ECOLE_FORMATION".equals(r.getCode()));
        if (dejaEcole) {
            return;
        }
        Role roleEcole = roleRepository.findByCode("ROLE_ECOLE_FORMATION");
        if (roleEcole == null) {
            throw new IllegalStateException("Le rôle ROLE_ECOLE_FORMATION est introuvable.");
        }
        user.getRoles().clear();
        user.addRole(roleEcole);
    }

    /** Une demande, seulement si elle appartient à l'école connectée. */
    private DemandeAccreditation demandePropriete(Long id, Authentication authentication) {
        EcoleFormation ecole = utilisateurCourant.ecoleFormation(authentication);
        if (ecole == null) {
            return null;
        }
        DemandeAccreditation demande = demandeRepository.findById(id).orElse(null);
        if (demande == null || !demande.getEcole().getId().equals(ecole.getId())) {
            return null;
        }
        return demande;
    }

    private DemandeAccreditationDto toDto(DemandeAccreditation demande) {
        List<PieceAccreditationDto> pieces = pieceRepository.findByDemandeId(demande.getId()).stream()
                .map(this::toPieceDto)
                .toList();
        return new DemandeAccreditationDto(
                demande.getId(),
                demande.getStatut().name(),
                demande.getDateSoumission(),
                demande.getDateDecision(),
                demande.getDateExpiration(),
                demande.getMotifRejet(),
                demande.getCommentaireEvaluation(),
                demande.getPlanActions(),
                demande.getEcole().getId(),
                demande.getEcole().getNomEcole(),
                demande.getIntituleProgramme(),
                demande.getNiveauProgramme() != null ? demande.getNiveauProgramme().name() : null,
                demande.getTypeStructure() != null ? demande.getTypeStructure().name() : null,
                demande.getEffectifCohorte(),
                demande.getDateDebutCohorte(),
                pieces);
    }

    private PieceAccreditationDto toPieceDto(PieceAccreditation piece) {
        return new PieceAccreditationDto(
                piece.getId(),
                piece.getTypeDocument().name(),
                piece.getFichierStocke() != null ? piece.getFichierStocke().getNomOriginal() : null);
    }

    private boolean estVide(String valeur) {
        return valeur == null || valeur.isBlank();
    }
}
