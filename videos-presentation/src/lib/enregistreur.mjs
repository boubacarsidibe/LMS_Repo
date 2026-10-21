// Enregistreur d'écran : capture des images brutes par le protocole CDP, avec leur horodatage, puis encodage
// à froid. L'enregistreur vidéo intégré de Playwright encode en VP8 à 1 Mbit/s (codé en dur, non réglable) : le
// texte d'une interface y devient flou. Ici, aucun encodage pendant la prise — on écrit des JPEG sur disque, et
// ffmpeg en tire ensuite un H.264 de haute qualité (voir montage.mjs).
import fs from 'node:fs';
import path from 'node:path';
import { maintenant } from './utilitaires.mjs';

export class Enregistreur {
  /**
   * @param page      page Playwright
   * @param dossier   où écrire les images
   * @param options   { largeur, hauteur, qualite (0-100), parN : une image sur N }
   */
  constructor(page, dossier, { largeur = 1920, hauteur = 1080, qualite = 92, parN = 2 } = {}) {
    this.page = page;
    this.dossier = dossier;
    this.largeur = largeur;
    this.hauteur = hauteur;
    this.qualite = qualite;
    this.parN = parN;
    this.images = [];
    this.ecritures = [];
    this.dimensions = null;
  }

  async demarrer() {
    fs.rmSync(this.dossier, { recursive: true, force: true });
    fs.mkdirSync(this.dossier, { recursive: true });
    this.cdp = await this.page.context().newCDPSession(this.page);
    this.cdp.on('Page.screencastFrame', (evt) => this.surImage(evt));
    await this.cdp.send('Page.startScreencast', {
      format: 'jpeg',
      quality: this.qualite,
      maxWidth: this.largeur,
      maxHeight: this.hauteur,
      everyNthFrame: this.parN,
    });
    this.t0 = maintenant();
  }

  surImage({ data, metadata, sessionId }) {
    const recu = maintenant();
    // Accusé immédiat : Chrome n'envoie l'image suivante qu'une fois la précédente acquittée.
    this.cdp.send('Page.screencastFrameAck', { sessionId }).catch(() => {});
    const nom = `${String(this.images.length).padStart(6, '0')}.jpg`;
    // L'horodatage du compositeur est en secondes depuis l'epoch ; on ne s'y fie que s'il est cohérent avec l'horloge.
    const ts = metadata?.timestamp;
    const t = ts && Math.abs(ts - recu) < 5 ? ts : recu;
    this.images.push({ nom, t });
    const buf = Buffer.from(data, 'base64');
    if (!this.dimensions && buf[0] === 0xff && buf[1] === 0xd8) this.dimensions = dimensionsJpeg(buf);
    this.ecritures.push(fs.promises.writeFile(path.join(this.dossier, nom), buf));
  }

  /** Secondes écoulées depuis le début de la prise (même horloge que les instants des étapes). */
  horloge() {
    return maintenant() - this.t0;
  }

  async arreter() {
    this.t1 = maintenant();
    await this.cdp.send('Page.stopScreencast').catch(() => {});
    await Promise.all(this.ecritures);
    return { images: this.images.length, duree: this.t1 - this.t0, dimensions: this.dimensions };
  }

  /**
   * Liste ffconcat : chaque image reste affichée jusqu'à l'arrivée de la suivante, ce qui restitue la durée réelle
   * des images fixes (une page qui ne bouge pas n'émet aucune image). La première est supposée affichée dès t0.
   */
  ecrireListe(fichier) {
    const lignes = ['ffconcat version 1.0'];
    const n = this.images.length;
    if (n === 0) throw new Error("Aucune image capturée : l'enregistrement est vide.");
    for (let i = 0; i < n; i++) {
      const debut = i === 0 ? this.t0 : this.images[i].t;
      const fin = i + 1 < n ? this.images[i + 1].t : this.t1;
      lignes.push(`file '${this.images[i].nom}'`, `duration ${Math.max(0.001, fin - debut).toFixed(4)}`);
    }
    // Quirk de ffmpeg : la durée de la dernière entrée n'est honorée que si le fichier est répété.
    lignes.push(`file '${this.images[n - 1].nom}'`);
    fs.writeFileSync(fichier, lignes.join('\n') + '\n');
  }
}

/** Largeur et hauteur d'un JPEG, lues dans son en-tête SOF. */
function dimensionsJpeg(buf) {
  let i = 2;
  while (i < buf.length) {
    if (buf[i] !== 0xff) { i++; continue; }
    const marqueur = buf[i + 1];
    if (marqueur >= 0xc0 && marqueur <= 0xcf && ![0xc4, 0xc8, 0xcc].includes(marqueur)) {
      return { hauteur: buf.readUInt16BE(i + 5), largeur: buf.readUInt16BE(i + 7) };
    }
    i += 2 + buf.readUInt16BE(i + 2);
  }
  return null;
}
