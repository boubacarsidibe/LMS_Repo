package inpg.sn.lms.recrutement.controller;

import java.time.LocalDateTime;
import java.time.Month;
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
import inpg.sn.lms.recrutement.entity.Paiement;
import inpg.sn.lms.recrutement.enums.EnumStatutCandidature;
import inpg.sn.lms.recrutement.enums.EnumStatutPaiement;
import inpg.sn.lms.recrutement.repository.CandidatureRepository;
import inpg.sn.lms.recrutement.repository.PaiementRepository;
import inpg.sn.lms.recrutement.repository.EntretienRepository;

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

    private final CandidatureRepository candidatureRepo;
    private final PaiementRepository paiementRepo;
    private final EntretienRepository entretienRepo;

    public AnalyticsController(CandidatureRepository candidatureRepo,
                                PaiementRepository paiementRepo,
                                EntretienRepository entretienRepo) {
        this.candidatureRepo = candidatureRepo;
        this.paiementRepo = paiementRepo;
        this.entretienRepo = entretienRepo;
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

        long admis = all.stream().filter(c -> c.getStatut() == EnumStatutCandidature.ADMIS3
                || c.getStatut() == EnumStatutCandidature.ADMIS2).count();
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
                    .filter(c -> c.getStatut() == EnumStatutCandidature.ADMIS1
                            || c.getStatut() == EnumStatutCandidature.ADMIS2
                            || c.getStatut() == EnumStatutCandidature.ADMIS3)
                    .count();
            long rejetees = duMois.stream().filter(c -> c.getStatut() == EnumStatutCandidature.REJETE).count();

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
        long admissibles = all.stream().filter(c -> estAuMoins(c.getStatut(), EnumStatutCandidature.ADMIS1)).count();
        long entretiens = all.stream().filter(c -> estAuMoins(c.getStatut(), EnumStatutCandidature.ADMIS2)).count();
        long inscrits = all.stream().filter(c -> c.getStatut() == EnumStatutCandidature.ADMIS3).count();

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
                EnumStatutCandidature.EN_COURS_DE_PRESELECTION, EnumStatutCandidature.ADMIS1,
                EnumStatutCandidature.ADMIS2, EnumStatutCandidature.ADMIS3);
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
}
