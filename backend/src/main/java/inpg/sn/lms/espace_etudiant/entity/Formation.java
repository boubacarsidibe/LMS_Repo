package inpg.sn.lms.espace_etudiant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class Formation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String intitule;

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getIntitule() {
        return intitule;
    }

    @ManyToMany(mappedBy = "favorites")
    @JsonIgnore
    private java.util.Set<inpg.sn.lms.auth.entity.User> favoritedBy = new java.util.HashSet<>();

    public java.util.Set<inpg.sn.lms.auth.entity.User> getFavoritedBy() {
        return favoritedBy;
    }

    public void setFavoritedBy(java.util.Set<inpg.sn.lms.auth.entity.User> favoritedBy) {
        this.favoritedBy = favoritedBy;
    }

}
