package inpg.sn.lms.auth.dto;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.entity.Formateur;

public class FormateurDto {
    private String username;
    private String password;
    private String email;

    public FormateurDto() {
    }

    public FormateurDto(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Formateur toFormateur() {
        Formateur formateur = new Formateur();
        formateur.setUsername(this.username);
        formateur.setPassword(this.password);
        formateur.setEmail(this.email);
        

        return formateur;
    }
}
