package inpg.sn.lms.espace_etudiant.controller;

import java.util.List;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.entity.Formateur;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.espace_etudiant.dto.CamaradeDto;
import inpg.sn.lms.espace_etudiant.dto.CoursEtudiantDto;
import inpg.sn.lms.espace_etudiant.dto.NoteModuleDto;
import inpg.sn.lms.espace_etudiant.dto.ReleveNotesDto;
import inpg.sn.lms.espace_etudiant.dto.PromotionDetailDto;
import inpg.sn.lms.espace_etudiant.dto.RepriseDto;
import inpg.sn.lms.espace_etudiant.dto.SeanceEtudiantDto;
import inpg.sn.lms.espace_etudiant.dto.SupportEtudiantDto;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.Formation;
import inpg.sn.lms.espace_etudiant.entity.NoteModule;
import inpg.sn.lms.espace_etudiant.entity.ProgressionSupport;
import inpg.sn.lms.espace_etudiant.entity.Promotion;
import inpg.sn.lms.espace_etudiant.entity.Seance;
import inpg.sn.lms.espace_etudiant.entity.SupportCours;
import inpg.sn.lms.espace_etudiant.repository.CoursRepository;
import inpg.sn.lms.espace_etudiant.repository.RetourEvaluationRepository;
import inpg.sn.lms.espace_etudiant.repository.ProgressionSupportRepository;
import inpg.sn.lms.espace_etudiant.repository.SeanceRepository;
import inpg.sn.lms.espace_etudiant.service.HabilitationCoursService;
import inpg.sn.lms.espace_etudiant.service.MatriculeService;
import inpg.sn.lms.espace_etudiant.service.NoteModuleService;
import inpg.sn.lms.espace_etudiant.dto.ReleveDelivreDto;
import inpg.sn.lms.espace_etudiant.entity.ReleveNotes;
import inpg.sn.lms.espace_etudiant.service.ReleveDelivranceService;
import inpg.sn.lms.espace_etudiant.service.ReleveNotesService;
import inpg.sn.lms.espace_etudiant.service.RelevePdfService;
import inpg.sn.lms.espace_etudiant.service.SupportService;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;

/**
 * Scolarité de l'étudiant connecté : sa promotion, ses cours autorisés, leurs
 * supports, sa progression et son calendrier académique.
 *
 * <p>Ce contrôleur matérialise l'exigence centrale du §3.2 du cahier des charges :
 * l'étudiant accède à « <em>les cours autorisés</em> », c'est-à-dire ceux au
 * programme de la formation suivie par sa promotion — et non au catalogue complet.
 * La chaîne d'habilitation est Étudiant → Promotion → Formation → Cours.
 *
 * <p>Il vit sous {@code /api/espace-etudiant/**} afin d'hériter de la règle
 * {@code hasAnyRole("ETUDIANT","FORMATEUR")} déjà posée dans SecurityConfig, et
 * chaque accès à une ressource identifiée est en plus vérifié contre la liste des
 * cours réellement autorisés.
 */
@RestController
@RequestMapping("/api/espace-etudiant/scolarite")
/*
 * Autorisation rendue explicite : elle ne reposait que sur le prefixe d'URL filtre
 * dans SecurityConfig. Un endpoint ajoute hors de ce prefixe aurait ete ouvert sans
 * que rien ne le signale. Les regles d'URL restent en place et s'appliquent en plus.
 */
@PreAuthorize("hasAnyRole('ETUDIANT','FORMATEUR')")
public class ScolariteController {

    private final UtilisateurCourantService utilisateurCourant;
    private final MatriculeService matriculeService;
    private final CoursRepository coursRepository;
    private final SeanceRepository seanceRepository;
    private final SupportService supportService;
    private final ProgressionSupportRepository progressionRepository;
    private final RetourEvaluationRepository retourRepository;
    private final EtudiantRepository etudiantRepository;
    private final HabilitationCoursService habilitation;
    private final NoteModuleService noteModuleService;
    private final ReleveNotesService releveNotesService;
    private final ReleveDelivranceService releveDelivranceService;
    private final RelevePdfService relevePdfService;

