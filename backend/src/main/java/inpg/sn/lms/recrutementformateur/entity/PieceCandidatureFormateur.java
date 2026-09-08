package inpg.sn.lms.recrutementformateur.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import inpg.sn.lms.recrutement.entity.FichierStocke;
import inpg.sn.lms.recrutementformateur.enums.EnumTypeDocumentCandidatureFormateur;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Pièce jointe d'une {@link CandidatureFormateur}. Réutilise
 * {@link FichierStocke} du module recrutement, avec {@code cascade = ALL} +
 * {@code orphanRemoval} sur le fichier (le fichier appartient à la pièce ;
 * sans cascade, l'enregistrement échoue en {@code TransientPropertyValueException}
 * — piège rencontré côté accréditation).
 */
@Entity
@Table(name = "pieces_candidature_formateur")
@Getter
@Setter
@NoArgsConstructor
public class PieceCandidatureFormateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidature_id", nullable = false)
    @JsonIgnore
    private CandidatureFormateur candidature;

    @Enumerated(EnumType.STRING)
    @jakarta.persistence.Column(name = "type_document", nullable = false, length = 40)
    private EnumTypeDocumentCandidatureFormateur typeDocument;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "fichier_stocke_id")
    private FichierStocke fichierStocke;

    public PieceCandidatureFormateur(CandidatureFormateur candidature,
            EnumTypeDocumentCandidatureFormateur typeDocument, FichierStocke fichierStocke) {
        this.candidature = candidature;
        this.typeDocument = typeDocument;
        this.fichierStocke = fichierStocke;
    }
}
