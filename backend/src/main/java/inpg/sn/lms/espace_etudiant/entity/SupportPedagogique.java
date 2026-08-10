package inpg.sn.lms.espace_etudiant.entity;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.annotation.Generated;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
@Entity
public class SupportPedagogique {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;
    private String titre;
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
