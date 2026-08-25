package inpg.sn.lms.espace_alumni.entity;

import java.time.LocalDateTime;

import inpg.sn.lms.auth.entity.User;
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
 * Document partagé dans l'espace Alumni (TDR §C), migration V14.
 *
 * <p>Un seul espace de partage plat, ouvert à tous les alumni — voir le
 * commentaire de la migration pour pourquoi aucune stratification par promotion
 * n'est introduite. Le stockage suit le même patron que la photothèque et les
 * pièces jointes du forum : nom d'origine conservé pour l'affichage, nom stocké
 * préfixé d'un UUID pour éviter toute collision.
 */
@Entity
@Table(name = "documents_partages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentPartage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auteur_id", nullable = false)
    private User auteur;

    @Column(nullable = false, length = 200)
    private String titre;

    @Column(length = 1000)
    private String description;

    @Column(name = "nom_fichier_original", nullable = false)
    private String nomFichierOriginal;

    @Column(name = "nom_fichier_stocke", nullable = false)
    private String nomFichierStocke;

    @Column(name = "type_contenu")
    private String typeContenu;

    @Column(name = "date_ajout", nullable = false)
    private LocalDateTime dateAjout = LocalDateTime.now();

    public DocumentPartage(User auteur, String titre, String description, String nomFichierOriginal,
            String nomFichierStocke, String typeContenu) {
        this.auteur = auteur;
        this.titre = titre;
        this.description = description;
        this.nomFichierOriginal = nomFichierOriginal;
        this.nomFichierStocke = nomFichierStocke;
        this.typeContenu = typeContenu;
        this.dateAjout = LocalDateTime.now();
    }
}
