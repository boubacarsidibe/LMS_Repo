package inpg.sn.lms.auth.dto;

import java.time.LocalDate;

import inpg.sn.lms.auth.entity.Etudiant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class EtudiantDto {
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