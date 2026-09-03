package inpg.sn.lms.espace_etudiant.controller;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

import inpg.sn.lms.auth.dto.EtudiantDto;
import inpg.sn.lms.auth.entity.AlumniProfil;
import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.AlumniProfilRepository;
import inpg.sn.lms.auth.repository.CandidatRepository;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.auth.service.EtudiantService;
import inpg.sn.lms.espace_alumni.entity.EnqueteSuivi;
import inpg.sn.lms.espace_alumni.repository.EnqueteSuiviRepository;
import inpg.sn.lms.espace_alumni.repository.ReponseEnqueteSuiviRepository;
import inpg.sn.lms.espace_alumni.service.AlumniProfilService;
import inpg.sn.lms.espace_etudiant.dto.CertificatCreateRequest;
import inpg.sn.lms.espace_etudiant.dto.CertificatDto;
import inpg.sn.lms.espace_etudiant.dto.EnqueteSuiviCreateRequest;
import inpg.sn.lms.espace_etudiant.dto.EtudiantDetailDto;
import inpg.sn.lms.espace_etudiant.dto.EtudiantSuiviDto;
import inpg.sn.lms.espace_etudiant.dto.PromotionCreateRequest;
import inpg.sn.lms.espace_etudiant.dto.PromotionDetailDto;
import inpg.sn.lms.espace_etudiant.dto.QualiteCoursDto;
import inpg.sn.lms.espace_etudiant.dto.ReleveCreateRequest;
import inpg.sn.lms.espace_etudiant.dto.ReleveDelivreDto;
import inpg.sn.lms.espace_etudiant.dto.ReleveNotesDto;
import inpg.sn.lms.espace_etudiant.dto.RelanceCollectiveRequest;
import inpg.sn.lms.espace_etudiant.dto.SyntheseEnqueteSuiviDto;
import inpg.sn.lms.espace_etudiant.entity.Certificat;
import inpg.sn.lms.espace_etudiant.entity.Formation;
import inpg.sn.lms.espace_etudiant.entity.Notification;
import inpg.sn.lms.espace_etudiant.entity.ReleveLigne;
import inpg.sn.lms.espace_etudiant.entity.ReleveNotes;
import inpg.sn.lms.espace_etudiant.entity.RetourEvaluation;
import inpg.sn.lms.espace_etudiant.entity.Promotion;
import inpg.sn.lms.espace_etudiant.repository.CoursRepository;
import inpg.sn.lms.espace_etudiant.repository.FormationRepository;
import inpg.sn.lms.espace_etudiant.repository.ForumMentionRepository;
import inpg.sn.lms.espace_etudiant.repository.ForumMessageRepository;
import inpg.sn.lms.espace_etudiant.repository.ForumSujetRepository;
import inpg.sn.lms.espace_etudiant.repository.NotificationRepository;
import inpg.sn.lms.espace_etudiant.repository.PromotionRepository;
import inpg.sn.lms.espace_etudiant.repository.RetourEvaluationRepository;
import inpg.sn.lms.espace_etudiant.service.CertificatService;
import inpg.sn.lms.espace_etudiant.service.CompletionProgrammeService;
import inpg.sn.lms.espace_etudiant.service.EtudiantImportService;
import inpg.sn.lms.espace_etudiant.service.EtudiantSuppressionService;
import inpg.sn.lms.espace_etudiant.service.MatriculeService;
import inpg.sn.lms.espace_etudiant.service.RapportQualitePdfService;
import inpg.sn.lms.espace_etudiant.service.RelevePdfService;
import inpg.sn.lms.espace_etudiant.service.ReleveDelivranceService;
import inpg.sn.lms.espace_etudiant.service.ReleveNotesService;
import inpg.sn.lms.recrutement.service.NotificationService;

/**
 * Scolarité pilotée par le responsable pédagogique : promotions, étudiants,
 * certificats et rappels d'évaluation.
 *
 * <p>Ces attributions étaient dispersées ailleurs, chacune dans un espace qui n'en
 * avait pas la charge. La création d'un compte étudiant relevait du responsable
 * d'admission, dont le rôle s'arrête à l'admission du dossier ; la délivrance d'un
 * certificat relevait du formateur, alors qu'elle engage l'établissement et non
 * l'enseignant. Elles reviennent ici.
 *
 * <p>Une promotion ne se rattache plus à des cours : elle suit une <strong>formation</strong>,
 * et hérite de son programme. C'est pourquoi le rattachement à une formation est
 * mis en avant à la création — une promotion sans formation ne donne accès à aucun
 * cours.
 */
@RestController
@RequestMapping("/api/pedagogie")
@PreAuthorize("hasRole('RESPONSABLE_PEDAGOGIQUE')")
public class PedagogieScolariteController {

    private static final Logger log = LoggerFactory.getLogger(PedagogieScolariteController.class);
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PromotionRepository promotionRepository;
    private final FormationRepository formationRepository;
    private final EtudiantRepository etudiantRepository;
    private final EtudiantService etudiantService;
    private final MatriculeService matriculeService;
    private final EtudiantImportService etudiantImportService;
    private final EtudiantSuppressionService etudiantSuppressionService;
    private final CertificatService certificatService;
    private final CoursRepository coursRepository;
    private final ForumSujetRepository forumSujetRepository;
    private final ForumMessageRepository forumMessageRepository;
    private final ForumMentionRepository forumMentionRepository;
    private final NotificationService notificationService;
    private final RetourEvaluationRepository retourRepository;
    private final ReleveNotesService releveNotesService;
    private final ReleveDelivranceService releveDelivranceService;
    private final CandidatRepository candidatRepository;
    private final AlumniProfilRepository alumniProfilRepository;
    private final AlumniProfilService alumniProfilService;
    private final EnqueteSuiviRepository enqueteSuiviRepository;
    private final ReponseEnqueteSuiviRepository reponseEnqueteSuiviRepository;
    private final NotificationRepository notificationRepository;
    private final RapportQualitePdfService rapportQualitePdfService;
    private final RelevePdfService relevePdfService;
    private final CompletionProgrammeService completionProgrammeService;

