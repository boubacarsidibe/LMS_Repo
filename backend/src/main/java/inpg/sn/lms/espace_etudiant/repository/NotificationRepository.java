package inpg.sn.lms.espace_etudiant.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import inpg.sn.lms.espace_etudiant.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop50ByDestinataire_IdOrderByDateCreationDesc(Long userId);

    long countByDestinataire_IdAndLuFalse(Long userId);

    /**
     * Sert au rappel automatique d'évaluation (TDR §G, besoin B4) : évite de
     * relancer un étudiant qui l'a déjà été récemment, quel que soit le nombre
     * de cours en attente au moment du rappel précédent.
     */
    boolean existsByDestinataire_IdAndTypeAndDateCreationAfter(Long userId, String type, LocalDateTime seuil);

    /**
     * Marque tout comme lu en une requête.
     *
     * <p>Charger puis réenregistrer chaque notification ferait autant d'écritures que
     * de lignes, pour un geste que l'utilisateur perçoit comme unique.
     */
    @Modifying
    @Query("UPDATE Notification n SET n.lu = true WHERE n.destinataire.id = :userId AND n.lu = false")
    int marquerToutLu(@Param("userId") Long userId);
}
