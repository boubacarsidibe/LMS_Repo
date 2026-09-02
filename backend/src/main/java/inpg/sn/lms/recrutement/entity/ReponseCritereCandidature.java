package inpg.sn.lms.recrutement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** Réponse d'un candidat à un {@link CritereAdmissionPersonnalise} de la session à laquelle il postule. */
@Entity
@Table(name = "reponses_critere_candidature")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReponseCritereCandidature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "candidature_id", nullable = false)
    private Candidature candidature;

    @ManyToOne
    @JoinColumn(name = "critere_id", nullable = false)
    private CritereAdmissionPersonnalise critere;

    @Column(length = 500)
    private String valeur;
}