    public PedagogieScolariteController(PromotionRepository promotionRepository,
            FormationRepository formationRepository, EtudiantRepository etudiantRepository,
            EtudiantService etudiantService, MatriculeService matriculeService,
            EtudiantImportService etudiantImportService, EtudiantSuppressionService etudiantSuppressionService,
            CertificatService certificatService, CoursRepository coursRepository,
            ForumSujetRepository forumSujetRepository, ForumMessageRepository forumMessageRepository,
            ForumMentionRepository forumMentionRepository,
            NotificationService notificationService, RetourEvaluationRepository retourRepository,
            ReleveNotesService releveNotesService, ReleveDelivranceService releveDelivranceService,
            CandidatRepository candidatRepository, AlumniProfilRepository alumniProfilRepository,
            AlumniProfilService alumniProfilService, EnqueteSuiviRepository enqueteSuiviRepository,
            ReponseEnqueteSuiviRepository reponseEnqueteSuiviRepository,
            NotificationRepository notificationRepository, RapportQualitePdfService rapportQualitePdfService,
            RelevePdfService relevePdfService,
            CompletionProgrammeService completionProgrammeService) {
        this.promotionRepository = promotionRepository;
        this.formationRepository = formationRepository;
        this.etudiantRepository = etudiantRepository;
        this.etudiantService = etudiantService;
        this.matriculeService = matriculeService;
        this.etudiantImportService = etudiantImportService;
        this.etudiantSuppressionService = etudiantSuppressionService;
        this.certificatService = certificatService;
        this.coursRepository = coursRepository;
        this.forumSujetRepository = forumSujetRepository;
        this.forumMessageRepository = forumMessageRepository;
        this.forumMentionRepository = forumMentionRepository;
        this.notificationService = notificationService;
        this.retourRepository = retourRepository;
        this.releveNotesService = releveNotesService;
        this.releveDelivranceService = releveDelivranceService;
        this.candidatRepository = candidatRepository;
        this.alumniProfilRepository = alumniProfilRepository;
        this.alumniProfilService = alumniProfilService;
        this.enqueteSuiviRepository = enqueteSuiviRepository;
        this.reponseEnqueteSuiviRepository = reponseEnqueteSuiviRepository;
        this.notificationRepository = notificationRepository;
        this.rapportQualitePdfService = rapportQualitePdfService;
        this.relevePdfService = relevePdfService;
        this.completionProgrammeService = completionProgrammeService;
    }

    // ===================== PROMOTIONS =====================

    /**
     * Liste des promotions avec leur programme et leurs effectifs.
     *
     * <p>Cet endpoint vivait dans {@code PedagogieController} et ne renvoyait que
     * {@code {id, code, libelle, annee}}. L'écran, lui, affiche le programme rattaché :
     * toutes les promotions se présentaient donc comme « sans programme », y compris
     * celles qui en avaient un. Plus grave, l'écran d'édition ne retrouvait pas la
     * formation et renvoyait {@code formationId: null} — un simple aller-retour dans la
     * fiche <strong>effaçait</strong> le rattachement.
     *
     * <p>La lecture rejoint donc l'écriture, dans le contrôleur qui porte déjà le
     * {@link PromotionDetailDto}. Deux contrôleurs répondant sur {@code /api/pedagogie},
     * garder les deux aurait produit un conflit de routage au démarrage.
     */
    @GetMapping("/promotions")
    @Transactional(readOnly = true)
    public ResponseEntity<List<PromotionDetailDto>> listerPromotions() {
        return ResponseEntity.ok(promotionRepository.findAll().stream()
                .sorted(Comparator.comparing(Promotion::getCode,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toPromotionDto)
                .toList());
    }

    @PostMapping("/promotions")
    @Transactional
    public ResponseEntity<?> creerPromotion(@RequestBody PromotionCreateRequest request) {
        String erreur = validerPromotion(request);
        if (erreur != null) {
            return ResponseEntity.badRequest().body(Map.of("message", erreur));
        }
        Promotion promotion = new Promotion();
        String appliquee = appliquer(request, promotion);
        if (appliquee != null) {
            return ResponseEntity.badRequest().body(Map.of("message", appliquee));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(toPromotionDto(promotionRepository.save(promotion)));
    }

    @PutMapping("/promotions/{id}")
    @Transactional
    public ResponseEntity<?> modifierPromotion(@PathVariable Long id, @RequestBody PromotionCreateRequest request) {
        Promotion promotion = promotionRepository.findById(id).orElse(null);
        if (promotion == null) {
            return ResponseEntity.notFound().build();
        }
        String erreur = validerPromotion(request);
        if (erreur != null) {
            return ResponseEntity.badRequest().body(Map.of("message", erreur));
        }
        String appliquee = appliquer(request, promotion);
        if (appliquee != null) {
            return ResponseEntity.badRequest().body(Map.of("message", appliquee));
        }
        return ResponseEntity.ok(toPromotionDto(promotionRepository.save(promotion)));
    }

    /**
     * Supprime une promotion vide.
     *
     * <p>Refusée si des étudiants y sont rattachés : la suppression les laisserait
     * sans promotion, donc sans aucun cours ni calendrier, sans que rien ne le
     * signale.
     *
     * <p>Les fils de forum scopés à cette promotion sont détachés avant : sans cela,
     * la contrainte de clé étrangère {@code forum_sujets.promotion_id} ferait
     * échouer la suppression avec une erreur SQL brute, comme c'était déjà le cas
     * pour un cours avant que {@code supprimerCours} ne fasse ce même nettoyage.
     */
    @DeleteMapping("/promotions/{id}")
    @Transactional
    public ResponseEntity<?> supprimerPromotion(@PathVariable Long id) {
        Promotion promotion = promotionRepository.findById(id).orElse(null);
        if (promotion == null) {
            return ResponseEntity.notFound().build();
        }
        long inscrits = etudiantRepository.countByPromotionId(id);
        if (inscrits > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message",
                    inscrits + " étudiant(s) sont rattachés à cette promotion. Réaffectez-les avant de la supprimer."));
        }
        forumMentionRepository.deleteByMessage_Sujet_PromotionId(id);
        forumMessageRepository.deleteBySujet_PromotionId(id);
        forumSujetRepository.deleteByPromotionId(id);
        promotionRepository.delete(promotion);
        return ResponseEntity.noContent().build();
    }

