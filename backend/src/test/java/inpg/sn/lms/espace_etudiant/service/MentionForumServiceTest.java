package inpg.sn.lms.espace_etudiant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.entity.Formateur;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.espace_etudiant.dto.MentionnableDto;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.ForumMention;
import inpg.sn.lms.espace_etudiant.entity.ForumMessage;
import inpg.sn.lms.espace_etudiant.entity.ForumSujet;
import inpg.sn.lms.espace_etudiant.entity.Notification;
import inpg.sn.lms.espace_etudiant.entity.Promotion;
import inpg.sn.lms.espace_etudiant.repository.ForumMentionRepository;
import inpg.sn.lms.espace_etudiant.repository.NotificationRepository;

/**
 * Périmètre des mentions au forum.
 *
 * <p>Deux risques, tous deux silencieux, justifient ces tests.
 *
 * <p><strong>Divulguer l'annuaire.</strong> Une autocomplétion qui proposerait tout
 * l'établissement révélerait les noms de toutes les promotions à quiconque ouvre un
 * champ de saisie. Les personnes proposées doivent être exactement celles qui
 * participent déjà au fil.
 *
 * <p><strong>Contourner le périmètre par la requête.</strong> La liste envoyée par le
 * navigateur ne fait pas foi : un identifiant glissé à la main y notifierait n'importe
 * qui, en lui montrant au passage le titre d'un fil auquel il n'a pas accès. Le serveur
 * doit donc recouper, et non faire confiance.
 */
@ExtendWith(MockitoExtension.class)
class MentionForumServiceTest {

    @Mock
    private EtudiantRepository etudiantRepository;

    @Mock
    private PromotionService promotionService;

    @Mock
    private ForumMentionRepository mentionRepository;

    @Mock
    private NotificationRepository notificationRepository;

    private MentionForumService service;

    private User userAuteur;
    private User userCamarade;
    private User userFormateur;
    private Cours cours;
    private ForumSujet sujet;

    @BeforeEach
    void preparer() {
        service = new MentionForumService(etudiantRepository, promotionService, mentionRepository,
                notificationRepository);

        userAuteur = user(101L, "fatou_ndiaye");
        userCamarade = user(102L, "cheikh_fall");
        userFormateur = user(201L, "moussa_ndiaye");

        Promotion promotion = new Promotion();
        setId(promotion, 1L);

        cours = new Cours();
        setId(cours, 9L);
        cours.setNom("DS-101");
        cours.setTitre("Introduction au Machine Learning");
        cours.getFormateurs().add(formateur(userFormateur, "Moussa", "Ndiaye"));

        sujet = new ForumSujet("Question sur le TP 2", cours, userAuteur);
        setId(sujet, 42L);

        // lenient : ce decor decrit le fil, mais les tests qui sortent tot — liste de
        // citations vide — ne l'interrogent pas, et le mode strict y verrait un oubli.
        lenient().when(promotionService.desFormationsDuCours(cours)).thenReturn(List.of(promotion));
        lenient().when(etudiantRepository.findByPromotion_IdIn(List.of(1L)))
                .thenReturn(List.of(etudiant(userAuteur, "Fatou", "Ndiaye"),
                        etudiant(userCamarade, "Cheikh", "Fall")));
    }

    private User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private Etudiant etudiant(User user, String prenom, String nom) {
        Etudiant etudiant = new Etudiant();
        etudiant.setId(user.getId());
        etudiant.setUser(user);
        etudiant.setPrenom(prenom);
        etudiant.setNom(nom);
        return etudiant;
    }

    private Formateur formateur(User user, String prenom, String nom) {
        Formateur formateur = new Formateur();
        formateur.setId(user.getId());
        formateur.setUser(user);
        formateur.setPrenom(prenom);
        formateur.setNom(nom);
        return formateur;
    }

