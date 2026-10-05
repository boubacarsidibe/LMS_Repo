package inpg.sn.lms.recrutement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.enums.EnumStatutCandidature;
import jakarta.persistence.LockModeType;

@Repository
public interface CandidatureRepository extends JpaRepository<Candidature, Long> {

    /**
     * Associations chargées d'office avec chaque dossier, parce que la DTO les lit
     * toutes ({@code CandidatureService#toResponse}) : sans elles, lister N dossiers
     * déclenchait 4×N requêtes supplémentaires.
     *
     * <p>Seules les associations « vers-un » figurent ici. Ajouter les deux
     * collections ({@code piecesJustificatives}, {@code reponsesCriteres}) ferait
     * échouer Hibernate au démarrage (MultipleBagFetchException, deux List dans le
     * même graphe) ; elles sont couvertes par {@code hibernate.default_batch_fetch_size},
     * réglé dans application.properties.
     */
    @Override
    @EntityGraph(attributePaths = { "candidat", "candidat.user", "sessionCandidature", "paiement" })
    List<Candidature> findAll();

    Optional<Candidature> findByNumeroDossier(String numeroDossier);

    /** Sérialise la création ou la relance d'un règlement pour un même dossier. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Candidature c WHERE c.id = :id")
    Optional<Candidature> findByIdForPayment(@Param("id") Long id);

    boolean existsByNumeroDossier(String numeroDossier);

    List<Candidature> findByCandidatId(Long candidatId);

    List<Candidature> findByStatut(EnumStatutCandidature statut);

    List<Candidature> findBySessionCandidatureId(Long sessionId);

    /**
     * Nombre de dossiers déposés sur une session — le « remplissage » affiché au
     * back-office. Il était auparavant stocké dans {@code sessions_candidature.quota_actuel},
     * colonne que rien n'incrémentait jamais : le taux affiché ne bougeait pas d'un pouce,
     * quel que soit le nombre réel de candidatures. Compter à la demande coûte une requête
     * et ne peut pas se désynchroniser.
     */
    long countBySessionCandidatureId(Long sessionId);

    List<Candidature> findByCandidatIdAndStatut(Long candidatId, EnumStatutCandidature statut);

    /** Liste back-office bornée : évite de charger tous les dossiers à chaque écran. */
    @EntityGraph(attributePaths = { "candidat", "candidat.user", "sessionCandidature", "paiement" })
    Page<Candidature> findAllBy(Pageable pageable);

    List<Candidature> findTop5ByOrderByDateSoumissionDesc();

    @Query("SELECT c FROM Candidature c LEFT JOIN c.candidat ca LEFT JOIN ca.user u " +
           "WHERE LOWER(c.numeroDossier) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(ca.nom) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(ca.prenom) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Candidature> searchByQuery(@Param("q") String q);
}
