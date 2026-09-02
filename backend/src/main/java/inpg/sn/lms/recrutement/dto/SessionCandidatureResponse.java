package inpg.sn.lms.recrutement.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionCandidatureResponse {

    private Long id;
    private String libelle;
    private LocalDate dateOuverture;
    private LocalDate dateFermeture;
    private boolean estOuverte;
    private boolean estActive;
    private boolean criteresActifs;
    private String criteresNiveauEtudeMinimum;
    private Integer criteresAgeMinimum;
    private Integer criteresAgeMaximum;
    private String criteresGenre;
    private Integer criteresExperienceMinimale;
    private float seuilReussiteTest;
    private int nbCandidatures;
    private String infoFraisScolarite;
    private String infoCarteEtudiante;
    private String infoRentree;
    private List<CritereAdmissionResponse> criteresPersonnalises;
}