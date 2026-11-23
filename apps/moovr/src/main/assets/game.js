var states = [];
var target = new Phaser.Point();
var firststart = true;
var cow;
var mine;
var ramps = 0;  // just to keep track of and enable the presolve event only if needed
var music;
var cowragfast;
var gamestate='running';
var loaderEmpty;
var loaderFull;
var inputBlock=false;
var distance;
var constraintcount = 0;
var cow_initial_health = 2;
var cowallow = true;  //allow to push the cow (switch variable)
var cover;
var texture;
var polygon;
var ice1;
var ice;

var randomnumber =1;
var randomtime = 0;
var slidespeed = 8;
var cowspeed = 500;
var xGravity = 800;
var detonatedwaterhole;
var bombedboxes = 0;


var timeDelta1 = 0;
var timeDelta2 = 0;

var randX;
var randXs;
var randY;
var randomsound1 = 0;
var randomsound2 = 0;

var frozenWaterholes = 0;
var cloverartist = false;

var difficulty = 0;
var supercow = false;
var normalizeCowevent;

var BootState = {
    preload: function() {
        game.load.audio('clumsytheme', 'assets/sounds/clumsycowrag.ogg');
        if (loader){
            game.load.image('loaderFull', 'assets/loaderfull');
            game.load.image('loaderEmpty', 'assets/loaderempty');
        }
    },
    create: function() {
        music = game.add.audio('clumsytheme',1,true);
        music.play('',0,1,true); 
        // fill the whole window/screen (same as cowsonice)
        if ('ontouchstart' in window || navigator.maxTouchPoints > 0) { game.scale.forceLandscape = true; }
        game.scale.scaleMode = Phaser.ScaleManager.EXACT_FIT;
        game.scale.fullScreenScaleMode = Phaser.ScaleManager.EXACT_FIT;
        game.scale.setScreenSize(true);
        game.state.start('preload');
    }
};

var PreloadState = {
    preload: function() {
        if (loader){
            loaderEmpty = game.add.sprite(0, 0, 'loaderEmpty');
            loaderEmpty.x = game.world.centerX - loaderEmpty.width / 2; // Center the preload bar
            loaderEmpty.y = game.world.centerY - loaderEmpty.height / 2;
            loaderFull = game.add.sprite(0, 0, 'loaderFull');
            loaderFull.x = game.world.centerX - loaderFull.width / 2;
            loaderFull.y = game.world.centerY - loaderFull.height / 2;
            game.load.setPreloadSprite(loaderFull);  //this is all you have to do to get a nice preload screen
        }
        // gray filter 
        game.load.script('Gray', 'Gray.js');
        // font
        game.load.bitmapFont('desyrel', 'assets/fonts/font1.png', 'assets/fonts/font1.xml');
        //sounds
        //game.load.audio('clumsytheme', 'assets/sounds/clumsycowrag.ogg');
        game.load.audio('clumsycowragfast', 'assets/sounds/clumsycowragfast.ogg',false);
        game.load.audio('yeehaw', 'assets/sounds/yeehaw.ogg');
        game.load.audio('penguin1', 'assets/sounds/penguin1.ogg',false);
        game.load.audio('penguin2', 'assets/sounds/penguin2.ogg',false);
        game.load.audio('penguin3', 'assets/sounds/penguin3.ogg',false);
        game.load.audio('moo1', 'assets/sounds/cow-moo1.ogg',false);
        game.load.audio('moo2', 'assets/sounds/cow-moo2.ogg',false);
        game.load.audio('moo3', 'assets/sounds/cow-moo3.ogg',false);
        game.load.audio('moo4', 'assets/sounds/cow-moo4.ogg',false);
        game.load.audio('moo5', 'assets/sounds/cow-moo5.ogg',false);
        game.load.audio('moo6', 'assets/sounds/cow-moo6.ogg',false);
        game.load.audio('mooa1', 'assets/sounds/cow-whine1.ogg',false);
        game.load.audio('mooa2', 'assets/sounds/cow-whine2.ogg',false);
        game.load.audio('explosion', 'assets/sounds/explosion.ogg',false);
        game.load.audio('leaves', 'assets/sounds/leaves.ogg',false);
        game.load.audio('splash', 'assets/sounds/splash.ogg',false);
        game.load.audio('bumper', 'assets/sounds/bumper.ogg',false);

        //polygons
        game.load.physics('border-polygons', './assets/polygons/border.json');
        //sprites
        game.load.atlas('allsprites', 'assets/sprites/allsprites.png', 'assets/sprites/allsprites.jsona');
        game.load.image('help','assets/sprites/clumsy-cheatsheet.jpg');
    },
    create: function() {
        if (loader){        
            var tween1 = game.add.tween(loaderFull).to({ alpha: 0 }, 1000, Phaser.Easing.Linear.None, true);
            var tween2 = game.add.tween(loaderEmpty).to({ alpha: 0 }, 1000, Phaser.Easing.Linear.None, true);
            tween2.onComplete.addOnce(function() { game.state.start('menu'); });
        }
        else {
            game.state.start('menu');
        }
    }
};






/////   MENU START ////////
var Menu = {
    create: function () {
        if (!music.isPlaying){  music.play('',0,1,true); }    
        if (cowragfast && cowragfast.isPlaying){ cowragfast.stop(); } 
      //  if (!game.device.desktop){game.input.onDown.add(gofull, this);}
        if (firststart === false){game.add.existing(cover);} // add the cover for the transition effect as early as possible
        else {game.world.alpha = 0;}

        game.time.desiredFps = physicsFPS;

        setupDisplayGroups();

        //do NOT know why but i need two of them to be displayed
        ice1 = game.add.tileSprite(0,0,worldwidth,worldheight,'allsprites','ufer');
        ice = game.add.tileSprite(0,0,worldwidth,worldheight,'allsprites','ufer');

        gray = game.add.filter('Gray');

        var maxdistance = localStorage.getItem('distance');
        var numberstars = game.add.bitmapText(47, worldheight-43, 'desyrel','Your highscore: '+maxdistance+' meters', 24);
       // numberstars.fixedToCamera = true;
        var totalStarsicon = game.add.image(15,worldheight-40 ,'allsprites','star');
        totalStarsicon.scale.setTo(0.4,0.4);
      //  totalStarsicon.fixedToCamera = true;

        mainmenu=game.add.group();

        mainmenu.fixedToCamera=true;

        levelbuttons = game.add.group();
        var button = game.add.button(worldwidth/2, worldheight/2, 'allsprites', function(){if (inputBlock === true){return;}  level=0; fadeOut(level); }, this, 'button-inactive', 'button-active', 'button-active');
        button.anchor.setTo(0.5,0.5);
        button.speed = 500;
        var infoTextx = game.add.bitmapText(button.x-75,  button.y+40, 'desyrel','go on a ride' , 26);
        infoTextx.speed = 500;
        var number = game.add.bitmapText(button.x-14, button.y-40, 'desyrel','s', 50);   number.alpha = 0.8;
        levelbuttons.add(button);
        levelbuttons.add(number);


        var audiobutton1 = game.add.button(30,30, 'allsprites', function(){ 
                game.sound.mute=true;
                audiobutton1.visible = false;
                audiobutton2.visible = true;
            }, this, 'signs-audio-on', 'signs-audio-on', 'signs-audio-on');
        audiobutton1.anchor.setTo(0.5,0.5);
       // audiobutton1.fixedToCamera=true;
        audiobutton1.alpha = 0.8;

        var audiobutton2 = game.add.button(30,30, 'allsprites', function(){ 
                game.sound.mute = false;
                audiobutton2.visible = false;
                audiobutton1.visible=true;
            }, this, 'signs-audio-off', 'signs-audio-off', 'signs-audio-off');
        audiobutton2.anchor.setTo(0.5,0.5);
        //audiobutton2.fixedToCamera=true;
        audiobutton2.alpha = 0.8;
        audiobutton2.visible=false;

        if (game.sound.mute === false){
            audiobutton1.visible = true;
            audiobutton2.visible = false;
        }
        else{
            audiobutton1.visible = false;
            audiobutton2.visible = true;
        }

        //help menu
        help = game.add.image(0,0,'help');
        help.alpha = 0;

        var helpmenubutton = game.add.button(worldwidth+10,worldheight+10, 'allsprites',showHelpMenu, this, 'button-inactive', 'button-active', 'button-active');
        helpmenubutton.anchor.setTo(0.5,0.5);
        helpmenubutton.scale.setTo(2);
        helpmenubutton.angle = 120;
        icon_helpmenu = game.add.bitmapText(helpmenubutton.x-35, helpmenubutton.y-30, 'desyrel','?', 50);  
        icon_helpmenu.alpha = 0.9;
        icon_helpmenu.anchor.setTo(0.5,0.5);
        mainmenu.add(helpmenubutton);
        mainmenu.add(icon_helpmenu);
        mainmenu.fixedToCamera=true;
        
        
        function showHelpMenu(){
            if(help.alpha==0){
                inputBlock = true;
                game.add.tween(help).to({ alpha:1}, 500, Phaser.Easing.Quadratic.Out, true);
            }
        }
        game.input.onDown.add(function(){ if(help.alpha==0){return;} helptween=game.add.tween(help).to({ alpha:0}, 600, Phaser.Easing.Quadratic.Out, false); helptween.onComplete.add(function(){inputBlock = false;}); helptween.start(); }, this);

        //fade in on first start of the game
        if (firststart === true){  game.time.events.add(0, function(){  var tween = game.add.tween(game.world).to({ alpha: 1 }, 1000, Phaser.Easing.Linear.None, true); firststart = false; });}
        else { cover.bringToTop(); game.time.events.add(0, fadeIn); }
        if (fps === true){ fpsText = game.add.text(60, 40, '', { font: '16px Arial', fill: '#ffffff' } );}
    },
    update: function () {
        ice.tilePosition.y += 2;
        if (fps === true){ fpsText.setText(game.time.fps + ' FPS');}
    }
};
/////   MENU END  ////////
var Finish = {
    create: function () {
        if (cowragfast && cowragfast.isPlaying){ cowragfast.stop(); } 
        randomnumber = game.rnd.integerInRange(1,2);
        game.add.existing(cover); // add the cover for the transition effect as early as possible
        game.time.desiredFps = physicsFPS;
        ice = game.add.tileSprite(0,0,worldwidth,worldheight,'allsprites','ufer');
        game.world.setBounds(0, 0, worldwidth, worldheight);
        playergroup.destroy(true,true);

        // check and set new HIGHSCORE
        distance = Math.floor(distance);
        maxdistance = localStorage.getItem('distance');
        if (maxdistance <= distance) {  
            localStorage.setItem('distance', distance); 
        }

        if (gamestate === 'lost'){
            var infoText1 = game.add.bitmapText(game.world.centerX,  game.world.centerY-100, 'desyrel',distance+' meters', 44);
            infoText1.x = game.world.centerX - infoText1.width/2;
            button=game.add.button(game.world.centerX, game.world.centerY, 'allsprites', function(){ fadeOut('0');  }, this, 'button-inactive', 'button-active', 'button-active');
            button.anchor.setTo(0.5,0.5);
            number = game.add.sprite(button.x, button.y, 'allsprites','signs-retry');
            number.anchor.setTo(0.5,0.5);
            var textarray = [
                'that\'s good...   for a baby !',
                '         user error.  \n  please replace user and \npress any key to continue !',
                'try harder !',
                'my cat got twice as far !',
                'you can do better, right?',
                'huh.. really?',
                'run forrest... run !',
                'this game is probably not made for you',
                'may the milk be with you !',
                'you are getting better !',
                'all your base are belong to us !',
                '    always look on \nthe cow side of life!',
                'cow for president !',
                'wow.. i didn\'t expect that !',
                'are you sure you are human?',
                '      1f u r34d th1s \nu r34lly n33d t0 g37 l41d !'
            ];
            var pos = 0;
            if      (distance < 250)  {   pos = game.rnd.integerInRange(0,7);  }
            else if (distance < 500)  {   pos = game.rnd.integerInRange(8,12); }
            else                      {   pos = game.rnd.integerInRange(13,15);  }

            var infoText = game.add.bitmapText(game.world.centerX,  button.y+64, 'desyrel',''+textarray[pos]+'', 28);
            infoText.x = game.world.centerX - infoText.width/2;
        }
        cover.bringToTop();  // bring the cover for the stateTransition to top
        game.time.events.add(0, fadeIn);  // start the fade out of the cover and therefore the fade in of the current state
    },
    update: function () {
        ice.tilePosition.y += 2;
    }
};






























