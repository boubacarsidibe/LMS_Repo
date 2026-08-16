package inpg.sn.lms.espace_etudiant.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.espace_etudiant.dto.AnnonceCreateRequest;
import inpg.sn.lms.espace_etudiant.dto.AnnonceDto;
import inpg.sn.lms.espace_etudiant.entity.Annonce;
import inpg.sn.lms.espace_etudiant.entity.AnnonceLecture;
import inpg.sn.lms.espace_etudiant.entity.Promotion;
import inpg.sn.lms.espace_etudiant.enums.EnumTypeAnnonce;
import inpg.sn.lms.espace_etudiant.repository.AnnonceLectureRepository;
import inpg.sn.lms.espace_etudiant.repository.AnnonceRepository;
import inpg.sn.lms.espace_etudiant.repository.PromotionRepository;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;

/**
 * Annonces, messages et mémos en provenance de l'INPG (TDR §3.2).
 *
 * <p>Le cahier des charges distingue explicitement ces trois natures ; elles sont
 * portées par {@link EnumTypeAnnonce} et non par un simple libellé libre.
 *
 * <p>La lecture est ouverte aux étudiants et aux formateurs (préfixe
 * {@code /api/espace-etudiant/**}), tandis que la publication passe par le préfixe
 * {@code /api/espace-etudiant/formateur/**}, déjà réservé au rôle FORMATEUR dans
 * SecurityConfig. L'accusé de lecture n'a de sens que pour un étudiant : un
 * formateur consulte le fil sans l'altérer.
 */
@RestController
@RequestMapping("/api/espace-etudiant")
/*
 * Autorisation rendue explicite : elle ne reposait que sur le prefixe d'URL filtre
 * dans SecurityConfig. Un endpoint ajoute hors de ce prefixe aurait ete ouvert sans
 * que rien ne le signale. Les regles d'URL restent en place et s'appliquent en plus.
 */
@PreAuthorize("hasAnyRole('ETUDIANT','FORMATEUR')")
public class AnnonceController {

    private final AnnonceRepository annonceRepository;
    private final AnnonceLectureRepository lectureRepository;
    private final PromotionRepository promotionRepository;
    private final UtilisateurCourantService utilisateurCourant;

    public AnnonceController(AnnonceRepository annonceRepository, AnnonceLectureRepository lectureRepository,
            PromotionRepository promotionRepository, UtilisateurCourantService utilisateurCourant) {
        this.annonceRepository = annonceRepository;
        this.lectureRepository = lectureRepository;
        this.promotionRepository = promotionRepository;
        this.utilisateurCourant = utilisateurCourant;
    }

    /** Fil des communications visibles par le connecté, épinglées en tête. */
    @GetMapping("/annonces")
    @Transactional(readOnly = true)
    public ResponseEntity<?> mesAnnonces(Authentication authentication) {
        Etudiant etudiant = utilisateurCourant.etudiant(authentication);
        List<Annonce> annonces = annoncesVisibles(etudiant);
        List<Long> lues = etudiant == null ? List.of() : lectureRepository.findAnnonceIdsLues(etudiant.getId());
        return ResponseEntity.ok(annonces.stream()
                .map(annonce -> toDto(annonce, lues.contains(annonce.getId())))
                .toList());
    }

    /** Compteur pour la pastille de notification. */
    @GetMapping("/annonces/count-non-lues")
    @Transactional(readOnly = true)
    public ResponseEntity<?> compterNonLues(Authentication authentication) {
        Etudiant etudiant = utilisateurCourant.etudiant(authentication);
        if (etudiant == null) {
            return ResponseEntity.ok(Map.of("count", 0));
        }
        List<Long> lues = lectureRepository.findAnnonceIdsLues(etudiant.getId());
        long nonLues = annoncesVisibles(etudiant).stream()
                .filter(annonce -> !lues.contains(annonce.getId()))
                .count();
        return ResponseEntity.ok(Map.of("count", nonLues));
    }

    /** Accuse réception d'une annonce. Idempotent. */
    @PostMapping("/annonces/{id}/lu")
    @Transactional
    public ResponseEntity<?> marquerLue(@PathVariable Long id, Authentication authentication) {
        Etudiant etudiant = utilisateurCourant.etudiant(authentication);
        if (etudiant == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Aucun profil étudiant n'est associé à ce compte."));
        }
        Annonce annonce = annonceRepository.findById(id).orElse(null);
        if (annonce == null) {
            return ResponseEntity.notFound().build();
        }
        // Une annonce adressée à une autre promotion ne doit pas pouvoir être
        // acquittée — ce serait admettre qu'elle est visible.
        if (!estVisiblePour(annonce, etudiant)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Cette communication ne vous est pas destinée."));
        }
        if (!lectureRepository.existsByAnnonceIdAndEtudiantId(id, etudiant.getId())) {
            lectureRepository.save(new AnnonceLecture(annonce, etudiant));
        }
        return ResponseEntity.ok(Map.of("lu", true));
    }

