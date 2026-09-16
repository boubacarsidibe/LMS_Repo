package inpg.sn.lms.recrutement.service;

import inpg.sn.lms.exception.BusinessException;
import inpg.sn.lms.exception.ResourceNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.CandidatRepository;
import inpg.sn.lms.recrutement.dto.CandidatureRequest;
import inpg.sn.lms.recrutement.dto.CandidatureResponse;
import inpg.sn.lms.recrutement.dto.FiltreCriteresResponse;
import inpg.sn.lms.recrutement.dto.PaiementResponse;
import inpg.sn.lms.recrutement.dto.PieceJustificativeResponse;
import inpg.sn.lms.recrutement.dto.RapportRecrutementDto;
import inpg.sn.lms.recrutement.dto.RapportStatistiqueDto;
import inpg.sn.lms.recrutement.dto.ReponseCritereResponse;
import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.ReponseCritereCandidature;
import inpg.sn.lms.recrutement.entity.SessionCandidature;
import inpg.sn.lms.recrutement.enums.EnumEtapeQms;
import inpg.sn.lms.recrutement.enums.EnumStatutCandidature;
import inpg.sn.lms.recrutement.repository.CandidatureRepository;
import inpg.sn.lms.recrutement.repository.SessionCandidatureRepository;

@Service
@Transactional
public class CandidatureService {

    private final CandidatureRepository candidatureRepository;
    private final SessionCandidatureRepository sessionCandidatureRepository;
    private final CandidatRepository candidatRepository;
    private final JournalEtapeService journalEtapeService;

    public CandidatureService(CandidatureRepository candidatureRepository,
            SessionCandidatureRepository sessionCandidatureRepository,
            CandidatRepository candidatRepository,
            JournalEtapeService journalEtapeService) {
        this.candidatureRepository = candidatureRepository;
        this.sessionCandidatureRepository = sessionCandidatureRepository;
        this.candidatRepository = candidatRepository;
        this.journalEtapeService = journalEtapeService;
    }

