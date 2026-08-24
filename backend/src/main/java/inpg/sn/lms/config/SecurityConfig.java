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
                // Vérification publique d'un certificat par sa référence (page vers laquelle
                // pointe le QR code imprimé) : destinée à un tiers sans compte, qui n'obtient
                // qu'une DTO restreinte (voir CertificatPublicController), jamais l'entité.
                .requestMatchers("/api/certificats/public/**").permitAll()
                // Liste publique des écoles accréditées (TDR §F : « processus transparent ») —
                // même patron que la vérification de certificat, aucune donnée que le nom et
                // la date d'accréditation n'y est exposée (voir AccreditationPublicController).
                .requestMatchers("/api/accreditation/public/**").permitAll()
                // Administration des comptes : création de formateurs, listings nominatifs,
                // inscription directe d'un candidat ou d'un étudiant. Ces opérations
                // attribuent des rôles ou exposent des données personnelles.
                .requestMatchers(
                        "/api/auth/formateurs",
                        "/api/auth/getAllUsers",
                        "/api/auth/getAllCandidats",
                        "/api/auth/user",
                        "/api/auth/registerCandidat").hasRole("RESPONSABLE_ADMISSION")
                // Évaluation des demandes d'accréditation : réservée à ce rôle dédié, distinct
                // de RESPONSABLE_ADMISSION — un évaluateur n'a pas à gérer les candidatures
                // étudiantes, ni l'inverse. Déclarée AVANT le joker /api/admin/** ci-dessous,
                // qui sinon l'intercepterait en exigeant RESPONSABLE_ADMISSION à la place.
                .requestMatchers("/api/admin/accreditation/**").hasRole("EVALUATEUR_ACCREDITATION")
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
                // Tout utilisateur connecté peut soumettre une demande d'accréditation : le
                // rôle ROLE_EVALUATEUR_ACCREDITATION n'a rien à voir avec ce chemin (c'est le
                // rôle de qui évalue, pas de qui dépose) — même patron que candidat ci-dessus,
                // aucun rôle métier n'est requis pour déposer un dossier.
                .requestMatchers("/api/accreditation/ecole/**").authenticated()
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
                // Espace Alumni (TDR §C) : profil, situation professionnelle, relevés délivrés.
                .requestMatchers("/api/espace-alumni/**").hasRole("ALUMNI")
                .anyRequest().authenticated()
                )
                
            
            
            .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
