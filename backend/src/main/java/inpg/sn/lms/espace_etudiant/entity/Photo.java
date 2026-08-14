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
public class Photo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false)
    private String nomFichier;

    @Column(nullable = false)
    private String typeContenu;

    @Column(nullable = false)
    private String auteurNom;

    @Column(nullable = false)
    private LocalDateTime dateAjout;

    @ManyToOne
    @JoinColumn(name = "cohorte_id")
    private Cohorte cohorte;

    public Long getId() {
        return id;
    }

    public String getTitre() {
        return titre;
    }

    public String getNomFichier() {
        return nomFichier;
    }

    public String getTypeContenu() {
        return typeContenu;
    }

    public String getAuteurNom() {
        return auteurNom;
    }

    public LocalDateTime getDateAjout() {
        return dateAjout;
    }

    public Cohorte getCohorte() {
        return cohorte;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public void setNomFichier(String nomFichier) {
        this.nomFichier = nomFichier;
    }

    public void setTypeContenu(String typeContenu) {
        this.typeContenu = typeContenu;
    }

    public void setAuteurNom(String auteurNom) {
        this.auteurNom = auteurNom;
    }

    public void setDateAjout(LocalDateTime dateAjout) {
        this.dateAjout = dateAjout;
    }

    public void setCohorte(Cohorte cohorte) {
        this.cohorte = cohorte;
    }
}
