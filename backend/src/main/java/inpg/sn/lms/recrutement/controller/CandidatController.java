package inpg.sn.lms.recrutement.controller;

import inpg.sn.lms.exception.ResourceNotFoundException;
import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.auth.entity.Role;
import inpg.sn.lms.auth.repository.CandidatRepository;
import inpg.sn.lms.auth.repository.RoleRepository;
import inpg.sn.lms.auth.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import inpg.sn.lms.exception.BusinessException;
import inpg.sn.lms.fichier.ValidationFichier;
import inpg.sn.lms.recrutement.dto.CandidatureRequest;
import inpg.sn.lms.recrutement.dto.CandidatureResponse;
import inpg.sn.lms.recrutement.dto.InfosPersonnellesDto;
import inpg.sn.lms.recrutement.dto.PaiementRequest;
import inpg.sn.lms.recrutement.dto.PaiementResponse;
import inpg.sn.lms.recrutement.entity.BrouillonCandidature;
import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.CritereAdmissionPersonnalise;
import inpg.sn.lms.recrutement.entity.FichierStocke;
import inpg.sn.lms.recrutement.entity.PieceJustificative;
import inpg.sn.lms.recrutement.entity.ReponseCritereCandidature;
import inpg.sn.lms.recrutement.entity.SessionCandidature;
import inpg.sn.lms.recrutement.enums.EnumEtapeQms;
import inpg.sn.lms.recrutement.service.CompletudeDossier;
import inpg.sn.lms.recrutement.enums.EnumStatutCandidature;
import inpg.sn.lms.recrutement.enums.EnumStatutVerificationPiece;
import inpg.sn.lms.recrutement.enums.EnumTypeDocument;
import inpg.sn.lms.recrutement.repository.BrouillonCandidatureRepository;
import inpg.sn.lms.recrutement.repository.CandidatureRepository;
import inpg.sn.lms.recrutement.repository.CritereAdmissionPersonnaliseRepository;
import inpg.sn.lms.recrutement.repository.PieceJustificativeRepository;
import inpg.sn.lms.recrutement.repository.EntretienRepository;
import inpg.sn.lms.recrutement.repository.SessionCandidatureRepository;
import inpg.sn.lms.recrutement.repository.TestAcademiqueRepository;
import inpg.sn.lms.recrutement.service.CandidatureService;
import inpg.sn.lms.recrutement.service.ConvocationPdfService;
import inpg.sn.lms.recrutement.service.EntretienService;
import inpg.sn.lms.recrutement.service.FichierStockeService;
import inpg.sn.lms.recrutement.service.LettreAdmissionPdfService;
import inpg.sn.lms.recrutement.service.NotificationCandidatService;
import inpg.sn.lms.recrutement.service.NotificationService;
import inpg.sn.lms.recrutement.service.PaiementService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/recrutement/candidat")
/*
 * Autorisation rendue explicite : elle ne reposait que sur le prefixe d'URL filtre
 * dans SecurityConfig. Un endpoint ajoute hors de ce prefixe aurait ete ouvert sans
 * que rien ne le signale. Les regles d'URL restent en place et s'appliquent en plus.
 */
@PreAuthorize("isAuthenticated()")
public class CandidatController {

    private static final Logger log = LoggerFactory.getLogger(CandidatController.class);

    /**
     * Rôles qui ouvrent déjà un espace de travail dans l'application. Un compte qui
     * en détient un ne peut pas déposer de candidature : {@link #garantirRoleCandidat}
     * vide les rôles avant d'accorder CANDIDAT, ce qui retirerait à ce compte l'accès
     * à ses cours, ses notes et ses certificats — sans qu'il puisse le rétablir seul.
     *
     * <p>Le cas normal reste intact : un compte fraîchement créé n'a aucun rôle
     * (voir UserService#createUser) et passe donc librement.
     */
    private static final Set<String> ROLES_OUVRANT_UN_ESPACE = Set.of(
            "ROLE_ETUDIANT",
            "ROLE_FORMATEUR",
            "ROLE_RESPONSABLE_PEDAGOGIQUE",
            "ROLE_RESPONSABLE_ADMISSION");

    /**
     * États depuis lesquels une nouvelle soumission reste légitime, c'est-à-dire tant
     * que l'administration ne s'est pas prononcée. Le TDR demande explicitement qu'un
     * candidat puisse « revenir plus tard pour poursuivre et finaliser sa candidature » ;
     * il ne demande pas qu'il puisse revenir sur une décision déjà rendue.
     */
    private static final Set<EnumStatutCandidature> STATUTS_ENCORE_MODIFIABLES = Set.of(
            EnumStatutCandidature.BROUILLON,
            EnumStatutCandidature.SOUMIS,
            // « Dossier incomplet » : l'admission a demandé un complément. Sans ce statut ici, le
            // candidat n'avait aucun moyen de déposer la pièce manquante ni de re-soumettre
            // (transition INCOMPLET -> SOUMIS, voir CandidatureService#estTransitionValide).
            EnumStatutCandidature.INCOMPLET);

    /** Repli quand le formulaire ne fournit pas de date : la colonne est NOT NULL en base. */
    private static final LocalDate DATE_NAISSANCE_PAR_DEFAUT = LocalDate.of(2000, 1, 1);

    /**
     * Plafond par pièce justificative, plus strict que la limite multipart globale
     * (10 Mo, {@code spring.servlet.multipart.max-file-size}) : un scan de diplôme ou
     * de pièce d'identité tient largement dans 5 Mo.
     */
    private static final long TAILLE_MAX_PIECE_OCTETS = 5L * 1024 * 1024;

    /**
     * Le formulaire candidat plafonne à 3 années récentes, chacune pouvant compter jusqu'à
     * 3 bulletins (ex: trimestres) : 9 est donc la borne réelle côté serveur, pas un chiffre
     * arbitraire. Jamais un nombre illimité.
     */
    private static final int MAX_RELEVES_PAR_CANDIDATURE = 9;

    /** Longueur maximale de l'adresse postale (colonne {@code candidats.adresse}). */
    private static final int ADRESSE_MAX = 500;

    /** Plafond des parcours académique et professionnel reçus en JSON : large pour un formulaire, borné contre l'abus. */
    private static final int INFORMATIONS_JSON_MAX = 20_000;

