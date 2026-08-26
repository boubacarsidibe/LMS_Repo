package inpg.sn.lms.personnel.entity;

import java.time.LocalDateTime;

import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.personnel.enums.EnumTypeActivite;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Activité mensuelle saisie par un membre du personnel pour un département
 * (TDR §I). {@code mois}/{@code annee} portent la période couverte, distincte
 * de {@code dateCreation} qui n'est que l'horodatage de la saisie — une
 * activité de janvier peut être saisie en retard, en février.
 */
@Entity
@Table(name = "activites_personnel")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActivitePersonnel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departement_id", nullable = false)
    private Departement departement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auteur_id", nullable = false)
    private User auteur;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_activite", nullable = false, length = 20)
    private EnumTypeActivite typeActivite;

    @Column(nullable = false)
    private String titre;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private int mois;

    @Column(nullable = false)
    private int annee;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    public ActivitePersonnel(Departement departement, User auteur, EnumTypeActivite typeActivite, String titre,
            String description, int mois, int annee) {
        this.departement = departement;
        this.auteur = auteur;
        this.typeActivite = typeActivite;
        this.titre = titre;
        this.description = description;
        this.mois = mois;
        this.annee = annee;
    }
}
