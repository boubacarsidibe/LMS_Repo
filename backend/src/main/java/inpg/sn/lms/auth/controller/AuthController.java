package inpg.sn.lms.auth.controller;

import inpg.sn.lms.auth.dto.AuthRequest;
import inpg.sn.lms.auth.dto.AuthResponse;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.service.OtpService;
import inpg.sn.lms.auth.service.UserService;
import inpg.sn.lms.security.JwtUtils;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtils jwtUtils;
    private final OtpService otpService;
    private final UserService userService;

    public AuthController(AuthenticationManager authenticationManager,
            UserDetailsService userDetailsService,
            JwtUtils jwtUtils, OtpService otpService, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtils = jwtUtils;
        this.otpService = otpService;
        this.userService = userService;
    }

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        
        
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "L'e-mail est requis."));
        }

        String otp = otpService.generateOtp(email);
        otpService.sendOtpEmail(email, otp);

        return ResponseEntity.ok(Map.of("message", "Code OTP envoyé par e-mail avec succès."));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");

        boolean isValid = otpService.validateOtp(email, code);

        if (isValid) {
            return ResponseEntity.ok(Map.of("message", "Vérification réussie !"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "Code OTP invalide ou expiré."));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            // 1. Récupération de l'utilisateur par e-mail
            User user = userService.findByEmail(request.getEmail());
            if (user == null) {
                // Si l'utilisateur n'existe pas, lever BadCredentials (sécurité : évite
                // l'énumération de comptes)
                throw new BadCredentialsException("Email ou mot de passe incorrect");
            }

            // 2. Authentification via le username extrait de l'utilisateur
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), request.getPassword()));

            // 3. Récupération des détails de l'utilisateur
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            

            // 5. Génération du JWT
            String jwt = jwtUtils.generateToken(userDetails);

            // Tu peux renvoyer le JWT ainsi qu'un indicateur informant le frontend de
            // demander l'OTP
            return ResponseEntity.ok(Map.of(
                    "token", jwt,
                    "message", "Authentification réussie. Un code OTP vous a été envoyé."));

        } catch (BadCredentialsException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Email ou mot de passe incorrect"));

        } catch (DisabledException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Votre compte est désactivé"));

        } catch (LockedException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Votre compte est verrouillé"));
        }
    }

    // 2. Vérification du Jeton (Pour le AuthGuard d'Angular)
    @GetMapping("/verify")
    public ResponseEntity<?> verifyToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String username = jwtUtils.extractUsername(token);

            if (username != null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Vérifie si le token est valide et non expiré
                if (jwtUtils.validateToken(token, userDetails)) {
                    return ResponseEntity.ok().build(); // HTTP 200 OK -> Le Guard Angular laisse passer
                }
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Jeton invalide ou expiré");
    }

    // 3. Récupérer l'utilisateur actuellement connecté
    @GetMapping("/current-user")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // Renvoie les détails de l'utilisateur (username, rôles, etc.)
        return ResponseEntity.ok(userDetails);
    }

    // 4. Déconnexion (Optionnel - Côté serveur si nécessaire)
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // En JWT sans état (stateless), la déconnexion se fait principalement côté
        // Angular
        // en supprimant le token du LocalStorage, mais cet endpoint confirme l'action.
        return ResponseEntity.ok().body("Déconnexion réussie");
    }
}