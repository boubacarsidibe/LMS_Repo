package inpg.sn.lms.espace_etudiant.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.espace_etudiant.dto.CertificatApercuDto;
import inpg.sn.lms.espace_etudiant.dto.CertificatLotRequest;
import inpg.sn.lms.espace_etudiant.dto.CertificatLotResultatDto;
import inpg.sn.lms.espace_etudiant.entity.Certificat;
import inpg.sn.lms.espace_etudiant.entity.Formation;
import inpg.sn.lms.espace_etudiant.entity.Promotion;
import inpg.sn.lms.espace_etudiant.repository.PromotionRepository;

/**
 * Délivrance de certificats à une promotion entière, à toute la promotion sauf quelques
 * étudiants, ou à un seul de ses étudiants (un retardataire, une délivrance isolée).
 *
 * <p>Une promotion suit une seule formation (CLAUDE.md §3) : l'intitulé imprimé est
 * celui de cette formation, lu ici côté serveur, jamais saisi. Les règles de fond sont
 * celles de la délivrance à l'unité, appliquées étudiant par étudiant : programme
 * terminé ({@link CompletionProgrammeService}), pas de doublon, civilité connue. Un
 * étudiant qui ne les remplit pas est écarté avec sa raison ; les autres reçoivent
 * leur certificat — un seul retardataire ne bloque pas une promotion de trente.
 *
 * <p>Les erreurs de demande (promotion inconnue, dates inversées, mode inconnu…) sont
 * levées en {@link IllegalArgumentException}, avant la moindre écriture.
 */
@Service
public class CertificatLotService {

    public static final int INTITULE_MAX = 150;
    public static final int CODE_MAX = 60;
    /** « /NN » ajouté au code de session. */
    private static final int SUFFIXE_CODE = 3;
    public static final String SIGNATAIRE = "INPG — Direction pédagogique";

    static final String MODE_TOUS = "TOUS";
    static final String MODE_TOUS_SAUF = "TOUS_SAUF";
    static final String MODE_UN_SEUL = "UN_SEUL";
    private static final Set<String> CIVILITES = Set.of("Monsieur", "Madame", "Mademoiselle");

    private final PromotionRepository promotionRepository;
    private final EtudiantRepository etudiantRepository;
    private final CompletionProgrammeService completionProgrammeService;
    private final CertificatService certificatService;

    public CertificatLotService(PromotionRepository promotionRepository, EtudiantRepository etudiantRepository,
            CompletionProgrammeService completionProgrammeService, CertificatService certificatService) {
        this.promotionRepository = promotionRepository;
        this.etudiantRepository = etudiantRepository;
        this.completionProgrammeService = completionProgrammeService;
        this.certificatService = certificatService;
    }

    /** Vide si la promotion n'existe pas. */
    @Transactional(readOnly = true)
    public Optional<CertificatApercuDto> apercu(Long promotionId) {
        if (promotionId == null) {
            return Optional.empty();
        }
        return promotionRepository.findById(promotionId).map(promotion -> {
            Formation formation = promotion.getFormation();
            List<CertificatApercuDto.EtudiantApercu> etudiants = etudiantsDe(promotion).stream()
                    .map(etudiant -> {
                        List<String> motifs = completionProgrammeService.coursNonTermines(etudiant);
                        return new CertificatApercuDto.EtudiantApercu(etudiant.getId(), nomComplet(etudiant),
                                etudiant.getMatricule(), etudiant.getCivilite(), etudiant.getDateNaissance(),
                                motifs.isEmpty(), motifs);
                    })
                    .toList();
            return new CertificatApercuDto(promotion.getId(), promotion.getLibelle(),
                    formation == null ? null : formation.getIntitule(),
                    promotion.getDateDebut(), promotion.getDateFin(), etudiants);
        });
    }

