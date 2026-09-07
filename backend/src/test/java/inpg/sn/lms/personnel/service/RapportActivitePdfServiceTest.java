package inpg.sn.lms.personnel.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.personnel.entity.ActivitePersonnel;
import inpg.sn.lms.personnel.entity.Departement;
import inpg.sn.lms.personnel.enums.EnumTypeActivite;

/**
 * {@link RapportActivitePdfService} : les rapports département et global se
 * génèrent en PDF valide, y compris avec une activité au titre hors WinAnsi et
 * une période sans aucune saisie.
 */
class RapportActivitePdfServiceTest {

    private final RapportActivitePdfService service = new RapportActivitePdfService();

    private ActivitePersonnel activite(String libelleDept, String auteur, EnumTypeActivite type, String titre) {
        Departement departement = new Departement();
        departement.setId((long) libelleDept.hashCode());
        departement.setLibelle(libelleDept);
        User user = new User();
        user.setUsername(auteur);
        return new ActivitePersonnel(departement, user, type, titre, "Description détaillée", 8, 2026);
    }

    @Test
    @DisplayName("rapport département : PDF valide, activité au titre exotique tolérée")
    void rapport_departement_valide() {
        byte[] pdf = service.genererRapportDepartement("Géosciences", 8, 2026, List.of(
                activite("Géosciences", "a.diop", EnumTypeActivite.RECHERCHE, "Forage 🛢 profond"),
                activite("Géosciences", "b.sow", EnumTypeActivite.FORMATION, "Atelier SIG")));

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("rapport global : PDF valide même sans aucune activité")
    void rapport_global_sans_activite() {
        assertThat(service.genererRapportGlobal(8, 2026, List.of())).isNotEmpty();
    }
}
