package inpg.sn.lms.espace_etudiant.entity;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import org.hibernate.annotations.UuidGenerator;

@Entity
public class SupportPedagogique {
    @Id
    @UuidGenerator
    private UUID id;
    private String titre;
    // Sans EnumType.STRING, JPA stocke l'ordinal : réordonner les constantes
    // corromprait silencieusement les lignes déjà enregistrées.
    @Enumerated(EnumType.STRING)
    private EnumTypeSupport typeSupport;
    private LocalDate dateMiseEnLigne;

    public UUID getId() {
        return id;
    }

    public String getTitre() {
        return titre;
    }

    public EnumTypeSupport getTypeSupport() {
        return typeSupport;
    }

    public LocalDate getDateMiseEnLigne() {
        return dateMiseEnLigne;
    }
}

enum EnumTypeSupport {
    PDF,
    VIDEO,
    AUDIO,
    AUTRE
}
