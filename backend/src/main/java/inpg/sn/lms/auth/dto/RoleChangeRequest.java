package inpg.sn.lms.auth.dto;

import java.util.List;

public class RoleChangeRequest {
    private List<String> roles;

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}