level0 = {
    create: function () {
        if (cover) {game.add.existing(cover);} // add the cover for the transition effect as early as possible
        setupLevelEvironment();
        setupPhysics();
        setupCollisionGroups();
        setupEmitters(); 
        setupBackground();
        setupEnemyobjects(400,1000);
        setupBumpers(300,-300);
        setupIceblocks();
        setupMines(300,-1500);
        setupPowerups('clover3',300,-650); 
        setupPowerups('star',300,-40);
        setupWaterhole(worldwidth/2,-2500, false);
        setupSpringCow();
        setupAssets('tuxice',worldwidth/2,-500);
        setupAssets('tuxbomb',worldwidth/2,-1500);
        game.physics.p2.updateBoundsCollisionGroup();  //the last thing to do after initializing the map and CGs 
        setupUI(); 
        cowragfast.play();
        cowragfast.stop();
        if (!cover){cover = game.add.image(0,0,'');}
        cover.bringToTop(); // bring the cover for the stateTransition to top
        game.time.events.add(0, fadeIn); // start the fade out of the cover and therefore the fade in of the current state
    },
    update: function () {
        if (!inputBlock &&  game.input.activePointer.isDown ){ shootAsset();  }
        if (!game.input.activePointer.isDown){ cowallow = true;}
        if (fps === true){ fpsText.setText(game.time.fps + ' FPS');  }
        if (supercow === true){ staremitter.emitX = cow.x; staremitter.emitY = cow.y;}
        
        if (game.time.time > timeDelta1) {
            ufer.tilePosition.y += slidespeed;
            uferlinks.body.y = ufer.tilePosition.y;   //make the backroundbodies scroll
            uferlinks1.body.y = ufer.tilePosition.y-ufer.height;  //make the backroundbodies scroll
            fixedobjects.forEach( moveFixedObjects ,this); // don't move invisible objects like the detonatedwaterhole   
            timeDelta1 = game.time.time + 16;
        }
         if (game.time.time > timeDelta2) {  //update meters text and check all game objects 
            timeDelta2 = game.time.time + (3840 / slidespeed);
            distance += 1;
            leveltext.text = 'meters: '+Math.floor(distance);
            movingobjects.forEach(checkKill ,this);
            fixedobjects.forEach( checkKill ,this);
            graveyard.forEach( checkReset ,this);
            if (cow.onAir == false && cow.body.y > worldheight-40){cow.body.y = worldheight - 100;} //correct position if cow out of place because of collisions
            checkLevel();
        }

    }
};



function moveFixedObjects(obj){
   if (obj && obj.body && obj.hidden !== true){ 
       obj.body.y += slidespeed;
    }
}

function checkKill(obj){
    if (obj && obj.body) { 
        if (obj.body.y > worldheight+50 && !obj.killed ){ 
            killObject(obj.body); 
        }
    }
}
function checkLevel(){
    if (distance > 300 && difficulty == 0){
        setupEnemyobjects(400,1000);
        difficulty+=1;
        setPlayerinfo("sliding to level 2");
    }
    else if (distance > 600 && difficulty == 1){
        setupPowerups('clover3',300,-50); 
        setupMines(300,-500);
        difficulty+=1;
        setPlayerinfo("level 3 - more clovers for you !");
    }
    else if (distance > 900 && difficulty == 2){
        setupIceblocks();
        setupAssets('tuxbomb',worldwidth/2,-500);
        difficulty+=1;
        setPlayerinfo("wow ! level 4");
    }
    else if (distance > 1200 && difficulty == 3){
        setupBumpers(300,-300);
        setupPowerups('clover3',300,-50); 
        difficulty+=1;
        setPlayerinfo("good luck with level 5");
    }
    else if (distance > 1500 && difficulty == 4){
        setupMines(300,-500);
        setupAssets('tuxice',worldwidth/2,-500);
        difficulty+=1;
        setPlayerinfo("no way! entering level 6");
    }
    
}

function checkReset(obj){
    if (!obj ){ return; }
    if (obj.name == 'tuxbomb'){ 
        graveyard.remove(obj); //remove immediately to avoid triggering this twice
        plantobjects.add(obj);  // ad to the above everything group but not to moving objects otherwise the object will be calculated again
        game.time.events.add(game.rnd.integerInRange(1000, 4000),resetObject,this,obj);
    }
    else if (obj.name == 'tuxice'){ 
        graveyard.remove(obj); //remove immediately to avoid triggering this twice
        plantobjects.add(obj); 
        game.time.events.add(game.rnd.integerInRange(1000, 6000),resetObject,this,obj);
    }
    else if (obj.name == 'waterhole'){ 
        graveyard.remove(obj);
        fixedobjects.add(obj);
        game.time.events.add(game.rnd.integerInRange(1000, 3500),resetObject,this,obj);
    }
    else if (obj.name == 'mine'){ 
        graveyard.remove(obj);
        fixedobjects.add(obj);
        game.time.events.add(game.rnd.integerInRange(2000, 5000),resetObject,this,obj);
    }
    else if (obj.name == 'stingi'){ 
        graveyard.remove(obj);
        fixedobjects.add(obj);
        game.time.events.add(game.rnd.integerInRange(1000, 4000),resetObject,this,obj);
    }
    else if (obj.name == 'iceblock'){ 
        graveyard.remove(obj);
        plantobjects.add(obj); 
        game.time.events.add(game.rnd.integerInRange(1000, 3000),resetObject,this,obj);
    }
    else if (obj.name == 'clover'){ 
        graveyard.remove(obj);
        plantobjects.add(obj); 
        game.time.events.add(game.rnd.integerInRange(3000, 6000),resetObject,this,obj);
    }
    else if (obj.name == 'star'){ 
        graveyard.remove(obj);
        plantobjects.add(obj); 
        game.time.events.add(game.rnd.integerInRange(40000, 70000),resetObject,this,obj);
    }
    else if (obj.name == 'bumper'){ 
        graveyard.remove(obj);
        plantobjects.add(obj); 
        game.time.events.add(game.rnd.integerInRange(3000, 6000),resetObject,this,obj);
    }
}