    @Transactional
    public CertificatLotResultatDto delivrer(CertificatLotRequest request) {
        if (request.promotionId() == null) {
            throw new IllegalArgumentException("La promotion est obligatoire.");
        }
        Promotion promotion = promotionRepository.findById(request.promotionId())
                .orElseThrow(() -> new IllegalArgumentException("Promotion introuvable."));
        String mode = request.mode() == null ? "" : request.mode().trim();
        if (!MODE_TOUS.equals(mode) && !MODE_TOUS_SAUF.equals(mode) && !MODE_UN_SEUL.equals(mode)) {
            throw new IllegalArgumentException(
                    "Choisissez « tous les étudiants », « tous sauf… » ou « un seul étudiant ».");
        }
        Formation formation = promotion.getFormation();
        if (formation == null) {
            throw new IllegalArgumentException(
                    "Cette promotion n'est rattachée à aucune formation : rattachez-la d'abord à sa formation.");
        }
        String intitule = formation.getIntitule() == null ? "" : formation.getIntitule().trim();
        if (intitule.length() > INTITULE_MAX) {
            throw new IllegalArgumentException("L'intitulé de la formation est trop long pour le certificat ("
                    + INTITULE_MAX + " caractères au plus).");
        }
        if (request.dateDebut() == null || request.dateFin() == null) {
            throw new IllegalArgumentException("Les dates de début et de fin de la formation sont obligatoires.");
        }
        if (request.dateFin().isBefore(request.dateDebut())) {
            throw new IllegalArgumentException("La date de fin de la formation précède sa date de début.");
        }
        // Un seul certificat : le code est imprimé tel quel. Plusieurs : un numéro d'ordre s'y ajoute.
        boolean unSeul = MODE_UN_SEUL.equals(mode);
        String prefixe = request.codeSession() == null ? "" : request.codeSession().trim();
        if (unSeul && prefixe.length() > CODE_MAX) {
            throw new IllegalArgumentException("Le code est trop long (" + CODE_MAX + " caractères au plus).");
        }
        if (!unSeul && prefixe.length() + SUFFIXE_CODE > CODE_MAX) {
            throw new IllegalArgumentException("Le code est trop long (" + (CODE_MAX - SUFFIXE_CODE)
                    + " caractères au plus : un numéro d'ordre y est ajouté).");
        }

        List<Etudiant> promotionComplete = etudiantsDe(promotion);
        Set<Long> exclus = new HashSet<>();
        if (MODE_TOUS_SAUF.equals(mode)) {
            Set<Long> demandes = request.exclusIds() == null ? Set.of() : new HashSet<>(request.exclusIds());
            // Seuls comptent les étudiants de cette promotion : un identifiant étranger ne retire personne.
            promotionComplete.stream().map(Etudiant::getId).filter(demandes::contains).forEach(exclus::add);
            if (exclus.isEmpty()) {
                throw new IllegalArgumentException("Choisissez au moins un étudiant à exclure de cette promotion.");
            }
        }
        if (unSeul) {
            if (request.etudiantId() == null) {
                throw new IllegalArgumentException("Choisissez l'étudiant à certifier.");
            }
            // Recoupé avec la promotion : un identifiant venu d'ailleurs ne doit pas permettre
            // de certifier quelqu'un qui n'y est pas.
            if (promotionComplete.stream().noneMatch(e -> e.getId().equals(request.etudiantId()))) {
                throw new IllegalArgumentException("Cet étudiant n'appartient pas à cette promotion.");
            }
        }
        List<Etudiant> concernes = promotionComplete.stream()
                .filter(e -> unSeul ? e.getId().equals(request.etudiantId()) : !exclus.contains(e.getId()))
                .toList();
        if (concernes.isEmpty()) {
            throw new IllegalArgumentException("Aucun étudiant à certifier dans cette promotion.");
        }

        Map<Long, CertificatLotRequest.Identite> identites = (request.identites() == null
                ? List.<CertificatLotRequest.Identite>of() : request.identites()).stream()
                .filter(i -> i != null && i.etudiantId() != null)
                .collect(Collectors.toMap(CertificatLotRequest.Identite::etudiantId, Function.identity(), (a, b) -> b));

        List<CertificatLotResultatDto.Delivre> delivres = new ArrayList<>();
        List<CertificatLotResultatDto.Refuse> refuses = new ArrayList<>();
        for (Etudiant etudiant : concernes) {
            List<String> motifs = completionProgrammeService.coursNonTermines(etudiant);
            if (!motifs.isEmpty()) {
                refuses.add(refus(etudiant, "Programme non terminé.", motifs));
                continue;
            }
            if (dejaDelivre(etudiant, intitule, request)) {
                refuses.add(refus(etudiant,
                        "Un certificat identique (même formation, même période) a déjà été délivré.", List.of()));
                continue;
            }
            CertificatLotRequest.Identite saisie = identites.get(etudiant.getId());
            String civilite = choisir(saisie == null ? null : saisie.civilite(), etudiant.getCivilite());
            if (civilite == null) {
                refuses.add(refus(etudiant, "Civilité manquante.", List.of()));
                continue;
            }
            if (!CIVILITES.contains(civilite)) {
                refuses.add(refus(etudiant, "Civilité inconnue.", List.of()));
                continue;
            }
            LocalDate naissance = saisie != null && saisie.dateNaissance() != null ? saisie.dateNaissance()
                    : etudiant.getDateNaissance();

            Certificat certificat = new Certificat();
            certificat.setEtudiant(etudiant);
            certificat.setPromotion(promotion);
            certificat.setIntituleFormation(intitule);
            certificat.setFormateurNom(SIGNATAIRE);
            certificat.setCivilite(civilite);
            certificat.setDateNaissance(naissance);
            certificat.setDateDebut(request.dateDebut());
            certificat.setDateFin(request.dateFin());
            if (!prefixe.isEmpty()) {
                certificat.setCodeCertificat(
                        unSeul ? prefixe : prefixe + "/" + String.format("%02d", delivres.size() + 1));
            }
            Certificat enregistre = certificatService.save(certificat);

            // Saisies une fois, retenues pour les prochains certificats de cet étudiant.
            etudiant.setCivilite(civilite);
            etudiant.setDateNaissance(naissance);
            etudiantRepository.save(etudiant);

            delivres.add(new CertificatLotResultatDto.Delivre(enregistre.getId(), enregistre.getReference(),
                    etudiant.getId(), nomComplet(etudiant)));
        }
        return new CertificatLotResultatDto(delivres, refuses);
    }

