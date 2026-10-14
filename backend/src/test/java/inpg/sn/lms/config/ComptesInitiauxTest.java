package inpg.sn.lms.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import inpg.sn.lms.auth.dto.UserDto;
import inpg.sn.lms.auth.entity.Role;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.RoleRepository;
import inpg.sn.lms.auth.repository.UserRepository;
import inpg.sn.lms.auth.service.UserService;

/**
 * Sur une base neuve de production, aucun compte n'existait et personne ne pouvait en attribuer (audit du 2026-10-14) :
 * le démarrage crée les comptes du personnel déclarés par variables d'environnement.
 */
@ExtendWith(MockitoExtension.class)
class ComptesInitiauxTest {

    private static final String MOT_DE_PASSE = "Initial@2026!";

    @Mock private UserService userService;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;

    @BeforeEach
    void preparer() {
        lenient().when(userService.createUser(any())).thenAnswer(inv -> {
            UserDto dto = inv.getArgument(0);
            return new User(dto.getUsername(), "haché", dto.getEmail());
        });
        lenient().when(roleRepository.findByCode(any())).thenAnswer(inv -> {
            Role r = new Role();
            r.setCode(inv.getArgument(0));
            return r;
        });
    }

    private ComptesInitiaux comptes(String motDePasse, String admission, String pedagogie, String evaluateur) {
        return new ComptesInitiaux(userService, userRepository, roleRepository, motDePasse, admission, pedagogie, evaluateur);
    }

    @Test
    void cree_les_trois_comptes_du_personnel_avec_leur_role() {
        comptes(MOT_DE_PASSE, "Admission@INPG.sn", "pedagogie@inpg.sn", "evaluation@inpg.sn").run(null);

        ArgumentCaptor<UserDto> admission = ArgumentCaptor.forClass(UserDto.class);
        verify(userService).createResponsableAdmission(admission.capture());
        assertThat(admission.getValue().getEmail()).isEqualTo("admission@inpg.sn");
        assertThat(admission.getValue().getPassword()).isEqualTo(MOT_DE_PASSE);

        ArgumentCaptor<User> sauves = ArgumentCaptor.forClass(User.class);
        verify(userRepository, org.mockito.Mockito.times(2)).save(sauves.capture());
        assertThat(sauves.getAllValues()).extracting(u -> u.getRoles().iterator().next().getCode())
                .containsExactlyInAnyOrder("ROLE_RESPONSABLE_PEDAGOGIQUE", "ROLE_EVALUATEUR_ACCREDITATION");
    }

    /** Idempotent : à chaque redémarrage, un compte existant n'est ni recréé ni réinitialisé. */
    @Test
    void un_compte_existant_n_est_jamais_modifie() {
        when(userRepository.findByEmail("pedagogie@inpg.sn")).thenReturn(new User("pedago", "ancien", "pedagogie@inpg.sn"));

        comptes(MOT_DE_PASSE, "", "pedagogie@inpg.sn", "").run(null);

        verify(userService, never()).createUser(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void sans_e_mail_declare_rien_n_est_fait() {
        comptes("", "", "", "").run(null);

        verifyNoInteractions(userService);
    }

    @Test
    void un_e_mail_sans_mot_de_passe_arrete_le_demarrage() {
        assertThatThrownBy(() -> comptes("", "admission@inpg.sn", "", "").run(null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("APP_INIT_PASSWORD");
        verifyNoInteractions(userService);
    }

    @Test
    void un_mot_de_passe_faible_arrete_le_demarrage() {
        assertThatThrownBy(() -> comptes("azerty", "admission@inpg.sn", "", "").run(null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("APP_INIT_PASSWORD");
        verifyNoInteractions(userService);
    }
}
