package inpg.sn.lms.recrutement.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SessionCandidatureRequest {

    private String libelle;
    private LocalDate dateOuverture;
    private LocalDate dateFermeture;
    private boolean estOuverte;
    private boolean criteresActifs;
    private String criteresNiveauEtudeMinimum;
    private Integer criteresAgeMinimum;
    private Integer criteresAgeMaximum;
    private String criteresGenre;
    private Integer criteresExperienceMinimale;
    private Float seuilReussiteTest;
    private String infoFraisScolarite;
    private String infoCarteEtudiante;
    private String infoRentree;
}