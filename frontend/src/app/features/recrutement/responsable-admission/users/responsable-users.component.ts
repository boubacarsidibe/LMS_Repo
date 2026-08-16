import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../../../core/auth/services/admin.service';
import { User } from '../../../../core/auth/models/user.model';

@Component({
  selector: 'app-responsable-users',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './responsable-users.component.html',
})
export class ResponsableUsersComponent implements OnInit {
  users: User[] = [];
  loading = false;

  constructor(private admin: AdminService) {}

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
      error: () => { this.loading = false; alert('Impossible de charger les utilisateurs'); }
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
      error: () => { alert('Erreur lors de la mise à jour des rôles'); this.loadUsers(); }
    });
  }
}
