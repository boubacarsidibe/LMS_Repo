import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NgForm } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';

import { ContactComponent } from './contact.component';
import { RecrutementService } from '../../../../core/recrutement/services/recrutement.service';

/**
 * Avant cette story, un champ requis vide n'affichait rien au niveau du
 * formulaire (seule la bulle native du navigateur bloquait, sans dire lequel
 * des six champs posait problème) — relevé lors d'un audit externe.
 */
describe('ContactComponent', () => {
  let component: ContactComponent;
  let fixture: ComponentFixture<ContactComponent>;
  let recrutementService: RecrutementService;

  function ngForm(): NgForm {
    return fixture.debugElement.query(By.css('form')).injector.get(NgForm);
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ContactComponent, RouterTestingModule],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(ContactComponent);
    component = fixture.componentInstance;
    recrutementService = TestBed.inject(RecrutementService);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it("n'appelle pas le service tant que les champs requis sont vides", () => {
    const spy = vi.spyOn(recrutementService, 'poserQuestion');

    component.submitForm(ngForm());

    expect(component.envoiTente).toBe(true);
    expect(spy).not.toHaveBeenCalled();
  });

  it("champEnErreur ignore un champ invalide tant que ni touché ni tentative d'envoi", () => {
    const form = ngForm();

    expect(form.controls['email']?.invalid).toBe(true);
    expect(component.champEnErreur(form, 'email')).toBe(false);
  });

  it("champEnErreur signale un champ invalide après une tentative d'envoi", () => {
    const form = ngForm();

    component.submitForm(form);

    expect(component.champEnErreur(form, 'email')).toBe(true);
  });

  it('envoie le message composé une fois tous les champs requis valides', async () => {
    const spy = vi.spyOn(recrutementService, 'poserQuestion').mockReturnValue(of({}));
    component.fullName = 'Jean Dupont';
    component.email = 'jean.dupont@exemple.com';
    component.subject = 'admission';
    component.message = 'Bonjour, une question.';
    fixture.detectChanges();
    await fixture.whenStable();

    component.submitForm(ngForm());

    expect(spy).toHaveBeenCalledWith('jean.dupont@exemple.com', expect.stringContaining('Bonjour, une question.'));
    // La réponse simulée (of({})) émet de façon synchrone : au retour de l'appel,
    // le cycle succès est déjà passé par sending=true puis sent=true.
    expect(component.sent).toBe(true);
    expect(component.sending).toBe(false);
  });
});