    private String validerPromotion(PromotionCreateRequest request) {
        if (estVide(request.code()) || estVide(request.libelle())) {
            return "Le code et le libellé de la promotion sont requis.";
        }
        if (request.dateDebut() != null && request.dateFin() != null
                && request.dateFin().isBefore(request.dateDebut())) {
            return "La date de fin ne peut pas précéder la date de début.";
        }
        return null;
    }

    private String appliquer(PromotionCreateRequest request, Promotion promotion) {
        promotion.setCode(request.code().trim());
        promotion.setLibelle(request.libelle().trim());
        promotion.setDateDebut(request.dateDebut());
        promotion.setDateFin(request.dateFin());
        promotion.setProgramme(request.programme());
        promotion.setActivitesParascolaires(request.activitesParascolaires());
        if (request.formationId() == null) {
            promotion.setFormation(null);
            return null;
        }
        Formation formation = formationRepository.findById(request.formationId()).orElse(null);
        if (formation == null) {
            return "Formation introuvable.";
        }
        promotion.setFormation(formation);
        return null;
    }

    // ===================== ÉTUDIANTS =====================

    /**
     * Répertoire des étudiants, filtrable par nom, matricule ou promotion.
     *
     * <p>Cette consultation relevait du responsable d'admission, dont la mission
     * s'arrête à l'admission du dossier : le suivi de scolarité qui suit revient au
     * responsable pédagogique.
     */
    @GetMapping("/etudiants")
    @Transactional(readOnly = true)
    public ResponseEntity<?> listerEtudiants(@RequestParam(required = false) String recherche) {
        List<Etudiant> etudiants = estVide(recherche)
                ? etudiantRepository.findAll()
                : etudiantRepository.rechercher(recherche.trim());
        return ResponseEntity.ok(etudiants.stream().map(this::toEtudiantDto).toList());
    }

