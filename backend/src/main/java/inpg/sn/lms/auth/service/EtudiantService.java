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
    private final PasswordGeneratorService passwordGeneratorService;

    // Utilisation des minuscules pour les paramètres et variables d'instance
    public EtudiantService(EtudiantRepository etudiantRepository, RoleRepository roleRepository,
            PasswordEncoder passwordEncoder, UserService userService,
            PasswordGeneratorService passwordGeneratorService) {
        this.etudiantRepository = etudiantRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
        this.passwordGeneratorService = passwordGeneratorService;
    }

    public Iterable<Etudiant> getAllEtudiants() {
        return etudiantRepository.findAll();
    }

    /**
     * Si {@code etudiantDto} n'apporte aucun mot de passe, un mot de passe est
     * généré et réécrit dans le DTO — l'appelant (qui connaît l'email) peut ainsi
     * le transmettre au titulaire du compte après l'appel.
     */
    public Etudiant createEtudiant(EtudiantDto etudiantDto) {
        if (etudiantDto.getPassword() == null || etudiantDto.getPassword().isBlank()) {
            etudiantDto.setPassword(passwordGeneratorService.generer());
        }
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
