// Montage : à partir des images capturées, de la voix off et des instants de chaque étape, produit la vidéo finale
// (MP4 H.264 + AAC, sous-titres incrustés, chapitres), plus les sous-titres en fichier séparé.
import fs from 'node:fs';
import path from 'node:path';
import { CFG } from './config.mjs';
import { exec, dureeMedia, horodatage } from './utilitaires.mjs';

const RATE_PCM = 24000; // edge-tts sort du 24 kHz mono

const ass = (s) => {
  const cs = Math.round(s * 100);
  const h = Math.floor(cs / 360000), m = Math.floor((cs % 360000) / 6000), sec = Math.floor((cs % 6000) / 100), c = cs % 100;
  return `${h}:${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}.${String(c).padStart(2, '0')}`;
};
const srt = (s) => {
  const ms = Math.round(s * 1000);
  const h = Math.floor(ms / 3600000), m = Math.floor((ms % 3600000) / 60000), sec = Math.floor((ms % 60000) / 1000), r = ms % 1000;
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')},${String(r).padStart(3, '0')}`;
};

/** Décode une voix en PCM brut (mis en cache à côté du MP3). */
async function versPcm(mp3) {
  const pcm = mp3.replace(/\.mp3$/, '.pcm');
  if (!fs.existsSync(pcm)) {
    await exec('ffmpeg', ['-y', '-v', 'error', '-i', mp3, '-ar', String(RATE_PCM), '-ac', '1', '-f', 's16le', pcm]);
  }
  return fs.readFileSync(pcm);
}

/** Piste sonore complète : silence partout, sauf les voix posées à l'instant exact de leur étape. */
async function ecrirePiste(segments, total, fichier) {
  const echantillons = Math.ceil(total * RATE_PCM);
  const piste = Buffer.alloc(echantillons * 2);
  for (const s of segments) {
    if (!s.mp3) continue;
    const pcm = await versPcm(s.mp3);
    const debut = Math.round(s.debutVoix * RATE_PCM) * 2;
    pcm.copy(piste, debut, 0, Math.max(0, Math.min(pcm.length, piste.length - debut)));
  }
  fs.writeFileSync(fichier, piste);
}

function ecrireAss(segments, fichier) {
  const entete = `[Script Info]
ScriptType: v4.00+
PlayResX: 1920
PlayResY: 1080
ScaledBorderAndShadow: yes
WrapStyle: 2

[V4+ Styles]
Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
Style: Defaut,Inter,40,&H00FFFFFF,&H000000FF,&H40141414,&H00000000,-1,0,0,0,100,100,0,0,3,13,0,2,150,150,44,1

[Events]
Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
`;
  const lignes = [];
  for (const s of segments) {
    for (const c of s.cues ?? []) {
      lignes.push(`Dialogue: 0,${ass(s.debutVoix + c.debut)},${ass(s.debutVoix + c.fin)},Defaut,,0,0,0,,${c.texte}`);
    }
  }
  fs.writeFileSync(fichier, entete + lignes.join('\n') + '\n');
}

function ecrireSrt(segments, fichier) {
  let n = 1;
  const blocs = [];
  for (const s of segments) {
    for (const c of s.cues ?? []) {
      blocs.push(`${n++}\n${srt(s.debutVoix + c.debut)} --> ${srt(s.debutVoix + c.fin)}\n${c.texte.replace(/\\N/g, '\n')}\n`);
    }
  }
  fs.writeFileSync(fichier, blocs.join('\n'));
}

/** Chapitres : un par étape qui porte un titre de chapitre, jusqu'au chapitre suivant. */
function chapitres(segments, total) {
  const debuts = segments.filter((s) => s.chapitre).map((s) => ({ titre: s.chapitre, debut: s.debutEtape }));
  return debuts.map((c, i) => ({ ...c, fin: i + 1 < debuts.length ? debuts[i + 1].debut : total }));
}

