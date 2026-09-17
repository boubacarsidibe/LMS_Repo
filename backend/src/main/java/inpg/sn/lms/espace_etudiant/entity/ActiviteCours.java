package inpg.sn.lms.espace_etudiant.entity;

import java.time.LocalDateTime;

import inpg.sn.lms.auth.entity.Formateur;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "activite_cours")
public class ActiviteCours {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "cours_id")
    private Cours cours;
    @ManyToOne(optional = false) @JoinColumn(name = "auteur_id")
    private Formateur auteur;
    @Column(nullable = false) private String module;
    @Column(nullable = false) private String titre;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(nullable = false) private String type;
    @Column(nullable = false) private String modalite;
    @Column(nullable = false) private LocalDateTime datePublication;
    private LocalDateTime dateLimite;
    @Column(nullable = false) private double bareme = 100;
    @Column(nullable = false) private LocalDateTime creeLe = LocalDateTime.now();
    @Column(nullable = false) private LocalDateTime modifieLe = LocalDateTime.now();
    public Long getId() { return id; }
    public Cours getCours() { return cours; }
    public Formateur getAuteur() { return auteur; }
    public String getModule() { return module; }
    public String getTitre() { return titre; }
    public String getDescription() { return description; }
    public String getType() { return type; }
    public String getModalite() { return modalite; }
    public LocalDateTime getDatePublication() { return datePublication; }
    public LocalDateTime getDateLimite() { return dateLimite; }
    public double getBareme() { return bareme; }
    public void setCours(Cours value) { cours = value; }
    public void setAuteur(Formateur value) { auteur = value; }
    public void setModule(String value) { module = value; }
    public void setTitre(String value) { titre = value; }
    public void setDescription(String value) { description = value; }
    public void setType(String value) { type = value; }
    public void setModalite(String value) { modalite = value; }
    public void setDatePublication(LocalDateTime value) { datePublication = value; }
    public void setDateLimite(LocalDateTime value) { dateLimite = value; }
    public void setBareme(double value) { bareme = value; }
    public void setModifieLe(LocalDateTime value) { modifieLe = value; }
}