function landTux(obj){
    obj.sprite.onAir = false;
    obj.sprite.killed = false;
    obj.data.shapes[0].sensor = false;
    obj.setCollisionGroup(enemyGroundCG);
    obj.collides([playerCG,cowCG,groundCG,enemyGroundCG]);
    randomsound1 = game.rnd.between(1,3);
    if      (randomsound1 == 1) { penguin1.play(); }
    else if (randomsound1 == 2) { penguin2.play(); }
    else                        { penguin3.play(); }
    obj.sprite.animations.play('walk');
    movingobjects.add(obj.sprite);
}







//this handles the resetting of objects that drive by..
function resetObject(obj){
    if (obj.constraint){ // make sure no constraints are left
        game.physics.p2.removeConstraint(obj.constraint);
        obj.constraint = null;
    }
    if (obj.name == 'tuxbomb'||obj.name == 'tuxice'){
        if (randomnumber == 1){var randXs = worldwidth+200; } else { var randXs = -200; }
        var randY = game.rnd.integerInRange(0, 100);
        obj.reset(randXs,randY);
        obj.alpha = 0 ;
        obj.onAir = true;
        obj.fetched = false;
        obj.body.setCollisionGroup(nocollideCG);
        obj.body.data.gravityScale = 0.3;
        obj.scale.setTo(10);
        if (randomnumber == 1){obj.angle = -90;  }   else { obj.angle = 90;  }
        obj.resetdescent.start();
        obj.resetfade.start();
        obj.resetmove.start();
    }
    else if (obj.name == 'waterhole'){ 
        var randX = game.rnd.integerInRange(60, worldwidth-60);
        obj.reset(randX,-150); // the hidden waterhole should never catch a tux so place it obove (tux spawns at -100)
        obj.body.kinematic=true;
        obj.body.setCollisionGroup(enemyGroundCG); // need to reset because of the nocollideCG change
        obj.body.collides([playerCG,cowCG,groundCG,enemyGroundCG,powerupCG]);
        obj.body.angle = game.rnd.integerInRange(-40,40);
        obj.killed = false;
        if (obj.detonated === true){  //this is just for the waterholes caused by naval mines
            obj.hidden = true; // only visible objects are moved automatically
        }
    }
    else if (obj.name == 'mine'){ 
        var randX = game.rnd.integerInRange(60, worldwidth-60);
        if (cow.body.x < 100 || cow.body.x > worldwidth-100){ randX = cow.body.x;}
        obj.reset(randX,-40);
        obj.body.kinematic=true;
        obj.killed = false;
        obj.bringToTop();
    }
    else if (obj.name == 'stingi'){
        var randX = game.rnd.integerInRange(60,worldwidth-60);
        obj.reset(randX,-40);
        obj.body.kinematic=true;
        obj.body.angle = game.rnd.integerInRange(0,180);
        obj.killed = false;
        obj.bringToTop();
    }
    else if (obj.name == 'iceblock'){
        var randX = game.rnd.integerInRange(60,worldwidth-60);
        obj.reset(randX,-40);
        if (randX > worldwidth/2){  obj.frameName = "cowsonice-tileset-64-25";}
        else { obj.frameName = "cowsonice-tileset-64-2"; }
        obj.alpha = 1 ;
        obj.scale.setTo(obj.initialscale);
        obj.body.data.shapes[0].sensor = false;
        obj.body.angle = game.rnd.integerInRange(0,180);
        obj.body.setCollisionGroup(enemyGroundCG); // need to reset because of the nocollideCG change
        obj.body.collides([playerCG,cowCG,groundCG,enemyGroundCG,powerupCG]);
        obj.killed = false;
        obj.onAir = false;
        movingobjects.add(obj);
        obj.bringToTop();
    }
    else if (obj.name == 'bumper'){
        var randX = game.rnd.integerInRange(60, worldwidth-60);
        obj.reset(randX,-40);
        obj.alpha = 1 ;
        obj.body.data.shapes[0].sensor = false;
        obj.scale.setTo(obj.initialscale);
        obj.body.setCollisionGroup(enemyGroundCG); // need to reset because of the nocollideCG change
        obj.body.collides([playerCG,cowCG,groundCG,enemyGroundCG,powerupCG]);
        obj.killed = false;
        obj.onAir = false;
        movingobjects.add(obj);
        obj.bringToTop();
    }
    else if (obj.name == 'clover'||obj.name == 'star'){ 
        var randX = game.rnd.integerInRange(60, worldwidth-60);
        obj.reset(randX,-40);
        if (obj.name == 'clover'){
            if (randX > 300){  obj.frameName = "clover1";}
            else if (randX > 180){  obj.frameName = "clover2";}
            else { obj.frameName = "clover3"; }
        }
        obj.alpha = 1 ;
        obj.scale.setTo(obj.initialscale);
        obj.body.data.shapes[0].sensor = false;
        obj.body.setCollisionGroup(powerupCG);
        obj.body.collides([cowCG,groundCG,enemyGroundCG]);
        obj.killed = false;
        obj.onAir = false;
        movingobjects.add(obj);
        obj.bringToTop();
    }
}































