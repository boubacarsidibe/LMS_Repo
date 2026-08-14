package inpg.sn.lms.espace_etudiant.entity;

import java.time.LocalDate;

import inpg.sn.lms.espace_etudiant.enums.EnumTypeEvenement;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Evenement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String heure;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EnumTypeEvenement type;

    private String lienVisio;

    public Long getId() {
        return id;
    }

    public String getTitre() {
        return titre;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getHeure() {
        return heure;
    }

    public EnumTypeEvenement getType() {
        return type;
    }

    public String getLienVisio() {
        return lienVisio;
    }

    public void setLienVisio(String lienVisio) {
        this.lienVisio = lienVisio;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setHeure(String heure) {
        this.heure = heure;
    }

    public void setType(EnumTypeEvenement type) {
        this.type = type;
    }
}
