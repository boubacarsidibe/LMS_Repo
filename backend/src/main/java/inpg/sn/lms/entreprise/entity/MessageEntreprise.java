package inpg.sn.lms.entreprise.entity;

import java.time.LocalDateTime;

import inpg.sn.lms.auth.entity.Entreprise;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Message du canal privé entre une entreprise et l'équipe pédagogique
 * (TDR §H : « système de messagerie sécurisé »).
 *
 * <p>Canal cloisonné par entreprise, pas par le patron habituel du forum
 * ({@code ForumSujet.portee}) : voir le commentaire de la migration V17 pour
 * pourquoi. L'auteur peut être soit l'entreprise elle-même, soit un membre de
 * l'équipe pédagogique — {@code auteur} référence directement {@link User},
 * la distinction se fait en comparant son identifiant à celui de
 * {@code entreprise.getUser()}.
 */
@Entity
@Table(name = "messages_entreprise")
@Getter
@Setter
@NoArgsConstructor
public class MessageEntreprise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id", nullable = false)
    private Entreprise entreprise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auteur_id", nullable = false)
    private User auteur;

    @Column(nullable = false, length = 2000)
    private String contenu;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    public MessageEntreprise(Entreprise entreprise, User auteur, String contenu) {
        this.entreprise = entreprise;
        this.auteur = auteur;
        this.contenu = contenu;
        this.dateCreation = LocalDateTime.now();
    }
}
