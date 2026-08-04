package inpg.sn.lms.auth.service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import inpg.sn.lms.auth.dto.CandidatDto;
import inpg.sn.lms.auth.dto.UserDto;
import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.CandidatRepository;
import inpg.sn.lms.auth.repository.RoleRepository;

@Service
public class CandidatService {
    
    private final CandidatRepository candidatRepository;
    private final RoleRepository roleService;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;


    public CandidatService(CandidatRepository candidatRepository, RoleRepository roleService, PasswordEncoder passwordEncoder, UserService userService) {
        this.candidatRepository = candidatRepository;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
    }

    public List<Candidat> getAllCandidats() {
        return candidatRepository.findAll();
    }
    
    public Candidat createCandidat(CandidatDto candidatDto) {
        User user = userService.findByUsername(candidatDto.getUsername());
        if (user == null) {
            user = new User(candidatDto.getUsername(), passwordEncoder.encode(candidatDto.getPassword()), candidatDto.getEmail());
        }
        user.addRole(roleService.findByCode("ROLE_CANDIDAT"));
        Candidat newCandidat = new Candidat(user, candidatDto.getNom(), candidatDto.getPrenom(), candidatDto.getTelephone(), candidatDto.getNationalite(), candidatDto.getDateNaissance(), candidatDto.getNiveauEtude());
        return candidatRepository.save(newCandidat);
    }

}
