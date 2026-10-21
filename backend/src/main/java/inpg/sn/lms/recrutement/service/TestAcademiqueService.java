package inpg.sn.lms.recrutement.service;

import inpg.sn.lms.exception.BusinessException;
import inpg.sn.lms.exception.ResourceNotFoundException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    private final ProgressionDossierService progressionDossierService;

    public TestAcademiqueService(TestAcademiqueRepository testAcademiqueRepository,
            CandidatRepository candidatRepository,
            CandidatureRepository candidatureRepository,
            NotificationService notificationService,
            NotificationCandidatService notificationCandidatService,
            ProgressionDossierService progressionDossierService) {
        this.testAcademiqueRepository = testAcademiqueRepository;
        this.candidatRepository = candidatRepository;
        this.candidatureRepository = candidatureRepository;
        this.notificationService = notificationService;
        this.notificationCandidatService = notificationCandidatService;
        this.progressionDossierService = progressionDossierService;
    }

    public TestAcademiqueResponse creerTest(TestAcademiqueRequest request) {
        return creerTest(request, UUID.randomUUID());
    }

    /** Crée une ligne de note dans une épreuve commune identifiée de façon persistante. */
    public TestAcademiqueResponse creerTest(TestAcademiqueRequest request, UUID groupeEpreuveId) {
        Candidat candidat = candidatRepository.findById(request.getCandidatId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidat non trouvé avec l'ID: " + request.getCandidatId()));

        TestAcademique test = new TestAcademique();
        test.setNomEpreuve(request.getNomEpreuve());
        test.setCoefficient(request.getCoefficient());
        test.setNoteObtenue(request.getNoteObtenue());
        test.setDateEpreuve(request.getDateEpreuve());
        test.setLieuOuLien(request.getLieuOuLien());
        test.setGroupeEpreuveId(groupeEpreuveId);
        test.setCandidat(candidat);

        TestAcademique saved = testAcademiqueRepository.save(test);
        progressionDossierService.apresTests(candidat.getId());

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

        verifierNote(request.getNoteObtenue());
        test.setNomEpreuve(request.getNomEpreuve());
        test.setCoefficient(request.getCoefficient());
        test.setNoteObtenuePrecedente(test.getNoteObtenue());
        test.setNoteObtenue(request.getNoteObtenue());
        test.setDateEpreuve(request.getDateEpreuve());
        test.setLieuOuLien(request.getLieuOuLien());

        TestAcademique saved = testAcademiqueRepository.save(test);
        progressionDossierService.apresTests(saved.getCandidat().getId());
        return toResponse(saved);
    }

    /**
     * Saisie rapide d'une note seule, sans repasser par le formulaire complet de
     * l'épreuve (nom, date, lieu, coefficient) — {@link #modifierTest} écrase tous ces
     * champs avec ce que le client envoie, donc l'utiliser pour ne changer que la note
     * les aurait effacés si le client ne les renvoyait pas.
     */
    public TestAcademiqueResponse modifierNote(Long id, Float noteObtenue) {
        TestAcademique test = testAcademiqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Test non trouvé avec l'ID: " + id));
        verifierNote(noteObtenue);
        test.setNoteObtenuePrecedente(test.getNoteObtenue());
        test.setNoteObtenue(noteObtenue);
        TestAcademique saved = testAcademiqueRepository.save(test);
        progressionDossierService.apresTests(saved.getCandidat().getId());
        return toResponse(saved);
    }

    /**
     * La plage de notes n'était contrôlée que par l'attribut {@code max} du champ HTML,
     * c'est-à-dire nulle part : un appel direct enregistrait 999 ou -5, et cette note
     * entrait ensuite dans le classement de délibération. {@code EntretienService}
     * appliquait déjà cette règle de son côté ; les deux notes se comparent, elles
     * doivent se contrôler pareil.
     *
     * <p>{@code null} reste admis — c'est ainsi qu'on efface une note saisie par erreur.
     */
    private void verifierNote(Float note) {
        if (note != null && (note < 0 || note > 20)) {
            throw new BusinessException("La note du test doit être comprise entre 0 et 20.");
        }
    }

    /** Enregistre atomiquement un lot de notes, après validation de toutes les lignes et de la session. */
    public int importerNotes(Map<Long, Float> notes, Long sessionId) {
        if (notes.isEmpty()) {
            throw new BusinessException("Le fichier ne contient aucune note à importer.");
        }
        List<TestAcademique> tests = notes.keySet().stream()
                .map(id -> testAcademiqueRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Test non trouvé avec l'ID: " + id)))
                .collect(Collectors.toList());
        for (TestAcademique test : tests) {
            verifierNote(notes.get(test.getId()));
            Candidature candidature = candidatureRepository.findByCandidatId(test.getCandidat().getId())
                    .stream().findFirst().orElse(null);
            Long sessionTest = candidature != null && candidature.getSessionCandidature() != null
                    ? candidature.getSessionCandidature().getId() : null;
            if (sessionId != null && !sessionId.equals(sessionTest)) {
                throw new BusinessException("Une épreuve du fichier n'appartient pas à la session sélectionnée.");
            }
        }
        tests.forEach(test -> {
            test.setNoteObtenuePrecedente(test.getNoteObtenue());
            test.setNoteObtenue(notes.get(test.getId()));
        });
        testAcademiqueRepository.saveAll(tests);
        tests.stream().map(t -> t.getCandidat().getId()).distinct().forEach(progressionDossierService::apresTests);
        return tests.size();
    }
    /** Annule la dernière saisie de note (un seul niveau, pas d'historique complet). */
    public TestAcademiqueResponse annulerNote(Long id) {
        TestAcademique test = testAcademiqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Test non trouvé avec l'ID: " + id));
        if (test.getNoteObtenuePrecedente() == null) {
            throw new BusinessException("Aucune saisie récente à annuler pour ce test.");
        }
        test.setNoteObtenue(test.getNoteObtenuePrecedente());
        test.setNoteObtenuePrecedente(null);
        TestAcademique saved = testAcademiqueRepository.save(test);
        progressionDossierService.apresTests(saved.getCandidat().getId());
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

    public List<TestAcademiqueResponse> getTestsParGroupe(UUID groupeEpreuveId) {
        return testAcademiqueRepository.findByGroupeEpreuveId(groupeEpreuveId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    /** Sauvegarde un lot uniquement s'il appartient à l'épreuve affichée. */
    public List<TestAcademiqueResponse> enregistrerNotesGroupe(UUID groupeEpreuveId, Map<Long, Float> notes) {
        List<TestAcademique> lignes = testAcademiqueRepository.findByGroupeEpreuveId(groupeEpreuveId);
        Map<Long, TestAcademique> parId = lignes.stream()
                .collect(Collectors.toMap(TestAcademique::getId, test -> test));
        if (notes.isEmpty() || !parId.keySet().containsAll(notes.keySet())) {
            throw new BusinessException("Une note ne correspond pas à l'épreuve sélectionnée.");
        }
        notes.forEach((id, note) -> verifierNote(note));
        notes.forEach((id, note) -> {
            TestAcademique test = parId.get(id);
            test.setNoteObtenuePrecedente(test.getNoteObtenue());
            test.setNoteObtenue(note);
        });
        List<TestAcademique> enregistres = testAcademiqueRepository.saveAll(notes.keySet().stream().map(parId::get).toList());
        enregistres.stream().map(t -> t.getCandidat().getId()).distinct().forEach(progressionDossierService::apresTests);
        return enregistres.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /** Modifie les informations communes sans toucher aux notes ni aux inscrits. */
    public List<TestAcademiqueResponse> modifierGroupe(UUID groupeEpreuveId, TestAcademiqueRequest request) {
        List<TestAcademique> lignes = testAcademiqueRepository.findByGroupeEpreuveId(groupeEpreuveId);
        if (lignes.isEmpty()) throw new ResourceNotFoundException("Test académique introuvable.");
        lignes.forEach(test -> {
            test.setNomEpreuve(request.getNomEpreuve());
            test.setCoefficient(request.getCoefficient());
            test.setDateEpreuve(request.getDateEpreuve());
            test.setLieuOuLien(request.getLieuOuLien());
        });
        List<TestAcademique> enregistres = testAcademiqueRepository.saveAll(lignes);
        // Le coefficient pèse dans la moyenne : le modifier peut faire basculer un résultat.
        enregistres.stream().map(t -> t.getCandidat().getId()).distinct().forEach(progressionDossierService::apresTests);
        return enregistres.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /** Supprime exclusivement les inscriptions et notes de l'épreuve sélectionnée. */
    public void supprimerGroupe(UUID groupeEpreuveId) {
        List<TestAcademique> lignes = testAcademiqueRepository.findByGroupeEpreuveId(groupeEpreuveId);
        if (lignes.isEmpty()) throw new ResourceNotFoundException("Test académique introuvable.");
        testAcademiqueRepository.deleteAll(lignes);
        lignes.stream().map(t -> t.getCandidat().getId()).distinct().forEach(progressionDossierService::apresTests);
    }

    public void supprimerTest(Long id) {
        TestAcademique test = testAcademiqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Test non trouvé avec l'ID: " + id));
        Long candidatId = test.getCandidat().getId();
        testAcademiqueRepository.deleteById(id);
        progressionDossierService.apresTests(candidatId);
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
                .noteObtenuePrecedente(t.getNoteObtenuePrecedente())
                .noteObtenue(t.getNoteObtenue())
                .dateEpreuve(t.getDateEpreuve())
                .lieuOuLien(t.getLieuOuLien())
                .candidatId(t.getCandidat().getId())
                .candidatNom(t.getCandidat().getNom())
                .candidatPrenom(t.getCandidat().getPrenom())
                .numeroDossier(candidature != null ? candidature.getNumeroDossier() : null)
                .sessionCandidatureId(candidature != null ? candidature.getSessionCandidature().getId() : null)
                .sessionCandidatureLibelle(candidature != null ? candidature.getSessionCandidature().getLibelle() : null)
                .groupeEpreuveId(t.getGroupeEpreuveId())
                .build();
    }
}
