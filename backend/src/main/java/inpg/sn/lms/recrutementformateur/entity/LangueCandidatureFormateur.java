package inpg.sn.lms.recrutementformateur.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import inpg.sn.lms.recrutementformateur.enums.EnumNiveauLangue;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Une compétence linguistique déclarée dans le dossier (bloc « Language
 * Skills » du portail de référence). Patron {@link PieceCandidatureFormateur} :
 * recréée en bloc à chaque dépôt.
 */
@Entity
@Table(name = "langues_candidature_formateur")
@Getter
@Setter
@NoArgsConstructor
public class LangueCandidatureFormateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidature_id", nullable = false)
    @JsonIgnore
    private CandidatureFormateur candidature;

    @Column(nullable = false, length = 100)
    private String langue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnumNiveauLangue niveau;

    public LangueCandidatureFormateur(CandidatureFormateur candidature, String langue, EnumNiveauLangue niveau) {
        this.candidature = candidature;
        this.langue = langue;
        this.niveau = niveau;
    }
}
