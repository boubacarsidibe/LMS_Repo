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
@Table(name = "tests_academiques")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestAcademique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nomEpreuve;

    @Column(nullable = false)
    private Float coefficient;

    @Column
    private Float noteObtenue;

    @Column
    private LocalDateTime dateEpreuve;

    @ManyToOne
    @JoinColumn(name = "candidat_id", nullable = false)
    private Candidat candidat;
}