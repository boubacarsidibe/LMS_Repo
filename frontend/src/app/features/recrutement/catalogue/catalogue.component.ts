import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { FooterComponent } from '../../../shared/components/footer/footer.component';
import { ContexteNavbarService } from '../../../shared/components/navbar/contexte-navbar.service';
import { RecrutementService } from '../../../core/recrutement/services/recrutement.service';

export interface FormationItem {
  id: number;
  libelle: string;
  description: string;
  imageUrl: string;
  niveau: string;
  domaine: string;
  duree: string;
}



const ITEMS_PER_PAGE = 6;

@Component({
  selector: 'app-catalogue',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, FooterComponent],
  templateUrl: './catalogue.component.html'
})
export class CatalogueComponent implements OnInit {
  /**
   * Catalogue servi par l'API. Il était auparavant écrit en dur dans ce fichier :
   * la vitrine de l'institut affichait des formations que personne ne pouvait
   * corriger sans redéployer le front.
   */
  formations: FormationItem[] = [];
  chargement = true;
  erreur = '';

  private readonly PAGE_STORAGE_KEY = 'catalogue_current_page';
  private contexteNavbar = inject(ContexteNavbarService);

  constructor(private router: Router, private recrutementService: RecrutementService) {}

  ngOnInit(): void {
    // Remet le navbar partagé en contexte étudiant : un formateur candidat
    // qui aurait navigué jusqu'ici depuis la vitrine des offres (via
    // « Formations », lien commun aux deux parcours) doit retrouver
    // « Sessions ouvertes » plutôt que « Offres de recrutement ».
    this.contexteNavbar.contexte.set('etudiant');
    this.recrutementService.getFormationsPubliques().subscribe({
      next: (formations) => {
        this.formations = formations.map((f) => ({
          id: f.id,
          libelle: f.intitule,
          description: f.description ?? '',
          imageUrl: f.imageUrl ?? '',
          niveau: f.niveau ?? '',
          domaine: f.domaine ?? '',
          duree: f.duree ?? '',
        }));
        this.chargement = false;
        this.restaurerPage();
      },
      error: () => {
        this.erreur = 'Le catalogue est momentanément indisponible.';
        this.chargement = false;
      },
    });
  }

  private restaurerPage(): void {
    const saved = sessionStorage.getItem(this.PAGE_STORAGE_KEY);
    if (saved) {
      const page = parseInt(saved, 10);
      if (page >= 1 && page <= this.totalPages) {
        this.currentPage = page;
      }
    }
  }

  // ---- Filtres ----
  searchTerm = '';
  /** Les listes de filtres se déduisent du catalogue reçu, plus d'une liste figée. */
  get niveaux(): string[] {
    return [...new Set(this.formations.map((f) => f.niveau).filter((v) => !!v))].sort();
  }
  get domaines(): string[] {
    return [...new Set(this.formations.map((f) => f.domaine).filter((v) => !!v))].sort();
  }
  selectedNiveaux: string[] = [];
  selectedDomaines: string[] = [];
  mobileFiltersOpen = false;

  toggleMobileFilters(): void {
    this.mobileFiltersOpen = !this.mobileFiltersOpen;
  }

  // ---- Pagination ----
  currentPage = 1;
  itemsPerPage = ITEMS_PER_PAGE;

  // ===================== FORMATIONS FILTRÉES =====================
  get filteredFormations(): FormationItem[] {
    let items: FormationItem[] = this.formations;

    if (this.searchTerm && this.searchTerm.trim() !== '') {
      const term = this.searchTerm.toLowerCase().trim();
      items = items.filter(f =>
        f.libelle.toLowerCase().includes(term) ||
        f.description.toLowerCase().includes(term) ||
        f.niveau.toLowerCase().includes(term) ||
        f.domaine.toLowerCase().includes(term)
      );
    }

    if (this.selectedNiveaux.length > 0) {
      items = items.filter(f => this.selectedNiveaux.includes(f.niveau));
    }

    if (this.selectedDomaines.length > 0) {
      items = items.filter(f => this.selectedDomaines.includes(f.domaine));
    }

    return items;
  }

  // ===================== PAGINATION =====================
  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredFormations.length / this.itemsPerPage));
  }

  get paginatedFormations(): FormationItem[] {
    const start = (this.currentPage - 1) * this.itemsPerPage;
    return this.filteredFormations.slice(start, start + this.itemsPerPage);
  }

  get visiblePages(): number[] {
    const pages: number[] = [];
    const maxVisible = 5;
    let start = Math.max(1, this.currentPage - Math.floor(maxVisible / 2));
    let end = start + maxVisible - 1;

    if (end > this.totalPages) {
      end = this.totalPages;
      start = Math.max(1, end - maxVisible + 1);
    }

    for (let i = start; i <= end; i++) {
      pages.push(i);
    }
    return pages;
  }

  get showEllipsis(): boolean {
    return this.visiblePages.length > 0 && this.visiblePages[this.visiblePages.length - 1] < this.totalPages;
  }

  changePage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
    }
  }

  // ===================== FILTRES =====================
  onSearchChange(): void {
    this.currentPage = 1;
  }

  toggleNiveau(niveau: string): void {
    const idx = this.selectedNiveaux.indexOf(niveau);
    if (idx >= 0) {
      this.selectedNiveaux.splice(idx, 1);
    } else {
      this.selectedNiveaux.push(niveau);
    }
    this.currentPage = 1;
  }

  toggleDomaine(domaine: string): void {
    const idx = this.selectedDomaines.indexOf(domaine);
    if (idx >= 0) {
      this.selectedDomaines.splice(idx, 1);
    } else {
      this.selectedDomaines.push(domaine);
    }
    this.currentPage = 1;
  }

  hasActiveFilters(): boolean {
    return this.selectedNiveaux.length > 0 || this.selectedDomaines.length > 0;
  }

  resetFilters(): void {
    this.searchTerm = '';
    this.selectedNiveaux = [];
    this.selectedDomaines = [];
    this.currentPage = 1;
  }

  navigateToDetail(id: number): void {
    sessionStorage.setItem(this.PAGE_STORAGE_KEY, String(this.currentPage));
    this.router.navigate(['/recrutement/catalogue', id]);
  }
}
