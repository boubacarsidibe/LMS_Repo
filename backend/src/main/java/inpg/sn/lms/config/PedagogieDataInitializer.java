package inpg.sn.lms.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.entity.Role;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.auth.repository.RoleRepository;
import inpg.sn.lms.auth.repository.UserRepository;
import inpg.sn.lms.espace_etudiant.entity.AlbumPhoto;
import inpg.sn.lms.espace_etudiant.entity.Annonce;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.Formation;
import inpg.sn.lms.espace_etudiant.entity.Promotion;
import inpg.sn.lms.espace_etudiant.entity.Seance;
import inpg.sn.lms.espace_etudiant.enums.EnumTypeAnnonce;
import inpg.sn.lms.espace_etudiant.repository.AlbumPhotoRepository;
import inpg.sn.lms.espace_etudiant.repository.AnnonceRepository;
import inpg.sn.lms.espace_etudiant.repository.CoursRepository;
import inpg.sn.lms.espace_etudiant.repository.FormationRepository;
import inpg.sn.lms.espace_etudiant.repository.PromotionRepository;
import inpg.sn.lms.espace_etudiant.repository.SeanceRepository;
import inpg.sn.lms.espace_etudiant.service.MatriculeService;

/**
 * Amorçage du cursus : une formation, ses cours, une promotion et de quoi
 * remplir l'espace étudiant (calendrier, annonces, photothèque).
 *
 * <p>Sans ces données, toutes les pages de l'espace étudiant s'afficheraient
 * vides : la chaîne Étudiant → Promotion → Formation → Cours doit exister de
 * bout en bout pour qu'un étudiant voie ne serait-ce qu'un cours.
 *
 * <p>Le runner est idempotent — chaque bloc vérifie son absence avant d'écrire —
 * et s'exécute après {@link DataInitializer}, dont il réutilise les rôles.
 */
@Configuration
public class PedagogieDataInitializer {

    private static final String CODE_FORMATION = "MPG";
    private static final String CODE_PROMOTION = "MPG-2026";

    @Bean
    @ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
    @Order(2)
    CommandLineRunner initPedagogie(
            FormationRepository formationRepository,
            CoursRepository coursRepository,
            PromotionRepository promotionRepository,
            SeanceRepository seanceRepository,
            AnnonceRepository annonceRepository,
            AlbumPhotoRepository albumRepository,
            EtudiantRepository etudiantRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            MatriculeService matriculeService,
            PasswordEncoder passwordEncoder) {
        return args -> {
            Formation formation = formationRepository.findAll().stream()
                    .filter(f -> CODE_FORMATION.equals(f.getCode()))
                    .findFirst()
                    .orElseGet(() -> creerFormationEtCours(formationRepository, coursRepository));

            Promotion promotion = promotionRepository.findAll().stream()
                    .filter(p -> CODE_PROMOTION.equals(p.getCode()))
                    .findFirst()
                    .orElseGet(() -> creerPromotion(promotionRepository, formation));

            if (seanceRepository.count() == 0) {
                creerSeances(seanceRepository, coursRepository, formation);
            }

            User auteur = userRepository.findByUsername("admin_admission");
            if (annonceRepository.count() == 0 && auteur != null) {
                creerAnnonces(annonceRepository, auteur, promotion);
            }

            if (albumRepository.count() == 0) {
                albumRepository.save(new AlbumPhoto(
                        "Visite du terminal pétrolier de Sendou",
                        "Sortie pédagogique de la promotion : découverte des installations de stockage et des procédures de sécurité.",
                        promotion,
                        LocalDate.now().minusDays(21)));
                System.out.println("✅ Photothèque : album de démonstration créé (sans cliché pour l'instant)");
            }

            creerEtudiantDemo(userRepository, roleRepository, etudiantRepository, passwordEncoder, promotion,
                    matriculeService);

            rattacherEtudiantsOrphelins(etudiantRepository, promotion);

            int matricules = matriculeService.attribuerAuxEtudiantsSansMatricule();
            if (matricules > 0) {
                System.out.println("✅ Matricules attribués à " + matricules + " étudiant(s)");
            }
        };
    }

