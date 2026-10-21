import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { Router } from '@angular/router';

import { FormateurLayout } from './formateur-layout';
import { NotificationCloche } from '../../../../shared/services/cloche-notifications.service';

/**
 * La cloche de notifications restait auparavant un menu 100 % statique
 * (« Rien à signaler », codé en dur) : un formateur qui recevait un message
 * privé d'un étudiant n'en était jamais informé avant d'ouvrir « Messagerie »
 * lui-même, alors que le backend crée bien une notification à chaque envoi
 * (MessagerieController.notifierNouveauMessage). Ces tests couvrent le
 * branchement sur le même service que l'espace pédagogique.
 */
describe('FormateurLayout', () => {
  let component: FormateurLayout;
  let fixture: ComponentFixture<FormateurLayout>;
  let httpMock: HttpTestingController;

  const notif = (id: number, lu = false): NotificationCloche => ({
    id, message: `Message privé #${id}`, type: 'MESSAGE_PRIVE', lu, dateCreation: '2026-09-28T10:00:00', lien: `/espace-etudiant/formateur/messagerie/${id}`,
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FormateurLayout, RouterTestingModule],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    })
    .compileComponents();

    fixture = TestBed.createComponent(FormateurLayout);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    // Les appels non liés à la cloche (étudiants, cours, supports) ne sont
    // volontairement ni attendus ni vidés ici — httpMock.verify() les ferait
    // échouer sans rapport avec ce qui est testé.
    httpMock.match(() => true).forEach((req) => req.flush([]));
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('charge les notifications non lues au démarrage et affiche le compteur', () => {
    const requeteCloche = httpMock.expectOne('/api/espace-etudiant/notifications');
    requeteCloche.flush([notif(1), notif(2, true)]);

    expect(component.notifications.length).toBe(1);
    expect(component.notifications[0].id).toBe(1);
  });

  it("ouvrir une notification la marque lue et redirige vers la conversation", () => {
    httpMock.expectOne('/api/espace-etudiant/notifications').flush([notif(5)]);
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigateByUrl');

    component.ouvrirNotification(component.notifications[0]);

    httpMock.expectOne('/api/espace-etudiant/notifications/5/lu').flush(notif(5, true));

    expect(component.notifications.length).toBe(0);
    expect(navigateSpy).toHaveBeenCalledWith('/espace-etudiant/formateur/messagerie/5');
  });
});
