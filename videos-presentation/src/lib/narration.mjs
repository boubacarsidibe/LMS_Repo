// Voix off : synthèse neuronale (edge-tts), mise en cache, et découpage du texte en sous-titres.
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { CFG } from './config.mjs';
import { exec, dormir, dureeMedia } from './utilitaires.mjs';

/** Texte tel qu'il doit être DIT (sigles épelés), distinct du texte affiché en sous-titres. */
export function parole(texte) {
  let t = texte;
  for (const [motif, remplacement] of CFG.prononciations) t = t.replace(motif, remplacement);
  return t;
}

/**
 * Synthétise `texte` et renvoie { mp3, duree }. Le résultat est mis en cache par empreinte du texte DIT, de la
 * voix et de la vitesse : réécrire une étape ne resynthétise que cette étape.
 */
export async function synthetiser(texte, { voix = CFG.voix, vitesse = CFG.vitesse } = {}) {
  const dossier = path.join(CFG.dossiers.cache, 'tts');
  fs.mkdirSync(dossier, { recursive: true });
  const dit = parole(texte);
  const cle = crypto.createHash('sha1').update(`${voix}|${vitesse}|${dit}`).digest('hex').slice(0, 16);
  const mp3 = path.join(dossier, `${cle}.mp3`);

  if (!fs.existsSync(mp3) || fs.statSync(mp3).size < 1000) {
    for (let essai = 1; ; essai++) {
      try {
        await exec('edge-tts', ['--voice', voix, `--rate=${vitesse}`, '--text', dit, '--write-media', mp3], {
          timeout: 90000,
        });
        if (fs.statSync(mp3).size < 1000) throw new Error('fichier audio vide');
        break;
      } catch (e) {
        if (essai >= 4) throw new Error(`Synthèse vocale impossible pour « ${texte.slice(0, 70)}… » : ${e.message}`);
        await dormir(1500 * essai);
      }
    }
  }
  return { mp3, duree: await dureeMedia(mp3) };
}

/** Découpe en phrases (point, point d'exclamation ou d'interrogation suivi d'une majuscule ou d'un chiffre). */
export function phrases(texte) {
  return texte
    .replace(/\s+/g, ' ')
    .trim()
    .split(/(?<=[.!?…])\s+(?=[A-ZÀÂÉÈÊÎÔÙÛÇ0-9«"'(])/)
    .filter(Boolean);
}

/** Coupe une phrase trop longue en deux lignes d'affichage équilibrées (retour ligne ASS « \N »). */
export function surDeuxLignes(texte, max = 84) {
  if (texte.length <= max) return texte;
  const milieu = texte.length / 2;
  let meilleur = -1;
  for (let i = 0; i < texte.length; i++) {
    if (texte[i] === ' ' && (meilleur < 0 || Math.abs(i - milieu) < Math.abs(meilleur - milieu))) meilleur = i;
  }
  return meilleur < 0 ? texte : texte.slice(0, meilleur) + '\\N' + texte.slice(meilleur + 1);
}

/**
 * Sous-titres d'une étape : une entrée par phrase, minutée au prorata du nombre de caractères sur la durée
 * de la voix. Les phrases trop longues pour deux lignes sont coupées en deux entrées, à une virgule de préférence.
 * @returns {{debut:number, fin:number, texte:string}[]} instants relatifs au début de la voix, en secondes
 */
export function sousTitres(texte, dureeVoix) {
  const brutes = phrases(texte);
  const morceaux = [];
  for (const p of brutes) {
    if (p.length <= 150) {
      morceaux.push(p);
      continue;
    }
    // Trop long : coupe à la virgule / point-virgule la plus proche du milieu.
    const milieu = p.length / 2;
    let coupe = -1;
    for (const m of p.matchAll(/[,;:]\s/g)) {
      if (coupe < 0 || Math.abs(m.index - milieu) < Math.abs(coupe - milieu)) coupe = m.index;
    }
    if (coupe > 20 && coupe < p.length - 20) {
      morceaux.push(p.slice(0, coupe + 1).trim(), p.slice(coupe + 2).trim());
    } else {
      morceaux.push(p);
    }
  }
  // La synthèse laisse ~0,1 s de silence au début et ~0,25 s à la fin.
  const debutVoix = 0.1;
  const finVoix = Math.max(debutVoix + 0.5, dureeVoix - 0.15);
  const poids = morceaux.map((m) => m.length + 8);
  const total = poids.reduce((a, b) => a + b, 0);
  let t = debutVoix;
  return morceaux.map((m, i) => {
    const d = ((finVoix - debutVoix) * poids[i]) / total;
    const entree = { debut: t, fin: t + d, texte: surDeuxLignes(m) };
    t += d;
    return entree;
  });
}
