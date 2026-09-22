package inpg.sn.lms.espace_alumni.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import inpg.sn.lms.auth.entity.AlumniProfil;
import inpg.sn.lms.espace_alumni.dto.SituationProfessionnelleDto;
import inpg.sn.lms.espace_alumni.dto.SituationProfessionnelleRequest;
import inpg.sn.lms.espace_alumni.entity.SituationProfessionnelle;
import inpg.sn.lms.espace_alumni.enums.EnumStatutEmploi;
import inpg.sn.lms.espace_alumni.repository.SituationProfessionnelleRepository;

/**
 * Une situation « en recherche d'emploi » n'a ni intitulé de poste ni entreprise
 * (voir {@code AlumniController#validerSituation}, qui n'exige ces deux champs que
 * pour EN_EMPLOI/ENTREPRENARIAT) : ce service ne doit pas planter sur un
 * {@code String#trim()} appelé sur null.
 */
@ExtendWith(MockitoExtension.class)
class SituationProfessionnelleServiceTest {

    @Mock private SituationProfessionnelleRepository repository;

    @InjectMocks private SituationProfessionnelleService service;

    private AlumniProfil profil() {
        AlumniProfil profil = new AlumniProfil();
        profil.setId(1L);
        return profil;
    }

    @Test
    @DisplayName("Création d'une situation sans intitulé ni entreprise (recherche d'emploi) : pas de NullPointerException")
    void creation_sans_poste_ne_leve_pas_de_npe() {
        when(repository.save(any(SituationProfessionnelle.class))).thenAnswer(inv -> inv.getArgument(0));
        SituationProfessionnelleRequest requete = new SituationProfessionnelleRequest(
                null, null, LocalDate.of(2026, 1, 1), null, EnumStatutEmploi.RECHERCHE_EMPLOI);

        SituationProfessionnelleDto dto = service.creer(profil(), requete);

        assertThat(dto.intitulePoste()).isNull();
        assertThat(dto.entreprise()).isNull();
        assertThat(dto.statutEmploi()).isEqualTo(EnumStatutEmploi.RECHERCHE_EMPLOI);
    }

    @Test
    @DisplayName("Un intitulé/entreprise renseignés sont bien nettoyés (espaces superflus retirés)")
    void creation_avec_poste_nettoie_les_espaces() {
        when(repository.save(any(SituationProfessionnelle.class))).thenAnswer(inv -> inv.getArgument(0));
        SituationProfessionnelleRequest requete = new SituationProfessionnelleRequest(
                "  Ingénieure Procédés  ", "  Petrosen  ", LocalDate.of(2026, 1, 1), null, EnumStatutEmploi.EN_EMPLOI);

        SituationProfessionnelleDto dto = service.creer(profil(), requete);

        assertThat(dto.intitulePoste()).isEqualTo("Ingénieure Procédés");
        assertThat(dto.entreprise()).isEqualTo("Petrosen");
    }

    @Test
    @DisplayName("Modification vers une situation sans poste : pas de NullPointerException")
    void modification_sans_poste_ne_leve_pas_de_npe() {
        AlumniProfil profil = profil();
        SituationProfessionnelle existante = new SituationProfessionnelle(profil, "Ingénieure Procédés", "Petrosen",
                LocalDate.of(2025, 1, 1), null, EnumStatutEmploi.EN_EMPLOI);
        existante.setId(7L);
        when(repository.findById(7L)).thenReturn(java.util.Optional.of(existante));
        when(repository.save(any(SituationProfessionnelle.class))).thenAnswer(inv -> inv.getArgument(0));
        SituationProfessionnelleRequest requete = new SituationProfessionnelleRequest(
                null, null, LocalDate.of(2026, 1, 1), null, EnumStatutEmploi.POURSUITE_ETUDES);

        SituationProfessionnelleDto dto = service.modifier(7L, profil, requete);

        assertThat(dto).isNotNull();
        assertThat(dto.intitulePoste()).isNull();
        assertThat(dto.entreprise()).isNull();
    }
}
