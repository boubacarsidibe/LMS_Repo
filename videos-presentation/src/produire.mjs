// Produit une vidéo à partir d'un scénario.
//
//   ./lancer.sh node src/produire.mjs admission              prise complète → sortie/admission/admission.mp4
//   ./lancer.sh node src/produire.mjs admission --essai      répétition rapide, sans voix ni image : vérifie le
//                                                            scénario et liste les erreurs de l'application
//
// Un scénario (src/scenarios/<id>.mjs) est une liste d'étapes { chapitre?, dire, faire? } : la voix dit `dire`
// pendant que `faire` exécute les gestes à l'écran. La durée d'une étape est celle de la voix, ou celle des gestes
// si elle est plus longue ; les sous-titres, la voix et les chapitres sont minutés sur les instants RÉELS de la prise.
import fs from 'node:fs';
import path from 'node:path';
import { chromium } from '/opt/pw/node_modules/playwright/index.mjs';
import { CFG } from './lib/config.mjs';
import { installer } from './lib/surcouche.mjs';
import { Acteur } from './lib/acteur.mjs';
import { Enregistreur } from './lib/enregistreur.mjs';
import { synthetiser, sousTitres, parole } from './lib/narration.mjs';
import { monter } from './lib/montage.mjs';
import { dormir, enParallele, horodatage } from './lib/utilitaires.mjs';

const [, , id, ...drapeaux] = process.argv;
if (!id) {
  console.error('Usage : node src/produire.mjs <scenario> [--essai [--captures]] [--sans-sous-titres]');
  process.exit(2);
}
const essai = drapeaux.includes('--essai');
// --captures : pendant une répétition, une image par étape (sortie/<id>/essai/NN.png) pour voir ce qui est réellement à l'écran.
const captures = essai && drapeaux.includes('--captures');
const sansSousTitres = drapeaux.includes('--sans-sous-titres');

const scenario = (await import(`./scenarios/${id}.mjs`)).default;
const dossierSortie = path.join(CFG.dossiers.sortie, id);
fs.mkdirSync(dossierSortie, { recursive: true });
if (captures) fs.mkdirSync(path.join(dossierSortie, 'essai'), { recursive: true });

// ------------------------------------------------------------------ 1. voix off (avant toute prise)
const textes = [];
if (scenario.ouverture?.dire) textes.push(scenario.ouverture.dire);
for (const e of scenario.etapes) textes.push(e.dire);
if (scenario.bilan?.dire) textes.push(scenario.bilan.dire);

const voix = new Map();
if (!essai) {
  const uniques = [...new Set(textes)];
  console.log(`Voix off : ${uniques.length} passage(s) à synthétiser (le cache évite de refaire les identiques)…`);
  const t = Date.now();
  await enParallele(uniques, 3, async (texte) => voix.set(texte, await synthetiser(texte)));
  const duree = [...voix.values()].reduce((s, v) => s + v.duree, 0);
  console.log(`  → ${(duree / 60).toFixed(1)} min de voix (${((Date.now() - t) / 1000).toFixed(0)} s)`);
}

// ------------------------------------------------------------------ 2. navigateur
// L'échelle (1,5) est imposée au NAVIGATEUR (--force-device-scale-factor) et non émulée par le protocole CDP
// (option deviceScaleFactor de Playwright) : avec l'émulation, le flux de capture livre des images de 1280×720
// seulement, agrandies ensuite — texte flou. Avec l'option navigateur, il livre les 1920×1080 pixels réels.
const navigateur = await chromium.launch({
  args: [
    '--hide-scrollbars',
    // Langue de l'interface du navigateur : sans elle, les champs date affichent « mm/dd/yyyy » (anglais) et les listes
    // natives, les menus du navigateur, restent en anglais dans une vidéo entièrement en français.
    '--lang=fr-FR',
    '--force-color-profile=srgb',
    '--font-render-hinting=none',
    `--force-device-scale-factor=${CFG.echelle}`,
    `--window-size=${CFG.fenetre.width},${CFG.fenetre.height}`,
  ],
});
const contexte = await navigateur.newContext({
  viewport: null,
  locale: 'fr-FR',
  timezoneId: 'Africa/Dakar',
  colorScheme: 'light',
  acceptDownloads: true,
});
await contexte.addInitScript(installer);
const page = await contexte.newPage();
const acteur = new Acteur(page, { rapide: essai });

// Diagnostic : tout ce que l'application signale pendant la prise (erreurs de console, requêtes en échec).
const diagnostic = [];
let etapeCourante = 'préparation';
const noter = (genre, message) => diagnostic.push({ etape: etapeCourante, genre, message: String(message).slice(0, 300) });
page.on('console', (m) => m.type() === 'error' && noter('console', m.text()));
page.on('pageerror', (e) => noter('exception', e));
page.on('response', (r) => {
  if (r.status() >= 400 && r.url().includes('/api/')) noter(`HTTP ${r.status()}`, `${r.request().method()} ${r.url().replace(CFG.base, '')}`);
});
page.on('requestfailed', (r) => !/\/api\/.*notifications/.test(r.url()) && noter('requête échouée', `${r.url().replace(CFG.base, '')} — ${r.failure()?.errorText}`));

