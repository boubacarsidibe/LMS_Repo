package inpg.sn.lms.recrutement.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import inpg.sn.lms.recrutement.entity.SessionCandidature;
import inpg.sn.lms.recrutement.repository.SessionCandidatureRepository;

@Service
public class ClotureSessionService {

    private final SessionCandidatureRepository repository;
    private final Clock clock;

    @Autowired
    public ClotureSessionService(SessionCandidatureRepository repository) {
        this(repository, Clock.systemDefaultZone());
    }

    ClotureSessionService(SessionCandidatureRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public int cloturerSessionsExpirees() {
        LocalDate aujourdHui = LocalDate.now(clock);
        List<SessionCandidature> expirees = repository.findAll().stream()
                .filter(session -> "OUVERTE".equals(session.getStatus()))
                .filter(session -> session.getDateFermeture() != null
                        && aujourdHui.isAfter(session.getDateFermeture()))
                .toList();
        expirees.forEach(session -> {
            session.setStatus("CLOTUREE");
            session.setEstOuverte(false);
        });
        repository.saveAll(expirees);
        return expirees.size();
    }
}
