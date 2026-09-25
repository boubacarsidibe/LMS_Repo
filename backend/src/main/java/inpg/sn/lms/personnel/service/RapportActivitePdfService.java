package inpg.sn.lms.personnel.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import inpg.sn.lms.pdf.RapportPdf;
import inpg.sn.lms.personnel.entity.ActivitePersonnel;
import inpg.sn.lms.personnel.enums.EnumTypeActivite;

/**
 * Rapports d'activités du personnel, en PDF (TDR §I : « génération automatique
 * des rapports mensuels par département » et « compilation des rapports pour
 * un rapport global de l'INPG »).
 *
 * <p>Aucun recalcul métier : le service met en page les {@link ActivitePersonnel}
 * déjà filtrées pour la période, en les regroupant par type et en résumant en
 * quelques indicateurs (nombre d'activités, de contributeurs, répartition).
 */
@Service
public class RapportActivitePdfService {

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] MOIS_LIBELLES = {
            "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
            "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
    };

    /** Rapport d'un seul département, pour la période donnée. */
    public byte[] genererRapportDepartement(String libelleDepartement, int mois, int annee,
            List<ActivitePersonnel> activites) {
        String titre = "Rapport d'activités — " + libelleDepartement;
        try (RapportPdf pdf = new RapportPdf(titre)) {
            pdf.titre(titre);
            enTete(pdf, mois, annee);
            pdf.espace(10);
            pdf.section("Chiffres clés");
            indicateurs(pdf, activites);
            pdf.espace(8);
            sectionParType(pdf, activites);
            return pdf.generer();
        } catch (IOException | RuntimeException exception) {
            throw new RuntimeException("La génération du rapport d'activités a échoué.", exception);
        }
    }

    /** Rapport consolidé de tous les départements, pour la période donnée. */
    public byte[] genererRapportGlobal(int mois, int annee, List<ActivitePersonnel> activites) {
        try (RapportPdf pdf = new RapportPdf("Rapport global d'activités — INPG")) {
            pdf.titre("Rapport global d'activités — INPG");
            enTete(pdf, mois, annee);
            pdf.espace(10);

            pdf.section("Synthèse de l'établissement");
            long departements = activites.stream().map(a -> a.getDepartement().getId()).distinct().count();
            pdf.ligneCle("Départements ayant saisi une activité", String.valueOf(departements));
            pdf.ligneCle("Total des activités", String.valueOf(activites.size()));
            pdf.ligneCle("Contributeurs distincts", String.valueOf(nbContributeurs(activites)));
            repartitionParType(pdf, activites);
            pdf.espace(10);

            Map<String, List<ActivitePersonnel>> parDepartement = activites.stream()
                    .collect(Collectors.groupingBy(a -> a.getDepartement().getLibelle(), Collectors.toList()));
            for (String libelle : parDepartement.keySet().stream().sorted().toList()) {
                pdf.section(libelle);
                indicateurs(pdf, parDepartement.get(libelle));
                pdf.espace(4);
                sectionParType(pdf, parDepartement.get(libelle));
                pdf.espace(8);
            }
            return pdf.generer();
        } catch (IOException | RuntimeException exception) {
            throw new RuntimeException("La génération du rapport global a échoué.", exception);
        }
    }

    private void enTete(RapportPdf pdf, int mois, int annee) throws IOException {
        pdf.meta("Institut National du Pétrole et du Gaz");
        pdf.meta("Période : " + MOIS_LIBELLES[mois - 1] + " " + annee
                + " — généré le " + LocalDateTime.now().format(FORMAT_DATE));
    }

    /** Bloc « chiffres clés » d'un lot d'activités. */
    private void indicateurs(RapportPdf pdf, List<ActivitePersonnel> activites) throws IOException {
        pdf.ligneCle("Activités saisies", String.valueOf(activites.size()));
        pdf.ligneCle("Contributeurs distincts", String.valueOf(nbContributeurs(activites)));
        repartitionParType(pdf, activites);
    }

    private long nbContributeurs(List<ActivitePersonnel> activites) {
        return activites.stream()
                .map(a -> a.getAuteur() == null ? null : a.getAuteur().getId())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();
    }

    private void repartitionParType(RapportPdf pdf, List<ActivitePersonnel> activites) throws IOException {
        if (activites.isEmpty()) {
            return;
        }
        Map<EnumTypeActivite, Long> comptes = activites.stream()
                .collect(Collectors.groupingBy(ActivitePersonnel::getTypeActivite, Collectors.counting()));
        String detail = java.util.Arrays.stream(EnumTypeActivite.values())
                .filter(comptes::containsKey)
                .map(t -> libelleType(t.name()) + " : " + comptes.get(t))
                .collect(Collectors.joining("  |  "));
        pdf.ligneCle("Répartition par type", detail);
    }

    /** Les activités regroupées par type, chacune datée avec sa description. */
    private void sectionParType(RapportPdf pdf, List<ActivitePersonnel> activites) throws IOException {
        if (activites.isEmpty()) {
            pdf.texteItalique("Aucune activité saisie pour cette période.");
            return;
        }
        for (EnumTypeActivite type : EnumTypeActivite.values()) {
            List<ActivitePersonnel> duType = activites.stream()
                    .filter(a -> a.getTypeActivite() == type)
                    .sorted((a, b) -> b.getDateCreation().compareTo(a.getDateCreation()))
                    .toList();
            if (duType.isEmpty()) {
                continue;
            }
            pdf.intertitre(libelleType(type.name()) + " (" + duType.size() + ")");
            for (ActivitePersonnel activite : duType) {
                String auteur = activite.getAuteur() == null ? "?" : activite.getAuteur().getUsername();
                pdf.puce(activite.getDateCreation().format(FORMAT_DATE) + " — " + activite.getTitre()
                        + "  (" + auteur + ")");
                if (activite.getDescription() != null && !activite.getDescription().isBlank()) {
                    pdf.detail(activite.getDescription());
                }
                pdf.espace(3);
            }
        }
    }

    private String libelleType(String type) {
        return type.charAt(0) + type.substring(1).toLowerCase(Locale.FRENCH);
    }
}
