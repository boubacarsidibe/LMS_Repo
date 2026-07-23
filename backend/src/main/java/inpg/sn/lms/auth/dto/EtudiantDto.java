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

    static public EtudiantDto toEtudiantDto(Etudiant etudiant) {
        EtudiantDto etudiantDto = new EtudiantDto();
        etudiantDto.setUsername(etudiant.getUsername());
        etudiantDto.setPassword(etudiant.getPassword());
        etudiantDto.setEmail(etudiant.getEmail());
        etudiantDto.setNom(etudiant.getNom());
        etudiantDto.setPrenom(etudiant.getPrenom());
        etudiantDto.setTelephone(etudiant.getTelephone());
        

        return etudiantDto;
    }

    public Etudiant toEtudiant() {
        Etudiant etudiant = new Etudiant();
        etudiant.setUsername(this.username);
        etudiant.setPassword(this.password);
        etudiant.setEmail(this.email);
        etudiant.setNom(this.nom);
        etudiant.setPrenom(this.prenom);
        etudiant.setTelephone(this.telephone);
       
        return etudiant;
    }

    
}
