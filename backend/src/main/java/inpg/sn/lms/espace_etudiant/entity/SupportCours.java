package inpg.sn.lms.espace_etudiant.entity;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class SupportCours {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titre;

    private String typeRessource;

    private String fichierStocke;

    private String url;

    private LocalDate datePublication;

    @ManyToOne
    @JoinColumn(name = "cours_id")
    private Cours cours;

    public Long getId() {
        return id;
    }

    public String getTitre() {
        return titre;
    }

    public String getTypeRessource() {
        return typeRessource;
    }

    public String getFichierStocke() {
        return fichierStocke;
    }

    public String getUrl() {
        return url;
    }

    public LocalDate getDatePublication() {
        return datePublication;
    }

    public Cours getCours() {
        return cours;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public void setTypeRessource(String typeRessource) {
        this.typeRessource = typeRessource;
    }

    public void setFichierStocke(String fichierStocke) {
        this.fichierStocke = fichierStocke;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setDatePublication(LocalDate datePublication) {
        this.datePublication = datePublication;
    }

    public void setCours(Cours cours) {
        this.cours = cours;
    }
}
