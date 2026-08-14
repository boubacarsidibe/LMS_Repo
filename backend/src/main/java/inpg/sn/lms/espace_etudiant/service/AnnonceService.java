package inpg.sn.lms.espace_etudiant.service;

import java.util.List;

import org.springframework.stereotype.Service;

import inpg.sn.lms.espace_etudiant.entity.Annonce;
import inpg.sn.lms.espace_etudiant.repository.AnnonceRepository;

@Service
public class AnnonceService {

    private final AnnonceRepository annonceRepository;

    public AnnonceService(AnnonceRepository annonceRepository) {
        this.annonceRepository = annonceRepository;
    }

    public List<Annonce> getAll() {
        return annonceRepository.findAllByOrderByEpingleeDescDateCreationDesc();
    }

    public Annonce save(Annonce annonce) {
        return annonceRepository.save(annonce);
    }
}
