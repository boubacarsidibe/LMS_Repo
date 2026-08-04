package inpg.sn.lms.config;

import inpg.sn.lms.auth.entity.ResponsableAdmission;
import inpg.sn.lms.auth.entity.Role;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.ResponsableAdmissionRepository;
import inpg.sn.lms.auth.repository.RoleRepository;
import inpg.sn.lms.auth.repository.UserRepository;
import inpg.sn.lms.recrutement.entity.SessionCandidature;
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
            RoleRepository roleRepository,
            UserRepository userRepository,
            ResponsableAdmissionRepository responsableAdmissionRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            // ===================== RÔLES =====================
            if (roleRepository.count() == 0) {
                Role roleCandidat = new Role();
                roleCandidat.setCode("ROLE_CANDIDAT");
                roleCandidat.setLibelle("Candidat");
                roleCandidat.setDescription("Étudiant postulant — soumet et suit sa candidature");
                roleRepository.save(roleCandidat);

                Role roleResponsable = new Role();
                roleResponsable.setCode("ROLE_RESPONSABLE_ADMISSION");
                roleResponsable.setLibelle("Responsable Admission");
                roleResponsable.setDescription("Staff administratif — gère les candidatures, sessions, entretiens");
                roleRepository.save(roleResponsable);

                Role roleEtudiant = new Role();
                roleEtudiant.setCode("ROLE_ETUDIANT");
                roleEtudiant.setLibelle("Étudiant");
                roleEtudiant.setDescription("Étudiant admis — accès à l'espace étudiant");
                roleRepository.save(roleEtudiant);

                System.out.println("✅ Rôles créés : ROLE_CANDIDAT, ROLE_RESPONSABLE_ADMISSION, ROLE_ETUDIANT");
            }

            // ===================== SESSIONS =====================
            if (sessionRepository.count() == 0) {
                SessionCandidature session = new SessionCandidature();
                session.setLibelle("Session 2026 — Recrutement INPG");
                session.setDateOuverture(LocalDate.of(2026, 1, 1));
                session.setDateFermeture(LocalDate.of(2026, 12, 31));
                session.setEstOuverte(true);
                sessionRepository.save(session);
                System.out.println("✅ Session de candidature par défaut créée (ID=" + session.getId() + ")");
            }

            // ===================== COMPTE RESPONSABLE ADMISSION PAR DÉFAUT =====================
            if (userRepository.findByUsername("admin_admission") == null) {
                Role roleResp = roleRepository.findByCode("ROLE_RESPONSABLE_ADMISSION");
                
                // 1. Créer le User
                User user = new User();
                user.setUsername("admin_admission");
                user.setEmail("admin@inpg.sn");
                user.setPassword(passwordEncoder.encode("Admin@2026"));
                user.addRole(roleResp);
                user = userRepository.save(user);
                
                // 2. Créer le ResponsableAdmission lié
                ResponsableAdmission resp = new ResponsableAdmission();
                resp.setUser(user);
                resp.setNom("Vallet");
                resp.setPrenom("Claire");
                resp.setTelephone("+221 77 000 00 00");
                responsableAdmissionRepository.save(resp);
                
                System.out.println("✅ Compte Responsable Admission créé : admin_admission / Admin@2026");
            }
        };
    }
}