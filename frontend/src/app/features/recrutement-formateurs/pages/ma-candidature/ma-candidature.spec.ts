import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { MediaProtegeService } from '../../../espace-etudiants/services/media-protege.service';
import { CandidatureFormateurSaisie, OffreFormateur } from '../../models/recrutement-formateur.models';
import { RecrutementFormateurService } from '../../services/recrutement-formateur.service';
import { MaCandidatureComponent } from './ma-candidature';

/**
 * Le dossier de candidature du formateur : état civil, adresse, formations, expériences, langues, certifications
 * et domaines d'expertise, en sections rétractables — sur le modèle d'un portail de recrutement de référence.
 * Seuls nom, prénom et téléphone sont exigés ; tout le reste est facultatif.
 */
describe('MaCandidatureComponent — dossier enrichi', () => {
  let fixture: ComponentFixture<MaCandidatureComponent>;
  let composant: MaCandidatureComponent;
  const el = () => fixture.nativeElement as HTMLElement;
  const texte = () => el().textContent!.replace(/\s+/g, ' ');

  const offre: OffreFormateur = {
    id: 8, intitule: 'Formateur HSE', domaine: 'Sécurité', descriptionMissions: null, profilRecherche: null,
    typeContrat: 'VACATAIRE', lieu: null, nbPostes: 1, formationId: null, intituleFormation: null,
    datePublication: '2026-10-01', dateLimiteCandidature: null, statut: 'PUBLIEE', dateCreation: '2026-10-01T10:00:00',
    nombreCandidatures: 0, fichePosteNom: null,
  };
  const postuler = vi.fn((donnees: CandidatureFormateurSaisie) => of({ id: 1, ...donnees } as never));

  beforeEach(() => {
    postuler.mockClear();
    TestBed.configureTestingModule({
      imports: [MaCandidatureComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap({ offre: '8' }) } } },
        { provide: MediaProtegeService, useValue: { ouvrir: () => undefined } },
        {
          provide: RecrutementFormateurService,
          useValue: {
            getDomainesExpertise: () => of(['Sécurité', 'Technique']),
            getMesCandidatures: () => of([]),
            getOffre: () => of(offre),
            postuler,
          },
        },
      ],
    });
    fixture = TestBed.createComponent(MaCandidatureComponent);
    composant = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("affiche l'offre visée et les sept sections du dossier, toutes repliées au départ", () => {
    expect(texte()).toContain('Formateur HSE');
    for (const titre of ['Informations personnelles', 'Coordonnées', 'Formation', 'Expérience professionnelle',
      'Compétences linguistiques', 'Certifications', "Domaines d'expertise"]) {
      expect(texte()).toContain(titre);
    }
    expect(Object.values(composant.sectionsOuvertes).every((ouverte) => !ouverte)).toBe(true);
    expect(el().querySelector('input[name="civilite"]')).toBeNull();
  });

  it("l'en-tête de Informations personnelles, Formation, Expérience professionnelle et Compétences linguistiques ne dit plus « facultatif »", () => {
    const enTete = (titre: string) =>
      Array.from(el().querySelectorAll('form button[type="button"]'))
        .map((b) => (b.textContent ?? '').replace(/\s+/g, ' ').trim())
        .find((t) => t.includes(titre))!;

    for (const titre of ['Informations personnelles', 'Formation', 'Expérience professionnelle', 'Compétences linguistiques']) {
      expect(enTete(titre)).not.toContain('facultatif');
    }
    // les trois autres sections gardent leur mention
    for (const titre of ['Coordonnées', 'Certifications', "Domaines d'expertise"]) {
      expect(enTete(titre)).toContain('facultatif');
    }
  });

  it("le compteur d'un bloc reste affiché une fois des entrées ajoutées : « Formation (2) »", () => {
    const enTeteFormation = () =>
      Array.from(el().querySelectorAll('form button[type="button"]'))
        .map((b) => (b.textContent ?? '').replace(/\s+/g, ' ').trim())
        .find((t) => t.includes('Formation'))!;
    expect(enTeteFormation()).not.toMatch(/\(\d+\)/);

    composant.ajouterFormation();
    composant.ajouterFormation();
    fixture.detectChanges();

    expect(enTeteFormation()).toMatch(/Formation\s*\(2\)/);
  });

  it('un clic sur une section la déplie, un second la replie', () => {
    const entete = () => el().querySelector('form button[type="button"]') as HTMLButtonElement;
    entete().click();
    fixture.detectChanges();
    expect(el().querySelector('input[name="civilite"]')).not.toBeNull();

    entete().click();
    fixture.detectChanges();
    expect(el().querySelector('input[name="civilite"]')).toBeNull();
  });

  it('propose les domaines du catalogue et se souvient de ceux que le candidat coche', () => {
    composant.toggleDomaine('Sécurité');
    expect(composant.domainesExpertise).toEqual(['Sécurité']);
    composant.toggleDomaine('Technique');
    composant.toggleDomaine('Sécurité');
    expect(composant.domainesExpertise).toEqual(['Technique']);
    expect(composant.domainesDisponibles).toEqual(['Sécurité', 'Technique']);
  });

  it('seuls nom, prénom et téléphone sont exigés pour déposer', () => {
    expect(composant.formulaireValide).toBe(false);
    composant.nom = 'Diop';
    composant.prenom = 'Awa';
    expect(composant.formulaireValide).toBe(false);
    composant.telephone = '770000000';
    expect(composant.formulaireValide).toBe(true);
  });

  it('ne dépose rien tant que le formulaire est incomplet', () => {
    composant.nom = 'Diop';
    composant.soumettre();

    expect(postuler).not.toHaveBeenCalled();
  });

  it("n'envoie pas les blocs ajoutés puis laissés vides, mais envoie ceux qui sont remplis", () => {
    composant.nom = 'Diop';
    composant.prenom = 'Awa';
    composant.telephone = '770000000';
    composant.ajouterFormation(); // laissée vide
    composant.ajouterFormation();
    composant.formations[1].etablissement = 'UCAD';
    composant.ajouterExperience(); // laissée vide
    composant.ajouterExperience();
    composant.experiences[1].employeur = 'INPG';
    composant.ajouterLangue(); // laissée vide
    composant.ajouterCertification(); // laissée vide

    composant.soumettre();

    const envoye = postuler.mock.calls[0][0];
    expect(envoye.formations).toHaveLength(1);
    expect(envoye.formations[0].etablissement).toBe('UCAD');
    expect(envoye.experiences).toHaveLength(1);
    expect(envoye.experiences[0].employeur).toBe('INPG');
    expect(envoye.langues).toEqual([]);
    expect(envoye.certifications).toEqual([]);
  });

  it("les champs facultatifs laissés vides partent à null, pas en chaîne vide", () => {
    composant.nom = ' Diop ';
    composant.prenom = 'Awa';
    composant.telephone = '770000000';

    composant.soumettre();

    const envoye = postuler.mock.calls[0][0];
    expect(envoye.nom).toBe('Diop');
    expect(envoye.civilite).toBeNull();
    expect(envoye.nationalite).toBeNull();
    expect(envoye.dateNaissance).toBeNull();
    expect(envoye.diplomePrincipal).toBeNull();
  });
});
