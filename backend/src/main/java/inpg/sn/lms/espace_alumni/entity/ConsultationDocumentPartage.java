package inpg.sn.lms.espace_alumni.entity;

import java.time.LocalDateTime;

import inpg.sn.lms.auth.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Trace de consultation d'un {@link DocumentPartage} par son lecteur, migration V29.
 *
 * <p>Même patron que {@code ProgressionSupport} (suivi des apprentissages, §3.5) :
 * une ligne posée à la première ouverture, jamais mise à jour ensuite — un document
 * est consulté ou il ne l'est pas, la date qui compte est la découverte. Comble un
 * écart signalé lors de l'epic « profil alumni enrichi » : les métriques du profil
 * ne comptaient que les documents déposés, faute de tout suivi de lecture.
 */
@Entity
@Table(name = "consultations_document_partage", uniqueConstraints = @UniqueConstraint(columnNames = { "document_id",
        "lecteur_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationDocumentPartage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private DocumentPartage document;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lecteur_id", nullable = false)
    private User lecteur;

    @Column(name = "date_consultation", nullable = false)
    private LocalDateTime dateConsultation = LocalDateTime.now();

    public ConsultationDocumentPartage(DocumentPartage document, User lecteur) {
        this.document = document;
        this.lecteur = lecteur;
        this.dateConsultation = LocalDateTime.now();
    }
}
