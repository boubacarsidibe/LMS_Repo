package inpg.sn.lms.recrutement.entity;

import java.time.LocalDateTime;

import inpg.sn.lms.auth.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Brouillon du formulaire de candidature, persisté côté serveur (une ligne
 * par utilisateur) afin qu'un candidat puisse reprendre sa saisie depuis un
 * autre appareil (TDR 3.1.1) — indépendant du modèle Candidature/Session
 * puisqu'à ce stade le candidat n'a pas forcément encore choisi de session.
 * Le contenu est stocké tel quel (JSON produit par le frontend), le backend
 * ne l'interprète pas.
 */
@Entity
@Table(name = "brouillons_candidature")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BrouillonCandidature {

    @Id
    private Long id;

    @Column(nullable = false, columnDefinition = "text")
    private String donneesJson;

    @Column(nullable = false)
    private LocalDateTime dateMaj;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id_user")
    private User user;

    public BrouillonCandidature(User user, String donneesJson) {
        this.user = user;
        this.donneesJson = donneesJson;
        this.dateMaj = LocalDateTime.now();
    }
}
