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

@Entity
@Table(name = "candidats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Candidat  {

    @Id
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false)
    private String telephone;

    @Column(nullable = false)
    private String nationalite;

    @Column(nullable = false)
    private LocalDate dateNaissance;

    @Column(nullable = false)
    private String niveauEtude;

    /** Champs démographiques optionnels, utilisés pour les statistiques du back-office (TDR 3.1.1). */
    @Column
    private String genre;

    @Column
    private String profession;

    @Column
    private Integer anneesExperience;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id_user")
    private User user;

    public Candidat(User user, String nom, String prenom, String telephone, String nationalite, LocalDate dateNaissance, String niveauEtude) {
        this.user = user;
        this.nom = nom;
        this.prenom = prenom;
        this.telephone = telephone;
        this.nationalite = nationalite;
        this.dateNaissance = dateNaissance;
        this.niveauEtude = niveauEtude;
    }

}