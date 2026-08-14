package inpg.sn.lms.espace_etudiant.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import inpg.sn.lms.espace_etudiant.enums.EnumStatutSujet;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

@Entity
public class Sujet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false)
    private String coursNom;

    @Column(nullable = false)
    private String module;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EnumStatutSujet statut;

    @Column(nullable = false)
    private String auteurNom;

    @Column(nullable = false)
    private LocalDateTime dateCreation;

    @OneToMany(mappedBy = "sujet", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dateCreation ASC")
    private List<Message> messages = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public String getTitre() {
        return titre;
    }

    public String getCoursNom() {
        return coursNom;
    }

    public String getModule() {
        return module;
    }

    public EnumStatutSujet getStatut() {
        return statut;
    }

    public String getAuteurNom() {
        return auteurNom;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public void setCoursNom(String coursNom) {
        this.coursNom = coursNom;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public void setStatut(EnumStatutSujet statut) {
        this.statut = statut;
    }

    public void setAuteurNom(String auteurNom) {
        this.auteurNom = auteurNom;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }
}
