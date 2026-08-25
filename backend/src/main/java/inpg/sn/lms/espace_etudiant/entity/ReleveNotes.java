package inpg.sn.lms.espace_etudiant.entity;

import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import inpg.sn.lms.auth.entity.Etudiant;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Relevé de notes délivré par la direction pédagogique.
 *
 * <p>L'étudiant consultait déjà ses notes en continu, mais rien ne constituait un
 * <strong>document</strong> : ni référence, ni date d'émission, ni trace de ce qui
 * avait été remis. Le responsable pédagogique, à qui revient la délivrance, n'avait
 * même aucun moyen de consulter le relevé d'un étudiant.
 *
 * <p>Les notes sont <strong>recopiées</strong> dans {@link ReleveLigne} à la
 * délivrance, jamais recalculées à l'affichage. Un relevé remis en janvier ne doit
 * pas changer parce qu'un formateur corrige une note en février — ce serait un écran,
 * pas un document. Les lignes peuvent donc diverger des notes courantes : c'est voulu.
 */
@Entity
@Table(name = "releve_notes")
public class ReleveNotes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "etudiant_id", nullable = false)
    private Etudiant etudiant;

    /** Celle de l'étudiant au moment de la délivrance ; conservée même s'il en change. */
    @ManyToOne
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    @Column(nullable = false, unique = true)
    private String reference;

    /** Ce que le relevé couvre : « Semestre 1 — 2025/2026 », « Année 2025/2026 »… */
    @Column(nullable = false)
    private String periode;

    @Column(name = "date_emission", nullable = false)
    private LocalDate dateEmission;

    /** L'établissement, jamais la personne connectée : la délivrance l'engage, lui. */
    @Column(name = "delivre_par", nullable = false)
    private String delivrePar;

    /** Figée. {@code null} si aucune note n'était saisie au moment de la délivrance. */
    @Column(name = "moyenne_generale")
    private Double moyenneGenerale;

    @Column(name = "nombre_notes", nullable = false)
    private int nombreNotes;

    @OneToMany(mappedBy = "releve", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReleveLigne> lignes = new ArrayList<>();

    /**
     * Référence et date posées à l'enregistrement, comme pour {@code Certificat}.
     *
     * <p>Les faire porter par l'appelant exposerait à délivrer deux relevés portant la
     * même référence, ce que la contrainte d'unicité refuserait au dernier moment.
     */
    @PrePersist
    private void avantEnregistrement() {
        if (reference == null || reference.isBlank()) {
            String suffixe = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            reference = "INPG-REL-" + Year.now().getValue() + "-" + suffixe;
        }
        if (dateEmission == null) {
            dateEmission = LocalDate.now();
        }
    }

    /** Tient les deux côtés à jour : sans cela la ligne n'aurait pas de relevé à l'insertion. */
    public void ajouterLigne(ReleveLigne ligne) {
        ligne.setReleve(this);
        lignes.add(ligne);
    }

    public Long getId() {
        return id;
    }

    public Etudiant getEtudiant() {
        return etudiant;
    }

    public void setEtudiant(Etudiant etudiant) {
        this.etudiant = etudiant;
    }

    public Promotion getPromotion() {
        return promotion;
    }

    public void setPromotion(Promotion promotion) {
        this.promotion = promotion;
    }

    public String getReference() {
        return reference;
    }

    public String getPeriode() {
        return periode;
    }

    public void setPeriode(String periode) {
        this.periode = periode;
    }

    public LocalDate getDateEmission() {
        return dateEmission;
    }

    public String getDelivrePar() {
        return delivrePar;
    }

    public void setDelivrePar(String delivrePar) {
        this.delivrePar = delivrePar;
    }

    public Double getMoyenneGenerale() {
        return moyenneGenerale;
    }

    public void setMoyenneGenerale(Double moyenneGenerale) {
        this.moyenneGenerale = moyenneGenerale;
    }

    public int getNombreNotes() {
        return nombreNotes;
    }

    public void setNombreNotes(int nombreNotes) {
        this.nombreNotes = nombreNotes;
    }

    public List<ReleveLigne> getLignes() {
        return lignes;
    }
}
