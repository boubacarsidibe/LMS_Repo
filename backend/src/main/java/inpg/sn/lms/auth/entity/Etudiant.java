package inpg.sn.lms.auth.entity;

import inpg.sn.lms.espace_etudiant.entity.Promotion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "etudiants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Etudiant  {

    @Id
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false, unique = true)
    private String telephone;

    /**
     * Identifiant unique de l'étudiant (TDR §3.2 : « créer automatiquement un
     * identifiant unique pour chaque étudiant ou apprenant »), au format
     * {@code INPG-<année>-<séquence>}.
     *
     * <p>Volontairement nullable : la colonne est ajoutée par {@code ddl-auto=update}
     * sur une table qui contient déjà des lignes, qu'une contrainte NOT NULL
     * rendrait impossible à créer. Le rattrapage est fait au démarrage par
     * {@code MatriculeService}.
     */
    @Column(unique = true)
    private String matricule;

    /**
     * Promotion suivie par l'étudiant. C'est le premier maillon de la chaîne
     * Etudiant → Promotion → Formation → Cours qui détermine ses « cours autorisés ».
     * Nullable tant que la scolarité n'a pas affecté l'étudiant à une promotion.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id_user")
    private User user;

    public Etudiant(User user, String nom, String prenom, String telephone) {
        this.user = user;
        this.nom = nom;
        this.prenom = prenom;
        this.telephone = telephone;
    }
}
