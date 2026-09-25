import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { PedagogieService } from '../../services/pedagogie.service';
import { PedagogieCertificatsComponent } from './pedagogie-certificats.component';

describe('PedagogieCertificatsComponent', () => {
  let delivrer: ReturnType<typeof vi.fn>;
  let composant: PedagogieCertificatsComponent;

  beforeEach(async () => {
    delivrer = vi.fn().mockReturnValue(of({ id: 1, reference: 'INPG-CERT-2026-AB12CD34', etudiantNom: 'Awa Ndoye' }));
    await TestBed.configureTestingModule({
      imports: [PedagogieCertificatsComponent],
      providers: [{
        provide: PedagogieService,
        useValue: {
          getCertificats: () => of([]),
          getEtudiants: () => of([]),
          getPromotions: () => of([]),
          delivrerCertificat: delivrer,
        },
      }],
    }).compileComponents();
    composant = TestBed.createComponent(PedagogieCertificatsComponent).componentInstance;
    composant.ngOnInit();
    composant.ouvrirFormulaire();
  });

  function remplir(): void {
    composant.etudiantId = 7;
    composant.intitule = 'Comptage du gaz';
    composant.civilite = 'Monsieur';
    composant.dateDebut = '2026-02-02';
    composant.dateFin = '2026-02-06';
  }

  it('exige la civilité et la période imprimées par le modèle INPG', () => {
    composant.etudiantId = 7;
    composant.intitule = 'Comptage du gaz';
    expect(composant.saisieValide).toBe(false);

    remplir();
    expect(composant.saisieValide).toBe(true);
  });

  it('refuse une fin de formation antérieure à son début', () => {
    remplir();
    composant.dateFin = '2026-02-01';

    expect(composant.periodeInversee).toBe(true);
    expect(composant.saisieValide).toBe(false);
  });

  it('la date de délivrance vaut aujourd\'hui par défaut', () => {
    expect(composant.dateDelivrance).toBe(new Date().toISOString().slice(0, 10));
  });

  it('envoie tous les champs du modèle, la naissance et le code vides valant null', () => {
    remplir();
    composant.delivrer();

    expect(delivrer).toHaveBeenCalledWith(expect.objectContaining({
      etudiantId: 7,
      intituleFormation: 'Comptage du gaz',
      civilite: 'Monsieur',
      dateDebut: '2026-02-02',
      dateFin: '2026-02-06',
      dateNaissance: null,
      code: null,
    }));

    remplir();
    composant.dateNaissance = '1985-08-24';
    composant.code = ' CG1001260206/03 ';
    composant.delivrer();
    expect(delivrer).toHaveBeenLastCalledWith(expect.objectContaining({
      dateNaissance: '1985-08-24',
      code: 'CG1001260206/03',
    }));
  });
});
