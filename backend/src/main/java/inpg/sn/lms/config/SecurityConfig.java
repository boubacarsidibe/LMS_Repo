package inpg.sn.lms.config;

import inpg.sn.lms.security.JwtRequestFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
                // Public : authentification, catalogue, contact
                .requestMatchers("/api/auth/**", "/api/recrutement/public/**", "/", "/error", "/login").permitAll()
                // Tout utilisateur connecté peut soumettre une candidature : le rôle
                // CANDIDAT n'est accordé qu'après une soumission réussie (voir
                // CandidatController#soumettreCandidature), donc pas encore présent
                // au moment où l'utilisateur atteint ces endpoints pour la première fois.
                .requestMatchers("/api/recrutement/candidat/**").authenticated()
                // Admin : gestion recrutement
                .requestMatchers("/api/admin/recrutement/**").hasRole("RESPONSABLE_ADMISSION")
                // Espace pédagogique : les opérations d'enseignant restent réservées aux formateurs.
                .requestMatchers("/api/espace-etudiant/formateur/**").hasRole("FORMATEUR")
                .requestMatchers("/api/espace-etudiant/**").hasAnyRole("ETUDIANT", "FORMATEUR")
                .anyRequest().authenticated()
                )
                
            
            
            .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
