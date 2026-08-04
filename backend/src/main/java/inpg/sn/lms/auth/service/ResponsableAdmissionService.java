package inpg.sn.lms.auth.service;

import java.util.List;

import org.springframework.stereotype.Service;

import inpg.sn.lms.auth.entity.ResponsableAdmission;
import inpg.sn.lms.auth.repository.ResponsableAdmissionRepository;

@Service
public class ResponsableAdmissionService {

    private final ResponsableAdmissionRepository repository;

    public ResponsableAdmissionService(ResponsableAdmissionRepository repository) {
        this.repository = repository;
    }

    public List<ResponsableAdmission> findAll() {
        return repository.findAll();
    }

    public ResponsableAdmission findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Responsable Admission non trouvé avec l'ID: " + id));
    }

    public ResponsableAdmission save(ResponsableAdmission responsable) {
        return repository.save(responsable);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}