function setupLevelEvironment(){
    randomnumber = game.rnd.integerInRange(1,2);
    if (!music.isPlaying){  music.play('',0,1,true); }  
   
    if (cowragfast && cowragfast.isPlaying){ cowragfast.stop(); } 
    gamestate='running';
    distance = 0;
    supercow=false;
    frozenWaterholes = 0;
    cloverartist = false;
    difficulty = 0;
    bombedboxes = 0;
    slidespeed = 8;
    
    setupDisplayGroups();
    game.camera.roundPx = false; // dont call view.floor as part of the camera update loop
    game.world.alpha = 1;
    explosion = game.add.audio('explosion',1,false);
    leaves = game.add.audio('leaves',0.4,false);
    splash = game.add.audio('splash',0.4,false);
    bumpersound = game.add.audio('bumper',0.4,false);
    penguin1 = game.add.audio('penguin1',0.4,false);
    penguin2 = game.add.audio('penguin2',0.4,false);
    penguin3 = game.add.audio('penguin3',0.4,false);
    cowwhine1 = game.add.audio('mooa1',0.2,false);
    cowwhine2 = game.add.audio('mooa2',0.2,false);
    moo1 = game.add.audio('moo1',0.2,false);
    moo2 = game.add.audio('moo2',0.2,false);
    moo3 = game.add.audio('moo3',0.2,false);
    moo4 = game.add.audio('moo4',0.2,false);
    moo5 = game.add.audio('moo5',0.2,false);
    moo6 = game.add.audio('moo6',0.2,false);
    yeehaw = game.add.audio('yeehaw',1, false);
    cowragfast = game.add.audio('clumsycowragfast',1, true);
   
}
function setupBackground(){
    ufer = game.add.tileSprite(0,0,worldwidth,worldheight,'allsprites','ufer');
    if (randomnumber == 2){
        polygon = "border-left";
        wind=game.add.sprite(worldwidth/2,worldheight-30, 'allsprites','windright');
    }
    else { 
        polygon = "border-right";
        ufer.tileScale.x=-1;
        wind=game.add.sprite(worldwidth/2,worldheight-30, 'allsprites','windleft');
    }
    wind.anchor.setTo(0.5);
    wind.alpha = 0.6;
    backgroundobjects.add(ufer);

    uferlinks = game.add.sprite(0,0,'');
    uferlinks.type = "ground";
    game.physics.p2.enable(uferlinks,false); 
    uferlinks.body.clearShapes();
    shape1=uferlinks.body.loadPolygon('border-polygons',polygon);  // load custom shape on this ramp
    uferlinks.body.x += uferlinks.width/2; // correct offset 
    uferlinks.body.data.gravityScale=0;
    uferlinks.body.kinematic=true;
    uferlinks.body.setCollisionGroup(groundCG);
    uferlinks.body.collides([playerCG,cowCG,iceBlocksCG,enemyGroundCG,powerupCG]); 
    uferlinks.body.setMaterial(woodboxMaterial); 
    uferlinks1 = game.add.sprite(0,0,'');
    uferlinks1.type = "ground";
    game.physics.p2.enable(uferlinks1,false); 
    uferlinks1.body.clearShapes();
    uferlinks1.body.loadPolygon('border-polygons',polygon);  // load custom shape on this ramp
    uferlinks1.body.x += uferlinks1.width/2; // correct offset 
    uferlinks1.body.y -= ufer.height;
    uferlinks1.body.data.gravityScale=0;
    uferlinks1.body.kinematic=true;
    uferlinks1.body.setCollisionGroup(groundCG);
    uferlinks1.body.collides([playerCG,cowCG,iceBlocksCG,enemyGroundCG,powerupCG]); 
    uferlinks1.body.setMaterial(woodboxMaterial); 

    bottom = game.add.sprite(worldwidth/2,worldheight-60,'');
    bottom.type = "ground";
    game.physics.p2.enable(bottom,false);
    bottom.body.static = true;
    bottom.body.setRectangle(worldwidth,20);
    bottom.body.setCollisionGroup(bottomCG);
    bottom.body.collides(cowCG);
}
function setupDisplayGroups(){
    stageGroup = game.add.group(); // this group will contain everything except the UI for scaling
    graveyard = game.add.group();
    uiobjects = game.add.group();
    backgroundobjects = game.add.group();
    fixedobjects = game.add.group();
    movingobjects=game.add.group();
    playergroup = game.add.group();
    plantobjects = game.add.group(); // this group is used as "above everything group"

    stageGroup.add(backgroundobjects);
    stageGroup.add(fixedobjects);
    stageGroup.add(movingobjects);
    stageGroup.add(playergroup);
    stageGroup.add(plantobjects);  
}
function setupUI(){
    mainmenu=game.add.group();
    var menubutton = game.add.button(worldwidth+10,-10, 'allsprites', showIngameMenu, this, 'button-inactive', 'button-active', 'button-active');
    menubutton.anchor.setTo(0.5,0.5);
    menubutton.scale.setTo(2);
    menubutton.angle = 120;
    icon_menu = game.add.sprite(menubutton.x-35, menubutton.y+30, 'allsprites', 'signs-menu');   
    icon_menu.alpha = 0.9;
    icon_menu.anchor.setTo(0.5,0.5);

    
    menu=game.add.group();
    menubutton1 = game.add.button(worldwidth+100,150, 'allsprites', function(){ level="menu"; fadeOut(level);   }, this, 'button-inactive', 'button-active', 'button-active');
    menubutton1.anchor.setTo(0.5,0.5);
    icon_retry = game.add.sprite(menubutton1.x, menubutton1.y,'allsprites', 'signs-logout');   icon_retry.alpha = 0.8;
    icon_retry.anchor.setTo(0.5,0.5);
    menubutton1.speed = 200; icon_retry.speed = 200;  //determins the menu-move-in-speed
    icon_retry.angle = 180;
    menu.add(menubutton1);

    menu.add(icon_retry);
    menu.fixedToCamera=true;
    menu.visible=false;

    if (randomnumber == 2){
        cowpushbutton = game.add.button(worldwidth+70,worldheight-120, 'allsprites',pushCow, this,'button-inactive', 'button-active', 'button-active');
        cowjumpbutton = game.add.button(-70,worldheight-120, 'allsprites',jumpCow, this,'button-inactive', 'button-active', 'button-active');
        icon_pushcow = game.add.sprite(cowpushbutton.x-35, cowpushbutton.y,'allsprites', 'signs-forward');  
        icon_cowjump = game.add.sprite(cowjumpbutton.x+35, cowjumpbutton.y,'allsprites', 'signs-forward');   
        icon_pushcow.angle = -180;
        icon_cowjump.angle = -90;
    }
    else {
        cowpushbutton = game.add.button(worldwidth+70,worldheight-120, 'allsprites',jumpCow, this,'button-inactive', 'button-active', 'button-active');
        cowjumpbutton = game.add.button(-70,worldheight-120, 'allsprites',pushCow, this,'button-inactive', 'button-active', 'button-active');
        icon_pushcow = game.add.sprite(cowpushbutton.x-35, cowpushbutton.y,'allsprites', 'signs-forward');  
        icon_cowjump = game.add.sprite(cowjumpbutton.x+35, cowjumpbutton.y,'allsprites', 'signs-forward');  
        icon_pushcow.angle = -90;
        icon_cowjump.angle = 0;
    }

    cowpushbutton.anchor.setTo(0.5,0.5);
    cowpushbutton.scale.setTo(2);
    cowpushbutton.angle = 120;
    cowpushbutton.alpha = 0;
    cowpushbutton.onInputDown.add(blockInput);
    cowpushbutton.onInputUp.add(unblockInput); 

    icon_pushcow.alpha = 0;
    icon_pushcow.anchor.setTo(0.5,0.5);

    cowjumpbutton.anchor.setTo(0.5,0.5);
    cowjumpbutton.scale.setTo(2);
    cowjumpbutton.angle = -60;
    cowjumpbutton.alpha = 0;
    cowjumpbutton.onInputDown.add(blockInput);
    cowjumpbutton.onInputUp.add(unblockInput); 

    icon_cowjump.alpha = 0; 
    icon_cowjump.anchor.setTo(0.5,0.5);

    mainmenu.add(menubutton);
    mainmenu.add(icon_menu);
    mainmenu.add(cowpushbutton);
    mainmenu.add(cowjumpbutton);
    mainmenu.add(icon_pushcow);
    mainmenu.add(icon_cowjump);
    mainmenu.fixedToCamera=true;

    leveltext = game.add.bitmapText(20,  10, 'desyrel','- 0 -', 26);
    leveltext.fixedToCamera = true;

    playerinfo = game.add.bitmapText(worldwidth/2,  worldheight/2, 'desyrel',"don\'t eat so much - you\'ll get fat", 24);
    playerinfo.anchor.setTo(0.5,0.5);
    playerinfo.fixedToCamera = true;  playerinfo.alpha = 0;

    // add everything to the non zoomable UI
    uiobjects.add(mainmenu);
    uiobjects.add(playerinfo);
    uiobjects.add(leveltext);

    if (fps===true){    game.time.advancedTiming = true; fpsText = game.add.text(60, 40, '', { font: '16px Arial', fill: '#ffffff' } ); fpsText.fixedToCamera = true;  }
}

function blockInput(){inputBlock=true;} 
function unblockInput(){inputBlock=false; cowallow = true;}   // be carefull with cowallow.. it wasnt on the jumpbutton first for a reason
function showIngameMenu(){
    if (menu.visible == true) { 
        menu.forEach(
            function(object){ 
                var tween = game.add.tween(object).to({ x:'+100' }, object.speed, Phaser.Easing.Quadratic.In, true);  
                game.time.events.add(300,function(){ menu.visible=false;   } );
            }
        );
        var tween = game.add.tween(icon_menu).to({ angle:'+90' }, menubutton1.speed, Phaser.Easing.Quadratic.Out, true); 
    }
    else { 
        menu.visible=true; 
        var tween = game.add.tween(icon_menu).to({ angle:'-90' }, menubutton1.speed, Phaser.Easing.Quadratic.Out, true);  

        menubutton1.x = worldwidth+40; 
        icon_retry.x = worldwidth+40; 
        var tween1 = game.add.tween(menubutton1).to({ x:'-100' }, menubutton1.speed, Phaser.Easing.Quadratic.Out, true);  
        tween1.start();

        var tween2 = game.add.tween(icon_retry).to({ x:'-100' }, icon_retry.speed, Phaser.Easing.Quadratic.Out, true);  
        tween2.onComplete.add(function(){  game.input.onDown.add(unpause, self);game.paused = true; });
        tween2.start();
    }
}








function setupPhysics(){                        //start physics system and enable "expensive" collision events
    game.physics.startSystem(Phaser.Physics.P2JS);
 //   game.physics.p2.setImpactEvents(true);  //heavy ? should we go there?
    game.physics.p2.defaultRestitution = 0.2 ;
    game.physics.p2.gravity.y = 300;
    game.world.enableBodySleeping=true;  //allow bodies to not get calculated when there is nothing to do
//  game.physics.p2.applyDamping = false;  // not needed for spring damping but still nice for the cow to slow down 
    game.physics.p2.applyGravity = true;
    game.physics.p2.applyFriction = false;  //top down game without much contact between objects.. no need for friction
    game.physics.p2.world.narrowphase.enableFriction = false;
    game.physics.p2.setImpactEvents(true);
    game.physics.p2.setImpactEvents(false);
    game.physics.p2.emitImpactEvent = false;
    game.canvas.oncontextmenu = function (e) { e.preventDefault();}; //prevent rightclick contextmenu
    bumperMaterial = game.physics.p2.createMaterial();
    playerMaterial = game.physics.p2.createMaterial();
    woodboxMaterial = game.physics.p2.createMaterial();
    game.physics.p2.createContactMaterial(bumperMaterial, playerMaterial, { restitution: 1.4 });
    game.physics.p2.createContactMaterial(bumperMaterial, woodboxMaterial, { restitution: 1.4 });
    game.time.desiredFps = physicsFPS;
    game.physics.p2.world.on("preSolve", onPresolve); // only use onpresolve if there are ramps in the level
     if (randomnumber == 1){ game.physics.p2.gravity.x = -xGravity;  }
     else {  game.physics.p2.gravity.x = xGravity;}
}
function setupCollisionGroups() {
    //for worldbounds to work the collision groups have to be defined after resizeWorld and setBoundsToWorld
    enemyGroundCG = game.physics.p2.createCollisionGroup();

    groundCG = game.physics.p2.createCollisionGroup();
    playerCG = game.physics.p2.createCollisionGroup();
    cowCG = game.physics.p2.createCollisionGroup();
    bottomCG = game.physics.p2.createCollisionGroup();
    powerupCG = game.physics.p2.createCollisionGroup();
    iceBlocksCG = game.physics.p2.createCollisionGroup();
  
    nocollideCG = game.physics.p2.createCollisionGroup(); // dummy class if something should not collide with anything
}
function setupEmitters(){
    smokeemitter = game.add.emitter(0, 0, 4);
    smokeemitter.makeParticles('allsprites',['smoke2','smoke-puff']);
    smokeemitter.setAlpha(1, 0,6000,Phaser.Easing.Quintic.Out);
    smokeemitter.setScale(0.5, 1, 0.5, 1, 6000, Phaser.Easing.Quintic.Out);
    smokeemitter.setRotation(0,0);
    smokeemitter.gravity = 0;

    glassemitter = game.add.emitter(0, 0, 4);
    glassemitter.makeParticles('allsprites','glass');
    glassemitter.setScale(0.2, 0.5);
    glassemitter.gravity=0;

    woodemitter = game.add.emitter(0, 0, 4);
    woodemitter.makeParticles('allsprites',['wood1','wood2','wood3']);
    woodemitter.setScale(0.2, 0.9);
    woodemitter.setAlpha(1, 0,4000,Phaser.Easing.Quintic.Out);
    woodemitter.gravity=0;

    fireemitter = game.add.emitter(0, 0, 4);
    fireemitter.makeParticles('allsprites',['flash1','flash2']);
    fireemitter.setRotation(0,180);
    fireemitter.gravity=0;

    dropsemitter = game.add.emitter(0, 0, 10);
    dropsemitter.makeParticles('allsprites',['drop1','drop2','drop3','drop2','drop5','drop6','drop7']);
    dropsemitter.setAlpha(1, 0, 8000, Phaser.Easing.Quadratic.Out);
    dropsemitter.setScale(0.5,1.3,0.5,1.3,1000, Phaser.Easing.Quadratic.InOut,true);
    dropsemitter.setRotation(0, 90);
    dropsemitter.setSize(10, 10);
    dropsemitter.gravity=500;

    cloveremitter = game.add.emitter(0, 0, 5);
    cloveremitter.makeParticles('allsprites',['clover1','clover2','clover3']);
    cloveremitter.setRotation(0,180);
    cloveremitter.setScale(0.2, 0.9,2000, Phaser.Easing.Quadratic.InOut,false);
    cloveremitter.setAlpha(1, 0, 4000, Phaser.Easing.Quadratic.Out);
    cloveremitter.gravity=0;

    staremitter = game.add.emitter(0, 0, 10);
    staremitter.makeParticles('allsprites',['star']);
    staremitter.setRotation(0,180);
    staremitter.setScale(0.2,1.3,0.2,1.3,2000, Phaser.Easing.Linear.None,false);
    staremitter.setAlpha(1, 0, 4000, Phaser.Easing.Quadratic.Out);
    staremitter.gravity=600;
    
    plantobjects.add(fireemitter);
    plantobjects.add(glassemitter);
    plantobjects.add(smokeemitter);
    plantobjects.add(woodemitter);
    plantobjects.add(dropsemitter); // add this to a group above waterobjects but below others
    plantobjects.add(cloveremitter);
}


















