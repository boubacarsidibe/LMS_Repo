package inpg.sn.lms.espace_etudiant.entity;

import java.util.List;

import inpg.sn.lms.auth.entity.Formateur;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

@Entity
public class Cours {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Long volumeHoraire;

    @ManyToMany
    @JoinTable(
        name = "cours_formateur", // Name of the join table in DB
        joinColumns = @JoinColumn(name = "cours_id"),
        inverseJoinColumns = @JoinColumn(name = "formateur_id")
    )
    private List<Formateur> formateur; // Consider renaming to 'formateurs' for clarity




}
