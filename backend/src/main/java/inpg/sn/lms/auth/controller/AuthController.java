package inpg.sn.lms.auth.controller;

import inpg.sn.lms.auth.dto.AuthRequest;
import inpg.sn.lms.auth.dto.AuthResponse;
import inpg.sn.lms.security.JwtUtils;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtils jwtUtils;

    public AuthController(AuthenticationManager authenticationManager,
            UserDetailsService userDetailsService,
            JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            // 1. Authentifier l'utilisateur via Spring Security (vérifie le mot de passe
            // hashé en BDD)
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            // 2. Charger les détails de l'utilisateur (rôles, username, etc.)
            final UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());

            // 3. Générer le jeton JWT
            final String jwt = jwtUtils.generateToken(userDetails);

            // 4. Renvoyer le token dans la réponse
            return ResponseEntity.ok(new AuthResponse(jwt));

        } catch (BadCredentialsException e) {
            // Si l'identifiant ou le mot de passe est incorrect
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Nom d'utilisateur ou mot de passe incorrect");
        }
    }
}