    private void setId(Object entite, Long id) {
        try {
            var champ = entite.getClass().getDeclaredField("id");
            champ.setAccessible(true);
            champ.set(entite, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Impossible de fixer l'id", e);
        }
    }

    @Test
    @DisplayName("Les citables d'un fil de cours sont ses étudiants et ses formateurs")
    void les_citables_couvrent_la_promotion_et_les_formateurs() {
        List<MentionnableDto> citables = service.citablesDans(sujet, userAuteur);

        assertThat(citables).extracting(MentionnableDto::id)
                .containsExactlyInAnyOrder(102L, 201L);
        assertThat(citables).extracting(MentionnableDto::role)
                .containsExactlyInAnyOrder("Étudiant", "Formateur");
    }

    /** Se citer soi-même s'enverrait une notification, et n'a aucun sens. */
    @Test
    @DisplayName("Le demandeur ne figure pas parmi les personnes citables")
    void le_demandeur_est_ecarte() {
        assertThat(service.citablesDans(sujet, userAuteur))
                .extracting(MentionnableDto::id)
                .doesNotContain(101L);
    }

    /**
     * Le cas qui compte. Un identifiant hors périmètre, glissé dans la requête, ne doit
     * ni créer de mention ni déposer de notification — sinon le titre du fil parvient à
     * quelqu'un qui n'y a pas accès.
     */
    @Test
    @DisplayName("Un identifiant hors périmètre est ignoré, sans mention ni notification")
    void un_cite_hors_perimetre_est_ignore() {
        ForumMessage message = new ForumMessage(sujet, userAuteur, "Bonjour @Inconnu");

        List<ForumMention> posees = service.enregistrer(message, List.of(999L), userAuteur);

        assertThat(posees).isEmpty();
        verify(mentionRepository, never()).save(any(ForumMention.class));
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("Citer un camarade crée la mention et dépose une notification")
    void citer_un_camarade_notifie() {
        when(mentionRepository.save(any(ForumMention.class))).thenAnswer(a -> a.getArgument(0));
        ForumMessage message = new ForumMessage(sujet, userAuteur, "Tu as compris @Cheikh Fall ?");

        List<ForumMention> posees = service.enregistrer(message, List.of(102L), userAuteur);

        assertThat(posees).hasSize(1);
        assertThat(posees.get(0).getCite().getId()).isEqualTo(102L);
        verify(notificationRepository).save(any(Notification.class));
    }

    /** Citer deux fois la même personne dans un message ne la prévient qu'une fois. */
    @Test
    @DisplayName("Un doublon dans la liste ne notifie qu'une seule fois")
    void un_doublon_ne_notifie_qu_une_fois() {
        when(mentionRepository.save(any(ForumMention.class))).thenAnswer(a -> a.getArgument(0));
        ForumMessage message = new ForumMessage(sujet, userAuteur, "@Cheikh @Cheikh");

        assertThat(service.enregistrer(message, List.of(102L, 102L), userAuteur)).hasSize(1);
    }

    @Test
    @DisplayName("Aucune personne citée : rien n'est écrit")
    void aucune_mention_n_ecrit_rien() {
        ForumMessage message = new ForumMessage(sujet, userAuteur, "Merci !");

        assertThat(service.enregistrer(message, List.of(), userAuteur)).isEmpty();
        assertThat(service.enregistrer(message, null, userAuteur)).isEmpty();
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    /** Un formateur cité n'a pas de fiche étudiant : la recherche doit tout de même aboutir. */
    @Test
    @DisplayName("Citer un formateur fonctionne, bien qu'il n'ait pas de fiche étudiant")
    void citer_un_formateur_fonctionne() {
        when(mentionRepository.save(any(ForumMention.class))).thenAnswer(a -> a.getArgument(0));
        ForumMessage message = new ForumMessage(sujet, userAuteur, "@Moussa Ndiaye une precision ?");

        List<ForumMention> posees = service.enregistrer(message, List.of(201L), userAuteur);

        assertThat(posees).hasSize(1);
        assertThat(posees.get(0).getCite().getId()).isEqualTo(201L);
    }

    /**
     * Un fil de l'espace alumni ne doit jamais retomber sur la branche « salon
     * général », qui proposerait la promotion de l'auteur — or l'auteur est un
     * alumni dont la ligne {@code Etudiant} existe toujours (migration V12) : ses
     * anciens camarades, encore étudiants, seraient à tort proposés comme
     * citables alors qu'ils n'ont pas accès au fil alumni.
     */
    @Test
    @DisplayName("Un fil alumni ne propose que ses participants, jamais la promotion de l'auteur")
    void fil_alumni_ne_propose_que_ses_participants() {
        User userAutreAlumni = user(301L, "awa_ndoye");
        inpg.sn.lms.espace_etudiant.entity.ForumSujet sujetAlumni = new inpg.sn.lms.espace_etudiant.entity.ForumSujet(
                "Retrouvailles promo 2020", userAuteur, inpg.sn.lms.espace_etudiant.entity.PorteeForumSujet.ALUMNI);
        sujetAlumni.getMessages().add(new ForumMessage(sujetAlumni, userAuteur, "Salut !"));
        sujetAlumni.getMessages().add(new ForumMessage(sujetAlumni, userAutreAlumni, "Salut, ça fait plaisir !"));
        // L'auteur a toujours une fiche étudiant (conversion sans suppression) : si la
        // branche « salon général » était prise par erreur, ce mock la ferait apparaître.
        lenient().when(etudiantRepository.findByUserId(301L))
                .thenReturn(java.util.Optional.of(etudiant(userAutreAlumni, "Awa", "Ndoye")));

        List<MentionnableDto> citables = service.citablesDans(sujetAlumni, userAuteur);

        assertThat(citables).extracting(MentionnableDto::id).containsExactly(301L);
        assertThat(citables).extracting(MentionnableDto::role).containsExactly("Alumni");
        // Le camarade encore étudiant (102L) et le formateur (201L) du décor
        // partagé n'ont pas posté dans ce fil : ils ne doivent pas apparaître.
        assertThat(citables).extracting(MentionnableDto::id).doesNotContain(102L, 201L);
    }
}
