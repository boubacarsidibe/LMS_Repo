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
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String auteurNom;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenu;

    @Column(nullable = false)
    private LocalDateTime dateCreation;

    @ManyToOne
    @JoinColumn(name = "sujet_id", nullable = false)
    private Sujet sujet;

    /** Nom original du fichier, affiche a l'utilisateur (ex: "capture.png"). Null si pas de piece jointe. */
    @Column
    private String pieceJointeNomOriginal;

    /** Nom du fichier tel que stocke sur le disque (prefixe UUID), utilise pour retrouver le fichier au telechargement. */
    @Column
    private String pieceJointeNomStocke;

    /** Content-Type du fichier, reutilise pour la reponse HTTP au telechargement. */
    @Column
    private String pieceJointeTypeContenu;

    public Long getId() {
        return id;
    }

    public String getAuteurNom() {
        return auteurNom;
    }

    public String getContenu() {
        return contenu;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public Sujet getSujet() {
        return sujet;
    }

    public String getPieceJointeNomOriginal() {
        return pieceJointeNomOriginal;
    }

    public String getPieceJointeNomStocke() {
        return pieceJointeNomStocke;
    }

    public String getPieceJointeTypeContenu() {
        return pieceJointeTypeContenu;
    }

    public void setAuteurNom(String auteurNom) {
        this.auteurNom = auteurNom;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public void setSujet(Sujet sujet) {
        this.sujet = sujet;
    }

    public void setPieceJointeNomOriginal(String pieceJointeNomOriginal) {
        this.pieceJointeNomOriginal = pieceJointeNomOriginal;
    }

    public void setPieceJointeNomStocke(String pieceJointeNomStocke) {
        this.pieceJointeNomStocke = pieceJointeNomStocke;
    }

    public void setPieceJointeTypeContenu(String pieceJointeTypeContenu) {
        this.pieceJointeTypeContenu = pieceJointeTypeContenu;
    }
}
