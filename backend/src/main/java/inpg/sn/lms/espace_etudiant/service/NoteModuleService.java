package inpg.sn.lms.espace_etudiant.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import inpg.sn.lms.espace_etudiant.entity.NoteModule;
import inpg.sn.lms.espace_etudiant.repository.NoteModuleRepository;

@Service
public class NoteModuleService {

    private final NoteModuleRepository noteModuleRepository;

    public NoteModuleService(NoteModuleRepository noteModuleRepository) {
        this.noteModuleRepository = noteModuleRepository;
    }

    public List<NoteModule> getByEtudiantId(Long etudiantId) {
        return noteModuleRepository.findByEtudiantIdOrderByDateEvaluationDesc(etudiantId);
    }

    public NoteModule save(NoteModule note) {
        return noteModuleRepository.save(note);
    }

    public Optional<NoteModule> findById(Long id) {
        return noteModuleRepository.findById(id);
    }

    public void deleteById(Long id) {
        noteModuleRepository.deleteById(id);
    }
}
