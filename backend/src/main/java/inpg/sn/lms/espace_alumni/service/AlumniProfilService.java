package inpg.sn.lms.espace_alumni.service;

import org.springframework.stereotype.Service;

import inpg.sn.lms.auth.entity.AlumniProfil;
import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.entity.Role;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.AlumniProfilRepository;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.auth.repository.RoleRepository;
import inpg.sn.lms.auth.repository.UserRepository;
import inpg.sn.lms.espace_alumni.dto.AlumniProfilDto;
import inpg.sn.lms.espace_alumni.dto.AlumniProfilUpdateRequest;

/**
 * Passage d'un étudiant à l'espace Alumni, et gestion de sa fiche.
 *
 * <p>Suit exactement le patron déjà en place pour Candidat → Étudiant
 * ({@code AdminRecrutementController#inscrireCommeEtudiant}) : un compte ne détient
 * qu'un seul rôle à la fois, le rôle est donc basculé (les rôles existants sont
 * vidés puis {@code ROLE_ALUMNI} est ajouté) et la ligne {@code Etudiant} d'origine
 * n'est PAS supprimée — elle reste la source du relevé de notes délivré, qui
 * référence {@code etudiants.id_user} sans savoir ce qu'est un alumni.
 */
@Service
public class AlumniProfilService {

    private final AlumniProfilRepository alumniProfilRepository;
    private final EtudiantRepository etudiantRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public AlumniProfilService(AlumniProfilRepository alumniProfilRepository, EtudiantRepository etudiantRepository,
            RoleRepository roleRepository, UserRepository userRepository) {
        this.alumniProfilRepository = alumniProfilRepository;
        this.etudiantRepository = etudiantRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    /**
     * Ouvre l'accès alumni à un étudiant, et retire son accès étudiant.
     *
     * <p>Idempotent : rejouer l'opération sur un compte déjà alumni ne fait rien de
     * plus. Refuse un étudiant introuvable plutôt que de lever une exception opaque.
     */
    public AlumniProfil promouvoir(Long etudiantId) {
        Etudiant etudiant = etudiantRepository.findById(etudiantId).orElse(null);
        if (etudiant == null || etudiant.getUser() == null) {
            return null;
        }
        User user = etudiant.getUser();

        boolean dejaAlumni = user.getRoles().stream().anyMatch(r -> "ROLE_ALUMNI".equals(r.getCode()));
        if (!dejaAlumni) {
            Role roleAlumni = roleRepository.findByCode("ROLE_ALUMNI");
            if (roleAlumni != null) {
                user.getRoles().clear();
                user.addRole(roleAlumni);
                userRepository.save(user);
            }
        }

        return alumniProfilRepository.findByUserId(user.getId())
                .orElseGet(() -> alumniProfilRepository.save(new AlumniProfil(user)));
    }

    public AlumniProfilDto toDto(AlumniProfil profil) {
        Etudiant etudiant = etudiantRepository.findByUserId(profil.getId()).orElse(null);
        return new AlumniProfilDto(
                profil.getId(),
                etudiant == null ? null : etudiant.getNom(),
                etudiant == null ? null : etudiant.getPrenom(),
                etudiant == null ? null : etudiant.getMatricule(),
                etudiant == null || etudiant.getPromotion() == null ? null : etudiant.getPromotion().getLibelle(),
                profil.getPosteActuel(),
                profil.getEntreprise(),
                profil.getSecteurActivite(),
                profil.isVisible(),
                profil.getDateCreation());
    }

    public AlumniProfil mettreAJour(AlumniProfil profil, AlumniProfilUpdateRequest requete) {
        profil.setPosteActuel(vide(requete.posteActuel()) ? null : requete.posteActuel().trim());
        profil.setEntreprise(vide(requete.entreprise()) ? null : requete.entreprise().trim());
        profil.setSecteurActivite(vide(requete.secteurActivite()) ? null : requete.secteurActivite().trim());
        profil.setVisible(requete.visible());
        return alumniProfilRepository.save(profil);
    }

    private boolean vide(String valeur) {
        return valeur == null || valeur.isBlank();
    }
}
