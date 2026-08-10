package inpg.sn.lms.auth.service;


import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import inpg.sn.lms.auth.entity.Role;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.RoleRepository;
import inpg.sn.lms.auth.repository.UserRepository;

import java.util.List;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Crée un utilisateur avec le rôle CANDIDAT par défaut (inscription publique).
     */
    public User createUser(inpg.sn.lms.auth.dto.UserDto userDto) {
        if (userDto.getEmail() == null || userDto.getEmail().isBlank()
                || userDto.getUsername() == null || userDto.getUsername().isBlank()
                || userDto.getPassword() == null || userDto.getPassword().isBlank()) {
            throw new IllegalArgumentException("Le nom d'utilisateur, l'e-mail et le mot de passe sont requis.");
        }

        String email = userDto.getEmail().trim().toLowerCase();
        if (userRepository.findByEmail(email) != null) {
            throw new RuntimeException("Cet email est déjà utilisé.");
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

        // Assigner le rôle CANDIDAT à tout nouvel inscrit
        Role roleCandidat = roleRepository.findByCode("ROLE_CANDIDAT");
        if (roleCandidat == null) {
            throw new IllegalStateException("Le rôle ROLE_CANDIDAT est introuvable.");
        }
        newUser.addRole(roleCandidat);

        return userRepository.save(newUser);
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
        return userRepository.save(user);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean hasRole(User user, String roleCode) {
        return user.getRoles().stream().anyMatch(r -> r.getCode().equals(roleCode));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getCode()))
                .toList();
        return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(), authorities);
    }

    public User ChangePassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email);
        if (user != null) {
            user.setPassword(passwordEncoder.encode(newPassword));
            return userRepository.save(user);
        }
        return null;
    }

    public User save(User user) {
        return userRepository.save(user);
    }
}
