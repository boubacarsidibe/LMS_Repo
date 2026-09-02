package inpg.sn.lms.recrutement.service;

import inpg.sn.lms.exception.ResourceNotFoundException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.auth.repository.CandidatRepository;
import inpg.sn.lms.recrutement.dto.TestAcademiqueRequest;
import inpg.sn.lms.recrutement.dto.TestAcademiqueResponse;
import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.TestAcademique;
import inpg.sn.lms.recrutement.repository.CandidatureRepository;
import inpg.sn.lms.recrutement.repository.TestAcademiqueRepository;

@Service
@Transactional
public class TestAcademiqueService {

    private static final Logger log = LoggerFactory.getLogger(TestAcademiqueService.class);
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final TestAcademiqueRepository testAcademiqueRepository;
    private final CandidatRepository candidatRepository;
    private final CandidatureRepository candidatureRepository;
    private final NotificationService notificationService;
    private final NotificationCandidatService notificationCandidatService;

    public TestAcademiqueService(TestAcademiqueRepository testAcademiqueRepository,
            CandidatRepository candidatRepository,
            CandidatureRepository candidatureRepository,
            NotificationService notificationService,
            NotificationCandidatService notificationCandidatService) {
        this.testAcademiqueRepository = testAcademiqueRepository;
        this.candidatRepository = candidatRepository;
        this.candidatureRepository = candidatureRepository;
        this.notificationService = notificationService;
        this.notificationCandidatService = notificationCandidatService;
    }

    public TestAcademiqueResponse creerTest(TestAcademiqueRequest request) {
        Candidat candidat = candidatRepository.findById(request.getCandidatId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidat non trouvé avec l'ID: " + request.getCandidatId()));

        TestAcademique test = new TestAcademique();
        test.setNomEpreuve(request.getNomEpreuve());
        test.setCoefficient(request.getCoefficient());
        test.setNoteObtenue(request.getNoteObtenue());
        test.setDateEpreuve(request.getDateEpreuve());
        test.setLieuOuLien(request.getLieuOuLien());
        test.setCandidat(candidat);

        TestAcademique saved = testAcademiqueRepository.save(test);

        // Convocation automatique du candidat dès la planification du test (TDR 3.1.2).
        if (saved.getDateEpreuve() != null) {
            String dateFormatee = saved.getDateEpreuve().format(DATETIME_FORMAT);
            String emailCandidat = candidat.getUser() != null ? candidat.getUser().getEmail() : null;
            if (emailCandidat != null) {
                try {
                    notificationService.notifierConvocationTest(emailCandidat, saved.getNomEpreuve(), dateFormatee,
                            saved.getLieuOuLien());
                } catch (Exception e) {
                    log.error("Échec de l'envoi de la convocation au test à {} : {}", emailCandidat, e.getMessage(), e);
                }
            }
            notificationCandidatService.creerNotification(candidat,
                    "Vous êtes convoqué(e) au test \"" + saved.getNomEpreuve() + "\" le " + dateFormatee + ".",
                    "TEST_CONVOCATION", "/recrutement/suivi");
        }

        return toResponse(saved);
    }

    public TestAcademiqueResponse modifierTest(Long id, TestAcademiqueRequest request) {
        TestAcademique test = testAcademiqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Test non trouvé avec l'ID: " + id));

        test.setNomEpreuve(request.getNomEpreuve());
        test.setCoefficient(request.getCoefficient());
        test.setNoteObtenue(request.getNoteObtenue());
        test.setDateEpreuve(request.getDateEpreuve());
        test.setLieuOuLien(request.getLieuOuLien());

        TestAcademique saved = testAcademiqueRepository.save(test);
        return toResponse(saved);
    }

    public TestAcademiqueResponse getTestById(Long id) {
        TestAcademique test = testAcademiqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Test non trouvé avec l'ID: " + id));
        return toResponse(test);
    }

    public List<TestAcademiqueResponse> getTestsByCandidatId(Long candidatId) {
        return testAcademiqueRepository.findByCandidatId(candidatId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<TestAcademiqueResponse> getAllTests() {
        return testAcademiqueRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public void supprimerTest(Long id) {
        if (!testAcademiqueRepository.existsById(id)) {
            throw new ResourceNotFoundException("Test non trouvé avec l'ID: " + id);
        }
        testAcademiqueRepository.deleteById(id);
    }

    private TestAcademiqueResponse toResponse(TestAcademique t) {
        // Un candidat n'a normalement qu'une seule candidature active à la fois
        // (garde-fou de CandidatController#soumettreCandidature) : la première suffit
        // à retrouver la session à afficher sur la convocation.
        Candidature candidature = candidatureRepository.findByCandidatId(t.getCandidat().getId())
                .stream().findFirst().orElse(null);

        return TestAcademiqueResponse.builder()
                .id(t.getId())
                .nomEpreuve(t.getNomEpreuve())
                .coefficient(t.getCoefficient())
                .noteObtenue(t.getNoteObtenue())
                .dateEpreuve(t.getDateEpreuve())
                .lieuOuLien(t.getLieuOuLien())
                .candidatId(t.getCandidat().getId())
                .sessionCandidatureId(candidature != null ? candidature.getSessionCandidature().getId() : null)
                .sessionCandidatureLibelle(candidature != null ? candidature.getSessionCandidature().getLibelle() : null)
                .build();
    }
}