function setupSpringCow(){
    //create springcow
    cow = game.add.sprite(worldwidth/2, worldheight-200,'allsprites','cow-spritesheet-01');
    cow.health= cow_initial_health;
    cow.name = 'springcow';
    cow.invincibleTimer=0;
    cow.animations.add('hit', ['cow-spritesheet-01','cow-spritesheet-02','cow-spritesheet-03','cow-spritesheet-04','cow-spritesheet-05','cow-spritesheet-06','cow-spritesheet-01','cow-spritesheet-02','cow-spritesheet-03','cow-spritesheet-04','cow-spritesheet-05','cow-spritesheet-06','cow-spritesheet-01'], 10, false);
    cow.animations.add('die', ['cow-spritesheet-01','cow-spritesheet-02','cow-spritesheet-03','cow-spritesheet-04','cow-spritesheet-05','cow-spritesheet-06'], 10, true);
    cow.animations.add('slide', ['cow-spritesheet-07','cow-spritesheet-08','cow-spritesheet-07','cow-spritesheet-07','cow-spritesheet-07','cow-spritesheet-07','cow-spritesheet-07','cow-spritesheet-09','cow-spritesheet-08','cow-spritesheet-08','cow-spritesheet-08'], 2, true);
    cow.initialscale = 0.7;
    cow.scale.setTo(cow.initialscale,cow.initialscale);
    game.physics.p2.enable(cow,false);
    cow.body.setCircle(25);
    cow.body.x+=cow.width/2-24;
    cow.body.y+=cow.height/2+24;
    cow.body.mass=1;
    cow.killed = false;
    cow.onAir=false;
    cow.animations.play('slide');
    cow.body.setCollisionGroup(cowCG);
    cow.body.collides([groundCG,enemyGroundCG,playerCG,bottomCG,powerupCG]);
    cow.body.setMaterial(playerMaterial);
    cow.body.onBeginContact.add(springCowCollision, cow.body);
    cow.healthbar = game.add.tileSprite(10,game.camera.height-40,41*cow.health,32,'allsprites','cowhead');
    cow.healthbar.alpha  = 0.8;
    cow.healthbar.visible = true;
    cow.constraintcount = 0;
    
    cow.jumpascent = game.add.tween(cow.scale).to({x:1.4,y:1.4}, 600, Phaser.Easing.Sinusoidal.Out);
    cow.jumpdescent = game.add.tween(cow.scale).to({x:cow.initialscale,y:cow.initialscale}, 400, Phaser.Easing.Sinusoidal.In);
    cow.jumpdescent.onComplete.add(jumpDescentResetOnFinish,this);
   
    uiobjects.add(cow.healthbar);
    playergroup.add(cow);
}
function jumpDescentResetOnFinish(){
    cow.onAir = false; 
    cow.body.data.gravityScale = 1; 
    if(cow.asset){ cow.asset.sprite.onAir = false; cow.asset.data.gravityScale = 0.3; }
}


function setupAssets(tuxtype,x,y){
    if (tuxtype == 'tuxbomb'){        helpertux = game.add.sprite(x, y,'allsprites','tuxbomb-spritesheet-1');   }
    else if (tuxtype == 'tuxice'){        helpertux = game.add.sprite(x, y,'allsprites','tuxice-spritesheet-1');   }
    helpertux.name = tuxtype;
    helpertux.type = tuxtype;
    helpertux.onAir=false;
    helpertux.killed = false;
    helpertux.fetched = false;
    helpertux.iFrame = helpertux.frameName;
    helpertux.initialscale = 0.5;
    helpertux.scale.setTo(helpertux.initialscale,helpertux.initialscale);
    var tuxanimation=helpertux.animations.add('walk', [helpertux.name+'-spritesheet-1',helpertux.name+'-spritesheet-2',helpertux.name+'-spritesheet-3',helpertux.name+'-spritesheet-4',helpertux.name+'-spritesheet-5',helpertux.name+'-spritesheet-6',helpertux.name+'-spritesheet-7',helpertux.name+'-spritesheet-6',helpertux.name+'-spritesheet-5',helpertux.name+'-spritesheet-4',helpertux.name+'-spritesheet-3',helpertux.name+'-spritesheet-2'], 30, true);
    game.physics.p2.enable(helpertux,false);
    helpertux.body.setCircle(18);
    helpertux.body.x+=helpertux.width/2+6;
    helpertux.body.y+=helpertux.height/2+6;
    helpertux.body.mass =  1;
    helpertux.body.setCollisionGroup(playerCG);
    helpertux.body.collides([groundCG,enemyGroundCG,playerCG,cowCG]);
    helpertux.body.setMaterial(playerMaterial);
    helpertux.body.onBeginContact.add(helperTuxCollision, helpertux.body);
    helpertux.body.collideWorldBounds = false;
    if (randomnumber == 1){   helpertux.angle = -90;}else{helpertux.angle = 90;}
    helpertux.body.data.gravityScale = 0.3;
    helpertux.body.fixedRotation = true;
    helpertux.animations.play('walk');
    
    helpertux.jumpascent = game.add.tween(helpertux.scale).to({x:1.4,y:1.4}, 600, Phaser.Easing.Sinusoidal.Out);
    helpertux.jumpdescent = game.add.tween(helpertux.scale).to({x:helpertux.initialscale,y:helpertux.initialscale}, 400, Phaser.Easing.Sinusoidal.In);
   
    helpertux.resetdescent = game.add.tween(helpertux.scale).to({x:helpertux.initialscale,y:helpertux.initialscale}, 1400, Phaser.Easing.Sinusoidal.Out,true);
    helpertux.resetfade = game.add.tween(helpertux).to({alpha:1}, 1400, Phaser.Easing.Sinusoidal.Out,true);
    helpertux.resetmove = game.add.tween(helpertux.body).to({x:game.rnd.integerInRange(200, worldwidth-200)}, 1400, Phaser.Easing.Linear.None,false);
    helpertux.resetmove.onComplete.add(landTux,helpertux.body);
    
    
    helpertux.explodeascent = game.add.tween(helpertux.scale).to({x:20,y:20}, 2000, Phaser.Easing.Sinusoidal.Out);  // zoom out and fade away
    helpertux.explodefade = game.add.tween(helpertux).to({alpha:0}, 2000, Phaser.Easing.Sinusoidal.Out);
    helpertux.explodefade.onComplete.add(getBackOnAirObject , helpertux.body)
    
    
    helpertux.drownfade = game.add.tween(helpertux).to( { alpha: 0 }, 300, Phaser.Easing.Linear.None, false, 0, 0, false);
    helpertux.drownscale = game.add.tween(helpertux.scale).to( { x: 0.2 , y:0.2}, 500, Phaser.Easing.Linear.None, false, 0, 0, false);
    helpertux.drownscale.onComplete.add(removeConstraintAfterDrowning,helpertux);
    
    movingobjects.add(helpertux);  // this is just for the reset if left worldbounds
}

function removeConstraintAfterDrowning(){
    game.physics.p2.removeConstraint(this.constraint);
    this.constraint = null;
   
}


