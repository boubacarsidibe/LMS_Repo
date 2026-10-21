// Surcouche injectée dans chaque page filmée (context.addInitScript) : curseur visible, onde au clic, surbrillance,
// puce de chapitre, cartes de titre et de bilan. Tout vit dans le DOM, hors de l'application, au-dessus de tout : ce
// que la capture enregistre est exactement ce que voit le spectateur.
//
// ATTENTION : cette fonction est sérialisée (toString) et exécutée dans le navigateur. Elle doit rester autonome :
// aucune variable ni import du module ne lui est accessible.
export function installer() {
  if (window.__demo) return;

  const NUIT = '#131741';
  const BLEU = '#30609a';
  const ACCENT = '#FF9F1C';

  const css = `
    #__d_racine{position:fixed;inset:0;z-index:2147483646;pointer-events:none;font-family:Inter,system-ui,sans-serif}
    #__d_curseur{position:fixed;left:0;top:0;z-index:2147483647;pointer-events:none;will-change:transform;
      filter:drop-shadow(0 2px 3px rgba(0,0,0,.35));transform:translate(-200px,-200px)}
    .__d_onde{position:fixed;z-index:2147483645;pointer-events:none;border-radius:50%;border:3px solid ${BLEU};
      background:rgba(48,96,154,.18);width:12px;height:12px;margin:-6px 0 0 -6px;animation:__d_onde .55s ease-out forwards}
    @keyframes __d_onde{to{transform:scale(4.2);opacity:0}}
    .__d_surbrillance{position:fixed;border:3px solid ${ACCENT};border-radius:10px;box-sizing:border-box;
      box-shadow:0 0 0 4px rgba(255,159,28,.28);opacity:0;transition:opacity .35s;animation:__d_pouls 1.4s ease-in-out infinite}
    .__d_surbrillance.projecteur{box-shadow:0 0 0 4px rgba(255,159,28,.28),0 0 0 100vmax rgba(8,12,40,.42)}
    .__d_surbrillance.visible{opacity:1}
    @keyframes __d_pouls{50%{box-shadow:0 0 0 9px rgba(255,159,28,.12)}}
    .__d_surbrillance.projecteur{animation:none}
    #__d_puce{position:fixed;left:28px;bottom:128px;display:flex;align-items:center;gap:12px;background:${NUIT};color:#fff;
      border-radius:12px;padding:9px 20px 9px 10px;font:600 17px/1.2 Inter,system-ui,sans-serif;
      box-shadow:0 10px 28px rgba(0,0,0,.38);opacity:0;transform:translateY(16px);transition:opacity .4s,transform .4s}
    #__d_puce.visible{opacity:1;transform:none}
    #__d_puce .num{background:${BLEU};border-radius:8px;min-width:30px;height:30px;display:grid;place-items:center;font-size:15px;font-weight:800}
    #__d_carte{position:fixed;inset:0;background:radial-gradient(1100px 650px at 72% 18%,#1f2a7a 0%,${NUIT} 55%,#0b0e2b 100%);
      color:#fff;display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center;
      opacity:0;transition:opacity .6s ease;padding:40px 90px;box-sizing:border-box}
    #__d_carte.visible{opacity:1}
    #__d_carte img.logo{height:84px;width:auto;margin-bottom:34px}
    #__d_carte .pastille{font-size:13px;letter-spacing:.2em;text-transform:uppercase;color:#a9bbff;
      border:1px solid rgba(169,187,255,.45);border-radius:999px;padding:7px 18px;margin-bottom:24px;font-weight:600}
    #__d_carte h1{font-size:50px;line-height:1.1;font-weight:800;margin:0;max-width:980px;letter-spacing:-.01em}
    #__d_carte p.sous{font-size:21px;line-height:1.45;opacity:.86;max-width:860px;margin:22px 0 0}
    #__d_carte .pied{position:absolute;bottom:118px;left:0;right:0;font-size:12px;letter-spacing:.16em;text-transform:uppercase;opacity:.5}
    #__d_carte ul{list-style:none;margin:30px 0 0;padding:0;display:grid;grid-template-columns:1fr 1fr;gap:12px 38px;
      text-align:left;max-width:1000px}
    #__d_carte li{display:flex;gap:12px;align-items:flex-start;font-size:19px;line-height:1.3;opacity:0;transform:translateY(8px);
      transition:opacity .5s,transform .5s}
    #__d_carte li.visible{opacity:1;transform:none}
    #__d_carte li i{flex:none;width:24px;height:24px;border-radius:50%;background:${ACCENT};color:${NUIT};display:grid;
      place-items:center;font-style:normal;font-weight:800;font-size:14px;margin-top:1px}
  `;

  let racine, curseur, puce, carte, surbrillances = [];

  function el(tag, classe, texte) {
    const e = document.createElement(tag);
    if (classe) e.className = classe;
    if (texte !== undefined) e.textContent = texte;
    return e;
  }

  function monter() {
    if (document.getElementById('__d_racine')) return;
    const style = document.createElement('style');
    style.id = '__d_style';
    style.textContent = css;
    document.documentElement.appendChild(style);

    racine = el('div');
    racine.id = '__d_racine';
    puce = el('div');
    puce.id = '__d_puce';
    racine.appendChild(puce);
    document.documentElement.appendChild(racine);

    curseur = el('div');
    curseur.id = '__d_curseur';
    curseur.innerHTML =
      '<svg width="30" height="30" viewBox="0 0 24 24"><path d="M5 3l14 8-6.2 1.8L10 19z" fill="#fff" stroke="#111" stroke-width="1.4" stroke-linejoin="round"/></svg>';
    document.documentElement.appendChild(curseur);
  }

  function deplacer(x, y) {
    if (!curseur) return;
    // La pointe de la flèche est en (5,3) sur 24 unités, soit (6.25, 3.75) à 30 px.
    curseur.style.transform = `translate(${x - 6.25}px, ${y - 3.75}px)`;
  }

  function onde(x, y) {
    const o = el('div', '__d_onde');
    o.style.left = x + 'px';
    o.style.top = y + 'px';
    document.documentElement.appendChild(o);
    setTimeout(() => o.remove(), 700);
  }

  window.addEventListener('mousemove', (e) => deplacer(e.clientX, e.clientY), true);
  window.addEventListener('mousedown', (e) => onde(e.clientX, e.clientY), true);

  function surligner(rect, { projecteur = false, marge = 6 } = {}) {
    monter();
    const s = el('div', '__d_surbrillance' + (projecteur ? ' projecteur' : ''));
    s.style.left = rect.x - marge + 'px';
    s.style.top = rect.y - marge + 'px';
    s.style.width = rect.width + marge * 2 + 'px';
    s.style.height = rect.height + marge * 2 + 'px';
    racine.appendChild(s);
    surbrillances.push(s);
    requestAnimationFrame(() => requestAnimationFrame(() => s.classList.add('visible')));
  }

  function effacerSurbrillances() {
    const anciennes = surbrillances;
    surbrillances = [];
    anciennes.forEach((s) => {
      s.classList.remove('visible');
      setTimeout(() => s.remove(), 450);
    });
  }

  function afficherPuce(numero, titre) {
    monter();
    puce.replaceChildren(el('span', 'num', String(numero)), el('span', '', titre));
    requestAnimationFrame(() => puce.classList.add('visible'));
  }
  function masquerPuce() {
    puce && puce.classList.remove('visible');
  }

  function afficherCarte({ pastille, titre, sousTitre, puces, pied }) {
    monter();
    retirerCarte(true);
    carte = el('div');
    carte.id = '__d_carte';
    const logo = el('img', 'logo');
    logo.src = '/images/logo-inpg-blanc.png';
    logo.alt = 'INPG';
    carte.appendChild(logo);
    if (pastille) carte.appendChild(el('div', 'pastille', pastille));
    if (titre) carte.appendChild(el('h1', '', titre));
    if (sousTitre) carte.appendChild(el('p', 'sous', sousTitre));
    if (puces && puces.length) {
      const ul = el('ul');
      puces.forEach((p) => {
        const li = el('li');
        li.append(el('i', '', '✓'), el('span', '', p));
        ul.appendChild(li);
      });
      carte.appendChild(ul);
    }
    carte.appendChild(el('div', 'pied', pied || 'Plateforme LMS · Institut National du Pétrole et du Gaz'));
    racine.insertBefore(carte, racine.firstChild);
    requestAnimationFrame(() => requestAnimationFrame(() => carte.classList.add('visible')));
    // Les puces d'une carte de bilan apparaissent l'une après l'autre.
    carte.querySelectorAll('li').forEach((li, i) => setTimeout(() => li.classList.add('visible'), 700 + i * 420));
  }

  function retirerCarte(immediat) {
    if (!carte) return;
    const c = carte;
    carte = null;
    if (immediat) return c.remove();
    c.classList.remove('visible');
    setTimeout(() => c.remove(), 700);
  }

  window.__demo = { monter, deplacer, onde, surligner, effacerSurbrillances, afficherPuce, masquerPuce, afficherCarte, retirerCarte };

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', monter);
  else monter();
}
