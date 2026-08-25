package inpg.sn.lms.recrutement.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "fichiers_stockes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FichierStocke {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nomOriginal;

    @Column(nullable = false)
    private String cheminStockage;

    @Column(nullable = false)
    private String mimeType;

    @Column(nullable = false)
    private Long tailleOctets;

    @Column(nullable = false)
    private LocalDateTime dateUpload;
}