package inpg.sn.lms.espace_etudiant.entity;

import java.time.LocalDateTime;

import inpg.sn.lms.auth.entity.Etudiant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "remise_activite")
public class RemiseActivite {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "activite_id") private ActiviteCours activite;
    @ManyToOne(optional = false) @JoinColumn(name = "etudiant_id") private Etudiant etudiant;
    @Column(columnDefinition = "TEXT") private String contenu;
    @Column(nullable = false) private LocalDateTime remiseLe = LocalDateTime.now();
    private Double note;
    @Column(columnDefinition = "TEXT") private String commentaireCorrection;
    private LocalDateTime corrigeLe;
    public Long getId() { return id; }
    public ActiviteCours getActivite() { return activite; }
    public Etudiant getEtudiant() { return etudiant; }
    public String getContenu() { return contenu; }
    public LocalDateTime getRemiseLe() { return remiseLe; }
    public Double getNote() { return note; }
    public String getCommentaireCorrection() { return commentaireCorrection; }
    public LocalDateTime getCorrigeLe() { return corrigeLe; }
    public void setActivite(ActiviteCours value) { activite = value; }
    public void setEtudiant(Etudiant value) { etudiant = value; }
    public void setContenu(String value) { contenu = value; }
    public void setRemiseLe(LocalDateTime value) { remiseLe = value; }
    public void setNote(Double value) { note = value; }
    public void setCommentaireCorrection(String value) { commentaireCorrection = value; }
    public void setCorrigeLe(LocalDateTime value) { corrigeLe = value; }
}
