package inpg.sn.lms.recrutementformateur.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import inpg.sn.lms.recrutement.entity.FichierStocke;
import inpg.sn.lms.recrutementformateur.dto.OffreFormateurDto;
import inpg.sn.lms.recrutementformateur.entity.OffreRecrutementFormateur;
import inpg.sn.lms.recrutementformateur.enums.EnumStatutOffreFormateur;
import inpg.sn.lms.recrutementformateur.enums.EnumTypeContratFormateur;
import inpg.sn.lms.recrutementformateur.repository.OffreRecrutementFormateurRepository;

/** La vitrine ne livre la fiche de poste que d'une offre publiée : un brouillon reste confidentiel. */
@ExtendWith(MockitoExtension.class)
class RecrutementFormateurPublicControllerTest {

    @Mock private OffreRecrutementFormateurRepository offreRepository;

    @TempDir Path dossier;

    private RecrutementFormateurPublicController controller;

    @BeforeEach
    void setUp() {
        controller = new RecrutementFormateurPublicController(offreRepository);
    }

    private OffreRecrutementFormateur offre(EnumStatutOffreFormateur statut, FichierStocke fiche) {
        OffreRecrutementFormateur o = new OffreRecrutementFormateur();
        o.setId(1L);
        o.setIntitule("Formateur HSE");
        o.setTypeContrat(EnumTypeContratFormateur.CDD);
        o.setStatut(statut);
        o.setFichePoste(fiche);
        return o;
    }

    private FichierStocke fichePresenteSurDisque() throws Exception {
        Path chemin = Files.writeString(dossier.resolve("fiche.pdf"), "%PDF-1.4 contenu");
        FichierStocke f = new FichierStocke();
        f.setId(10L);
        f.setNomOriginal("fiche-hse.pdf");
        f.setCheminStockage(chemin.toString());
        f.setMimeType("application/pdf");
        return f;
    }

    @Test
    @DisplayName("Offre publiée : la fiche se télécharge, en pièce jointe, sous son nom d'origine")
    void fiche_d_une_offre_publiee_est_servie() throws Exception {
        when(offreRepository.findById(1L))
                .thenReturn(Optional.of(offre(EnumStatutOffreFormateur.PUBLIEE, fichePresenteSurDisque())));

        ResponseEntity<?> reponse = controller.fichePoste(1L);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponse.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .startsWith("attachment").contains("fiche-hse.pdf");
        assertThat((byte[]) reponse.getBody()).isEqualTo("%PDF-1.4 contenu".getBytes());
    }

    @Test
    @DisplayName("Offre en brouillon ou clôturée : la fiche n'est pas servie (404)")
    void fiche_d_une_offre_non_publiee_reste_confidentielle() throws Exception {
        for (EnumStatutOffreFormateur statut : new EnumStatutOffreFormateur[] {
                EnumStatutOffreFormateur.BROUILLON, EnumStatutOffreFormateur.CLOTUREE }) {
            when(offreRepository.findById(1L)).thenReturn(Optional.of(offre(statut, fichePresenteSurDisque())));
            assertThat(controller.fichePoste(1L).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Test
    @DisplayName("Offre publiée sans fiche : 404")
    void offre_sans_fiche() {
        when(offreRepository.findById(1L)).thenReturn(Optional.of(offre(EnumStatutOffreFormateur.PUBLIEE, null)));

        assertThat(controller.fichePoste(1L).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("La vue publique d'une offre annonce le nom de la fiche, ou rien si elle n'en a pas")
    void vue_publique_annonce_le_nom_de_la_fiche() throws Exception {
        assertThat(RecrutementFormateurPublicController
                .toDto(offre(EnumStatutOffreFormateur.PUBLIEE, fichePresenteSurDisque())).fichePosteNom())
                .isEqualTo("fiche-hse.pdf");
        OffreFormateurDto sans = RecrutementFormateurPublicController
                .toDto(offre(EnumStatutOffreFormateur.PUBLIEE, null));
        assertThat(sans.fichePosteNom()).isNull();
    }
}
