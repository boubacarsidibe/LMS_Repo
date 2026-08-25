package inpg.sn.lms.recrutement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.recrutement.entity.NotificationTemplate;
import inpg.sn.lms.recrutement.enums.EnumTypeNotification;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, EnumTypeNotification> {
}
