package inpg.sn.lms.espace_alumni.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.espace_alumni.entity.DocumentPartage;
import inpg.sn.lms.espace_alumni.repository.DocumentPartageRepository;
import inpg.sn.lms.espace_alumni.service.AlumniDocumentStorageService;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;

/**
 * Partage de documents alumni : validation d'entrée et cloisonnement par
 * propriétaire.
 *
 * <p>Le risque à couvrir est symétrique à celui du forum : n'importe quel alumni
 * peut consulter et télécharger n'importe quel document (c'est l'objet même du
 * partage), mais seul son auteur peut le supprimer.
 */
@ExtendWith(MockitoExtension.class)
class AlumniDocumentControllerTest {

    @Mock
    private DocumentPartageRepository documentRepository;
    @Mock
    private AlumniDocumentStorageService storage;
    @Mock
    private UtilisateurCourantService utilisateurCourant;
    @Mock
    private Authentication authentication;

    private AlumniDocumentController controller;
    private User auteur;

    @BeforeEach
    void setUp() {
        controller = new AlumniDocumentController(documentRepository, storage, utilisateurCourant);
        auteur = new User();
        auteur.setId(42L);
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
    @DisplayName("Titre vide : 400, rien n'est enregistré")
    void ajouter_sans_titre_est_refuse() throws IOException {
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(auteur);
        MockMultipartFile fichier = new MockMultipartFile("fichier", "cv.pdf", "application/pdf", new byte[] { 1 });

        ResponseEntity<?> reponse = controller.ajouter("   ", null, fichier, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(documentRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Extension refusée par ValidationFichier : 400, message renvoyé au client")
    void ajouter_avec_fichier_invalide_est_refuse() throws IOException {
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(auteur);
        MockMultipartFile fichier = new MockMultipartFile("fichier", "script.exe", "application/octet-stream",
                new byte[] { 1 });
        when(storage.enregistrer(auteur, fichier, "Un exécutable", null))
                .thenThrow(new IllegalArgumentException("Format de fichier non autorisé."));

        ResponseEntity<?> reponse = controller.ajouter("Un exécutable", null, fichier, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(documentRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Dépôt valide : enregistré via le service de stockage puis persisté")
    void ajouter_valide_est_enregistre() throws IOException {
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(auteur);
        MockMultipartFile fichier = new MockMultipartFile("fichier", "cv.pdf", "application/pdf", new byte[] { 1 });
        DocumentPartage document = new DocumentPartage(auteur, "Mon CV", null, "cv.pdf", "uuid-cv.pdf",
                "application/pdf");
        when(storage.enregistrer(auteur, fichier, "Mon CV", null)).thenReturn(document);

        ResponseEntity<?> reponse = controller.ajouter("Mon CV", null, fichier, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(documentRepository).save(document);
    }

    @Test
    @DisplayName("Suppression par un autre alumni que l'auteur : 403, rien n'est supprimé")
    void supprimer_document_d_autrui_est_refusee() {
        User autreAlumni = new User();
        autreAlumni.setId(999L);
        DocumentPartage document = new DocumentPartage(autreAlumni, "Titre", null, "f.pdf", "uuid-f.pdf", null);
        setId(document, 3L);
        when(documentRepository.findById(3L)).thenReturn(Optional.of(document));
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(auteur);

        ResponseEntity<?> reponse = controller.supprimer(3L, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(documentRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Suppression par son auteur : autorisée")
    void supprimer_son_propre_document_est_autorise() {
        DocumentPartage document = new DocumentPartage(auteur, "Titre", null, "f.pdf", "uuid-f.pdf", null);
        setId(document, 3L);
        when(documentRepository.findById(3L)).thenReturn(Optional.of(document));
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(auteur);

        ResponseEntity<?> reponse = controller.supprimer(3L, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(documentRepository).delete(document);
    }
}
