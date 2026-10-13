import { HttpErrorResponse } from '@angular/common/http';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { NEVER, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { Auth } from '../../../../core/auth/services/authService';
import { RecrutementService } from '../../../../core/recrutement/services/recrutement.service';
import { FooterComponent } from '../../../../shared/components/footer/footer.component';
import { NavbarComponent } from '../../../../shared/components/navbar/navbar.component';
import { NotificationService } from '../../../../shared/components/ui/notification.service';
import { FormulaireComponent } from './formulaire.component';

/**
 * Ce qui se passe entre « Soumettre » et le suivi : le dépôt du dossier, puis l'envoi des pièces.
 *
 * <p>Audit du 2026-10-13 : le candidat déposait son dossier, une pièce échouait, une alerte fugitive le disait, et
 * le suivi lui redemandait ses documents sans qu'il ait compris pourquoi.
 */
describe('FormulaireComponent — dépôt du dossier et envoi des pièces', () => {
  let fixture: ComponentFixture<FormulaireComponent>;
  let composant: FormulaireComponent;
  let naviguer: ReturnType<typeof vi.fn>;

  const pdf = (nom: string) => new File(['%PDF-1.4'], nom, { type: 'application/pdf' });
  const erreur = (status: number, message?: string) => new HttpErrorResponse({ status, error: message ? { message } : null });
  const texte = () => (fixture.nativeElement as HTMLElement).textContent!.replace(/\s+/g, ' ');

  const DOSSIER = { id: 7, numeroDossier: 'INPG-2026-0007', statut: 'SOUMIS' };
  const complet = [
    { type: 'PIECE_IDENTITE', libelle: "Pièce d'identité", etat: 'FOURNI' },
    { type: 'CV', libelle: 'CV', etat: 'FOURNI' },
    { type: 'RELEVE_NOTES', libelle: 'Relevé de notes', etat: 'FOURNI' },
    { type: 'DIPLOME', libelle: 'Diplôme', etat: 'FOURNI' },
  ];

  /** Le même objet est fourni au composant : les tests en remplacent les méthodes pour jouer chaque scénario. */
  const service = {
    soumettreCandidature: vi.fn(),
    televerserPiece: vi.fn(),
    getMesCandidatures: vi.fn(),
    sauvegarderBrouillon: vi.fn(),
    getCataloguePublic: vi.fn(),
    getBrouillon: vi.fn(),
  };

  /** Fait s'écouler le temps simulé : reprises après panne, sauvegarde différée du brouillon, redirection. */
  const laisserPasserLeTemps = async (ms = 20000) => {
    await vi.advanceTimersByTimeAsync(ms);
    fixture.detectChanges();
  };

  /** Un formulaire entièrement et valablement rempli, prêt à être soumis. */
  function remplir(): void {
    composant.sessions = [{ id: 3, estActive: true } as never];
    Object.assign(composant.data, {
      nom: 'Diop', prenom: 'Awa', email: 'awa@example.com', telephone: '771234567', adresse: 'Point E, Dakar',
      genre: 'Femme', sessionCandidatureId: 3, niveauActuel: 'Licence (L3)', consentement: true,
    });
    composant.filesToUpload.set('identite', [pdf('cni.pdf')]);
    composant.filesToUpload.set('cv', [pdf('cv.pdf')]);
    composant.data.bulletinsParAnnee = [{ bulletins: [{ moyenne: 13.5, fichierNom: 'releve.pdf' }] }];
    composant.bulletinFiles = [[pdf('releve.pdf')]];
    composant.diplomes = [{ file: pdf('licence.pdf'), description: 'Licence, 2023' }];
  }

  beforeEach(() => {
    vi.useFakeTimers();
    localStorage.clear();
    naviguer = vi.fn();
    service.soumettreCandidature = vi.fn(() => of(DOSSIER));
    service.televerserPiece = vi.fn(() => of({ message: 'ok' }));
    service.getMesCandidatures = vi.fn()
      .mockReturnValueOnce(of([]))                                                    // ouverture : aucun dossier
      .mockReturnValue(of([{ ...DOSSIER, documentsAttendus: complet }]));              // contrôle final
    service.sauvegarderBrouillon = vi.fn(() => of(null));
    service.getCataloguePublic = vi.fn(() => of([]));
    service.getBrouillon = vi.fn(() => of(null));

    TestBed.configureTestingModule({
      imports: [FormulaireComponent],
      providers: [
        provideRouter([]),
        { provide: RecrutementService, useValue: service },
        { provide: Auth, useValue: { currentUser: () => null, isLoggedIn: () => true, getCurrentUser: () => of({}) } },
        { provide: NotificationService, useValue: { erreur: () => undefined, succes: () => undefined } },
      ],
    });
    TestBed.overrideComponent(FormulaireComponent, {
      remove: { imports: [NavbarComponent, FooterComponent] },
      add: { schemas: [NO_ERRORS_SCHEMA] },
    });
    vi.spyOn(TestBed.inject(Router), 'navigate').mockImplementation(naviguer as never);
    fixture = TestBed.createComponent(FormulaireComponent);
    composant = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => vi.useRealTimers());

  it('soumet une seule fois, même si le bouton est cliqué deux fois de suite', () => {
    remplir();
    service.soumettreCandidature = vi.fn(() => NEVER);   // le serveur n'a pas encore répondu

    composant.soumettre();
    composant.soumettre();

    expect(service.soumettreCandidature).toHaveBeenCalledTimes(1);
  });

  it('transmet la certification sur l\'honneur au serveur', () => {
    remplir();

    composant.soumettre();

    expect(service.soumettreCandidature).toHaveBeenCalledWith(expect.objectContaining({ consentement: true }));
  });

  /**
   * Constaté en exécution réelle : la dernière sauvegarde de brouillon, différée d'une seconde, partait après le dépôt
   * et recréait côté serveur le brouillon que le dépôt venait de supprimer.
   */
  it('une fois le dossier déposé, plus aucun brouillon n\'est écrit, ni localement ni sur le serveur', async () => {
    remplir();
    composant.saveDraft();                                   // frappe juste avant le clic : écriture serveur en attente
    expect(localStorage.getItem('candidature_brouillon')).not.toBeNull();

    composant.soumettre();
    composant.saveDraft();                                   // et une frappe tardive, après le dépôt
    await laisserPasserLeTemps();

    expect(localStorage.getItem('candidature_brouillon')).toBeNull();
    expect(service.sauvegarderBrouillon).not.toHaveBeenCalled();
  });

  it('envoie chaque pièce choisie avec son type de document', async () => {
    remplir();

    composant.soumettre();
    await laisserPasserLeTemps();

    const appels = service.televerserPiece.mock.calls;
    expect(appels.map((a) => a[2]).sort()).toEqual(['CV', 'DIPLOME', 'PIECE_IDENTITE', 'RELEVE_NOTES']);
    expect(appels.find((a) => a[2] === 'DIPLOME')![3]).toBe('Licence, 2023');
    expect(appels.find((a) => a[2] === 'RELEVE_NOTES')![3]).toBe(13.5);
    expect(appels.every((a) => a[0] === 7)).toBe(true);
  });

  it('quand tout est reçu et confirmé par le serveur, le candidat est conduit au suivi', async () => {
    remplir();

    composant.soumettre();
    await laisserPasserLeTemps();

    expect(composant.piecesEnEchec).toEqual([]);
    expect(composant.successMessage).toContain('INPG-2026-0007');
    expect(naviguer).toHaveBeenCalledWith(['/recrutement/suivi']);
  });

  /** Le cas signalé : une pièce n'arrive pas. Le candidat doit le lire, avec la raison, avant de partir. */
  it('une pièce refusée est listée avec sa raison, et le candidat n\'est pas renvoyé de force', async () => {
    remplir();
    service.televerserPiece = vi.fn((_id: number, file: File) =>
      file.name === 'cv.pdf' ? throwError(() => erreur(400, 'Format de fichier non autorisé.')) : of({ message: 'ok' }));

    composant.soumettre();
    await laisserPasserLeTemps();

    expect(composant.piecesEnEchec).toHaveLength(1);
    expect(composant.piecesEnEchec[0].raison).toBe('Format de fichier non autorisé.');
    expect(composant.piecesEnEchec[0].definitif).toBe(true);
    expect(composant.successMessage).toBe('');
    expect(naviguer).not.toHaveBeenCalled();
    expect(texte()).toContain('tous les documents ne sont pas arrivés');
    expect(texte()).toContain('cv.pdf');
    expect(texte()).toContain('Format de fichier non autorisé.');
  });

  it('une coupure passagère est reprise sans que le candidat ait à intervenir', async () => {
    remplir();
    let essais = 0;
    service.televerserPiece = vi.fn((_id: number, file: File) =>
      file.name === 'cv.pdf' && ++essais < 3 ? throwError(() => erreur(0)) : of({ message: 'ok' }));

    composant.soumettre();
    await laisserPasserLeTemps();

    expect(essais).toBe(3);
    expect(composant.piecesEnEchec).toEqual([]);
    expect(naviguer).toHaveBeenCalled();
  });

  it('le serveur a le dernier mot : un document qu\'il déclare manquant empêche de conclure « tout est transmis »', async () => {
    remplir();
    // Le composant est déjà ouvert : le prochain appel est le contrôle final.
    service.getMesCandidatures = vi.fn(() => of([{
      ...DOSSIER,
      documentsAttendus: complet.map((d) => (d.type === 'DIPLOME' ? { ...d, etat: 'A_FOURNIR' } : d)),
    }]));

    composant.soumettre();
    await laisserPasserLeTemps();

    expect(composant.documentsEncoreManquants).toEqual(['Diplôme']);
    expect(composant.successMessage).toBe('');
    expect(naviguer).not.toHaveBeenCalled();
    expect(texte()).toContain('Diplôme : non reçu');
  });

  it('« Renvoyer » ne repart qu\'avec les pièces en échec, sans redéposer le dossier', async () => {
    remplir();
    let cvRefuse = true;
    service.televerserPiece = vi.fn((_id: number, file: File) =>
      file.name === 'cv.pdf' && cvRefuse ? throwError(() => erreur(503)) : of({ message: 'ok' }));
    composant.soumettre();
    await laisserPasserLeTemps();
    expect(composant.piecesEnEchec).toHaveLength(1);
    expect(composant.aDesEchecsRenvoyables).toBe(true);
    service.televerserPiece.mockClear();
    cvRefuse = false;

    composant.reessayerEnvoi();
    await laisserPasserLeTemps();

    expect(service.televerserPiece).toHaveBeenCalledTimes(1);
    expect(service.televerserPiece.mock.calls[0][1].name).toBe('cv.pdf');
    expect(service.soumettreCandidature).toHaveBeenCalledTimes(1);
    expect(composant.piecesEnEchec).toEqual([]);
  });

  it('un refus de dépôt (téléphone déjà pris) laisse le formulaire en place et rien n\'est envoyé', async () => {
    remplir();
    service.soumettreCandidature = vi.fn(() =>
      throwError(() => erreur(409, 'Ce numéro de téléphone est déjà associé à un autre compte.')));

    composant.soumettre();
    await laisserPasserLeTemps();

    expect(composant.candidature).toBeNull();
    expect(composant.loading).toBe(false);
    expect(service.televerserPiece).not.toHaveBeenCalled();
  });
});
