package inpg.sn.lms.config;

import inpg.sn.lms.security.JwtRequestFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Permet d'utiliser @PreAuthorize sur vos contrôleurs
public class SecurityConfig {

    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;
    private final JwtRequestFilter jwtRequestFilter;

    // Constructeur complet pour injecter les dépendances correctement
    public SecurityConfig(UserDetailsService userDetailsService, JwtRequestFilter jwtRequestFilter, PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.jwtRequestFilter = jwtRequestFilter;
        this.passwordEncoder = passwordEncoder;
    }

    

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            // API REST Stateless -> Pas de CSRF ni de Session
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            
            .authenticationProvider(authenticationProvider())
            
            
            .authorizeHttpRequests(auth -> auth
                // Public : strictement les opérations nécessaires avant d'être connecté.
                // Surtout PAS « /api/auth/** » en bloc : ce préfixe héberge aussi la
                // création de comptes formateur et les listings d'utilisateurs et de
                // candidats, qu'un tel joker rendrait accessibles anonymement.
                .requestMatchers(HttpMethod.POST,
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/send-otp",
                        "/api/auth/verify-otp",
                        "/api/auth/reset-password").permitAll()
                .requestMatchers("/api/recrutement/public/**", "/", "/error", "/login", "/uploads/**").permitAll()
                // Vérification d'un certificat depuis son QR code : doit rester joignable
                // sans compte, y compris par un tiers qui contrôle un diplôme (cf. TDR §B).
                .requestMatchers(HttpMethod.GET, "/api/verification-certificat/**").permitAll()
                // Administration des comptes : création de formateurs, listings nominatifs,
                // inscription directe d'un candidat ou d'un étudiant. Ces opérations
                // attribuent des rôles ou exposent des données personnelles.
                .requestMatchers(
                        "/api/auth/formateurs",
                        "/api/auth/getAllUsers",
                        "/api/auth/getAllCandidats",
                        "/api/auth/user",
                        "/api/auth/registerCandidat").hasRole("RESPONSABLE_ADMISSION")
                // Gestion des utilisateurs et des rôles : filet de sécurité pour que tout
                // futur endpoint ajouté sous /api/admin ne soit pas exposé par omission.
                .requestMatchers("/api/admin/**").hasRole("RESPONSABLE_ADMISSION")
                // Tout utilisateur connecté peut soumettre une candidature : le rôle
                // CANDIDAT n'est accordé qu'après une soumission réussie (voir
                // CandidatController#soumettreCandidature), donc pas encore présent
                // au moment où l'utilisateur atteint ces endpoints pour la première fois.
                .requestMatchers("/api/recrutement/candidat/**").authenticated()
                // Admin : gestion recrutement
                .requestMatchers("/api/admin/recrutement/**").hasRole("RESPONSABLE_ADMISSION")
                // Pilotage de l'offre pédagogique : cours et supports, tous formateurs confondus.
                .requestMatchers("/api/pedagogie/**").hasRole("RESPONSABLE_PEDAGOGIQUE")
                // La cloche est commune aux trois espaces : elle est adossée à l'utilisateur,
                // et chaque endpoint ne lit que les notifications du connecté. Déclarée AVANT
                // la règle générale ci-dessous, qui sinon en exclurait le responsable
                // pédagogique — l'ordre des règles décide, la plus précise doit venir d'abord.
                .requestMatchers("/api/espace-etudiant/notifications/**", "/api/espace-etudiant/notifications")
                    .hasAnyRole("ETUDIANT", "FORMATEUR", "RESPONSABLE_PEDAGOGIQUE")
                // Espace pédagogique : les opérations d'enseignant restent réservées aux formateurs.
                .requestMatchers("/api/espace-etudiant/formateur/**").hasRole("FORMATEUR")
                .requestMatchers("/api/espace-etudiant/**").hasAnyRole("ETUDIANT", "FORMATEUR")
                .anyRequest().authenticated()
                )
                
            
            
            .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