    /**
     * Consigne une étape de la procédure QMS-009 sur un dossier. Passe par ce service
     * plutôt que directement par le journal : les appelants n'ont ainsi pas besoin du
     * dépôt des candidatures pour retrouver l'entité.
     */
    public void journaliser(Long candidatureId, EnumEtapeQms etape, EnumStatutCandidature statutResultant,
            User auteur, String commentaire) {
        Candidature candidature = candidatureRepository.findById(candidatureId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature non trouvée avec l'ID: " + candidatureId));
        journalEtapeService.enregistrer(candidature, etape, statutResultant, auteur, commentaire);
    }

    /**
     * Filtre automatique de premier niveau (TDR 3.1.1) : évalue chaque dossier SOUMIS
     * de la session par rapport à ses critères (de base + personnalisés) et bascule en
     * présélection ou rejette en conséquence. Déclenché à la main par le responsable
     * admission (jamais un effet de bord de la soumission — voir CandidatController),
     * afin que chaque dossier reste visible à l'état SOUMIS le temps de la revue.
     */
    public FiltreCriteresResponse appliquerFiltreCriteres(Long sessionId, User auteur) {
        SessionCandidature session = sessionCandidatureRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session non trouvée avec l'ID: " + sessionId));
        if (!session.isCriteresActifs()) {
            throw new BusinessException("Le filtre de critères n'est pas activé pour cette session.");
        }

        List<Candidature> soumis = candidatureRepository.findBySessionCandidatureId(sessionId).stream()
                .filter(c -> c.getStatut() == EnumStatutCandidature.SOUMIS)
                .toList();

        int preselectionnes = 0;
        int rejetes = 0;
        for (Candidature candidature : soumis) {
            Map<Long, String> reponses = candidature.getReponsesCriteres().stream()
                    .collect(Collectors.toMap(r -> r.getCritere().getId(), ReponseCritereCandidature::getValeur));
            String resultat;
            if (session.respecteCriteres(candidature.getCandidat(), reponses)) {
                candidature.setStatut(EnumStatutCandidature.EN_COURS_DE_PRESELECTION);
                resultat = "Critères remplis : dossier retenu pour la présélection.";
                preselectionnes++;
            } else {
                candidature.setStatut(EnumStatutCandidature.REJETE);
                candidature.setMotifRejet("Critères de présélection de la session non remplis (filtre automatique).");
                resultat = "Critères non remplis : dossier rejeté par le filtre automatique.";
                rejetes++;
            }
            candidatureRepository.save(candidature);
            journalEtapeService.enregistrer(candidature, EnumEtapeQms.FILTRE_INITIAL,
                    candidature.getStatut(), auteur, resultat);
        }

        return FiltreCriteresResponse.builder()
                .total(soumis.size())
                .preselectionnes(preselectionnes)
                .rejetes(rejetes)
                .build();
    }

    // creerBrouillon() et soumettreCandidature() vivaient ici sans aucun appelant :
    // CandidatController fait tout lui-même sur /candidature/soumettre. Elles étaient
    // les seules à produire le statut BROUILLON, resté de ce fait inatteignable — d'où
    // une branche de estTransitionValide() ci-dessous que rien ne peut emprunter. Le
    // brouillon réel est géré par BrouillonCandidature, une entité distincte, ce qui
    // rendait ce code d'autant plus trompeur à la lecture.

    /**
     * Met à jour le statut d'une candidature (utilisé par l'admin)
     */
    public CandidatureResponse changerStatut(Long candidatureId, EnumStatutCandidature nouveauStatut, String motif) {
        Candidature candidature = candidatureRepository.findById(candidatureId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Candidature non trouvée avec l'ID: " + candidatureId));

        // Validation des transitions autorisées
        if (!estTransitionValide(candidature.getStatut(), nouveauStatut)) {
            throw new BusinessException("Transition de statut non autorisée: "
                    + candidature.getStatut() + " → " + nouveauStatut);
        }

        candidature.setStatut(nouveauStatut);
        if (nouveauStatut == EnumStatutCandidature.REJETE && motif != null) {
            candidature.setMotifRejet(motif);
        }

        Candidature saved = candidatureRepository.save(candidature);
        return toResponse(saved);
    }

    /**
     * Récupère toutes les candidatures d'un candidat
     */
    public List<CandidatureResponse> getCandidaturesByCandidatId(Long candidatId) {
        return candidatureRepository.findByCandidatId(candidatId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère toutes les candidatures d'une session
     */
    public List<CandidatureResponse> getCandidaturesBySessionId(Long sessionId) {
        return candidatureRepository.findBySessionCandidatureId(sessionId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère toutes les candidatures par statut
     */
    public List<CandidatureResponse> getCandidaturesByStatut(EnumStatutCandidature statut) {
        return candidatureRepository.findByStatut(statut)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère une candidature par son ID
     */
    public CandidatureResponse getCandidatureById(Long id) {
        Candidature candidature = candidatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature non trouvée avec l'ID: " + id));
        return toResponse(candidature);
    }

    /**
     * Récupère une candidature par numéro de dossier
     */
    public CandidatureResponse getCandidatureByNumeroDossier(String numeroDossier) {
        Candidature candidature = candidatureRepository.findByNumeroDossier(numeroDossier)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Candidature non trouvée avec le numéro: " + numeroDossier));
        return toResponse(candidature);
    }

    /**
     * Get toutes les candidatures
     */
    public List<CandidatureResponse> getAllCandidatures() {
        return candidatureRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private static final List<EnumStatutCandidature> ETAPES_WORKFLOW = List.of(
            EnumStatutCandidature.SOUMIS,
            EnumStatutCandidature.EN_COURS_DE_PRESELECTION,
            EnumStatutCandidature.ADMIS1,
            EnumStatutCandidature.ADMIS2,
            EnumStatutCandidature.ADMIS3,
            EnumStatutCandidature.REJETE);

    private static final Map<EnumStatutCandidature, String> LIBELLES_ETAPES = Map.of(
            EnumStatutCandidature.SOUMIS, "Soumis",
            EnumStatutCandidature.EN_COURS_DE_PRESELECTION, "En présélection",
            EnumStatutCandidature.ADMIS1, "Admis au test académique",
            EnumStatutCandidature.ADMIS2, "Admis à l'entretien",
            EnumStatutCandidature.ADMIS3, "Admis (décision finale)",
            EnumStatutCandidature.REJETE, "Rejetés");

    /**
     * Rapport qualité recrutement (demande responsable admission) : répartition des
     * candidatures par étape du workflow, avec la liste nominative de chaque étape.
     * Ne recalcule rien : s'appuie sur getAllCandidatures()/toResponse() déjà en place.
     */
    public RapportRecrutementDto genererRapportParEtape() {
        List<CandidatureResponse> toutes = getAllCandidatures();

        List<RapportRecrutementDto.EtapeRapport> etapes = ETAPES_WORKFLOW.stream()
                .map(statut -> {
                    List<RapportRecrutementDto.CandidatRapport> candidats = toutes.stream()
                            .filter(c -> c.getStatut() == statut)
                            .sorted((a, b) -> {
                                LocalDateTime da = a.getDateSoumission();
                                LocalDateTime db = b.getDateSoumission();
                                if (da == null || db == null) return 0;
                                return da.compareTo(db);
                            })
                            .map(c -> new RapportRecrutementDto.CandidatRapport(
                                    c.getNumeroDossier(),
                                    c.getCandidatNom(),
                                    c.getCandidatPrenom(),
                                    c.getCandidatEmail(),
                                    c.getSessionCandidatureLibelle(),
                                    c.getDateSoumission(),
                                    statut == EnumStatutCandidature.REJETE ? c.getMotifRejet() : null))
                            .toList();
                    return new RapportRecrutementDto.EtapeRapport(
                            statut.name(), LIBELLES_ETAPES.get(statut), candidats.size(), candidats);
                })
                .toList();

        return new RapportRecrutementDto(toutes.size(), etapes);
    }

    /**
     * Rapport statistique recrutement (demande responsable admission) : mêmes
     * agrégats que la page Statistiques du portail (répartition par étape, taux
     * d'admission, profil démographique) — recalculés ici à l'identique pour que
     * le PDF téléchargé corresponde à ce que l'écran affiche.
     */
    public RapportStatistiqueDto genererRapportStatistique() {
        List<CandidatureResponse> toutes = getAllCandidatures();
        int total = toutes.size();

        int validationEnCours = compterStatut(toutes, EnumStatutCandidature.SOUMIS);
        int preselectionnes = (int) toutes.stream()
                .filter(c -> c.getStatut() == EnumStatutCandidature.EN_COURS_DE_PRESELECTION
                        || c.getStatut() == EnumStatutCandidature.ADMIS1
                        || c.getStatut() == EnumStatutCandidature.ADMIS2)
                .count();
        int rejetes = compterStatut(toutes, EnumStatutCandidature.REJETE);

        List<RapportStatistiqueDto.Segment> repartitionStatuts = ETAPES_WORKFLOW.stream()
                .map(statut -> segment(LIBELLES_ETAPES.get(statut), compterStatut(toutes, statut), total))
                .toList();

        int candidaturesDecidees = (int) toutes.stream()
                .filter(c -> c.getStatut() == EnumStatutCandidature.ADMIS3 || c.getStatut() == EnumStatutCandidature.REJETE)
                .count();
        int admis = compterStatut(toutes, EnumStatutCandidature.ADMIS3);
        int tauxAdmission = candidaturesDecidees == 0 ? 0 : Math.round(100f * admis / candidaturesDecidees);

        List<RapportStatistiqueDto.Segment> repartitionGenre = repartitionGenre(toutes, total);
        List<RapportStatistiqueDto.Segment> repartitionAge = repartitionAge(toutes, total);
        List<RapportStatistiqueDto.Segment> topProfessions = topGroupes(toutes,
                c -> c.getCandidatProfession() == null ? "" : c.getCandidatProfession().trim(),
                true, total);
        int nbSansProfession = (int) toutes.stream()
                .filter(c -> c.getCandidatProfession() == null || c.getCandidatProfession().trim().isEmpty())
                .count();
        List<RapportStatistiqueDto.Segment> topNationalites = topGroupes(toutes,
                c -> c.getCandidatNationalite() == null ? "" : c.getCandidatNationalite().trim(),
                false, total);

        return new RapportStatistiqueDto(total, validationEnCours, preselectionnes, rejetes,
                repartitionStatuts, tauxAdmission, candidaturesDecidees,
                repartitionGenre, repartitionAge, topProfessions, nbSansProfession, topNationalites);
    }

    private int compterStatut(List<CandidatureResponse> toutes, EnumStatutCandidature statut) {
        return (int) toutes.stream().filter(c -> c.getStatut() == statut).count();
    }

    private RapportStatistiqueDto.Segment segment(String label, int count, int total) {
        int pourcentage = total == 0 ? 0 : Math.round(100f * count / total);
        return new RapportStatistiqueDto.Segment(label, count, pourcentage);
    }

    private List<RapportStatistiqueDto.Segment> repartitionGenre(List<CandidatureResponse> toutes, int total) {
        List<RapportStatistiqueDto.Segment> segments = new java.util.ArrayList<>();
        for (String genre : List.of("Homme", "Femme", "Autre")) {
            int count = (int) toutes.stream().filter(c -> genre.equals(c.getCandidatGenre())).count();
            segments.add(segment(genre, count, total));
        }
        int nonRenseigne = (int) toutes.stream().filter(c -> c.getCandidatGenre() == null || c.getCandidatGenre().isBlank()).count();
        if (nonRenseigne > 0) {
            segments.add(segment("Non renseigné", nonRenseigne, total));
        }
        return segments;
    }

    private List<RapportStatistiqueDto.Segment> repartitionAge(List<CandidatureResponse> toutes, int total) {
        List<Integer> ages = toutes.stream()
                .map(CandidatureResponse::getCandidatDateNaissance)
                .filter(java.util.Objects::nonNull)
                .map(naissance -> java.time.Period.between(naissance, java.time.LocalDate.now()).getYears())
                .toList();
        record Tranche(String libelle, java.util.function.IntPredicate test) {
        }
        List<Tranche> tranches = List.of(
                new Tranche("< 20 ans", a -> a < 20),
                new Tranche("20-25 ans", a -> a >= 20 && a <= 25),
                new Tranche("26-30 ans", a -> a >= 26 && a <= 30),
                new Tranche("31-40 ans", a -> a >= 31 && a <= 40),
                new Tranche("> 40 ans", a -> a > 40));
        return tranches.stream()
                .map(t -> segment(t.libelle(), (int) ages.stream().filter(a -> t.test().test(a)).count(), total))
                .toList();
    }

    /**
     * Regroupements les plus fréquents (top 6) d'une valeur déclarative libre —
     * profession ou nationalité. {@code normaliserCasse} reproduit exactement le
     * comportement de la page Statistiques : la profession est regroupée sans
     * tenir compte de la casse puis réaffichée avec une seule majuscule initiale
     * (perdant la casse d'origine) ; la nationalité, elle, vient d'une liste
     * fermée déjà correctement casée et n'est jamais transformée.
     */
    private List<RapportStatistiqueDto.Segment> topGroupes(List<CandidatureResponse> toutes,
            java.util.function.Function<CandidatureResponse, String> extracteur, boolean normaliserCasse, int total) {
        Map<String, Integer> comptes = new java.util.LinkedHashMap<>();
        for (CandidatureResponse c : toutes) {
            String valeur = extracteur.apply(c);
            if (valeur == null || valeur.isBlank()) {
                continue;
            }
            String cle = normaliserCasse ? valeur.toLowerCase() : valeur;
            comptes.merge(cle, 1, Integer::sum);
        }
        return comptes.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(6)
                .map(e -> segment(
                        normaliserCasse ? Character.toUpperCase(e.getKey().charAt(0)) + e.getKey().substring(1) : e.getKey(),
                        e.getValue(), total))
                .toList();
    }

    // --- Méthodes privées ---

    private String genererNumeroDossier() {
        return "INPG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private boolean estTransitionValide(EnumStatutCandidature actuel, EnumStatutCandidature nouveau) {
        return switch (actuel) {
            case BROUILLON -> nouveau == EnumStatutCandidature.SOUMIS || nouveau == EnumStatutCandidature.BROUILLON;
            case SOUMIS -> nouveau == EnumStatutCandidature.EN_COURS_DE_PRESELECTION
                    || nouveau == EnumStatutCandidature.REJETE;
            case EN_COURS_DE_PRESELECTION ->
                nouveau == EnumStatutCandidature.ADMIS1 || nouveau == EnumStatutCandidature.REJETE;
            case ADMIS1 -> nouveau == EnumStatutCandidature.ADMIS2 || nouveau == EnumStatutCandidature.REJETE;
            case ADMIS2 -> nouveau == EnumStatutCandidature.ADMIS3 || nouveau == EnumStatutCandidature.REJETE;
            case ADMIS3, REJETE -> false; // État terminal
        };
    }

    private CandidatureResponse toResponse(Candidature c) {
        List<PieceJustificativeResponse> pieces = c.getPiecesJustificatives().stream()
                .map(p -> PieceJustificativeResponse.builder()
                        .id(p.getId())
                        .typeDocument(p.getTypeDocument())
                        .estConforme(p.isEstConforme())
                        .description(p.getDescription())
                        .fichierStockeId(p.getFichierStocke() != null ? p.getFichierStocke().getId() : null)
                        .fichierNomOriginal(
                                p.getFichierStocke() != null ? p.getFichierStocke().getNomOriginal() : null)
                        .build())
                .collect(Collectors.toList());

        PaiementResponse paiementResponse = null;
        if (c.getPaiement() != null) {
            paiementResponse = PaiementResponse.builder()
                    .id(c.getPaiement().getId())
                    .referenceTransaction(c.getPaiement().getReferenceTransaction())
                    .montant(c.getPaiement().getMontant())
                    .devise(c.getPaiement().getDevise())
                    .statut(c.getPaiement().getStatut())
                    .datePaiement(c.getPaiement().getDatePaiement())
                    .methodePaiement(c.getPaiement().getMethodePaiement())
                    .build();
        }

        return CandidatureResponse.builder()
                .id(c.getId())
                .numeroDossier(c.getNumeroDossier())
                .statut(c.getStatut())
                .dateSoumission(c.getDateSoumission())
                .motifRejet(c.getMotifRejet())
                .candidatId(c.getCandidat().getId())
                .candidatNom(c.getCandidat().getNom())
                .candidatPrenom(c.getCandidat().getPrenom())
                .candidatEmail(c.getCandidat().getUser() != null ? c.getCandidat().getUser().getEmail() : null)
                .candidatGenre(c.getCandidat().getGenre())
                .candidatProfession(c.getCandidat().getProfession())
                .candidatAnneesExperience(c.getCandidat().getAnneesExperience())
                .candidatDateNaissance(c.getCandidat().getDateNaissance())
                .candidatNiveauEtude(c.getCandidat().getNiveauEtude())
                .candidatNationalite(c.getCandidat().getNationalite())
                .sessionCandidatureId(c.getSessionCandidature().getId())
                .sessionCandidatureLibelle(c.getSessionCandidature().getLibelle())
                .sessionInfoFraisScolarite(c.getSessionCandidature().getInfoFraisScolarite())
                .sessionInfoCarteEtudiante(c.getSessionCandidature().getInfoCarteEtudiante())
                .sessionInfoRentree(c.getSessionCandidature().getInfoRentree())
                .informationsAcademiques(c.getInformationsAcademiques())
                .informationsProfessionnelles(c.getInformationsProfessionnelles())
                .piecesJustificatives(pieces)
                .paiement(paiementResponse)
                .reponsesCriteres(c.getReponsesCriteres().stream()
                        .map(r -> ReponseCritereResponse.builder()
                                .critereId(r.getCritere().getId())
                                .libelle(r.getCritere().getLibelle())
                                .valeur(r.getValeur())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}