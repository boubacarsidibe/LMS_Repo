package inpg.sn.lms.personnel.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import inpg.sn.lms.auth.entity.Role;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;
import inpg.sn.lms.personnel.dto.ActiviteCreateRequest;
import inpg.sn.lms.personnel.dto.DepartementCreateRequest;
import inpg.sn.lms.personnel.entity.ActivitePersonnel;
import inpg.sn.lms.personnel.entity.Departement;
import inpg.sn.lms.personnel.enums.EnumTypeActivite;
import inpg.sn.lms.personnel.repository.ActivitePersonnelRepository;
import inpg.sn.lms.personnel.repository.DepartementRepository;
import inpg.sn.lms.personnel.service.RapportActivitePdfService;

/**
 * {@code /api/personnel} — suivi des activités du personnel INPG (TDR §I).
 *
 * <p>Couvre la validation de la saisie, l'auteur toujours dérivé du jeton (jamais
 * transmis par le client), et le double droit de suppression (auteur ou rôle de
 * modération) sur lequel repose la story.
 */
@ExtendWith(MockitoExtension.class)
class PersonnelActiviteControllerTest {

    @Mock
    private DepartementRepository departementRepository;
    @Mock
    private ActivitePersonnelRepository activiteRepository;
    @Mock
    private UtilisateurCourantService utilisateurCourant;
    @Mock
    private RapportActivitePdfService rapportPdfService;
    @Mock
    private Authentication authentication;

    private PersonnelActiviteController controller;

    private User formateur(long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("f" + id);
        user.setRoles(Set.of(role("ROLE_FORMATEUR")));
        return user;
    }

    private User moderateur(long id, String code) {
        User user = new User();
        user.setId(id);
        user.setUsername("m" + id);
        user.setRoles(Set.of(role(code)));
        return user;
    }

    private Role role(String code) {
        Role r = new Role();
        r.setCode(code);
        return r;
    }

    private Departement departement(long id) {
        Departement d = new Departement();
        d.setId(id);
        d.setCode("DSI");
        d.setLibelle("Direction du système d'information");
        d.setActif(true);
        return d;
    }

    @BeforeEach
    void setUp() {
        controller = new PersonnelActiviteController(departementRepository, activiteRepository, utilisateurCourant,
                rapportPdfService);
        lenient().when(utilisateurCourant.nomAffiche(any())).thenReturn("Nom Affiché");
    }

