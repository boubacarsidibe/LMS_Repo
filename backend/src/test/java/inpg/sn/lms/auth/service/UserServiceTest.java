package inpg.sn.lms.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserServiceTest {
    private final UserRepository repository = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final UserService service = new UserService(repository, mock(RoleRepository.class), encoder,
            mock(ResponsableAdmissionRepository.class));

    @Test void deconnexion_ne_reenregistre_pas_un_ancien_hash() {
        User stale = new User("alice", "ancien-hash", "alice@example.test");
        stale.setId(1L);
        User fresh = new User("alice", "nouveau-hash", "alice@example.test");
        fresh.setSessionVersion(5);
        when(repository.findById(1L)).thenReturn(Optional.of(fresh));
        assertThat(service.invaliderSessions(stale)).isSameAs(fresh);
        verify(repository).invaliderSessions(1L);
        verify(repository, never()).save(any());
    }

    @Test void changement_hash_et_revocation_dans_la_meme_ecriture() {
        User user = new User("alice", "hash", "alice@example.test");
        user.setId(1L);
        when(repository.findByEmail("alice@example.test")).thenReturn(user);
        when(encoder.encode("mot-de-passe-long")).thenReturn("nouveau-hash");
        service.changePassword(" ALICE@example.test ", "mot-de-passe-long");
        verify(repository).changerMotDePasse(1L, "nouveau-hash");
        verify(repository, never()).save(any());
    }

    @Test void mot_de_passe_invalide_ne_modifie_pas_le_compte() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.changePassword("alice@example.test", "court"));
        verifyNoInteractions(repository, encoder);
    }
}
