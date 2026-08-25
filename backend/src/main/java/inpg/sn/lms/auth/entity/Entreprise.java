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
 * Profil d'une entreprise partenaire (TDR §H — Espace Entreprise).
 *
 * <p>Suit le même patron {@code @MapsId} que {@link Candidat}, {@link Etudiant}
 * et {@link EcoleFormation} : une entreprise EST un utilisateur, pas une
 * nouvelle identité. Créée à la première soumission d'une demande (besoin de
 * formation ou devis), comme {@code EcoleFormation} l'est à la première
 * soumission d'une demande d'accréditation.
 */
@Entity
@Table(name = "entreprises")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Entreprise {

    @Id
    private Long id;

    @Column(name = "raison_sociale", nullable = false)
    private String raisonSociale;

    @Column
    private String secteur;

    @Column(nullable = false)
    private String adresse;

    @Column(nullable = false)
    private String telephone;

    @Column(name = "date_creation", nullable = false)
    private LocalDate dateCreation = LocalDate.now();

    @OneToOne
    @MapsId
    @JoinColumn(name = "id_user")
    private User user;

    public Entreprise(User user, String raisonSociale, String secteur, String adresse, String telephone) {
        this.user = user;
        this.raisonSociale = raisonSociale;
        this.secteur = secteur;
        this.adresse = adresse;
        this.telephone = telephone;
        this.dateCreation = LocalDate.now();
    }
}
