// Gestes partagés par plusieurs scénarios.

/**
 * Connexion « à l'écran » : le spectateur voit où l'on saisit l'adresse et le mot de passe. Si la page affichée n'est
 * pas celle de connexion (après une déconnexion, on arrive sur l'accueil), on s'y rend d'abord.
 */
export async function seConnecter(a, email, mdp) {
  if (!(await a.url()).startsWith('/auth/login')) {
    await a.aller('/auth/login');
  }
  await a.taper('#email', email);
  await a.taper('#password', mdp);
  await a.pause(300);
  await a.cliquer('button[type=submit]');
  await a.page.waitForURL((u) => !u.pathname.startsWith('/auth'), { timeout: 20000 });
  await a.patienter(900);
}

/**
 * Déconnexion par le menu du compte. Selon l'espace, le menu est celui de la barre publique (#profile-btn) ou du bandeau
 * des espaces (« Menu du compte » / « Menu utilisateur »), et l'on atterrit sur l'accueil ou sur la page de connexion.
 */
export async function seDeconnecter(a) {
  await a.cliquer('#profile-btn, button[aria-label="Menu du compte"], button[aria-label="Menu utilisateur"]');
  await a.cliquer('text=Déconnexion');
  await a.page.waitForURL((u) => /^\/(accueil|auth\/login)/.test(u.pathname), { timeout: 15000 });
  await a.patienter(500);
}

/** Inventaire des éléments interactifs visibles de la page — outil de mise au point des scénarios. */
export async function inventaire(page) {
  return page.evaluate(() => {
    const vis = (e) => { const r = e.getBoundingClientRect(); const s = getComputedStyle(e); return r.width > 2 && r.height > 2 && s.visibility !== 'hidden' && s.display !== 'none'; };
    const d = (e) => {
      const txt = (e.innerText || e.value || e.getAttribute('aria-label') || e.getAttribute('placeholder') || '').replace(/\s+/g, ' ').trim().slice(0, 70);
      return `${e.tagName.toLowerCase()}${e.id ? '#' + e.id : ''}${e.getAttribute('type') ? '[' + e.getAttribute('type') + ']' : ''}${e.getAttribute('accept') ? '{' + e.getAttribute('accept') + '}' : ''} « ${txt} »${e.disabled ? ' (désactivé)' : ''}`;
    };
    return [...document.querySelectorAll('input,select,textarea,button,label[for],a[href]')]
      .filter((e) => vis(e) || e.type === 'file').map(d).join('\n');
  });
}

/**
 * Valeur (attribut value) de la première option d'une liste déroulante dont le texte contient `fragment`. Les listes du
 * produit portent des libellés composés (« Elhadji Diouf — Promotion 2026 — Master… ») : on les désigne par un morceau.
 */
export async function valeurOption(a, selecteur, fragment) {
  const valeur = await a.page
    .locator(`${selecteur} option`)
    .evaluateAll((options, f) => options.find((o) => o.textContent.includes(f))?.value ?? null, fragment);
  if (valeur === null) throw new Error(`Aucune option contenant « ${fragment} » dans ${selecteur}`);
  return { value: valeur };
}
