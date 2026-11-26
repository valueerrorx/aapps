// moovr: Hauptmenü, ein Lauf mit Bild alle ~0,5 s und gelegentlichem Tipp, Ergebnis
export default {
  async run({ page, base, sleep, shot, ev, waitState, seed }) {
    await page.addInitScript(() => { if (!localStorage.getItem('distance')) localStorage.setItem('distance', '742'); });
    await page.goto(base + 'index.html');
    await waitState('menu');
    await sleep(2500);
    await shot('menu');
    await page.mouse.click(480, 240); // „go on a ride“
    await waitState('0');
    const tag = `s${seed}-`; // mehrere Läufe in einen Ordner
    const rnd = () => (seed = (seed * 9301 + 49297) % 233280) / 233280;
    for (let k = 0; k < 90; k++) {
      if (await ev(() => game.state.current) !== '0') { await sleep(1500); await shot(`${tag}result`); break; }
      if (k % 4 === 0) { // Tipp vorne aufs Eis
        await page.mouse.move(200 + rnd() * 560, 60 + rnd() * 220);
        await page.mouse.down(); await sleep(120 + rnd() * 300); await page.mouse.up();
      }
      await sleep(450);
      await shot(`${tag}run${String(k).padStart(2, '0')}`);
    }
  },
};
