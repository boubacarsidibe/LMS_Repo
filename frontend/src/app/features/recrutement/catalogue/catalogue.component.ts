import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { FooterComponent } from '../../../shared/components/footer/footer.component';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';

export interface FormationItem {
  id: number;
  libelle: string;
  description: string;
  imageUrl: string;
  niveau: string;
  domaine: string;
  duree: string;
}

const FORMATIONS_MOCK: FormationItem[] = [
  // ---- Page 1 (6 items) ----
  {
    id: 1,
    libelle: 'MBA en Économie et Management du Pétrole et du Gaz',
    description: 'Formez-vous à la gestion stratégique et économique de toute la chaîne de valeur des hydrocarbures.',
    imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuC8Z3B_KvgaZWOxVKKog0RKAC4l3EpnRWXYgf5EH4H_rs3d1weyL1QT9pVZaox4BZYGdWo2zzTTzbBT-l-_4lLPgd2ahXF3YCCIKH4gEjMN8GQOZDdNlo7d7wzkIkeBwsSVz34Cr-EPFcsAIH-xUh0JVBNNN-JXfQSSNlm2K2BeAEwU3XmQq3N5cJpgSa-dPLRra9PN-i-cjKzFBdWvz0ypzdgtQ_Ol6iM5QIvvy-eINlt0BGVki-KuCQ',
    niveau: 'Executive MBA',
    domaine: 'Management',
    duree: '18-24 Mois'
  },
  {
    id: 2,
    libelle: 'Ingénierie du Pétrole et du Gaz',
    description: 'Spécialisation de haut niveau pour ingénieurs couvrant l\'exploration, la production et le raffinage.',
    imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuDrfUkHIoEEVDLPeGSLuk5TdJ01_NW-pys3UYPAlbCVINhzZJbtEfPjtbgcxpBmMcg8USGuYuIsogdAxHg-JHLhA__h8VLmTGzQmSWC7Hns3mUNU-gEKj67qXoPw1D5_cetLzfwJCZxvIRUfJAvKCqATAwPcZ7ZNlHcmda__UadZhoZLt_IBr8hMJ8_x_simo7IoXnyu06F5FcPMg6r2VxlOSE_Uf6FhfJoRh4wGkx3o167n_mPgzgXIg',
    niveau: 'Master 2',
    domaine: 'Ingénierie',
    duree: '12 mois'
  },
  {
    id: 3,
    libelle: 'Renforcement de Capacités',
    description: 'Programmes courts et intensifs pour actualiser les compétences des professionnels du secteur.',
    imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuAkQkRSzD_KKRPwNeHlGDrY9wBwrDkpt2yLZNORckLWnX9sgSMJUmLwgnUmF_WpuTSwg5gs0r1Cc9Tfn973iexdNQVxDNXqm7h_yne4l4tJMi2Dl9hjWl-E9OPWKu4sEvpiGyQyrP3zG6k36GgsEzXJu-PjcFVC54hSZSKfCl5eK-31TMrBFLSDaShjCM-zYhHt2PiEuhTP3F8PYYV7picL-zU7W9n0W4qnnkIJfCmKqVZaKWIkbQ6ScA',
    niveau: 'Certificat',
    domaine: 'Management',
    duree: 'Courte durée'
  },
  {
    id: 4,
    libelle: 'Certifications HSE',
    description: 'Certifications internationales : Survival, Fire fighting, H2S, et sécurité opérationnelle.',
    imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuBObmr2ldoCiTTmAfZeOilPQ9uPxCSycTKtEgWiWSLdrf64dk2NAks8F1iZdj3wamLvH_18zZUz_SYP6SNlTQJOrvNiOH-VopUFAPhGLE0cqqJMtutquZLKsRENtCSxk40BxG3U_RKdio0RJiwOsxsOK-BMFrqgiOQ1kBVLYMlaWCicvMQUwXP4yjlmzG_HQ2fOl7KIgzGYQDVENkgho-T6OxKDYdcea3rDFmnhsv3zifuwM9pe8pn_JQ',
    niveau: 'Certificat',
    domaine: 'Sécurité',
    duree: 'Certificat'
  },
  {
    id: 5,
    libelle: 'Certifications Techniciens et Opérations',
    description: 'Certifications techniques pour les opérateurs de la chaîne de valeur pétrolière.',
    imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuC-x1KiJHk5v4SFWRvwg9NCVbHXM4F-kQJLSA822TwE0IeXajhPVP69U-AZFaXitxo3Y26R7QPzUKQ_dKg_8Z1IZ6yp--Ax7zMtdlN-HHWGlMyXj3LwAAGpnnzX8loGQKj6UxAt4Zw1vIqwUUgXUGw7mHOdieIHbbWi09z4JlNYYZz3uVfBjpZvLd4xOdsiW5olqiRAqZndtuHDYvERwJWmAx6iRK9mziMtdRquN9sZbf_wPON1NJmFOA',
    niveau: 'Certificat',
    domaine: 'Technique',
    duree: 'Certificat'
  },
  {
    id: 6,
    libelle: 'Géosciences et Ingénierie de Réservoir',
    description: 'Spécialisation avancée dans l\'exploration et la gestion des réservoirs.',
    imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuC2Ep5heUDsvYKrgF4aMlsSuMHJ2rcIlWnT2L9BHXsSnCKnTP1dIWqqImwKw1HVFtMQ2uWqnG7Sqdv3_t47SFnpIA2_pZeqNc7CVEXhU5DiCeNKfVcIWstCYLeuaoz2vlH2gUMdVvgjzbNs276zU67FCtHHkCWbVLUf8gGU8nDPWkzl_ao4kV-2CJt10O57G5dE8WCRmBa5HQ68sfPC9Ftr1-ygBwfSg1gidShdQul8oXbQeAUu1QMFpQ',
    niveau: 'Master 2',
    domaine: 'Géosciences',
    duree: '12 mois'
  },
  // ---- Page 2 (6 items) ----
  {
    id: 7,
    libelle: 'Survie (Survival)',
    description: 'Techniques et protocoles essentiels pour réagir efficacement en situation d\'urgence extrême. Formation pratique axée sur la survie en milieu hostile et les premiers secours avancés.',
    imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuCL6u8cL7fbnRCzbRUdcKOSAXZB4npLaQ7rkJGUeRZ4Rnk6UlMBSPrgt3lZALg5TiIVt1OV23hwb0qUujICapCKFYZhik4g87Y2gnVjapBOc7wgW3bCbbLo1PpXeB9n9J-hxyhf0qGWdQtJmug5v0_Rvzvt3L0QaruEZ9lwE-Wstvt0z2YYJX4cECYKTNS88_uJaOWuOWkaxXIqOjrow_oaBRbnpzc8Vh6sn3wOy1i2u7w42rwndv71AA',
    niveau: 'Certificat',
    domaine: 'Sécurité',
    duree: 'Courte durée'
  },
  {
    id: 8,
    libelle: 'Sécurité chimique (Chemical Safety)',
    description: 'Maîtrisez la manipulation, le stockage et l\'élimination des produits chimiques dangereux. Apprenez à évaluer les risques et à mettre en œuvre les mesures de prévention adéquates.',
    imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuB8xymg_jdpe0rAUREnGnJJIVDKJoqStJxgR7fdmO6zPz9l0tGQ3D0yNql7WptxQ6TqlXiui2HzpZ-S7VXBllq9fBoWoluQf1FHGwEAmZafoYP3PCaTw9aAly9OiOqwqmxFLKjhMgeuLOKnbSWVG228K-fSNbBL9i5AgEbgxqJRbhMfvpL3fQqa4NhzBZ3QScA4_CmT5IFQjuQ7aS_pwnf--VLemHtuQmMxaR8ynm7icTLL-l-ec1EvHg',
    niveau: 'Certificat',
    domaine: 'Sécurité',
    duree: 'Courte durée'
  },
  {
    id: 9,
    libelle: 'Hygiène et sécurité au travail',
    description: 'Fondamentaux de la réglementation HSE (Hygiène, Sécurité, Environnement). Développez une culture de la prévention pour garantir un environnement de travail sain et sûr.',
    imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuBlYh5YAttneehy2p2yXGN9e6xrV19GaWy4V9Hrr92Iux4C0xcH0N6o9-QfDBPXHrWzM6rsdUMh6ROwgKz20JsF0W_U2BnpwcIFJPISovB7Plljw6nWsAQFRACtkrhzuh1IcMjcxtSE57g5cUItOcACQe3yhbFWkKxW0HFspblZkjKqomza6fysPnFIfwDmGagKcUJKAtK9xtly3kIbf9ReXTWB7s-ckcB95QlemkpKntMvTq6N2L9tzA',
    niveau: 'Certificat',
    domaine: 'Sécurité',
    duree: 'Courte durée'
  },
  {
    id: 10,
    libelle: 'Espaces confinés et travail en hauteur',
    description: 'Procédures critiques pour les interventions en milieux périlleux. Utilisation des EPI, détection de gaz, sauvetage et techniques d\'accès sur corde.',
    imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuBPQHjYus84uLbV7qhxgDcPEXjwb0blZ021CjNMUNxXjN-8U3C8mVhTBP8GAsrkxwopSz1v6EsOmTKLVnor461I-YU_l26Mu499s5OshwI60dzaIIRDhhwAxiuTf9fjGO14UEp0srJChtUYhc_qG3BAzfqYyeC86869ZoR_I6QLcMCWMC_D76YC6qjS-c5CzcUpFgDJRucoG0-s1TLGGg7B2HjoWPJJ5qOyeWSOF4_FxUKBUKl8Fqaa2A',
    niveau: 'Certificat',
    domaine: 'Sécurité',
    duree: 'Courte durée'
  },
  {
    id: 11,
    libelle: 'Conduite défensive (Defensive Driving)',
    description: 'Anticipation des risques routiers et maîtrise du véhicule en situations d\'urgence. Formation théorique et pratique pour réduire la sinistralité de la flotte automobile.',
    imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuCrlFaDmfl-QESD3P06Qz9y84vmsmgWeuyl4Qe3gNQX52gP_P51cGDtdHEYFp5P29mD332ZCmS8z2TSgF7Ec2Rzksg5EOeq5a4_sIvPmqGi2a9TRsecjj9lllSlwqtOrWRs4CH7O9QZAcf89Rg9hH08ed-FJCC0AJQBhmnmtq1VoDXkxHs9YXobsnGuGEf3e5pP6N9l1iCGWNxFgh4cZ2D0ZvouqGGHSqh75pn8JpJf3n4SLKBUefLagQ',
    niveau: 'Certificat',
    domaine: 'Sécurité',
    duree: 'Courte durée'
  },
  {
    id: 12,
    libelle: 'Lutte contre l\'incendie (Firefighting)',
    description: 'Formation complète sur la prévention incendie, l\'évacuation et l\'utilisation des moyens de première intervention. Exercices sur feux réels en centre d\'entraînement sécurisé.',
    imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuCIT2uuTe9v1Yx8h28xwQWVZuwVeHKI4wnEwL7T5qW-RXUoXzelw-z2F2AxnXakLmbV3tDKhvZWN8QO1CdNBpRJdbUrM5L-U-YLKglFhzIQLIGyYxRYVu7xa9l5cn-Yto89617vK_1dbXHB78rvg5PvAXMZ1N6yNDMXiUPsOWLwBmu1cUzZgCfAwrwvyUxdq1qnIh8TR04pTupbNJr7MMm-OYrxamzy68TVM0g-s5MVnyTnDXBQcQu7OQ',
    niveau: 'Certificat',
    domaine: 'Sécurité',
    duree: 'Courte durée'
  },
  // ---- Page 3 (3 items) ----
  {
    id: 13,
    libelle: 'Détection H₂S et gaz',
    description: 'Formation spécialisée sur la détection des gaz dangereux, en particulier le sulfure d\'hydrogène (H₂S). Apprenez à utiliser les équipements de détection, à interpréter les alarmes et à réagir efficacement en cas d\'urgence dans des environnements à haut risque.',
    imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuA3OQuNP4tDf-sBiwH97c7DOvXisTDF8oDdmk_sCT02n0rBjCZr9L3ko6UCKCqdJQi0X28FHAj2OyfjESSR3YUSOKNmoACGA9hd21sgVmYMcjPaVKDIbyfSEM8KRyaE_kkOqTbDSRO3SBEYMOAfVwJU97dgmpWhv-pjg8bUp2FOT-xf6szm4GFppgfHMB9U125xj8TWJipibPcKvS9_5AQ_CBIyFm6MA3D_1_mJWou2xNf1yAw3Qv943w',
    niveau: 'Certificat',
    domaine: 'Sécurité',
    duree: 'Courte durée'
  },
  {
    id: 14,
    libelle: 'Levage et gréage',
    description: 'Maîtrisez les techniques sécuritaires de levage et de gréage. Ce cours couvre l\'inspection du matériel de levage, le calcul des charges, la sélection des élingues appropriées et les communications essentielles pour des opérations de manutention complexes et sûres.',
    imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuBTt5XV1wj0V8yBwk80jSwuKpb25VetkKYd1DSVSm0HpULv1raQRDZcdnQwSyNQbsNFza6wbgJ2Hq2Y8JsmrL63JABdBu1KE3iYUe65b1w9E9WILhgyxokGHZ4ISYYy2m7MKlkaPIxLZQKRuU1SlTIfa6C_w3KUCTQzr6GiQic0I49u8D1lbpOFBdNoPBBVauvhQ3c1rTiixodzxRd0ymzjbp3NRkogGzN-Ytw_4EReEqriszMoFkCOIw',
    niveau: 'Certificat',
    domaine: 'Sécurité',
    duree: 'Courte durée'
  },
  {
    id: 15,
    libelle: 'Rôles spécifiques',
    description: 'Formation destinée aux rôles critiques en matière de sécurité (superviseurs, surveillants d\'espace clos, gardes-feu). Développez les compétences nécessaires pour assumer des responsabilités spécifiques en HSE et garantir la conformité réglementaire sur le terrain.',
    imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuDYWNBRG-HKl1qYdPej1nsb6UfCNR958oOgrqV1oYN3-zJ4uqhPU-TBPDNiJYcKNBG1E0Sp3w7So2yfrcu7zFPV3UUT-BzUt1-JQcTbAzdO54HMfM99yZ06c02--5QEWTpys01Yk4_DbziGEc7gx2oD2Mb0mtmjiLmgwSlpDRRdgQwMAe2cBnY-T4FYRXg7KRivGefI7GBYNxWsL6e-8u4LYBEMYJG6T1LWcGMbVIzbxZoMvDJiM64YmQ',
    niveau: 'Certificat',
    domaine: 'Sécurité',
    duree: 'Courte durée'
  },
  {
    id: 16,
    libelle: 'MBA en Finance et Stratégie',
    description: 'Programme d\'excellence combinant finance avancée et stratégie d\'entreprise. Préparez-vous aux postes de direction dans un environnement économique mondialisé.',
    imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuDrfUkHIoEEVDLPeGSLuk5TdJ01_NW-pys3UYPAlbCVINhzZJbtEfPjtbgcxpBmMcg8USGuYuIsogdAxHg-JHLhA__h8VLmTGzQmSWC7Hns3mUNU-gEKj67qXoPw1D5_cetLzfwJCZxvIRUfJAvKCqATAwPcZ7ZNlHcmda__UadZhoZLt_IBr8hMJ8_x_simo7IoXnyu06F5FcPMg6r2VxlOSE_Uf6FhfJoRh4wGkx3o167n_mPgzgXIg',
    niveau: 'Executive MBA',
    domaine: 'Finance',
    duree: '18-24 Mois'
  }
];

const ITEMS_PER_PAGE = 6;

@Component({
  selector: 'app-catalogue',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, FooterComponent, NavbarComponent],
  templateUrl: './catalogue.component.html'
})
export class CatalogueComponent {
  formations: FormationItem[] = FORMATIONS_MOCK;

  private readonly PAGE_STORAGE_KEY = 'catalogue_current_page';

  constructor(private router: Router) {}

  ngOnInit(): void {
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
  niveaux: string[] = ['Master 2', 'Executive MBA', 'Certificat', 'Ingénierie', 'Géosciences'];
  domaines: string[] = ['Management', 'Finance', 'Digital', 'Ingénierie', 'Sécurité', 'Technique', 'Géosciences'];
  selectedNiveaux: string[] = [];
  selectedDomaines: string[] = [];

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
