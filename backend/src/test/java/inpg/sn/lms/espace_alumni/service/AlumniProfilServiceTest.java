package inpg.sn.lms.espace_alumni.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import inpg.sn.lms.auth.entity.AlumniProfil;
import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.entity.Role;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.AlumniProfilRepository;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.auth.repository.RoleRepository;
import inpg.sn.lms.auth.repository.UserRepository;
import inpg.sn.lms.espace_alumni.dto.AlumniProfilUpdateRequest;

/**
 * Conversion Étudiant → Alumni.
 *
 * <p>Suit le même patron que la conversion Candidat → Étudiant déjà en place
 * ({@code AdminRecrutementController#inscrireCommeEtudiant}) : un compte ne détient
 * qu'un seul rôle à la fois, et rejouer l'opération sur un compte déjà converti ne
 * doit rien faire de plus (idempotence attendue à chaque passage de la routine
 * autonome sur ce dépôt, cf. CLAUDE.md §5).
 */
@ExtendWith(MockitoExtension.class)
class AlumniProfilServiceTest {

    @Mock
    private AlumniProfilRepository alumniProfilRepository;
    @Mock
    private EtudiantRepository etudiantRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserRepository userRepository;

    private AlumniProfilService service;

    @BeforeEach
    void setUp() {
        service = new AlumniProfilService(alumniProfilRepository, etudiantRepository, roleRepository, userRepository);
    }

    private User utilisateur(long id) {
        User user = new User("awa_ndoye", "hash", "awa.ndoye@inpg.sn");
        user.setId(id);
        return user;
    }

    private Role role(String code) {
        Role role = new Role();
        role.setCode(code);
        return role;
    }

    @Test
    @DisplayName("La promotion retire ROLE_ETUDIANT au profit de ROLE_ALUMNI, sans supprimer la fiche étudiant")
    void promouvoirBasculeLeRoleEtCreeLeProfil() {
        User user = utilisateur(42L);
        user.addRole(role("ROLE_ETUDIANT"));
        Etudiant etudiant = new Etudiant(user, "Ndoye", "Awa", "770000000");
        Role roleAlumni = role("ROLE_ALUMNI");

        when(etudiantRepository.findById(42L)).thenReturn(Optional.of(etudiant));
        when(roleRepository.findByCode("ROLE_ALUMNI")).thenReturn(roleAlumni);
        when(alumniProfilRepository.findByUserId(42L)).thenReturn(Optional.empty());
        when(alumniProfilRepository.save(any(AlumniProfil.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AlumniProfil profil = service.promouvoir(42L);

        assertThat(profil).isNotNull();
        assertThat(profil.getUser()).isSameAs(user);
        assertThat(user.getRoles()).extracting(Role::getCode).containsExactly("ROLE_ALUMNI");
        verify(userRepository).save(user);
        // La ligne Etudiant reste en base : c'est elle qui garde le matricule et
        // permet à ScolariteController comme à AlumniController de retrouver le
        // relevé délivré (voir migration V12).
        verify(etudiantRepository, never()).delete(any(Etudiant.class));
    }

    @Test
    @DisplayName("Rejouer la promotion sur un compte déjà alumni est sans effet")
    void promouvoirEstIdempotent() {
        User user = utilisateur(7L);
        user.addRole(role("ROLE_ALUMNI"));
        Etudiant etudiant = new Etudiant(user, "Diop", "Awa", "770000001");
        AlumniProfil profilExistant = new AlumniProfil(user);

        when(etudiantRepository.findById(7L)).thenReturn(Optional.of(etudiant));
        when(alumniProfilRepository.findByUserId(7L)).thenReturn(Optional.of(profilExistant));

        AlumniProfil profil = service.promouvoir(7L);

        assertThat(profil).isSameAs(profilExistant);
        verify(roleRepository, never()).findByCode(anyString());
        verify(userRepository, never()).save(any());
        verify(alumniProfilRepository, never()).save(any());
    }

    @Test
    @DisplayName("Étudiant introuvable : aucune écriture, retourne null plutôt que de lever une exception")
    void promouvoirEtudiantIntrouvable() {
        when(etudiantRepository.findById(99L)).thenReturn(Optional.empty());

        AlumniProfil profil = service.promouvoir(99L);

        assertThat(profil).isNull();
        verifyNoInteractions(alumniProfilRepository, roleRepository, userRepository);
    }

    @Test
    @DisplayName("mettreAJour : les champs vides sont ramenés à null plutôt qu'à une chaîne vide")
    void mettreAJourVideLesChampsBlancs() {
        User user = utilisateur(5L);
        AlumniProfil profil = new AlumniProfil(user);
        when(alumniProfilRepository.save(profil)).thenReturn(profil);

        AlumniProfil misAJour = service.mettreAJour(profil,
                new AlumniProfilUpdateRequest("  ", "Petrosen", "   ", false));

        assertThat(misAJour.getPosteActuel()).isNull();
        assertThat(misAJour.getEntreprise()).isEqualTo("Petrosen");
        assertThat(misAJour.getSecteurActivite()).isNull();
        assertThat(misAJour.isVisible()).isFalse();
    }
}
