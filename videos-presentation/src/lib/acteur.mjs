// L'« acteur » : ce que les scénarios savent faire devant la caméra. Chaque geste est lent et lisible — curseur qui
// glisse, clic marqué d'une onde, frappe au clavier avec une cadence humaine, défilement progressif — parce qu'un
// spectateur doit pouvoir suivre ce qui se passe, pas seulement voir le résultat.
import { CFG } from './config.mjs';
import { dormir } from './utilitaires.mjs';

const doux = (p) => (p < 0.5 ? 4 * p * p * p : 1 - Math.pow(-2 * p + 2, 3) / 2);

export class Acteur {
  /**
   * @param page    page Playwright
   * @param options { rapide : répétition sans prise (gestes et pauses ramenés au quart) }
   */
  constructor(page, { rapide = false } = {}) {
    this.page = page;
    this.ratio = rapide ? 0.2 : 1;
    this.souris = { x: 920, y: 380 };
    this.graine = 7;
  }

  /** Pseudo-aléa déterministe : la même prise se rejoue à l'identique. */
  alea() {
    this.graine = (this.graine * 16807) % 2147483647;
    return this.graine / 2147483647;
  }

  async pause(ms) {
    await dormir(ms * this.ratio);
  }

  // ---------------------------------------------------------------- navigation

  async aller(chemin) {
    await this.page.goto(CFG.base + chemin, { waitUntil: 'domcontentloaded' });
    await this.patienter();
  }

  /** Attend que la page soit posée : réseau calme, polices chargées, plus de pastille de chargement. */
  async patienter(extra = CFG.pauses.apresNavigation) {
    await this.page.waitForLoadState('networkidle', { timeout: 5000 }).catch(() => {});
    await this.page.evaluate(() => document.fonts && document.fonts.ready).catch(() => {});
    await this.page
      .waitForFunction(() => !document.querySelector('app-ui-spinner, [role="progressbar"], .animate-spin'), null, { timeout: 6000 })
      .catch(() => {});
    await this.pause(extra);
  }

  async url() {
    return new URL(this.page.url()).pathname;
  }

  async attendreUrl(motif, timeout = 15000) {
    await this.page.waitForURL(motif, { timeout });
    await this.patienter(400);
  }

  // ---------------------------------------------------------------- ciblage

  loc(cible) {
    return typeof cible === 'string' ? this.page.locator(cible).first() : cible.first();
  }

  async visible(cible, timeout = 15000) {
    const l = this.loc(cible);
    await l.waitFor({ state: 'visible', timeout });
    return l;
  }

  async existe(cible, timeout = 1500) {
    return this.loc(cible).waitFor({ state: 'visible', timeout }).then(() => true, () => false);
  }

  /** Fait défiler (en douceur) jusqu'à ce que l'élément soit bien visible, et renvoie sa boîte. */
  async boite(cible) {
    const l = await this.visible(cible);
    const aDefile = await l.evaluate(
      (el, ratio) =>
        new Promise((resolve) => {
          const r = el.getBoundingClientRect();
          const vh = window.innerHeight;
          if (r.top >= 70 && r.bottom <= vh - 150) return resolve(false);
          let p = el.parentElement;
          while (p && !(p.scrollHeight > p.clientHeight + 4 && /(auto|scroll)/.test(getComputedStyle(p).overflowY))) p = p.parentElement;
          const cibleDefil = p || document.scrollingElement;
          const delta = r.top - (p ? p.getBoundingClientRect().top : 0) - vh * 0.3;
          const debut = cibleDefil.scrollTop;
          const t0 = performance.now();
          const dur = Math.max(80, Math.min(1100, 400 + Math.abs(delta) * 0.6) * ratio);
          const e = (x) => (x < 0.5 ? 4 * x * x * x : 1 - Math.pow(-2 * x + 2, 3) / 2);
          const tick = (now) => {
            const q = Math.min(1, (now - t0) / dur);
            cibleDefil.scrollTop = debut + delta * e(q);
            q < 1 ? requestAnimationFrame(tick) : resolve(true);
          };
          requestAnimationFrame(tick);
        }),
      this.ratio
    );
    if (aDefile) await this.pause(250);
    const b = await l.boundingBox();
    if (!b) throw new Error('Élément sans boîte : ' + String(cible));
    return b;
  }