    /**
     * Fiche complète d'un étudiant : coordonnées comprises, à la différence du
     * répertoire ({@link #listerEtudiants}) qui alimente aussi l'écran du formateur.
     */
    @GetMapping("/etudiants/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> detailEtudiant(@PathVariable Long id) {
        return etudiantRepository.findById(id)
                .<ResponseEntity<?>>map(e -> ResponseEntity.ok(toEtudiantDetailDto(e)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Supprime un compte étudiant. Refusée si un dossier de candidature ou un
     * historique alumni existe, ou si l'étudiant a publié dans le forum — voir
     * {@link EtudiantSuppressionService}.
     */
    @DeleteMapping("/etudiants/{id}")
    @Transactional
    public ResponseEntity<?> supprimerEtudiant(@PathVariable Long id) {
        if (!etudiantRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        try {
            etudiantSuppressionService.supprimer(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException erreur) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", erreur.getMessage()));
        }
    }

    /**
     * Ouvre un compte étudiant et lui attribue son matricule.
     *
     * <p>Le mot de passe est optionnel : laissé vide, {@link EtudiantService} en
     * génère un et le réécrit dans {@code requete}, qui sert alors à l'envoyer par
     * courriel au titulaire du compte. Un mot de passe saisi à la main n'est jamais
     * envoyé par courriel — la personne qui l'a choisi le connaît déjà.
     */
    @PostMapping("/etudiants")
    @Transactional
    public ResponseEntity<?> creerEtudiant(@RequestBody EtudiantDto requete) {
        if (estVide(requete.getUsername()) || estVide(requete.getEmail())
                || estVide(requete.getNom()) || estVide(requete.getPrenom()) || estVide(requete.getTelephone())) {
            // Le telephone n'est pas facultatif : Etudiant.telephone est NOT NULL UNIQUE en
            // base (voir EtudiantImportService). Sans ce garde-fou, une omission se solde
            // par une violation de contrainte non rattrapee, remontee en 500 sans message.
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Le nom, le prénom, l'identifiant, l'email et le téléphone sont requis."));
        }
        boolean motDePasseGenere = estVide(requete.getPassword());
        try {
            Etudiant etudiant = etudiantService.createEtudiant(requete);
            matriculeService.attribuerSiAbsent(etudiant);
            if (motDePasseGenere) {
                envoyerIdentifiants(requete.getEmail(), requete.getUsername(), requete.getPassword());
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(toEtudiantDto(etudiantRepository.save(etudiant)));
        } catch (IllegalStateException erreur) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", erreur.getMessage()));
        }
    }

    /**
     * Transmet les identifiants d'un compte nouvellement créé. Une panne SMTP ne
     * doit pas faire échouer la création elle-même — le compte reste utilisable,
     * seul l'envoi manque, d'où le rattrapage plutôt qu'une exception propagée.
     */
    private void envoyerIdentifiants(String email, String identifiant, String motDePasse) {
        try {
            notificationService.envoyerEmail(email, "Votre compte INPG LMS",
                    "Un compte vient de vous être créé sur la plateforme INPG LMS.\n\n"
                            + "Identifiant : " + identifiant + "\n"
                            + "Mot de passe provisoire : " + motDePasse + "\n\n"
                            + "Merci de le modifier dès votre première connexion.");
        } catch (Exception exception) {
            log.error("Échec de l'envoi des identifiants à {} : {}", email, exception.getMessage());
        }
    }

    /** Rattache un étudiant à une promotion, ou l'en détache si {@code promotionId} est nul. */
    @PutMapping("/etudiants/{id}/promotion")
    @Transactional
    public ResponseEntity<?> affecterPromotion(@PathVariable Long id, @RequestBody Map<String, Long> corps) {
        Etudiant etudiant = etudiantRepository.findById(id).orElse(null);
        if (etudiant == null) {
            return ResponseEntity.notFound().build();
        }
        Long promotionId = corps.get("promotionId");
        if (promotionId == null) {
            etudiant.setPromotion(null);
        } else {
            Promotion promotion = promotionRepository.findById(promotionId).orElse(null);
            if (promotion == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Promotion introuvable."));
            }
            etudiant.setPromotion(promotion);
        }
        matriculeService.attribuerSiAbsent(etudiant);
        return ResponseEntity.ok(toEtudiantDto(etudiantRepository.save(etudiant)));
    }

    /**
     * Fait passer un étudiant côté alumni (TDR §C, « Espace Alumni »).
     *
     * <p>Suit le même patron que la conversion Candidat → Étudiant
     * ({@code AdminRecrutementController#inscrireCommeEtudiant}) : un compte ne
     * détient qu'un seul rôle à la fois, {@code ROLE_ETUDIANT} est donc retiré au
     * profit de {@code ROLE_ALUMNI}. La ligne {@code Etudiant} n'est pas supprimée —
     * c'est elle qui garde le matricule et l'accès aux relevés délivrés.
     */
    @PutMapping("/etudiants/{id}/promouvoir-alumni")
    @Transactional
    public ResponseEntity<?> promouvoirAlumni(@PathVariable Long id) {
        Etudiant etudiant = etudiantRepository.findById(id).orElse(null);
        if (etudiant == null) {
            return ResponseEntity.notFound().build();
        }
        List<String> coursNonTermines = completionProgrammeService.coursNonTermines(etudiant);
        if (!coursNonTermines.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message",
                    "Cet étudiant n'a pas terminé son programme, il ne peut pas encore être promu alumni.",
                    "coursNonTermines", coursNonTermines));
        }
        AlumniProfil profil = alumniProfilService.promouvoir(id);
        if (profil == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le rôle ALUMNI n'est pas configuré."));
        }
        return ResponseEntity.ok(toEtudiantDto(etudiantRepository.findById(id).orElseThrow()));
    }

    /**
     * Import en masse depuis un fichier CSV.
     *
     * <p>Une rentree se compte en dizaines d'inscriptions : les saisir une a une est
     * le poste de travail le plus couteux de la scolarite. Colonnes attendues, dans
     * cet ordre : {@code nom;prenom;email;telephone}. La premiere ligne peut etre un
     * en-tete, elle est ignoree si elle commence par « nom ». Le telephone est
     * requis comme les trois autres colonnes : la colonne {@code Etudiant.telephone}
     * est {@code NOT NULL UNIQUE} en base, une ligne qui l'omet ferait donc echouer
     * l'insertion.
     *
     * <p>L'import est TOLERANT : une ligne invalide n'interrompt pas les autres, et le
     * compte rendu enumere les rejets avec leur motif. Un import de trente etudiants
     * ne doit pas echouer en bloc parce qu'une adresse est mal formee. Chaque ligne
     * est creee dans sa propre transaction ({@link EtudiantImportService}) : sans
     * cela, PostgreSQL abandonne toute la transaction de la requete des la premiere
     * erreur SQL, et chaque ligne suivante echoue a son tour avec « current
     * transaction is aborted » jusqu'a ce que Spring, trouvant la transaction
     * marquee pour retour arriere, fasse echouer toute la requete en 500 — alors que
     * ce controleur avait construit une reponse 200 normale.
     */
    @PostMapping("/etudiants/import")
    public ResponseEntity<?> importerEtudiants(@RequestBody Map<String, Object> corps) {
        String contenu = corps.get("csv") == null ? null : corps.get("csv").toString();
        if (estVide(contenu)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le contenu du fichier est vide."));
        }
        Long promotionId = corps.get("promotionId") == null ? null
                : Long.valueOf(corps.get("promotionId").toString());
        Promotion promotion = null;
        if (promotionId != null) {
            promotion = promotionRepository.findById(promotionId).orElse(null);
            if (promotion == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Promotion introuvable."));
            }
        }

        int crees = 0;
        List<String> rejets = new ArrayList<>();
        String[] lignes = contenu.replace("\r", "").split("\n");
        for (int i = 0; i < lignes.length; i++) {
            String ligne = lignes[i].trim();
            if (ligne.isEmpty()) {
                continue;
            }
            if (i == 0 && ligne.toLowerCase().startsWith("nom")) {
                continue;   // en-tete
            }
            String[] cellules = ligne.split("[;,]", -1);
            if (cellules.length < 4) {
                rejets.add("Ligne " + (i + 1) + " : nom, prenom, email et telephone sont requis.");
                continue;
            }
            String nom = cellules[0].trim();
            String prenom = cellules[1].trim();
            String email = cellules[2].trim();
            String telephone = cellules[3].trim();
            if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || telephone.isEmpty()) {
                rejets.add("Ligne " + (i + 1) + " : nom, prenom, email et telephone sont requis.");
                continue;
            }

            EtudiantDto dto = new EtudiantDto();
            dto.setNom(nom);
            dto.setPrenom(prenom);
            dto.setEmail(email);
            dto.setTelephone(telephone);
            dto.setUsername(email.split("@")[0]);
            // Mot de passe laisse vide : EtudiantService en genere un propre a chaque
            // compte plutot qu'un mot de passe partage entre tous les importes, envoye
            // par courriel ci-dessous.
            try {
                etudiantImportService.creerPourImport(dto, promotion);
                envoyerIdentifiants(dto.getEmail(), dto.getUsername(), dto.getPassword());
                crees++;
            } catch (Exception exception) {
                log.warn("Import etudiant refuse ligne {} : {}", i + 1, exception.getMessage());
                rejets.add("Ligne " + (i + 1) + " (" + email + ") : " + exception.getMessage());
            }
        }
        return ResponseEntity.ok(Map.of("crees", crees, "rejets", rejets));
    }

    // ===================== CERTIFICATS =====================

    @GetMapping("/certificats")
    @Transactional(readOnly = true)
    public ResponseEntity<?> listerCertificats() {
        return ResponseEntity.ok(certificatService.getAll().stream().map(this::toCertificatDto).toList());
    }

    /**
     * Délivre un certificat.
     *
     * <p>Cette délivrance relevait du formateur. Elle engage pourtant l'établissement
     * et non l'enseignant : le nom porté sur le document est désormais celui de
     * l'INPG, et non plus celui de la personne connectée.
     */
    @PostMapping("/certificats")
    @Transactional
    public ResponseEntity<?> delivrerCertificat(@RequestBody CertificatCreateRequest request) {
        if (request.etudiantId() == null || estVide(request.intituleFormation())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "L'étudiant et l'intitulé de la formation sont requis."));
        }
        Etudiant etudiant = etudiantRepository.findById(request.etudiantId()).orElse(null);
        if (etudiant == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Étudiant introuvable."));
        }
        List<String> coursNonTermines = completionProgrammeService.coursNonTermines(etudiant);
        if (!coursNonTermines.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message",
                    "Cet étudiant n'a pas terminé son programme, le certificat ne peut pas encore être délivré.",
                    "coursNonTermines", coursNonTermines));
        }
        Promotion promotion = null;
        if (request.cohorteId() != null) {
            promotion = promotionRepository.findById(request.cohorteId()).orElse(null);
            if (promotion == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Promotion introuvable."));
            }
        } else if (etudiant.getPromotion() != null) {
            promotion = etudiant.getPromotion();
        }

        Certificat certificat = new Certificat();
        certificat.setEtudiant(etudiant);
        certificat.setPromotion(promotion);
        certificat.setIntituleFormation(request.intituleFormation().trim());
        certificat.setFormateurNom("INPG — Direction pédagogique");

        return ResponseEntity.status(HttpStatus.CREATED).body(toCertificatDto(certificatService.save(certificat)));
    }

    // ===================== RELEVES DE NOTES =====================

    /**
     * Relevé courant d'un étudiant, tel qu'il le voit lui-même.
     *
     * <p>Le responsable pédagogique délivre les relevés, mais n'avait aucun moyen de
     * consulter celui d'un étudiant : seul l'intéressé y accédait. Il fallait donc
     * délivrer un document sans pouvoir en vérifier le contenu.
     *
     * <p>Recalculé à chaque appel, contrairement à un relevé délivré.
     */
    @GetMapping("/etudiants/{id}/releve")
    @Transactional(readOnly = true)
    public ResponseEntity<?> releveCourant(@PathVariable Long id) {
        if (!etudiantRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(releveNotesService.duEtudiant(id));
    }

    /** Registre des relevés délivrés, du plus récent au plus ancien. */
    @GetMapping("/releves")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ReleveDelivreDto>> listerReleves() {
        return ResponseEntity.ok(releveDelivranceService.tous().stream().map(this::toReleveDto).toList());
    }

    /** Version PDF d'un relevé délivré — le responsable pédagogique peut télécharger celui de n'importe quel étudiant. */
    @GetMapping("/releves/{id}/pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<?> relevePdf(@PathVariable Long id) {
        ReleveNotes releve = releveDelivranceService.tous().stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElse(null);
        if (releve == null) {
            return ResponseEntity.notFound().build();
        }
        return relevePdfService.reponsePdf(releve);
    }

    /**
     * Délivre un relevé à un ou plusieurs étudiants.
     *
     * <p>La délivrance <strong>fige</strong> les moyennes : le document remis ne changera
     * plus si un formateur corrige une note ensuite. C'est ce qui le distingue de la
     * consultation, et la raison pour laquelle les lignes sont recopiées.
     *
     * <p>Une promotion peut être visée sans lister ses étudiants : en fin de semestre,
     * un relevé se délivre à la promotion entière, et le faire un par un serait la
     * corvée qui pousse à ne pas le faire du tout.
     *
     * <p>Les étudiants sans aucune note sont écartés, et signalés dans le compte rendu :
     * leur délivrer un relevé vide donnerait un document attestant de rien.
     */
    @PostMapping("/releves")
    @Transactional
    public ResponseEntity<?> delivrerReleves(@RequestBody ReleveCreateRequest request) {
        if (estVide(request.periode())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "La période couverte par le relevé est requise."));
        }

        List<Etudiant> vises = new ArrayList<>();
        if (request.etudiantIds() != null && !request.etudiantIds().isEmpty()) {
            for (Long id : request.etudiantIds().stream().distinct().toList()) {
                Etudiant etudiant = etudiantRepository.findById(id).orElse(null);
                if (etudiant == null) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("message", "Étudiant introuvable : " + id));
                }
                vises.add(etudiant);
            }
        } else if (request.promotionId() != null) {
            vises.addAll(etudiantRepository.findByPromotionId(request.promotionId()));
            if (vises.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Aucun étudiant n'est rattaché à cette promotion."));
            }
        } else {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Désignez au moins un étudiant, ou une promotion."));
        }

        String periode = request.periode().trim();
        List<ReleveDelivreDto> delivres = new ArrayList<>();
        List<String> sansNote = new ArrayList<>();
        List<String> reemissions = new ArrayList<>();
        for (Etudiant etudiant : vises) {
            if (releveNotesService.duEtudiant(etudiant.getId()).nombreNotes() == 0) {
                sansNote.add(nomComplet(etudiant));
                continue;
            }
            // Constate avant d'ecrire, sinon le releve qu'on vient de creer compterait
            // comme son propre precedent et tout passerait pour une reemission.
            if (releveDelivranceService.dejaDelivre(etudiant.getId(), periode)) {
                reemissions.add(nomComplet(etudiant));
            }
            delivres.add(toReleveDto(releveDelivranceService.delivrer(etudiant, periode)));
        }

        return ResponseEntity.status(delivres.isEmpty() ? HttpStatus.OK : HttpStatus.CREATED)
                .body(Map.of("delivres", delivres, "sansNote", sansNote, "reemissions", reemissions));
    }

    // ===================== RAPPELS =====================

    /**
     * Rappel adressé aux étudiants d'une promotion, ou à tous si aucune n'est visée.
     *
     * <p>L'envoi se fait par courriel : aucune notification n'est déposée dans
     * l'application, faute de mécanisme côté étudiant. Le compte rendu distingue les
     * envois aboutis des échecs plutôt que d'annoncer un succès global.
     */
    @PostMapping("/rappels")
    @Transactional(readOnly = true)
    public ResponseEntity<?> envoyerRappel(@RequestBody RelanceCollectiveRequest request) {
        if (estVide(request.objet()) || estVide(request.message())) {
            return ResponseEntity.badRequest().body(Map.of("message", "L'objet et le message sont requis."));
        }
        List<Etudiant> destinataires = request.etudiantIds() == null || request.etudiantIds().isEmpty()
                ? etudiantRepository.findAll()
                : etudiantRepository.findAllById(request.etudiantIds());
        if (destinataires.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Aucun destinataire."));
        }

        int envoyes = 0;
        List<String> echecs = new ArrayList<>();
        for (Etudiant etudiant : destinataires) {
            String email = etudiant.getUser() == null ? null : etudiant.getUser().getEmail();
            if (email == null) {
                echecs.add(nomComplet(etudiant) + " (aucune adresse)");
                continue;
            }
            try {
                notificationService.envoyerEmail(email, request.objet(), request.message());
                envoyes++;
            } catch (Exception exception) {
                log.error("Échec de l'envoi du rappel à {} : {}", email, exception.getMessage());
                echecs.add(email);
            }
        }
        return ResponseEntity.ok(Map.of("envoyes", envoyes, "echecs", echecs));
    }

    // ===================== SUIVI QUALITE =====================

    /**
     * Synthese des avis etudiants, par cours (TDR §3.7).
     *
     * <p>Ces avis existaient deja, mais seul le formateur concerne voyait les siens :
     * personne ne disposait d'une vue d'ensemble, alors que c'est elle qui permet de
     * reperer un cours en difficulte. Un avis d'au plus 3 sur 5 est compte comme
     * appelant une action.
     */
    @GetMapping("/qualite")
    @Transactional(readOnly = true)
    public ResponseEntity<?> suiviQualite() {
        List<RetourEvaluation> avis = retourRepository.findAllByOrderByDateCreationDesc().stream()
                .filter(r -> r.getCours() != null)
                .toList();

        Map<Long, List<RetourEvaluation>> parCours = avis.stream()
                .collect(Collectors.groupingBy(r -> r.getCours().getId(), LinkedHashMap::new, Collectors.toList()));

        List<QualiteCoursDto.LigneQualite> lignes = parCours.values().stream()
                .map(groupe -> {
                    var cours = groupe.get(0).getCours();
                    return new QualiteCoursDto.LigneQualite(
                            cours.getId(),
                            cours.getNom(),
                            cours.getTitre(),
                            moyenne(groupe),
                            groupe.size(),
                            (int) groupe.stream().filter(r -> r.getNote() <= 3).count(),
                            groupe.stream()
                                    .map(RetourEvaluation::getCommentaire)
                                    .filter(c -> c != null && !c.isBlank())
                                    .limit(3)
                                    .toList());
                })
                // les cours les plus mal notes en tete : c'est la que l'attention est utile
                .sorted(Comparator.comparing(
                        (QualiteCoursDto.LigneQualite l) -> l.moyenne() == null ? Double.MAX_VALUE : l.moyenne()))
                .toList();

        return ResponseEntity.ok(new QualiteCoursDto(
                moyenne(avis),
                avis.size(),
                lignes.size(),
                (int) lignes.stream().filter(l -> l.moyenne() != null && l.moyenne() <= 3).count(),
                lignes));
    }

    /**
     * Rapport PDF des avis des étudiants sur les cours (TDR §G, besoin B5).
     *
     * <p>Distinct du rapport d'enquêtes de suivi : deux lectures, deux publics.
     * Rien n'est recalculé — mise en page de {@link #suiviQualite()}.
     */
    @GetMapping("/qualite/rapport/avis-cours")
    @Transactional(readOnly = true)
    public ResponseEntity<?> rapportAvisCours() {
        QualiteCoursDto qualiteCours = (QualiteCoursDto) suiviQualite().getBody();
        byte[] pdf = rapportQualitePdfService.genererRapportAvisCours(qualiteCours);
        return pdfEnPieceJointe(pdf, "INPG-avis-cours-" + rapportQualitePdfService.suffixeDuJour() + ".pdf");
    }

    /**
     * Rapport PDF des enquêtes de suivi de carrière des diplômés (TDR §G).
     *
     * <p>Mise en page de {@link #enquetesSuivi()}, sans rien recalculer.
     */
    @GetMapping("/qualite/rapport/enquetes-suivi")
    @Transactional(readOnly = true)
    public ResponseEntity<?> rapportEnquetesSuivi() {
        List<SyntheseEnqueteSuiviDto> enquetes = enqueteSuiviRepository.findAllByOrderByDateCreationDesc().stream()
                .map(this::toSyntheseDto)
                .toList();
        byte[] pdf = rapportQualitePdfService.genererRapportEnquetes(enquetes);
        return pdfEnPieceJointe(pdf,
                "INPG-enquetes-suivi-carriere-" + rapportQualitePdfService.suffixeDuJour() + ".pdf");
    }

    private ResponseEntity<byte[]> pdfEnPieceJointe(byte[] pdf, String nomFichier) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nomFichier).build().toString())
                .body(pdf);
    }

    /** Moyenne arrondie au dixieme, {@code null} si la liste est vide. */
    private Double moyenne(List<RetourEvaluation> avis) {
        return avis.isEmpty() ? null
                : Math.round(avis.stream().mapToInt(RetourEvaluation::getNote).average().orElse(0) * 10) / 10.0;
    }

    // ===================== ENQUETES DE SUIVI =====================

    /**
     * Toutes les campagnes d'enquête de suivi, les plus récentes en tête
     * (TDR §G : « mesurer l'impact à long terme de la formation sur la carrière
     * des diplômés »).
     */
    @GetMapping("/enquetes-suivi")
    @Transactional(readOnly = true)
    public ResponseEntity<?> enquetesSuivi() {
        List<SyntheseEnqueteSuiviDto> dtos = enqueteSuiviRepository.findAllByOrderByDateCreationDesc().stream()
                .map(this::toSyntheseDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Lance une nouvelle campagne et notifie tous les alumni visibles — même
     * canal (cloche in-app) que les mentions de forum, cohérent avec le volume
     * attendu (une poignée de campagnes par an, pas un événement par message).
     */
    @PostMapping("/enquetes-suivi")
    @Transactional
    public ResponseEntity<?> lancerEnqueteSuivi(@RequestBody EnqueteSuiviCreateRequest requete) {
        if (requete.titre() == null || requete.titre().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le titre de l'enquête est obligatoire."));
        }
        EnqueteSuivi enquete = enqueteSuiviRepository.save(
                new EnqueteSuivi(requete.titre().trim(),
                        requete.description() == null || requete.description().isBlank() ? null
                                : requete.description().trim()));

        for (AlumniProfil alumni : alumniProfilRepository.findAll()) {
            User destinataire = alumni.getUser();
            if (destinataire == null) {
                continue;
            }
            notificationRepository.save(new Notification(
                    destinataire,
                    "Une nouvelle enquête de suivi est disponible : « " + enquete.getTitre() + " »",
                    Notification.ENQUETE_SUIVI,
                    "/espace-alumni/enquetes"));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(toSyntheseDto(enquete));
    }

    /** Clôt une campagne : elle n'accepte plus de nouvelles réponses. */
    @PutMapping("/enquetes-suivi/{id}/cloturer")
    @Transactional
    public ResponseEntity<?> cloturerEnqueteSuivi(@PathVariable Long id) {
        EnqueteSuivi enquete = enqueteSuiviRepository.findById(id).orElse(null);
        if (enquete == null) {
            return ResponseEntity.notFound().build();
        }
        enquete.setActive(false);
        enqueteSuiviRepository.save(enquete);
        return ResponseEntity.ok(toSyntheseDto(enquete));
    }

    private SyntheseEnqueteSuiviDto toSyntheseDto(EnqueteSuivi enquete) {
        List<String> commentaires = reponseEnqueteSuiviRepository.findByEnqueteIdOrderByDateReponseDesc(enquete.getId())
                .stream()
                .map(r -> r.getCommentaire())
                .filter(c -> c != null && !c.isBlank())
                .limit(3)
                .toList();
        return new SyntheseEnqueteSuiviDto(
                enquete.getId(),
                enquete.getTitre(),
                enquete.getDescription(),
                enquete.getDateCreation(),
                enquete.isActive(),
                (int) reponseEnqueteSuiviRepository.countByEnqueteId(enquete.getId()),
                // Population alumni visée par une campagne : approchée par l'effectif
                // alumni courant (tous sont notifiés au lancement). Sert au taux de
                // réponse du rapport, pas à un décompte nominatif.
                (int) alumniProfilRepository.count(),
                arrondi(reponseEnqueteSuiviRepository.moyenneApplicationCompetences(enquete.getId())),
                arrondi(reponseEnqueteSuiviRepository.moyenneImpactCarriere(enquete.getId())),
                commentaires);
    }

    /** Moyenne arrondie au dixième, {@code null} si la campagne n'a aucune réponse. */
    private Double arrondi(Double moyenne) {
        return moyenne == null ? null : Math.round(moyenne * 10) / 10.0;
    }

    // ===================== Correspondances =====================

    private ReleveDelivreDto toReleveDto(ReleveNotes releve) {
        Etudiant etudiant = releve.getEtudiant();
        return new ReleveDelivreDto(
                releve.getId(),
                releve.getReference(),
                etudiant == null ? null : etudiant.getId(),
                etudiant == null ? "" : nomComplet(etudiant),
                etudiant == null ? null : etudiant.getMatricule(),
                releve.getPromotion() == null ? null : releve.getPromotion().getLibelle(),
                releve.getPeriode(),
                releve.getDateEmission(),
                releve.getDelivrePar(),
                releve.getMoyenneGenerale(),
                releve.getNombreNotes(),
                releve.getLignes().stream()
                        .map(l -> new ReleveDelivreDto.LigneDto(
                                l.getCoursCode(), l.getCoursTitre(), l.getCoefficient(), l.getMoyenne()))
                        .toList());
    }

    private PromotionDetailDto toPromotionDto(Promotion promotion) {
        Formation formation = promotion.getFormation();
        long nbCours = formation == null ? 0 : coursRepository.findByFormations_Id(formation.getId()).size();
        return new PromotionDetailDto(
                promotion.getId(),
                promotion.getCode(),
                promotion.getLibelle(),
                promotion.getDateDebut(),
                promotion.getDateFin(),
                promotion.estEnCours(),
                promotion.getProgramme(),
                promotion.getActivitesParascolaires(),
                formation == null ? null : formation.getCode(),
                formation == null ? null : formation.getIntitule(),
                formation == null ? null : formation.getDescription(),
                etudiantRepository.countByPromotionId(promotion.getId()),
                nbCours);
    }

    private EtudiantSuiviDto toEtudiantDto(Etudiant etudiant) {
        Promotion promotion = etudiant.getPromotion();
        // La moyenne valait zero pour tout le monde, codee en dur. C'est pourtant sur cet
        // ecran qu'on decide de delivrer un releve ou de relancer un etudiant : la seule
        // information utile a la decision manquait.
        Double moyenne = releveNotesService.moyenneDeLEtudiant(etudiant.getId());
        return new EtudiantSuiviDto(
                etudiant.getId(),
                nomComplet(etudiant),
                promotion == null ? "Sans promotion" : promotion.getLibelle(),
                "Non renseignée",
                0,
                moyenne,
                promotion == null ? "Sans promotion" : releveNotesService.statutDeSuivi(moyenne),
                initiales(etudiant),
                "bg-primary-container",
                etudiant.getMatricule(),
                // Un dossier de candidature derriere le compte = il vient de l'admission.
                candidatRepository.findByUserId(etudiant.getId()).isPresent() ? "Admission" : null,
                alumniProfilRepository.findByUserId(etudiant.getId()).isPresent());
    }

    private EtudiantDetailDto toEtudiantDetailDto(Etudiant etudiant) {
        Promotion promotion = etudiant.getPromotion();
        Double moyenne = releveNotesService.moyenneDeLEtudiant(etudiant.getId());
        return new EtudiantDetailDto(
                etudiant.getId(),
                etudiant.getNom(),
                etudiant.getPrenom(),
                etudiant.getUser() == null ? null : etudiant.getUser().getEmail(),
                etudiant.getTelephone(),
                etudiant.getUser() == null ? null : etudiant.getUser().getUsername(),
                etudiant.getMatricule(),
                promotion == null ? null : promotion.getId(),
                promotion == null ? "Sans promotion" : promotion.getLibelle(),
                moyenne,
                promotion == null ? "Sans promotion" : releveNotesService.statutDeSuivi(moyenne),
                candidatRepository.findByUserId(etudiant.getId()).isPresent() ? "Admission" : null,
                alumniProfilRepository.findByUserId(etudiant.getId()).isPresent());
    }

    private CertificatDto toCertificatDto(Certificat certificat) {
        Etudiant etudiant = certificat.getEtudiant();
        return new CertificatDto(
                certificat.getId(),
                certificat.getReference(),
                etudiant == null ? null : etudiant.getId(),
                etudiant == null ? null : nomComplet(etudiant),
                etudiant == null ? null : etudiant.getMatricule(),
                certificat.getIntituleFormation(),
                certificat.getPromotion() == null ? null : certificat.getPromotion().getLibelle(),
                certificat.getDateEmission() == null ? null : certificat.getDateEmission().format(JOUR),
                certificat.getFormateurNom());
    }

    private String nomComplet(Etudiant etudiant) {
        return ((etudiant.getPrenom() == null ? "" : etudiant.getPrenom()) + " "
                + (etudiant.getNom() == null ? "" : etudiant.getNom())).trim();
    }

    private String initiales(Etudiant etudiant) {
        String p = etudiant.getPrenom();
        String n = etudiant.getNom();
        String resultat = (estVide(p) ? "" : p.substring(0, 1)) + (estVide(n) ? "" : n.substring(0, 1));
        return resultat.isBlank() ? "?" : resultat.toUpperCase();
    }

    private boolean estVide(String valeur) {
        return valeur == null || valeur.trim().isEmpty();
    }
}
