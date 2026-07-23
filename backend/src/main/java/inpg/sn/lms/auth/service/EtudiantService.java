package inpg.sn.lms.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import inpg.sn.lms.auth.dto.EtudiantDto;
import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.auth.repository.RoleRepository;

@Service
public class EtudiantService {

    private final EtudiantRepository etudiantRepository;
    private final RoleRepository roleRepository;


    
    private final PasswordEncoder passwordEncoder;

    // Utilisation des minuscules pour les paramètres et variables d'instance
    public EtudiantService(EtudiantRepository etudiantRepository, RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.etudiantRepository = etudiantRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Iterable<Etudiant> getAllEtudiants() {
        return etudiantRepository.findAll();
    }

    public Etudiant createEtudiant(EtudiantDto etudiantDto) {
        Etudiant newEtudiant = etudiantDto.toEtudiant();
        newEtudiant.setPassword(passwordEncoder.encode(etudiantDto.getPassword()));

        
        newEtudiant.addRole(roleRepository.findByCode("ROLE_ETUDIANT"));

        return etudiantRepository.save(newEtudiant);
    }
}