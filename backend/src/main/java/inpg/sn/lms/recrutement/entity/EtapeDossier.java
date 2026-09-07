package inpg.sn.lms.recrutement.entity;

import java.time.LocalDateTime;

import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.recrutement.enums.EnumEtapeQms;
import inpg.sn.lms.recrutement.enums.EnumStatutCandidature;
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
 * Une ligne du journal d'étapes d'un dossier (procédure qualité QMS-009).
 *
 * <p>Écrite une fois, jamais modifiée : c'est la preuve qu'une étape a eu lieu, par qui
 * et quand. {@code auteurNom} est un cliché figé et non une jointure vers {@code users}
 * — même raison que {@code releve_ligne} (migration V9) : un compte supprimé ou renommé
 * ne doit pas réécrire l'historique après coup, sinon le journal cesse d'être une preuve.
 */
@Entity
@Table(name = "etapes_dossier")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EtapeDossier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "candidature_id", nullable = false)
    private Candidature candidature;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EnumEtapeQms etape;

    /** Statut du dossier au terme de l'étape ; nul quand l'étape ne change pas le statut. */
    @Enumerated(EnumType.STRING)
    @Column(name = "statut_resultant", length = 40)
    private EnumStatutCandidature statutResultant;

    /** Nul lorsque l'étape n'est pas le fait d'un agent (dépôt par le candidat, traitement automatique). */
    @ManyToOne
    @JoinColumn(name = "auteur_id")
    private User auteur;

    @Column(name = "auteur_nom", nullable = false, length = 150)
    private String auteurNom;

    @Column(name = "date_etape", nullable = false)
    private LocalDateTime dateEtape = LocalDateTime.now();

    @Column(length = 1000)
    private String commentaire;
}
