package inpg.sn.lms.auth.dto;

import java.time.LocalDate;

import inpg.sn.lms.auth.entity.Candidat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidatDto {
    private String username;
    private String password;
    private String email;
    private String nom;
    private String prenom;
    private String telephone;
    private String nationalite;
    private LocalDate dateNaissance;
    private String niveauEtude;

    static public CandidatDto toCandidatDto(Candidat candidat) {
        CandidatDto candidatDto = new CandidatDto();
        candidatDto.setUsername(candidat.getUsername());
        candidatDto.setPassword(candidat.getPassword());
        candidatDto.setEmail(candidat.getEmail());
        candidatDto.setNom(candidat.getNom());
        candidatDto.setPrenom(candidat.getPrenom());
        candidatDto.setTelephone(candidat.getTelephone());
        candidatDto.setNationalite(candidat.getNationalite());
        candidatDto.setDateNaissance(candidat.getDateNaissance());
        candidatDto.setNiveauEtude(candidat.getNiveauEtude());

        return candidatDto;
    }

    public Candidat toCandidat() {
        Candidat candidat = new Candidat();
        candidat.setUsername(this.username);
        candidat.setPassword(this.password);
        candidat.setEmail(this.email);
        candidat.setNom(this.nom);
        candidat.setPrenom(this.prenom);
        candidat.setTelephone(this.telephone);
        candidat.setNationalite(this.nationalite);
        candidat.setDateNaissance(this.dateNaissance);
        candidat.setNiveauEtude(this.niveauEtude);
        return candidat;
    }

    
}