    private Formation creerFormationEtCours(FormationRepository formationRepository, CoursRepository coursRepository) {
        Formation formation = new Formation();
        formation.setCode(CODE_FORMATION);
        formation.setIntitule("Master Pétrole & Gaz");
        formation.setDescription(
                "Formation d'ingénierie couvrant l'exploration, la production et la gestion des ressources "
                        + "pétrolières et gazières, adossée aux besoins de la filière sénégalaise.");
        formation = formationRepository.save(formation);

        Set<Cours> cours = new LinkedHashSet<>(List.of(
                cours(coursRepository, "GEO-101", "Géologie des réservoirs",
                        "Caractérisation des roches-mères, migration et pièges pétroliers.", 40L, "Semestre 1"),
                cours(coursRepository, "FOR-102", "Techniques de forage",
                        "Architecture des puits, fluides de forage et contrôle des venues.", 35L, "Semestre 1"),
                cours(coursRepository, "PRO-201", "Production et traitement",
                        "Séparation, traitement du gaz associé et optimisation de la production.", 45L, "Semestre 2"),
                cours(coursRepository, "HSE-202", "Sécurité et environnement",
                        "Analyse de risques, barrières de sécurité et réglementation environnementale.", 30L,
                        "Semestre 2"),
                cours(coursRepository, "ECO-203", "Économie pétrolière",
                        "Contrats pétroliers, fiscalité et évaluation économique des projets.", 25L, "Semestre 2")));

        formation.setCours(cours);
        formation = formationRepository.save(formation);
        System.out.println("✅ Formation « Master Pétrole & Gaz » créée avec " + cours.size() + " cours");
        return formation;
    }

    private Cours cours(CoursRepository coursRepository, String code, String titre, String description,
            Long volumeHoraire, String semestre) {
        Cours cours = new Cours();
        cours.setNom(code);
        cours.setTitre(titre);
        cours.setDescription(description);
        cours.setVolumeHoraire(volumeHoraire);
        cours.setSemestre(semestre);
        return coursRepository.save(cours);
    }

    private Promotion creerPromotion(PromotionRepository promotionRepository, Formation formation) {
        Promotion promotion = new Promotion();
        promotion.setCode(CODE_PROMOTION);
        promotion.setLibelle("Promotion 2026 — Master Pétrole & Gaz");
        promotion.setDateDebut(LocalDate.of(2026, 1, 12));
        promotion.setDateFin(LocalDate.of(2026, 12, 18));
        promotion.setFormation(formation);
        promotion.setProgramme(
                "Deux semestres académiques suivis d'un stage de fin d'études de quatre mois en entreprise. "
                        + "Le premier semestre pose les fondamentaux géologiques et techniques ; le second "
                        + "aborde la production, la sécurité et l'économie de la filière.");
        promotion.setActivitesParascolaires(
                "Visite du terminal pétrolier de Sendou · Conférence annuelle sur le contenu local · "
                        + "Ateliers de simulation de forage · Séminaire d'anglais technique.");
        Promotion enregistree = promotionRepository.save(promotion);
        System.out.println("✅ Promotion « " + enregistree.getLibelle() + " » créée");
        return enregistree;
    }

    private void creerSeances(SeanceRepository seanceRepository, CoursRepository coursRepository,
            Formation formation) {
        List<Cours> cours = coursRepository.findByFormations_Id(formation.getId());
        if (cours.isEmpty()) {
            return;
        }
        // Séances réparties sur le mois courant pour que le calendrier soit lisible
        // dès le premier lancement, quelle que soit la date de démarrage.
        LocalDateTime base = LocalDate.now().withDayOfMonth(1).atTime(9, 0);
        String[] types = { "cours", "td", "tp", "examen" };
        String[] lieux = { "Amphi A", "Salle B12", "Laboratoire 2", "Amphi A" };

        int index = 0;
        for (Cours c : cours) {
            for (int occurrence = 0; occurrence < 2; occurrence++) {
                Seance seance = new Seance();
                seance.setTitre(c.getTitre() + (occurrence == 0 ? " — séance d'ouverture" : " — travaux dirigés"));
                seance.setType(types[index % types.length]);
                seance.setDateHeure(base.plusDays(2L * index + occurrence * 7L).plusHours(occurrence * 3L));
                seance.setDureeMinutes(occurrence == 0 ? 120 : 90);
                seance.setLieuOuLien(lieux[index % lieux.length]);
                seance.setCours(c);
                seanceRepository.save(seance);
            }
            index++;
        }
        System.out.println("✅ Calendrier académique : " + (cours.size() * 2) + " séances créées");
    }

