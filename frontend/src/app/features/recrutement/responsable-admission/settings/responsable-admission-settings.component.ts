import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

interface EquipeUser {
  initials: string;
  nom: string;
  email: string;
  role: string;
  actif: boolean;
}

interface EmailTemplate {
  cle: 'confirmation' | 'convocation';
  label: string;
  objet: string;
  corps: string;
}

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
  users: EquipeUser[] = [
    { initials: 'DN', nom: 'Dr. Ndiaye', email: 'a.ndiaye@inpg.sn', role: 'Responsable Admissions', actif: true },
    { initials: 'CS', nom: 'Dr. Cheikh Samba', email: 'c.samba@inpg.sn', role: 'Super Admin', actif: true },
  ];
  /** Index de l'utilisateur en cours d'édition dans `users`, ou -1 pour une création. */
  private editIndex = -1;
  formPrenom = '';
  formNom = '';
  formEmail = '';
  formRole = 'Responsable Admissions';

  // Templates — édités en mémoire, comme le reste de cette page qui ne persiste rien en base.
  templates: EmailTemplate[] = [
    {
      cle: 'confirmation',
      label: 'Confirmation Inscription',
      objet: "Confirmation de réception de votre dossier - INPG",
      corps: `Bonjour {NOM_CANDIDAT},

Nous vous confirmons la bonne réception de votre dossier pour la session {SESSION}.
Votre numéro de dossier est le : {NUMERO_DOSSIER}.

Vous serez informé(e) des prochaines étapes par email.

Cordialement,
L'équipe Admissions INPG`,
    },
    {
      cle: 'convocation',
      label: 'Convocation Test',
      objet: "Convocation au test académique - INPG",
      corps: `Bonjour {NOM_CANDIDAT},

Vous êtes convoqué(e) au test académique de la session {SESSION}.
Merci de vous présenter muni(e) d'une pièce d'identité.

Cordialement,
L'équipe Admissions INPG`,
    },
  ];
  selectedTemplateCle: EmailTemplate['cle'] = 'confirmation';

  get selectedTemplate(): EmailTemplate {
    return this.templates.find((t) => t.cle === this.selectedTemplateCle) ?? this.templates[0];
  }

  selectTemplate(cle: EmailTemplate['cle']): void {
    this.selectedTemplateCle = cle;
  }

  switchTab(tab: 'general' | 'rules' | 'users' | 'templates'): void {
    this.activeTab = tab;
  }

  saveSettings(message: string): void {
    this.toastMessage = message;
    this.toastVisible = true;
    setTimeout(() => this.toastVisible = false, 3000);
  }

  openAddUserModal(): void {
    this.editIndex = -1;
    this.formPrenom = '';
    this.formNom = '';
    this.formEmail = '';
    this.formRole = 'Responsable Admissions';
    this.modalAddUserOpen = true;
  }

  openEditUserModal(index: number): void {
    const user = this.users[index];
    this.editIndex = index;
    const [prenom, ...reste] = user.nom.replace(/^Dr\.\s*/, '').split(' ');
    this.formPrenom = prenom ?? '';
    this.formNom = reste.join(' ');
    this.formEmail = user.email;
    this.formRole = user.role;
    this.modalAddUserOpen = true;
  }

  closeAddUserModal(): void { this.modalAddUserOpen = false; }

  get modalTitle(): string {
    return this.editIndex >= 0 ? 'Modifier le membre de l\'équipe' : '+ Ajouter un membre de l\'équipe';
  }

  createUser(): void {
    const nomComplet = `${this.formPrenom} ${this.formNom}`.trim();
    const initials = `${this.formPrenom.charAt(0)}${this.formNom.charAt(0)}`.toUpperCase();

    if (this.editIndex >= 0) {
      const user = this.users[this.editIndex];
      user.nom = nomComplet || user.nom;
      user.email = this.formEmail || user.email;
      user.role = this.formRole;
      user.initials = initials || user.initials;
      this.modalAddUserOpen = false;
      this.saveSettings('Utilisateur modifié avec succès.');
      return;
    }

    this.users.push({
      initials: initials || '??',
      nom: nomComplet || 'Nouvel utilisateur',
      email: this.formEmail,
      role: this.formRole,
      actif: true,
    });
    this.modalAddUserOpen = false;
    this.saveSettings('Utilisateur ajouté avec succès.');
  }
}