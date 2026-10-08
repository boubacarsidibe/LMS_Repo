import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ApercuCertificats, PedagogieService } from '../../services/pedagogie.service';
import { PedagogieCertificatsComponent } from './pedagogie-certificats.component';

const APERCU: ApercuCertificats = {
  promotionId: 5,
  promotionLibelle: 'CG-2026',
  formationIntitule: 'Comptage du gaz',
  dateDebut: '2026-02-02',
  dateFin: '2026-02-06',
  etudiants: [
    { id: 1, nom: 'Awa Ba', matricule: 'INPG-2026-001', civilite: null, dateNaissance: null, eligible: true, coursNonTermines: [] },
    { id: 2, nom: 'Moussa Ndiaye', matricule: 'INPG-2026-002', civilite: 'Monsieur', dateNaissance: '1990-03-04', eligible: true, coursNonTermines: [] },
    { id: 3, nom: 'Fatou Diop', matricule: 'INPG-2026-003', civilite: null, dateNaissance: null, eligible: false, coursNonTermines: ['Métrologie'] },
  ],
};

describe('PedagogieCertificatsComponent', () => {
  let delivrerLot: ReturnType<typeof vi.fn>;
  let apercu: ReturnType<typeof vi.fn>;
  let composant: PedagogieCertificatsComponent;

  beforeEach(async () => {
    delivrerLot = vi.fn().mockReturnValue(of({ delivres: [{ certificatId: 1, reference: 'R', etudiantId: 1, etudiantNom: 'Awa Ba' }], refuses: [] }));
    apercu = vi.fn().mockReturnValue(of(APERCU));
    await TestBed.configureTestingModule({
      imports: [PedagogieCertificatsComponent],
      providers: [{
        provide: PedagogieService,
        useValue: {
          getCertificats: () => of([]),
          getPromotions: () => of([{ id: 5, libelle: 'CG-2026', formationIntitule: 'Comptage du gaz', effectif: 3 }]),
          getApercuCertificats: apercu,
          delivrerCertificatsLot: delivrerLot,
        },
      }],
    }).compileComponents();
    composant = TestBed.createComponent(PedagogieCertificatsComponent).componentInstance;
    composant.ngOnInit();
    composant.ouvrirFormulaire();
  });

  /** Promotion choisie, et civilité renseignée pour Awa Ba (Moussa Ndiaye l'a déjà sur sa fiche). */
  function remplir(): void {
    composant.surPromotion([5]);
    composant.identites[1].civilite = 'Madame';
  }

  it('exige la promotion en premier : rien ne peut être délivré sans elle', () => {
    expect(composant.promotionId).toBeNull();
    expect(composant.saisieValide).toBe(false);
    expect(composant.obstacle).toContain('promotion');
  });

  it('propose les promotions avec leur formation, pour la recherche', () => {
    expect(composant.optionsPromotions).toEqual([
      { valeur: 5, libelle: 'CG-2026', detail: 'Comptage du gaz · 3 étudiant(s)' },
    ]);
  });

  it('choisir la promotion remplit la formation et la période, et reprend la civilité de la fiche', () => {
    composant.surPromotion([5]);

    expect(apercu).toHaveBeenCalledWith(5);
    expect(composant.apercu?.formationIntitule).toBe('Comptage du gaz');
    expect(composant.dateDebut).toBe('2026-02-02');
    expect(composant.dateFin).toBe('2026-02-06');
    expect(composant.identites[2]).toEqual({ civilite: 'Monsieur', dateNaissance: '1990-03-04' });
  });

  it('écarte les étudiants au programme inachevé, sans bloquer les autres', () => {
    remplir();

    expect(composant.destinataires.map((e) => e.id)).toEqual([1, 2]);
    expect(composant.ecartes.map((e) => e.id)).toEqual([3]);
    expect(composant.saisieValide).toBe(true);
  });

  it('exige la civilité de chaque destinataire, imprimée par le modèle INPG', () => {
    composant.surPromotion([5]);

    expect(composant.sansCivilite.map((e) => e.id)).toEqual([1]);
    expect(composant.saisieValide).toBe(false);
    expect(composant.obstacle).toContain('civilité');
  });

  it('« tous sauf » exige au moins un étudiant à exclure, et le retire des destinataires', () => {
    remplir();
    composant.choisirMode('TOUS_SAUF');
    expect(composant.saisieValide).toBe(false);

    composant.exclusIds = [1];
    expect(composant.destinataires.map((e) => e.id)).toEqual([2]);
    expect(composant.saisieValide).toBe(true);
  });

  it('refuse une promotion sans formation', () => {
    apercu.mockReturnValue(of({ ...APERCU, formationIntitule: null }));
    remplir();

    expect(composant.formationManquante).toBe(true);
    expect(composant.saisieValide).toBe(false);
    expect(composant.obstacle).toContain('aucune formation');
  });

  it('refuse une fin de formation antérieure à son début', () => {
    remplir();
    composant.dateFin = '2026-02-01';

    expect(composant.periodeInversee).toBe(true);
    expect(composant.saisieValide).toBe(false);
  });

  it('repère un étudiant déjà certifié sur la même période, d’après le registre (dates en JJ/MM/AAAA)', () => {
    remplir();
    composant.certificats = [{
      id: 9, reference: 'R9', etudiantId: 2, etudiantNom: 'Moussa Ndiaye', etudiantMatricule: null,
      intituleFormation: 'comptage du gaz', promotionLibelle: 'CG-2026', dateEmission: null, formateurNom: null,
      civilite: 'Monsieur', dateNaissance: null, dateDebut: '02/02/2026', dateFin: '06/02/2026', codeCertificat: null,
    }];

    expect(composant.dejaDelivre(APERCU.etudiants[1])).toBe(true);
    expect(composant.destinataires.map((e) => e.id)).toEqual([1]);

    composant.dateFin = '2026-02-07';
    expect(composant.dejaDelivre(APERCU.etudiants[1])).toBe(false);
  });

  it('envoie la promotion et le mode, sans intitulé : la formation vient du serveur', () => {
    remplir();
    composant.identites[1].dateNaissance = '1992-05-17';
    composant.codeSession = ' CG1001260206 ';
    composant.delivrer();

    const demande = delivrerLot.mock.calls[0][0];
    expect(demande).not.toHaveProperty('intituleFormation');
    expect(demande).toEqual({
      promotionId: 5,
      mode: 'TOUS',
      exclusIds: [],
      etudiantId: null,
      dateDebut: '2026-02-02',
      dateFin: '2026-02-06',
      codeSession: 'CG1001260206',
      identites: [
        { etudiantId: 1, civilite: 'Madame', dateNaissance: '1992-05-17' },
        { etudiantId: 2, civilite: 'Monsieur', dateNaissance: '1990-03-04' },
      ],
    });
    expect(composant.messageSucces).toContain('1 certificat(s) délivré(s)');
  });

  it('en « tous sauf », envoie les exclus ; en « tous », jamais', () => {
    remplir();
    composant.choisirMode('TOUS_SAUF');
    composant.exclusIds = [1];
    composant.delivrer();
    expect(delivrerLot.mock.calls[0][0]).toMatchObject({ mode: 'TOUS_SAUF', exclusIds: [1] });

    composant.choisirMode('TOUS');
    composant.delivrer();
    expect(delivrerLot.mock.calls[1][0]).toMatchObject({ mode: 'TOUS', exclusIds: [] });
  });

  it('« un seul étudiant » exige de choisir l’étudiant, puis ne vise que lui', () => {
    remplir();
    composant.choisirMode('UN_SEUL');
    expect(composant.saisieValide).toBe(false);
    expect(composant.obstacle).toContain('Choisissez l’étudiant');
    expect(composant.beneficiaires).toEqual([]);

    composant.etudiantId = 2;
    expect(composant.beneficiaires.map((e) => e.id)).toEqual([2]);
    expect(composant.destinataires.map((e) => e.id)).toEqual([2]);
    expect(composant.saisieValide).toBe(true);
  });

  it('« un seul étudiant » envoie l’étudiant et son code tel quel, sans exclus', () => {
    remplir();
    composant.choisirMode('UN_SEUL');
    composant.etudiantId = 2;
    composant.codeSession = ' CG1001260206/03 ';
    composant.exclusIds = [1];
    composant.delivrer();

    expect(delivrerLot.mock.calls[0][0]).toMatchObject({
      mode: 'UN_SEUL',
      etudiantId: 2,
      exclusIds: [],
      codeSession: 'CG1001260206/03',
      identites: [{ etudiantId: 2, civilite: 'Monsieur', dateNaissance: '1990-03-04' }],
    });
  });

  it('« un seul étudiant » sur un étudiant au programme inachevé : rien à délivrer', () => {
    remplir();
    composant.choisirMode('UN_SEUL');
    composant.etudiantId = 3;

    expect(composant.destinataires).toEqual([]);
    expect(composant.saisieValide).toBe(false);
    expect(composant.obstacle).toContain('Aucun étudiant');
  });

  it('changer de promotion oublie l’étudiant choisi, qui n’en fait peut-être plus partie', () => {
    remplir();
    composant.choisirMode('UN_SEUL');
    composant.etudiantId = 2;

    composant.surPromotion([5]);
    expect(composant.etudiantId).toBeNull();
  });

  it('en « tous » ou « tous sauf », n’envoie jamais d’étudiant isolé', () => {
    remplir();
    composant.etudiantId = 2;
    composant.delivrer();

    expect(delivrerLot.mock.calls[0][0]).toMatchObject({ mode: 'TOUS', etudiantId: null });
  });

  it('ne délivre rien tant que la saisie est invalide', () => {
    composant.surPromotion([5]);
    composant.delivrer();

    expect(delivrerLot).not.toHaveBeenCalled();
  });
});
