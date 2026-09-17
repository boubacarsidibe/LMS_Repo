package inpg.sn.lms.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.ResponsableAdmissionRepository;
import inpg.sn.lms.auth.repository.RoleRepository;
import inpg.sn.lms.auth.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ResponsableAdmissionRepository responsableAdmissionRepository;

    @Test
    @DisplayName("La déconnexion révoque les JWT existants via la version de session")
    void invalider_sessions_augmente_la_version_et_persiste_le_compte() {
        User user = new User("alice", "hash", "alice@example.test");
        user.setSessionVersion(4);
        when(userRepository.save(user)).thenReturn(user);

        UserService service = new UserService(userRepository, roleRepository, passwordEncoder,
                responsableAdmissionRepository);
        User resultat = service.invaliderSessions(user);

        assertThat(resultat.getSessionVersion()).isEqualTo(5);
        verify(userRepository).save(user);
    }
}
