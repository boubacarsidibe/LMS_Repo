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

    /**
     * Code du référentiel DRCQ (ex. « 1.3 », « 3.1 »). Nul pour un critère créé
     * à la main hors référentiel. Unique quand renseigné (index partiel V31).
     */
    @Column(length = 10)
    private String code;

    /** Section du référentiel (ex. « 1. Management de la qualité »). */
    @Column(length = 120)
    private String section;

    @Column(nullable = false, length = 600)
    private String libelle;

    @Column(length = 2000)
    private String description;

    /** Rang d'affichage dans la grille d'évaluation. */
    @Column(nullable = false)
    private int ordre = 0;

    @Column(nullable = false)
    private boolean actif = true;

    public CritereAccreditation(String libelle, String description) {
        this.libelle = libelle;
        this.description = description;
        this.actif = true;
    }

    public CritereAccreditation(String code, String section, String libelle, String description, int ordre) {
        this.code = code;
        this.section = section;
        this.libelle = libelle;
        this.description = description;
        this.ordre = ordre;
        this.actif = true;
    }
}
