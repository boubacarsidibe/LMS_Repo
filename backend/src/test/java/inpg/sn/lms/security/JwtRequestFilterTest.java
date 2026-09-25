package inpg.sn.lms.security;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.service.UserService;
import jakarta.servlet.FilterChain;

class JwtRequestFilterTest {
    private final JwtUtils jwt = mock(JwtUtils.class);
    private final UserService users = mock(UserService.class);
    private final JwtRequestFilter filter = new JwtRequestFilter(jwt, users);

    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    @Test void utilisateur_supprime_pendant_la_requete_reste_anonyme() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer test-token");
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);
        User user = new User("alice", "hash", "alice@example.test");
        when(jwt.extractUsername("test-token")).thenReturn("alice");
        when(users.findByUsername("alice")).thenReturn(user);
        when(jwt.validateToken("test-token", user)).thenReturn(true);
        when(users.loadUserByUsername("alice")).thenThrow(new UsernameNotFoundException("deleted"));
        filter.doFilter(request, response, chain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test void erreur_de_validation_ne_devient_pas_une_erreur_serveur() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer test-token");
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);
        when(jwt.extractUsername("test-token")).thenThrow(new io.jsonwebtoken.MalformedJwtException("invalid"));
        filter.doFilter(request, response, chain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }
}
