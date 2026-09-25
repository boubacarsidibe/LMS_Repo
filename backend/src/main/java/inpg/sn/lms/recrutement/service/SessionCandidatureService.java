package inpg.sn.lms.recrutement.service;

import inpg.sn.lms.exception.BusinessException;
import inpg.sn.lms.exception.ResourceNotFoundException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import inpg.sn.lms.recrutement.dto.CritereAdmissionRequest;
import inpg.sn.lms.recrutement.dto.CritereAdmissionResponse;
import inpg.sn.lms.recrutement.dto.SessionCandidatureRequest;
import inpg.sn.lms.recrutement.dto.SessionCandidatureResponse;
import inpg.sn.lms.recrutement.entity.CritereAdmissionPersonnalise;
import inpg.sn.lms.recrutement.entity.SessionCandidature;
import inpg.sn.lms.recrutement.repository.CritereAdmissionPersonnaliseRepository;
import inpg.sn.lms.recrutement.repository.ProgramRepository;
import inpg.sn.lms.recrutement.repository.SessionCandidatureRepository;

@Service
@Transactional
public class SessionCandidatureService {

    private final SessionCandidatureRepository sessionRepository;
    private final CritereAdmissionPersonnaliseRepository critereRepository;
    private final ProgramRepository programRepository;

    public SessionCandidatureService(SessionCandidatureRepository sessionRepository,
            CritereAdmissionPersonnaliseRepository critereRepository,
            ProgramRepository programRepository) {
        this.sessionRepository = sessionRepository;
        this.critereRepository = critereRepository;
        this.programRepository = programRepository;
    }

    public SessionCandidatureResponse creerSession(SessionCandidatureRequest request) {
        SessionCandidature session = new SessionCandidature();
        session.setLibelle(request.getLibelle());
        session.setDateOuverture(request.getDateOuverture());
        session.setDateFermeture(request.getDateFermeture());
        session.setEstOuverte(request.isEstOuverte());
        appliquerConfiguration(session, request);
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
        appliquerConfiguration(session, request);
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
        session.setMontantFraisDossier(request.getMontantFraisDossier());
    }

    private void appliquerConfiguration(SessionCandidature session, SessionCandidatureRequest request) {
        session.setQuotaMax(request.getQuotaMax() != null ? request.getQuotaMax() : 220);
        String status = vide(request.getStatus())
                ? (request.isEstOuverte() ? "OUVERTE" : "BROUILLON")
                : request.getStatus();
        session.setStatus(status);
        session.setEstOuverte("OUVERTE".equals(status));
        session.getProgrammes().clear();
        if (request.getProgrammeIds() != null) {
            session.getProgrammes().addAll(programRepository.findAllById(request.getProgrammeIds()));
        }
        session.setProgram(session.getProgrammes().stream().findFirst().orElse(null));
    }

    private boolean vide(String valeur) {
        return valeur == null || valeur.isBlank();
    }

