package inpg.sn.lms.espace_etudiant.dto;

import java.util.List;

public record AnnonceDto(Long id, String titre, String contenu, String auteur, String date, boolean epinglee,
        List<CohorteDto> cohortes) {

}
