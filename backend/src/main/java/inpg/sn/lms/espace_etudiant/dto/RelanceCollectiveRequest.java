package inpg.sn.lms.espace_etudiant.dto;

import java.util.List;

public record RelanceCollectiveRequest(List<Long> etudiantIds, String objet, String message) {
}
