import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule, NgForm } from '@angular/forms';
import { FooterComponent } from '../../../../shared/components/footer/footer.component';
import { NavbarComponent } from '../../../../shared/components/navbar/navbar.component';
import { RecrutementService } from '../../../../core/recrutement/services/recrutement.service';

@Component({
  selector: 'app-contact',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, FooterComponent, NavbarComponent],
  templateUrl: './contact.component.html',
})
export class ContactComponent {
  fullName = '';
  email = '';
  phonePrefix = '+221';
  phone = '';
  subject = '';
  message = '';

  sending = false;
  sent = false;
  erreur = '';

  /**
   * Avant une tentative d'envoi, un champ requis vide n'affiche aucune erreur
   * (seule la bulle native du navigateur bloquait, sans indiquer lequel des
   * six champs posait problème — relevé lors d'un audit externe).
   */
  envoiTente = false;

  constructor(private recrutementService: RecrutementService) {}

  /** Un champ n'affiche son erreur qu'après une tentative d'envoi, jamais avant (même en le quittant). */
  champEnErreur(form: NgForm, nom: string): boolean {
    const controle = form.controls[nom];
    return !!controle && controle.invalid && this.envoiTente;
  }

  /**
   * Transmet la question au service admission.
   *
   * <p>Le formulaire simulait auparavant l'envoi avec un délai, alors que
   * l'endpoint public existait et fonctionnait : les messages des visiteurs
   * n'arrivaient jamais à l'INPG.
   */
  submitForm(form: NgForm): void {
    this.envoiTente = true;
    if (this.sending || this.sent) return;

    if (form.invalid) {
      form.form.markAllAsTouched();
      return;
    }

    this.sending = true;
    this.erreur = '';

    // L'endpoint ne transporte qu'un e-mail et un message : on y replie le reste
    // du formulaire pour que le destinataire dispose du contexte complet.
    this.recrutementService.poserQuestion(this.email.trim(), this.composerMessage(this.message.trim())).subscribe({
      next: () => {
        this.sending = false;
        this.sent = true;
        this.envoiTente = false;
        setTimeout(() => {
          this.sent = false;
          this.fullName = '';
          this.email = '';
          this.phone = '';
          this.subject = '';
          this.message = '';
          form.form.markAsUntouched();
        }, 3000);
      },
      error: (e) => {
        this.sending = false;
        this.erreur =
          e?.error?.message || "L'envoi de votre message a échoué. Merci de réessayer dans un instant.";
      },
    });
  }

  private composerMessage(message: string): string {
    const lignes: string[] = [];
    if (this.fullName.trim()) lignes.push(`Nom : ${this.fullName.trim()}`);
    if (this.phone.trim()) lignes.push(`Téléphone : ${this.phonePrefix} ${this.phone.trim()}`);
    if (this.subject.trim()) lignes.push(`Objet : ${this.subject.trim()}`);
    return lignes.length ? `${lignes.join('\n')}\n\n${message}` : message;
  }
}