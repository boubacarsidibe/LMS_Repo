import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { UiEmptyStateComponent } from '../../../../shared/components/ui/ui-empty-state.component';
import { UiSpinnerComponent } from '../../../../shared/components/ui/ui-spinner.component';
import { CoursEtudiant, ForumSujet } from '../../models/scolarite.models';
import { CommunicationService } from '../../services/communication.service';
import { ScolariteService } from '../../services/scolarite.service';

/** Un thème de discussion et les sujets qui s'y rangent. */
interface BlocTheme {
  cle: string;
  titre: string;
  icone: string;
  /** Pour préremplir le formulaire « Nouveau sujet dans ce thème ». */
  rattachement: string;
  sujets: ForumSujet[];
  nbNonLus: number;
  nbMessages: number;
  derniereActivite: string;
  dernierSujet: string;
}

/**
 * Forum de discussion (TDR §3.2), branché sur le backend.
 *
 * <p>L'étudiant ne peut ouvrir un sujet que dans un cours de son cursus, dans sa
 * promotion, ou dans le salon général.
 *
 * <p>Un thème (un cours, la promotion, le salon général) = un bloc. Les sujets d'un
 * même thème se rangent dans son bloc au lieu d'en créer un nouveau, et une pastille
 * numérotée, façon WhatsApp, y compte les messages non lus.
 */
