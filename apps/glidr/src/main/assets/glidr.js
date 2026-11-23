// glidr: Bruecke zur Android-Activity (Zurueck-Taste, Pause/Resume), game.js bleibt unveraendert
(function () {
    'use strict';

    var MENU_TWEEN_MS = 350;
    var lastMenuToggle = 0;

    function ready() {
        return typeof game !== 'undefined' && game && game.isBooted && game.state;
    }

    function audioContext() {
        return ready() && game.sound && game.sound.usingWebAudio ? game.sound.context : null;
    }

    // Uebergang (Cover-Blende) laeuft gerade
    function inTransition() {
        return !!(cover && cover.game);
    }

    function isLevel(state) {
        return /^\d+$/.test(state);
    }

    // Spielmenue rechts ein-/ausfahren, nicht waehrend es noch faehrt
    function toggleIngameMenu() {
        var now = Date.now();
        if (now - lastMenuToggle < MENU_TWEEN_MS) return;
        lastMenuToggle = now;
        showIngameMenu();
    }

    // Antwort an die Activity: 'exit' = App schliessen, 'handled' = Spiel hat reagiert
    window.glidrBack = function () {
        if (!ready()) return 'exit';
        if (inTransition()) return 'handled';
        var state = game.state.current;
        if (isLevel(state)) {
            // verloren: Blende zum Ergebnis laeuft gleich
            if (gamestate !== 'running') return 'handled';
            if (window.menu && window.menu.parent) toggleIngameMenu();
            return 'handled';
        }
        if (state === 'finish' || state === 'final') {
            level = 'menu';
            fadeOut('menu');
            return 'handled';
        }
        if (state === 'menu' && game.camera.y > 0) {
            // Levelauswahl: erst eine Seite zurueckblaettern
            switchpageup();
            return 'handled';
        }
        return 'exit';
    };

    // App im Hintergrund: Spielzeit anhalten und Audio wirklich stoppen
    window.glidrPause = function () {
        if (!ready()) return;
        game.gamePaused({ type: 'pause' });
        var ctx = audioContext();
        if (ctx && ctx.suspend) ctx.suspend();
    };

    window.glidrResume = function () {
        if (!ready()) return;
        var ctx = audioContext();
        if (ctx && ctx.resume) ctx.resume();
        game.gameResumed({ type: 'resume' });
    };

    // Falls der AudioContext doch gesperrt startet: beim ersten Tipp freigeben
    document.addEventListener('touchstart', function () {
        var ctx = audioContext();
        if (ctx && ctx.state === 'suspended' && !document.hidden) ctx.resume();
    }, true);
})();
