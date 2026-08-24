package inpg.sn.lms.accreditation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Critère d'accréditation défini par l'INPG (TDR §F : « définir et gérer les
 * critères d'accréditation »), consulté par les évaluateurs comme grille de
 * lecture d'un dossier. Un critère désactivé n'est plus proposé pour une
 * nouvelle évaluation, mais reste visible sur les décisions déjà rendues.
 */
@Entity
@Table(name = "criteres_accreditation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CritereAccreditation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String libelle;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean actif = true;

    public CritereAccreditation(String libelle, String description) {
        this.libelle = libelle;
        this.description = description;
        this.actif = true;
    }
}
