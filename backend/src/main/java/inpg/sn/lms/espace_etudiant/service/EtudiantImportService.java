package inpg.sn.lms.espace_etudiant.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import inpg.sn.lms.auth.dto.EtudiantDto;
import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.auth.service.EtudiantService;
import inpg.sn.lms.espace_etudiant.entity.Promotion;

/**
 * Création d'un étudiant pour l'import CSV en masse, isolée dans sa propre
 * transaction.
 *
 * <p>PostgreSQL abandonne toute la transaction en cours dès la première erreur SQL
 * (contrainte violée, par exemple), même si le code Java l'a rattrapée avec un
 * try/catch : toute requête suivante sur la même transaction échoue à son tour avec
 * « current transaction is aborted », et la validation finale de Spring — trouvant
 * la transaction déjà marquée pour retour arrière — fait échouer la requête entière
 * en 500, alors que le contrôleur avait construit une réponse 200 normale listant
 * les rejets. Faire tourner chaque ligne dans sa propre transaction ({@code
 * REQUIRES_NEW}) l'isole des lignes voisines : une ligne en échec ne peut plus faire
 * échouer, ni fausser le compte-rendu, les lignes qui la suivent dans le fichier.
 */
@Service
public class EtudiantImportService {

    private final EtudiantService etudiantService;
    private final MatriculeService matriculeService;
    private final EtudiantRepository etudiantRepository;

    public EtudiantImportService(EtudiantService etudiantService, MatriculeService matriculeService,
            EtudiantRepository etudiantRepository) {
        this.etudiantService = etudiantService;
        this.matriculeService = matriculeService;
        this.etudiantRepository = etudiantRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Etudiant creerPourImport(EtudiantDto dto, Promotion promotion) {
        Etudiant etudiant = etudiantService.createEtudiant(dto);
        if (promotion != null) {
            etudiant.setPromotion(promotion);
        }
        matriculeService.attribuerSiAbsent(etudiant);
        return etudiantRepository.save(etudiant);
    }
}
