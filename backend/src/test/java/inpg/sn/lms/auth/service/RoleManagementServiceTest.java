package inpg.sn.lms.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import inpg.sn.lms.auth.dto.RoleChangeRequest;
import inpg.sn.lms.auth.entity.Role;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.auth.repository.FormateurRepository;
import inpg.sn.lms.auth.repository.RoleRepository;
import inpg.sn.lms.auth.repository.UserRepository;

/**
 * Le responsable admission pouvait donner n'importe quel rôle à n'importe qui — y compris responsable pédagogique ou
 * évaluateur — et retirer le leur aux autres responsables (audit du 2026-10-12, C3 ; reproduit le 2026-10-14).
 */
@ExtendWith(MockitoExtension.class)
class RoleManagementServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private FormateurRepository formateurRepository;
    @Mock private EtudiantRepository etudiantRepository;
    @InjectMocks private RoleManagementService service;

    private User cible;

    @BeforeEach
    void preparer() {
        cible = new User("awa", "x", "awa@example.sn");
        cible.setId(7L);
        cible.addRole(role("ROLE_CANDIDAT"));
        lenient().when(userRepository.findById(7L)).thenReturn(Optional.of(cible));
        lenient().when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(roleRepository.findByCode(any())).thenAnswer(inv -> role(inv.getArgument(0)));
    }

    private static Role role(String code) {
        Role r = new Role();
        r.setCode(code);
        r.setLibelle(code);
        return r;
    }

    private static RoleChangeRequest demande(String code) {
        RoleChangeRequest r = new RoleChangeRequest();
        r.setRoles(List.of(code));
        return r;
    }

    @Test
    void les_roles_du_personnel_ne_s_attribuent_pas_depuis_l_application() {
        for (String code : List.of("ROLE_RESPONSABLE_PEDAGOGIQUE", "ROLE_RESPONSABLE_ADMISSION",
                "ROLE_EVALUATEUR_ACCREDITATION", "ROLE_ALUMNI", "ROLE_ECOLE_FORMATION", "ROLE_ENTREPRISE")) {
            assertThatThrownBy(() -> service.changerRole(7L, demande(code), "admin_admission"))
                    .as(code).isInstanceOf(AccessDeniedException.class);
        }
        assertThat(cible.getRoles()).extracting(Role::getCode).containsExactly("ROLE_CANDIDAT");
        verify(userRepository, never()).save(any());
    }

    @Test
    void candidat_etudiant_formateur_restent_attribuables() {
        for (String code : List.of("ROLE_ETUDIANT", "ROLE_FORMATEUR", "ROLE_CANDIDAT")) {
            User modifie = service.changerRole(7L, demande(code), "admin_admission");
            assertThat(modifie.getRoles()).extracting(Role::getCode).as(code).containsExactly(code);
        }
    }

    @Test
    void on_ne_modifie_pas_son_propre_compte() {
        cible.setUsername("admin_admission");

        assertThatThrownBy(() -> service.changerRole(7L, demande("ROLE_FORMATEUR"), "admin_admission"))
                .isInstanceOf(AccessDeniedException.class).hasMessageContaining("propre compte");
        verify(userRepository, never()).save(any());
    }

    @Test
    void le_compte_d_un_responsable_ne_se_retrograde_pas() {
        for (String code : List.of("ROLE_RESPONSABLE_PEDAGOGIQUE", "ROLE_RESPONSABLE_ADMISSION", "ROLE_EVALUATEUR_ACCREDITATION")) {
            cible.getRoles().clear();
            cible.addRole(role(code));

            assertThatThrownBy(() -> service.changerRole(7L, demande("ROLE_CANDIDAT"), "admin_admission"))
                    .as(code).isInstanceOf(AccessDeniedException.class);
            assertThat(cible.getRoles()).extracting(Role::getCode).containsExactly(code);
        }
        verify(userRepository, never()).save(any());
    }

    @Test
    void un_role_inconnu_ou_multiple_reste_refuse() {
        RoleChangeRequest deux = new RoleChangeRequest();
        deux.setRoles(List.of("ROLE_ETUDIANT", "ROLE_FORMATEUR"));
        assertThatThrownBy(() -> service.changerRole(7L, deux, "x")).isInstanceOf(IllegalArgumentException.class);
        when(roleRepository.findByCode("ROLE_ETUDIANT")).thenReturn(null);
        assertThatThrownBy(() -> service.changerRole(7L, demande("ROLE_ETUDIANT"), "x")).isInstanceOf(IllegalArgumentException.class);
    }
}