function setupIceblocks(){
    iceblock = game.add.sprite(240,-200,'allsprites','cowsonice-tileset-64-2');
    iceblock.initialscale = 1;
    iceblock.scale.setTo(iceblock.initialscale,iceblock.initialscale);
    game.physics.p2.enable(iceblock,false);
    iceblock.name = 'iceblock'; 
    iceblock.body.x+=iceblock.width/2;
    iceblock.body.y+=iceblock.height/2;
    iceblock.body.static = false;
    iceblock.body.damping=0.99;
    iceblock.body.angularDamping=0.6;
    iceblock.body.enemy = true;
    iceblock.killed = false;
    iceblock.type = 'movingblock';
    iceblock.body.setMaterial(woodboxMaterial);  // give it the playerMaterial so it can bounce of bumperMaterial when in motion (for woodboxes mostly)
    iceblock.body.setCollisionGroup(enemyGroundCG);
    iceblock.body.collides([playerCG,cowCG,groundCG,enemyGroundCG,powerupCG]); // because iceblocks can be movable they should collide
    iceblock.body.collideWorldBounds = false;
    
    iceblock.explodeascent = game.add.tween(iceblock.scale).to({x:20,y:20}, 2000, Phaser.Easing.Sinusoidal.Out);  // zoom out and fade away
    iceblock.explodefade = game.add.tween(iceblock).to({alpha:0}, 2000, Phaser.Easing.Sinusoidal.Out);
    iceblock.explodefade.onComplete.add(getBackOnAirObject , iceblock.body)
    
    iceblock.drownfade = game.add.tween(iceblock).to( { alpha: 0 }, 300, Phaser.Easing.Linear.None, false, 0, 0, false);
    iceblock.drownscale = game.add.tween(iceblock.scale).to( { x: 0.2 , y:0.2}, 500, Phaser.Easing.Linear.None, false, 0, 0, false);
    iceblock.drownscale.onComplete.add(removeConstraintAfterDrowning,iceblock);
    
    movingobjects.add(iceblock);
}
function setupPowerups(type,x,y){
    powerup = game.add.sprite(x, y, 'allsprites',type);
    powerup.initialscale = 0.7;
    powerup.scale.setTo(powerup.initialscale,powerup.initialscale);
    game.physics.p2.enable(powerup,false); 
    if (type == 'clover3'){    
        powerup.name = 'clover'; 
        powerup.body.damping=0.95;
    }
    else if(type == 'star'){
        powerup.name = 'star';
        powerup.body.damping=0.8;
    }
    powerup.body.setCircle(10);
    powerup.body.x+=powerup.width/2;
    powerup.body.y+=powerup.height/2;
    powerup.body.angularDamping=0.999;
    powerup.body.mass=0.001;  //don't block the player - ever
    powerup.body.setMaterial(woodboxMaterial);  // give it the woodboxMaterial so it can bounce of bumperMaterial
    powerup.body.setCollisionGroup(powerupCG);
    powerup.body.collides([cowCG,groundCG,enemyGroundCG,powerupCG]);
    powerup.body.collideWorldBounds = false;
    powerup.killed = false;
    powerup.type = 'powerup';
    powerup.drownfade = game.add.tween(powerup).to( { alpha: 0 }, 300, Phaser.Easing.Linear.None, false, 0, 0, false);
    powerup.drownscale = game.add.tween(powerup.scale).to( { x: 0.2 , y:0.2}, 500, Phaser.Easing.Linear.None, false, 0, 0, false);
    powerup.drownscale.onComplete.add(removeConstraintAfterDrowning,powerup);
    movingobjects.add(powerup);
}
function setupEnemyobjects(x,y){
    stingblock = game.add.sprite(x,y,'allsprites','cowsonice-tileset-64-17');
    stingblock.initialscale = 1;
    stingblock.scale.setTo(stingblock.initialscale,stingblock.initialscale);
    game.physics.p2.enable(stingblock,false);
    stingblock.type = 'fixed';
    stingblock.body.angle = game.rnd.integerInRange(0,180);
    stingblock.body.kinematic=true;
    stingblock.name = 'stingi'; 
    stingblock.body.enemy = true;  // so cowhit recognizes this as enemy
    stingblock.killed = false;
    stingblock.body.setCircle(16);
    stingblock.body.setMaterial(woodboxMaterial);  // give it the woodboxMaterial so it can bounce of bumperMaterial when in motion (for woodboxes mostly and for new enemyobjects)
    stingblock.body.setCollisionGroup(enemyGroundCG);
    stingblock.body.collides([playerCG,cowCG,groundCG,enemyGroundCG,powerupCG]); // because iceblocks can be movable they should collide
    stingblock.body.collideWorldBounds = false;
    fixedobjects.add(stingblock);
}
function setupBumpers(x,y){
    bumper = game.add.sprite(x,y, 'allsprites','bumper-spritesheet-1');
    game.physics.p2.enable(bumper,false); 
    bumper.initialscale = 1;
    bumper.scale.setTo(bumper.initialscale,bumper.initialscale);
    bumper.animations.add('hit', ['bumper-spritesheet-1','bumper-spritesheet-2','bumper-spritesheet-3','bumper-spritesheet-4','bumper-spritesheet-3','bumper-spritesheet-2','bumper-spritesheet-1','bumper-spritesheet-2','bumper-spritesheet-3','bumper-spritesheet-2','bumper-spritesheet-1'], 30, false);
    bumper.name = 'bumper'; 
    bumper.body.enemy = true;  // so cowhit recognizes this as enemy
    bumper.killed = false;
    bumper.type = 'bumper';
    bumper.body.setCircle(15);
    bumper.body.collideWorldBounds = false;
    bumper.body.setCollisionGroup(enemyGroundCG);
    bumper.body.collides([playerCG,cowCG,groundCG,enemyGroundCG,powerupCG]);
    bumper.body.setMaterial(bumperMaterial);
    bumper.body.onBeginContact.add( playBumperAnim ,bumper);
    bumper.explodeascent = game.add.tween(bumper.scale).to({x:20,y:20}, 2000, Phaser.Easing.Sinusoidal.Out);  // zoom out and fade away
    bumper.explodefade = game.add.tween(bumper).to({alpha:0}, 2000, Phaser.Easing.Sinusoidal.Out);
    bumper.explodefade.onComplete.add(getBackOnAirObject , bumper.body)
    bumper.drownfade = game.add.tween(bumper).to( { alpha: 0 }, 300, Phaser.Easing.Linear.None, false, 0, 0, false);
    bumper.drownscale = game.add.tween(bumper.scale).to( { x: 0.2 , y:0.2}, 500, Phaser.Easing.Linear.None, false, 0, 0, false);
    bumper.drownscale.onComplete.add(removeConstraintAfterDrowning,bumper);
    movingobjects.add(bumper);
}
function playBumperAnim(){
    this.animations.play('hit');
    bumpersound.play();
}

function setupMines(x,y){
    mine = game.add.sprite(x,y, 'allsprites','watermine');
    mine.initialscale = 1;
    mine.type='fixed';
    game.physics.p2.enable(mine,false); 
    mine.name = 'mine'; 
    mine.body.setCircle(15);
    mine.body.x+=mine.width/2;
    mine.body.y+=mine.height/2;
    mine.body.kinematic=true;
    mine.killed = false;
    mine.body.setCollisionGroup(enemyGroundCG);
    mine.body.collides([playerCG,cowCG,groundCG,enemyGroundCG]);
    mine.body.onBeginContact.add(mineCollision,mine.body);
    setupWaterhole(worldwidth/2,-2000, true, mine); // every mine gets it's own waterhole
    fixedobjects.add(mine);
}
function setupWaterhole(x,y,detonated,thisMine){
    waterhole = game.add.sprite(x, y, 'allsprites','waterhole-2');
    waterhole.iFrame = waterhole.frameName;  //store the initial frame here for later used
    waterhole.initialscale = 1;
    game.physics.p2.enable(waterhole,false); 
    waterhole.body.angle = 0;
    waterhole.alpha = 0.5;
    waterhole.type='fixed';
    waterhole.detonated = detonated;
    waterhole.name = 'waterhole';
    // correct position of body and sprite 
    waterhole.pivot.x = 12; 
    waterhole.pivot.y = 24;  
    waterhole.body.setCircle(30);
    waterhole.body.data.gravityScale=0;
    waterhole.body.kinematic=true;
    waterhole.killed = false;
    waterhole.body.setCollisionGroup(enemyGroundCG);
    waterhole.body.collides([playerCG,cowCG,groundCG,enemyGroundCG,powerupCG]);  // tuxes and cows are in the playerCG
    waterhole.body.onBeginContact.add(waterholeCollision, waterhole.body);
    //plant a new image above the waterhole to invoke the freeze animation on
    waterhole.freeze = game.add.sprite(waterhole.x-waterhole.width/2-12,waterhole.y-waterhole.height/2-12, 'allsprites','freeze2');  //needs correction because of the pivot - could pivot freeze image to instead to be more flexible  
    waterhole.freeze.animations.add('freeze', ['freeze2','freeze3','freeze4','freeze5','freeze6','freeze7'], 15, false);
    waterhole.freeze.name = 'freeze';
    game.physics.p2.enable(waterhole.freeze,false); 
    waterhole.freeze.body.kinematic = true;
    if (!detonated){
        fixedobjects.add(waterhole);
    }
    else{
        waterhole.hidden = true;
        thisMine.detonatedwaterhole = waterhole;
        fixedobjects.add(thisMine.detonatedwaterhole);
    }
}




























































//----------------------------------//
//      COLLISION HANDLER           //
//----------------------------------//

