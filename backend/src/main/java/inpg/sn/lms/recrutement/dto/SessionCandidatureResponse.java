package inpg.sn.lms.recrutement.dto;

import java.time.LocalDate;

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
}