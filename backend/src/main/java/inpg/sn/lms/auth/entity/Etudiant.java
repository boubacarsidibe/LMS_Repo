package inpg.sn.lms.auth.entity;

import inpg.sn.lms.espace_etudiant.entity.Cohorte;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Year;
import java.util.UUID;

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

    @OneToOne
    @MapsId
    @JoinColumn(name = "id_user")
    private User user;

    @ManyToOne
    @JoinColumn(name = "cohorte_id")
    private Cohorte cohorte;

    @Column(unique = true)
    private String matricule;

    public Etudiant(User user, String nom, String prenom, String telephone) {
        this.user = user;
        this.nom = nom;
        this.prenom = prenom;
        this.telephone = telephone;
    }

    /**
     * Couvre tous les chemins de création (EtudiantService, AdminController, etc.)
     * sans avoir à dupliquer la génération dans chacun.
     */
    @PrePersist
    private void genererMatriculeSiAbsent() {
        if (matricule == null || matricule.isBlank()) {
            String suffixe = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
            matricule = "INPG" + Year.now().getValue() + suffixe;
        }
    }
}