function helperTuxCollision(object){  //run on EVERY sort of collision with tux.. so be prepared
    var asset = this;
    if (!object || (object && !object.sprite)  ||  asset.sprite.onAir === true    ){return;}
    if ( asset.sprite.name == 'tuxbomb' && (object.sprite.name == "iceblock" || object.sprite.name == "enemyobject" ) ){
        explosion.play();
        if (cow.spring && cow.asset == asset){
            game.physics.p2.removeConstraint(cow.spring);
            cow.asset=null;
            cow.spring = null;
            game.time.events.add(500, function(){  cow.constraintcount = 0;   });
        }
        fireemitter.x = asset.x;
        fireemitter.y = asset.y;
        fireemitter.explode(200, 1);
        smokeemitter.x = asset.x;
        smokeemitter.y = asset.y;
        smokeemitter.explode(1400, 1);
        if (object.sprite && object.sprite.frameName == "cowsonice-tileset-64-25"){
            woodemitter.x = object.x;
            woodemitter.y = object.y;
            woodemitter.explode(400,2);
            if (asset.sprite.fetched === true){bombedboxes++;}
            killObject(object);
        }
        else if (object.sprite && object.sprite.frameName == "cowsonice-tileset-64-2"){
            glassemitter.x = object.x;
            glassemitter.y = object.y;
            glassemitter.explode(400,2);
            if (asset.sprite.fetched === true){bombedboxes++;}
            killObject(object);
        }
        if (asset.sprite) { killObject(asset); }
    }
}


function springCowCollision(object){
    var player = this;
    if (!object ||  player.sprite.onAir === true || (!object.sprite && object.enemy !== true) || player.sprite.invincibleTimer-500 > game.time.time){return;}
       //make absolutely sure that this weird bug gets killed (worked around) immediately if detected
    if (supercow && !normalizeCowevent){
        console.log('why the fuck');
        normalizeCowevent = game.time.events.add(1000, normalizeCow, this, 'star');
    }
   // FETCH ASSETS
    if (object.sprite &&  (object.sprite.type === 'tuxice' || object.sprite.type === 'tuxbomb') && cow.constraintcount === 0){
        penguin1.play();
        object.sprite.fetched = true;
        object.mass = 0.01;
        object.sprite.animations.stop();
        movingobjects.remove(object.sprite);  //wozu hab ich das getan? 
        playergroup.add(object.sprite);
        cow.constraintcount = 1;
        cow.asset = object;
        cow.spring   = game.physics.p2.createRevoluteConstraint(player, [30,0], object, [0,0], 10000); 
        cow.spring.collideConnected = false;
        
    }
    else if ( object.enemy === true && supercow === false){
        if ( player.sprite.health > 0){ player.sprite.healthbar.width-=41;}
        player.sprite.health -= 1;
        player.sprite.invincibleTimer=game.time.time+1000;
        player.sprite.animations.play('hit').onComplete.add(function(){player.sprite.animations.play('slide');});
        randomsound2 = game.rnd.between(1,2);
        if (randomsound2 == 1)  { cowwhine1.play(); }
        else                    { cowwhine2.play(); }
        if (player.sprite.health === 0 ) { 
            gamestate = 'lost'; 
            player.sprite.animations.play('die'); 
            game.time.events.add(Phaser.Timer.SECOND * 2, function(){fadeOut('finish');});
        }
    }
    else if (object.sprite &&  object.sprite.name === 'clover'){
        leaves.play();
        cloveremitter.x = object.x;
        cloveremitter.y = object.y;
        cloveremitter.explode(2000, 4);
        if (supercow === false){  // if already invincible skip this
            player.sprite.tint= 0xccff00;
            supercow = true;
            game.time.events.remove(normalizeCowevent);
            normalizeCowevent = game.time.events.add(2000, normalizeCow, this, 'clover');
        }
        if (player.sprite.health < 6){
            player.sprite.healthbar.width += 41;
            player.sprite.health += 1;
        }else{
            cloverartist = true;
            player.sprite.healthbar.alpha  = 0.8;
            game.add.tween(player.sprite.healthbar).to({alpha:0}, 2000, Phaser.Easing.Sinusoidal.Out,true,0,0,true);
            setPlayerinfo("don\'t eat so much - you\'ll get fat");
        }
        killObject(object);
    }
    else if (object.sprite &&  object.sprite.name === 'star' && supercow === false){
        supercow = true;
        game.time.events.remove(normalizeCowevent);  // get rid of possible normalizeCowevent initialized by clovers immediately
        normalizeCowevent = game.time.events.add(16000, normalizeCow, this, 'star');
        staremitter.x = object.x;
        staremitter.y = object.y;
        //staremitter.explode(2000, 4);
        staremitter.start(false,500, 200);
        player.sprite.tint= 0xfff714;
        yeehaw.play();
        music.fadeOut(500);
        cowragfast.fadeIn(500);
        slidespeed = 16;
        setPlayerinfo("COWABUNGA !");
       
        killObject(object);
    }
    
}

// displays an info text in the center of the screen.. 
function setPlayerinfo(content){
    playerinfo.alpha = 0
    playerinfo.text = content;
    game.add.tween(playerinfo).to({alpha:1}, 2000, Phaser.Easing.Sinusoidal.Out,true,0,0,true);
}


function normalizeCow(reason){
    staremitter.kill();
    cow.tint = 0xffffff;
    supercow = false;
    slidespeed = 8;
    if (reason == 'star'){
        cowragfast.fadeOut(500);
        music.fadeIn(500,true);
    }
}

function waterholeCollision(object){
    var Awaterhole = this;
    if (!object || !object.sprite || object.sprite.onAir === true ){return;}
    // FETCH ASSETS
    else {
        if (cow.spring && cow.asset == object){  // if the asset that is drowning is the asset connected to the cow by cowspring
            game.physics.p2.removeConstraint(cow.spring);
            cow.asset = null;
            cow.spring = null;
            game.time.events.add(500, function(){  cow.constraintcount = 0;   });
        }
        drownInWater(Awaterhole, object);
    }
}



function mineCollision(object){
    var thisMine = this;  //dirty hack..  gets mine.body as context
    if (!object || !object.sprite || object.sprite.onAir === true ){return;}

    else if (object.sprite.name == 'springcow' && supercow == false){
        playergroup.remove(object.sprite);  //get player on top of everything
        plantobjects.add(object.sprite);
        object.sprite.onAir = true;
        explosion.play();
        loseAsset('mine'); //if any
        fireemitter.x = object.x;
        fireemitter.y = object.y;
        fireemitter.explode(200, 1);
        smokeemitter.x = object.x;
        smokeemitter.y = object.y;
        smokeemitter.explode(1400, 2);
        glassemitter.x = object.x;
        glassemitter.y = object.y;
        glassemitter.explode(400,2);

        thisMine.sprite.detonatedwaterhole.hidden = false; // get the hidden whaterhole and make it visible here
        thisMine.sprite.detonatedwaterhole.body.x = thisMine.x;
        thisMine.sprite.detonatedwaterhole.body.y = thisMine.y;
        killObject(thisMine);

        randomsound2 = game.rnd.between(1,6);
        if      (randomsound2 == 1){ moo1.play(); }
        else if (randomsound2 == 2){ moo2.play(); }
        else if (randomsound2 == 3){ moo3.play(); }
        else if (randomsound2 == 4){ moo4.play(); }
        else if (randomsound2 == 5){ moo5.play(); }
        else if (randomsound2 == 6){ moo6.play(); }

        object.data.shapes[0].sensor = true;

        ////////////////////////////  reuse this tween (not really neccesary)
        var ascent = game.add.tween(object.sprite.scale).to({x:20,y:20}, 2000, Phaser.Easing.Sinusoidal.Out);
        var fade = game.add.tween(object.sprite).to({alpha:0}, 3000, Phaser.Easing.Sinusoidal.Out);
        ascent.onComplete.addOnce(function(){ 
            object.sprite.health = 0; 
            gamestate = 'lost';
            game.time.events.add(Phaser.Timer.SECOND * 2, function(){fadeOut('finish');} );
        });
        ///////////////////////////
        ascent.start();
        fade.start();
    }
    else if (object.sprite.name == 'bumper'||object.sprite.name == 'iceblock'||object.sprite.name == 'tuxice'||object.sprite.name == 'tuxbomb'){
        movingobjects.remove(object.sprite); // delete frome moving objects immediately so it will not get killed because its out ouf bounds
        plantobjects.add(object.sprite); //get player on top of everything
        if (cow.spring && cow.asset == object){
            game.physics.p2.removeConstraint(cow.spring);
            cow.asset = null;
            cow.spring = null;
            game.time.events.add(500, function(){  cow.constraintcount = 0;   });
        }
        //stop colliding
        object.setCollisionGroup(nocollideCG);
        object.sprite.onAir = true;
        object.data.shapes[0].sensor = true; 
        explosion.play();
        fireemitter.x = object.x;
        fireemitter.y = object.y;
        fireemitter.explode(200, 1);
        smokeemitter.x = object.x;
        smokeemitter.y = object.y;
        smokeemitter.explode(1400, 2);

        // get the waterhole associated with this mine here pronto 
        // if the waterhole is still in the graveyart (because detonated waterholes are treated like normal ones - trigger resurrection)
        if (thisMine.sprite.detonatedwaterhole.killed){
            resetObject(thisMine.sprite.detonatedwaterhole);
        }
        thisMine.sprite.detonatedwaterhole.hidden = false;
        thisMine.sprite.detonatedwaterhole.body.x = thisMine.x;
        thisMine.sprite.detonatedwaterhole.body.y = thisMine.y;
        killObject(thisMine);
        if (object.sprite) {  
            object.sprite.animations.stop();
            object.sprite.explodeascent.start();
            object.sprite.explodefade.start();
        }
    }
    return;
}