  // ---------------------------------------------------------------- curseur

  async deplacerVers(x, y, duree) {
    const { x: x0, y: y0 } = this.souris;
    const dist = Math.hypot(x - x0, y - y0);
    if (dist < 2) return;
    const d = (duree ?? Math.max(450, Math.min(1400, 350 + dist * 0.9))) * this.ratio;
    // Trajectoire en arc léger : un humain ne glisse jamais en ligne droite.
    const nx = -(y - y0) / dist, ny = (x - x0) / dist;
    const bombe = Math.min(70, dist * 0.14) * (this.alea() > 0.5 ? 1 : -1);
    const cx = (x0 + x) / 2 + nx * bombe, cy = (y0 + y) / 2 + ny * bombe;
    const t0 = performance.now();
    for (;;) {
      const p = Math.min(1, (performance.now() - t0) / d);
      const e = doux(p);
      const bx = (1 - e) * (1 - e) * x0 + 2 * (1 - e) * e * cx + e * e * x;
      const by = (1 - e) * (1 - e) * y0 + 2 * (1 - e) * e * cy + e * e * y;
      await this.page.mouse.move(bx, by);
      if (p >= 1) break;
      await dormir(8);
    }
    this.souris = { x, y };
  }

  /** Place le curseur sur l'élément sans cliquer (pour « montrer du doigt »). */
  async pointer(cible, { duree, dx = 0, dy = 0 } = {}) {
    const b = await this.boite(cible);
    await this.deplacerVers(b.x + b.width / 2 + dx, b.y + b.height / 2 + dy, duree);
  }

  async cliquer(cible, { attente = CFG.pauses.apresClic, dx = 0, dy = 0 } = {}) {
    const l = await this.visible(cible);
    let b = await this.boite(cible);
    await this.deplacerVers(b.x + b.width / 2 + dx, b.y + b.height / 2 + dy);
    // La mise en page a pu bouger pendant le trajet (chargement tardif) : on revise la visée.
    const b2 = await l.boundingBox();
    let visee = b;
    if (b2 && (Math.abs(b2.x - b.x) > 3 || Math.abs(b2.y - b.y) > 3)) {
      visee = b2;
      await this.deplacerVers(b2.x + b2.width / 2 + dx, b2.y + b2.height / 2 + dy, 250);
    }
    // Un clic physique atterrit sur ce qui recouvre la cible à cet instant : fenêtre modale en train de se fermer (fondu de
    // sortie), notification, voile de chargement. Le curseur survole alors le bon lien mais rien ne se passe. On attend
    // que la cible soit réellement atteignable avant de cliquer, au lieu de semer des pauses dans chaque scénario.
    const point = [visee.x + visee.width / 2 + dx, visee.y + visee.height / 2 + dy];
    const debut = Date.now();
    while (Date.now() - debut < 5000) {
      const atteignable = await l
        .evaluate((el, [x, y]) => {
          const haut = document.elementFromPoint(x, y);
          return !!haut && (el === haut || el.contains(haut) || haut.contains(el));
        }, point)
        .catch(() => true);
      if (atteignable) break;
      await dormir(150);
    }
    await this.pause(120);
    await this.page.mouse.down();
    await dormir(70 * this.ratio);
    await this.page.mouse.up();
    await this.pause(attente);
  }

  /** Clique sur un texte précis, pratique pour les liens de menu : `a.cliquerTexte('Candidatures')`. */
  async cliquerTexte(texte, options = {}) {
    return this.cliquer(this.page.getByText(texte, { exact: true }), options);
  }

  // ---------------------------------------------------------------- saisie

  async taper(cible, texte, { effacer = true, delai = 55 } = {}) {
    await this.cliquer(cible, { attente: 150 });
    // Un champ date ou heure se saisit par segments selon la langue du navigateur (jj/mm/aaaa, hh:mm AM/PM) : y taper
    // « 1996-05-14 » ou « 10:00 » donne n'importe quoi (« 10:00 -- » : heure refusée, formulaire invalide). On le remplit
    // d'un coup, au format ISO, après avoir montré le geste.
    if (['date', 'time', 'datetime-local', 'month', 'week'].includes(await this.loc(cible).getAttribute('type'))) {
      await this.loc(cible).fill(texte);
      await this.pause(400);
      return;
    }
    if (effacer) {
      await this.page.keyboard.press('Control+A');
      await this.page.keyboard.press('Backspace');
    }
    for (const c of texte) {
      await this.page.keyboard.type(c);
      await dormir((delai * (0.6 + this.alea() * 0.9)) * this.ratio);
    }
    await this.pause(250);
  }

