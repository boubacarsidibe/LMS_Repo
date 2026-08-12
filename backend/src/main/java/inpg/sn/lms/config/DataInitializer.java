package inpg.sn.lms.config;

import inpg.sn.lms.auth.entity.ResponsableAdmission;
import inpg.sn.lms.auth.entity.Role;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.ResponsableAdmissionRepository;
import inpg.sn.lms.auth.repository.RoleRepository;
import inpg.sn.lms.auth.repository.UserRepository;
import inpg.sn.lms.recrutement.entity.Program;
import inpg.sn.lms.recrutement.entity.SessionCandidature;
import inpg.sn.lms.recrutement.repository.ProgramRepository;
import inpg.sn.lms.recrutement.repository.SessionCandidatureRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initData(
            SessionCandidatureRepository sessionRepository,
            ProgramRepository programRepository,
            RoleRepository roleRepository,
            UserRepository userRepository,
            ResponsableAdmissionRepository responsableAdmissionRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            // ===================== PROGRAMMES =====================
            if (programRepository.count() == 0) {
                Program p1 = new Program();
                p1.setTitle("Master 1 Data Science & AI");
                p1.setLevel("Master 1");
                p1.setDepartment("Informatique");
                programRepository.save(p1);

                Program p2 = new Program();
                p2.setTitle("Executive MBA Petrole & Gaz");
                p2.setLevel("MBA");
                p2.setDepartment("Management");
                programRepository.save(p2);

                Program p3 = new Program();
                p3.setTitle("Cycle Ingenieur Procedes");
                p3.setLevel("Ingenieur");
                p3.setDepartment("Genie Industriel");
                programRepository.save(p3);

                Program p4 = new Program();
                p4.setTitle("Bachelor Renewable Energy");
                p4.setLevel("Licence");
                p4.setDepartment("Energies");
                programRepository.save(p4);

                System.out.println("Programmes crees (4)");
            }

            // ===================== ROLES =====================
            if (roleRepository.count() == 0) {
                Role roleCandidat = new Role();
                roleCandidat.setCode("ROLE_CANDIDAT");
                roleCandidat.setLibelle("Candidat");
                roleCandidat.setDescription("Etudiant postulant");
                roleRepository.save(roleCandidat);

                Role roleResponsable = new Role();
                roleResponsable.setCode("ROLE_RESPONSABLE_ADMISSION");
                roleResponsable.setLibelle("Responsable Admission");
                roleResponsable.setDescription("Staff administratif");
                roleRepository.save(roleResponsable);

                Role roleEtudiant = new Role();
                roleEtudiant.setCode("ROLE_ETUDIANT");
                roleEtudiant.setLibelle("Etudiant");
                roleEtudiant.setDescription("Etudiant admis");
                roleRepository.save(roleEtudiant);

                System.out.println("Roles crees");
            }

            // ===================== SESSIONS =====================
            if (sessionRepository.count() == 0) {
                var programs = programRepository.findAll();
                Program p1 = programs.get(0);
                Program p2 = programs.get(1);
                Program p3 = programs.get(2);
                Program p4 = programs.get(3);

                SessionCandidature s1 = new SessionCandidature();
                s1.setLibelle("Session Automne 2026");
                s1.setDateOuverture(LocalDate.of(2026, 5, 15));
                s1.setDateFermeture(LocalDate.of(2026, 9, 30));
                s1.setEstOuverte(true);
                s1.setQuotaMax(220);
                s1.setQuotaActuel(186);
                s1.setStatus("OUVERT");
                s1.setProgram(p1);
                sessionRepository.save(s1);

                SessionCandidature s2 = new SessionCandidature();
                s2.setLibelle("Session Printemps 2027 MBA");
                s2.setDateOuverture(LocalDate.of(2027, 1, 15));
                s2.setDateFermeture(LocalDate.of(2027, 5, 30));
                s2.setEstOuverte(true);
                s2.setQuotaMax(150);
                s2.setQuotaActuel(45);
                s2.setStatus("OUVERT");
                s2.setProgram(p2);
                sessionRepository.save(s2);

                SessionCandidature s3 = new SessionCandidature();
                s3.setLibelle("Brouillon Ingenieur 2026");
                s3.setDateOuverture(LocalDate.of(2026, 9, 1));
                s3.setDateFermeture(LocalDate.of(2027, 1, 31));
                s3.setEstOuverte(false);
                s3.setQuotaMax(60);
                s3.setQuotaActuel(0);
                s3.setStatus("BROUILLON");
                s3.setProgram(p3);
                sessionRepository.save(s3);

                SessionCandidature s4 = new SessionCandidature();
                s4.setLibelle("Hiver 2025 Cloturee");
                s4.setDateOuverture(LocalDate.of(2025, 11, 1));
                s4.setDateFermeture(LocalDate.of(2026, 3, 15));
                s4.setEstOuverte(false);
                s4.setQuotaMax(100);
                s4.setQuotaActuel(98);
                s4.setStatus("CLOTURE");
                s4.setProgram(p4);
                sessionRepository.save(s4);

                System.out.println("Sessions crees (4)");
            }

            // ===================== COMPTE RESPONSABLE =====================
            if (userRepository.findByUsername("admin_admission") == null) {
                Role roleResp = roleRepository.findByCode("ROLE_RESPONSABLE_ADMISSION");
                User user = new User();
                user.setUsername("admin_admission");
                user.setEmail("admin@inpg.sn");
                user.setPassword(passwordEncoder.encode("Admin@2026"));
                user.addRole(roleResp);
                user = userRepository.save(user);

                ResponsableAdmission resp = new ResponsableAdmission();
                resp.setUser(user);
                resp.setNom("Vallet");
                resp.setPrenom("Claire");
                resp.setTelephone("+221 77 000 00 00");
                responsableAdmissionRepository.save(resp);

                System.out.println("Compte admin_admission cree");
            }
        };
    }
}