function ecrireChapitres(liste, titre, fichier) {
  const l = [';FFMETADATA1', `title=${titre}`];
  for (const c of liste) {
    l.push('[CHAPTER]', 'TIMEBASE=1/1000', `START=${Math.round(c.debut * 1000)}`, `END=${Math.round(c.fin * 1000)}`, `title=${c.titre}`);
  }
  fs.writeFileSync(fichier, l.join('\n') + '\n');
}

/**
 * @param o { id, titre, segments, enregistreur, dossier }
 * segments : [{ debutEtape, debutVoix, texte, cues, mp3, chapitre }] — instants en secondes depuis le début de la prise
 */
export async function monter({ id, titre, segments, enregistreur, dossier, sansSousTitres = false }) {
  fs.mkdirSync(dossier, { recursive: true });
  const travail = path.join(CFG.dossiers.cache, 'montage', id);
  fs.mkdirSync(travail, { recursive: true });

  const total = enregistreur.t1 - enregistreur.t0;
  const liste = path.join(enregistreur.dossier, 'images.ffconcat');
  enregistreur.ecrireListe(liste);
  const pcm = path.join(travail, 'voix.pcm');
  await ecrirePiste(segments, total, pcm);
  const fichierAss = path.join(travail, 'sous-titres.ass');
  ecrireAss(segments, fichierAss);
  const liste_chapitres = chapitres(segments, total);
  const meta = path.join(travail, 'chapitres.ffmeta');
  ecrireChapitres(liste_chapitres, titre, meta);

  const sortie = path.join(dossier, `${id}.mp4`);
  const filtreVideo = [
    `[0:v]fps=${CFG.fps}`,
    `scale=${CFG.fenetre.width * CFG.echelle}:${CFG.fenetre.height * CFG.echelle}:flags=lanczos`,
    'format=yuv420p',
    ...(sansSousTitres ? [] : [`subtitles=filename=sous-titres.ass:fontsdir=/usr/share/fonts`]),
  ].join(',') + '[v]';
  const filtreAudio = '[1:a]loudnorm=I=-16:TP=-1.5:LRA=11,aresample=48000[a]';

  await exec(
    'ffmpeg',
    [
      '-y', '-hide_banner', '-v', 'error',
      '-f', 'concat', '-safe', '0', '-i', liste,
      '-f', 's16le', '-ar', String(RATE_PCM), '-ac', '1', '-i', pcm,
      '-i', meta,
      '-filter_complex', `${filtreVideo};${filtreAudio}`,
      '-map', '[v]', '-map', '[a]', '-map_metadata', '2', '-map_chapters', '2',
      '-c:v', 'libx264', '-preset', CFG.preset, '-crf', CFG.crf, '-tune', 'animation',
      '-profile:v', 'high', '-pix_fmt', 'yuv420p', '-g', String(CFG.fps * 2), '-movflags', '+faststart',
      '-c:a', 'aac', '-b:a', '160k',
      '-t', total.toFixed(3),
      sortie,
    ],
    { cwd: travail, maxBuffer: 64 * 1024 * 1024, timeout: 30 * 60 * 1000 }
  );

  ecrireSrt(segments, path.join(dossier, `${id}.srt`));
  // Minutage détaillé : sert au contrôle automatique du calage voix/image (src/outils/verifier-audio.py).
  fs.writeFileSync(
    path.join(dossier, `${id}.minutage.json`),
    JSON.stringify(segments.map(({ mp3, ...s }) => s), null, 2)
  );
  fs.writeFileSync(
    path.join(dossier, `${id}.chapitres.txt`),
    liste_chapitres.map((c) => `${horodatage(c.debut)}  ${c.titre}`).join('\n') + '\n'
  );

  // Planche-contact pour relire la vidéo d'un coup d'œil : 12 images régulièrement espacées.
  const planche = path.join(dossier, `${id}.planche.png`);
  await exec('ffmpeg', [
    '-y', '-v', 'error', '-i', sortie,
    '-vf', `fps=12/${total.toFixed(2)},scale=640:-1,tile=4x3`, '-frames:v', '1', planche,
  ]).catch(() => {});

  const taille = fs.statSync(sortie).size / 1048576;
  return { sortie, duree: await dureeMedia(sortie), tailleMo: taille, chapitres: liste_chapitres };
}
