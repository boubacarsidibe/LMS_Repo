package inpg.sn.lms.recrutement.entity;

import inpg.sn.lms.recrutement.enums.EnumTypeDocument;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pieces_justificatives")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PieceJustificative {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnumTypeDocument typeDocument;

    @Column(nullable = false)
    private boolean estConforme;

    /** Moyenne declaree par le candidat pour ce releve (sur 20) : le PDF en est la preuve.
     * Nul pour tout document qui n'est pas un releve de notes. */
    @Column(name = "moyenne_declaree")
    private Double moyenneDeclaree;

    /** À quoi correspond ce document (obligatoire côté serveur pour un DIPLOME : plusieurs
     * diplômes/certificats peuvent être ajoutés librement par le candidat, rien d'autre
     * ne les distingue). */
    @Column(length = 500)
    private String description;

    @ManyToOne
    @JoinColumn(name = "candidature_id", nullable = false)
    private Candidature candidature;

    @OneToOne
    @JoinColumn(name = "fichier_stocke_id")
    private FichierStocke fichierStocke;

    public void televerser() {
        // Logique métier de téléversement gérée dans le service
    }
}