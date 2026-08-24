package inpg.sn.lms.auth.entity;

import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Profil d'un ancien étudiant (TDR §C, « Espace Alumni »).
 *
 * <p>Suit le même patron {@code @MapsId} que {@link Candidat} et {@link Etudiant} :
 * un alumni EST un étudiant, pas une nouvelle identité — sa clé primaire est
 * partagée avec {@link User}. La ligne {@link Etudiant} correspondante n'est pas
 * supprimée à la conversion, exactement comme la ligne {@code Candidat} reste après
 * le passage à étudiant : c'est ce qui permet à l'alumni de continuer à consulter
 * son relevé de notes délivré, qui référence {@code etudiants.id_user}.
 */
@Entity
@Table(name = "alumni_profils")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AlumniProfil {

    @Id
    private Long id;

    @Column(name = "poste_actuel")
    private String posteActuel;

    private String entreprise;

    @Column(name = "secteur_activite")
    private String secteurActivite;

    /**
     * Visibilité de la fiche dans un futur annuaire alumni. Vrai par défaut :
     * refuser l'accès plutôt que le cacher serait le geste par défaut le moins
     * utile pour un réseau d'anciens qui n'existe que si on peut s'y trouver.
     */
    @Column(nullable = false)
    private boolean visible = true;

    @Column(name = "date_creation", nullable = false)
    private LocalDate dateCreation = LocalDate.now();

    @OneToOne
    @MapsId
    @JoinColumn(name = "id_user")
    private User user;

    public AlumniProfil(User user) {
        this.user = user;
        this.dateCreation = LocalDate.now();
        this.visible = true;
    }
}
