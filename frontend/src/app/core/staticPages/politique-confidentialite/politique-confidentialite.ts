import { Component, HostListener, signal } from '@angular/core';

@Component({
  selector: 'app-politique-confidentialite',
  standalone: true,
  templateUrl: './politique-confidentialite.html',
  styleUrl: './politique-confidentialite.css',
})
export class PolitiqueConfidentialite {

  // Corregido a 'section-1' para coincidir con los IDs del TOC
  activeSection = signal<string>('section-1');

  tocItems = [
    { id: 'section-1', label: '1. Responsable du Traitement' },
    { id: 'section-2', label: '2. Données Personnelles Collectées' },
    { id: 'section-3', label: '3. Finalités du Traitement' },
    { id: 'section-4', label: '4. Hébergement et Confidentialité' },
    { id: 'section-5', label: '5. Durée de Conservation' },
    { id: 'section-6', label: '6. Vos Droits' },
    { id: 'section-7', label: '7. Sécurité des Données' },
    { id: 'section-8', label: '8. Cookies et Session' }
  ];

  @HostListener('window:scroll', [])
  onWindowScroll() {
    const scrollPosition = window.scrollY || document.documentElement.scrollTop;

    for (const item of this.tocItems) {
      const section = document.getElementById(item.id);
      if (section) {
        // Offset de 150px para compensar el header o barra superior
        const sectionTop = section.offsetTop - 150; 
        const sectionHeight = section.offsetHeight;

        if (scrollPosition >= sectionTop && scrollPosition < sectionTop + sectionHeight) {
          this.activeSection.set(item.id);
          break;
        }
      }
    }
  }

  scrollTo(sectionId: string, event: Event) {
    event.preventDefault();
    const element = document.getElementById(sectionId);
    if (element) {
      element.scrollIntoView({ behavior: 'smooth' });
    }
  }
}
