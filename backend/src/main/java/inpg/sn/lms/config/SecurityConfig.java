package inpg.sn.lms.config;

import inpg.sn.lms.security.JwtRequestFilter;
import jakarta.servlet.http.HttpServletResponse;
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
            // Jeton absent, illisible ou expiré -> 401 (et non le 403 par défaut de
            // Spring Security). Sans ce point d'entrée, JwtRequestFilter laisse passer
            // la requête non authentifiée et l'autorisation répond 403 : le front, qui
            // ne réagit qu'au 401, ne détecte alors jamais la session expirée et les
            // écrans « ne chargent plus » jusqu'au rechargement manuel. Un utilisateur
            // authentifié mais sans le rôle requis continue de recevoir 403.
            .exceptionHandling(handling -> handling.authenticationEntryPoint(
                    (request, response, authException) ->
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentification requise")))
            
            
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
                .requestMatchers("/api/recrutement/public/**", "/api/recrutement-formateurs/public/**",
                        "/", "/error", "/login", "/uploads/**").permitAll()
                // Vérification publique d'un certificat par sa référence (page vers laquelle
                // pointe le QR code imprimé) : destinée à un tiers sans compte, qui n'obtient
                // qu'une DTO restreinte (voir CertificatPublicController), jamais l'entité.
                .requestMatchers("/api/certificats/public/**").permitAll()
                // Liste des écoles accréditées (voir AccreditationPublicController) : le TDR §F
                // demandait une liste publique (« processus transparent »), mais décision produit
                // du 2026-08-25 (LOG_BLOQUANT.md) de la réserver aux comptes connectés — tout rôle
                // confondu, pas de restriction par rôle, d'où la chute sur .anyRequest().authenticated()
                // plus bas plutôt qu'un permitAll ici.
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
                // ApplicationController (vue candidatures/relances) ne portait que la protection
                // de méthode @PreAuthorize, sans règle d'URL de secours — contrairement à son
                // voisin /api/admin/recrutement/** ci-dessus. Alignée sur le même rôle.
                .requestMatchers("/api/applications/**").hasRole("RESPONSABLE_ADMISSION")
                // Tout utilisateur connecté peut soumettre une demande d'accréditation : le
                // rôle ROLE_EVALUATEUR_ACCREDITATION n'a rien à voir avec ce chemin (c'est le
                // rôle de qui évalue, pas de qui dépose) — même patron que candidat ci-dessus,
                // aucun rôle métier n'est requis pour déposer un dossier.
                .requestMatchers("/api/accreditation/ecole/**").authenticated()
                // Tout utilisateur connecté peut soumettre une demande entreprise : le rôle
                // ROLE_ENTREPRISE n'est accordé qu'à la première soumission réussie — même
                // patron que candidat et école de formation ci-dessus.
                .requestMatchers("/api/entreprise/**").authenticated()
                // Pilotage de l'offre pédagogique : cours et supports, tous formateurs confondus.
                // Couvre aussi /api/pedagogie/entreprises/** (PedagogieEntrepriseController).
                .requestMatchers("/api/pedagogie/**").hasRole("RESPONSABLE_PEDAGOGIQUE")
                // La cloche est commune aux trois espaces : elle est adossée à l'utilisateur,
                // et chaque endpoint ne lit que les notifications du connecté. Déclarée AVANT
                // la règle générale ci-dessous, qui sinon en exclurait le responsable
                // pédagogique — l'ordre des règles décide, la plus précise doit venir d'abord.
                .requestMatchers("/api/espace-etudiant/notifications/**", "/api/espace-etudiant/notifications")
                    .hasAnyRole("ETUDIANT", "FORMATEUR", "RESPONSABLE_PEDAGOGIQUE", "RESPONSABLE_ADMISSION")
                // Espace pédagogique : les opérations d'enseignant restent réservées aux formateurs.
                .requestMatchers("/api/espace-etudiant/formateur/**").hasRole("FORMATEUR")
                .requestMatchers("/api/espace-etudiant/**").hasAnyRole("ETUDIANT", "FORMATEUR")
                // Espace Alumni (TDR §C) : profil, situation professionnelle, relevés délivrés.
                .requestMatchers("/api/espace-alumni/**").hasRole("ALUMNI")
                // Suivi des activités du personnel INPG (TDR §I) : réservé au responsable
                // pédagogique, qui pilote seul ce suivi (décision produit). Le formateur et
                // le responsable admission n'y ont plus accès.
                .requestMatchers("/api/personnel/**").hasRole("RESPONSABLE_PEDAGOGIQUE")
                // Gestion des rendez-vous (TDR §J) : « chaque utilisateur » peut consulter le
                // personnel et demander un rendez-vous — pas de restriction de rôle au niveau de
                // l'URL, les endpoints de gestion de créneaux/réponse restent verrouillés par
                // @PreAuthorize au niveau méthode (RendezVousController).
                .requestMatchers("/api/rendez-vous/**").authenticated()
                .anyRequest().authenticated()
                )
                
            
            
            .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
