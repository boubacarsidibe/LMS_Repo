package inpg.sn.lms.recrutementformateur.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.LocalDate;

/**
 * Une certification déclarée dans le dossier (bloc « Certificates » du
 * portail de référence). {@code dateExpiration} est pertinente pour les
 * certifications HSE, qui expirent. Patron {@link PieceCandidatureFormateur} :
 * recréée en bloc à chaque dépôt.
 */
@Entity
@Table(name = "certifications_candidature_formateur")
@Getter
@Setter
@NoArgsConstructor
public class CertificationCandidatureFormateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidature_id", nullable = false)
    @JsonIgnore
    private CandidatureFormateur candidature;

    @Column(nullable = false, length = 200)
    private String intitule;

    @Column(length = 200)
    private String organisme;

    @Column(name = "date_obtention")
    private LocalDate dateObtention;

    @Column(name = "date_expiration")
    private LocalDate dateExpiration;

    public CertificationCandidatureFormateur(CandidatureFormateur candidature, String intitule, String organisme,
            LocalDate dateObtention, LocalDate dateExpiration) {
        this.candidature = candidature;
        this.intitule = intitule;
        this.organisme = organisme;
        this.dateObtention = dateObtention;
        this.dateExpiration = dateExpiration;
    }
}