    private void creerAnnonces(AnnonceRepository annonceRepository, User auteur, Promotion promotion) {
        Annonce rentree = new Annonce(
                "Rentrée solennelle le 12 janvier",
                "La cérémonie de rentrée se tiendra à l'amphithéâtre A à 9 h. La présence de tous les étudiants "
                        + "de la promotion est requise. La remise des badges d'accès suivra la cérémonie.",
                EnumTypeAnnonce.ANNONCE, auteur, promotion);
        rentree.setEpinglee(true);
        annonceRepository.save(rentree);

        annonceRepository.save(new Annonce(
                "Ouverture de la bibliothèque le samedi",
                "À compter de ce mois, la bibliothèque reste ouverte le samedi de 9 h à 13 h. "
                        + "Les 4 500 ouvrages du fonds pétrolier et gazier sont consultables sur place.",
                EnumTypeAnnonce.MESSAGE, auteur, null));

        annonceRepository.save(new Annonce(
                "Mémo — procédure de justification d'absence",
                "Toute absence doit être justifiée sous 48 heures auprès du service de scolarité, "
                        + "pièce à l'appui. Au-delà de trois absences non justifiées, l'accès aux évaluations "
                        + "de fin de semestre est suspendu.",
                EnumTypeAnnonce.MEMO, auteur, null));

        System.out.println("✅ Annonces : 3 communications de démonstration créées (annonce, message, mémo)");
    }

    private void creerEtudiantDemo(UserRepository userRepository, RoleRepository roleRepository,
            EtudiantRepository etudiantRepository, PasswordEncoder passwordEncoder, Promotion promotion,
            MatriculeService matriculeService) {
        if (userRepository.findByUsername("etudiant_demo") != null) {
            return;
        }
        Role roleEtudiant = roleRepository.findByCode("ROLE_ETUDIANT");
        if (roleEtudiant == null) {
            return;
        }
        User user = new User();
        user.setUsername("etudiant_demo");
        user.setEmail("etudiant@inpg.sn");
        user.setPassword(passwordEncoder.encode("Etudiant@2026"));
        user.addRole(roleEtudiant);
        // Antérieure à tout contenu semé par cette classe : sans quoi l'isolation
        // temporelle (AnnonceController, ForumVisibiliteService) masquerait à ce
        // compte de démonstration le contenu de démonstration lui-même.
        user.setDateCreation(LocalDateTime.of(2020, 1, 1, 0, 0));
        user = userRepository.save(user);

        Etudiant etudiant = new Etudiant(user, "Ndiaye", "Fatou", "+221 77 000 01 00");
        etudiant.setPromotion(promotion);
        etudiant = etudiantRepository.save(etudiant);
        matriculeService.attribuerSiAbsent(etudiant);

        System.out.println("✅ Compte étudiant de démonstration : etudiant@inpg.sn / Etudiant@2026 ("
                + etudiant.getMatricule() + ")");
    }

    /**
     * Rattache à la promotion les étudiants créés avant l'introduction du lien.
     * Sans cela, leur espace resterait vide alors que leur compte est valide.
     */
    private void rattacherEtudiantsOrphelins(EtudiantRepository etudiantRepository, Promotion promotion) {
        List<Etudiant> orphelins = etudiantRepository.findAll().stream()
                .filter(etudiant -> etudiant.getPromotion() == null)
                .toList();
        if (orphelins.isEmpty()) {
            return;
        }
        orphelins.forEach(etudiant -> etudiant.setPromotion(promotion));
        etudiantRepository.saveAll(orphelins);
        System.out.println("✅ " + orphelins.size() + " étudiant(s) rattaché(s) à « " + promotion.getLibelle() + " »");
    }
}