@Component({
  selector: 'app-forum-etudiant',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, UiSpinnerComponent, UiEmptyStateComponent],
  template: `
    <div class="p-margin-desktop space-y-stack-lg max-w-container-max mx-auto">
      <header class="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 class="font-display text-display text-on-surface leading-tight">Forum</h1>
          <p class="font-body-md text-body-md text-on-surface-variant mt-1">
            Échangez avec votre promotion et vos formateurs.
          </p>
        </div>
        <button
          type="button"
          (click)="formulaireOuvert = !formulaireOuvert"
          class="px-4 py-2 rounded-lg bg-secondary text-on-secondary font-label-md text-label-md hover:opacity-90 transition-opacity"
        >
          {{ formulaireOuvert ? 'Annuler' : 'Nouveau sujet' }}
        </button>
      </header>

      <form
        *ngIf="formulaireOuvert"
        (ngSubmit)="ouvrirSujet()"
        class="bg-surface-container-lowest rounded-xl border border-outline-variant p-stack-lg space-y-3 shadow-sm"
      >
        <div>
          <label for="titre" class="font-label-md text-label-md text-on-surface">Titre</label>
          <input
            id="titre"
            [(ngModel)]="nouveauTitre"
            name="titre"
            required
            placeholder="Votre question en une phrase"
            class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors"
          />
        </div>
        <div>
          <label for="cours" class="font-label-md text-label-md text-on-surface">Thème</label>
          <select
            id="cours"
            [ngModel]="rattachementValeur()"
            (ngModelChange)="surRattachementChange($event)"
            name="cours"
            class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors"
          >
            <option value="general">Salon général</option>
            <option value="promotion">Ma promotion</option>
            <option *ngFor="let c of cours" [value]="'cours:' + c.id">{{ c.titre }}</option>
          </select>
          <p class="mt-1 font-body-sm text-body-sm text-on-surface-variant">
            Votre sujet est rangé dans le bloc de ce thème, sans en créer un nouveau.
          </p>
        </div>
        <div>
          <label for="message" class="font-label-md text-label-md text-on-surface">Message</label>
          <textarea
            id="message"
            [(ngModel)]="nouveauMessage"
            name="message"
            rows="4"
            placeholder="Détaillez votre question…"
            class="mt-1 w-full p-3 rounded-lg bg-surface-container-low border border-outline-variant font-body-sm text-body-sm text-on-surface outline-none focus:border-secondary transition-colors resize-y"
          ></textarea>
        </div>
        <p *ngIf="erreurCreation" class="font-body-sm text-body-sm text-error">
          {{ erreurCreation }}
        </p>
        <button
          type="submit"
          [disabled]="!nouveauTitre.trim() || creationEnCours"
          class="px-4 py-2 rounded-lg bg-secondary text-on-secondary font-label-md text-label-md hover:opacity-90 transition-opacity disabled:opacity-50"
        >
          {{ creationEnCours ? 'Publication…' : 'Publier le sujet' }}
        </button>
      </form>

      <app-ui-spinner *ngIf="chargement" message="Chargement du forum…"></app-ui-spinner>

      <app-ui-empty-state
        *ngIf="!chargement && sujets.length === 0"
        icone="forum"
        titre="Aucun sujet ouvert"
        description="Soyez le premier à lancer une discussion."
        actionLibelle="Ouvrir un sujet"
        (action)="formulaireOuvert = true"
      ></app-ui-empty-state>

      <ul *ngIf="!chargement && blocs.length > 0" class="space-y-base">
        <li
          *ngFor="let b of blocs; trackBy: suivreBloc"
          class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-sm"
        >
          <button
            type="button"
            (click)="basculer(b)"
            [attr.aria-expanded]="estOuvert(b)"
            class="w-full flex items-start gap-3 p-stack-lg text-left rounded-xl hover:bg-surface-container-low transition-colors"
          >
            <span
              class="shrink-0 w-10 h-10 rounded-full bg-primary-container flex items-center justify-center"
              aria-hidden="true"
            >
              <span class="material-symbols-outlined text-on-primary-container">{{ b.icone }}</span>
            </span>
            <span class="min-w-0 flex-1">
              <span class="flex items-baseline justify-between gap-3">
                <span class="font-label-md text-label-md text-on-surface truncate">{{ b.titre }}</span>
                <span
                  class="font-label-sm text-label-sm shrink-0"
                  [ngClass]="b.nbNonLus > 0 ? 'text-secondary' : 'text-on-surface-variant'"
                  >{{ b.derniereActivite | date: 'd MMM, HH:mm' }}</span
                >
              </span>
              <span class="flex items-end justify-between gap-3 mt-1">
                <span class="min-w-0">
                  <span class="block font-body-sm text-body-sm text-on-surface-variant truncate"
                    >Dernier échange : « {{ b.dernierSujet }} »</span
                  >
                  <span class="block font-label-sm text-label-sm text-on-surface-variant mt-0.5"
                    >{{ b.sujets.length }} sujet(s) · {{ b.nbMessages }} message(s)</span
                  >
                </span>
                <span class="flex items-center gap-2 shrink-0">
                  <span
                    *ngIf="b.nbNonLus > 0"
                    class="inline-flex items-center justify-center min-w-[24px] h-6 px-2 rounded-full bg-secondary text-on-secondary font-label-sm text-label-sm"
                    [attr.aria-label]="b.nbNonLus + ' message(s) non lu(s)'"
                    >{{ b.nbNonLus > 99 ? '99+' : b.nbNonLus }}</span
                  >
                  <span class="material-symbols-outlined text-on-surface-variant" aria-hidden="true">{{
                    estOuvert(b) ? 'expand_less' : 'expand_more'
                  }}</span>
                </span>
              </span>
            </span>
          </button>

          <div *ngIf="estOuvert(b)" class="border-t border-outline-variant">
            <ul class="divide-y divide-outline-variant">
              <li *ngFor="let s of b.sujets">
                <a
                  [routerLink]="['/espace-etudiant/forum', s.id]"
                  class="flex items-center justify-between gap-3 px-stack-lg py-3 hover:bg-surface-container-low transition-colors"
                >
                  <span class="min-w-0">
                    <span class="flex flex-wrap items-center gap-2">
                      <span
                        *ngIf="s.epingle"
                        class="material-symbols-outlined text-[16px] text-secondary"
                        title="Sujet épinglé"
                        >push_pin</span
                      >
                      <span
                        class="font-label-md text-label-md text-on-surface"
                        [ngClass]="{ 'font-semibold': s.nbNonLus > 0 }"
                        >{{ s.titre }}</span
                      >
                      <span
                        *ngIf="s.clos"
                        class="px-2 py-0.5 rounded-full bg-surface-container text-on-surface-variant font-label-sm text-label-sm"
                        >Clos</span
                      >
                    </span>
                    <span class="block font-body-sm text-body-sm text-on-surface-variant mt-1">
                      Ouvert par {{ s.auteur }} · {{ s.dateCreation | date: 'd MMM y' }} ·
                      {{ s.nbMessages }} message(s)
                      <span *ngIf="s.nbMessages > 0">
                        · dernier {{ s.dateDernierMessage | date: 'd MMM, HH:mm' }}</span
                      >
                    </span>
                  </span>
                  <span
                    *ngIf="s.nbNonLus > 0"
                    class="inline-flex items-center justify-center min-w-[24px] h-6 px-2 rounded-full bg-secondary text-on-secondary font-label-sm text-label-sm shrink-0"
                    [attr.aria-label]="s.nbNonLus + ' message(s) non lu(s)'"
                    >{{ s.nbNonLus > 99 ? '99+' : s.nbNonLus }}</span
                  >
                </a>
              </li>
            </ul>
            <div class="px-stack-lg py-3 border-t border-outline-variant">
              <button
                type="button"
                (click)="nouveauSujetDans(b)"
                class="inline-flex items-center gap-1 font-label-md text-label-md text-secondary hover:underline"
              >
                <span class="material-symbols-outlined text-[18px]" aria-hidden="true">add</span>
                Nouveau sujet dans ce thème
              </button>
            </div>
          </div>
        </li>
      </ul>
    </div>
  `,
})
export class ForumEtudiant implements OnInit {
  sujets: ForumSujet[] = [];
  blocs: BlocTheme[] = [];
  /** Blocs dépliés. Les blocs qui ont des non lus le sont d'emblée. */
  private ouverts = new Set<string>();
  cours: CoursEtudiant[] = [];
  chargement = true;

  formulaireOuvert = false;
  nouveauTitre = '';
  nouveauCoursId: number | null = null;
  nouveauPourMaPromotion = false;
  nouveauMessage = '';
  creationEnCours = false;
  erreurCreation = '';

  constructor(
    private communication: CommunicationService,
    private scolarite: ScolariteService
  ) {}

