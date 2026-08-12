package inpg.sn.lms.espace_etudiant.entity;

import java.util.ArrayList;
import java.util.List;

import inpg.sn.lms.auth.entity.Formateur;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

@Entity
public class Cours {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Long volumeHoraire;

    private String semestre;

    @ManyToMany
    @JoinTable(
        name = "cours_formateur", // Name of the join table in DB
        joinColumns = @JoinColumn(name = "cours_id"),
        inverseJoinColumns = @JoinColumn(name = "formateur_id")
    )
    private List<Formateur> formateurs = new ArrayList<>();



    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public String getTitre() {
        return titre;
    }

    public String getDescription() {
        return description;
    }

    public Long getVolumeHoraire() {
        return volumeHoraire;
    }

    public String getSemestre() {
        return semestre;
    }

    public List<Formateur> getFormateurs() {
        return formateurs;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setVolumeHoraire(Long volumeHoraire) {
        this.volumeHoraire = volumeHoraire;
    }

    public void setSemestre(String semestre) {
        this.semestre = semestre;
    }

}
