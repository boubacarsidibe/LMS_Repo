// Diagnostic : que voit le visiteur PENDANT le chargement d'une fiche de formation (réponse de l'API retardée) ?
import { chromium } from '/opt/pw/node_modules/playwright/index.mjs';
const BASE = process.env.BASE_URL ?? 'http://localhost:4200';
const b = await chromium.launch({ args: ['--hide-scrollbars'] });
const page = await (await b.newContext({ viewport: { width: 1280, height: 720 }, locale: 'fr-FR' })).newPage();
await page.route('**/api/recrutement/public/formations/8', async (route) => { await new Promise((r) => setTimeout(r, 2500)); await route.continue(); });
await page.goto(`${BASE}/recrutement/catalogue`, { waitUntil: 'networkidle' });
await page.click('article >> nth=0');
await page.waitForTimeout(1000);
console.log('Pendant le chargement — « non trouvée » :', await page.locator('text=Formation non trouvée').count(), '· « Chargement » :', await page.locator('text=Chargement de la formation').count());
await page.waitForTimeout(3000);
console.log('Après la réponse     — « non trouvée » :', await page.locator('text=Formation non trouvée').count(), '· titre fiche :', await page.locator('h1').first().innerText());
await b.close();
