import { CommonModule } from '@angular/common';
import { Component, HostListener } from '@angular/core';
import { TocItem } from '../models/utils';

@Component({
  selector: 'app-terms-and-conditions',
  imports: [CommonModule,],
  templateUrl: './terms-and-conditions.html',
  styleUrl: './terms-and-conditions.css',
})
export class TermsAndConditions {
  activeSectionId: string = 'cgu-1';

  tocItems: TocItem[] = [
    { id: 'cgu-1', title: '1. Objet' },
    { id: 'cgu-2', title: '2. Accès à la Plateforme' },
    { id: 'cgu-3', title: '3. Inscription et Sécurité' },
    { id: 'cgu-4', title: '4. Soumission des Candidatures' },
    { id: 'cgu-5', title: '5. Propriété Intellectuelle' },
    { id: 'cgu-6', title: '6. Responsabilités' },
    { id: 'cgu-7', title: '7. Modification des CGU' },
    { id: 'cgu-8', title: '8. Contact' }
  ];

  @HostListener('window:scroll', [])
  onWindowScroll() {
    const sections = this.tocItems.map(item => document.getElementById(item.id));
    const scrollPosition = window.pageYOffset || document.documentElement.scrollTop || document.body.scrollTop || 0;

    for (const section of sections) {
      if (section && scrollPosition >= section.offsetTop - 120) {
        this.activeSectionId = section.id;
      }
    }
  }

  scrollToSection(event: Event, sectionId: string) {
    event.preventDefault();
    const element = document.getElementById(sectionId);
    if (element) {
      element.scrollIntoView({ behavior: 'smooth' });
      this.activeSectionId = sectionId;
    }
  }
}
