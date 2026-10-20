package inpg.sn.lms.recrutement.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.ObjectMapper;

import inpg.sn.lms.espace_etudiant.entity.Formation;
import inpg.sn.lms.espace_etudiant.repository.FormationRepository;
import inpg.sn.lms.recrutement.service.NotificationService;
import inpg.sn.lms.recrutement.service.SessionCandidatureService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring Boot 4 sérialise les réponses HTTP avec Jackson 3 ({@code tools.jackson}), alors que le code parse la fiche
 * avec Jackson 2 ({@code com.fasterxml}). Un {@code JsonNode} de Jackson 2 glissé dans un DTO n'est pas reconnu par
 * Jackson 3, qui l'écrit comme un objet Java ordinaire : {@code "array": false, "containerNode": true…}. Les dix-sept
 * fiches de formation du site public arrivaient ainsi au navigateur sans un seul module, point clé ni débouché.
 *
 * <p>Le test sérialise la réponse avec le sérialiseur de Spring MVC lui-même : lire seulement le DTO ne voyait rien.
 */
class PublicControllerFicheFormationTest {

    private static final String FICHE = "{\"badgeLabel\":\"Executive MBA\","
            + "\"modules\":[{\"titre\":\"Économie de l'énergie\",\"items\":[\"Marchés\",\"Contrats\"]}],"
            + "\"pointsCles\":[{\"titre\":\"Alternance\"}]}";

    @Test
    void la_fiche_detaillee_arrive_au_navigateur_comme_du_json() throws Exception {
        FormationRepository formations = mock(FormationRepository.class);
        Formation formation = new Formation();
        formation.setCode("MBA-ECO-MAN-01");
        formation.setIntitule("MBA en Économie");
        formation.setFicheDetaillee(FICHE);
        when(formations.findById(5L)).thenReturn(Optional.of(formation));
        PublicController controller = new PublicController(mock(SessionCandidatureService.class),
                mock(NotificationService.class), formations, new ObjectMapper());

        ResponseEntity<?> reponse = controller.consulterFormation(5L);
        JsonNode json = JsonMapper.builder().build().readTree(JsonMapper.builder().build().writeValueAsString(reponse.getBody()));

        JsonNode fiche = json.get("ficheDetaillee");
        assertThat(fiche.get("badgeLabel").asString()).isEqualTo("Executive MBA");
        assertThat(fiche.get("modules").get(0).get("titre").asString()).isEqualTo("Économie de l'énergie");
        assertThat(fiche.get("modules").get(0).get("items")).hasSize(2);
        assertThat(fiche.has("containerNode")).as("un JsonNode sérialisé comme un objet Java").isFalse();
    }

    @Test
    void une_fiche_absente_reste_nulle() throws Exception {
        FormationRepository formations = mock(FormationRepository.class);
        Formation formation = new Formation();
        formation.setIntitule("Sans fiche");
        when(formations.findById(1L)).thenReturn(Optional.of(formation));
        PublicController controller = new PublicController(mock(SessionCandidatureService.class),
                mock(NotificationService.class), formations, new ObjectMapper());

        ResponseEntity<?> reponse = controller.consulterFormation(1L);
        JsonNode json = JsonMapper.builder().build().readTree(JsonMapper.builder().build().writeValueAsString(reponse.getBody()));

        assertThat(json.get("ficheDetaillee").isNull()).isTrue();
    }
}