// ------------------------------------------------------------------ 3. prise
const segments = [];
const echecs = [];
let enregistreur = null;
const MARGE_VOIX = 0.25; // secondes de silence avant que la voix ne démarre dans une étape

async function etape(e, numero, chapitreNum) {
  etapeCourante = e.chapitre ?? e.dire.slice(0, 40);
  const t0 = enregistreur ? enregistreur.horloge() : 0;
  if (e.chapitre && !e.sansPuce) await acteur.puce(chapitreNum, e.chapitre);
  const v = voix.get(e.dire);

  const geste = (async () => {
    if (!e.faire) return;
    await acteur.pause(e.retard ?? CFG.pauses.avantAction);
    await e.faire(acteur);
  })();
  const attenteVoix = essai || !v ? Promise.resolve() : dormir((v.duree + MARGE_VOIX) * 1000);

  try {
    await Promise.all([geste, attenteVoix]);
  } catch (erreur) {
    echecs.push({ etape: etapeCourante, message: erreur.message });
    await acteur.capturer(path.join(dossierSortie, `erreur-${String(numero).padStart(2, '0')}.png`));
    console.error(`  ✗ Étape « ${etapeCourante} » : ${erreur.message.split('\n')[0]}`);
    if (!essai) throw erreur;
  }
  await acteur.pause(e.pause ?? CFG.pauses.entreEtapes);
  if (captures) await acteur.capturer(path.join(dossierSortie, 'essai', `${String(numero).padStart(2, '0')}.png`));

  if (v) {
    segments.push({
      debutEtape: t0,
      debutVoix: t0 + MARGE_VOIX,
      texte: e.dire,
      dit: parole(e.dire),
      mp3: v.mp3,
      cues: sousTitres(e.dire, v.duree),
      chapitre: e.chapitre,
    });
  }
}

try {
  // Page de départ chargée AVANT la prise : la première image de la vidéo est directement la carte de titre.
  await acteur.aller(scenario.depart ?? '/auth/login');
  if (scenario.ouverture) {
    await page.evaluate((o) => window.__demo.afficherCarte(o), scenario.ouverture);
    await dormir(essai ? 100 : 1000);
  }
  if (!essai) {
    enregistreur = new Enregistreur(page, path.join(CFG.dossiers.cache, 'images', id), {
      largeur: CFG.fenetre.width * CFG.echelle,
      hauteur: CFG.fenetre.height * CFG.echelle,
    });
    await enregistreur.demarrer();
    console.log('Prise en cours…');
  }

  let numero = 0;
  if (scenario.ouverture?.dire) {
    await etape({ dire: scenario.ouverture.dire, chapitre: 'Présentation', sansPuce: true, pause: 300 }, numero++, 0);
    await acteur.retirerCarte();
  }
  let chapitreNum = 0;
  for (const e of scenario.etapes) {
    if (e.chapitre) chapitreNum++;
    await etape(e, numero++, chapitreNum);
  }
  if (scenario.bilan) {
    await acteur.carte(
      { pastille: scenario.bilan.pastille ?? 'Récapitulatif', titre: scenario.bilan.titre, puces: scenario.bilan.puces },
      {}
    );
    await etape({ dire: scenario.bilan.dire, chapitre: 'Récapitulatif', sansPuce: true, pause: 1800 }, numero++, chapitreNum + 1);
  }
} finally {
  if (enregistreur) {
    const r = await enregistreur.arreter();
    console.log(`Prise terminée : ${horodatage(r.duree)} — ${r.images} images, ${r.dimensions?.largeur}×${r.dimensions?.hauteur} px`);
  }
  await navigateur.close();
}

// ------------------------------------------------------------------ 4. diagnostic de l'application
const significatifs = diagnostic.filter((d) => !/favicon|ERR_ABORTED|Failed to load resource.*(401|404)/i.test(d.message));
fs.writeFileSync(path.join(dossierSortie, `${id}.diagnostic.json`), JSON.stringify({ echecs, diagnostic }, null, 2));
if (significatifs.length) {
  console.log(`\n⚠ ${significatifs.length} signalement(s) de l'application pendant la prise :`);
  const vus = new Set();
  for (const d of significatifs) {
    const cle = `${d.genre}|${d.message}`;
    if (vus.has(cle)) continue;
    vus.add(cle);
    console.log(`  [${d.etape}] ${d.genre} — ${d.message}`);
  }
}
if (echecs.length) {
  console.log(`\n✗ ${echecs.length} étape(s) en échec :`);
  echecs.forEach((e) => console.log(`  - ${e.etape} : ${e.message.split('\n')[0]}`));
  if (essai) process.exit(1);
}
if (essai && !echecs.length) console.log(`✓ Répétition réussie : ${scenario.etapes.length} étape(s) jouées sans échec.`);

// ------------------------------------------------------------------ 5. montage
if (!essai && enregistreur) {
  console.log('Montage (encodage H.264, voix, sous-titres, chapitres)…');
  const t = Date.now();
  const r = await monter({ id, titre: scenario.titre, segments, enregistreur, dossier: dossierSortie, sansSousTitres });
  console.log(`✓ ${r.sortie}`);
  console.log(`  durée ${horodatage(r.duree)} · ${r.tailleMo.toFixed(1)} Mo · ${r.chapitres.length} chapitre(s) · montage ${((Date.now() - t) / 1000).toFixed(0)} s`);
}
