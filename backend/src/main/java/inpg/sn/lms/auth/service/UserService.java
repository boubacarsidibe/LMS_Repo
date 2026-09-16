package inpg.sn.lms.auth.service;


import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import inpg.sn.lms.auth.entity.ResponsableAdmission;
import inpg.sn.lms.auth.entity.Role;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.ResponsableAdmissionRepository;
import inpg.sn.lms.auth.repository.RoleRepository;
import inpg.sn.lms.auth.repository.UserRepository;

import java.util.List;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ResponsableAdmissionRepository responsableAdmissionRepository;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, ResponsableAdmissionRepository responsableAdmissionRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.responsableAdmissionRepository = responsableAdmissionRepository;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Crée un utilisateur sans rôle métier (inscription publique). Le rôle
     * CANDIDAT n'est plus attribué ici : il l'est uniquement lors de la
     * soumission réussie d'une candidature (CandidatController#soumettreCandidature),
     * pour qu'un simple inscrit ne soit pas considéré candidat avant d'avoir
     * réellement postulé.
     */
    public User createUser(inpg.sn.lms.auth.dto.UserDto userDto) {
        if (userDto.getEmail() == null || userDto.getEmail().isBlank()
                || userDto.getUsername() == null || userDto.getUsername().isBlank()
                || userDto.getPassword() == null || userDto.getPassword().isBlank()) {
            throw new IllegalArgumentException("Le nom d'utilisateur, l'e-mail et le mot de passe sont requis.");
        }

        PasswordPolicy.exigerMotDePasseRobuste(userDto.getPassword());

        String email = userDto.getEmail().trim().toLowerCase();
        if (userRepository.findByEmail(email) != null) {
            throw new IllegalArgumentException("Cet e-mail est déjà utilisé.");
        }
        String baseUsername = userDto.getUsername().trim();
        String username = baseUsername;
        int suffix = 1;
        while (userRepository.findByUsername(username) != null) {
            username = baseUsername + suffix;
            suffix++;
        }
        User newUser = userDto.toUser();
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(userDto.getPassword()));
        newUser.setTypeInscription(parseTypeInscription(userDto.getTypeInscription()));

        return userRepository.save(newUser);
    }

    /**
     * Valeur libre venue du frontend : une entrée absente ou invalide retombe
     * sur CANDIDAT plutôt que de faire échouer toute l'inscription pour un
     * champ qui n'est qu'indicatif.
     */
    private inpg.sn.lms.auth.enums.EnumTypeInscription parseTypeInscription(String valeur) {
        if (valeur == null) {
            return inpg.sn.lms.auth.enums.EnumTypeInscription.CANDIDAT;
        }
        try {
            return inpg.sn.lms.auth.enums.EnumTypeInscription.valueOf(valeur.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return inpg.sn.lms.auth.enums.EnumTypeInscription.CANDIDAT;
        }
    }

    /**
     * Crée un compte Responsable Admission (réservé à l'admin ou au seed).
     */
    public User createResponsableAdmission(inpg.sn.lms.auth.dto.UserDto userDto) {
        User user = createUser(userDto);
        // Remplacer le rôle CANDIDAT par RESPONSABLE_ADMISSION
        user.getRoles().clear();
        Role roleResp = roleRepository.findByCode("ROLE_RESPONSABLE_ADMISSION");
        if (roleResp != null) {
            user.addRole(roleResp);
        }
        user = userRepository.save(user);

        // Persister les attributs spécifiques ResponsableAdmission
        ResponsableAdmission responsable = new ResponsableAdmission(
            user,
            userDto.getNom() != null ? userDto.getNom() : "",
            userDto.getPrenom() != null ? userDto.getPrenom() : "",
            userDto.getTelephone() != null ? userDto.getTelephone() : ""
        );
        responsableAdmissionRepository.save(responsable);

        return user;
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User findByEmail(String email) {
        return email == null ? null : userRepository.findByEmail(email.trim().toLowerCase());
    }

    public boolean hasRole(User user, String roleCode) {
        return user.getRoles().stream().anyMatch(r -> r.getCode().equals(roleCode));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            // Essayer par email si le login est un email
            user = userRepository.findByEmail(username);
        }
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getCode()))
                .toList();
        return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(), authorities);
    }

    @Transactional
    public User changePassword(String email, String newPassword) {
        PasswordPolicy.exigerMotDePasseRobuste(newPassword);
        User user = findByEmail(email);
        if (user != null) {
            user.setPassword(passwordEncoder.encode(newPassword));
            return invaliderSessions(user);
        }
        return null;
    }

    /**
     * Révoque tous les JWT déjà émis pour l'utilisateur. Le jeton embarque cette
     * version et {@code JwtUtils} la compare à la valeur persistée à chaque
     * requête authentifiée.
     */
    @Transactional
    public User invaliderSessions(User user) {
        user.incrementSessionVersion();
        return userRepository.save(user);
    }

    public User save(User user) {
        return userRepository.save(user);
    }
}
