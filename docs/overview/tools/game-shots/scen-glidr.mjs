// glidr: Levelauswahl, Level (Übersicht, Spielansicht, Kuh ziehen), Levelende
const startLevel = (ev, n) => ev((n) => { music.stop(); level = n; fadeOut(level); }, n);

// Viewport-Position eines Sprites (Spielwelt 768×480 auf 960×480)
const screenPos = (ev, expr) => ev((expr) => {
  const p = eval(expr).toGlobal(new PIXI.Point(0, 0));
  return { x: p.x * 960 / 768, y: p.y };
}, expr);

// Kuh am Gummi ziehen: halten (Bild), loslassen (Bild)
async function pull({ page, ev, sleep, shot }, dx, dy, nameHold, nameAfter) {
  const p = await screenPos(ev, 'cows[0]');
  await page.mouse.move(p.x, p.y); await sleep(250);
  await page.mouse.down(); await sleep(250);
  for (let i = 1; i <= 12; i++) { await page.mouse.move(p.x + dx * i / 12, p.y + dy * i / 12); await sleep(30); }
  await sleep(300); await shot(nameHold);
  await page.mouse.up(); await sleep(350); await shot(nameAfter);
}

export default {
  async store(c) {
    const { page, base, sleep, shot, ev, waitState } = c;
    // Spielstand: Level 0–17 mit Sternen, damit die Levelauswahl nach Fortschritt aussieht
    await page.addInitScript(() => {
      if (localStorage.getItem('seeded')) return;
      [3, 4, 3, 4, 2, 4, 3, 3, 4, 2, 3, 4, 3, 2, 4, 3, 1, 3].forEach((v, i) => localStorage.setItem(String(i), String(v)));
      localStorage.setItem('seeded', '1');
    });
    await page.goto(base + 'index.html');
    await waitState('menu');
    await sleep(3000);
    await shot('menu');
    for (const n of [11, 16, 23]) {
      await startLevel(ev, n);
      await waitState(String(n));
      await sleep(3500);
      await shot(`l${n}-overview`);
      await page.mouse.click(480, 360); // Zoom-Knopf: Übersicht -> Spielansicht
      await sleep(1500);
      await pull(c, -150, 60, `l${n}-hold1`, `l${n}-after1`);
      await sleep(500);
      await pull(c, 120, -90, `l${n}-hold2`, `l${n}-after2`);
      await ev(() => { level = 'menu'; fadeOut('menu'); });
      await waitState('menu');
      await sleep(500);
    }
    // Levelende: echter Finish-State, Werte gesetzt (4 Sterne, 2 Kleeblätter)
    await startLevel(ev, 16);
    await waitState('16');
    await sleep(1500);
    await ev(() => {
      gamestate = 'running'; totalAssets = 4; cowsInLevel = 1; cow_initial_health = 3; cloversCollected = 2;
      totalAssetsDestroyed = 0; totalHits = 3; totalHealthwasted = 0; fadeOut('finish');
    });
    await waitState('finish');
    await sleep(1500);
    await shot('cleared');
  },
};
