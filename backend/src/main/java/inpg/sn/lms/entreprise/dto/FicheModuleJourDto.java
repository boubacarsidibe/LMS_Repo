package inpg.sn.lms.entreprise.dto;

/** Une journée du planning d'une Fiche Technique de Module (section ④). */
public record FicheModuleJourDto(
        Long id,
        int numero,
        String contenus,
        Integer dureeHeures,
        String typeEnseignement,
        String modalites) {
}
