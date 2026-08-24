package inpg.sn.lms.espace_alumni.controller;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import inpg.sn.lms.auth.entity.AlumniProfil;
import inpg.sn.lms.espace_alumni.dto.AlumniProfilDto;
import inpg.sn.lms.espace_alumni.dto.AlumniProfilUpdateRequest;
import inpg.sn.lms.espace_alumni.dto.SituationProfessionnelleDto;
import inpg.sn.lms.espace_alumni.dto.SituationProfessionnelleRequest;
import inpg.sn.lms.espace_alumni.service.AlumniProfilService;
import inpg.sn.lms.espace_alumni.service.SituationProfessionnelleService;
import inpg.sn.lms.espace_etudiant.dto.ReleveDelivreDto;
import inpg.sn.lms.espace_etudiant.entity.ReleveNotes;
import inpg.sn.lms.espace_etudiant.service.ReleveDelivranceService;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;

/**
 * Espace Alumni (TDR §C) : fiche personnelle, historique professionnel, et accès
 * aux relevés délivrés.
 *
 * <p>Vit sous {@code /api/espace-alumni/**}, protégé par {@code hasRole('ALUMNI')}
 * à la fois au niveau de l'URL ({@code SecurityConfig}) et de la méthode — la
 * double protection est volontaire, cf. {@code PhotothequeController} : le
 * filtrage d'URL seul est trop implicite, un futur endpoint ajouté par erreur en
 * dehors du préfixe filtré serait exposé sans le savoir.
 *
 * <p>Le forum et le partage de documents demandés par la même section du TDR ne
 * sont pas couverts ici (voir la migration V12 et LOG_BLOQUANT.md) : ils demandent
 * une décision de modèle à part entière sur la portée d'un sujet de forum.
 */
@RestController
@RequestMapping("/api/espace-alumni")
@PreAuthorize("hasRole('ALUMNI')")
public class AlumniController {

    private final UtilisateurCourantService utilisateurCourant;
    private final AlumniProfilService alumniProfilService;
    private final SituationProfessionnelleService situationService;
    private final ReleveDelivranceService releveDelivranceService;

    public AlumniController(UtilisateurCourantService utilisateurCourant, AlumniProfilService alumniProfilService,
            SituationProfessionnelleService situationService, ReleveDelivranceService releveDelivranceService) {
        this.utilisateurCourant = utilisateurCourant;
        this.alumniProfilService = alumniProfilService;
        this.situationService = situationService;
        this.releveDelivranceService = releveDelivranceService;
    }

    @GetMapping("/mon-profil")
    @Transactional(readOnly = true)
    public ResponseEntity<?> monProfil(Authentication authentication) {
        AlumniProfil profil = utilisateurCourant.alumniProfil(authentication);
        if (profil == null) {
            return profilIntrouvable();
        }
        return ResponseEntity.ok(alumniProfilService.toDto(profil));
    }

    @PutMapping("/mon-profil")
    @Transactional
    public ResponseEntity<?> mettreAJourProfil(@RequestBody AlumniProfilUpdateRequest requete,
            Authentication authentication) {
        AlumniProfil profil = utilisateurCourant.alumniProfil(authentication);
        if (profil == null) {
            return profilIntrouvable();
        }
        AlumniProfil misAJour = alumniProfilService.mettreAJour(profil, requete);
        return ResponseEntity.ok(alumniProfilService.toDto(misAJour));
    }

    @GetMapping("/mes-situations")
    @Transactional(readOnly = true)
    public ResponseEntity<?> mesSituations(Authentication authentication) {
        AlumniProfil profil = utilisateurCourant.alumniProfil(authentication);
        if (profil == null) {
            return profilIntrouvable();
        }
        return ResponseEntity.ok(situationService.lister(profil.getId()));
    }

    @PostMapping("/mes-situations")
    @Transactional
    public ResponseEntity<?> ajouterSituation(@RequestBody SituationProfessionnelleRequest requete,
            Authentication authentication) {
        AlumniProfil profil = utilisateurCourant.alumniProfil(authentication);
        if (profil == null) {
            return profilIntrouvable();
        }
        String erreur = validerSituation(requete);
        if (erreur != null) {
            return ResponseEntity.badRequest().body(Map.of("message", erreur));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(situationService.creer(profil, requete));
    }

    @PutMapping("/mes-situations/{id}")
    @Transactional
    public ResponseEntity<?> modifierSituation(@PathVariable Long id,
            @RequestBody SituationProfessionnelleRequest requete, Authentication authentication) {
        AlumniProfil profil = utilisateurCourant.alumniProfil(authentication);
        if (profil == null) {
            return profilIntrouvable();
        }
        String erreur = validerSituation(requete);
        if (erreur != null) {
            return ResponseEntity.badRequest().body(Map.of("message", erreur));
        }
        SituationProfessionnelleDto misAJour = situationService.modifier(id, profil, requete);
        if (misAJour == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(misAJour);
    }

    @DeleteMapping("/mes-situations/{id}")
    @Transactional
    public ResponseEntity<?> supprimerSituation(@PathVariable Long id, Authentication authentication) {
        AlumniProfil profil = utilisateurCourant.alumniProfil(authentication);
        if (profil == null) {
            return profilIntrouvable();
        }
        if (!situationService.supprimer(id, profil)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * Relevés officiels délivrés à l'alumni, identique à
     * {@code ScolariteController#mesReleves} : la ligne {@code Etudiant} d'origine
     * n'a pas été supprimée à la conversion (voir migration V12), le relevé délivré
     * reste donc accessible sous le même identifiant.
     */
    @GetMapping("/mes-releves")
    @Transactional(readOnly = true)
    public ResponseEntity<?> mesReleves(Authentication authentication) {
        AlumniProfil profil = utilisateurCourant.alumniProfil(authentication);
        if (profil == null) {
            return profilIntrouvable();
        }
        List<ReleveDelivreDto> releves = releveDelivranceService.deLEtudiant(profil.getId()).stream()
                .map(this::toReleveDelivreDto)
                .toList();
        return ResponseEntity.ok(releves);
    }

    private ReleveDelivreDto toReleveDelivreDto(ReleveNotes releve) {
        var etudiant = releve.getEtudiant();
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

    private String validerSituation(SituationProfessionnelleRequest requete) {
        if (estVide(requete.intitulePoste()) || estVide(requete.entreprise())) {
            return "L'intitulé du poste et l'entreprise sont requis.";
        }
        if (requete.dateDebut() == null) {
            return "La date de début est requise.";
        }
        if (requete.dateFin() != null && requete.dateFin().isBefore(requete.dateDebut())) {
            return "La date de fin ne peut pas précéder la date de début.";
        }
        return null;
    }

    private boolean estVide(String valeur) {
        return valeur == null || valeur.isBlank();
    }

    private ResponseEntity<?> profilIntrouvable() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Aucun profil alumni n'est associé à ce compte."));
    }
}
