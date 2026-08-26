package inpg.sn.lms.espace_alumni.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Campagne d'enquête de suivi (TDR §G) : « mesurer l'impact à long terme de la
 * formation sur la carrière des diplômés ». Lancée par la pédagogie, une ligne
 * par campagne (pas par alumni) — chaque alumni y répond au plus une fois
 * ({@link ReponseEnqueteSuivi}, contrainte unique).
 *
 * <p>Distincte de {@link SituationProfessionnelle} : celle-ci est un historique
 * de poste librement tenu par l'alumni, jamais une question posée par l'INPG.
 */
@Entity
@Table(name = "enquetes_suivi")
@Getter
@Setter
@NoArgsConstructor
public class EnqueteSuivi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titre;

    @Column(length = 1000)
    private String description;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(nullable = false)
    private boolean active = true;

    public EnqueteSuivi(String titre, String description) {
        this.titre = titre;
        this.description = description;
        this.dateCreation = LocalDateTime.now();
        this.active = true;
    }
}
