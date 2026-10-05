package inpg.sn.lms.security;

import java.io.IOException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.service.UserService;

@AllArgsConstructor
@Component
public class JwtRequestFilter extends OncePerRequestFilter {
    private final JwtUtils jwtUtils;
    private final UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String token = header.substring(7);
                String username = jwtUtils.extractUsername(token);
                User user = username == null ? null : userService.findByUsername(username);
                if (user != null && jwtUtils.validateToken(token, user)) {
                    UserDetails details = userService.loadUserByUsername(username);
                    if (details.isEnabled() && details.isAccountNonLocked()
                            && details.isAccountNonExpired() && details.isCredentialsNonExpired()) {
                        var authentication = new UsernamePasswordAuthenticationToken(
                                details, null, details.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
                // Aucun détail du jeton dans les logs. SecurityConfig produit le 401.
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
