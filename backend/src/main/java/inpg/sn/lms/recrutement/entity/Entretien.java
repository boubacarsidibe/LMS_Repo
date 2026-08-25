package inpg.sn.lms.recrutement.entity;

import java.time.LocalDateTime;

import inpg.sn.lms.auth.entity.Candidat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "entretiens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Entretien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime dateHeure;

    @Column(nullable = false)
    private String lieuOuLien;

    @Column
    private Float noteEntretien;

    @Column(length = 2000)
    private String appreciationGlobale;

    @Column(columnDefinition = "boolean default false")
    private boolean confirmeParCandidat = false;

    @Column(length = 1000)
    private String motifReport;

    @ManyToOne
    @JoinColumn(name = "candidat_id", nullable = false)
    private Candidat candidat;
}