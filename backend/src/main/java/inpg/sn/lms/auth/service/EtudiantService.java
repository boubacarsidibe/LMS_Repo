package inpg.sn.lms.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import inpg.sn.lms.auth.dto.EtudiantDto;
import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.auth.repository.RoleRepository;

@Service
public class EtudiantService {

    private final EtudiantRepository etudiantRepository;
    private final RoleRepository roleRepository;
    private final UserService userService;

    private final PasswordEncoder passwordEncoder;

    // Utilisation des minuscules pour les paramètres et variables d'instance
    public EtudiantService(EtudiantRepository etudiantRepository, RoleRepository roleRepository,
            PasswordEncoder passwordEncoder, UserService userService) {
        this.etudiantRepository = etudiantRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
    }

    public Iterable<Etudiant> getAllEtudiants() {
        return etudiantRepository.findAll();
    }

    public Etudiant createEtudiant(EtudiantDto etudiantDto) {
        User user = userService.findByUsername(etudiantDto.getUsername());
        if (user == null) {
            user = new User(etudiantDto.getUsername(), passwordEncoder.encode(etudiantDto.getPassword()),
                    etudiantDto.getEmail());
        }
        var roleEtudiant = roleRepository.findByCode("ROLE_ETUDIANT");
        if (roleEtudiant == null) {
            throw new IllegalStateException("Le rôle ROLE_ETUDIANT est introuvable.");
        }
        user.addRole(roleEtudiant);
        user = userService.save(user);
        Etudiant newEtudiant = new Etudiant(user, etudiantDto.getNom(), etudiantDto.getPrenom(), etudiantDto.getTelephone());

        return etudiantRepository.save(newEtudiant);
    }
}
