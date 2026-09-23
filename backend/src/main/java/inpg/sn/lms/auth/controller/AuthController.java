package inpg.sn.lms.auth.controller;

import inpg.sn.lms.auth.dto.AuthRequest;
import inpg.sn.lms.auth.dto.FormateurDto;
import inpg.sn.lms.auth.entity.Formateur;
import inpg.sn.lms.auth.service.FormateurService;
import inpg.sn.lms.auth.service.PasswordPolicy;
import inpg.sn.lms.auth.service.LoginAttemptService;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.service.OtpService;
import inpg.sn.lms.auth.service.UserService;
import inpg.sn.lms.security.JwtUtils;

import java.util.Map;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final OtpService otpService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final FormateurService formateurService;
    private final LoginAttemptService loginAttemptService;

    public AuthController(AuthenticationManager authenticationManager,
            JwtUtils jwtUtils, OtpService otpService, UserService userService, PasswordEncoder passwordEncoder,
            FormateurService formateurService, LoginAttemptService loginAttemptService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.otpService = otpService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.formateurService = formateurService;
        this.loginAttemptService = loginAttemptService;
    }

    /**
     * Création de compte publique (le seul point d'entrée anonyme avec /login).
     *
     * <p>Vivait dans un contrôleur à part, {@code CrudControlleur}, aux côtés de cinq
     * endpoints qu'aucun écran n'appelait et qui sérialisaient les entités JPA brutes.
     * Ces cinq-là ont été retirés ; l'inscription rejoint ici la connexion, dont elle
     * est le pendant.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody inpg.sn.lms.auth.dto.UserDto userDto) {
        try {
            User newUser = userService.createUser(userDto);
            String token = jwtUtils.generateToken(newUser);
            Map<String, Object> user = Map.of(
                    "id", newUser.getId(),
                    "username", newUser.getUsername(),
                    "email", newUser.getEmail(),
                    "roles", newUser.getRoles().stream().map(role -> role.getCode()).toList(),
                    "typeInscription", newUser.getTypeInscription().name());
            return ResponseEntity.ok(Map.of("token", token, "user", user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Impossible de créer le compte pour le moment."));
        }
    }

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        if (email == null || email.isBlank() || email.length() > 254) {
            return ResponseEntity.badRequest().body(Map.of("message", "L'e-mail est requis."));
        }

        email = email.trim().toLowerCase(Locale.ROOT);
        String otp;
        try {
            otp = otpService.generateOtp(email);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", e.getMessage()));
        }
        try {
            if (userService.findByEmail(email) != null) {
                otpService.sendOtpEmail(email, otp);
            }
        } catch (MailException e) {
            // Réponse identique pour ne pas révéler si le compte existe.
            otpService.invalidateOtp(email);
            LOGGER.warn("Envoi du code de récupération impossible : service SMTP indisponible.");
        }

        return ResponseEntity.ok(Map.of("message", "Si un compte correspond à cette adresse, un code a été envoyé."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");
        String newPassword = request.get("newPassword");

        if (email == null || email.isBlank() || email.length() > 254) {
            return ResponseEntity.badRequest().body(Map.of("message", "L'e-mail est requis."));
        }
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le code de vérification est requis."));
        }
        try {
            // Cette vérification doit précéder la consommation de l'OTP : un mot
            // de passe corrigé par l'utilisateur ne doit pas exiger un nouvel e-mail.
            PasswordPolicy.exigerMotDePasseRobuste(newPassword);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
        email = email.trim().toLowerCase(Locale.ROOT);
        boolean otpValide = otpService.validateOtp(email, code);
        if (!otpValide) {
            return ResponseEntity.badRequest().body(Map.of("message", "Code de vérification invalide ou expiré."));
        }

        User user = userService.findByEmail(email);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Code de vérification invalide ou expiré."));
        }

        userService.changePassword(email, newPassword);
        return ResponseEntity.ok(Map.of("message", "Mot de passe réinitialisé avec succès."));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank() || request.getEmail().length() > 254
                || request.getPassword() == null || request.getPassword().isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Email ou mot de passe incorrect"));
        }
        if (!loginAttemptService.autoriser(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", "Trop de tentatives de connexion. Réessayez dans 15 minutes."));
        }
        try {
            // 1. Récupération de l'utilisateur par e-mail
            User user = userService.findByEmail(request.getEmail());
            if (user == null) {
                // Si l'utilisateur n'existe pas, lever BadCredentials (sécurité : évite
                // l'énumération de comptes)
                throw new BadCredentialsException("Email ou mot de passe incorrect");
            }

            // 2. Authentification via le username extrait de l'utilisateur
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), request.getPassword()));

            // 3. Récupération des détails de l'utilisateur
            // 4. Génération du JWT
            String jwt = jwtUtils.generateToken(user);

            // 5. Construction de l'objet utilisateur pour le frontend
            Map<String, Object> userMap = Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "roles", user.getRoles().stream()
                            .map(role -> role.getCode())
                            .toList());

            // 6. Renvoyer le token + l'utilisateur
            return ResponseEntity.ok(Map.of(
                    "token", jwt,
                    "user", userMap,
                    "message", "Authentification réussie."));

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
    public ResponseEntity<?> verifyToken(@AuthenticationPrincipal UserDetails userDetails) {
        return userDetails == null
                ? ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Jeton invalide ou expiré")
                : ResponseEntity.ok().build();
    }

    // 3. Récupérer l'utilisateur actuellement connecté
    @GetMapping("/current-user")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userService.findByUsername(userDetails.getUsername());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(toUserMap(user));
    }

    // 4. Déconnexion : invalide aussi le jeton côté serveur, afin qu'un jeton
    // copié ne reste pas utilisable jusqu'à son expiration.
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userService.findByUsername(userDetails.getUsername());
        if (user != null) {
            userService.invaliderSessions(user);
        }
        return ResponseEntity.ok().body("Déconnexion réussie");
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Utilisateur non authentifié"));
        }
        String currentPassword = request.get("currentPassword");
        String newPassword = request.get("newPassword");

        User user = userService.findByUsername(userDetails.getUsername());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Utilisateur non trouvé"));
        }
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Mot de passe actuel incorrect."));
        }
        try {
            PasswordPolicy.exigerMotDePasseRobuste(newPassword);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
        userService.changePassword(user.getEmail(), newPassword);
        return ResponseEntity.ok(Map.of("message", "Mot de passe modifié avec succès."));
    }

    /**
     * Crée un compte formateur. Réservé à l'administration : l'opération attribue
     * le rôle FORMATEUR, qui ouvre l'accès à l'ensemble des dossiers étudiants et
     * des supports de cours.
     */
    @PostMapping("/formateurs")
    @PreAuthorize("hasAuthority('ROLE_RESPONSABLE_ADMISSION')")
    public ResponseEntity<?> createFormateur(@RequestBody FormateurDto formateurDto) {
        try {
            Formateur created = formateurService.createFormateur(formateurDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "id", created.getId(),
                    "message", "Formateur créé avec succès."));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la création du formateur."));
        }
    }

    /**
     * Réservé à RESPONSABLE_ADMISSION, comme la création juste au-dessus : c'est
     * {@code SecurityConfig} qui décide en dernier ressort (le filtre d'URL
     * rejette une requête RESPONSABLE_PEDAGOGIQUE avant même d'atteindre cette
     * annotation), donc l'autoriser ici sans l'autoriser là serait une autorité
     * inatteignable, jamais vérifiée par aucun test. Le responsable pédagogique
     * dispose déjà de son propre endpoint dédié pour ce besoin :
     * {@code GET /api/pedagogie/formateurs} (PedagogieController), non utilisé
     * par aucun écran frontend à ce jour — l'écart a été repéré lors d'un audit
     * de tous les endpoints, sans caller réel affecté par ce resserrement.
     */
    @GetMapping("/formateurs")
    @PreAuthorize("hasAuthority('ROLE_RESPONSABLE_ADMISSION')")
    public ResponseEntity<?> getAllFormateurs() {
        return ResponseEntity.ok(formateurService.getAllFormateurs());
    }

    private Map<String, Object> toUserMap(User user) {
        return Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "roles", user.getRoles().stream().map(role -> role.getCode()).toList());
    }
}
