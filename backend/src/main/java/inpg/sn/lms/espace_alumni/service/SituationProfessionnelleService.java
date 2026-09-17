package inpg.sn.lms.espace_alumni.service;

import java.util.List;
import org.springframework.stereotype.Service;

import inpg.sn.lms.auth.entity.AlumniProfil;
import inpg.sn.lms.espace_alumni.dto.SituationProfessionnelleDto;
import inpg.sn.lms.espace_alumni.dto.SituationProfessionnelleRequest;
import inpg.sn.lms.espace_alumni.entity.SituationProfessionnelle;
import inpg.sn.lms.espace_alumni.enums.EnumStatutEmploi;
import inpg.sn.lms.espace_alumni.repository.SituationProfessionnelleRepository;

/** Historique professionnel d'un alumni : lecture et écriture, cloisonnées par propriétaire. */
@Service
public class SituationProfessionnelleService {

    private final SituationProfessionnelleRepository repository;

    public SituationProfessionnelleService(SituationProfessionnelleRepository repository) {
        this.repository = repository;
    }

    public List<SituationProfessionnelleDto> lister(Long alumniProfilId) {
        return repository.findByAlumniProfilIdOrderByDateDebutDesc(alumniProfilId).stream()
                .map(this::toDto)
                .toList();
    }

    public SituationProfessionnelleDto creer(AlumniProfil profil, SituationProfessionnelleRequest requete) {
        SituationProfessionnelle situation = new SituationProfessionnelle(profil,
                nettoyer(requete.intitulePoste()), nettoyer(requete.entreprise()), requete.dateDebut(), requete.dateFin(),
                requete.statutEmploi());
        return toDto(repository.save(situation));
    }

    /** {@code null} si la situation n'existe pas ou n'appartient pas à ce profil (cloisonnement). */
    public SituationProfessionnelleDto modifier(Long id, AlumniProfil profil, SituationProfessionnelleRequest requete) {
        SituationProfessionnelle situation = trouverAppartenant(id, profil);
        if (situation == null) {
            return null;
        }
        situation.setIntitulePoste(nettoyer(requete.intitulePoste()));
        situation.setEntreprise(nettoyer(requete.entreprise()));
        situation.setDateDebut(requete.dateDebut());
        situation.setDateFin(requete.dateFin());
        situation.setStatutEmploi(requete.statutEmploi() == null ? EnumStatutEmploi.EN_EMPLOI : requete.statutEmploi());
        return toDto(repository.save(situation));
    }

    /** {@code false} si la situation n'existe pas ou n'appartient pas à ce profil. */
    public boolean supprimer(Long id, AlumniProfil profil) {
        SituationProfessionnelle situation = trouverAppartenant(id, profil);
        if (situation == null) {
            return false;
        }
        repository.delete(situation);
        return true;
    }

    /** Nul pour une situation sans poste (en recherche d'emploi, poursuite d'études, autre) — {@link String#trim()} planterait sur null. */
    private String nettoyer(String valeur) {
        return valeur == null ? null : valeur.trim();
    }

    private SituationProfessionnelle trouverAppartenant(Long id, AlumniProfil profil) {
        SituationProfessionnelle situation = repository.findById(id).orElse(null);
        if (situation == null || situation.getAlumniProfil() == null
                || !situation.getAlumniProfil().getId().equals(profil.getId())) {
            return null;
        }
        return situation;
    }

    private SituationProfessionnelleDto toDto(SituationProfessionnelle situation) {
        return new SituationProfessionnelleDto(
                situation.getId(),
                situation.getIntitulePoste(),
                situation.getEntreprise(),
                situation.getDateDebut(),
                situation.getDateFin(),
                situation.getStatutEmploi());
    }
}
