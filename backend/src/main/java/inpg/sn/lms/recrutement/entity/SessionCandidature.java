package inpg.sn.lms.recrutement.entity;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

import inpg.sn.lms.auth.entity.Candidat;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sessions_candidature")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SessionCandidature {

    /** Ordre croissant des niveaux d'études proposés dans le formulaire de candidature (frontend). */
    private static final List<String> NIVEAUX_ORDONNES = List.of(
            "Baccalauréat", "Licence (L3)", "Master 1 (M1)", "Master 2 (M2)");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String libelle;

    @Column(nullable = false)
    private LocalDate dateOuverture;

    @Column(nullable = false)
    private LocalDate dateFermeture;

    @Column(nullable = false)
    private boolean estOuverte;

    /**
     * Nombre de places annoncé pour la session. Purement indicatif : l'INPG suit le
     * remplissage, il ne bloque pas les dépôts au-delà.
     *
     * <p>Le compteur {@code quotaActuel} qui l'accompagnait a été retiré : aucune ligne
     * de code ne l'incrémentait à la soumission d'un dossier, si bien que le « taux de
     * remplissage » du tableau de bord et de l'export CSV restait figé à sa valeur
     * initiale. Le nombre de dossiers se lit désormais dans la table {@code candidatures}
     * (voir {@code CandidatureRepository#countBySessionCandidatureId}), où il ne peut pas
     * diverger de la réalité.
     */
    @Column(nullable = false)
    private int quotaMax = 220;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private String status = "BROUILLON"; // BROUILLON, OUVERT, CLOTURE, SUSPENDU

    @ManyToOne
    @JoinColumn(name = "program_id")
    private Program program;

    /**
     * Premier filtre automatique de présélection, propre à cette session
     * (configuré à la création, modifiable ensuite) : quand actif, une
     * candidature qui ne remplit pas ces critères est rejetée automatiquement
     * à la soumission, les autres passent directement en présélection.
     */
    @Column(nullable = false)
    private boolean criteresActifs = false;

    @Column
    private String criteresNiveauEtudeMinimum;

    @Column
    private Integer criteresAgeMinimum;

    @Column
    private Integer criteresAgeMaximum;

    @Column
    private String criteresGenre;

    @Column
    private Integer criteresExperienceMinimale;

    /** Seuil de réussite (note/20) des tests académiques pour cette session (TDR 3.1.2 : "pass/fail" défini par l'INPG). */
    @Column(nullable = false)
    private float seuilReussiteTest = 10f;

    @OneToMany(mappedBy = "sessionCandidature", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Candidature> candidatures = new ArrayList<>();

    public boolean estSessionActive() {
        LocalDate now = LocalDate.now();
        return estOuverte
                && (dateOuverture == null || !now.isBefore(dateOuverture))
                && (dateFermeture == null || !now.isAfter(dateFermeture));
    }

    /** Évalue un candidat par rapport aux critères de cette session. Toujours vrai si le filtre est désactivé. */
    public boolean respecteCriteres(Candidat candidat) {
        if (!criteresActifs) {
            return true;
        }

        if (criteresNiveauEtudeMinimum != null) {
            int seuil = NIVEAUX_ORDONNES.indexOf(criteresNiveauEtudeMinimum);
            int niveauCandidat = NIVEAUX_ORDONNES.indexOf(candidat.getNiveauEtude());
            // Niveau candidat non reconnu (ex: "Autre" ou valeur libre) : on ne bloque pas dessus.
            if (seuil >= 0 && niveauCandidat >= 0 && niveauCandidat < seuil) {
                return false;
            }
        }

        if (candidat.getDateNaissance() != null && (criteresAgeMinimum != null || criteresAgeMaximum != null)) {
            int age = Period.between(candidat.getDateNaissance(), LocalDate.now()).getYears();
            if (criteresAgeMinimum != null && age < criteresAgeMinimum) {
                return false;
            }
            if (criteresAgeMaximum != null && age > criteresAgeMaximum) {
                return false;
            }
        }

        if (criteresGenre != null && candidat.getGenre() != null && !criteresGenre.equalsIgnoreCase(candidat.getGenre())) {
            return false;
        }

        if (criteresExperienceMinimale != null && candidat.getAnneesExperience() != null
                && candidat.getAnneesExperience() < criteresExperienceMinimale) {
            return false;
        }

        return true;
    }
}