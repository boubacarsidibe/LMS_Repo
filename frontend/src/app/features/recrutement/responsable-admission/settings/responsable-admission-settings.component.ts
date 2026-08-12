import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-responsable-admission-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './responsable-admission-settings.component.html',
})
export class ResponsableAdmissionSettingsComponent {
  activeTab: 'general' | 'rules' | 'users' | 'templates' = 'general';
  modalAddUserOpen = false;
  toastMessage = '';
  toastVisible = false;

  // General
  institutName = "INPG Dakar - Institut National du Pétrole et du Gaz";
  contactEmail = "admissions@inpg.sn";
  candidaturesSpontanees = true;
  paiementEnLigne = true;

  // Rules
  seuilTests = 60;
  seuilEntretien = 70;

  // Users
  users = [
    { initials: 'DN', nom: 'Dr. Ndiaye', email: 'a.ndiaye@inpg.sn', role: 'Responsable Admissions', actif: true },
    { initials: 'CS', nom: 'Dr. Cheikh Samba', email: 'c.samba@inpg.sn', role: 'Super Admin', actif: true },
  ];

  switchTab(tab: 'general' | 'rules' | 'users' | 'templates'): void {
    this.activeTab = tab;
  }

  saveSettings(message: string): void {
    this.toastMessage = message;
    this.toastVisible = true;
    setTimeout(() => this.toastVisible = false, 3000);
  }

  openAddUserModal(): void { this.modalAddUserOpen = true; }
  closeAddUserModal(): void { this.modalAddUserOpen = false; }

  createUser(): void {
    this.modalAddUserOpen = false;
    this.saveSettings('Utilisateur ajouté avec succès.');
  }
}