// HD-Grafik (glidr, moovr): Canvas in doppelter Aufloesung, Spiellogik bleibt 768x480.
// URL-Parameter ?hd=0 = Original (768x480, alte PNGs) zum Vergleich.
// Prinzip: Atlas-/Font-Koordinaten bleiben logisch (JSON/XML unveraendert),
// die @2x-PNGs bekommen baseTexture.resolution=2 und nur das Zeichnen liest Quellpixel x2.
(function () {
    var hd = new URLSearchParams(location.search).get("hd") !== "0";
    window.hdGfx = {
        on: hd,
        resolution: hd ? 2 : 1,
        sprites: hd ? "assets/sprites/allsprites@2x.png" : "assets/sprites/allsprites.png",
        font: hd ? "assets/fonts/font1@2x.png" : "assets/fonts/font1.png",
    };
    if (!hd) return;

    // @2x-Bilder nach dem Laden als HD-Basistextur markieren
    function markHD(orig) {
        return function (key, url) {
            var r = orig.apply(this, arguments);
            var base = PIXI.BaseTextureCache[key];
            if (base && /@2x/.test(url)) {
                base.resolution = 2;
                base.hdGfx = true;
            }
            return r;
        };
    }
    var C = Phaser.Cache.prototype;
    C.addTextureAtlas = markHD(C.addTextureAtlas);
    C.addBitmapFont = markHD(C.addBitmapFont);

    // Einfaerben wie PIXI.CanvasTinter.tintWithMultiply, nur in Quellaufloesung
    function tintHD(t, tint, B, canvas) {
        var cr = t.crop, w = Math.round(cr.width * B), h = Math.round(cr.height * B);
        canvas = canvas || document.createElement("canvas");
        canvas.width = w;
        canvas.height = h;
        var c = canvas.getContext("2d"), src = t.baseTexture.source;
        c.fillStyle = "#" + ("00000" + (tint | 0).toString(16)).substr(-6);
        c.fillRect(0, 0, w, h);
        c.globalCompositeOperation = "multiply";
        c.drawImage(src, cr.x * B, cr.y * B, w, h, 0, 0, w, h);
        c.globalCompositeOperation = "destination-atop";
        c.drawImage(src, cr.x * B, cr.y * B, w, h, 0, 0, w, h);
        return canvas;
    }

    // Sprite/Image/BitmapText-Zeichen: Quelle = crop*B, Ziel = logisch*R (sonst Original-Pfad)
    var S = PIXI.Sprite.prototype;
    var spriteCanvas = S._renderCanvas;
    S._renderCanvas = function (rs) {
        var t = this.texture;
        if (!t.baseTexture.hdGfx) return spriteCanvas.call(this, rs);
        if (this.visible === false || this.alpha === 0 || this.renderable === false || t.crop.width <= 0 || t.crop.height <= 0) return;
        var ctx = rs.context, B = t.baseTexture.resolution, R = rs.resolution, wt = this.worldTransform, cr = t.crop;
        if (this.blendMode !== rs.currentBlendMode) {
            rs.currentBlendMode = this.blendMode;
            ctx.globalCompositeOperation = PIXI.blendModesCanvas[rs.currentBlendMode];
        }
        if (this._mask) rs.maskManager.pushMask(this._mask, rs);
        if (t.valid) {
            ctx.globalAlpha = this.worldAlpha;
            if (rs.smoothProperty && rs.scaleMode !== t.baseTexture.scaleMode) {
                rs.scaleMode = t.baseTexture.scaleMode;
                ctx[rs.smoothProperty] = rs.scaleMode === PIXI.scaleModes.LINEAR;
            }
            var dx = t.trim ? t.trim.x - this.anchor.x * t.trim.width : this.anchor.x * -t.frame.width;
            var dy = t.trim ? t.trim.y - this.anchor.y * t.trim.height : this.anchor.y * -t.frame.height;
            if (rs.roundPixels) {
                ctx.setTransform(wt.a, wt.b, wt.c, wt.d, (wt.tx * R) | 0, (wt.ty * R) | 0);
                dx |= 0;
                dy |= 0;
            } else ctx.setTransform(wt.a, wt.b, wt.c, wt.d, wt.tx * R, wt.ty * R);
            var src = t.baseTexture.source, sx = cr.x * B, sy = cr.y * B;
            if (this.tint !== 0xffffff) {
                // Animationen tauschen crop im selben Texture-Objekt -> Schluessel inkl. crop
                var key = this.tint + ":" + cr.x + "," + cr.y + "," + cr.width + "," + cr.height;
                if (this._hdTintKey !== key) {
                    this._hdTintKey = key;
                    this._hdTinted = tintHD(t, this.tint, B, this._hdTinted);
                }
                src = this._hdTinted;
                sx = sy = 0;
            }
            ctx.drawImage(src, sx, sy, cr.width * B, cr.height * B, dx * R, dy * R, cr.width * R, cr.height * R);
        }
        for (var i = 0; i < this.children.length; i++) this.children[i]._renderCanvas(rs);
        if (this._mask) rs.maskManager.popMask(rs);
    };

    // TileSprite aus dem HD-Atlas: Kachel in voller Aufloesung, Muster um 1/B skaliert
    var T = PIXI.TilingSprite.prototype;
    var tileGen = T.generateTilingTexture, tileCanvas = T._renderCanvas;
    function hdSource(ts) {
        var e = ts.originalTexture || ts.texture;
        return e.baseTexture.hdGfx ? e : null;
    }
    T.generateTilingTexture = function (pow2) {
        var e = hdSource(this);
        if (!e || pow2) return tileGen.call(this, pow2);
        if (!e.baseTexture.hasLoaded) return;
        var B = e.baseTexture.resolution, cr = e.crop, w = Math.round(cr.width * B), h = Math.round(cr.height * B);
        var buf = new PIXI.CanvasBuffer(w, h);
        buf.context.drawImage(e.baseTexture.source, cr.x * B, cr.y * B, w, h, 0, 0, w, h);
        if (this.tilingTexture && this.tilingTexture.isTiling) this.tilingTexture.destroy(true);
        this.tilingTexture = PIXI.Texture.fromCanvas(buf.canvas);
        this.tilingTexture.canvasBuffer = buf;
        this.tilingTexture.isTiling = true;
        this.tilingTexture.hdScale = B;
        this.tileScaleOffset.x = 1 / B;
        this.tileScaleOffset.y = 1 / B;
        this.refreshTexture = false;
        this.originalTexture = e;
        this.texture = this.tilingTexture;
    };
    T._renderCanvas = function (rs) {
        if (!hdSource(this)) return tileCanvas.call(this, rs);
        if (this.visible === false || this.alpha === 0) return;
        var ctx = rs.context, R = rs.resolution, f = this.worldTransform;
        if (this._mask) rs.maskManager.pushMask(this._mask, rs);
        ctx.globalAlpha = this.worldAlpha;
        ctx.setTransform(f.a * R, f.b * R, f.c * R, f.d * R, f.tx * R, f.ty * R);
        if (!this.__tilePattern || this.refreshTexture) {
            this.generateTilingTexture(false);
            if (!this.tilingTexture) return;
            this.__tilePattern = ctx.createPattern(this.tilingTexture.baseTexture.source, "repeat");
        }
        if (this.blendMode !== rs.currentBlendMode) {
            rs.currentBlendMode = this.blendMode;
            ctx.globalCompositeOperation = PIXI.blendModesCanvas[rs.currentBlendMode];
        }
        var B = this.tilingTexture.hdScale || 1, p = this.tilePosition, s = this.tileScale;
        p.x %= this.tilingTexture.baseTexture.width / B;
        p.y %= this.tilingTexture.baseTexture.height / B;
        // wie Original, nur Musterpixel = 1/B Welteinheit
        ctx.scale(s.x, s.y);
        ctx.translate(p.x + this.anchor.x * -this._width, p.y + this.anchor.y * -this._height);
        ctx.scale(1 / B, 1 / B);
        ctx.fillStyle = this.__tilePattern;
        ctx.fillRect(-p.x * B, -p.y * B, (this._width / s.x) * B, (this._height / s.y) * B);
        ctx.setTransform(f.a * R, f.b * R, f.c * R, f.d * R, f.tx * R, f.ty * R);
        if (this._mask) rs.maskManager.popMask(rs);
        for (var i = 0; i < this.children.length; i++) this.children[i]._renderCanvas(rs);
    };
})();