    @Test
    @DisplayName("refuse la saisie sans département, titre, mois ou année")
    void saisie_incomplete_refusee() {
        ResponseEntity<?> reponse = controller.saisirActivite(
                new ActiviteCreateRequest(null, "FORMATION", "Titre", null, 1, 2026), authentication);
        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("refuse un mois hors bornes")
    void saisie_mois_invalide_refusee() {
        ResponseEntity<?> reponse = controller.saisirActivite(
                new ActiviteCreateRequest(1L, "FORMATION", "Titre", null, 13, 2026), authentication);
        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("refuse un type d'activité invalide")
    void saisie_type_invalide_refusee() {
        when(departementRepository.findById(1L)).thenReturn(Optional.of(departement(1L)));
        ResponseEntity<?> reponse = controller.saisirActivite(
                new ActiviteCreateRequest(1L, "AUTRE_CHOSE", "Titre", null, 3, 2026), authentication);
        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("dépose l'activité avec l'auteur dérivé du jeton, jamais du corps de la requête")
    void saisie_valide_enregistree_avec_auteur_du_jeton() {
        Departement d = departement(1L);
        User auteur = formateur(9L);
        when(departementRepository.findById(1L)).thenReturn(Optional.of(d));
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(auteur);
        when(activiteRepository.save(any(ActivitePersonnel.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> reponse = controller.saisirActivite(
                new ActiviteCreateRequest(1L, "PROJET", "Refonte du site", "Description", 3, 2026), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var captureur = org.mockito.ArgumentCaptor.forClass(ActivitePersonnel.class);
        verify(activiteRepository).save(captureur.capture());
        assertThat(captureur.getValue().getAuteur()).isEqualTo(auteur);
        assertThat(captureur.getValue().getTypeActivite()).isEqualTo(EnumTypeActivite.PROJET);
    }

    @Test
    @DisplayName("l'auteur peut retirer sa propre activité")
    void suppression_par_auteur_autorisee() {
        Departement d = departement(1L);
        User auteur = formateur(9L);
        ActivitePersonnel activite = new ActivitePersonnel(d, auteur, EnumTypeActivite.AUTRE, "Titre", null, 3, 2026);
        when(activiteRepository.findById(5L)).thenReturn(Optional.of(activite));
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(auteur);

        ResponseEntity<?> reponse = controller.supprimerActivite(5L, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(activiteRepository).delete(activite);
    }

    @Test
    @DisplayName("la pédagogie peut retirer l'activité d'un tiers (modération)")
    void suppression_par_moderateur_autorisee() {
        Departement d = departement(1L);
        User auteur = formateur(9L);
        User pedagogie = moderateur(2L, "ROLE_RESPONSABLE_PEDAGOGIQUE");
        ActivitePersonnel activite = new ActivitePersonnel(d, auteur, EnumTypeActivite.AUTRE, "Titre", null, 3, 2026);
        when(activiteRepository.findById(5L)).thenReturn(Optional.of(activite));
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(pedagogie);

        ResponseEntity<?> reponse = controller.supprimerActivite(5L, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(activiteRepository).delete(activite);
    }

    @Test
    @DisplayName("un tiers sans rôle de modération ne peut pas retirer l'activité d'un autre")
    void suppression_par_tiers_refusee() {
        Departement d = departement(1L);
        User auteur = formateur(9L);
        User autreFormateur = formateur(10L);
        ActivitePersonnel activite = new ActivitePersonnel(d, auteur, EnumTypeActivite.AUTRE, "Titre", null, 3, 2026);
        when(activiteRepository.findById(5L)).thenReturn(Optional.of(activite));
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(autreFormateur);

        ResponseEntity<?> reponse = controller.supprimerActivite(5L, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(activiteRepository, never()).delete(any());
    }

    @Test
    @DisplayName("refuse un code de département déjà utilisé")
    void creation_departement_code_duplique_refusee() {
        when(departementRepository.findByCode("DSI")).thenReturn(Optional.of(departement(1L)));
        ResponseEntity<?> reponse = controller.creerDepartement(new DepartementCreateRequest("DSI", "Autre libellé"));
        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(departementRepository, never()).save(any());
    }

    @Test
    @DisplayName("crée un département valide")
    void creation_departement_valide() {
        when(departementRepository.findByCode("DSI")).thenReturn(Optional.empty());
        when(departementRepository.save(any(Departement.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> reponse = controller.creerDepartement(
                new DepartementCreateRequest("DSI", "Direction du système d'information"));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(departementRepository).save(any(Departement.class));
    }

    @Test
    @DisplayName("liste les activités du mois pour un département donné")
    void liste_activites_filtrees_par_departement() {
        Departement d = departement(1L);
        ActivitePersonnel a = new ActivitePersonnel(d, formateur(9L), EnumTypeActivite.FORMATION, "Titre", null, 3, 2026);
        when(activiteRepository.findByDepartementIdAndAnneeAndMoisOrderByDateCreationDesc(1L, 2026, 3))
                .thenReturn(List.of(a));

        ResponseEntity<?> reponse = controller.listerActivites(1L, 3, 2026);

        assertThat(reponse.getStatusCode().is2xxSuccessful()).isTrue();
        verify(activiteRepository).findByDepartementIdAndAnneeAndMoisOrderByDateCreationDesc(1L, 2026, 3);
        verify(activiteRepository, never())
                .findByAnneeAndMoisOrderByDepartement_LibelleAscDateCreationDesc(anyInt(), anyInt());
    }
}
