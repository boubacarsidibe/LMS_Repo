package inpg.sn.lms.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
public class Formateur extends User {

    // L'id est hérité de User — pas besoin de le redéclarer !

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false, unique = true)
    private String telephone;
}