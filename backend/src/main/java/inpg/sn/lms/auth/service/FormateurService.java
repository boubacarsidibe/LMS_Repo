package inpg.sn.lms.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;

import inpg.sn.lms.auth.dto.CandidatDto;
import inpg.sn.lms.auth.dto.FormateurDto;
import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.auth.entity.Formateur;
import inpg.sn.lms.auth.repository.CandidatRepository;
import inpg.sn.lms.auth.repository.FormateurRepository;
import inpg.sn.lms.auth.repository.RoleRepository;

public class FormateurService {
    
    private final FormateurRepository formateurRepository;
    private final RoleRepository roleService;
    private final PasswordEncoder passwordEncoder;


    public FormateurService(FormateurRepository formateurRepository, RoleRepository roleService, PasswordEncoder passwordEncoder) {
        this.formateurRepository = formateurRepository;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
    }

    public Iterable<Formateur> getAllFormateurs() {
        return formateurRepository.findAll();
    }
    public Formateur createFormateur(FormateurDto formateurDto) {
        Formateur newFormateur = formateurDto.toFormateur();
        newFormateur.setPassword(passwordEncoder.encode(formateurDto.getPassword()));
        newFormateur.addRole(roleService.findByCode("ROLE_FORMATEUR"));
        return formateurRepository.save(newFormateur);
    }
}
