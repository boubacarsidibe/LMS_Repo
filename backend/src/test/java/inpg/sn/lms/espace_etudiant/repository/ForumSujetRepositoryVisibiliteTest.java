package inpg.sn.lms.espace_etudiant.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.auth.repository.UserRepository;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.ForumSujet;
import inpg.sn.lms.espace_etudiant.entity.PorteeForumSujet;
import inpg.sn.lms.espace_etudiant.entity.Promotion;

/**
 * Vérifie contre une vraie base (H2 en mémoire, schéma généré par Hibernate)
 * que {@link ForumSujetRepository#findVisiblesPour} et
 * {@link ForumSujetRepository#findGeneraux} renvoient réellement ce que leur
 * JPQL prétend renvoyer.
 *
 * <p>{@link inpg.sn.lms.espace_etudiant.service.ForumVisibiliteServiceTest}
 * mockait déjà ces deux méthodes de dépôt : il prouve que le service appelle
 * la bonne méthode avec les bons arguments, jamais que la requête elle-même
 * est correcte. Un piège JPQL classique menace précisément ce genre de
 * requête : accéder à {@code s.cours.id} ou {@code s.promotion.id} dans une
 * clause {@code WHERE} peut, selon le fournisseur JPA, ajouter une jointure
 * implicite sur l'association — une jointure {@code INNER} exclurait alors
 * silencieusement toute ligne où cette association est {@code null}, y
 * compris les lignes qu'une autre branche du {@code OR} aurait dû laisser
 * passer. Seule l'exécution réelle du JPQL peut trancher cette question.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class ForumSujetRepositoryVisibiliteTest {

    @Autowired
    private ForumSujetRepository sujetRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EtudiantRepository etudiantRepository;
    @Autowired
    private inpg.sn.lms.espace_etudiant.repository.CoursRepository coursRepository;
    @Autowired
    private inpg.sn.lms.espace_etudiant.repository.PromotionRepository promotionRepository;

    private User nouvelUtilisateur(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("hash");
        user.setEmail(username + "@inpg.sn");
        return userRepository.save(user);
    }

    /**
     * Reproduit le scénario signalé : un étudiant qui a au moins un cours
     * autorisé (donc {@code findVisiblesPour}, pas {@code findGeneraux}) doit
     * quand même voir le salon général de sa promotion et les sujets ouverts
     * par ses camarades — pas seulement les sujets rattachés à ce cours précis.
     */
    @Test
    @DisplayName("Un étudiant avec un cours autorisé voit aussi le salon général et les sujets de sa promotion")
    void etudiant_avec_cours_voit_aussi_le_general_et_sa_promotion() {
        Promotion promotion = promotionRepository.save(promotionBrute("MPG-2026", "Master Pétrole & Gaz"));
        Cours cours = coursRepository.save(coursBrut("Thermodynamique"));

        User auteurGeneral = nouvelUtilisateur("camarade1");
        User auteurCours = nouvelUtilisateur("camarade2");
        User auteurPromotion = nouvelUtilisateur("camarade3");

        ForumSujet sujetGeneral = new ForumSujet("Bienvenue à tous", auteurGeneral, PorteeForumSujet.ETUDIANT);
        ForumSujet sujetCours = new ForumSujet("Question sur le TD", cours, auteurCours);
        ForumSujet sujetPromotion = new ForumSujet("Sortie de promotion", promotion, auteurPromotion);
        sujetRepository.save(sujetGeneral);
        sujetRepository.save(sujetCours);
        sujetRepository.save(sujetPromotion);

        List<ForumSujet> visibles = sujetRepository.findVisiblesPour(List.of(cours.getId()), promotion.getId());

        assertThat(visibles)
                .as("l'étudiant a un cours autorisé : findVisiblesPour est la méthode réellement appelée")
                .extracting(ForumSujet::getTitre)
                .containsExactlyInAnyOrder("Bienvenue à tous", "Question sur le TD", "Sortie de promotion");
    }

    @Test
    @DisplayName("findVisiblesPour n'expose pas le sujet d'une autre promotion ni d'un autre cours")
    void etudiant_ne_voit_pas_les_sujets_hors_perimetre() {
        Promotion maPromotion = promotionRepository.save(promotionBrute("MPG-2026", "Master Pétrole & Gaz"));
        Promotion autrePromotion = promotionRepository.save(promotionBrute("GEN-2026", "Génie civil"));
        Cours monCours = coursRepository.save(coursBrut("Thermodynamique"));
        Cours autreCours = coursRepository.save(coursBrut("Comptabilité"));
        User auteur = nouvelUtilisateur("camarade1");

        sujetRepository.save(new ForumSujet("Hors promotion", autrePromotion, auteur));
        sujetRepository.save(new ForumSujet("Hors cursus", autreCours, auteur));

        List<ForumSujet> visibles = sujetRepository.findVisiblesPour(List.of(monCours.getId()), maPromotion.getId());

        assertThat(visibles).extracting(ForumSujet::getTitre).doesNotContain("Hors promotion", "Hors cursus");
    }

    @Test
    @DisplayName("findGeneraux (étudiant sans cours autorisé) voit aussi les sujets de sa promotion")
    void etudiant_sans_cours_voit_generaux_et_sa_promotion() {
        Promotion promotion = promotionRepository.save(promotionBrute("MPG-2026", "Master Pétrole & Gaz"));
        User auteur = nouvelUtilisateur("camarade1");

        sujetRepository.save(new ForumSujet("Bienvenue à tous", auteur, PorteeForumSujet.ETUDIANT));
        sujetRepository.save(new ForumSujet("Sortie de promotion", promotion, auteur));

        List<ForumSujet> visibles = sujetRepository.findGeneraux(promotion.getId());

        assertThat(visibles).extracting(ForumSujet::getTitre)
                .containsExactlyInAnyOrder("Bienvenue à tous", "Sortie de promotion");
    }

    private Promotion promotionBrute(String code, String libelle) {
        Promotion promotion = new Promotion();
        promotion.setCode(code);
        promotion.setLibelle(libelle);
        return promotion;
    }

    private Cours coursBrut(String titre) {
        Cours cours = new Cours();
        cours.setNom(titre);
        cours.setTitre(titre);
        cours.setDescription("Description");
        cours.setVolumeHoraire(20L);
        return cours;
    }
}