    private final CandidatureService candidatureService;
    private final CandidatRepository candidatRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SessionCandidatureRepository sessionRepository;
    private final CandidatureRepository candidatureRepository;
    private final FichierStockeService fichierStockeService;
    private final PieceJustificativeRepository pieceJustificativeRepository;
    private final BrouillonCandidatureRepository brouillonRepository;
    private final NotificationService notificationService;
    private final NotificationCandidatService notificationCandidatService;
    private final PaiementService paiementService;
    private final TestAcademiqueRepository testAcademiqueRepository;
    private final EntretienRepository entretienRepository;
    private final EntretienService entretienService;
    private final ConvocationPdfService convocationPdfService;
    private final LettreAdmissionPdfService lettreAdmissionPdfService;
    private final ObjectMapper objectMapper;
    private final CritereAdmissionPersonnaliseRepository critereRepository;

    @Value("${app.admission.email:admin@inpg.sn}")
    private String emailAdmission;

    public CandidatController(
            CandidatureService candidatureService,
            CandidatRepository candidatRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            SessionCandidatureRepository sessionRepository,
            CandidatureRepository candidatureRepository,
            FichierStockeService fichierStockeService,
            PieceJustificativeRepository pieceJustificativeRepository,
            BrouillonCandidatureRepository brouillonRepository,
            NotificationService notificationService,
            NotificationCandidatService notificationCandidatService,
            PaiementService paiementService,
            TestAcademiqueRepository testAcademiqueRepository,
            EntretienRepository entretienRepository,
            EntretienService entretienService,
            ConvocationPdfService convocationPdfService,
            LettreAdmissionPdfService lettreAdmissionPdfService,
            ObjectMapper objectMapper,
            CritereAdmissionPersonnaliseRepository critereRepository) {
        this.objectMapper = objectMapper;
        this.critereRepository = critereRepository;
        this.testAcademiqueRepository = testAcademiqueRepository;
        this.entretienRepository = entretienRepository;
        this.entretienService = entretienService;
        this.convocationPdfService = convocationPdfService;
        this.lettreAdmissionPdfService = lettreAdmissionPdfService;
        this.candidatureService = candidatureService;
        this.candidatRepository = candidatRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.sessionRepository = sessionRepository;
        this.candidatureRepository = candidatureRepository;
        this.fichierStockeService = fichierStockeService;
        this.pieceJustificativeRepository = pieceJustificativeRepository;
        this.brouillonRepository = brouillonRepository;
        this.notificationService = notificationService;
        this.notificationCandidatService = notificationCandidatService;
        this.paiementService = paiementService;
    }

    /**
     * Accorde ROLE_CANDIDAT à l'utilisateur (idempotent). Un compte ne peut détenir
     * qu'un seul rôle à la fois : les rôles existants sont retirés avant l'ajout.
     */
    private void garantirRoleCandidat(inpg.sn.lms.auth.entity.User user) {
        boolean dejaCandidat = user.getRoles().stream().anyMatch(r -> "ROLE_CANDIDAT".equals(r.getCode()));
        if (dejaCandidat) {
            return;
        }
        Role roleCandidat = roleRepository.findByCode("ROLE_CANDIDAT");
        if (roleCandidat == null) {
            throw new IllegalStateException("Le rôle ROLE_CANDIDAT est introuvable.");
        }
        user.getRoles().clear();
        user.addRole(roleCandidat);
        userRepository.save(user);
    }

