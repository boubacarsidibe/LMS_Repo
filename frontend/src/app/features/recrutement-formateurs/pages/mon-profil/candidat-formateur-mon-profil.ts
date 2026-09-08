import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Auth } from '../../../../core/auth/services/authService';

/**
 * Profil du candidat formateur. Même patron que {@code EcoleMonProfilComponent} :
 * identité lue depuis {@link Auth.currentUser}, mot de passe via l'endpoint
 * générique `/api/auth/change-password`.
 */
@Component({
  selector: 'app-candidat-formateur-mon-profil',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="p-margin-desktop space-y-stack-lg max-w-container-max mx-auto">
      <header>
        <span class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-widest mb-2 block">Compte</span>
        <h1 class="font-display text-display text-on-surface leading-none -ml-1">Mon profil</h1>
        <p class="font-body-lg text-body-lg text-on-surface-variant mt-4 max-w-2xl">Votre identité et votre mot de passe.</p>
      </header>

      <section class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg">
        <h2 class="font-headline-sm text-headline-sm text-primary flex items-center gap-2 mb-6">
          <span class="material-symbols-outlined text-secondary">badge</span>Identité
        </h2>
        <dl class="grid grid-cols-1 sm:grid-cols-2 gap-stack-md">
          <div>
            <dt class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider">Nom d'utilisateur</dt>
            <dd class="font-body-md text-body-md text-on-surface mt-1">{{ utilisateur?.username || '—' }}</dd>
          </div>
          <div>
            <dt class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider">Courriel</dt>
            <dd class="font-body-md text-body-md text-on-surface mt-1">{{ utilisateur?.email || '—' }}</dd>
          </div>
          <div>
            <dt class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider">Rôle</dt>
            <dd class="font-body-md text-body-md text-on-surface mt-1">{{ roleAffiche }}</dd>
          </div>
        </dl>
      </section>

      <section class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm p-stack-lg">
        <h2 class="font-headline-sm text-headline-sm text-primary flex items-center gap-2 mb-6">
          <span class="material-symbols-outlined text-secondary">lock</span>Mot de passe
        </h2>

        <p *ngIf="messageSucces" class="font-body-sm text-body-sm text-on-surface bg-secondary-container/30 rounded-lg px-4 py-3 mb-4">{{ messageSucces }}</p>
        <p *ngIf="messageErreur" class="font-body-sm text-body-sm text-error bg-error-container/20 rounded-lg px-4 py-3 mb-4">{{ messageErreur }}</p>

        <form [formGroup]="form" (ngSubmit)="changerMotDePasse()" class="space-y-stack-md max-w-md">
          <label class="flex flex-col gap-1">
            <span class="font-label-sm text-label-sm text-on-surface-variant">Mot de passe actuel</span>
            <input type="password" formControlName="motDePasseActuel" autocomplete="current-password"
                   class="p-3 bg-surface-container-low rounded-lg border border-outline-variant font-body-sm text-body-sm text-on-surface" />
          </label>
          <label class="flex flex-col gap-1">
            <span class="font-label-sm text-label-sm text-on-surface-variant">Nouveau mot de passe</span>
            <input type="password" formControlName="nouveauMotDePasse" autocomplete="new-password"
                   class="p-3 bg-surface-container-low rounded-lg border border-outline-variant font-body-sm text-body-sm text-on-surface" />
            <span class="font-body-sm text-body-sm text-on-surface-variant">6 caractères minimum.</span>
          </label>
          <label class="flex flex-col gap-1">
            <span class="font-label-sm text-label-sm text-on-surface-variant">Confirmation</span>
            <input type="password" formControlName="confirmation" autocomplete="new-password"
                   class="p-3 bg-surface-container-low rounded-lg border border-outline-variant font-body-sm text-body-sm text-on-surface" />
          </label>
          <button type="submit" [disabled]="enregistrement"
                  class="px-6 py-3 bg-secondary text-on-secondary rounded-lg font-label-md text-label-md hover:opacity-90 transition-colors disabled:opacity-50">
            {{ enregistrement ? 'Enregistrement…' : 'Changer le mot de passe' }}
          </button>
        </form>
      </section>
    </div>
  `,
})
export class CandidatFormateurMonProfilComponent {
  private auth = inject(Auth);
  private fb = inject(FormBuilder);

  utilisateur = this.auth.currentUser();

  enregistrement = false;
  messageSucces = '';
  messageErreur = '';

  readonly form: FormGroup = this.fb.group({
    motDePasseActuel: ['', [Validators.required]],
    nouveauMotDePasse: ['', [Validators.required, Validators.minLength(6)]],
    confirmation: ['', [Validators.required]],
  });

  get roleAffiche(): string {
    const role = this.utilisateur?.roles?.[0] || '';
    if (role === 'ROLE_CANDIDAT_FORMATEUR') return 'Candidat formateur';
    if (role === 'ROLE_FORMATEUR') return 'Formateur';
    return role;
  }

  changerMotDePasse(): void {
    this.messageSucces = '';
    this.messageErreur = '';
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { motDePasseActuel, nouveauMotDePasse, confirmation } = this.form.getRawValue();
    if (nouveauMotDePasse !== confirmation) {
      this.messageErreur = 'La confirmation ne correspond pas au nouveau mot de passe.';
      return;
    }
    this.enregistrement = true;
    this.auth.ChangePassword(motDePasseActuel, nouveauMotDePasse).subscribe({
      next: () => {
        this.enregistrement = false;
        this.messageSucces = 'Mot de passe modifié.';
        this.form.reset();
      },
      error: (err) => {
        this.enregistrement = false;
        this.messageErreur = err?.error?.message || 'Le mot de passe actuel est incorrect.';
      },
    });
  }
}
