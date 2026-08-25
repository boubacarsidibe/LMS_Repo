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
 * Profil d'une école de formation candidate à l'accréditation (TDR §F).
 *
 * <p>Suit le même patron {@code @MapsId} que {@link Candidat}, {@link Etudiant}
 * et {@link AlumniProfil} : une école EST un utilisateur, pas une nouvelle
 * identité — sa clé primaire est partagée avec {@link User}. Créée à la première
 * soumission d'une demande, comme {@code Candidat} l'est à la première
 * soumission d'une candidature (voir {@code EcoleAccreditationController}).
 */
@Entity
@Table(name = "ecoles_formation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EcoleFormation {

    @Id
    private Long id;

    @Column(name = "nom_ecole", nullable = false)
    private String nomEcole;

    @Column(nullable = false)
    private String adresse;

    @Column(nullable = false)
    private String telephone;

    @Column(name = "site_web")
    private String siteWeb;

    @Column(name = "date_creation", nullable = false)
    private LocalDate dateCreation = LocalDate.now();

    @OneToOne
    @MapsId
    @JoinColumn(name = "id_user")
    private User user;

    public EcoleFormation(User user, String nomEcole, String adresse, String telephone, String siteWeb) {
        this.user = user;
        this.nomEcole = nomEcole;
        this.adresse = adresse;
        this.telephone = telephone;
        this.siteWeb = siteWeb;
        this.dateCreation = LocalDate.now();
    }
}
