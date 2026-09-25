package inpg.sn.lms.espace_etudiant.entity;

import java.time.LocalDate;
import java.time.Year;
import java.util.UUID;

import inpg.sn.lms.auth.entity.Etudiant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;

@Entity
public class Certificat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "etudiant_id", nullable = false)
    private Etudiant etudiant;

    @ManyToOne
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    @Column(nullable = false)
    private String intituleFormation;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(nullable = false)
    private LocalDate dateEmission;

    @Column(nullable = false)
    private String formateurNom;

    /**
     * Signature electronique (HMAC-SHA256 hexadecimal) du contenu du certificat,
     * calculee a la delivrance par {@code CertificatSignatureService}. Permet a la
     * verification publique de detecter une donnee modifiee apres coup.
     */
    @Column(length = 64)
    private String signature;

    /** « Monsieur », « Madame »… : imprimée avant le nom sur le certificat au modèle INPG. */
    @Column(length = 20)
    private String civilite;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    /** Période de la formation (« Du 02 au 06 février 2026 »). */
    @Column(name = "date_debut")
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    /** Code propre au certificat (ex. CG1001260206/03), imprimé en bas de page. */
    @Column(name = "code_certificat", length = 60)
    private String codeCertificat;

    public Long getId() {
        return id;
    }

    public Etudiant getEtudiant() {
        return etudiant;
    }

    public Promotion getPromotion() {
        return promotion;
    }

    public String getIntituleFormation() {
        return intituleFormation;
    }

    public String getReference() {
        return reference;
    }

    public LocalDate getDateEmission() {
        return dateEmission;
    }

    public String getFormateurNom() {
        return formateurNom;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getCivilite() {
        return civilite;
    }

    public void setCivilite(String civilite) {
        this.civilite = civilite;
    }

    public LocalDate getDateNaissance() {
        return dateNaissance;
    }

    public void setDateNaissance(LocalDate dateNaissance) {
        this.dateNaissance = dateNaissance;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public String getCodeCertificat() {
        return codeCertificat;
    }

    public void setCodeCertificat(String codeCertificat) {
        this.codeCertificat = codeCertificat;
    }

    public void setEtudiant(Etudiant etudiant) {
        this.etudiant = etudiant;
    }

    public void setPromotion(Promotion promotion) {
        this.promotion = promotion;
    }

    public void setIntituleFormation(String intituleFormation) {
        this.intituleFormation = intituleFormation;
    }

    public void setDateEmission(LocalDate dateEmission) {
        this.dateEmission = dateEmission;
    }

    public void setFormateurNom(String formateurNom) {
        this.formateurNom = formateurNom;
    }

    @PrePersist
    private void genererReferenceSiAbsente() {
        assurerReferenceEtDate();
    }

    /**
     * Genere la reference et la date d'emission si elles sont absentes. Expose en
     * public pour que {@code CertificatService} puisse les fixer AVANT de calculer
     * la signature electronique, qui doit porter sur leur valeur definitive.
     */
    public void assurerReferenceEtDate() {
        if (reference == null || reference.isBlank()) {
            String suffixe = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            reference = "INPG-CERT-" + Year.now().getValue() + "-" + suffixe;
        }
        if (dateEmission == null) {
            dateEmission = LocalDate.now();
        }
    }
}
