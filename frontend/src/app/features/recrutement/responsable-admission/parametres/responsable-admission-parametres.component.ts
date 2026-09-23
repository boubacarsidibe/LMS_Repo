import { passwordPolicy } from '../../../../core/auth/validators/password-policy';
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Auth } from '../../../../core/auth/services/authService';
import { DashboardPreferences, DashboardPreferencesService } from '../services/dashboard-preferences.service';

@Component({
  selector: 'app-responsable-admission-parametres',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './responsable-admission-parametres.component.html',
})
export class ResponsableAdmissionParametresComponent implements OnInit {
  chargementProfil = true;
  username = '';
  email = '';
  roleLabel = 'Responsable Admission';

  passwordForm: FormGroup;
  isSubmitting = false;
  errorMessage = '';
  successMessage = '';

  dashboardPrefs: DashboardPreferences;
  dashboardPrefsSaved = false;

  constructor(
    private auth: Auth,
    private fb: FormBuilder,
    private dashboardPrefsService: DashboardPreferencesService
  ) {
    this.passwordForm = this.fb.group({
      currentPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, passwordPolicy]],
      confirmPassword: ['', Validators.required],
    });
    this.dashboardPrefs = this.dashboardPrefsService.charger();
  }

  enregistrerDashboardPrefs(): void {
    this.dashboardPrefsService.enregistrer(this.dashboardPrefs);
    this.dashboardPrefsSaved = true;
    setTimeout(() => (this.dashboardPrefsSaved = false), 2500);
  }

  ngOnInit(): void {
    this.auth.getCurrentUser().subscribe({
      next: (user) => {
        this.username = user?.username || '';
        this.email = user?.email || '';
        this.chargementProfil = false;
      },
      error: () => {
        this.chargementProfil = false;
      },
    });
  }

  get passwordsMismatch(): boolean {
    const { newPassword, confirmPassword } = this.passwordForm.getRawValue();
    return !!confirmPassword && newPassword !== confirmPassword;
  }

  onSubmit(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (this.passwordForm.invalid || this.passwordsMismatch) {
      this.passwordForm.markAllAsTouched();
      if (this.passwordForm.get('newPassword')?.hasError('passwordPolicy')) {
        this.errorMessage = 'Le mot de passe doit contenir au moins 12 caractères et ne pas dépasser 72 octets UTF-8.';
      }
      if (this.passwordsMismatch) {
        this.errorMessage = 'Les mots de passe ne correspondent pas.';
      }
      return;
    }

    const { currentPassword, newPassword } = this.passwordForm.getRawValue();
    this.isSubmitting = true;
    this.auth.ChangePassword(currentPassword, newPassword).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.successMessage = 'Mot de passe modifié avec succès.';
        this.passwordForm.reset();
      },
      error: (err) => {
        this.isSubmitting = false;
        this.errorMessage = err.error?.message || 'Échec du changement de mot de passe.';
      },
    });
  }
}
