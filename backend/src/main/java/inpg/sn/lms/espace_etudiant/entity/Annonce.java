package inpg.sn.lms.espace_etudiant.entity;

import java.time.LocalDateTime;

import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.espace_etudiant.enums.EnumTypeAnnonce;
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
 * Annonce, message ou mémo diffusé par l'INPG aux étudiants (TDR §3.2).
 */
@Entity
@Table(name = "annonces")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Annonce {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false, columnDefinition = "text")
    private String contenu;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnumTypeAnnonce type;

    @Column(nullable = false)
    private LocalDateTime datePublication = LocalDateTime.now();

    /** Auteur de la communication (membre du personnel INPG). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auteur_id")
    private User auteur;

    /**
     * Promotion destinataire. {@code null} signifie une diffusion à l'ensemble
     * des étudiants, toutes promotions confondues.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    /** Une annonce épinglée reste en tête de liste quelle que soit sa date. */
    @Column(nullable = false)
    private boolean epinglee = false;

    public Annonce(String titre, String contenu, EnumTypeAnnonce type, User auteur, Promotion promotion) {
        this.titre = titre;
        this.contenu = contenu;
        this.type = type;
        this.auteur = auteur;
        this.promotion = promotion;
        this.datePublication = LocalDateTime.now();
    }
}
