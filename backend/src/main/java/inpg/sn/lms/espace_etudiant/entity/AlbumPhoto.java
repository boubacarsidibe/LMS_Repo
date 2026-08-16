package inpg.sn.lms.espace_etudiant.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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

/**
 * Album de la photothèque (TDR §3.2), typiquement un événement : sortie,
 * visite d'installation, cérémonie de promotion.
 */
@Entity
@Table(name = "albums_photo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AlbumPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "text")
    private String description;

    /** Promotion concernée, ou {@code null} pour un album institutionnel. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    private LocalDate dateEvenement;

    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Photo> photos = new ArrayList<>();

    public AlbumPhoto(String titre, String description, Promotion promotion, LocalDate dateEvenement) {
        this.titre = titre;
        this.description = description;
        this.promotion = promotion;
        this.dateEvenement = dateEvenement;
    }
}