    /** Catalogue public : seuls les critères encore proposés (actifs) apparaissent sur le formulaire. */
    public List<SessionCandidatureResponse> getSessionsOuvertes() {
        return sessionRepository.findByEstOuverteTrue()
                .stream()
                .map(s -> toResponse(s, true))
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

    // ===================== CRITÈRES PERSONNALISÉS =====================

    public List<CritereAdmissionResponse> getCriteresDeSession(Long sessionId) {
        return critereRepository.findBySessionCandidatureIdOrderByOrdreAsc(sessionId).stream()
                .map(this::toCritereResponse)
                .collect(Collectors.toList());
    }

    public CritereAdmissionResponse getCritere(Long critereId) {
        CritereAdmissionPersonnalise critere = critereRepository.findById(critereId)
                .orElseThrow(() -> new ResourceNotFoundException("Critère non trouvé avec l'ID: " + critereId));
        return toCritereResponse(critere);
    }

    public CritereAdmissionResponse creerCritere(Long sessionId, CritereAdmissionRequest request) {
        SessionCandidature session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session non trouvée avec l'ID: " + sessionId));

        CritereAdmissionPersonnalise critere = new CritereAdmissionPersonnalise();
        critere.setSessionCandidature(session);
        appliquerCritere(critere, request);

        return toCritereResponse(critereRepository.save(critere));
    }

    public CritereAdmissionResponse modifierCritere(Long critereId, CritereAdmissionRequest request) {
        CritereAdmissionPersonnalise critere = critereRepository.findById(critereId)
                .orElseThrow(() -> new ResourceNotFoundException("Critère non trouvé avec l'ID: " + critereId));
        appliquerCritere(critere, request);
        return toCritereResponse(critereRepository.save(critere));
    }

    /**
     * Retrait logique uniquement (voir migration V30) : un critère déjà répondu par
     * des candidats ne doit jamais perdre ces réponses parce que l'admin a cessé de
     * le proposer aux nouvelles candidatures.
     */
    public CritereAdmissionResponse retirerCritere(Long critereId) {
        CritereAdmissionPersonnalise critere = critereRepository.findById(critereId)
                .orElseThrow(() -> new ResourceNotFoundException("Critère non trouvé avec l'ID: " + critereId));
        critere.setActif(false);
        return toCritereResponse(critereRepository.save(critere));
    }

    public CritereAdmissionResponse reactiverCritere(Long critereId) {
        CritereAdmissionPersonnalise critere = critereRepository.findById(critereId)
                .orElseThrow(() -> new ResourceNotFoundException("Critère non trouvé avec l'ID: " + critereId));
        critere.setActif(true);
        return toCritereResponse(critereRepository.save(critere));
    }

    private void appliquerCritere(CritereAdmissionPersonnalise critere, CritereAdmissionRequest request) {
        if (request.getLibelle() == null || request.getLibelle().isBlank()) {
            throw new BusinessException("Le libellé du critère est obligatoire.");
        }
        if (request.getType() == null) {
            throw new BusinessException("Le type du critère est obligatoire.");
        }
        critere.setLibelle(request.getLibelle());
        critere.setType(request.getType());
        critere.setObligatoire(request.isObligatoire());
        critere.setOptions(vide(request.getOptions()) ? null : request.getOptions());
        critere.setValeurMinimale(request.getValeurMinimale());
        critere.setValeurAttendue(vide(request.getValeurAttendue()) ? null : request.getValeurAttendue());
        critere.setOrdre(request.getOrdre());
    }

    private CritereAdmissionResponse toCritereResponse(CritereAdmissionPersonnalise c) {
        return CritereAdmissionResponse.builder()
                .id(c.getId())
                .libelle(c.getLibelle())
                .type(c.getType())
                .obligatoire(c.isObligatoire())
                .options(c.getOptions())
                .valeurMinimale(c.getValeurMinimale())
                .valeurAttendue(c.getValeurAttendue())
                .ordre(c.getOrdre())
                .actif(c.isActif())
                .build();
    }

    private SessionCandidatureResponse toResponse(SessionCandidature s) {
        return toResponse(s, false);
    }

    /** {@code seulsActifs} : le formulaire public ne doit jamais proposer un critère retiré. */
    private SessionCandidatureResponse toResponse(SessionCandidature s, boolean seulsActifs) {
        List<CritereAdmissionResponse> criteres = s.getCriteresPersonnalises().stream()
                .filter(c -> !seulsActifs || c.isActif())
                .sorted((a, b) -> Integer.compare(a.getOrdre(), b.getOrdre()))
                .map(this::toCritereResponse)
                .collect(Collectors.toList());

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
                .montantFraisDossier(s.getMontantFraisDossier())
                .programmeIds(s.getProgrammes().stream().map(p -> p.getId()).toList())
                .quotaMax(s.getQuotaMax())
                .status(s.getStatus())
                .criteresPersonnalises(criteres)
                .build();
    }
}