    private List<Etudiant> etudiantsDe(Promotion promotion) {
        return etudiantRepository.findByPromotionId(promotion.getId()).stream()
                .sorted(Comparator.comparing((Etudiant e) -> minuscules(e.getNom()))
                        .thenComparing(e -> minuscules(e.getPrenom())))
                .toList();
    }

    /** Même règle que la délivrance à l'unité : même étudiant, même formation, même période. */
    private boolean dejaDelivre(Etudiant etudiant, String intitule, CertificatLotRequest request) {
        return certificatService.getByEtudiant(etudiant.getId()).stream()
                .anyMatch(c -> c.getIntituleFormation().trim().equalsIgnoreCase(intitule)
                        && Objects.equals(c.getDateDebut(), request.dateDebut())
                        && Objects.equals(c.getDateFin(), request.dateFin()));
    }

    /** La saisie de l'écran l'emporte sur la fiche ; vide = on garde la fiche. */
    private static String choisir(String saisie, String fiche) {
        if (saisie != null && !saisie.isBlank()) {
            return saisie.trim();
        }
        return fiche == null || fiche.isBlank() ? null : fiche.trim();
    }

    private static CertificatLotResultatDto.Refuse refus(Etudiant etudiant, String raison, List<String> motifs) {
        return new CertificatLotResultatDto.Refuse(etudiant.getId(), nomComplet(etudiant), raison, motifs);
    }

    private static String nomComplet(Etudiant etudiant) {
        return ((etudiant.getPrenom() == null ? "" : etudiant.getPrenom()) + " "
                + (etudiant.getNom() == null ? "" : etudiant.getNom())).trim();
    }

    private static String minuscules(String valeur) {
        return valeur == null ? "" : valeur.toLowerCase();
    }
}
