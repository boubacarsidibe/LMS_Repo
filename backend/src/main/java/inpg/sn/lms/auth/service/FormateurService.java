package inpg.sn.lms.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import inpg.sn.lms.auth.dto.FormateurDto;

import inpg.sn.lms.auth.entity.Formateur;
import inpg.sn.lms.auth.entity.User;

import inpg.sn.lms.auth.repository.FormateurRepository;
import inpg.sn.lms.auth.repository.RoleRepository;

@Service
public class FormateurService {

    private final FormateurRepository formateurRepository;
    private final RoleRepository roleService;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    public FormateurService(FormateurRepository formateurRepository, RoleRepository roleService,
            PasswordEncoder passwordEncoder, UserService userService) {
        this.formateurRepository = formateurRepository;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
    }

    public Iterable<Formateur> getAllFormateurs() {
        return formateurRepository.findAll();
    }

    public Formateur createFormateur(FormateurDto formateurDto) {
        User user = userService.findByUsername(formateurDto.getUsername());
        if (user == null) {
            user = new User(formateurDto.getUsername(), passwordEncoder.encode(formateurDto.getPassword()),
                    formateurDto.getEmail());
        }
        var roleFormateur = roleService.findByCode("ROLE_FORMATEUR");
        if (roleFormateur == null) {
            throw new IllegalStateException("Le rôle ROLE_FORMATEUR est introuvable.");
        }
        user.addRole(roleFormateur);
        user = userService.save(user);
        Formateur newFormateur = new Formateur(user, formateurDto.getNom(), formateurDto.getPrenom(),
                formateurDto.getTelephone());

        return formateurRepository.save(newFormateur);
    }
}
