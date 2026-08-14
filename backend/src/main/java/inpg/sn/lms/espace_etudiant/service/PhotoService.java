package inpg.sn.lms.espace_etudiant.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import inpg.sn.lms.espace_etudiant.entity.Photo;
import inpg.sn.lms.espace_etudiant.repository.PhotoRepository;

@Service
public class PhotoService {

    private final PhotoRepository photoRepository;

    public PhotoService(PhotoRepository photoRepository) {
        this.photoRepository = photoRepository;
    }

    public List<Photo> getAll() {
        return photoRepository.findAllByOrderByDateAjoutDesc();
    }

    public Optional<Photo> findById(Long id) {
        return photoRepository.findById(id);
    }

    public Photo save(Photo photo) {
        return photoRepository.save(photo);
    }
}
