package inpg.sn.lms.espace_etudiant.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

    @Column(nullable = true)
    private String imageUrl;

    @ManyToMany
    @JoinTable(
        name = "cours_formateur", // Name of the join table in DB
        joinColumns = @JoinColumn(name = "cours_id"),
        inverseJoinColumns = @JoinColumn(name = "formateur_id")
    )
    private List<Formateur> formateurs = new ArrayList<>();

    /**
     * Promotions directement rattachées par un formateur (perimetre de
     * "suivi etudiants", notes, presence, ciblage d'annonces...). Coexiste avec
     * {@link #formations} : la chaine Etudiant -> Promotion -> Formation -> Cours
     * (cours "autorises" du cahier des charges) est un chemin distinct, pas encore
     * unifie avec ce rattachement manuel historique. A consolider plus tard.
     */
    @ManyToMany
    @JoinTable(
        name = "cours_promotion",
        joinColumns = @JoinColumn(name = "cours_id"),
        inverseJoinColumns = @JoinColumn(name = "promotion_id")
    )
    private Set<Promotion> promotions = new LinkedHashSet<>();

    /**
     * Formations dont ce cours fait partie. Côté inverse : la table de jointure
     * {@code formation_cours} est écrite par {@link Formation}. Sert à retrouver
     * les cours autorisés d'un étudiant via sa promotion.
     */
    @ManyToMany(mappedBy = "cours")
    @JsonIgnore
    private Set<Formation> formations = new HashSet<>();

    public Set<Formation> getFormations() {
        return formations;
    }

    public void setFormations(Set<Formation> formations) {
        this.formations = formations;
    }

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

    public Set<Promotion> getPromotions() {
        return promotions;
    }

    public void setPromotions(Set<Promotion> promotions) {
        this.promotions = promotions;
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

    public String getImageUrl() {
    return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

}
