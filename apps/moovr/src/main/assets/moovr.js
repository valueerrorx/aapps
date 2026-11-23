// moovr: Bruecke zur Android-Activity (Zurueck-Taste, Pause/Resume), game.js bleibt unveraendert
(function () {
    'use strict';

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

    // Antwort an die Activity: 'exit' = App schliessen, 'handled' = Spiel hat reagiert
    window.moovrBack = function () {
        try {
            return back();
        } catch (e) {
            // Fehler im Spiel oder in der Blende: nicht mitten im Spiel beenden
            try {
                if (ready() && (game.state.current === '0' || inTransition())) return 'handled';
            } catch (ignored) {}
            return 'exit';
        }
    };

    function back() {
        if (!ready()) return 'exit';
        if (inTransition()) return 'handled';
        var state = game.state.current;
        if (state === '0') {
            if (gamestate !== 'running') return 'handled';
            if (game.paused) {
                // Menue offen: schliessen und weiterspielen (wie Tipp auf den Menueknopf)
                game.paused = false;
                showIngameMenu();
            } else if (window.menu && !window.menu.visible) {
                // Menue oeffnen, Spiel pausiert danach selbst
                showIngameMenu();
            }
            return 'handled';
        }
        if (state === 'finish') {
            fadeOut('menu');
            return 'handled';
        }
        if (state === 'menu' && window.help && window.help.alpha > 0) {
            var t = game.add.tween(window.help).to({ alpha: 0 }, 600, Phaser.Easing.Quadratic.Out, true);
            t.onComplete.add(function () { inputBlock = false; });
            return 'handled';
        }
        return 'exit';
    }

    // App im Hintergrund: Spielzeit anhalten und Audio wirklich stoppen
    window.moovrPause = function () {
        if (!ready()) return;
        game.gamePaused({ type: 'pause' });
        var ctx = audioContext();
        if (ctx && ctx.suspend) ctx.suspend();
    };

    window.moovrResume = function () {
        if (!ready()) return;
        var ctx = audioContext();
        if (ctx && ctx.resume) ctx.resume();
        // laesst eine Pause durch das Spielmenue bestehen (_codePaused)
        game.gameResumed({ type: 'resume' });
    };

    // Falls der AudioContext doch gesperrt startet: beim ersten Tipp freigeben
    document.addEventListener('touchstart', function () {
        var ctx = audioContext();
        if (ctx && ctx.state === 'suspended' && !document.hidden) ctx.resume();
    }, true);
})();