  ngOnInit(): void {
    forkJoin({
      sujets: this.communication.getSujets(),
      cours: this.scolarite.getMesCours(),
    }).subscribe({
      next: ({ sujets, cours }) => {
        this.sujets = sujets;
        this.cours = cours;
        this.construireBlocs();
        this.ouverts = new Set(this.blocs.filter((b) => b.nbNonLus > 0).map((b) => b.cle));
        this.chargement = false;
      },
      error: () => (this.chargement = false),
    });
  }

  /** Un sujet appartient à un seul thème : son cours, sa promotion, ou le salon général. */
  private cleTheme(s: ForumSujet): string {
    if (s.coursId != null) {
      return 'cours:' + s.coursId;
    }
    return s.promotionId != null ? 'promotion:' + s.promotionId : 'general';
  }

  /** Regroupe les sujets par thème : un thème = un bloc, quel que soit le nombre de sujets. */
  private construireBlocs(): void {
    const parTheme = new Map<string, ForumSujet[]>();
    for (const s of this.sujets) {
      const cle = this.cleTheme(s);
      parTheme.set(cle, [...(parTheme.get(cle) ?? []), s]);
    }
    const blocs = [...parTheme.entries()].map(([cle, liste]): BlocTheme => {
      // Épinglés d'abord, puis le plus récemment actif.
      const sujets = [...liste].sort(
        (a, b) =>
          Number(b.epingle) - Number(a.epingle) ||
          b.dateDernierMessage.localeCompare(a.dateDernierMessage)
      );
      const recent = [...liste].sort((a, b) => b.dateDernierMessage.localeCompare(a.dateDernierMessage))[0];
      const modele = liste[0];
      const estCours = modele.coursId != null;
      const estPromotion = !estCours && modele.promotionId != null;
      return {
        cle,
        titre: estCours
          ? (modele.coursTitre ?? 'Cours')
          : estPromotion
            ? 'Promotion ' + (modele.promotionLibelle ?? '')
            : 'Salon général',
        icone: estCours ? 'menu_book' : estPromotion ? 'groups' : 'forum',
        rattachement: estCours ? 'cours:' + modele.coursId : estPromotion ? 'promotion' : 'general',
        sujets,
        nbNonLus: liste.reduce((somme, s) => somme + (s.nbNonLus ?? 0), 0),
        nbMessages: liste.reduce((somme, s) => somme + s.nbMessages, 0),
        derniereActivite: recent.dateDernierMessage,
        dernierSujet: recent.titre,
      };
    });
    // Comme une messagerie : le thème le plus récemment actif en tête.
    this.blocs = blocs.sort((a, b) => b.derniereActivite.localeCompare(a.derniereActivite));
  }

  suivreBloc(_: number, bloc: BlocTheme): string {
    return bloc.cle;
  }

  estOuvert(bloc: BlocTheme): boolean {
    return this.ouverts.has(bloc.cle);
  }

  basculer(bloc: BlocTheme): void {
    if (!this.ouverts.delete(bloc.cle)) {
      this.ouverts.add(bloc.cle);
    }
  }

  /** Ouvre le formulaire avec le thème du bloc déjà choisi. */
  nouveauSujetDans(bloc: BlocTheme): void {
    this.surRattachementChange(bloc.rattachement);
    this.formulaireOuvert = true;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  /** Traduit la sélection du menu en `coursId` / `pourMaPromotion`, mutuellement exclusifs. */
  surRattachementChange(valeur: string): void {
    if (valeur === 'promotion') {
      this.nouveauCoursId = null;
      this.nouveauPourMaPromotion = true;
    } else if (valeur.startsWith('cours:')) {
      this.nouveauCoursId = Number(valeur.slice('cours:'.length));
      this.nouveauPourMaPromotion = false;
    } else {
      this.nouveauCoursId = null;
      this.nouveauPourMaPromotion = false;
    }
  }

  rattachementValeur(): string {
    if (this.nouveauPourMaPromotion) {
      return 'promotion';
    }
    return this.nouveauCoursId != null ? 'cours:' + this.nouveauCoursId : 'general';
  }

  ouvrirSujet(): void {
    if (!this.nouveauTitre.trim()) {
      return;
    }
    this.creationEnCours = true;
    this.erreurCreation = '';
    this.communication
      .ouvrirSujet({
        titre: this.nouveauTitre.trim(),
        coursId: this.nouveauCoursId,
        pourMaPromotion: this.nouveauPourMaPromotion,
        premierMessage: this.nouveauMessage.trim(),
      })
      .subscribe({
        next: (sujet) => {
          this.sujets = [sujet, ...this.sujets];
          this.construireBlocs();
          // Le sujet rejoint le bloc de son thème : on le déplie pour qu'on le voie.
          this.ouverts.add(this.cleTheme(sujet));
          this.creationEnCours = false;
          this.formulaireOuvert = false;
          this.nouveauTitre = '';
          this.nouveauMessage = '';
          this.nouveauCoursId = null;
          this.nouveauPourMaPromotion = false;
        },
        error: (e) => {
          this.creationEnCours = false;
          this.erreurCreation = e?.error?.message || "L'ouverture du sujet a échoué.";
        },
      });
  }
}
