package inpg.sn.lms.espace_etudiant.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import inpg.sn.lms.espace_etudiant.entity.Message;
import inpg.sn.lms.espace_etudiant.entity.Sujet;
import inpg.sn.lms.espace_etudiant.enums.EnumStatutSujet;
import inpg.sn.lms.espace_etudiant.repository.MessageRepository;
import inpg.sn.lms.espace_etudiant.repository.SujetRepository;

@Service
public class SujetService {

    private final SujetRepository sujetRepository;
    private final MessageRepository messageRepository;

    public SujetService(SujetRepository sujetRepository, MessageRepository messageRepository) {
        this.sujetRepository = sujetRepository;
        this.messageRepository = messageRepository;
    }

    public List<Sujet> getAll() {
        return sujetRepository.findAll();
    }

    public Optional<Sujet> findById(Long id) {
        return sujetRepository.findById(id);
    }

    public Sujet save(Sujet sujet) {
        return sujetRepository.save(sujet);
    }

    public void deleteById(Long id) {
        sujetRepository.deleteById(id);
    }

    public Sujet addMessage(Sujet sujet, String auteurNom, String contenu, boolean marquerCommeResolu,
            String pieceJointeNomOriginal, String pieceJointeNomStocke, String pieceJointeTypeContenu) {
        Message message = new Message();
        message.setAuteurNom(auteurNom);
        message.setContenu(contenu);
        message.setDateCreation(LocalDateTime.now());
        message.setSujet(sujet);
        message.setPieceJointeNomOriginal(pieceJointeNomOriginal);
        message.setPieceJointeNomStocke(pieceJointeNomStocke);
        message.setPieceJointeTypeContenu(pieceJointeTypeContenu);
        sujet.getMessages().add(message);
        if (marquerCommeResolu) {
            sujet.setStatut(EnumStatutSujet.RESOLU);
        }
        return sujetRepository.save(sujet);
    }

    public Optional<Message> findMessageById(Long id) {
        return messageRepository.findById(id);
    }
}