    public ScolariteController(UtilisateurCourantService utilisateurCourant, MatriculeService matriculeService,
            CoursRepository coursRepository, SeanceRepository seanceRepository, SupportService supportService,
            ProgressionSupportRepository progressionRepository, RetourEvaluationRepository retourRepository,
            EtudiantRepository etudiantRepository, HabilitationCoursService habilitation,
            NoteModuleService noteModuleService, ReleveNotesService releveNotesService,
            ReleveDelivranceService releveDelivranceService, RelevePdfService relevePdfService) {
        this.habilitation = habilitation;
        this.releveNotesService = releveNotesService;
        this.releveDelivranceService = releveDelivranceService;
        this.relevePdfService = relevePdfService;
        this.utilisateurCourant = utilisateurCourant;
        this.matriculeService = matriculeService;
        this.coursRepository = coursRepository;
        this.seanceRepository = seanceRepository;
        this.supportService = supportService;
        this.progressionRepository = progressionRepository;
        this.retourRepository = retourRepository;
        this.etudiantRepository = etudiantRepository;
        this.noteModuleService = noteModuleService;
    }

    /**
     * Relevé de notes de l'étudiant connecté, groupé par cours.
     *
     * <p>Ces notes existaient depuis l'origine mais n'étaient exposées que par des
     * endpoints réservés au formateur : l'étudiant ne pouvait pas consulter ses propres
     * résultats. Aucun filtre par cours autorisé n'est appliqué — une note appartient à
     * l'étudiant qui l'a obtenue, et la retirer parce que le programme a changé
     * reviendrait à effacer son parcours.
     */
    @GetMapping("/mes-notes")
    @Transactional(readOnly = true)
    public ResponseEntity<?> mesNotes(Authentication authentication) {
        Etudiant etudiant = utilisateurCourant.etudiant(authentication);
        if (etudiant == null) {
            return profilIntrouvable();
        }
        return ResponseEntity.ok(releveNotesService.duEtudiant(etudiant.getId()));
    }

    /**
     * Relevés officiels délivrés à l'étudiant par la direction pédagogique.
     *
     * <p>À distinguer de {@code /mes-notes}, qui montre les notes <em>en cours</em>,
     * recalculées à chaque affichage. Un relevé délivré est un document : ses chiffres
     * sont figés à sa date d'émission et porte une référence, seul moyen d'en vérifier
     * l'authenticité. Les deux peuvent donc différer — c'est normal, et c'est même
     * l'intérêt : sans cela, un document remis en janvier changerait en février.
     */
    @GetMapping("/mes-releves")
    @Transactional(readOnly = true)
    public ResponseEntity<?> mesReleves(Authentication authentication) {
        Etudiant etudiant = utilisateurCourant.etudiant(authentication);
        if (etudiant == null) {
            return profilIntrouvable();
        }
        return ResponseEntity.ok(releveDelivranceService.deLEtudiant(etudiant.getId()).stream()
                .map(this::toReleveDelivreDto)
                .toList());
    }

    /**
     * Version PDF d'un relevé délivré à l'étudiant connecté. Le relevé est
     * cherché dans <em>ses</em> relevés, ce qui vaut contrôle de propriété.
     */
    @GetMapping("/mes-releves/{id}/pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<?> monRelevePdf(@PathVariable Long id, Authentication authentication) {
        Etudiant etudiant = utilisateurCourant.etudiant(authentication);
        if (etudiant == null) {
            return profilIntrouvable();
        }
        ReleveNotes releve = releveDelivranceService.deLEtudiant(etudiant.getId()).stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElse(null);
        if (releve == null) {
            return ResponseEntity.notFound().build();
        }
        return relevePdfService.reponsePdf(releve);
    }

