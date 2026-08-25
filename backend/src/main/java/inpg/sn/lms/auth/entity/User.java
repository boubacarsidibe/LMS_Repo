package inpg.sn.lms.auth.entity;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import inpg.sn.lms.auth.enums.EnumTypeInscription;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "dtype", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    // WRITE_ONLY : le hash bcrypt ne doit jamais partir dans une réponse JSON
    // (GET /api/admin/users sérialise l'entité User telle quelle).
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    // Intention declaree a l'inscription (candidat / ecole) - voir EnumTypeInscription.
    // N'attribue aucun role : ROLE_CANDIDAT/ROLE_ECOLE_FORMATION restent accordes
    // uniquement a la soumission reelle.
    @Enumerated(EnumType.STRING)
    @Column(name = "type_inscription", nullable = false, length = 20)
    private EnumTypeInscription typeInscription = EnumTypeInscription.CANDIDAT;

    // @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    // private Candidat candidat;
    // @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    // private Etudiant etudiant;
    // @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    // private Formateur formateur;


    @ManyToMany(fetch = FetchType.EAGER, cascade = { CascadeType.MERGE })
    @JoinTable(
        name = "user_roles", 
        joinColumns = @JoinColumn(name = "user_id"), 
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "user_favorites",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "formation_id")
    )
    @JsonIgnore
    private Set<inpg.sn.lms.espace_etudiant.entity.Formation> favorites = new HashSet<>();

    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public Set<inpg.sn.lms.espace_etudiant.entity.Formation> getFavorites() {
        return favorites;
    }

    public void setFavorites(Set<inpg.sn.lms.espace_etudiant.entity.Formation> favorites) {
        this.favorites = favorites;
    }

    


    
    




}