import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../../../core/auth/services/admin.service';
import { User } from '../../../../core/auth/models/user.model';
import { NotificationService } from '../../../../shared/components/ui/notification.service';

@Component({
  selector: 'app-responsable-users',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './responsable-users.component.html',
})
export class ResponsableUsersComponent implements OnInit {
  users: User[] = [];
  loading = false;

  /** Même liste que RoleManagementService.ROLES_ATTRIBUABLES (serveur) : le serveur refuse tout autre rôle. */
  readonly rolesAttribuables = [
    { code: 'ROLE_CANDIDAT', libelle: 'Candidat' },
    { code: 'ROLE_ETUDIANT', libelle: 'Étudiant' },
    { code: 'ROLE_FORMATEUR', libelle: 'Formateur' },
  ];
  private readonly rolesPersonnel = ['ROLE_RESPONSABLE_ADMISSION', 'ROLE_RESPONSABLE_PEDAGOGIQUE', 'ROLE_EVALUATEUR_ACCREDITATION'];
  private readonly libelles: Record<string, string> = {
    ROLE_RESPONSABLE_ADMISSION: 'Responsable admission', ROLE_RESPONSABLE_PEDAGOGIQUE: 'Responsable pédagogique',
    ROLE_EVALUATEUR_ACCREDITATION: 'Évaluateur accréditation', ROLE_ALUMNI: 'Alumni', ROLE_ECOLE_FORMATION: 'École de formation',
    ROLE_ENTREPRISE: 'Entreprise', ROLE_CANDIDAT_FORMATEUR: 'Candidat formateur',
    ROLE_CANDIDAT: 'Candidat', ROLE_ETUDIANT: 'Étudiant', ROLE_FORMATEUR: 'Formateur',
  };

  estPersonnel(user: User): boolean {
    return (user.roles || []).some((r) => this.rolesPersonnel.includes(r as string));
  }

  estAttribuable(code: string): boolean {
    return this.rolesAttribuables.some((r) => r.code === code);
  }

  libelleRole(code: string): string {
    return code ? this.libelles[code] ?? code : 'Aucun rôle';
  }

  constructor(private admin: AdminService, private notifications: NotificationService) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.admin.getUsers().subscribe({
      next: (u) => {
        // Normalize roles: backend may return Role objects or strings.
        this.users = u.map(x => ({
          ...x,
          roles: (x.roles || []).map((r: any) => typeof r === 'string' ? r : r.code)
        }));
        this.loading = false;
      },
      error: () => { this.loading = false; this.notifications.erreur('Impossible de charger les utilisateurs'); }
    });
  }

  /** Un compte ne peut détenir qu'un seul rôle à la fois : le sélecteur est un choix unique. */
  currentRole(user: User): string {
    return (user.roles && user.roles[0]) || '';
  }

  onRoleChange(user: User, newRoleCode: string) {
    const roles = newRoleCode ? [newRoleCode] : [];
    this.admin.changeRoles(user.id, roles).subscribe({
      next: (updated: any) => { user.roles = (updated.roles || []).map((r: any) => typeof r === 'string' ? r : r.code); },
      error: (err) => { this.notifications.erreur(err?.error?.message || 'Erreur lors de la mise à jour des rôles'); this.loadUsers(); }
    });
  }
}
