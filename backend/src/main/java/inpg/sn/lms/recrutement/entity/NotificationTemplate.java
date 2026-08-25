package inpg.sn.lms.recrutement.entity;

import inpg.sn.lms.recrutement.enums.EnumTypeNotification;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Modèle d'email personnalisable pour un type de notification donné (une
 * ligne par valeur de {@link EnumTypeNotification}). Quand {@code actif} est
 * faux, l'envoi de ce type d'email est désactivé (TDR : "activer les
 * notifications automatiques").
 */
@Entity
@Table(name = "notification_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplate {

    @Id
    @Enumerated(EnumType.STRING)
    private EnumTypeNotification type;

    @Column(nullable = false)
    private String sujet;

    @Column(nullable = false, columnDefinition = "text")
    private String corps;

    @Column(nullable = false)
    private boolean actif = true;
}
