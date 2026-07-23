package inpg.sn.lms.auth.entity;

import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "candidats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Candidat extends User {

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false, unique = true)
    private String telephone;

    @Column(nullable = false)
    private String nationalite;

    @Column(nullable = false)
    private LocalDate dateNaissance;

    @Column(nullable = false)
    private String niveauEtude;

    public static Candidat userToCandidat(User user, String nom, String prenom, String telephone, String nationalite,
            LocalDate dateNaissance, String niveauEtude) {
        Candidat candidat = user.convertTo(Candidat.class);
        candidat.setNom(nom);
        candidat.setPrenom(prenom);
        candidat.setTelephone(telephone);
        candidat.setNationalite(nationalite);
        candidat.setDateNaissance(dateNaissance);
        candidat.setNiveauEtude(niveauEtude);
        return candidat;
    }

}