// created these mini functions to get rid of anonymous functions (closures) to NOT create a new function ever second
function getBackOnAirObject(){
    this.sprite.onAir = false;
    movingobjects.add(this.sprite); 
}

















function drownInWater(Awaterhole,object){
    splash.play();
    if (object && object.sprite && object.sprite.name == 'springcow' && object.data.shapes[0].sensor !== true && supercow === false) { // the sensor check is because the cow could be in the air jumping over a ramp over the water
        object.data.shapes[0].sensor = true;

        cowwhine1.play();
        game.physics.p2.createRevoluteConstraint(object, [0,2], Awaterhole, [0,0], 10000);
        ////////////////////////////
        game.add.tween(object.sprite).to( { alpha: 0 }, 500, Phaser.Easing.Linear.None, true, 0, 0, false);
        game.add.tween(object.sprite.scale).to( { x: 0.5 , y:0.5}, 1000, Phaser.Easing.Linear.None, true, 0, 0, false);
        ////////////////////////////
        dropsemitter.x = Awaterhole.x;
        dropsemitter.y = Awaterhole.y+40; //everything is moving down so correct this a little bit
        dropsemitter.explode(2000,10);
        gamestate = 'lost';
        object.sprite.health = 0;
        game.time.events.add(Phaser.Timer.SECOND * 2, function(){fadeOut('finish');});
        loseAsset();
    }
    else if (object && object.sprite && object.sprite.name != 'springcow') { 
        object.setCollisionGroup(nocollideCG); // immediately set tux to no collide to "minimize" the chance tux is triggering 2 collisions with the waterhole
        object.sprite.constraint = game.physics.p2.createRevoluteConstraint(object, [0,2], Awaterhole, [0,0], 10000);
        movingobjects.add(object.sprite);  //tux is added to playergroup on fetch.. make sure its back in its original group before it goes out of screen
        object.sprite.drownfade.start();
        object.sprite.drownscale.start();
        
        dropsemitter.x = Awaterhole.x;
        dropsemitter.y = Awaterhole.y+40;
        dropsemitter.explode(2000,5);  
        if (object.sprite.name == 'tuxice'){
            if (object.sprite.fetched === true){frozenWaterholes++;}
            Awaterhole.setCollisionGroup(nocollideCG); 
            fixedobjects.add(Awaterhole.sprite.freeze);
            Awaterhole.sprite.freeze.reset(Awaterhole.x-20,Awaterhole.y);
            Awaterhole.sprite.freeze.killed = false;
            Awaterhole.sprite.freeze.bringToTop();
            Awaterhole.sprite.freeze.animations.play('freeze');
        }
    }
}








function killObject(obj){
    if (obj.sprite.onAir === true){ return;}
    if (obj.sprite.constraint){ // make sure no constraints are left
        game.physics.p2.removeConstraint(obj.sprite.constraint);
        obj.sprite.constraint = null;
    }
    obj.sprite.killed = true;
    obj.sprite.kill();
    if (obj.sprite.name == "freeze" ){   return; }  //dont add it to graveyard because it doesnt need to be automatically revived and it would lead to a loop in the forEach function
    else { graveyard.add(obj.sprite); } 
    
}








function onPresolve(presolve){  // let everything through the collisionbody if its coming from the right side
    for (var i = 0; i < presolve.contactEquations.length; i++) {
        c = presolve.contactEquations[i];
        //make cow and assets not collide but still trigger the collision// collideconnected could solve this
        if ( (c.bodyB.parent && c.bodyB.parent.sprite && c.bodyB.parent.sprite.type === 'tux') && (c.bodyA.parent && c.bodyA.parent.sprite && c.bodyA.parent.sprite.name === 'springcow') ){
            if (c) c.enabled = false;
        }
        if ( (c.bodyB.parent && c.bodyB.parent.sprite && c.bodyB.parent.sprite.name === 'springcow') && (c.bodyA.parent && c.bodyA.parent.sprite && c.bodyA.parent.sprite.type === 'tux') ){
            if (c) c.enabled = false;
        }
        if ( (c.bodyB.parent && c.bodyB.parent.sprite && c.bodyB.parent.sprite.onAir) && (c.bodyA.parent && c.bodyA.parent.sprite && (c.bodyA.parent.sprite.type !== 'ground' )) ){
            if (c) c.enabled = false;
        }
        if ( (c.bodyA.parent && c.bodyA.parent.sprite && c.bodyA.parent.sprite.onAir) && (c.bodyB.parent && c.bodyB.parent.sprite && (c.bodyB.parent.sprite.type !== 'ground' )) ){
            if (c) c.enabled = false;
        }
    }
}
function jumpCow(){
    if ( cow.onAir === true ){return;}
    cow.onAir = true;
    cow.body.data.gravityScale = 0;
    randomsound2 = game.rnd.between(1,6);
    if      (randomsound2 == 1){ moo1.play(); }
    else if (randomsound2 == 2){ moo2.play(); }
    else if (randomsound2 == 3){ moo3.play(); }
    else if (randomsound2 == 4){ moo4.play(); }
    else if (randomsound2 == 5){ moo5.play(); }
    else if (randomsound2 == 6){ moo6.play(); }
    cow.jumpascent.chain(cow.jumpdescent).start();
    if (cow.asset){
        cow.asset.sprite.onAir = true;
        cow.asset.data.gravityScale = 0;
        cow.asset.sprite.jumpascent.chain(cow.asset.sprite.jumpdescent).start();
    }
}
function pushCow(){
    if ( cowallow ){
        if (randomnumber == 1){ cow.body.moveRight(cowspeed);  }
        else {  cow.body.moveLeft(cowspeed); }
        cowallow = false;
    }
}
function shootAsset(){  
    if (cow.spring && !cow.onAir){
        clearAllConstraints();  // because the current problem is probably due to a constraint we delete ALL constraints here.. probably fixes it when shooting a tuxbomb
        //game.physics.p2.removeConstraint(cow.spring);
        playergroup.remove(cow.asset.sprite);
        movingobjects.add(cow.asset.sprite);
        cow.asset.sprite.animations.play('walk');
        cow.asset.moveUp(1500);
        cow.asset=null;
        cow.spring = null;
        game.time.events.add(500, function(){
            cow.constraintcount = 0;
        });
    }
}

function unpause(event){
    if(game.paused){
        if(event.x > worldwidth-130 && event.x < worldwidth-7 && event.y > 120 && event.y < 180 ){
            game.paused = false;
            fadeOut('menu');
        }
        else if(event.x > worldwidth-100 && event.y < 100 ){
            game.paused = false;
            showIngameMenu();
        }
       
    }
}



function loseAsset(why){
    if (cow.spring){
        game.physics.p2.removeConstraint(cow.spring);
        if (why == 'mine'){ cow.asset.sprite.kill();}
        cow.asset=null;
        cow.spring = null;
        game.time.events.add(500, function(){
            cow.constraintcount = 0;
        });
    }
}
function clearAllConstraints(){
    var allConstraints = game.physics.p2.world.constraints.splice(0,game.physics.p2.world.constraints.length);
    if (allConstraints.length > 0){       for (i=0; i<=allConstraints.length; i++){ game.physics.p2.removeConstraint(allConstraints[i]);}   }
}



























function fadeOut(target){
    texture = new PIXI.RenderTexture(game.width, game.height, game.renderer);
    matrix = new PIXI.Matrix();
    matrix.translate(-game.camera.x, -game.camera.y);
    texture.render(game.world, matrix);
    cover = new Phaser.Sprite(game, 0, 0, texture);
    cover.fixedToCamera = true;
    cover.anchor.setTo(0.5,0.5);
    cover.cameraOffset.x = game.width / 2;
    cover.cameraOffset.y = game.height / 2;
    game.state.start(''+target+''); 
}
function fadeIn() {
    if (!cover) {return;} // catch a very rare error where the cover is not created for some reason
    //game.paused = false;
    tween1 = game.add.tween(cover).to({alpha: 0},  1000,Phaser.Easing.Exponential.InOut, true);
    tween2 = game.add.tween(cover.scale).to({ x: 1.5,  y: 1.5 },  1000,Phaser.Easing.Exponential.InOut, true);
    tween2.onComplete.addOnce(destroyCover, this);
}
function destroyCover() {
    cover&&cover.destroy();
    cover = null;
    texture&&texture.destroy();
    texture = null;
    if (cowpushbutton){ // entry tween to get the attention of the zoom button on level start
        tweenx1 = game.add.tween(cowpushbutton).to({ alpha : 0.9, x: "-60" }, 600, Phaser.Easing.Exponential.Out, true);
        tweenx2 = game.add.tween(icon_pushcow ).to({ alpha : 1, x:"-60"  }, 600, Phaser.Easing.Exponential.Out, true);
        tweenx1 = game.add.tween(cowjumpbutton).to({ alpha : 0.9, x: "60" }, 600, Phaser.Easing.Exponential.Out, true);
        tweenx2 = game.add.tween(icon_cowjump ).to({ alpha : 1, x:"60"  }, 600, Phaser.Easing.Exponential.Out, true);
    }
}
var game = new Phaser.Game(worldwidth, worldheight, Phaser.CANVAS, '', {});
game.state.add('boot', BootState, true);
game.state.add('preload', PreloadState, false);
game.state.add('menu', Menu, false);
game.state.add('finish', Finish, false);
game.state.add('0', level0, false);




