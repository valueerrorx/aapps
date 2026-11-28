#!/usr/bin/env python3
# Anteil Bildinhalt in einem Screenshot (0..1): Pixel, die deutlich von der haeufigsten Farbe abweichen.
# Leerer Startbildschirm (nur Hintergrund) ~0. Rand oben/unten (Statusleiste, Home-Balken) zaehlt nicht.
# shot-content.py <bild.png>  -> druckt den Anteil; nur Standardbibliothek (8-Bit-PNG, ohne Interlace)
import struct
import sys
import zlib


def read_png(path):
    data = open(path, 'rb').read()
    if data[:8] != b'\x89PNG\r\n\x1a\n':
        raise SystemExit(f'{path}: kein PNG')
    pos, idat, plte = 8, b'', None
    while pos < len(data):
        n, kind = struct.unpack('>I4s', data[pos:pos + 8])
        body = data[pos + 8:pos + 8 + n]
        if kind == b'IHDR':
            w, h, depth, ctype, _, _, inter = struct.unpack('>IIBBBBB', body)
        elif kind == b'PLTE':
            plte = body
        elif kind == b'IDAT':
            idat += body
        pos += 12 + n
    if depth != 8 or inter != 0:
        raise SystemExit(f'{path}: PNG {depth} Bit/Interlace {inter} nicht unterstuetzt')
    bpp = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ctype]
    raw, stride = zlib.decompress(idat), w * bpp
    rows, prev, i = [], bytearray(stride), 0
    for _ in range(h):
        f, line = raw[i], bytearray(raw[i + 1:i + 1 + stride])
        i += 1 + stride
        for x in range(stride):
            a = line[x - bpp] if x >= bpp else 0
            b = prev[x]
            if f == 1:
                line[x] = (line[x] + a) & 255
            elif f == 2:
                line[x] = (line[x] + b) & 255
            elif f == 3:
                line[x] = (line[x] + ((a + b) >> 1)) & 255
            elif f == 4:
                c = prev[x - bpp] if x >= bpp else 0
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        rows.append(line)
        prev = line
    def rgb(line, x):
        if ctype == 3:
            k = line[x] * 3
            return plte[k], plte[k + 1], plte[k + 2]
        if ctype in (0, 4):
            v = line[x * bpp]
            return v, v, v
        return line[x * bpp], line[x * bpp + 1], line[x * bpp + 2]
    return w, h, [[rgb(r, x) for x in range(w)] for r in rows]


def content(path):
    w, h, px = read_png(path)
    # oben 8 %, unten 6 % weglassen (Statusleiste, Home-Balken)
    area = [p for row in px[int(h * 0.08):int(h * 0.94)] for p in row]
    counts = {}
    for r, g, b in area:
        k = (r >> 3, g >> 3, b >> 3)
        counts[k] = counts.get(k, 0) + 1
    br, bg, bb = (v << 3 for v in max(counts, key=counts.get))
    off = sum(1 for r, g, b in area if max(abs(r - br), abs(g - bg), abs(b - bb)) > 32)
    return off / len(area)


if __name__ == '__main__':
    print(f'{content(sys.argv[1]):.4f}')
