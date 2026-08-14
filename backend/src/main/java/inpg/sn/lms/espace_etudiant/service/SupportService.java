package inpg.sn.lms.espace_etudiant.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

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

    public long countCourseSupports(Long coursId) {
        return supportCoursRepository.countByCoursId(coursId);
    }

    public List<SupportCours> getCourseSupports(Long coursId) {
        return supportCoursRepository.findByCoursIdOrderByDatePublicationDesc(coursId);
    }

    public Optional<SupportCours> findCourseSupportById(Long id) {
        return supportCoursRepository.findById(id);
    }

    public void deleteCourseSupport(SupportCours support) {
        supportCoursRepository.delete(support);
    }

    public void deleteCourseSupports(Long coursId) {
        supportCoursRepository.deleteByCoursId(coursId);
    }
}