    /** Acquitte toutes les communications visibles d'un coup. */
    @PostMapping("/annonces/tout-lu")
    @Transactional
    public ResponseEntity<?> marquerToutLu(Authentication authentication) {
        Etudiant etudiant = utilisateurCourant.etudiant(authentication);
        if (etudiant == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Aucun profil étudiant n'est associé à ce compte."));
        }
        List<Long> lues = lectureRepository.findAnnonceIdsLues(etudiant.getId());
        List<AnnonceLecture> nouvelles = annoncesVisibles(etudiant).stream()
                .filter(annonce -> !lues.contains(annonce.getId()))
                .map(annonce -> new AnnonceLecture(annonce, etudiant))
                .toList();
        lectureRepository.saveAll(nouvelles);
        return ResponseEntity.ok(Map.of("marquees", nouvelles.size()));
    }

    /** Publication d'une communication. Réservé au rôle FORMATEUR par l'URL. */
    @PostMapping("/formateur/annonces")
    @Transactional
    public ResponseEntity<?> publier(@RequestBody AnnonceCreateRequest request, Authentication authentication) {
        User auteur = utilisateurCourant.utilisateur(authentication);
        if (auteur == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (estVide(request.titre()) || estVide(request.contenu())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le titre et le contenu sont obligatoires."));
        }
        Promotion promotion = null;
        if (request.promotionId() != null) {
            promotion = promotionRepository.findById(request.promotionId()).orElse(null);
            if (promotion == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Promotion introuvable."));
            }
        }
        Annonce annonce = new Annonce(
                request.titre().trim(),
                request.contenu().trim(),
                request.type() == null ? EnumTypeAnnonce.ANNONCE : request.type(),
                auteur,
                promotion);
        annonce.setEpinglee(Boolean.TRUE.equals(request.epinglee()));
        annonce = annonceRepository.save(annonce);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(annonce, false));
    }

    /** Retrait d'une communication, réservé à son auteur. */
    @DeleteMapping("/formateur/annonces/{id}")
    @Transactional
    public ResponseEntity<?> retirer(@PathVariable Long id, Authentication authentication) {
        User utilisateur = utilisateurCourant.utilisateur(authentication);
        Annonce annonce = annonceRepository.findById(id).orElse(null);
        if (annonce == null) {
            return ResponseEntity.notFound().build();
        }
        if (utilisateur == null || annonce.getAuteur() == null
                || !annonce.getAuteur().getId().equals(utilisateur.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Seul l'auteur peut retirer cette communication."));
        }
        annonceRepository.delete(annonce);
        return ResponseEntity.noContent().build();
    }

    // ===================== Visibilité =====================

    private List<Annonce> annoncesVisibles(Etudiant etudiant) {
        if (etudiant == null) {
            // Formateur ou compte sans profil étudiant : le fil institutionnel.
            return annonceRepository.findAllByOrderByEpingleeDescDatePublicationDesc();
        }
        Promotion promotion = etudiant.getPromotion();
        return promotion == null
                ? annonceRepository.findGenerales()
                : annonceRepository.findVisiblesPour(promotion.getId());
    }

    private boolean estVisiblePour(Annonce annonce, Etudiant etudiant) {
        if (annonce.getPromotion() == null) {
            return true;
        }
        Promotion promotion = etudiant.getPromotion();
        return promotion != null && promotion.getId().equals(annonce.getPromotion().getId());
    }

    private AnnonceDto toDto(Annonce annonce, boolean lue) {
        return new AnnonceDto(
                annonce.getId(),
                annonce.getTitre(),
                annonce.getContenu(),
                annonce.getType(),
                annonce.getDatePublication(),
                utilisateurCourant.nomAffiche(annonce.getAuteur()),
                annonce.getPromotion() == null ? null : annonce.getPromotion().getLibelle(),
                annonce.isEpinglee(),
                lue);
    }

    private boolean estVide(String valeur) {
        return valeur == null || valeur.trim().isEmpty();
    }
}
