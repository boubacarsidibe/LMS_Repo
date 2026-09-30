import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { Router } from '@angular/router';

import { EtudiantLayout } from './etudiant-layout';
import { NotificationCloche } from '../../../../shared/services/cloche-notifications.service';

/**
 * La seule « cloche » de cet en-tête menait aux annonces (nbAnnoncesNonLues,
 * CommunicationService) — sans rapport avec la table `notification` : un
 * étudiant qui recevait un message privé d'un formateur n'en était jamais
 * informé avant d'ouvrir « Messagerie » lui-même, alors que le backend crée
 * bien une notification à chaque envoi (MessagerieController.notifierNouveauMessage).
 * Ces tests couvrent la cloche de notifications ajoutée à côté, sur le même
 * patron que l'espace pédagogique — le lien annonces reste inchangé (seule
 * son icône, jusque-là identique à celle des notifications, a changé).
 */
describe('EtudiantLayout', () => {
  let component: EtudiantLayout;
  let fixture: ComponentFixture<EtudiantLayout>;
  let httpMock: HttpTestingController;

  const notif = (id: number, lu = false): NotificationCloche => ({
    id, message: `Message privé #${id}`, type: 'MESSAGE_PRIVE', lu, dateCreation: '2026-09-28T10:00:00', lien: `/espace-etudiant/messagerie/${id}`,
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EtudiantLayout, RouterTestingModule],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EtudiantLayout);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    // Profil, promotion, annonces non lues : sans rapport avec ce qui est testé ici. []
    // convient à chacun (les gestionnaires ne font que de l'accès optionnel sur la réponse).
    httpMock.match(() => true).forEach((req) => req.flush([]));
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('charge les notifications non lues au démarrage et affiche le compteur', () => {
    const requeteCloche = httpMock.expectOne('/api/espace-etudiant/notifications');
    requeteCloche.flush([notif(1), notif(2, true), notif(3)]);

    expect(component.notifications.length).toBe(2);
  });

  it("ouvrir une notification la marque lue, ferme le menu et redirige", () => {
    httpMock.expectOne('/api/espace-etudiant/notifications').flush([notif(7)]);
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigateByUrl');
    component.notificationOpen = true;

    component.ouvrirNotification(component.notifications[0]);

    httpMock.expectOne('/api/espace-etudiant/notifications/7/lu').flush(notif(7, true));

    expect(component.notifications.length).toBe(0);
    expect(component.notificationOpen).toBe(false);
    expect(navigateSpy).toHaveBeenCalledWith('/espace-etudiant/messagerie/7');
  });

  it("un clic en dehors de la cloche referme le menu de notifications", () => {
    httpMock.expectOne('/api/espace-etudiant/notifications').flush([]);
    component.notificationOpen = true;

    component.onDocumentClick({ target: document.body } as unknown as MouseEvent);

    expect(component.notificationOpen).toBe(false);
  });
});
