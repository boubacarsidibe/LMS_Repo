package inpg.sn.lms.config;

import inpg.sn.lms.auth.entity.AlumniProfil;
import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.entity.Formateur;
import inpg.sn.lms.auth.entity.ResponsableAdmission;
import inpg.sn.lms.auth.entity.Role;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.AlumniProfilRepository;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.auth.repository.FormateurRepository;
import inpg.sn.lms.auth.repository.ResponsableAdmissionRepository;
import inpg.sn.lms.auth.repository.RoleRepository;
import inpg.sn.lms.auth.repository.UserRepository;
import inpg.sn.lms.espace_alumni.entity.SituationProfessionnelle;
import inpg.sn.lms.espace_alumni.repository.SituationProfessionnelleRepository;
import inpg.sn.lms.espace_etudiant.entity.AlbumPhoto;
import inpg.sn.lms.espace_etudiant.entity.Annonce;
import inpg.sn.lms.espace_etudiant.entity.Certificat;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.Formation;
import inpg.sn.lms.espace_etudiant.entity.Evenement;
import inpg.sn.lms.espace_etudiant.entity.ForumMessage;
import inpg.sn.lms.espace_etudiant.entity.ForumSujet;
import inpg.sn.lms.espace_etudiant.entity.Photo;
import inpg.sn.lms.espace_etudiant.entity.Promotion;
import inpg.sn.lms.espace_etudiant.entity.RetourEvaluation;
import inpg.sn.lms.espace_etudiant.entity.SupportCours;
import inpg.sn.lms.espace_etudiant.enums.EnumStatutRetour;
import inpg.sn.lms.espace_etudiant.enums.EnumTypeAnnonce;
import inpg.sn.lms.espace_etudiant.enums.EnumTypeEvenement;
import inpg.sn.lms.espace_etudiant.repository.AlbumPhotoRepository;
import inpg.sn.lms.espace_etudiant.repository.AnnonceRepository;
import inpg.sn.lms.espace_etudiant.repository.CoursRepository;
import inpg.sn.lms.espace_etudiant.repository.FormationRepository;
import inpg.sn.lms.espace_etudiant.repository.EvenementRepository;
import inpg.sn.lms.espace_etudiant.repository.PhotoRepository;
import inpg.sn.lms.espace_etudiant.repository.PromotionRepository;
import inpg.sn.lms.espace_etudiant.repository.RetourEvaluationRepository;
import inpg.sn.lms.espace_etudiant.repository.ForumMessageRepository;
import inpg.sn.lms.espace_etudiant.repository.ForumSujetRepository;
import inpg.sn.lms.espace_etudiant.repository.SupportCoursRepository;
import inpg.sn.lms.espace_etudiant.service.CertificatService;
import inpg.sn.lms.recrutement.entity.Program;
import inpg.sn.lms.recrutement.entity.SessionCandidature;
import inpg.sn.lms.recrutement.repository.ProgramRepository;
import inpg.sn.lms.recrutement.repository.SessionCandidatureRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    @Order(1) // Doit précéder PedagogieDataInitializer, qui s'appuie sur les rôles créés ici.
    CommandLineRunner initData(
            SessionCandidatureRepository sessionRepository,
            ProgramRepository programRepository,
            RoleRepository roleRepository,
            UserRepository userRepository,
            ResponsableAdmissionRepository responsableAdmissionRepository,
            ForumSujetRepository forumSujetRepository,
            ForumMessageRepository forumMessageRepository,
            FormateurRepository formateurRepository,
            EtudiantRepository etudiantRepository,
            PromotionRepository promotionRepository,
            FormationRepository formationRepository,
            CoursRepository coursRepository,
            AnnonceRepository annonceRepository,
            EvenementRepository evenementRepository,
            RetourEvaluationRepository retourEvaluationRepository,
            SupportCoursRepository supportCoursRepository,
            PhotoRepository photoRepository,
            AlbumPhotoRepository albumPhotoRepository,
            CertificatService certificatService,
            AlumniProfilRepository alumniProfilRepository,
            SituationProfessionnelleRepository situationProfessionnelleRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.upload.dir:uploads}") String uploadDir) {
        return args -> {
            // ===================== PROGRAMMES =====================
            // Les sessions de candidature plus bas rattachent chacune un Program :
            // ce bloc doit donc rester AVANT elles. Il avait été retiré lors de la
            // fusion, à tort — CatalogueDataInitializer amorce les Formation, pas
            // les Program — et toute base neuve échouait alors sur une liste vide.
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

            // ===================== RÔLES =====================
            // Chaque rôle est vérifié séparément afin de rendre l'initialisation sûre
            // pour une base déjà existante.
            ensureRole(roleRepository, "ROLE_CANDIDAT", "Candidat",
                    "Étudiant postulant — soumet et suit sa candidature");
            ensureRole(roleRepository, "ROLE_RESPONSABLE_ADMISSION", "Responsable Admission",
                    "Staff administratif — gère les candidatures, sessions, entretiens");
            ensureRole(roleRepository, "ROLE_ETUDIANT", "Étudiant",
                    "Étudiant admis — accès à l'espace étudiant");
            ensureRole(roleRepository, "ROLE_FORMATEUR", "Formateur",
                    "Enseignant — gère ses cours et ses supports pédagogiques");
            ensureRole(roleRepository, "ROLE_RESPONSABLE_PEDAGOGIQUE", "Responsable Pédagogique",
                    "Staff pédagogique — pilote l'offre de formation et le suivi des enseignements");
            ensureRole(roleRepository, "ROLE_ALUMNI", "Alumni",
                    "Ancien étudiant — accès à son profil, son suivi professionnel et ses relevés délivrés");

            // ===================== SESSIONS =====================
            // Garde-fou : plutôt qu'un IndexOutOfBoundsException opaque au démarrage,
            // on dit ce qui manque et on saute ce seul bloc — les sections suivantes
            // (compte responsable, forum, données de démo) doivent rester exécutées.
            var programs = programRepository.findAll();
            if (sessionRepository.count() == 0 && programs.size() < 4) {
                System.out.println("Sessions de demo ignorees : " + programs.size()
                        + " programme(s) en base, 4 attendus.");
            } else if (sessionRepository.count() == 0) {
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
                s1.setStatus("OUVERT");
                s1.setProgram(p1);
                sessionRepository.save(s1);

                SessionCandidature s2 = new SessionCandidature();
                s2.setLibelle("Session Printemps 2027 MBA");
                s2.setDateOuverture(LocalDate.of(2027, 1, 15));
                s2.setDateFermeture(LocalDate.of(2027, 5, 30));
                s2.setEstOuverte(true);
                s2.setQuotaMax(150);
                s2.setStatus("OUVERT");
                s2.setProgram(p2);
                sessionRepository.save(s2);

                SessionCandidature s3 = new SessionCandidature();
                s3.setLibelle("Brouillon Ingenieur 2026");
                s3.setDateOuverture(LocalDate.of(2026, 9, 1));
                s3.setDateFermeture(LocalDate.of(2027, 1, 31));
                s3.setEstOuverte(false);
                s3.setQuotaMax(60);
                s3.setStatus("BROUILLON");
                s3.setProgram(p3);
                sessionRepository.save(s3);

                SessionCandidature s4 = new SessionCandidature();
                s4.setLibelle("Hiver 2025 Cloturee");
                s4.setDateOuverture(LocalDate.of(2025, 11, 1));
                s4.setDateFermeture(LocalDate.of(2026, 3, 15));
                s4.setEstOuverte(false);
                s4.setQuotaMax(100);
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


            // ===================== DONNEES DE DEMO (presentation client) =====================
            if (userRepository.findByUsername("prof_demo") == null) {
                Role roleFormateur = roleRepository.findByCode("ROLE_FORMATEUR");
                Role roleEtudiant = roleRepository.findByCode("ROLE_ETUDIANT");

                Promotion promotionDS = new Promotion();
                promotionDS.setCode("M1-DS-2026");
                promotionDS.setLibelle("Master 1 - Data Science & IA");
                promotionDS.setDateDebut(LocalDate.of(2026, 9, 1));
                promotionDS.setDateFin(LocalDate.of(2027, 6, 30));
                promotionDS.setFormation(formationDemo(formationRepository, "DEMO-DS",
                        "Master Data Science & IA",
                        "Programme de demonstration : fondamentaux de l'apprentissage automatique."));
                promotionDS = promotionRepository.save(promotionDS);

                Promotion promotionFinance = new Promotion();
                promotionFinance.setCode("M2-FIN-2026");
                promotionFinance.setLibelle("Master 2 - Finance Durable");
                promotionFinance.setDateDebut(LocalDate.of(2026, 9, 1));
                promotionFinance.setDateFin(LocalDate.of(2027, 6, 30));
                promotionFinance.setFormation(formationDemo(formationRepository, "DEMO-FIN",
                        "Master Finance Durable",
                        "Programme de demonstration : evaluation d'actifs et criteres ESG."));
                promotionFinance = promotionRepository.save(promotionFinance);

                Promotion promotionProcedes = new Promotion();
                promotionProcedes.setCode("ING-PROC-2026");
                promotionProcedes.setLibelle("Cycle Ingenieur - Procedes Industriels");
                promotionProcedes.setDateDebut(LocalDate.of(2026, 9, 1));
                promotionProcedes.setDateFin(LocalDate.of(2029, 6, 30));
                promotionProcedes.setFormation(formationDemo(formationRepository, "DEMO-PROC",
                        "Cycle Ingenieur Procedes Industriels",
                        "Programme de demonstration : dimensionnement et optimisation d'unites."));
                promotionProcedes = promotionRepository.save(promotionProcedes);

                User userFormateur = new User();
                userFormateur.setUsername("prof_demo");
                userFormateur.setEmail("prof.demo@inpg.sn");
                userFormateur.setPassword(passwordEncoder.encode("Demo@2026"));
                userFormateur.addRole(roleFormateur);
                userFormateur = userRepository.save(userFormateur);
                Formateur formateurDemo = new Formateur(userFormateur, "Ndiaye", "Moussa", "+221771112233");
                formateurRepository.save(formateurDemo);

                Etudiant aissatou = creerEtudiantDemo(userRepository, etudiantRepository, passwordEncoder, roleEtudiant,
                        "aissatou_ba", "aissatou.ba@inpg-demo.sn", "Ba", "Aissatou", "+221770000001", promotionDS);
                Etudiant cheikh = creerEtudiantDemo(userRepository, etudiantRepository, passwordEncoder, roleEtudiant,
                        "cheikh_fall", "cheikh.fall@inpg-demo.sn", "Fall", "Cheikh", "+221770000002", promotionDS);
                Etudiant fatou = creerEtudiantDemo(userRepository, etudiantRepository, passwordEncoder, roleEtudiant,
                        "fatou_diagne", "fatou.diagne@inpg-demo.sn", "Diagne", "Fatou", "+221770000003", promotionFinance);
                creerEtudiantDemo(userRepository, etudiantRepository, passwordEncoder, roleEtudiant,
                        "ibrahima_sow", "ibrahima.sow@inpg-demo.sn", "Sow", "Ibrahima", "+221770000004", promotionFinance);
                creerEtudiantDemo(userRepository, etudiantRepository, passwordEncoder, roleEtudiant,
                        "mamadou_diallo", "mamadou.diallo@inpg-demo.sn", "Diallo", "Mamadou", "+221770000005", promotionProcedes);
                Etudiant awa = creerEtudiantDemo(userRepository, etudiantRepository, passwordEncoder, roleEtudiant,
                        "awa_ndoye", "awa.ndoye@inpg-demo.sn", "Ndoye", "Awa", "+221770000006", promotionProcedes);

                // ----- Alumni (Espace Alumni, TDR C) : un ancien etudiant de demonstration -----
                // Passe cote alumni exactement comme le ferait PedagogieScolariteController
                // #promouvoirAlumni : le role ETUDIANT est retire au profit d'ALUMNI, la ligne
                // Etudiant reste en base (c'est elle qui garde le matricule et le releve).
                Role roleAlumni = roleRepository.findByCode("ROLE_ALUMNI");
                if (roleAlumni != null) {
                    User userAwa = awa.getUser();
                    userAwa.getRoles().clear();
                    userAwa.addRole(roleAlumni);
                    userRepository.save(userAwa);

                    AlumniProfil alumniAwa = new AlumniProfil(userAwa);
                    alumniAwa.setPosteActuel("Ingenieure Procedes");
                    alumniAwa.setEntreprise("Petrosen");
                    alumniAwa.setSecteurActivite("Petrole & Gaz");
                    alumniAwa = alumniProfilRepository.save(alumniAwa);

                    situationProfessionnelleRepository.save(new SituationProfessionnelle(alumniAwa,
                            "Ingenieure Procedes", "Petrosen", LocalDate.of(2026, 10, 1), null));
                }

                Cours coursDS = new Cours();
                coursDS.setNom("DS-101");
                coursDS.setTitre("Introduction au Machine Learning");
                coursDS.setDescription("Fondamentaux de l'apprentissage automatique : regression, classification, evaluation de modeles.");
                coursDS.setSemestre("Semestre 1 - 2026");
                coursDS.setVolumeHoraire(40L);
                coursDS.getFormateurs().add(formateurDemo);
                coursDS = coursRepository.save(coursDS);
                inscrireAuProgramme(formationRepository, promotionDS, coursDS);

                Cours coursFinance = new Cours();
                coursFinance.setNom("FIN-201");
                coursFinance.setTitre("Analyse Financiere Avancee");
                coursFinance.setDescription("Evaluation d'actifs, gestion de portefeuille et criteres de durabilite (ESG).");
                coursFinance.setSemestre("Semestre 1 - 2026");
                coursFinance.setVolumeHoraire(36L);
                coursFinance.getFormateurs().add(formateurDemo);
                coursFinance = coursRepository.save(coursFinance);
                inscrireAuProgramme(formationRepository, promotionFinance, coursFinance);

                Cours coursProcedes = new Cours();
                coursProcedes.setNom("PROC-301");
                coursProcedes.setTitre("Genie des Procedes Industriels");
                coursProcedes.setDescription("Dimensionnement d'unites industrielles et optimisation des procedes de production.");
                coursProcedes.setSemestre("Semestre 1 - 2026");
                coursProcedes.setVolumeHoraire(45L);
                coursProcedes.getFormateurs().add(formateurDemo);
                coursProcedes = coursRepository.save(coursProcedes);
                inscrireAuProgramme(formationRepository, promotionProcedes, coursProcedes);

                // ----- Supports de cours (fichiers PDF reels + liens externes) -----
                Path uploadPath = Path.of(uploadDir).toAbsolutePath().normalize();
                Files.createDirectories(uploadPath);
                Files.createDirectories(uploadPath.resolve("photos"));

                creerSupportPdf(supportCoursRepository, uploadPath, coursDS,
                        "Slides - Introduction au Machine Learning", "demo-support-ds101-slides.pdf");
                creerSupportLien(supportCoursRepository, coursDS,
                        "Dataset Kaggle - Titanic", "https://www.kaggle.com/c/titanic");
                creerSupportPdf(supportCoursRepository, uploadPath, coursFinance,
                        "Support - Analyse Financiere Avancee", "demo-support-fin201-support.pdf");
                creerSupportLien(supportCoursRepository, coursFinance,
                        "Article ESG - Banque Mondiale", "https://www.worldbank.org");
                creerSupportPdf(supportCoursRepository, uploadPath, coursProcedes,
                        "Cours - Genie des Procedes Chapitre 1", "demo-support-proc301-chap1.pdf");
                creerSupportLien(supportCoursRepository, coursProcedes,
                        "Norme ISO 9001", "https://www.iso.org/standard/62085.html");

                // ----- Photos (fichiers image reels, placeholder) -----
                byte[] photoPlaceholder = Base64.getDecoder().decode(
                        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");
                creerPhotoDemo(photoRepository, albumPhotoRepository, uploadPath, photoPlaceholder,
                        "Rentree M1 Data Science 2026", "demo-photo-ds.png", promotionDS);
                creerPhotoDemo(photoRepository, albumPhotoRepository, uploadPath, photoPlaceholder,
                        "Seminaire Finance Durable", "demo-photo-fin.png", promotionFinance);
                creerPhotoDemo(photoRepository, albumPhotoRepository, uploadPath, photoPlaceholder,
                        "Visite technique - Procedes Industriels", "demo-photo-proc.png", promotionProcedes);

                // ----- Certificats (le PDF est genere a la volee au telechargement, pas besoin de fichier) -----
                // Passe par le service, et non le repository, pour que les certificats de
                // demo portent une vraie signature electronique verifiable.
                certificatService.save(creerCertificatDemo(aissatou, "Introduction au Machine Learning", promotionDS, "Moussa Ndiaye"));
                certificatService.save(creerCertificatDemo(fatou, "Analyse Financiere Avancee", promotionFinance, "Moussa Ndiaye"));

                Annonce annonce1 = new Annonce("Bienvenue a la rentree 2026",
                        "L'ensemble des cours demarre la semaine du 1er septembre. Consultez votre planning pour les horaires de vos classes virtuelles.",
                        EnumTypeAnnonce.ANNONCE, userFormateur, null);
                annonce1.setEpinglee(true);
                annonceRepository.save(annonce1);

                Annonce annonce2 = new Annonce("Modification de l'emploi du temps - Semaine 3",
                        "La seance de tutorat Finance Durable est deplacee au jeudi 10h en raison d'un conflit de salle.",
                        EnumTypeAnnonce.MESSAGE, userFormateur, promotionFinance);
                annonce2.setEpinglee(false);
                annonceRepository.save(annonce2);

                Evenement evenement1 = new Evenement();
                evenement1.setTitre("Classe virtuelle - Introduction au Machine Learning");
                evenement1.setDate(LocalDate.now().plusDays(2));
                evenement1.setHeure("09:00");
                evenement1.setType(EnumTypeEvenement.COURS);
                evenement1.setLienVisio("https://meet.example.com/ds-101");
                evenementRepository.save(evenement1);

                Evenement evenement2 = new Evenement();
                evenement2.setTitre("Tutorat - Analyse Financiere Avancee");
                evenement2.setDate(LocalDate.now().plusDays(3));
                evenement2.setHeure("14:30");
                evenement2.setType(EnumTypeEvenement.TUTORAT);
                evenementRepository.save(evenement2);

                Evenement evenement3 = new Evenement();
                evenement3.setTitre("Rendu de projet - Genie des Procedes");
                evenement3.setDate(LocalDate.now().plusDays(7));
                evenement3.setHeure("23:59");
                evenement3.setType(EnumTypeEvenement.ECHEANCE);
                evenementRepository.save(evenement3);

                Evenement evenement4 = new Evenement();
                evenement4.setTitre("Classe virtuelle - Analyse Financiere Avancee");
                evenement4.setDate(LocalDate.now().plusDays(9));
                evenement4.setHeure("10:00");
                evenement4.setType(EnumTypeEvenement.COURS);
                evenement4.setLienVisio("https://meet.example.com/fin-201");
                evenementRepository.save(evenement4);

                // ----- Forum : un sujet par cours, avec de vraies associations -----
                // Depuis la fusion des deux forums (migration V5), l'auteur est un User et
                // le cours une cle etrangere : c'est ce qui permet a ForumController de
                // restreindre la visibilite aux cours de la promotion de l'etudiant.
                creerSujetForumDemo(forumSujetRepository, forumMessageRepository, coursDS, aissatou.getUser(),
                        "Difficulte sur le TP de simulation",
                        "Bonjour Professeur,\n\nLe modele du TP refuse de converger, meme apres 500 iterations."
                                + " Est-ce un probleme de parametrage ?",
                        false, LocalDateTime.now().minusMinutes(14));
                creerSujetForumDemo(forumSujetRepository, forumMessageRepository, coursFinance, fatou.getUser(),
                        "Support PDF inaccessible",
                        "Bonjour,\n\nLe support PDF du cours n'est pas telechargeable, j'obtiens une erreur 404."
                                + " Pourriez-vous verifier ?",
                        true, LocalDateTime.now().minusHours(2));

                retourEvaluationRepository.save(creerRetourDemo(coursDS, aissatou, 5,
                        "Cours tres bien structure, les exemples pratiques m'ont beaucoup aide."));
                retourEvaluationRepository.save(creerRetourDemo(coursDS, cheikh, 4,
                        "Bon rythme, j'aurais aime un peu plus de travaux pratiques sur les reseaux de neurones."));
                retourEvaluationRepository.save(creerRetourDemo(coursFinance, fatou, 2,
                        "Le volume de lecture pour le module 2 est tres dense, difficile a suivre avant le test."));

                System.out.println("Donnees de demo creees (3 cohortes, 1 formateur, 6 etudiants dont 1 alumni, 3 cours, 6 supports, 3 photos, 2 certificats, 2 annonces, 4 evenements, 3 retours)");
            }
        };
    }

    /**
     * Programme de demonstration, cree si absent.
     *
     * <p>Les promotions de demonstration n'en avaient aucun. Or un cours se rattache
     * desormais a une formation, jamais directement a une promotion : sans programme,
     * les cours de demonstration seraient invisibles pour tout le monde.
     */
    private Formation formationDemo(FormationRepository formationRepository, String code, String intitule,
            String description) {
        return formationRepository.findByCode(code).orElseGet(() -> {
            Formation formation = new Formation();
            formation.setCode(code);
            formation.setIntitule(intitule);
            formation.setDescription(description);
            return formationRepository.save(formation);
        });
    }

    /** Inscrit le cours au programme suivi par cette promotion. */
    private void inscrireAuProgramme(FormationRepository formationRepository, Promotion promotion, Cours cours) {
        Formation formation = promotion.getFormation();
        if (formation == null) {
            return;
        }
        formation.getCours().add(cours);
        formationRepository.save(formation);
    }

    private Etudiant creerEtudiantDemo(UserRepository userRepository, EtudiantRepository etudiantRepository,
            PasswordEncoder passwordEncoder, Role roleEtudiant, String username, String email, String nom,
            String prenom, String telephone, Promotion promotion) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("Etudiant@2026"));
        user.addRole(roleEtudiant);
        user = userRepository.save(user);
        Etudiant etudiant = new Etudiant(user, nom, prenom, telephone);
        etudiant.setPromotion(promotion);
        return etudiantRepository.save(etudiant);
    }

    /**
     * Cree un sujet de forum et son premier message. {@code epingle} materialise
     * l'ancien statut « urgent » : le sujet remonte en tete de la liste du formateur.
     */
    private void creerSujetForumDemo(ForumSujetRepository sujetRepository,
            ForumMessageRepository messageRepository, Cours cours, User auteur,
            String titre, String contenu, boolean epingle, LocalDateTime date) {
        ForumSujet sujet = new ForumSujet(titre, cours, auteur);
        sujet.setDateCreation(date);
        sujet.setEpingle(epingle);
        sujet = sujetRepository.save(sujet);
        ForumMessage message = new ForumMessage(sujet, auteur, contenu);
        message.setDateCreation(date);
        messageRepository.save(message);
    }

    private RetourEvaluation creerRetourDemo(Cours cours, Etudiant etudiant, int note, String commentaire) {
        RetourEvaluation retour = new RetourEvaluation();
        retour.setCours(cours);
        retour.setEtudiant(etudiant);
        retour.setNote(note);
        retour.setCommentaire(commentaire);
        retour.setDateCreation(LocalDateTime.now().minusDays(1));
        retour.setStatut(note <= 3 ? EnumStatutRetour.ACTION_REQUISE : EnumStatutRetour.VALIDE);
        retour.setEpingle(false);
        return retour;
    }

    private void creerSupportPdf(SupportCoursRepository supportCoursRepository, Path uploadPath, Cours cours,
            String titre, String nomFichier) throws IOException {
        Path cible = uploadPath.resolve(nomFichier);
        if (!Files.exists(cible)) {
            Files.write(cible, genererPdfPlaceholder(titre));
        }
        SupportCours support = new SupportCours();
        support.setTitre(titre);
        support.setTypeRessource("pdf");
        support.setFichierStocke(nomFichier);
        support.setDatePublication(LocalDate.now().minusDays(3));
        support.setCours(cours);
        support = supportCoursRepository.save(support);
        support.setUrl("/api/espace-etudiant/supports/cours/" + support.getId() + "/download");
        supportCoursRepository.save(support);
    }

    private void creerSupportLien(SupportCoursRepository supportCoursRepository, Cours cours, String titre, String url) {
        SupportCours support = new SupportCours();
        support.setTitre(titre);
        support.setTypeRessource("lien");
        support.setUrl(url);
        support.setDatePublication(LocalDate.now().minusDays(3));
        support.setCours(cours);
        supportCoursRepository.save(support);
    }

    private void creerPhotoDemo(PhotoRepository photoRepository, AlbumPhotoRepository albumPhotoRepository,
            Path uploadPath, byte[] contenu, String titre, String nomFichier, Promotion promotion) throws IOException {
        Path cible = uploadPath.resolve("photos").resolve(nomFichier);
        if (!Files.exists(cible)) {
            Files.write(cible, contenu);
        }
        AlbumPhoto album = new AlbumPhoto(titre, null, promotion, LocalDate.now().minusDays(1));
        album = albumPhotoRepository.save(album);
        Photo photo = new Photo(album, nomFichier, nomFichier, titre);
        photoRepository.save(photo);
    }

    private Certificat creerCertificatDemo(Etudiant etudiant, String intituleFormation, Promotion promotion, String formateurNom) {
        Certificat certificat = new Certificat();
        certificat.setEtudiant(etudiant);
        certificat.setPromotion(promotion);
        certificat.setIntituleFormation(intituleFormation);
        certificat.setFormateurNom(formateurNom);
        return certificat;
    }

    /**
     * Construit un PDF minimal valide (une page, un peu de texte) en calculant les
     * offsets xref programmatiquement, pour eviter tout risque d'erreur de calcul
     * manuel. Sert uniquement de contenu d'exemple pour les supports de demo.
     */
    private byte[] genererPdfPlaceholder(String titre) throws IOException {
        String texte = titre.replace("\\", "").replace("(", "").replace(")", "");
        String contenuFlux = "BT /F1 14 Tf 40 150 Td (" + texte + ") Tj ET";

        List<String> objets = List.of(
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 300 200] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>",
                "<< /Length " + contenuFlux.length() + " >>\nstream\n" + contenuFlux + "\nendstream",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        out.write("%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII));
        int numero = 1;
        for (String objet : objets) {
            offsets.add(out.size());
            out.write((numero + " 0 obj\n" + objet + "\nendobj\n").getBytes(StandardCharsets.US_ASCII));
            numero++;
        }
        int xrefOffset = out.size();
        StringBuilder xref = new StringBuilder();
        xref.append("xref\n0 ").append(objets.size() + 1).append("\n0000000000 65535 f \n");
        for (int offset : offsets) {
            xref.append(String.format("%010d 00000 n \n", offset));
        }
        xref.append("trailer\n<< /Size ").append(objets.size() + 1).append(" /Root 1 0 R >>\nstartxref\n")
                .append(xrefOffset).append("\n%%EOF");
        out.write(xref.toString().getBytes(StandardCharsets.US_ASCII));
        return out.toByteArray();
    }

    private Role ensureRole(RoleRepository roleRepository, String code, String libelle, String description) {
        Role existingRole = roleRepository.findByCode(code);
        if (existingRole != null) {
            return existingRole;
        }

        Role role = new Role();
        role.setCode(code);
        role.setLibelle(libelle);
        role.setDescription(description);
        return roleRepository.save(role);
    }
}
