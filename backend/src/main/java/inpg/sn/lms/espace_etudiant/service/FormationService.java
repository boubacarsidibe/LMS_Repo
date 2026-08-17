package inpg.sn.lms.espace_etudiant.service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.Formation;
import inpg.sn.lms.espace_etudiant.entity.Promotion;
import inpg.sn.lms.espace_etudiant.repository.FormationRepository;

@Service
public class FormationService {

    private final FormationRepository formationRepository;

    public FormationService(FormationRepository formationRepository) {
        this.formationRepository = formationRepository;
    }

    public List<Formation> getAll() {
        return formationRepository.findAll();
    }

    public Optional<Formation> findById(Long id) {
        return formationRepository.findById(id);
    }

    /**
     * Inscrit un cours au programme des formations suivies par ces promotions, et
     * l'en retire partout ailleurs.
     *
     * <p>Le formateur continue de désigner des promotions — c'est ce que son écran
     * affiche — mais le rattachement se fait désormais au niveau de la formation.
     * Conséquence voulue : <strong>toutes</strong> les promotions de cette formation
     * accèdent au cours, y compris celles des rentrées suivantes, sans qu'il faille
     * refaire le geste chaque année.
     *
     * <p>L'écriture passe par {@link Formation} : c'est le côté propriétaire de la
     * table {@code formation_cours}, {@code Cours.formations} n'en est que la vue
     * inverse et ne persiste rien.
     *
     * @param cours cours déjà enregistré — son identifiant doit être connu
     */
    @Transactional
    public void rattacherAuxFormationsDe(Cours cours, Collection<Promotion> promotions) {
        if (cours == null || cours.getId() == null) {
            return;
        }
        Set<Long> formationsVoulues = promotions.stream()
                .map(Promotion::getFormation)
                .filter(Objects::nonNull)
                .map(Formation::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Formation formation : formationRepository.findAll()) {
            boolean doitContenir = formationsVoulues.contains(formation.getId());
            boolean contientDeja = formation.getCours().stream()
                    .anyMatch(c -> cours.getId().equals(c.getId()));
            if (doitContenir == contientDeja) {
                continue;
            }
            if (doitContenir) {
                formation.getCours().add(cours);
                // Les deux cotes sont tenus a jour : Cours.formations est la vue inverse,
                // et c'est elle que lisent les DTO et le calcul des promotions. Sans cela,
                // la reponse renvoyee juste apres l'ecriture serait vide.
                cours.getFormations().add(formation);
            } else {
                formation.getCours().removeIf(c -> cours.getId().equals(c.getId()));
                cours.getFormations().removeIf(f -> formation.getId().equals(f.getId()));
            }
            formationRepository.save(formation);
        }
    }

    /**
     * Retire le cours de tous les programmes.
     *
     * <p>A appeler avant de supprimer un cours : la table {@code formation_cours} le
     * reference, et la suppression echouait sur une violation de cle etrangere.
     */
    @Transactional
    public void detacherDeToutProgramme(Cours cours) {
        rattacherAuxFormationsDe(cours, List.of());
    }
}
