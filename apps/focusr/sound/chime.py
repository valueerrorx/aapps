#!/usr/bin/env python3
# Erzeugt res/raw/chime.wav: drei sanfte Glockenschlaege (selbst synthetisiert, frei von Rechten)
# Aufruf: python3 apps/focusr/sound/chime.py apps/focusr/src/main/res/raw/chime.wav
import math
import struct
import sys
import wave

RATE = 22050
LENGTH = 3.4
# (Start s, Grundton Hz, Lautstaerke)
STRIKES = [(0.0, 1046.5, 1.0), (0.42, 784.0, 0.9), (0.84, 1318.5, 0.8)]
# Glockenartige Teiltoene: (Faktor, Pegel, Abklingzeit s)
PARTIALS = [(1.0, 1.0, 1.6), (2.0, 0.35, 0.9), (2.76, 0.22, 0.6), (5.4, 0.08, 0.25)]


def sample(t):
    v = 0.0
    for start, f0, amp in STRIKES:
        dt = t - start
        if dt < 0:
            continue
        attack = min(1.0, dt / 0.006)
        for k, lvl, tau in PARTIALS:
            v += amp * lvl * attack * math.exp(-dt / tau) * math.sin(2 * math.pi * f0 * k * dt)
    # sanft ausblenden
    tail = max(0.0, min(1.0, (LENGTH - t) / 0.4))
    return v * tail


def main(path):
    n = int(RATE * LENGTH)
    raw = [sample(i / RATE) for i in range(n)]
    peak = max(abs(x) for x in raw) or 1.0
    gain = 0.6 / peak
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", int(x * gain * 32767)) for x in raw))


if __name__ == "__main__":
    main(sys.argv[1])
