package inpg.sn.lms.espace_etudiant.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import inpg.sn.lms.espace_etudiant.entity.ReleveNotes;

public interface ReleveNotesRepository extends JpaRepository<ReleveNotes, Long> {

    /** Les lignes sont chargées d'emblée : sans elles un relevé n'a rien à montrer. */
    @EntityGraph(attributePaths = { "lignes", "etudiant", "promotion" })
    List<ReleveNotes> findByEtudiant_IdOrderByDateEmissionDesc(Long etudiantId);

    @EntityGraph(attributePaths = { "lignes", "etudiant", "promotion" })
    List<ReleveNotes> findAllByOrderByDateEmissionDesc();

    /**
     * Un relevé existe-t-il déjà pour cet étudiant et cette période ?
     *
     * <p>Sert à avertir, jamais à interdire : un duplicata ou un relevé corrigé se
     * réémet légitimement, et on ne supprime pas un document délivré. Mais un double
     * clic produit exactement le même résultat qu'une réémission voulue, sans rien
     * pour les distinguer — d'où l'avertissement.
     */
    boolean existsByEtudiant_IdAndPeriode(Long etudiantId, String periode);
}