  /** Liste déroulante native : la fenêtre d'options n'est pas capturée, on montre le geste puis on applique le choix. */
  async choisir(cible, option) {
    const b = await this.boite(cible);
    await this.deplacerVers(b.x + b.width / 2, b.y + b.height / 2);
    await this.page.evaluate(([x, y]) => window.__demo.onde(x, y), [b.x + b.width / 2, b.y + b.height / 2]);
    await this.pause(350);
    await this.loc(cible).selectOption(option);
    await this.pause(500);
  }

  async cocher(cible) {
    await this.cliquer(cible);
  }

  async televerser(cible, fichier) {
    await this.loc(cible).setInputFiles(fichier);
    await this.pause(700);
  }

  /**
   * Téléversement par un BOUTON qui ouvre le sélecteur de fichiers du système (le champ <input type=file> est caché et
   * l'application retient quelle pièce on remplace au moment du clic) : on clique, on intercepte le sélecteur, on lui
   * donne le fichier. Poser le fichier directement sur le champ caché ne dirait pas à l'application quelle pièce c'est.
   */
  async televerserViaBouton(cible, fichier) {
    const [selecteur] = await Promise.all([this.page.waitForEvent('filechooser'), this.cliquer(cible)]);
    await selecteur.setFiles(fichier);
    await this.pause(900);
  }

  // ---------------------------------------------------------------- mise en scène

  /** Entoure un élément d'un anneau orange pendant que la voix l'explique. `projecteur` assombrit le reste. */
  async surligner(cible, { duree = 2400, projecteur = false, marge = 6 } = {}) {
    const b = await this.boite(cible);
    await this.page.evaluate(([rect, opts]) => window.__demo.surligner(rect, opts), [b, { projecteur, marge }]);
    await this.pause(duree);
    await this.page.evaluate(() => window.__demo.effacerSurbrillances());
    await this.pause(300);
  }

  /** Défilement progressif de la zone de contenu principale (la plus grande zone défilante sous le centre de l'écran). */
  async defiler(delta, { duree = 1400 } = {}) {
    await this.page.evaluate(
      ([d, dur]) =>
        new Promise((resolve) => {
          let e = document.elementFromPoint(window.innerWidth * 0.6, window.innerHeight * 0.55);
          while (e && !(e.scrollHeight > e.clientHeight + 4 && /(auto|scroll)/.test(getComputedStyle(e).overflowY))) e = e.parentElement;
          const c = e || document.scrollingElement;
          const debut = c.scrollTop;
          const t0 = performance.now();
          const ease = (x) => (x < 0.5 ? 4 * x * x * x : 1 - Math.pow(-2 * x + 2, 3) / 2);
          const tick = (now) => {
            const q = Math.min(1, (now - t0) / dur);
            c.scrollTop = debut + d * ease(q);
            q < 1 ? requestAnimationFrame(tick) : resolve();
          };
          requestAnimationFrame(tick);
        }),
      [delta, Math.max(60, duree * this.ratio)]
    );
    await this.pause(350);
  }

  async enHaut() {
    await this.defiler(-100000, { duree: 900 });
  }

  async carte(options, { duree = 0 } = {}) {
    await this.page.evaluate((o) => window.__demo.afficherCarte(o), options);
    await this.pause(700 + duree);
  }

  async retirerCarte() {
    await this.page.evaluate(() => window.__demo.retirerCarte());
    await this.pause(700);
  }

  /** Puce de chapitre en bas à gauche ; elle se retire seule. */
  async puce(numero, titre, duree = 3200) {
    await this.page.evaluate(
      ([n, t, d]) => {
        window.__demo.afficherPuce(n, t);
        setTimeout(() => window.__demo.masquerPuce(), d);
      },
      [numero, titre, duree]
    );
  }

  // ---------------------------------------------------------------- diagnostic

  async capturer(fichier) {
    await this.page.screenshot({ path: fichier }).catch(() => {});
  }
}
