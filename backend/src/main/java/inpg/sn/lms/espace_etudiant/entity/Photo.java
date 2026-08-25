package inpg.sn.lms.espace_etudiant.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Cliché rattaché à un {@link AlbumPhoto}.
 *
 * <p>Comme {@link SupportCours}, le fichier lui-même vit sur le disque sous
 * {@code ${app.upload.dir}} et seul son nom généré est stocké ici.
 */
@Entity
@Table(name = "photos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "album_id", nullable = false)
    private AlbumPhoto album;

    /** Nom du fichier sur le disque (préfixé d'un UUID à l'upload). */
    @Column(nullable = false)
    private String fichierStocke;

    /** Nom d'origine, réutilisé comme nom de téléchargement. */
    @Column(nullable = false)
    private String nomOriginal;

    private String legende;

    @Column(nullable = false)
    private LocalDateTime dateAjout = LocalDateTime.now();

    public Photo(AlbumPhoto album, String fichierStocke, String nomOriginal, String legende) {
        this.album = album;
        this.fichierStocke = fichierStocke;
        this.nomOriginal = nomOriginal;
        this.legende = legende;
        this.dateAjout = LocalDateTime.now();
    }
}
