package inpg.sn.lms.espace_etudiant.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import inpg.sn.lms.espace_etudiant.dto.CompteurParId;
import inpg.sn.lms.espace_etudiant.entity.SupportCours;
import inpg.sn.lms.espace_etudiant.entity.SupportPedagogique;
import inpg.sn.lms.espace_etudiant.repository.SupportCoursRepository;
import inpg.sn.lms.espace_etudiant.repository.SupportPedagogiqueRepository;

@Service
public class SupportService {

    private final SupportCoursRepository supportCoursRepository;
    private final SupportPedagogiqueRepository supportPedagogiqueRepository;

    public SupportService(SupportCoursRepository supportCoursRepository,
            SupportPedagogiqueRepository supportPedagogiqueRepository) {
        this.supportCoursRepository = supportCoursRepository;
        this.supportPedagogiqueRepository = supportPedagogiqueRepository;
    }

    public List<SupportCours> getAllCourseSupports() {
        return supportCoursRepository.findAll();
    }

    public List<SupportPedagogique> getAllPedagogicalSupports() {
        return supportPedagogiqueRepository.findAll();
    }

    public SupportPedagogique findPedagogicalById(UUID id) {
        return supportPedagogiqueRepository.findById(id).orElse(null);
    }

    public SupportCours saveCourseSupport(SupportCours supportCours) {
        return supportCoursRepository.save(supportCours);
    }

    public Optional<SupportCours> findCourseSupportById(Long id) {
        return supportCoursRepository.findById(id);
    }

    public void deleteCourseSupport(SupportCours supportCours) {
        supportCoursRepository.delete(supportCours);
    }

    public long countCourseSupports(Long coursId) {
        return supportCoursRepository.countByCoursId(coursId);
    }

    /** Comptage groupe : une seule requete pour toute une liste de cours. */
    public List<CompteurParId> compterSupportsParCours(List<Long> coursIds) {
        if (coursIds.isEmpty()) {
            return List.of();
        }
        return supportCoursRepository.compterParCours(coursIds);
    }

    public List<SupportCours> getCourseSupports(Long coursId) {
        return supportCoursRepository.findByCoursIdOrderByDatePublicationDesc(coursId);
    }

    public void deleteCourseSupports(Long coursId) {
        supportCoursRepository.deleteByCoursId(coursId);
    }
}
