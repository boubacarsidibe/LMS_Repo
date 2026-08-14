package inpg.sn.lms.espace_etudiant.dto;

import java.util.List;

public record AnnonceCreateRequest(String titre, String contenu, boolean epinglee, List<Long> cohorteIds) {

}
