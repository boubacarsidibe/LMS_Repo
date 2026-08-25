package inpg.sn.lms.recrutement.entity;

import java.time.LocalDateTime;

import inpg.sn.lms.auth.entity.Candidat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notifications_candidat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationCandidat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "candidat_id", nullable = false)
    private Candidat candidat;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private String type; // STATUT_CHANGE, TEST_CONVOCATION, ENTRETIEN_CONVOCATION, DOCUMENT, PAIEMENT, ADMISSION, GENERAL

    @Column(nullable = false)
    private boolean lu = false;

    @Column(nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column
    private String lien; // URL optionnelle (ex: /recrutement/suivi)

    public NotificationCandidat(Candidat candidat, String message, String type, String lien) {
        this.candidat = candidat;
        this.message = message;
        this.type = type;
        this.lien = lien;
        this.dateCreation = LocalDateTime.now();
        this.lu = false;
    }
}