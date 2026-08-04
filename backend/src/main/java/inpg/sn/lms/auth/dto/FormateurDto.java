package inpg.sn.lms.auth.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormateurDto {
    private String username;
    private String password;
    private String email;
    private String nom;
    private String prenom;
    private String telephone;
    private String nationalite;
    private LocalDate dateNaissance;
    private String niveauEtude;   
}
