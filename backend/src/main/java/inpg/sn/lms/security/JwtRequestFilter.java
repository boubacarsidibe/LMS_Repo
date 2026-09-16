package inpg.sn.lms.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;


import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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
    

    // JwtRequestFilter(JwtUtils jwtUtils,UserDetailsService jwtUserDetailsService) {
    //     this.jwtUtils = jwtUtils;
    //     this.jwtUserDetailsService = jwtUserDetailsService;
    // }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        final String requestTokenHeader = request.getHeader("Authorization");

        String username = null;
        String jwtToken = null;

        // Extraction du token
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);

            try {
                username = jwtUtils.extractUsername(jwtToken);
            } catch (Exception e) {
                // Token invalide / expiré : on laisse la requête continuer sans
                // authentification. Le point d'entrée déclaré dans SecurityConfig
                // renverra alors un 401 (et non un 403), que le front interprète
                // comme une session expirée.
                logger.debug("JWT invalide ou expiré : " + e.getMessage());
            }
        }

        // Validation + authentification
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            User user = userService.findByUsername(username);

            if (user != null && jwtUtils.validateToken(jwtToken, user)) {
                UserDetails userDetails = this.userService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // Continuer la chaîne de filtres
        chain.doFilter(request, response);
    }
}
