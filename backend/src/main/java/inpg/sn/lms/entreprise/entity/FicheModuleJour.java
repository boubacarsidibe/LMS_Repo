package inpg.sn.lms.entreprise.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Une journée du planning d'une {@link FicheTechniqueModule} (section ④ du
 * gabarit : contenus et activités, durée, type d'enseignement, modalités).
 */
@Entity
@Table(name = "fiche_module_jour")
@Getter
@Setter
@NoArgsConstructor
public class FicheModuleJour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiche_id", nullable = false)
    @JsonIgnore
    private FicheTechniqueModule fiche;

    @Column(nullable = false)
    private int numero;

    @Column(length = 2000)
    private String contenus;

    @Column(name = "duree_heures")
    private Integer dureeHeures;

    @Column(name = "type_enseignement", length = 200)
    private String typeEnseignement;

    @Column(length = 500)
    private String modalites;

    public FicheModuleJour(FicheTechniqueModule fiche, int numero, String contenus, Integer dureeHeures,
            String typeEnseignement, String modalites) {
        this.fiche = fiche;
        this.numero = numero;
        this.contenus = contenus;
        this.dureeHeures = dureeHeures;
        this.typeEnseignement = typeEnseignement;
        this.modalites = modalites;
    }
}
