// Mesure ce que chaque technique de capture livre réellement (taille des images, cadence).
import { chromium } from '/opt/pw/node_modules/playwright/index.mjs';
const b = await chromium.launch({ args: ['--hide-scrollbars', '--force-color-profile=srgb'] });
const ctx = await b.newContext({ viewport: { width: 1280, height: 720 }, deviceScaleFactor: 1.5, locale: 'fr-FR' });
const page = await ctx.newPage();
await page.goto('http://localhost:4200/auth/login', { waitUntil: 'networkidle' });
console.log('devicePixelRatio =', await page.evaluate(() => devicePixelRatio), ' innerWidth =', await page.evaluate(() => innerWidth));

function dims(buf) { let i = 2; while (i < buf.length) { if (buf[i] !== 0xff) { i++; continue; } const m = buf[i+1]; if (m >= 0xc0 && m <= 0xcf && ![0xc4,0xc8,0xcc].includes(m)) return `${buf.readUInt16BE(i+7)}x${buf.readUInt16BE(i+5)}`; i += 2 + buf.readUInt16BE(i+2); } }

// (a) screencast CDP avec de grandes bornes
const cdp = await ctx.newCDPSession(page);
let n = 0, vu = new Set();
cdp.on('Page.screencastFrame', ({ data, sessionId }) => { n++; vu.add(dims(Buffer.from(data, 'base64'))); cdp.send('Page.screencastFrameAck', { sessionId }); });
await cdp.send('Page.startScreencast', { format: 'jpeg', quality: 90, maxWidth: 3840, maxHeight: 2160, everyNthFrame: 1 });
// provoque du mouvement
for (let i = 0; i < 60; i++) { await page.mouse.move(100 + i * 10, 200 + (i % 7) * 20); await page.waitForTimeout(16); }
await cdp.send('Page.stopScreencast');
console.log('(a) screencast CDP bornes 3840x2160 → tailles:', [...vu], ' images:', n);

// (b) page.screenshot à l'échelle de l'appareil : cadence maximale
const t0 = Date.now(); let k = 0, taille;
while (Date.now() - t0 < 4000) { const buf = await page.screenshot({ type: 'jpeg', quality: 90, scale: 'device' }); taille = dims(buf); k++; }
console.log(`(b) page.screenshot JPEG q90 scale=device → ${taille}, ${(k / ((Date.now() - t0) / 1000)).toFixed(1)} images/s`);
const t1 = Date.now(); k = 0;
while (Date.now() - t1 < 4000) { await page.screenshot({ type: 'png', scale: 'device' }); k++; }
console.log(`    page.screenshot PNG → ${(k / ((Date.now() - t1) / 1000)).toFixed(1)} images/s`);
await b.close();
