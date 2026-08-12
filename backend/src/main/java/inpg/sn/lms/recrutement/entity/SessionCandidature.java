package inpg.sn.lms.recrutement.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sessions_candidature")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SessionCandidature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String libelle;

    @Column(nullable = false)
    private LocalDate dateOuverture;

    @Column(nullable = false)
    private LocalDate dateFermeture;

    @Column(nullable = false)
    private boolean estOuverte;

    @Column(nullable = false)
    private int quotaMax = 220;

    @Column(nullable = false)
    private int quotaActuel = 0;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private String status = "BROUILLON"; // BROUILLON, OUVERT, CLOTURE, SUSPENDU

    @ManyToOne
    @JoinColumn(name = "program_id")
    private Program program;

    @OneToMany(mappedBy = "sessionCandidature", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Candidature> candidatures = new ArrayList<>();

    public boolean estSessionActive() {
        LocalDate now = LocalDate.now();
        return estOuverte
                && (dateOuverture == null || !now.isBefore(dateOuverture))
                && (dateFermeture == null || !now.isAfter(dateFermeture));
    }
}