package inpg.sn.lms.espace_etudiant.entity;

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
 * Réponse postée dans un {@link ForumSujet}.
 *
 * <p>C'est aussi le canal de communication apprenant ↔ formateur demandé au
 * §3.5 du cahier des charges : l'auteur est un {@code User}, ce qui couvre
 * indifféremment un étudiant ou un formateur.
 */
@Entity
@Table(name = "forum_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ForumMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sujet_id", nullable = false)
    private ForumSujet sujet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auteur_id")
    private User auteur;

    @Column(nullable = false, columnDefinition = "text")
    private String contenu;

    @Column(nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    /**
     * Pièce jointe facultative, reprise du modèle {@code Message} auquel ce forum
     * s'est substitué : le formateur s'en sert pour joindre une correction ou une
     * capture à sa réponse.
     *
     * <p>Trois informations sont nécessaires. Le nom d'origine est celui montré à
     * l'utilisateur ; le nom stocké porte un préfixe UUID, sans lequel deux envois
     * du même fichier s'écraseraient ; le type de contenu est rejoué tel quel dans
     * la réponse HTTP au téléchargement.
     */
    @Column
    private String pieceJointeNomOriginal;

    @Column
    private String pieceJointeNomStocke;

    @Column
    private String pieceJointeTypeContenu;

    /**
     * Message précis auquel celui-ci répond (citation façon WhatsApp), facultatif.
     * L'auteur du message cité est notifié — voir {@code ForumController#repondre}.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reponse_a_id")
    private ForumMessage reponseA;

    public ForumMessage(ForumSujet sujet, User auteur, String contenu) {
        this.sujet = sujet;
        this.auteur = auteur;
        this.contenu = contenu;
        this.dateCreation = LocalDateTime.now();
    }
}
