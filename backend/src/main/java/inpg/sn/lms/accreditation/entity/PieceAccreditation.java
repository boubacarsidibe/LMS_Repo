package inpg.sn.lms.accreditation.entity;

import inpg.sn.lms.accreditation.enums.EnumTypeDocumentAccreditation;
import inpg.sn.lms.recrutement.entity.FichierStocke;
import jakarta.persistence.CascadeType;
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

/**
 * Pièce jointe d'un dossier d'accréditation.
 *
 * <p>Réutilise {@link FichierStocke} (module recrutement) pour le fichier
 * physique, exactement comme {@code PieceJustificative} le fait déjà pour un
 * dossier de candidature — un seul mécanisme de stockage de fichiers, pas un
 * second par module.
 */
@Entity
@Table(name = "pieces_accreditation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PieceAccreditation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_document", nullable = false, length = 40)
    private EnumTypeDocumentAccreditation typeDocument;

    @ManyToOne
    @JoinColumn(name = "demande_id", nullable = false)
    private DemandeAccreditation demande;

    /**
     * Le fichier physique appartient à la pièce : sa ligne {@code FichierStocke}
     * est créée par le storage sans être persistée — c'est cette cascade qui
     * l'enregistre au {@code save} de la pièce (sinon
     * {@code TransientPropertyValueException} au flush). {@code orphanRemoval}
     * fait suivre la suppression.
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "fichier_stocke_id")
    private FichierStocke fichierStocke;

    public PieceAccreditation(EnumTypeDocumentAccreditation typeDocument, DemandeAccreditation demande,
            FichierStocke fichierStocke) {
        this.typeDocument = typeDocument;
        this.demande = demande;
        this.fichierStocke = fichierStocke;
    }
}
