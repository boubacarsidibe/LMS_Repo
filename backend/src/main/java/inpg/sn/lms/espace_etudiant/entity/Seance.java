package inpg.sn.lms.espace_etudiant.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Seance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private LocalDateTime dateHeure;

    @Column(nullable = false)
    private Integer dureeMinutes;

    private String lieuOuLien;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cours_id")
    private Cours cours;

    public Long getId() { return id; }
    public String getTitre() { return titre; }
    public String getType() { return type; }
    public LocalDateTime getDateHeure() { return dateHeure; }
    public Integer getDureeMinutes() { return dureeMinutes; }
    public String getLieuOuLien() { return lieuOuLien; }
    public Cours getCours() { return cours; }
    public void setTitre(String titre) { this.titre = titre; }
    public void setType(String type) { this.type = type; }
    public void setDateHeure(LocalDateTime dateHeure) { this.dateHeure = dateHeure; }
    public void setDureeMinutes(Integer dureeMinutes) { this.dureeMinutes = dureeMinutes; }
    public void setLieuOuLien(String lieuOuLien) { this.lieuOuLien = lieuOuLien; }
    public void setCours(Cours cours) { this.cours = cours; }
}