package inpg.sn.lms.recrutement.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Period;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.Entretien;
import inpg.sn.lms.recrutement.entity.Paiement;
import inpg.sn.lms.recrutement.entity.SessionCandidature;
import inpg.sn.lms.recrutement.entity.TestAcademique;
import inpg.sn.lms.recrutement.enums.EnumStatutCandidature;
import inpg.sn.lms.recrutement.enums.EnumStatutPaiement;
import inpg.sn.lms.recrutement.repository.CandidatureRepository;
import inpg.sn.lms.recrutement.repository.PaiementRepository;
import inpg.sn.lms.recrutement.repository.EntretienRepository;
import inpg.sn.lms.recrutement.repository.SessionCandidatureRepository;
import inpg.sn.lms.recrutement.repository.TestAcademiqueRepository;
import inpg.sn.lms.recrutement.service.ExcelExportService;
import inpg.sn.lms.recrutement.service.LigneRapport;
import inpg.sn.lms.recrutement.service.RapportSyntheseRecrutementPdfService;

/**
 * Statistiques du recrutement, calculées à partir des données réelles.
 *
 * <p>Avant cette révision, la plupart des méthodes renvoyaient des valeurs
 * codées en dur (repéré lors d'un audit complet des endpoints) — au mieux un
 * gabarit jamais branché, au pire un calcul silencieusement cassé : {@code
 * getKpis} comparait le statut d'un paiement à la chaîne {@code "SUCCESS"},
 * qui ne correspond à aucune valeur de {@link EnumStatutPaiement} (la bonne
 * est {@code SUCCES}), donc les frais collectés valaient toujours 0 et
 * retombaient sur un montant fictif. Toutes les méthodes ci-dessous
 * n'utilisent désormais plus que les repositories.
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private static final int NOMBRE_MOIS_AFFICHES = 5;
    private static final int NOMBRE_PAYS_AFFICHES = 4;

    /** Seuil d'admissibilité par défaut si la session du candidat n'est pas identifiable — même valeur que le frontend (Tests). */
    private static final float SEUIL_ADMISSIBILITE_DEFAUT = 10f;

    private final CandidatureRepository candidatureRepo;
    private final PaiementRepository paiementRepo;
    private final EntretienRepository entretienRepo;
    private final TestAcademiqueRepository testAcademiqueRepo;
    private final SessionCandidatureRepository sessionCandidatureRepo;
    private final RapportSyntheseRecrutementPdfService rapportPdfService;
    private final ExcelExportService excelExportService;

    public AnalyticsController(CandidatureRepository candidatureRepo,
                                PaiementRepository paiementRepo,
                                EntretienRepository entretienRepo,
                                TestAcademiqueRepository testAcademiqueRepo,
                                SessionCandidatureRepository sessionCandidatureRepo,
                                RapportSyntheseRecrutementPdfService rapportPdfService,
                                ExcelExportService excelExportService) {
        this.candidatureRepo = candidatureRepo;
        this.paiementRepo = paiementRepo;
        this.entretienRepo = entretienRepo;
        this.testAcademiqueRepo = testAcademiqueRepo;
        this.sessionCandidatureRepo = sessionCandidatureRepo;
        this.rapportPdfService = rapportPdfService;
        this.excelExportService = excelExportService;
    }

    // ==============================================
    // GET /api/analytics/kpis
    // ==============================================
    @GetMapping("/kpis")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<Map<String, Object>> getKpis() {
        return ResponseEntity.ok(calculerKpis());
    }

    /**
     * Extrait pour être réutilisé par {@link #exportReport}, qui doit refléter
     * exactement les mêmes chiffres que le tableau de bord — les recalculer
     * séparément aurait risqué de les faire diverger.
     */
    private Map<String, Object> calculerKpis() {
        List<Candidature> all = candidatureRepo.findAll();
        LocalDateTime maintenant = LocalDateTime.now();
        YearMonth moisCourant = YearMonth.from(maintenant);
        YearMonth moisPrecedent = moisCourant.minusMonths(1);

        long total = all.size();

        long soumisesCeMois = all.stream()
                .filter(c -> c.getDateSoumission() != null
                        && YearMonth.from(c.getDateSoumission()).equals(moisCourant))
                .count();
        long soumisesMoisPrecedent = all.stream()
                .filter(c -> c.getDateSoumission() != null
                        && YearMonth.from(c.getDateSoumission()).equals(moisPrecedent))
                .count();
        double variation = soumisesMoisPrecedent > 0
                ? ((double) (soumisesCeMois - soumisesMoisPrecedent) / soumisesMoisPrecedent) * 100
                : (soumisesCeMois > 0 ? 100.0 : 0.0);

        long admis = all.stream().filter(c -> c.getStatut() == EnumStatutCandidature.ADMIS
                || c.getStatut() == EnumStatutCandidature.INSCRIPTION_FINALISEE).count();
        double tauxAdmission = total > 0 ? ((double) admis / total) * 100 : 0.0;

        double frais = paiementRepo.findAll().stream()
                .filter(p -> p.getStatut() == EnumStatutPaiement.SUCCES)
                .mapToDouble(p -> p.getMontant() != null ? p.getMontant().doubleValue() : 0)
                .sum();

        double avgDaysCeMois = tempsTraitementMoyen(all, moisCourant);
        double avgDaysMoisPrecedent = tempsTraitementMoyen(all, moisPrecedent);
        double deltaTemps = avgDaysCeMois - avgDaysMoisPrecedent;

        Map<String, Object> kpis = new HashMap<>();
        kpis.put("totalCandidatures", total);
        kpis.put("variation", Math.round(variation * 10.0) / 10.0);
        kpis.put("tauxAdmission", Math.round(tauxAdmission * 10.0) / 10.0);
        // Objectif fixé par l'établissement, pas une donnée calculée.
        kpis.put("cibleAdmission", 15.0);
        kpis.put("fraisCollectes", Math.round(frais));
        kpis.put("tempsTraitement", Math.round(avgDaysCeMois * 10.0) / 10.0);
        kpis.put("deltaTemps", Math.round(deltaTemps * 10.0) / 10.0);
        return kpis;
    }

    /** Jours moyens écoulés depuis la soumission, pour les dossiers soumis durant {@code mois}. */
    private double tempsTraitementMoyen(List<Candidature> candidatures, YearMonth mois) {
        return candidatures.stream()
                .filter(c -> c.getDateSoumission() != null && YearMonth.from(c.getDateSoumission()).equals(mois))
                .mapToLong(c -> ChronoUnit.DAYS.between(c.getDateSoumission(), LocalDateTime.now()))
                .average()
                .orElse(0.0);
    }

    // ==============================================
    // GET /api/analytics/volume-by-month
    // ==============================================
    @GetMapping("/volume-by-month")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<List<Map<String, Object>>> volumeByMonth() {
        List<Candidature> all = candidatureRepo.findAll();
        YearMonth moisCourant = YearMonth.from(LocalDateTime.now());

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = NOMBRE_MOIS_AFFICHES - 1; i >= 0; i--) {
            YearMonth mois = moisCourant.minusMonths(i);
            List<Candidature> duMois = all.stream()
                    .filter(c -> c.getDateSoumission() != null && YearMonth.from(c.getDateSoumission()).equals(mois))
                    .toList();
            long validees = duMois.stream()
                    .filter(c -> c.getStatut() == EnumStatutCandidature.TEST_REUSSI
                            || c.getStatut() == EnumStatutCandidature.ENTRETIEN_EFFECTUE
                            || c.getStatut() == EnumStatutCandidature.ADMIS
                            || c.getStatut() == EnumStatutCandidature.INSCRIPTION_FINALISEE)
                    .count();
            long rejetees = duMois.stream().filter(c -> c.getStatut() == EnumStatutCandidature.DOSSIER_REJETE
                    || c.getStatut() == EnumStatutCandidature.NON_ADMIS).count();

            Map<String, Object> m = new HashMap<>();
            m.put("mois", capitaliser(mois.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH)));
            m.put("total", duMois.size());
            m.put("validees", validees);
            m.put("rejetees", rejetees);
            result.add(m);
        }
        return ResponseEntity.ok(result);
    }

    private String capitaliser(String mot) {
        return mot.isEmpty() ? mot : Character.toUpperCase(mot.charAt(0)) + mot.substring(1);
    }

    // ==============================================
    // GET /api/analytics/geographic-distribution
    // ==============================================
    @GetMapping("/geographic-distribution")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<List<Map<String, Object>>> geographicDistribution() {
        Map<String, Long> parNationalite = candidatureRepo.findAll().stream()
                .map(c -> c.getCandidat() != null ? c.getCandidat().getNationalite() : null)
                .filter(n -> n != null && !n.isBlank())
                .collect(Collectors.groupingBy(n -> n, Collectors.counting()));

        long total = parNationalite.values().stream().mapToLong(Long::longValue).sum();
        List<Map.Entry<String, Long>> triees = parNationalite.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .toList();

        List<Map<String, Object>> result = new ArrayList<>();
        long effectifAutres = 0;
        for (int i = 0; i < triees.size(); i++) {
            Map.Entry<String, Long> entree = triees.get(i);
            if (i < NOMBRE_PAYS_AFFICHES) {
                result.add(createGeoEntry(entree.getKey(), pourcentage(entree.getValue(), total), entree.getValue()));
            } else {
                effectifAutres += entree.getValue();
            }
        }
        if (effectifAutres > 0) {
            result.add(createGeoEntry("Autres", pourcentage(effectifAutres, total), effectifAutres));
        }
        return ResponseEntity.ok(result);
    }

    private double pourcentage(long effectif, long total) {
        return total > 0 ? Math.round(effectif * 1000.0 / total) / 10.0 : 0.0;
    }

    private Map<String, Object> createGeoEntry(String pays, double pourcentage, long effectif) {
        Map<String, Object> m = new HashMap<>();
        m.put("pays", pays);
        m.put("pourcentage", pourcentage);
        m.put("effectif", effectif);
        return m;
    }

    // ==============================================
    // GET /api/analytics/conversion-funnel
    // ==============================================
    @GetMapping("/conversion-funnel")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<List<Map<String, Object>>> conversionFunnel() {
        List<Candidature> all = candidatureRepo.findAll();

        long crees = all.stream().filter(c -> c.getStatut() != EnumStatutCandidature.BROUILLON).count();
        long payes = all.stream()
                .filter(c -> c.getStatut() != EnumStatutCandidature.BROUILLON)
                .filter(this::aUnPaiementReussi)
                .count();
        long admissibles = all.stream().filter(c -> estAuMoins(c.getStatut(), EnumStatutCandidature.TEST_REUSSI)).count();
        long entretiens = all.stream().filter(c -> estAuMoins(c.getStatut(), EnumStatutCandidature.CONVOQUE_ENTRETIEN)).count();
        long inscrits = all.stream().filter(c -> c.getStatut() == EnumStatutCandidature.INSCRIPTION_FINALISEE).count();

        List<Map<String, Object>> funnel = new ArrayList<>();
        funnel.add(createFunnelStep("Dossiers créés", crees, 100));
        funnel.add(createFunnelStep("Payés", payes, tauxPourcentage(payes, crees)));
        funnel.add(createFunnelStep("Admissibles", admissibles, tauxPourcentage(admissibles, crees)));
        funnel.add(createFunnelStep("Entretiens", entretiens, tauxPourcentage(entretiens, crees)));
        funnel.add(createFunnelStep("Inscrits", inscrits, tauxPourcentage(inscrits, crees)));
        return ResponseEntity.ok(funnel);
    }

    private boolean aUnPaiementReussi(Candidature candidature) {
        Paiement paiement = candidature.getPaiement();
        return paiement != null && paiement.getStatut() == EnumStatutPaiement.SUCCES;
    }

    /** Un statut donné a-t-il au moins atteint {@code seuil} dans le parcours SOUMIS→ADMIS3 ? */
    private boolean estAuMoins(EnumStatutCandidature statut, EnumStatutCandidature seuil) {
        List<EnumStatutCandidature> parcours = List.of(EnumStatutCandidature.SOUMIS,
                EnumStatutCandidature.PRESELECTIONNE, EnumStatutCandidature.PAIEMENT_EN_ATTENTE,
                EnumStatutCandidature.PAIEMENT_CONFIRME, EnumStatutCandidature.CONVOQUE_TEST,
                EnumStatutCandidature.TEST_REUSSI, EnumStatutCandidature.CONVOQUE_ENTRETIEN,
                EnumStatutCandidature.ENTRETIEN_EFFECTUE, EnumStatutCandidature.ADMIS,
                EnumStatutCandidature.INSCRIPTION_FINALISEE);
        int indexStatut = parcours.indexOf(statut);
        int indexSeuil = parcours.indexOf(seuil);
        return indexStatut >= 0 && indexStatut >= indexSeuil;
    }

    private int tauxPourcentage(long nombre, long total) {
        return total > 0 ? (int) Math.round(nombre * 100.0 / total) : 0;
    }

    private Map<String, Object> createFunnelStep(String etape, long nombre, int pourcentage) {
        Map<String, Object> m = new HashMap<>();
        m.put("etape", etape);
        m.put("nombre", nombre);
        m.put("pourcentage", pourcentage);
        return m;
    }

    // ==============================================
    // GET /api/analytics/export-report
    // ==============================================
    @GetMapping("/export-report")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<byte[]> exportReport(@RequestParam(defaultValue = "2026-2027") String annee) {
        Map<String, Object> kpis = calculerKpis();

        StringBuilder csv = new StringBuilder();
        csv.append("Rapport Statistiques INPG - Année ").append(annee).append("\n\n");
        csv.append("Indicateur,Valeur\n");
        csv.append("Total Candidatures,").append(kpis.get("totalCandidatures")).append("\n");
        csv.append("Taux Admission,").append(kpis.get("tauxAdmission")).append("%\n");
        csv.append("Frais Collectés,").append(kpis.get("fraisCollectes")).append(" FCFA\n");
        csv.append("Temps Traitement Moyen,").append(kpis.get("tempsTraitement")).append("j\n");
        byte[] bytes = csv.toString().getBytes();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=rapport_statistiques_" + annee + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }

    // ==============================================
    // GET /api/analytics/rapport-candidatures
    // GET /api/analytics/rapport-tests
    // GET /api/analytics/rapport-entretiens
    //
    // Rapport de synthèse par étape (indicateurs + répartitions), au format
    // choisi — PDF (RapportSyntheseRecrutementPdfService) ou Excel avec graphique natif
    // (ExcelExportService, déjà utilisé par l'export Candidatures). Les deux
    // formats partagent exactement les mêmes lignes calculées une seule fois
    // ci-dessous, pour ne jamais diverger entre eux ni de l'écran.
    // ==============================================

    @GetMapping("/rapport-candidatures")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<byte[]> rapportCandidatures(@RequestParam(defaultValue = "pdf") String format) {
        return genererReponseRapport("rapport_candidatures", "Rapport de synthèse — Candidatures",
                construireRapportCandidatures(), format);
    }

    @GetMapping("/rapport-tests")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<byte[]> rapportTests(@RequestParam(defaultValue = "pdf") String format) {
        return genererReponseRapport("rapport_tests", "Rapport de synthèse — Tests académiques",
                construireRapportTests(), format);
    }

    @GetMapping("/rapport-entretiens")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<byte[]> rapportEntretiens(@RequestParam(defaultValue = "pdf") String format) {
        return genererReponseRapport("rapport_entretiens", "Rapport de synthèse — Entretiens",
                construireRapportEntretiens(), format);
    }

    /** Contenu d'un rapport : les lignes imprimables (PDF/Excel) + la série numérique du seul graphique (Excel natif). */
    private record DonneesRapport(List<LigneRapport> lignes, String titreGraphique,
            List<String> categoriesGraphique, List<Double> valeursGraphique, String legendeGraphique) {
    }

    /** PDF (texte + indicateurs) ou Excel (mêmes lignes + graphique natif) — même contenu, deux mises en forme. */
    private ResponseEntity<byte[]> genererReponseRapport(String nomFichier, String titre, DonneesRapport donnees,
            String format) {
        byte[] fichier;
        String nomComplet;
        MediaType type;
        if ("xlsx".equalsIgnoreCase(format)) {
            List<String> entetes = List.of("Section", "Indicateur", "Valeur");
            List<List<Object>> lignes = donnees.lignes().stream()
                    .map(l -> List.<Object>of(l.section(), l.libelle(), l.valeur()))
                    .toList();
            fichier = excelExportService.genererClasseur(titre, entetes, lignes,
                    donnees.titreGraphique(), donnees.categoriesGraphique(), donnees.valeursGraphique(),
                    donnees.legendeGraphique());
            nomComplet = nomFichier + ".xlsx";
            type = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } else {
            fichier = rapportPdfService.genererRapport(titre, donnees.lignes());
            nomComplet = nomFichier + ".pdf";
            type = MediaType.APPLICATION_PDF;
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + nomComplet)
                .contentType(type)
                .body(fichier);
    }

    private LigneRapport ligne(String section, String libelle, String valeur) {
        return new LigneRapport(section, libelle, valeur);
    }

    private String compteEtPourcentage(long compte, long total) {
        return compte + " (" + Math.round(pourcentage(compte, total)) + "%)";
    }

    // ===================== Rapport Candidatures =====================

    private DonneesRapport construireRapportCandidatures() {
        List<Candidature> all = candidatureRepo.findAll();
        long total = all.size();
        Map<String, Object> kpis = calculerKpis();

        List<LigneRapport> lignes = new ArrayList<>();
        lignes.add(ligne("Indicateurs clés", "Total candidatures", String.valueOf(total)));
        lignes.add(ligne("Indicateurs clés", "Taux d'admission", kpis.get("tauxAdmission") + "%"));
        lignes.add(ligne("Indicateurs clés", "Frais collectés", kpis.get("fraisCollectes") + " FCFA"));
        lignes.add(ligne("Indicateurs clés", "Temps de traitement moyen", kpis.get("tempsTraitement") + " j"));

        List<String> statutsLabels = List.of("Soumis", "Présélectionné", "Test réussi", "Entretien effectué",
                "Admis", "Non admis");
        List<EnumStatutCandidature> statuts = List.of(EnumStatutCandidature.SOUMIS,
                EnumStatutCandidature.PRESELECTIONNE, EnumStatutCandidature.TEST_REUSSI,
                EnumStatutCandidature.ENTRETIEN_EFFECTUE, EnumStatutCandidature.ADMIS,
                EnumStatutCandidature.NON_ADMIS);
        List<Double> statutsValeurs = new ArrayList<>();
        for (int i = 0; i < statuts.size(); i++) {
            EnumStatutCandidature statutCourant = statuts.get(i);
            long compte = all.stream().filter(c -> c.getStatut() == statutCourant).count();
            statutsValeurs.add((double) compte);
            lignes.add(ligne("Répartition par statut", statutsLabels.get(i), compteEtPourcentage(compte, total)));
        }

        ajouterDemographie(lignes, all, total);

        return new DonneesRapport(lignes, "Répartition par statut", statutsLabels, statutsValeurs, "Nb candidatures");
    }

    /** Mêmes quatre axes démographiques que la page Statistiques (genre, âge, profession, nationalité). */
    private void ajouterDemographie(List<LigneRapport> lignes, List<Candidature> all, long total) {
        Map<String, Long> parGenre = all.stream()
                .map(c -> c.getCandidat() != null ? c.getCandidat().getGenre() : null)
                .collect(Collectors.groupingBy(g -> g == null || g.isBlank() ? "Non renseigné" : g, Collectors.counting()));
        parGenre.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> lignes.add(ligne("Démographie — Genre", e.getKey(), compteEtPourcentage(e.getValue(), total))));

        LocalDate maintenant = LocalDate.now();
        List<int[]> tranches = List.of(new int[] { 0, 19 }, new int[] { 20, 25 }, new int[] { 26, 30 },
                new int[] { 31, 40 }, new int[] { 41, 999 });
        List<String> tranchesLabels = List.of("< 20 ans", "20-25 ans", "26-30 ans", "31-40 ans", "> 40 ans");
        List<Integer> ages = all.stream()
                .map(c -> c.getCandidat() != null ? c.getCandidat().getDateNaissance() : null)
                .filter(Objects::nonNull)
                .map(naissance -> Period.between(naissance, maintenant).getYears())
                .toList();
        for (int i = 0; i < tranches.size(); i++) {
            int[] bornes = tranches.get(i);
            long compte = ages.stream().filter(age -> age >= bornes[0] && age <= bornes[1]).count();
            lignes.add(ligne("Démographie — Tranche d'âge", tranchesLabels.get(i), compteEtPourcentage(compte, total)));
        }

        Map<String, Long> parProfession = all.stream()
                .map(c -> c.getCandidat() != null ? c.getCandidat().getProfession() : null)
                .filter(p -> p != null && !p.isBlank())
                .collect(Collectors.groupingBy(p -> p.trim().toLowerCase(Locale.FRENCH), Collectors.counting()));
        parProfession.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(6)
                .forEach(e -> lignes.add(
                        ligne("Démographie — Professions", capitaliser(e.getKey()), compteEtPourcentage(e.getValue(), total))));

        Map<String, Long> parNationalite = all.stream()
                .map(c -> c.getCandidat() != null ? c.getCandidat().getNationalite() : null)
                .filter(n -> n != null && !n.isBlank())
                .collect(Collectors.groupingBy(n -> n, Collectors.counting()));
        parNationalite.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(6)
                .forEach(e -> lignes.add(
                        ligne("Démographie — Nationalités", e.getKey(), compteEtPourcentage(e.getValue(), total))));
    }

    // ===================== Rapport Tests =====================

    private DonneesRapport construireRapportTests() {
        List<TestAcademique> tests = testAcademiqueRepo.findAll();
        Map<Long, Candidature> candidatureParCandidat = candidatureRepo.findAll().stream()
                .filter(c -> c.getCandidat() != null)
                .collect(Collectors.toMap(c -> c.getCandidat().getId(), c -> c, (a, b) -> a));

        long total = tests.size();
        long notes = tests.stream().filter(t -> t.getNoteObtenue() != null).count();
        long enAttente = total - notes;
        long admissibles = tests.stream()
                .filter(t -> t.getNoteObtenue() != null && t.getNoteObtenue() >= seuilPour(t, candidatureParCandidat))
                .count();
        long insuffisants = notes - admissibles;
        double moyenne = tests.stream().filter(t -> t.getNoteObtenue() != null)
                .mapToDouble(TestAcademique::getNoteObtenue).average().orElse(0.0);

        List<LigneRapport> lignes = new ArrayList<>();
        lignes.add(ligne("Indicateurs clés", "Total tests programmés", String.valueOf(total)));
        lignes.add(ligne("Indicateurs clés", "Notés", String.valueOf(notes)));
        lignes.add(ligne("Indicateurs clés", "En attente de note", String.valueOf(enAttente)));
        lignes.add(ligne("Indicateurs clés", "Moyenne des notes obtenues",
                notes > 0 ? String.format(Locale.FRENCH, "%.1f/20", moyenne) : "—"));

        List<String> labels = List.of("Admissible", "Insuffisant", "En attente");
        List<Double> valeurs = List.of((double) admissibles, (double) insuffisants, (double) enAttente);
        for (int i = 0; i < labels.size(); i++) {
            lignes.add(ligne("Répartition par résultat", labels.get(i), compteEtPourcentage(valeurs.get(i).longValue(), total)));
        }

        Map<String, List<TestAcademique>> parSession = tests.stream()
                .collect(Collectors.groupingBy(t -> {
                    Candidature c = candidatureParCandidat.get(t.getCandidat().getId());
                    return c != null && c.getSessionCandidature() != null ? c.getSessionCandidature().getLibelle()
                            : "Session inconnue";
                }));
        parSession.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    double moyenneSession = e.getValue().stream().filter(t -> t.getNoteObtenue() != null)
                            .mapToDouble(TestAcademique::getNoteObtenue).average().orElse(0.0);
                    lignes.add(ligne("Répartition par session", e.getKey(),
                            e.getValue().size() + " test(s), moyenne " + String.format(Locale.FRENCH, "%.1f/20", moyenneSession)));
                });

        return new DonneesRapport(lignes, "Répartition par résultat", labels, valeurs, "Nb tests");
    }

    /** Seuil de réussite (note/20) de la session du candidat de ce test — même règle que le frontend (page Tests). */
    private float seuilPour(TestAcademique test, Map<Long, Candidature> candidatureParCandidat) {
        Candidature candidature = candidatureParCandidat.get(test.getCandidat().getId());
        SessionCandidature session = candidature != null ? candidature.getSessionCandidature() : null;
        return session != null ? session.getSeuilReussiteTest() : SEUIL_ADMISSIBILITE_DEFAUT;
    }

    // ===================== Rapport Entretiens =====================

    private DonneesRapport construireRapportEntretiens() {
        List<Entretien> entretiens = entretienRepo.findAll();
        LocalDateTime maintenant = LocalDateTime.now();

        long total = entretiens.size();
        long effectues = entretiens.stream().filter(e -> e.getDateHeure().isBefore(maintenant)).count();
        long aVenir = total - effectues;
        long aujourdhui = entretiens.stream()
                .filter(e -> e.getDateHeure().toLocalDate().equals(maintenant.toLocalDate()))
                .count();
        long confirmes = entretiens.stream().filter(Entretien::isConfirmeParCandidat).count();
        long notes = entretiens.stream().filter(e -> e.getNoteEntretien() != null).count();
        double moyenne = entretiens.stream().filter(e -> e.getNoteEntretien() != null)
                .mapToDouble(Entretien::getNoteEntretien).average().orElse(0.0);

        List<LigneRapport> lignes = new ArrayList<>();
        lignes.add(ligne("Indicateurs clés", "Total entretiens", String.valueOf(total)));
        lignes.add(ligne("Indicateurs clés", "Aujourd'hui", String.valueOf(aujourdhui)));
        lignes.add(ligne("Indicateurs clés", "Confirmés par le candidat", compteEtPourcentage(confirmes, total)));
        lignes.add(ligne("Indicateurs clés", "Moyenne des notes", notes > 0 ? String.format(Locale.FRENCH, "%.1f/20", moyenne) : "—"));

        List<String> labels = List.of("Effectués", "À venir");
        List<Double> valeurs = List.of((double) effectues, (double) aVenir);
        for (int i = 0; i < labels.size(); i++) {
            lignes.add(ligne("Répartition par état", labels.get(i), compteEtPourcentage(valeurs.get(i).longValue(), total)));
        }

        return new DonneesRapport(lignes, "Répartition par état", labels, valeurs, "Nb entretiens");
    }
}
