package inpg.sn.lms.recrutement.service;

import inpg.sn.lms.exception.ResourceNotFoundException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import inpg.sn.lms.recrutement.dto.SessionCandidatureRequest;
import inpg.sn.lms.recrutement.dto.SessionCandidatureResponse;
import inpg.sn.lms.recrutement.entity.SessionCandidature;
import inpg.sn.lms.recrutement.repository.SessionCandidatureRepository;

@Service
@Transactional
public class SessionCandidatureService {

    private final SessionCandidatureRepository sessionRepository;

    public SessionCandidatureService(SessionCandidatureRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public SessionCandidatureResponse creerSession(SessionCandidatureRequest request) {
        SessionCandidature session = new SessionCandidature();
        session.setLibelle(request.getLibelle());
        session.setDateOuverture(request.getDateOuverture());
        session.setDateFermeture(request.getDateFermeture());
        session.setEstOuverte(request.isEstOuverte());
        appliquerCriteres(session, request);

        SessionCandidature saved = sessionRepository.save(session);
        return toResponse(saved);
    }

    public SessionCandidatureResponse modifierSession(Long id, SessionCandidatureRequest request) {
        SessionCandidature session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session non trouvée avec l'ID: " + id));

        session.setLibelle(request.getLibelle());
        session.setDateOuverture(request.getDateOuverture());
        session.setDateFermeture(request.getDateFermeture());
        session.setEstOuverte(request.isEstOuverte());
        appliquerCriteres(session, request);

        SessionCandidature saved = sessionRepository.save(session);
        return toResponse(saved);
    }

    private void appliquerCriteres(SessionCandidature session, SessionCandidatureRequest request) {
        session.setCriteresActifs(request.isCriteresActifs());
        session.setCriteresNiveauEtudeMinimum(
                request.getCriteresNiveauEtudeMinimum() == null || request.getCriteresNiveauEtudeMinimum().isBlank()
                        ? null
                        : request.getCriteresNiveauEtudeMinimum());
        session.setCriteresAgeMinimum(request.getCriteresAgeMinimum());
        session.setCriteresAgeMaximum(request.getCriteresAgeMaximum());
        session.setCriteresGenre(
                request.getCriteresGenre() == null || request.getCriteresGenre().isBlank()
                        ? null
                        : request.getCriteresGenre());
        session.setCriteresExperienceMinimale(request.getCriteresExperienceMinimale());
        session.setSeuilReussiteTest(request.getSeuilReussiteTest() != null ? request.getSeuilReussiteTest() : 10f);
        session.setInfoFraisScolarite(vide(request.getInfoFraisScolarite()) ? null : request.getInfoFraisScolarite());
        session.setInfoCarteEtudiante(vide(request.getInfoCarteEtudiante()) ? null : request.getInfoCarteEtudiante());
        session.setInfoRentree(vide(request.getInfoRentree()) ? null : request.getInfoRentree());
    }

    private boolean vide(String valeur) {
        return valeur == null || valeur.isBlank();
    }

    public List<SessionCandidatureResponse> getSessionsOuvertes() {
        return sessionRepository.findByEstOuverteTrue()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<SessionCandidatureResponse> getAllSessions() {
        return sessionRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public SessionCandidatureResponse getSessionById(Long id) {
        SessionCandidature session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session non trouvée avec l'ID: " + id));
        return toResponse(session);
    }

    public void supprimerSession(Long id) {
        if (!sessionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Session non trouvée avec l'ID: " + id);
        }
        sessionRepository.deleteById(id);
    }

    private SessionCandidatureResponse toResponse(SessionCandidature s) {
        return SessionCandidatureResponse.builder()
                .id(s.getId())
                .libelle(s.getLibelle())
                .dateOuverture(s.getDateOuverture())
                .dateFermeture(s.getDateFermeture())
                .estOuverte(s.isEstOuverte())
                .estActive(s.estSessionActive())
                .criteresActifs(s.isCriteresActifs())
                .criteresNiveauEtudeMinimum(s.getCriteresNiveauEtudeMinimum())
                .criteresAgeMinimum(s.getCriteresAgeMinimum())
                .criteresAgeMaximum(s.getCriteresAgeMaximum())
                .criteresGenre(s.getCriteresGenre())
                .criteresExperienceMinimale(s.getCriteresExperienceMinimale())
                .seuilReussiteTest(s.getSeuilReussiteTest())
                .nbCandidatures(s.getCandidatures().size())
                .infoFraisScolarite(s.getInfoFraisScolarite())
                .infoCarteEtudiante(s.getInfoCarteEtudiante())
                .infoRentree(s.getInfoRentree())
                .build();
    }
}