package inpg.sn.lms.config;

import inpg.sn.lms.recrutement.entity.SessionCandidature;
import inpg.sn.lms.recrutement.repository.SessionCandidatureRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initSessions(SessionCandidatureRepository sessionRepository) {
        return args -> {
            if (sessionRepository.count() == 0) {
                SessionCandidature session = new SessionCandidature();
                session.setLibelle("Session 2026 — Recrutement INPG");
                session.setDateOuverture(LocalDate.of(2026, 1, 1));
                session.setDateFermeture(LocalDate.of(2026, 12, 31));
                session.setEstOuverte(true);
                sessionRepository.save(session);
                System.out.println("✅ Session de candidature par défaut créée (ID=" + session.getId() + ")");
            }
        };
    }
}