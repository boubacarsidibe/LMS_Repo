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
    @JoinColumn(name = "cohorte_id")
    private Cohorte cohorte;

    @Column(nullable = false)
    private String intituleFormation;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(nullable = false)
    private LocalDate dateEmission;

    @Column(nullable = false)
    private String formateurNom;

    public Long getId() {
        return id;
    }

    public Etudiant getEtudiant() {
        return etudiant;
    }

    public Cohorte getCohorte() {
        return cohorte;
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

    public void setEtudiant(Etudiant etudiant) {
        this.etudiant = etudiant;
    }

    public void setCohorte(Cohorte cohorte) {
        this.cohorte = cohorte;
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
        if (reference == null || reference.isBlank()) {
            String suffixe = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            reference = "INPG-CERT-" + Year.now().getValue() + "-" + suffixe;
        }
        if (dateEmission == null) {
            dateEmission = LocalDate.now();
        }
    }
}
