package inpg.sn.lms.recrutement.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.auth.repository.CandidatRepository;
import inpg.sn.lms.recrutement.dto.TestAcademiqueRequest;
import inpg.sn.lms.recrutement.dto.TestAcademiqueResponse;
import inpg.sn.lms.recrutement.entity.TestAcademique;
import inpg.sn.lms.recrutement.repository.TestAcademiqueRepository;

@Service
@Transactional
public class TestAcademiqueService {

    private final TestAcademiqueRepository testAcademiqueRepository;
    private final CandidatRepository candidatRepository;

    public TestAcademiqueService(TestAcademiqueRepository testAcademiqueRepository,
            CandidatRepository candidatRepository) {
        this.testAcademiqueRepository = testAcademiqueRepository;
        this.candidatRepository = candidatRepository;
    }

    public TestAcademiqueResponse creerTest(TestAcademiqueRequest request) {
        Candidat candidat = candidatRepository.findById(request.getCandidatId())
                .orElseThrow(() -> new RuntimeException("Candidat non trouvé avec l'ID: " + request.getCandidatId()));

        TestAcademique test = new TestAcademique();
        test.setNomEpreuve(request.getNomEpreuve());
        test.setCoefficient(request.getCoefficient());
        test.setNoteObtenue(request.getNoteObtenue());
        test.setDateEpreuve(request.getDateEpreuve());
        test.setCandidat(candidat);

        TestAcademique saved = testAcademiqueRepository.save(test);
        return toResponse(saved);
    }

    public TestAcademiqueResponse modifierTest(Long id, TestAcademiqueRequest request) {
        TestAcademique test = testAcademiqueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Test non trouvé avec l'ID: " + id));

        test.setNomEpreuve(request.getNomEpreuve());
        test.setCoefficient(request.getCoefficient());
        test.setNoteObtenue(request.getNoteObtenue());
        test.setDateEpreuve(request.getDateEpreuve());

        TestAcademique saved = testAcademiqueRepository.save(test);
        return toResponse(saved);
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
            throw new RuntimeException("Test non trouvé avec l'ID: " + id);
        }
        testAcademiqueRepository.deleteById(id);
    }

    private TestAcademiqueResponse toResponse(TestAcademique t) {
        return TestAcademiqueResponse.builder()
                .id(t.getId())
                .nomEpreuve(t.getNomEpreuve())
                .coefficient(t.getCoefficient())
                .noteObtenue(t.getNoteObtenue())
                .dateEpreuve(t.getDateEpreuve())
                .candidatId(t.getCandidat().getId())
                .build();
    }
}