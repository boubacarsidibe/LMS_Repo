package inpg.sn.lms.recrutement.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.recrutement.enums.EnumStatutCandidature;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "candidatures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Candidature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String numeroDossier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnumStatutCandidature statut;

    @Column(nullable = false)
    private LocalDateTime dateSoumission;

    @Column(length = 1000)
    private String motifRejet;

    @Column(name = "rang_liste_attente")
    private Integer rangListeAttente;

    /** Statut d'avant la dernière décision — permet d'« annuler » sans historique complet. */
    @Enumerated(EnumType.STRING)
    @Column(name = "statut_precedent")
    private EnumStatutCandidature statutPrecedent;

    @Column(columnDefinition = "text")
    private String informationsAcademiques;

    @Column(columnDefinition = "text")
    private String informationsProfessionnelles;

    @ManyToOne
    @JoinColumn(name = "candidat_id", nullable = false)
    private Candidat candidat;

    @ManyToOne
    @JoinColumn(name = "session_candidature_id", nullable = false)
    private SessionCandidature sessionCandidature;

    @OneToMany(mappedBy = "candidature", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PieceJustificative> piecesJustificatives = new ArrayList<>();

    @OneToOne(mappedBy = "candidature", cascade = CascadeType.ALL, orphanRemoval = true)
    private Paiement paiement;

    @OneToMany(mappedBy = "candidature", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReponseCritereCandidature> reponsesCriteres = new ArrayList<>();

    public void sauvegarderBrouillon() {
        this.statut = EnumStatutCandidature.BROUILLON;
    }

    public void soumettre() {
        this.statut = EnumStatutCandidature.SOUMIS;
        this.dateSoumission = LocalDateTime.now();
    }
}
