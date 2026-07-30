import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { FooterComponent } from '../../../../shared/components/footer/footer.component';
import { NavbarComponent } from '../../../../shared/components/navbar/navbar.component';

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

  submitForm(): void {
    if (this.sending || this.sent) return;
    this.sending = true;
    // Simuler l'envoi
    setTimeout(() => {
      this.sending = false;
      this.sent = true;
      // Réinitialiser après 3s
      setTimeout(() => {
        this.sent = false;
        this.fullName = '';
        this.email = '';
        this.phone = '';
        this.subject = '';
        this.message = '';
      }, 3000);
    }, 1500);
  }
}