    /**
     * Route unique de soumission : crée le Candidat (si besoin)
     * et la Candidature en statut SOUMIS directement.
     * Un visiteur ne peut avoir qu'une seule candidature active.
     */
    /*
     * @Transactional est indispensable ici, pas décoratif : avec open-in-view=false, la
     * branche « redépôt » plus bas travaille sur une Candidature chargée par le dépôt puis
     * détachée, et enregistrerReponsesCriteres() touche sa collection paresseuse
     * reponsesCriteres. Sans session ouverte, cet accès levait une
     * LazyInitializationException remontée en HTTP 500 : tout candidat possédant déjà un
     * dossier actif se voyait refuser un nouveau dépôt. Le défaut ne se voyait pas sur un
     * dossier REJETE, qui emprunte la branche « création » et construit une entité neuve
     * dont la collection est déjà initialisée.
     */
    @PostMapping("/candidature/soumettre")
    @Transactional
    public ResponseEntity<?> soumettreCandidature(
            @RequestBody CandidatureRequest request,
            Authentication authentication) {

        // 1. Récupérer l'utilisateur connecté
        inpg.sn.lms.auth.entity.User user = findUser(authentication);

        // Le contrôle ne visait que le Responsable Admission, alors que garantirRoleCandidat()
        // plus bas vide les rôles de N'IMPORTE QUEL compte. Un étudiant qui postulait à un
        // second programme perdait donc silencieusement l'accès à son espace, sans recours.
        // L'exclusivité des rôles reste la règle (voir AdminController#changeUserRoles) :
        // c'est le périmètre du garde-fou qui était trop étroit.
        Optional<Role> roleDejaAttribue = user.getRoles().stream()
                .filter(r -> ROLES_OUVRANT_UN_ESPACE.contains(r.getCode()))
                .findFirst();
        if (roleDejaAttribue.isPresent()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "message", "Ce compte donne déjà accès à l'espace « " + roleDejaAttribue.get().getLibelle()
                            + " ». Pour déposer une candidature, utilisez un compte distinct."));
        }

        // 2. Un dossier déjà examiné se verrouille.
        //    Ce contrôle précède délibérément toute écriture : placé plus bas, il aurait
        //    laissé le bloc suivant réécrire l'état civil du candidat (nom, date de
        //    naissance, niveau d'étude...) avant de refuser la soumission — soit une
        //    modification de dossier obtenue malgré le refus.
        Optional<Candidat> candidatExistant = candidatRepository.findByUserId(user.getId());
        Optional<Candidature> dossierVerrouille = candidatExistant
                .map(c -> candidatureRepository.findByCandidatId(c.getId()))
                .orElseGet(List::of)
                .stream()
                .filter(c -> c.getStatut() != EnumStatutCandidature.DOSSIER_REJETE
                        && c.getStatut() != EnumStatutCandidature.NON_ADMIS)
                .filter(c -> !STATUTS_ENCORE_MODIFIABLES.contains(c.getStatut()))
                .findFirst();
        if (dossierVerrouille.isPresent()) {
            Candidature verrouille = dossierVerrouille.get();
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "message", "Votre dossier n°" + verrouille.getNumeroDossier()
                            + " est déjà en cours d'examen : il ne peut plus être soumis à nouveau.",
                    "statut", verrouille.getStatut().name()));
        }

        // 2 bis. Tous les contrôles de ce qui a été saisi, avant la moindre écriture : un refus ne doit rien
        //    laisser de modifié (voir l'étape 2).
        InfosPersonnellesDto infos = lireInfosPersonnelles(request.getInformationsPersonnelles());
        String informationsAcademiques = lireInformationsJson(request.getInformationsAcademiques(), "académiques");
        String informationsProfessionnelles =
                lireInformationsJson(request.getInformationsProfessionnelles(), "professionnelles");
        if (infos != null && infos.aAdresse() && infos.adresse().trim().length() > ADRESSE_MAX) {
            throw new BusinessException("L'adresse ne doit pas dépasser " + ADRESSE_MAX + " caractères.");
        }
        if (infos != null && infos.aTelephone()
                && candidatRepository.existsByTelephoneAndIdNot(infos.telephone().trim(), user.getId())) {
            // Le téléphone est unique en base. Sans ce contrôle, la violation n'éclatait qu'en fin de transaction,
            // et le candidat lisait un conflit générique sans savoir ce qu'il devait changer.
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "message", "Ce numéro de téléphone est déjà associé à un autre compte. Utilisez le vôtre, ou "
                            + "connectez-vous avec le compte qui a déjà déposé un dossier ; sinon contactez le "
                            + "service des admissions."));
        }

        // 3. Chercher ou créer le Candidat avec les infos du formulaire
        Candidat candidat;
        if (candidatExistant.isPresent()) {
            candidat = candidatExistant.get();
            appliquerInfosPersonnelles(candidat, infos);
            candidatRepository.save(candidat);
        } else {
            // creerCandidatDepuisUser applique déjà ces informations et enregistre.
            candidat = creerCandidatDepuisUser(user, infos);
        }

        // 4. Vérifier si une candidature existe déjà (non rejetée)
        List<Candidature> existantes = candidatureRepository.findByCandidatId(candidat.getId());
        Optional<Candidature> active = existantes.stream()
                .filter(c -> c.getStatut() != EnumStatutCandidature.DOSSIER_REJETE
                        && c.getStatut() != EnumStatutCandidature.NON_ADMIS)
                .findFirst();

        if (active.isPresent()) {
            Candidature existante = active.get();
            // soumettre() réécrit le statut à SOUMIS sans regarder d'où l'on vient : appelé
            // sur un dossier déjà présélectionné, il le ramenait dans la pile « à traiter »
            // et effaçait la décision du responsable d'admission. Le seul garde-fou est
            // celui de l'étape 2, qui a déjà écarté ces dossiers — ne pas appeler cette
            // méthode depuis un autre chemin sans reproduire ce contrôle.
            enregistrerReponsesCriteres(existante, existante.getSessionCandidature(), request.getReponsesCriteres());
            existante.soumettre(); // passe le statut à SOUMIS + date
            conserverInformationsDeposees(existante, informationsAcademiques, informationsProfessionnelles);
            enregistrerCertification(existante, request);
            candidatureRepository.save(existante);
            garantirRoleCandidat(user);
            brouillonRepository.findByUserId(user.getId()).ifPresent(brouillonRepository::delete);
            candidatureService.journaliser(existante.getId(), EnumEtapeQms.RECEPTION,
                    existante.getStatut(), null, "Dossier redéposé par le candidat.");
            return ResponseEntity.ok(candidatureService.getCandidatureById(existante.getId()));
        }

        // 5. Créer une nouvelle candidature SOUMIS
        SessionCandidature session = sessionRepository.findById(request.getSessionCandidatureId())
                .orElseThrow(() -> new ResourceNotFoundException("Session non trouvée"));

        // Le TDR impose de suspendre le dépôt de candidature une fois le délai dépassé ;
        // l'administrateur garde la main pour rouvrir temporairement la session si besoin.
        if (!session.estSessionActive()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "La session « " + session.getLibelle() + " » n'est plus ouverte aux candidatures."));
        }

        Candidature candidature = new Candidature();
        candidature.setNumeroDossier(genererNumeroDossier());
        candidature.setStatut(EnumStatutCandidature.SOUMIS);
        candidature.setDateSoumission(LocalDateTime.now());
        candidature.setCandidat(candidat);
        candidature.setSessionCandidature(session);
        // Le parcours académique et la situation professionnelle saisis à l'étape 2 : la requête les portait, la
        // colonne existait, la réponse les renvoyait — mais rien ne les copiait entre les deux, et ils étaient
        // perdus à chaque dépôt sans la moindre erreur.
        conserverInformationsDeposees(candidature, informationsAcademiques, informationsProfessionnelles);
        enregistrerCertification(candidature, request);
        enregistrerReponsesCriteres(candidature, session, request.getReponsesCriteres());

        Candidature saved = candidatureRepository.save(candidature);
        garantirRoleCandidat(user);
        brouillonRepository.findByUserId(user.getId()).ifPresent(brouillonRepository::delete);

        // Étape 1 de QMS-009 (« Réception du dossier ») : le journal se remplit ici, à
        // l'arrivée du dossier, et non plus tard depuis son statut — un dossier rejeté
        // puis redéposé passe deux fois par cette étape, ce qu'un état courant ne dit pas.
        candidatureService.journaliser(saved.getId(), EnumEtapeQms.RECEPTION,
                saved.getStatut(), null, "Dossier déposé par le candidat.");

        // Le dossier reste en SOUMIS : le filtre automatique de premier niveau (TDR 3.1.1)
        // est une action d'étape 2 déclenchée par l'admin (cf. AdminRecrutementController
        // #appliquerFiltreCriteres), pas un effet de bord de la soumission elle-même —
        // sinon l'admin n'a jamais l'occasion de voir passer le dossier par l'état SOUMIS.
        String candidatEmail = user.getEmail();
        notificationCandidatService.creerNotification(candidat,
                "Votre dossier n°" + saved.getNumeroDossier() + " a été reçu et est en attente de présélection.",
                "STATUT_CHANGE", "/recrutement/suivi");

        // Notification immédiate à l'INPG (TDR 3.1.1). envoyerEmail() est best-effort :
        // un SMTP indisponible est journalisé sans faire échouer la soumission.
        notificationService.envoyerEmail(emailAdmission,
                "Nouvelle candidature reçue",
                "Une nouvelle candidature a été soumise.\n\n"
                        + "N° de dossier : " + saved.getNumeroDossier() + "\n"
                        + "Candidat : " + candidat.getPrenom() + " " + candidat.getNom() + "\n"
                        + "Email : " + (candidatEmail != null ? candidatEmail : "N/A") + "\n"
                        + "Session : " + session.getLibelle());

        return ResponseEntity.ok(candidatureService.getCandidatureById(saved.getId()));
    }

    /**
     * Valide les critères personnalisés actifs et obligatoires de la session, puis
     * remplace les réponses de la candidature par celles reçues. Un critère retiré
     * ({@code actif=false}) n'est ni exigé, ni écrasé s'il portait déjà une réponse.
     */
    private void enregistrerReponsesCriteres(Candidature candidature, SessionCandidature session,
            Map<Long, String> reponsesRecues) {
        Map<Long, String> reponses = reponsesRecues == null ? Map.of() : reponsesRecues;
        List<CritereAdmissionPersonnalise> criteresActifs = critereRepository
                .findBySessionCandidatureIdAndActifTrueOrderByOrdreAsc(session.getId());

        List<String> manquants = criteresActifs.stream()
                .filter(CritereAdmissionPersonnalise::isObligatoire)
                .filter(c -> {
                    String valeur = reponses.get(c.getId());
                    return valeur == null || valeur.isBlank();
                })
                .map(CritereAdmissionPersonnalise::getLibelle)
                .toList();
        if (!manquants.isEmpty()) {
            throw new BusinessException("Critère(s) requis manquant(s) : " + String.join(", ", manquants));
        }

        candidature.getReponsesCriteres().clear();
        for (CritereAdmissionPersonnalise critere : criteresActifs) {
            String valeur = reponses.get(critere.getId());
            if (valeur == null || valeur.isBlank()) {
                continue;
            }
            ReponseCritereCandidature reponse = new ReponseCritereCandidature();
            reponse.setCandidature(candidature);
            reponse.setCritere(critere);
            reponse.setValeur(valeur.trim());
            candidature.getReponsesCriteres().add(reponse);
        }
    }

    /**
     * Charge le brouillon du formulaire de candidature sauvegardé côté serveur
     * pour l'utilisateur connecté (reprise depuis un autre appareil).
     */
    @GetMapping("/brouillon")
    public ResponseEntity<?> getBrouillon(Authentication authentication) {
        inpg.sn.lms.auth.entity.User user = findUser(authentication);
        return brouillonRepository.findByUserId(user.getId())
                .<ResponseEntity<?>>map(b -> ResponseEntity.ok(Map.of("donneesJson", b.getDonneesJson())))
                .orElseGet(() -> ResponseEntity.ok(Map.of()));
    }

    /**
     * Sauvegarde (ou met à jour) le brouillon du formulaire de candidature côté
     * serveur. Le contenu n'est pas interprété : c'est le JSON tel qu'assemblé
     * par le frontend.
     */
    @PutMapping("/brouillon")
    public ResponseEntity<?> sauvegarderBrouillon(@RequestBody Map<String, String> body,
            Authentication authentication) {
        inpg.sn.lms.auth.entity.User user = findUser(authentication);
        // Le formulaire sauvegarde ses brouillons avec un délai d'une seconde : le dernier peut donc partir APRÈS le
        // dépôt, qui venait de supprimer le brouillon, et en recréer un avec toutes les données saisies (constaté :
        // 0,6 s après la soumission). Une fois un dossier déposé, il n'y a plus rien à reprendre.
        if (aUnDossierDepose(user)) {
            return ResponseEntity.ok(Map.of(
                    "message", "Dossier déjà déposé : le brouillon n'est plus conservé.", "ignore", true));
        }
        String donneesJson = body.getOrDefault("donneesJson", "");
        BrouillonCandidature brouillon = brouillonRepository.findByUserId(user.getId())
                .orElseGet(() -> new BrouillonCandidature(user, donneesJson));
        brouillon.setDonneesJson(donneesJson);
        brouillon.setDateMaj(LocalDateTime.now());
        brouillonRepository.save(brouillon);
        return ResponseEntity.ok(Map.of("message", "Brouillon sauvegardé."));
    }

    @GetMapping("/candidatures")
    public ResponseEntity<List<CandidatureResponse>> getMesCandidatures(Authentication authentication) {
        inpg.sn.lms.auth.entity.User user = findUser(authentication);
        Optional<Candidat> candidatOpt = candidatRepository.findByUserId(user.getId());
        if (candidatOpt.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(candidatureService.getCandidaturesByCandidatId(candidatOpt.get().getId()));
    }

    @GetMapping("/candidature/{id}")
    public ResponseEntity<CandidatureResponse> getCandidature(
            @PathVariable Long id, Authentication authentication) {
        inpg.sn.lms.auth.entity.User user = findUser(authentication);
        Optional<Candidat> candidatOpt = candidatRepository.findByUserId(user.getId());
        if (candidatOpt.isEmpty()) return ResponseEntity.status(403).build();
        CandidatureResponse resp = candidatureService.getCandidatureById(id);
        if (!resp.getCandidatId().equals(candidatOpt.get().getId())) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(resp);
    }

    /**
     * Lettre d'admission PDF. LettreAdmissionPdfService#genererLettreAdmission existait déjà
     * complet mais n'était appelé nulle part : le bouton "Télécharger la lettre" de l'espace
     * candidat n'avait aucune action câblée.
     */
    @GetMapping("/candidature/{id}/lettre-admission")
    public ResponseEntity<byte[]> telechargerLettreAdmission(@PathVariable Long id, Authentication authentication) {
        inpg.sn.lms.auth.entity.User user = findUser(authentication);
        Candidat candidat = candidatRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidat non trouvé"));

        Candidature candidature = candidatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature non trouvée"));
        if (!candidature.getCandidat().getId().equals(candidat.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (candidature.getStatut() != EnumStatutCandidature.ADMIS
                && candidature.getStatut() != EnumStatutCandidature.INSCRIPTION_FINALISEE) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(("Lettre disponible uniquement pour une candidature admise.").getBytes(StandardCharsets.UTF_8));
        }

        byte[] pdf = lettreAdmissionPdfService.genererLettreAdmission(candidature);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename("lettre-admission.pdf", StandardCharsets.UTF_8).build());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .headers(headers)
                .body(pdf);
    }

    /**
     * Téléverser une pièce justificative pour une candidature
     */
    @PostMapping("/candidature/{candidatureId}/pieces")
    @Transactional
    public ResponseEntity<?> televerserPiece(
            @PathVariable Long candidatureId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("typeDocument") String typeDocumentStr,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "moyenneDeclaree", required = false) Double moyenneDeclaree,
            Authentication authentication) {

        // Vérifier que le candidat est bien le propriétaire
        inpg.sn.lms.auth.entity.User user = findUser(authentication);
        Candidat candidat = candidatRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidat non trouvé"));

        Candidature candidature = candidatureRepository.findById(candidatureId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature non trouvée"));

        if (!candidature.getCandidat().getId().equals(candidat.getId())) {
            return ResponseEntity.status(403).body(java.util.Map.of("message", "Accès non autorisé"));
        }

        // Une pièce est figée dès que le dossier a atteint une décision de sélection.
        // Un éventuel complément suit alors une réouverture tracée par le service admission,
        // plutôt que de modifier silencieusement le dossier examiné.
        //
        // La condition était inversée depuis la refonte du workflow (22/09) : elle n'autorisait
        // l'ajout qu'aux statuts PRESELECTIONNE et PAIEMENT_EN_ATTENTE, soit APRÈS la décision, et
        // refusait le dossier tout juste soumis ou à compléter. Elle réutilise désormais l'ensemble
        // qui décide aussi si le dossier peut encore être re-soumis : une seule définition de
        // « encore modifiable ».
        if (!STATUTS_ENCORE_MODIFIABLES.contains(candidature.getStatut())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(java.util.Map.of(
                    "message", "Vous ne pouvez plus ajouter de document à cette étape du dossier. "
                            + "Contactez le service admission si un complément est nécessaire."));
        }

        // Valider le type de document
        EnumTypeDocument typeDocument;
        try {
            typeDocument = EnumTypeDocument.valueOf(typeDocumentStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("message", "Type de document invalide: " + typeDocumentStr));
        }

        // Un diplôme/certificat s'ajoute en nombre libre : rien d'autre que cette
        // description ne permet ensuite de savoir à quoi il correspond.
        if (typeDocument == EnumTypeDocument.DIPLOME && (description == null || description.isBlank())) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("message", "Une description est requise pour un diplôme ou certificat."));
        }
        if (description != null && description.length() > 500) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("message", "La description ne doit pas dépasser 500 caractères."));
        }

        // Seule la taille est contrôlée ici : c'est la seule règle que
        // FichierStockeService ne porte pas. Le fichier vide et le format étaient
        // vérifiés en double, avec une liste plus étroite (sans WEBP) et fondée sur le
        // seul type MIME — que le client déclare et falsifie trivialement.
        // ValidationFichier, appelée juste en dessous, vérifie d'abord l'extension,
        // qui est ce qui décide réellement du Content-Type de réponse.
        if (file.getSize() > TAILLE_MAX_PIECE_OCTETS) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("message", "Le fichier dépasse la taille maximale autorisée (5 Mo)."));
        }

        // Un même document renvoyé tel quel n'est enregistré qu'une fois. Le formulaire relance lui-même un envoi
        // qui a échoué sans réponse (coupure, délai dépassé) : si le premier essai avait en réalité abouti, le
        // second créerait sinon une pièce en double. Placé avant le plafond des relevés : le renvoi du neuvième
        // relevé, déjà reçu, doit être reconnu et non refusé comme un dixième.
        Optional<PieceJustificative> dejaRecue = pieceJustificativeRepository
                .findByCandidatureIdAndTypeDocument(candidatureId, typeDocument).stream()
                .filter(existante -> estLeMemeDepot(existante, typeDocument, file, description, moyenneDeclaree))
                .findFirst();
        if (dejaRecue.isPresent()) {
            return ResponseEntity.ok(java.util.Map.of(
                    "message", "Pièce déjà reçue.",
                    "fichierId", dejaRecue.get().getFichierStocke().getId().toString(),
                    "doublon", "true"));
        }

        // Un releve de notes n'est plus accepte sans limite : au plus 9 (jusqu'a trois annees recentes de trois
        // bulletins), verifie cote serveur pour ne pas dependre du seul formulaire candidat.
        if (typeDocument == EnumTypeDocument.RELEVE_NOTES) {
            long dejaDeposes = pieceJustificativeRepository.countByCandidatureIdAndTypeDocument(
                    candidatureId, EnumTypeDocument.RELEVE_NOTES);
            if (dejaDeposes >= MAX_RELEVES_PAR_CANDIDATURE) {
                return ResponseEntity.badRequest().body(java.util.Map.of(
                        "message", "Au plus " + MAX_RELEVES_PAR_CANDIDATURE + " relevés de notes peuvent être déposés."));
            }
            if (moyenneDeclaree != null && (moyenneDeclaree < 0 || moyenneDeclaree > 20)) {
                return ResponseEntity.badRequest()
                        .body(java.util.Map.of("message", "La moyenne déclarée doit être comprise entre 0 et 20."));
            }
        }

        // Enregistrer le fichier (extension, type MIME et chemin de destination validés)
        FichierStocke fichier = fichierStockeService.enregistrerFichier(file);

        // Créer la pièce justificative
        PieceJustificative piece = new PieceJustificative();
        piece.setTypeDocument(typeDocument);
        piece.setEstConforme(false);
        piece.setStatutVerification(EnumStatutVerificationPiece.A_VERIFIER);
        piece.setDescription(description != null && !description.isBlank() ? description.trim() : null);
        piece.setFichierStocke(fichier);
        piece.setCandidature(candidature);
        piece.setMoyenneDeclaree(typeDocument == EnumTypeDocument.RELEVE_NOTES ? moyenneDeclaree : null);
        pieceJustificativeRepository.save(piece);
        candidature.getPiecesJustificatives().add(piece);

        // Dossier mis en attente par « demander un complément » : dès que la dernière pièce arrive, il retourne à
        // l'examen sans que le candidat ait autre chose à faire (la page de suivi le lui promet : « votre dossier sera
        // de nouveau examiné »). Rien ne le faisait revenir, et le formulaire ne permettait pas de le re-soumettre.
        if (candidature.getStatut() == EnumStatutCandidature.INCOMPLET
                && CompletudeDossier.manques(candidature).isEmpty()) {
            candidature.setStatut(EnumStatutCandidature.SOUMIS);
            candidature.setStatutPrecedent(null);
            candidatureRepository.save(candidature);
            candidatureService.journaliser(candidature.getId(), EnumEtapeQms.RECEPTION, EnumStatutCandidature.SOUMIS,
                    user, "Complément reçu : le dossier est complet et revient à l'examen.");
        }

        return ResponseEntity.ok(java.util.Map.of(
                "message", "Pièce téléversée avec succès.",
                "fichierId", fichier.getId().toString()));
    }

    /**
     * Initie le paiement des frais de dossier pour une candidature. Aucune passerelle
     * réelle (PayDunya/Touch/Kalpay) n'est connectée : ce endpoint ne fait que ce que fait
     * un candidat, à savoir déclencher la tentative — il rend EN_ATTENTE, jamais SUCCES.
     *
     * <p>La confirmation était auparavant enchaînée ici-même, juste après l'ouverture de la
     * tentative : côté candidat, un paiement se soldait donc toujours par un succès immédiat,
     * sans jamais passer par une étape distincte de validation. Cela court-circuitait
     * {@code AdminRecrutementController#simulerSuccesPaiement}, qui existe justement pour
     * jouer ce rôle (TDR étape 3 : "l'INPG valide le statut Payé sur le profil du candidat") —
     * un webhook d'agrégateur réel appellerait demain {@link PaiementService#confirmerPaiement}
     * exactement à cet endroit-là, pas depuis la requête du candidat.
     */
    @PostMapping("/paiement")
    public ResponseEntity<?> initierPaiement(@RequestBody PaiementRequest request, Authentication authentication) {
        inpg.sn.lms.auth.entity.User user = findUser(authentication);
        Candidat candidat = candidatRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidat non trouvé"));

        Candidature candidature = candidatureRepository.findById(request.getCandidatureId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidature non trouvée"));

        if (!candidature.getCandidat().getId().equals(candidat.getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "Accès non autorisé"));
        }

        PaiementResponse paiement = paiementService.initierPaiement(request);
        return ResponseEntity.ok(paiement);
    }

    /**
     * Reçu PDF des frais de dossier. Le contrôle de propriété est délégué au service :
     * un candidat ne peut télécharger que le reçu de son propre paiement.
     */
    @GetMapping("/paiement/{id}/recu")
    public ResponseEntity<byte[]> telechargerRecu(@PathVariable Long id, Authentication authentication) {
        inpg.sn.lms.auth.entity.User user = findUser(authentication);
        Candidat candidat = candidatRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidat non trouvé"));

        PaiementService.Recu recu = paiementService.genererRecu(id, candidat.getId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(recu.nomFichier(), StandardCharsets.UTF_8).build());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .headers(headers)
                .body(recu.contenu());
    }

    /**
     * Confirme la présence du candidat à son entretien. EntretienService#confirmerPresence
     * et le champ Entretien#confirmeParCandidat existaient déjà, prêts à l'emploi, mais
     * aucune route n'y menait — le bouton "Confirmer ma présence" de l'espace candidat
     * n'avait donc aucun effet réel (TDR étape 5).
     */
    @PutMapping("/entretien/{id}/confirmer-presence")
    public ResponseEntity<?> confirmerPresenceEntretien(@PathVariable Long id, Authentication authentication) {
        inpg.sn.lms.auth.entity.User user = findUser(authentication);
        Candidat candidat = candidatRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidat non trouvé"));
        return ResponseEntity.ok(entretienService.confirmerPresence(id, candidat.getId()));
    }

    /** Même constat que ci-dessus pour EntretienService#demanderReport. */
    @PutMapping("/entretien/{id}/demander-report")
    public ResponseEntity<?> demanderReportEntretien(@PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body, Authentication authentication) {
        inpg.sn.lms.auth.entity.User user = findUser(authentication);
        Candidat candidat = candidatRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidat non trouvé"));
        String motif = body != null ? body.get("motif") : null;
        return ResponseEntity.ok(entretienService.demanderReport(id, candidat.getId(), motif));
    }

    /**
     * Convocation PDF à l'entretien. ConvocationPdfService#genererConvocation existait déjà
     * complet (en-tête, détails, checklist, signature) mais n'était appelé nulle part : le
     * bouton "Télécharger Convocation" affichait un message "bientôt disponible" alors que
     * le PDF pouvait déjà être produit.
     */
    @GetMapping("/entretien/{id}/convocation")
    public ResponseEntity<byte[]> telechargerConvocationEntretien(@PathVariable Long id, Authentication authentication) {
        inpg.sn.lms.auth.entity.User user = findUser(authentication);
        Candidat candidat = candidatRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidat non trouvé"));

        var entretien = entretienService.getEntretienEntity(id);
        if (!entretien.getCandidat().getId().equals(candidat.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Candidature candidature = candidatureRepository.findByCandidatId(candidat.getId()).stream()
                .filter(c -> c.getStatut() != EnumStatutCandidature.DOSSIER_REJETE
                        && c.getStatut() != EnumStatutCandidature.NON_ADMIS)
                .findFirst()
                .orElse(null);

        byte[] pdf = convocationPdfService.genererConvocation(entretien, candidature);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename("convocation-entretien.pdf", StandardCharsets.UTF_8).build());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .headers(headers)
                .body(pdf);
    }

    /**
     * Suivi par numéro de dossier.
     *
     * <p>Le numéro de dossier circule dans les courriels, sur les reçus et dans les
     * exports : ce n'est pas un secret. Sans contrôle de propriété, tout compte
     * connecté pouvait donc lire l'état civil, la date de naissance et le dossier
     * de paiement d'un autre candidat. La réponse est désormais réservée au
     * titulaire du dossier.
     */
    @GetMapping("/suivi/{numeroDossier}")
    public ResponseEntity<?> suivreAvancement(@PathVariable String numeroDossier, Authentication auth) {
        CandidatureResponse reponse = candidatureService.getCandidatureByNumeroDossier(numeroDossier);
        Optional<Candidat> candidat = candidatRepository.findByUserId(findUser(auth).getId());
        if (candidat.isEmpty() || !reponse.getCandidatId().equals(candidat.get().getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "Accès non autorisé"));
        }
        return ResponseEntity.ok(reponse);
    }

    /**
     * Parcours d'évaluation du candidat : épreuves passées et entretiens planifiés.
     *
     * <p>Alimente la page de suivi, qui affichait jusqu'ici des notes, un jury et un
     * rendez-vous d'entretien écrits en dur dans le composant Angular. Les
     * appréciations du jury ne sont volontairement pas exposées : elles relèvent de
     * la délibération interne.
     */
    @GetMapping("/candidature/{id}/parcours")
    public ResponseEntity<?> parcours(@PathVariable Long id, Authentication auth) {
        CandidatureResponse candidature = candidatureService.getCandidatureById(id);
        Optional<Candidat> candidatOpt = candidatRepository.findByUserId(findUser(auth).getId());
        if (candidatOpt.isEmpty() || !candidature.getCandidatId().equals(candidatOpt.get().getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "Accès non autorisé"));
        }
        Long candidatId = candidatOpt.get().getId();

        List<Map<String, Object>> tests = testAcademiqueRepository.findByCandidatId(candidatId).stream()
                .map(test -> {
                    Map<String, Object> ligne = new LinkedHashMap<>();
                    ligne.put("nomEpreuve", test.getNomEpreuve());
                    ligne.put("coefficient", test.getCoefficient());
                    ligne.put("noteObtenue", test.getNoteObtenue());
                    ligne.put("dateEpreuve", test.getDateEpreuve());
                    // Pas de champ "lieu" sur TestAcademique (ni entite ni colonne) : retire
                    // plutot que d'inventer une colonne sans decision produit sur son usage.
                    // Voir LOG_BLOQUANT.md (session du 2026-08-24).
                    return ligne;
                })
                .toList();

        List<Map<String, Object>> entretiens = entretienRepository.findByCandidatId(candidatId).stream()
                .map(entretien -> {
                    Map<String, Object> ligne = new LinkedHashMap<>();
                    ligne.put("id", entretien.getId());
                    ligne.put("dateHeure", entretien.getDateHeure());
                    ligne.put("lieuOuLien", entretien.getLieuOuLien());
                    ligne.put("confirmeParCandidat", entretien.isConfirmeParCandidat());
                    ligne.put("motifReport", entretien.getMotifReport());
                    return ligne;
                })
                .toList();

        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("tests", tests);
        corps.put("entretiens", entretiens);
        // Seuil réellement défini sur la session, plutôt qu'une valeur d'affichage.
        corps.put("seuilReussiteTest", seuilDeLaSession(candidature));
        return ResponseEntity.ok(corps);
    }

    private Float seuilDeLaSession(CandidatureResponse candidature) {
        if (candidature.getSessionCandidatureId() == null) {
            return null;
        }
        return sessionRepository.findById(candidature.getSessionCandidatureId())
                .map(SessionCandidature::getSeuilReussiteTest)
                .orElse(null);
    }

    // --- Méthodes privées ---

    private Candidat creerCandidatDepuisUser(inpg.sn.lms.auth.entity.User user, InfosPersonnellesDto infos) {
        String username = user.getUsername();
        // Supprimer le suffixe numérique (ex: "ibrahima_pouye1" → "ibrahima_pouye")
        String cleanUsername = username != null ? username.replaceAll("\\d+$", "") : "";
        String prenom = cleanUsername;
        String nom = "";
        if (cleanUsername.contains("_")) {
            String[] parts = cleanUsername.split("_", 2);
            prenom = parts[0];
            nom = parts.length > 1 ? parts[1] : "";
        }

        Candidat c = new Candidat();
        c.setUser(user);
        // Valeurs de repli, remplacées ensuite par ce que le formulaire a réellement fourni.
        c.setNom(nom.isEmpty() ? prenom : nom);
        c.setPrenom(prenom);
        c.setTelephone("N/A");
        c.setNationalite("Sénégalaise");
        c.setDateNaissance(DATE_NAISSANCE_PAR_DEFAUT);
        c.setNiveauEtude("Non spécifié");

        appliquerInfosPersonnelles(c, infos);
        return candidatRepository.save(c);
    }

    /**
     * Reporte sur le candidat les champs réellement renseignés du formulaire. Un champ
     * absent laisse la valeur existante en place — c'est ce qui permet de compléter un
     * dossier en plusieurs fois sans effacer ce qui avait déjà été saisi.
     */
    private void appliquerInfosPersonnelles(Candidat candidat, InfosPersonnellesDto infos) {
        if (infos == null) {
            return;
        }
        if (infos.aNom()) candidat.setNom(infos.nom());
        if (infos.aPrenom()) candidat.setPrenom(infos.prenom());
        if (infos.aTelephone()) candidat.setTelephone(infos.telephone());
        if (infos.aNationalite()) candidat.setNationalite(infos.nationalite());
        if (infos.dateNaissance() != null) candidat.setDateNaissance(infos.dateNaissance());
        if (infos.aNiveauEtude()) candidat.setNiveauEtude(infos.niveauEtude());
        if (infos.aGenre()) candidat.setGenre(infos.genre());
        if (infos.aProfession()) candidat.setProfession(infos.profession());
        if (infos.anneesExperience() != null) candidat.setAnneesExperience(infos.anneesExperience());
        if (infos.aAdresse()) candidat.setAdresse(infos.adresse().trim());
    }

    /**
     * Le candidat a-t-il un dossier déposé, c'est-à-dire ni brouillon ni refusé ? Même notion de « dossier actif »
     * que le verrou de {@link #soumettreCandidature} : un dossier rejeté (ou non admis) laisse déposer à nouveau.
     */
    private boolean aUnDossierDepose(inpg.sn.lms.auth.entity.User user) {
        return candidatRepository.findByUserId(user.getId())
                .map(c -> candidatureRepository.findByCandidatId(c.getId()))
                .orElseGet(List::of)
                .stream()
                .anyMatch(c -> c.getStatut() != EnumStatutCandidature.BROUILLON
                        && c.getStatut() != EnumStatutCandidature.DOSSIER_REJETE
                        && c.getStatut() != EnumStatutCandidature.NON_ADMIS);
    }

    /**
     * Cette pièce déjà enregistrée est-elle exactement ce que le candidat envoie à nouveau ? Même nom, même taille,
     * même description, même moyenne déclarée, et surtout même contenu octet pour octet : deux relevés différents
     * qui porteraient le même nom restent deux pièces. Un fichier disparu du disque n'est jamais « le même » : le
     * renvoi le rétablit.
     */
    private boolean estLeMemeDepot(PieceJustificative existante, EnumTypeDocument type, MultipartFile file,
            String description, Double moyenneDeclaree) {
        FichierStocke stocke = existante.getFichierStocke();
        if (stocke == null || stocke.getTailleOctets() == null || stocke.getTailleOctets() != file.getSize()) {
            return false;
        }
        if (!Objects.equals(stocke.getNomOriginal(), ValidationFichier.nomSur(file, "document"))) {
            return false;
        }
        String descriptionRecue = description != null && !description.isBlank() ? description.trim() : null;
        if (!Objects.equals(existante.getDescription(), descriptionRecue)) {
            return false;
        }
        Double moyenneRecue = type == EnumTypeDocument.RELEVE_NOTES ? moyenneDeclaree : null;
        if (!Objects.equals(existante.getMoyenneDeclaree(), moyenneRecue)) {
            return false;
        }
        return contenuIdentique(file, stocke);
    }

    private boolean contenuIdentique(MultipartFile file, FichierStocke stocke) {
        Path chemin = Paths.get(stocke.getCheminStockage());
        if (!Files.isRegularFile(chemin)) {
            return false;
        }
        try (InputStream recu = file.getInputStream(); InputStream enregistre = Files.newInputStream(chemin)) {
            // Au plus 5 Mo de chaque côté (plafond par pièce), et la taille est déjà égale.
            return Arrays.equals(recu.readAllBytes(), enregistre.readAllBytes());
        } catch (IOException e) {
            log.warn("Comparaison impossible avec la pièce déjà enregistrée ({}) : {}", chemin, e.getMessage());
            return false;
        }
    }

    /**
     * Un parcours académique ou professionnel reçu en JSON : objet JSON valide et de taille raisonnable, conservé tel
     * quel (le formulaire en fait l'unique juge de sa structure). Absent ou vide : {@code null}, c'est-à-dire « ne
     * rien changer » — un client plus ancien, ou une re-soumission partielle, ne doit pas effacer ce qui existe.
     */
    private String lireInformationsJson(String json, String rubrique) {
        if (json == null || json.isBlank()) {
            return null;
        }
        if (json.length() > INFORMATIONS_JSON_MAX) {
            throw new BusinessException("Les informations " + rubrique + " transmises sont trop volumineuses.");
        }
        try {
            JsonNode noeud = objectMapper.readTree(json);
            if (noeud == null || !noeud.isObject()) {
                throw new BusinessException(
                        "Les informations " + rubrique + " transmises n'ont pas pu être lues. Vérifiez le formulaire.");
            }
            return json;
        } catch (JsonProcessingException e) {
            log.warn("Informations {} illisibles : {}", rubrique, e.getOriginalMessage());
            throw new BusinessException(
                    "Les informations " + rubrique + " transmises n'ont pas pu être lues. Vérifiez le formulaire.");
        }
    }

    private void conserverInformationsDeposees(Candidature candidature, String academiques, String professionnelles) {
        if (academiques != null) {
            candidature.setInformationsAcademiques(academiques);
        }
        if (professionnelles != null) {
            candidature.setInformationsProfessionnelles(professionnelles);
        }
    }

    /** Date la certification sur l'honneur quand le candidat l'a cochée ; sans elle, rien n'est modifié. */
    private void enregistrerCertification(Candidature candidature, CandidatureRequest request) {
        if (Boolean.TRUE.equals(request.getConsentement())) {
            candidature.setConsentementLe(LocalDateTime.now());
        }
    }

    /**
     * Désérialise l'état civil transmis par le formulaire.
     *
     * <p>Le découpage manuel de la chaîne qui tenait ce rôle ne reconnaissait que les
     * valeurs entre guillemets : un {@code "anneesExperience": 5} était silencieusement
     * perdu, alors que ce champ sert au filtre automatique de présélection. Un JSON
     * illisible est désormais signalé au candidat plutôt qu'ignoré.
     */
    private InfosPersonnellesDto lireInfosPersonnelles(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, InfosPersonnellesDto.class);
        } catch (JsonProcessingException e) {
            log.warn("Informations personnelles illisibles : {}", e.getOriginalMessage());
            throw new BusinessException(
                    "Les informations personnelles transmises n'ont pas pu être lues. Vérifiez le formulaire.");
        }
    }

    private inpg.sn.lms.auth.entity.User findUser(Authentication auth) {
        String name = auth.getName();
        inpg.sn.lms.auth.entity.User u = userRepository.findByUsername(name);
        if (u == null) u = userRepository.findByEmail(name);
        if (u == null) throw new ResourceNotFoundException("Utilisateur non trouvé: " + name);
        return u;
    }

    /**
     * Numéro de dossier, unique en base (contrainte {@code unique = true}). Un UUID
     * tronqué à 8 caractères a une probabilité de collision non nulle : la génération
     * est retentée tant que le numéro existe déjà plutôt que de laisser l'INSERT
     * échouer en pleine soumission de candidature.
     */
    private String genererNumeroDossier() {
        for (int tentative = 0; tentative < 10; tentative++) {
            String candidat = "INPG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            if (!candidatureRepository.existsByNumeroDossier(candidat)) {
                return candidat;
            }
        }
        throw new IllegalStateException("Impossible de générer un numéro de dossier unique.");
    }
}
