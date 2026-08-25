package inpg.sn.lms.espace_etudiant.service;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import inpg.sn.lms.espace_etudiant.dto.NoteModuleDto;
import inpg.sn.lms.espace_etudiant.dto.ReleveNotesDto;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.NoteModule;

/**
 * Construit le relevé de notes d'un étudiant.
 *
 * <p>Ce calcul vivait dans {@code ScolariteController}, où seul l'étudiant connecté
 * pouvait l'atteindre. Le responsable pédagogique, qui délivre les relevés, en a
 * besoin pour n'importe quel étudiant — et le figera au moment de la délivrance. Le
 * laisser dans le contrôleur aurait produit une deuxième, puis une troisième copie de
 * la moyenne pondérée, qui auraient fini par diverger : c'est exactement ce qui était
 * arrivé aux deux chaînes de rattachement des cours.
 */
@Service
public class ReleveNotesService {

    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final NoteModuleService noteModuleService;

    public ReleveNotesService(NoteModuleService noteModuleService) {
        this.noteModuleService = noteModuleService;
    }

    /**
     * Relevé courant d'un étudiant, groupé par cours.
     *
     * <p>Aucun filtre par cours autorisé n'est appliqué : une note appartient à
     * l'étudiant qui l'a obtenue, et la retirer parce que le programme a changé
     * reviendrait à effacer son parcours.
     */
    @Transactional(readOnly = true)
    public ReleveNotesDto duEtudiant(Long etudiantId) {
        List<NoteModule> notes = noteModuleService.getByEtudiantId(etudiantId);

        Map<Long, List<NoteModule>> parCours = notes.stream()
                .filter(note -> note.getCours() != null)
                .collect(Collectors.groupingBy(note -> note.getCours().getId(), LinkedHashMap::new,
                        Collectors.toList()));

        List<ReleveNotesDto.NotesCoursDto> groupes = parCours.values().stream()
                .map(notesDuCours -> {
                    Cours cours = notesDuCours.get(0).getCours();
                    return new ReleveNotesDto.NotesCoursDto(
                            cours.getId(),
                            cours.getNom(),
                            cours.getTitre(),
                            moyenneDuCours(notesDuCours),
                            cours.getCoefficient(),
                            notesDuCours.stream().map(ReleveNotesService::toNoteDto).toList());
                })
                .toList();

        List<NoteModule> orphelines = notes.stream().filter(note -> note.getCours() == null).toList();
        return new ReleveNotesDto(moyenneGenerale(groupes, orphelines), notes.size(), groupes);
    }

    /**
     * Moyenne générale, pondérée par le coefficient de chaque cours.
     *
     * <p>Elle portait auparavant sur la liste à plat de toutes les notes. Le poids
     * d'un cours dépendait donc du <strong>nombre de devoirs</strong> que son formateur
     * avait saisis : un cours évalué trois fois pesait trois fois plus qu'un cours
     * évalué une seule fois. Personne n'avait choisi cette pondération — elle venait
     * d'une habitude de saisie, et faussait le bulletin de plus d'un point.
     *
     * <p>Chaque cours pèse désormais son coefficient, quel que soit le nombre
     * d'évaluations qu'il contient. Les cours sans note sont écartés : les compter
     * pour zéro sanctionnerait un étudiant pour un devoir que son formateur n'a pas
     * encore corrigé.
     *
     * <p>Une note dont le cours a été supprimé n'a plus de coefficient à porter. Elle
     * reste comptée, au poids de 1 : elle a bien été obtenue, et l'écarter ferait
     * afficher « 1 note » sous une moyenne vide.
     */
    public Double moyenneGenerale(List<ReleveNotesDto.NotesCoursDto> groupes,
            List<NoteModule> orphelines) {
        double poids = 0;
        double total = 0;

        for (ReleveNotesDto.NotesCoursDto groupe : groupes) {
            if (groupe.moyenne() == null || groupe.coefficient() <= 0) {
                continue;
            }
            total += groupe.moyenne() * groupe.coefficient();
            poids += groupe.coefficient();
        }
        for (NoteModule orpheline : orphelines) {
            total += orpheline.getNote();
            poids += 1;
        }

        return poids <= 0 ? null : arrondi(total / poids);
    }

    /**
     * Moyenne générale d'un étudiant, pour les écrans de suivi.
     *
     * <p>Les listes d'étudiants du formateur et du responsable pédagogique affichaient
     * chacune leur propre chiffre : le formateur une moyenne à plat, le responsable
     * pédagogique un <strong>zéro codé en dur</strong>. Ni l'un ni l'autre ne
     * correspondait au relevé que l'étudiant, lui, voyait — et c'est pourtant sur ces
     * écrans qu'on décide de délivrer un relevé ou de signaler un étudiant en
     * difficulté. Une seule définition, ici, pour les trois rôles.
     *
     * @return {@code null} si aucune note n'a été saisie — à distinguer d'un zéro
     */
    @Transactional(readOnly = true)
    public Double moyenneDeLEtudiant(Long etudiantId) {
        return duEtudiant(etudiantId).moyenneGenerale();
    }

    /**
     * Statut de suivi déduit de la seule moyenne.
     *
     * <p>Faute d'un suivi d'activité réel (dernière connexion, avancement), le statut
     * se limite à ce que les notes permettent d'affirmer. Un étudiant sans aucune note
     * est « à surveiller » : rien ne permet encore de dire qu'il va bien.
     */
    public String statutDeSuivi(Double moyenne) {
        if (moyenne == null) {
            return "À surveiller";
        }
        return moyenne >= 12 ? "Actif" : "À risque";
    }

    /** Moyenne d'un cours, {@code null} si la liste est vide. */
    public Double moyenneDuCours(List<NoteModule> notes) {
        return notes.isEmpty() ? null
                : arrondi(notes.stream().mapToDouble(NoteModule::getNote).average().orElse(0));
    }

    /** Au dixième : une moyenne affichée avec plus de décimales n'est pas vérifiable à la main. */
    private static double arrondi(double valeur) {
        return Math.round(valeur * 10) / 10.0;
    }

    public static NoteModuleDto toNoteDto(NoteModule note) {
        Cours cours = note.getCours();
        return new NoteModuleDto(
                note.getId(),
                note.getModule(),
                note.getNote(),
                note.getCommentaire(),
                note.getDateEvaluation() == null ? null : note.getDateEvaluation().format(JOUR),
                cours == null ? null : cours.getId(),
                cours == null ? null : cours.getNom(),
                cours == null ? null : cours.getTitre(),
                note.getFormateurNom());
    }
}
