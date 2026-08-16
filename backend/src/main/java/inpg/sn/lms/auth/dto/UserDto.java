package inpg.sn.lms.auth.dto;

public class UserDto {
    private String username;
    private String password;
    private String email;
    private String nom;
    private String prenom;
    private String telephone;

    public UserDto() {
    }

    public UserDto(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public UserDto(String username, String password, String email, String nom, String prenom, String telephone) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.nom = nom;
        this.prenom = prenom;
        this.telephone = telephone;
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

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public inpg.sn.lms.auth.entity.User toUser() {
        inpg.sn.lms.auth.entity.User user = new inpg.sn.lms.auth.entity.User();
        user.setUsername(this.username);
        user.setPassword(this.password);
        user.setEmail(this.email);
        return user;
    }
}
