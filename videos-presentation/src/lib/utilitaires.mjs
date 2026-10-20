import { execFile } from 'node:child_process';
import { promisify } from 'node:util';

export const exec = promisify(execFile);
export const dormir = (ms) => new Promise((r) => setTimeout(r, ms));
/** Horloge commune à la vidéo et à l'audio : secondes depuis l'epoch, flottant. */
export const maintenant = () => Date.now() / 1000;

export async function dureeMedia(fichier) {
  const { stdout } = await exec('ffprobe', [
    '-v', 'error', '-show_entries', 'format=duration', '-of', 'default=nw=1:nk=1', fichier,
  ]);
  return parseFloat(stdout);
}

/** Exécute des tâches asynchrones avec au plus `n` en parallèle. */
export async function enParallele(elements, n, tache) {
  const resultats = new Array(elements.length);
  let suivant = 0;
  async function ouvrier() {
    while (suivant < elements.length) {
      const i = suivant++;
      resultats[i] = await tache(elements[i], i);
    }
  }
  await Promise.all(Array.from({ length: Math.min(n, elements.length) }, ouvrier));
  return resultats;
}

export const horodatage = (s) => {
  const h = Math.floor(s / 3600), m = Math.floor((s % 3600) / 60), sec = s % 60;
  return `${h ? h + ':' : ''}${String(m).padStart(2, '0')}:${sec.toFixed(0).padStart(2, '0')}`;
};
