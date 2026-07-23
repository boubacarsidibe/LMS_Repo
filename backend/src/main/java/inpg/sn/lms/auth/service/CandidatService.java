package inpg.sn.lms.auth.service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import inpg.sn.lms.auth.dto.CandidatDto;
import inpg.sn.lms.auth.dto.UserDto;
import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.auth.repository.CandidatRepository;
import inpg.sn.lms.auth.repository.RoleRepository;

@Service
public class CandidatService {
    
    private final CandidatRepository candidatRepository;
    private final RoleRepository roleService;
    private final PasswordEncoder passwordEncoder;


    public CandidatService(CandidatRepository candidatRepository, RoleRepository roleService, PasswordEncoder passwordEncoder) {
        this.candidatRepository = candidatRepository;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
    }

    public Iterable<Candidat> getAllCandidats() {
        return candidatRepository.findAll();
    }
    public Candidat createCandidat(CandidatDto candidatDto) {
        Candidat newCandidat = candidatDto.toCandidat();
        newCandidat.setPassword(passwordEncoder.encode(candidatDto.getPassword()));
        newCandidat.addRole(roleService.findByCode("ROLE_CANDIDAT"));
        return candidatRepository.save(newCandidat);
    }

    
}
