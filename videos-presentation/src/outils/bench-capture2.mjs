import { chromium } from '/opt/pw/node_modules/playwright/index.mjs';
function dims(buf) { let i = 2; while (i < buf.length) { if (buf[i] !== 0xff) { i++; continue; } const m = buf[i+1]; if (m >= 0xc0 && m <= 0xcf && ![0xc4,0xc8,0xcc].includes(m)) return `${buf.readUInt16BE(i+7)}x${buf.readUInt16BE(i+5)}`; i += 2 + buf.readUInt16BE(i+2); } }

async function essai(nom, args, optionsContexte) {
  const b = await chromium.launch({ args: ['--hide-scrollbars', '--force-color-profile=srgb', ...args] });
  const ctx = await b.newContext(optionsContexte);
  const page = await ctx.newPage();
  await page.goto('http://localhost:4200/auth/login', { waitUntil: 'networkidle' });
  const info = await page.evaluate(() => ({ dpr: devicePixelRatio, w: innerWidth, h: innerHeight }));
  const cdp = await ctx.newCDPSession(page);
  const vus = new Set(); let n = 0;
  cdp.on('Page.screencastFrame', ({ data, sessionId }) => { n++; vus.add(dims(Buffer.from(data, 'base64'))); cdp.send('Page.screencastFrameAck', { sessionId }); });
  await cdp.send('Page.startScreencast', { format: 'jpeg', quality: 90, maxWidth: 3840, maxHeight: 2160, everyNthFrame: 1 });
  for (let i = 0; i < 40; i++) { await page.evaluate((i) => { document.body.style.background = i % 2 ? '#fff' : '#fafafa'; }, i); await page.waitForTimeout(30); }
  await cdp.send('Page.stopScreencast');
  console.log(`${nom}: dpr=${info.dpr} css=${info.w}x${info.h} → frames ${[...vus]} (${n})`);
  await b.close();
}
await essai('(c1) DSF navigateur 1.5 + window-size 1280x720, viewport null', ['--force-device-scale-factor=1.5', '--window-size=1280,720'], { viewport: null, locale: 'fr-FR' });
await essai('(c2) DSF navigateur 1.5, viewport 1280x720 sans émulation DSF', ['--force-device-scale-factor=1.5'], { viewport: { width: 1280, height: 720 }, locale: 'fr-FR' });
await essai('(c3) émulation DSF 1.5 (référence)', [], { viewport: { width: 1280, height: 720 }, deviceScaleFactor: 1.5, locale: 'fr-FR' });
