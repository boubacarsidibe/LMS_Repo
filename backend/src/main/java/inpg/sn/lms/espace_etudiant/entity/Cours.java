package inpg.sn.lms.espace_etudiant.entity;

import java.util.ArrayList;
import java.util.HashSet;
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
     * Formations dont ce cours fait partie. Côté inverse : la table de jointure
     * {@code formation_cours} est écrite par {@link Formation}. Sert à retrouver
     * les cours autorisés d'un étudiant via sa promotion.
     *
     * <p>C'est désormais le <strong>seul</strong> rattachement d'un cours. Un champ
     * {@code promotions} coexistait ici, adossé à la table {@code cours_promotion} :
     * les deux relations répondaient à la même question et avaient divergé au point
     * de ne plus désigner aucun cours commun. Pour savoir quelles promotions suivent
     * un cours, passer par {@code PerimetreFormateurService.promotionsDuCours} — la
     * promotion se déduit de la formation, elle ne se rattache plus à la main.
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
