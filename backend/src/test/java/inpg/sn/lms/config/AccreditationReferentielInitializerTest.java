package inpg.sn.lms.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import inpg.sn.lms.accreditation.entity.CritereAccreditation;
import inpg.sn.lms.accreditation.repository.CritereAccreditationRepository;
import org.mockito.Mockito;

/**
 * Amorçage du référentiel DRCQ : les 31 conditions sont créées sur une base
 * vierge, et une seconde exécution n'en recrée aucune (idempotence par code).
 */
class AccreditationReferentielInitializerTest {

    private final AccreditationReferentielInitializer initializer = new AccreditationReferentielInitializer();

    @Test
    void amorce_les_31_criteres_sur_base_vierge() throws Exception {
        CritereAccreditationRepository repo = Mockito.mock(CritereAccreditationRepository.class);
        when(repo.findByCode(anyString())).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        initializer.initReferentielAccreditation(repo, new ObjectMapper()).run();

        verify(repo, times(31)).save(any(CritereAccreditation.class));
    }

    @Test
    void ne_recree_rien_si_les_codes_existent_deja() throws Exception {
        CritereAccreditationRepository repo = Mockito.mock(CritereAccreditationRepository.class);
        when(repo.findByCode(anyString()))
                .thenReturn(Optional.of(new CritereAccreditation("déjà là", null)));

        initializer.initReferentielAccreditation(repo, new ObjectMapper()).run();

        verify(repo, never()).save(any());
    }

    @Test
    void chaque_critere_amorce_porte_un_code_une_section_et_un_ordre() throws Exception {
        CritereAccreditationRepository repo = Mockito.mock(CritereAccreditationRepository.class);
        when(repo.findByCode(anyString())).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        initializer.initReferentielAccreditation(repo, new ObjectMapper()).run();

        var capteur = org.mockito.ArgumentCaptor.forClass(CritereAccreditation.class);
        verify(repo, times(31)).save(capteur.capture());
        assertThat(capteur.getAllValues()).allSatisfy(c -> {
            assertThat(c.getCode()).isNotBlank();
            assertThat(c.getSection()).isNotBlank();
            assertThat(c.getOrdre()).isGreaterThan(0);
            assertThat(c.getLibelle()).isNotBlank();
        });
        assertThat(capteur.getAllValues()).extracting(CritereAccreditation::getCode)
                .contains("1.1", "1.12", "2.6", "3.1", "4.5");
    }
}
