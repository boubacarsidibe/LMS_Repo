package inpg.sn.lms.recrutement.entity;

import inpg.sn.lms.recrutement.enums.EnumTypeCritere;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Critère d'admission ajouté librement par le responsable admission, en plus des
 * critères de base fixes de {@link SessionCandidature} (niveau d'étude, âge, genre,
 * expérience). Apparaît comme un champ additionnel du formulaire de candidature.
 *
 * <p>{@code valeurMinimale} (type {@code NUMERIQUE}) et {@code valeurAttendue}
 * (autres types) sont les seules règles d'évaluation automatique possibles ; un
 * critère qui ne porte ni l'une ni l'autre reste purement informatif — collecté,
 * mais jamais éliminatoire — et ne bloque jamais {@link SessionCandidature#respecteCriteres}.
 */
@Entity
@Table(name = "criteres_admission_personnalises")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CritereAdmissionPersonnalise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "session_candidature_id", nullable = false)
    private SessionCandidature sessionCandidature;

    @Column(nullable = false, length = 150)
    private String libelle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnumTypeCritere type;

    @Column(nullable = false)
    private boolean obligatoire = true;

    /** Options proposées, séparées par des virgules — uniquement pour le type {@code CHOIX}. */
    @Column(length = 500)
    private String options;

    @Column(name = "valeur_minimale")
    private Integer valeurMinimale;

    @Column(name = "valeur_attendue", length = 255)
    private String valeurAttendue;

    @Column(nullable = false)
    private int ordre = 0;

    /**
     * Retrait logique : {@code false} retire le critère du formulaire des nouvelles
     * candidatures sans toucher aux réponses déjà enregistrées par des candidats
     * (voir migration V30 — jamais de suppression physique tant que des réponses existent).
     */
    @Column(nullable = false)
    private boolean actif = true;

    /** Un critère informatif (sans règle) ne bloque jamais ; sinon compare la réponse à la règle définie. */
    public boolean estRespecte(String valeurCandidat) {
        if (type == EnumTypeCritere.NUMERIQUE && valeurMinimale != null) {
            if (valeurCandidat == null || valeurCandidat.isBlank()) {
                return false;
            }
            try {
                return Integer.parseInt(valeurCandidat.trim()) >= valeurMinimale;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        if (valeurAttendue != null) {
            return valeurAttendue.equalsIgnoreCase(valeurCandidat == null ? "" : valeurCandidat.trim());
        }

        return true;
    }
}
