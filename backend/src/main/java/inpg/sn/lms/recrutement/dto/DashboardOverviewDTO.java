package inpg.sn.lms.recrutement.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO agrégé renvoyé par GET /api/dashboard/overview.
 */
public class DashboardOverviewDTO {

    // Session active
    private SessionSummaryDTO sessionActive;

    // KPIs
    private long totalCandidatures;
    private long candidaturesValidees;
    private long candidaturesEnAttente;
    private long candidaturesRejetees;

    // Candidatures récentes
    private List<CandidatureResumeDTO> candidaturesRecentes;

    // Tests académiques
    private List<TestSummaryDTO> testsAcademiques;

    // Entretiens
    private List<EntretienSummaryDTO> entretiens;

    // ---- Getters / Setters ----

    public SessionSummaryDTO getSessionActive() { return sessionActive; }
    public void setSessionActive(SessionSummaryDTO sessionActive) { this.sessionActive = sessionActive; }

    public long getTotalCandidatures() { return totalCandidatures; }
    public void setTotalCandidatures(long totalCandidatures) { this.totalCandidatures = totalCandidatures; }

    public long getCandidaturesValidees() { return candidaturesValidees; }
    public void setCandidaturesValidees(long candidaturesValidees) { this.candidaturesValidees = candidaturesValidees; }

    public long getCandidaturesEnAttente() { return candidaturesEnAttente; }
    public void setCandidaturesEnAttente(long candidaturesEnAttente) { this.candidaturesEnAttente = candidaturesEnAttente; }

    public long getCandidaturesRejetees() { return candidaturesRejetees; }
    public void setCandidaturesRejetees(long candidaturesRejetees) { this.candidaturesRejetees = candidaturesRejetees; }

    public List<CandidatureResumeDTO> getCandidaturesRecentes() { return candidaturesRecentes; }
    public void setCandidaturesRecentes(List<CandidatureResumeDTO> candidaturesRecentes) { this.candidaturesRecentes = candidaturesRecentes; }

    public List<TestSummaryDTO> getTestsAcademiques() { return testsAcademiques; }
    public void setTestsAcademiques(List<TestSummaryDTO> testsAcademiques) { this.testsAcademiques = testsAcademiques; }

    public List<EntretienSummaryDTO> getEntretiens() { return entretiens; }
    public void setEntretiens(List<EntretienSummaryDTO> entretiens) { this.entretiens = entretiens; }

    // ---- Inner DTOs ----

    public static class SessionSummaryDTO {
        private Long id;
        private String libelle;
        private boolean estOuverte;
        private LocalDate dateOuverture;
        private LocalDate dateFermeture;
        private int quotaMax;
        private int quotaActuel;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getLibelle() { return libelle; }
        public void setLibelle(String libelle) { this.libelle = libelle; }
        public boolean isEstOuverte() { return estOuverte; }
        public void setEstOuverte(boolean estOuverte) { this.estOuverte = estOuverte; }
        public LocalDate getDateOuverture() { return dateOuverture; }
        public void setDateOuverture(LocalDate dateOuverture) { this.dateOuverture = dateOuverture; }
        public LocalDate getDateFermeture() { return dateFermeture; }
        public void setDateFermeture(LocalDate dateFermeture) { this.dateFermeture = dateFermeture; }
        public int getQuotaMax() { return quotaMax; }
        public void setQuotaMax(int quotaMax) { this.quotaMax = quotaMax; }
        public int getQuotaActuel() { return quotaActuel; }
        public void setQuotaActuel(int quotaActuel) { this.quotaActuel = quotaActuel; }
    }

    public static class CandidatureResumeDTO {
        private Long id;
        private String numeroDossier;
        private String nomCandidat;
        private String prenomCandidat;
        private String email;
        private String statut;
        private String formation;
        private LocalDate dateSoumission;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getNumeroDossier() { return numeroDossier; }
        public void setNumeroDossier(String numeroDossier) { this.numeroDossier = numeroDossier; }
        public String getNomCandidat() { return nomCandidat; }
        public void setNomCandidat(String nomCandidat) { this.nomCandidat = nomCandidat; }
        public String getPrenomCandidat() { return prenomCandidat; }
        public void setPrenomCandidat(String prenomCandidat) { this.prenomCandidat = prenomCandidat; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getStatut() { return statut; }
        public void setStatut(String statut) { this.statut = statut; }
        public String getFormation() { return formation; }
        public void setFormation(String formation) { this.formation = formation; }
        public LocalDate getDateSoumission() { return dateSoumission; }
        public void setDateSoumission(LocalDate dateSoumission) { this.dateSoumission = dateSoumission; }
    }

    public static class TestSummaryDTO {
        private Long id;
        private String nomEpreuve;
        private LocalDate dateEpreuve;
        private Long nombreCandidats;
        private int seuilReussite;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getNomEpreuve() { return nomEpreuve; }
        public void setNomEpreuve(String nomEpreuve) { this.nomEpreuve = nomEpreuve; }
        public LocalDate getDateEpreuve() { return dateEpreuve; }
        public void setDateEpreuve(LocalDate dateEpreuve) { this.dateEpreuve = dateEpreuve; }
        public Long getNombreCandidats() { return nombreCandidats; }
        public void setNombreCandidats(Long nombreCandidats) { this.nombreCandidats = nombreCandidats; }
        public int getSeuilReussite() { return seuilReussite; }
        public void setSeuilReussite(int seuilReussite) { this.seuilReussite = seuilReussite; }
    }

    public static class EntretienSummaryDTO {
        private Long id;
        private String commission;
        private String salle;
        private String jury;
        private LocalDate dateEntretien;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getCommission() { return commission; }
        public void setCommission(String commission) { this.commission = commission; }
        public String getSalle() { return salle; }
        public void setSalle(String salle) { this.salle = salle; }
        public String getJury() { return jury; }
        public void setJury(String jury) { this.jury = jury; }
        public LocalDate getDateEntretien() { return dateEntretien; }
        public void setDateEntretien(LocalDate dateEntretien) { this.dateEntretien = dateEntretien; }
    }
}