package inpg.sn.lms.espace_etudiant.dto;

import java.util.List;

public record PresenceBulkUpdateRequest(List<Entree> presences) {

    public record Entree(Long etudiantId, boolean present) {
    }
}
