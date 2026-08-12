package inpg.sn.lms.recrutement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Configuration unique (ligne id=1) des critères automatiques de premier
 * filtre appliqués à la soumission d'une candidature (TDR 3.1.1) : quand
 * {@code actif} est vrai, une candidature qui ne remplit pas ces critères
 * est automatiquement rejetée, les autres passent directement en
 * présélection.
 */
@Entity
@Table(name = "criteres_selection")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CritereSelection {

    @Id
    private Long id = 1L;

    @Column(nullable = false)
    private boolean actif = false;

    /** Doit correspondre à une des valeurs du sélecteur "niveau d'études actuel" du formulaire ; null = pas de contrainte. */
    @Column
    private String niveauEtudeMinimum;

    @Column
    private Integer ageMinimum;

    @Column
    private Integer ageMaximum;
}
