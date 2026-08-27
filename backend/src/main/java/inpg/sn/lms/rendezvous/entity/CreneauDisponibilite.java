package inpg.sn.lms.rendezvous.entity;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Créneau ponctuel publié par un membre du personnel (TDR §J : « calendriers
 * individuels de chaque membre du personnel »).
 *
 * <p>Pas de moteur de récurrence hebdomadaire : le TDR ne le demande pas, et
 * l'inventer aurait ajouté une complexité (règles de récurrence, exceptions,
 * fuseaux horaires) hors de portée d'une story raisonnable — voir
 * LOG_BLOQUANT.md. Le membre du personnel publie lui-même les plages où il
 * est disponible ; {@code reserve} passe à {@code true} dès qu'une demande de
 * rendez-vous le couvre, pour qu'il cesse d'apparaître comme libre.
 */
@Entity
@Table(name = "creneaux_disponibilite")
@Getter
@Setter
@NoArgsConstructor
public class CreneauDisponibilite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personnel_id", nullable = false)
    private User personnel;

    @Column(name = "date_heure_debut", nullable = false)
    private LocalDateTime dateHeureDebut;

    @Column(name = "date_heure_fin", nullable = false)
    private LocalDateTime dateHeureFin;

    @Column(nullable = false)
    private boolean reserve = false;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    public CreneauDisponibilite(User personnel, LocalDateTime dateHeureDebut, LocalDateTime dateHeureFin) {
        this.personnel = personnel;
        this.dateHeureDebut = dateHeureDebut;
        this.dateHeureFin = dateHeureFin;
    }
}
