package inpg.sn.lms.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
@Table(name = "formateurs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Formateur  {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false, unique = true)
    private String telephone;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id_user")
    private User user;

    public Formateur(User user, String nom, String prenom, String telephone) {
        this.user = user;
        this.nom = nom;
        this.prenom = prenom;
        this.telephone = telephone;
    }
}