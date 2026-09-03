package inpg.sn.lms.espace_etudiant.entity;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import inpg.sn.lms.auth.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

/**
 * Programme de formation de l'INPG (ex. « Master Pétrole & Gaz »).
 *
 * <p>C'est le pivot du cursus : une {@link Promotion} suit une formation, et une
 * formation regroupe les {@link Cours} qui la composent. La chaîne
 * Etudiant → Promotion → Formation → Cours est ce qui permet de servir
 * « les cours autorisés » du cahier des charges plutôt que le catalogue complet.
 */
@Entity
public class Formation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String intitule;

    @Column(columnDefinition = "text")
    private String description;

    // ===== Vitrine publique =====
    // Ces champs alimentent le catalogue du site : ce sont ceux sur lesquels le
    // visiteur filtre, donc les seuls à mériter des colonnes propres.

    private String niveau;

    private String domaine;

    private String duree;

    @Column(length = 1000)
    private String imageUrl;

    /** Seules les formations publiées apparaissent sur le site public. */
    @Column(columnDefinition = "boolean default true")
    private boolean publiee = true;

    /**
     * Contenu éditorial de la fiche détaillée, stocké en JSON : modules, méthode
     * pédagogique, points clés, débouchés, encadré latéral.
     *
     * <p>Le modéliser en tables relationnelles serait disproportionné — c'est de
     * la copie marketing, jamais interrogée par champ, toujours lue en bloc.
     */
    @Column(columnDefinition = "text")
    private String ficheDetaillee;

    // ===== Conformité au §3 du Référentiel d'accréditation DRCQ =====
    // Heures de simulation en laboratoire INPG (§3.1 : 20 h technicien / 70 h ingénieur).
    @Column(name = "heures_simulation_labo")
    private Integer heuresSimulationLabo;

    /** Le programme intègre un enseignement d'anglais professionnel (§3.4). */
    @Column(name = "anglais_professionnel", columnDefinition = "boolean default false")
    private boolean anglaisProfessionnel = false;

    /** Le programme développe des compétences transversales / soft skills (§3.5). */
    @Column(name = "soft_skills", columnDefinition = "boolean default false")
    private boolean softSkills = false;

    /** Nombre total de crédits pour l'obtention du diplôme (§3.6). */
    @Column(name = "credits_totaux")
    private Integer creditsTotaux;

    /** Volume horaire total du programme, en heures (§3.6). */
    @Column(name = "volume_horaire_total")
    private Integer volumeHoraireTotal;

    /**
     * Cours au programme de la formation. Formation est le côté propriétaire :
     * c'est elle qui écrit dans la table de jointure {@code formation_cours}.
     */
    @ManyToMany
    @JoinTable(name = "formation_cours", joinColumns = @JoinColumn(name = "formation_id"), inverseJoinColumns = @JoinColumn(name = "cours_id"))
    @JsonIgnore
    private Set<Cours> cours = new HashSet<>();

    @ManyToMany(mappedBy = "favorites")
    @JsonIgnore
    private Set<User> favoritedBy = new HashSet<>();

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getIntitule() {
        return intitule;
    }

    public void setIntitule(String intitule) {
        this.intitule = intitule;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }

    public String getDomaine() {
        return domaine;
    }

    public void setDomaine(String domaine) {
        this.domaine = domaine;
    }

    public String getDuree() {
        return duree;
    }

    public void setDuree(String duree) {
        this.duree = duree;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isPubliee() {
        return publiee;
    }

    public void setPubliee(boolean publiee) {
        this.publiee = publiee;
    }

    public String getFicheDetaillee() {
        return ficheDetaillee;
    }

    public void setFicheDetaillee(String ficheDetaillee) {
        this.ficheDetaillee = ficheDetaillee;
    }

    public Integer getHeuresSimulationLabo() {
        return heuresSimulationLabo;
    }

    public void setHeuresSimulationLabo(Integer heuresSimulationLabo) {
        this.heuresSimulationLabo = heuresSimulationLabo;
    }

    public boolean isAnglaisProfessionnel() {
        return anglaisProfessionnel;
    }

    public void setAnglaisProfessionnel(boolean anglaisProfessionnel) {
        this.anglaisProfessionnel = anglaisProfessionnel;
    }

    public boolean isSoftSkills() {
        return softSkills;
    }

    public void setSoftSkills(boolean softSkills) {
        this.softSkills = softSkills;
    }

    public Integer getCreditsTotaux() {
        return creditsTotaux;
    }

    public void setCreditsTotaux(Integer creditsTotaux) {
        this.creditsTotaux = creditsTotaux;
    }

    public Integer getVolumeHoraireTotal() {
        return volumeHoraireTotal;
    }

    public void setVolumeHoraireTotal(Integer volumeHoraireTotal) {
        this.volumeHoraireTotal = volumeHoraireTotal;
    }

    public Set<Cours> getCours() {
        return cours;
    }

    public void setCours(Set<Cours> cours) {
        this.cours = cours;
    }

    public Set<User> getFavoritedBy() {
        return favoritedBy;
    }

    public void setFavoritedBy(Set<User> favoritedBy) {
        this.favoritedBy = favoritedBy;
    }
}