    private ReleveDelivreDto toReleveDelivreDto(ReleveNotes releve) {
        Etudiant etudiant = releve.getEtudiant();
        return new ReleveDelivreDto(
                releve.getId(),
                releve.getReference(),
                etudiant == null ? null : etudiant.getId(),
                etudiant == null ? "" : (etudiant.getPrenom() + " " + etudiant.getNom()).trim(),
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




    /**
     * Derniers supports ouverts, pour reprendre son travail là où il s'est arrêté.
     *
     * <p>Contrairement au relevé de notes, la liste est <strong>filtrée</strong> sur les
     * cours autorisés : une note est un résultat acquis, qu'on ne retire pas parce que
     * le programme a changé, tandis qu'une reprise est un raccourci de navigation — elle
     * doit mener quelque part. Proposer un cours devenu inaccessible n'offrirait qu'un
     * lien vers un refus.
     */
    @GetMapping("/reprendre")
    @Transactional(readOnly = true)
    public ResponseEntity<?> reprendre(Authentication authentication) {
        Etudiant etudiant = utilisateurCourant.etudiant(authentication);
        if (etudiant == null) {
            return profilIntrouvable();
        }
        List<Long> coursAutorises = habilitation.idsCoursAutorises(etudiant);
        List<RepriseDto> reprises = progressionRepository
                .findTop5ByEtudiantIdOrderByDateConsultationDesc(etudiant.getId()).stream()
                .filter(progression -> progression.getSupport() != null
                        && progression.getSupport().getCours() != null
                        && coursAutorises.contains(progression.getSupport().getCours().getId()))
                .map(this::toRepriseDto)
                .toList();
        return ResponseEntity.ok(reprises);
    }

    private RepriseDto toRepriseDto(ProgressionSupport progression) {
        SupportCours support = progression.getSupport();
        Cours cours = support.getCours();
        return new RepriseDto(
                support.getId(),
                support.getTitre(),
                support.getTypeRessource(),
                support.getUrl(),
                cours.getId(),
                cours.getTitre(),
                progression.getDateConsultation());
    }

    /** Fiche de la promotion de l'étudiant, matricule compris. */
    @GetMapping("/ma-promotion")
    @Transactional
    public ResponseEntity<?> maPromotion(Authentication authentication) {
        Etudiant etudiant = utilisateurCourant.etudiant(authentication);
        if (etudiant == null) {
            return profilIntrouvable();
        }
        // Le matricule est attribué à la volée : les comptes créés avant son
        // introduction obtiennent le leur au premier accès à leur scolarité.
        String matricule = matriculeService.attribuerSiAbsent(etudiant);

        Promotion promotion = etudiant.getPromotion();
        if (promotion == null) {
            return ResponseEntity.ok(Map.of(
                    "matricule", matricule,
                    "promotion", Map.of(),
                    "message", "Vous n'êtes rattaché à aucune promotion. Contactez le service de scolarité."));
        }
        return ResponseEntity.ok(Map.of(
                "matricule", matricule,
                "promotion", toPromotionDto(promotion)));
    }

    /**
     * Camarades de la promotion de l'étudiant connecté — nom et matricule
     * seulement, jamais l'email ni le téléphone (principe de minimisation déjà
     * appliqué à la vérification de certificat).
     *
     * <p>La promotion n'est jamais reçue du client : elle est toujours dérivée de
     * {@code Etudiant.getPromotion()} côté serveur, comme {@link #maPromotion} —
     * sans quoi un identifiant glissé à la main révélerait la composition de
     * n'importe quelle promotion.
     */
    @GetMapping("/ma-promotion/etudiants")
    @Transactional(readOnly = true)
    public ResponseEntity<?> camaradesDePromotion(Authentication authentication) {
        Etudiant etudiant = utilisateurCourant.etudiant(authentication);
        if (etudiant == null) {
            return profilIntrouvable();
        }
        Promotion promotion = etudiant.getPromotion();
        if (promotion == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(etudiantRepository.findByPromotionId(promotion.getId()).stream()
                .map(this::toCamaradeDto)
                .toList());
    }

    /** Les cours autorisés de l'étudiant, avec sa progression sur chacun. */
    @GetMapping("/mes-cours")
    @Transactional(readOnly = true)
    public ResponseEntity<?> mesCours(Authentication authentication) {
        Etudiant etudiant = utilisateurCourant.etudiant(authentication);
        if (etudiant == null) {
            return profilIntrouvable();
        }
        List<Cours> cours = coursAutorises(etudiant);
        return ResponseEntity.ok(cours.stream().map(c -> toCoursDto(c, etudiant)).toList());
    }

    /** Détail d'un cours, refusé si le cours n'est pas au programme de l'étudiant. */
    @GetMapping("/mes-cours/{coursId}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> detailCours(@PathVariable Long coursId, Authentication authentication) {
        Etudiant etudiant = utilisateurCourant.etudiant(authentication);
        if (etudiant == null) {
            return profilIntrouvable();
        }
        Cours cours = trouverCoursAutorise(coursId, etudiant);
        if (cours == null) {
            return coursNonAutorise();
        }
        return ResponseEntity.ok(toCoursDto(cours, etudiant));
    }

    /** Supports d'un cours autorisé, avec l'état de consultation de l'étudiant. */
    @GetMapping("/mes-cours/{coursId}/supports")
    @Transactional(readOnly = true)
    public ResponseEntity<?> supportsDuCours(@PathVariable Long coursId, Authentication authentication) {
        Etudiant etudiant = utilisateurCourant.etudiant(authentication);
        if (etudiant == null) {
            return profilIntrouvable();
        }
        if (trouverCoursAutorise(coursId, etudiant) == null) {
            return coursNonAutorise();
        }
        List<Long> consultes = progressionRepository.findSupportIdsConsultes(etudiant.getId());
        List<SupportEtudiantDto> supports = supportService.getCourseSupports(coursId).stream()
                .map(support -> new SupportEtudiantDto(
                        support.getId(),
                        support.getTitre(),
                        support.getTypeRessource(),
                        support.getUrl(),
                        support.getDatePublication(),
                        consultes.contains(support.getId())))
                .toList();
        return ResponseEntity.ok(supports);
    }

    /**
     * Marque un support comme consulté. C'est ce qui alimente le suivi de
     * progression du §3.5. L'opération est idempotente.
     */
    @PostMapping("/supports/{supportId}/consulte")
    @Transactional
    public ResponseEntity<?> marquerConsulte(@PathVariable Long supportId, Authentication authentication) {
        Etudiant etudiant = utilisateurCourant.etudiant(authentication);
        if (etudiant == null) {
            return profilIntrouvable();
        }
        SupportCours support = supportService.findCourseSupportById(supportId).orElse(null);
        if (support == null || support.getCours() == null) {
            return ResponseEntity.notFound().build();
        }
        // Sans ce contrôle, n'importe quel étudiant pourrait gonfler sa progression
        // sur un cours auquel il n'est pas inscrit.
        if (trouverCoursAutorise(support.getCours().getId(), etudiant) == null) {
            return coursNonAutorise();
        }
        if (!progressionRepository.existsByEtudiantIdAndSupportId(etudiant.getId(), supportId)) {
            progressionRepository.save(new ProgressionSupport(etudiant, support));
        }
        long coursId = support.getCours().getId();
        return ResponseEntity.ok(Map.of(
                "consulte", true,
                "progression", progressionDuCours(coursId, etudiant.getId())));
    }

    /** Calendrier académique : les séances des cours autorisés de l'étudiant. */
    @GetMapping("/mon-calendrier")
    @Transactional(readOnly = true)
    public ResponseEntity<?> monCalendrier(Authentication authentication) {
        Etudiant etudiant = utilisateurCourant.etudiant(authentication);
        if (etudiant == null) {
            return profilIntrouvable();
        }
        List<Long> coursIds = coursAutorises(etudiant).stream().map(Cours::getId).toList();
        if (coursIds.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        List<SeanceEtudiantDto> seances = seanceRepository.findByCoursIdInOrderByDateHeureAsc(coursIds).stream()
                .map(this::toSeanceDto)
                .toList();
        return ResponseEntity.ok(seances);
    }

    // ===================== Habilitation =====================

    /**
     * Cours au programme de la formation suivie par la promotion de l'étudiant.
     * Liste vide tant que l'étudiant n'est rattaché à rien : mieux vaut un espace
     * vide qu'un catalogue auquel il n'a pas droit.
     */
    private List<Cours> coursAutorises(Etudiant etudiant) {
        return habilitation.coursAutorises(etudiant);
    }

    private Cours trouverCoursAutorise(Long coursId, Etudiant etudiant) {
        return habilitation.trouverAutorise(coursId, etudiant);
    }

    // ===================== Correspondances =====================

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

    private CoursEtudiantDto toCoursDto(Cours cours, Etudiant etudiant) {
        long nbSupports = supportService.countCourseSupports(cours.getId());
        long nbConsultes = progressionRepository.countByEtudiantIdAndSupport_Cours_Id(etudiant.getId(), cours.getId());
        // Appreciation donnee AU cours par l'etudiant (1 a 5), a ne pas confondre avec
        // ses notes academiques : celles-ci sont servies par /scolarite/mes-notes.
        Integer maNote = retourRepository.findByEtudiantIdAndCoursId(etudiant.getId(), cours.getId())
                .map(avis -> avis.getNote())
                .orElse(null);
        return new CoursEtudiantDto(
                cours.getId(),
                cours.getNom(),
                cours.getTitre(),
                cours.getDescription(),
                cours.getSemestre(),
                cours.getVolumeHoraire(),
                cours.getImageUrl(),
                cours.getFormateurs().stream().map(this::nomFormateur).toList(),
                nbSupports,
                nbConsultes,
                pourcentage(nbConsultes, nbSupports),
                maNote);
    }

    private SeanceEtudiantDto toSeanceDto(Seance seance) {
        Cours cours = seance.getCours();
        return new SeanceEtudiantDto(
                seance.getId(),
                seance.getTitre(),
                seance.getType(),
                seance.getDateHeure(),
                seance.getDureeMinutes(),
                seance.getLieuOuLien(),
                cours == null ? null : cours.getId(),
                cours == null ? null : cours.getTitre());
    }

    private CamaradeDto toCamaradeDto(Etudiant camarade) {
        String nom = ((camarade.getPrenom() == null ? "" : camarade.getPrenom()) + " "
                + (camarade.getNom() == null ? "" : camarade.getNom())).trim();
        return new CamaradeDto(camarade.getId(), nom, camarade.getMatricule());
    }

    private String nomFormateur(Formateur formateur) {
        return formateur.getPrenom() + " " + formateur.getNom();
    }

    private int progressionDuCours(Long coursId, Long etudiantId) {
        long total = supportService.countCourseSupports(coursId);
        long consultes = progressionRepository.countByEtudiantIdAndSupport_Cours_Id(etudiantId, coursId);
        return pourcentage(consultes, total);
    }

    private int pourcentage(long partie, long total) {
        return total == 0 ? 0 : (int) Math.round(100.0 * partie / total);
    }

    // ===================== Réponses d'erreur =====================

    private ResponseEntity<?> profilIntrouvable() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Aucun profil étudiant n'est associé à ce compte."));
    }

    private ResponseEntity<?> coursNonAutorise() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Ce cours ne fait pas partie de votre formation."));
    }
}
