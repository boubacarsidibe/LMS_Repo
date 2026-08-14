package inpg.sn.lms.espace_etudiant.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

@Entity
public class Annonce {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenu;

    @Column(nullable = false)
    private String auteurNom;

    @Column(nullable = false)
    private LocalDateTime dateCreation;

    @Column(nullable = false)
    private boolean epinglee;

    // Cohortes ciblees par l'annonce. Vide = annonce diffusee a tout le monde
    // (institutionnelle), non vide = visible seulement par les etudiants de ces
    // cohortes et les formateurs qui les enseignent.
    @ManyToMany
    @JoinTable(
        name = "annonce_cohorte",
        joinColumns = @JoinColumn(name = "annonce_id"),
        inverseJoinColumns = @JoinColumn(name = "cohorte_id")
    )
    private List<Cohorte> cohortes = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public String getTitre() {
        return titre;
    }

    public String getContenu() {
        return contenu;
    }

    public String getAuteurNom() {
        return auteurNom;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public boolean isEpinglee() {
        return epinglee;
    }

    public List<Cohorte> getCohortes() {
        return cohortes;
    }

    public void setCohortes(List<Cohorte> cohortes) {
        this.cohortes = cohortes;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public void setAuteurNom(String auteurNom) {
        this.auteurNom = auteurNom;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public void setEpinglee(boolean epinglee) {
        this.epinglee = epinglee;
    }
}
