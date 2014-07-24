package com.dv.jump;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.dv.jump.game.Assets;
import com.dv.jump.screens.GameScreen;
import com.dv.jump.screens.MenuScreen;

public class GameMain extends Game {
	@Override
	public void create () {		
		// Set Libgdx log level
		Gdx.app.setLogLevel(Application.LOG_DEBUG);
		Gdx.app.debug("GameMain", "game create...");

		// Load assets
		Assets.instance.init(new AssetManager());;

		// Start game at menu screen
		setScreen(new MenuScreen(this));
		//setScreen(new GameScreen(this));
	}
	
	@Override
	public void dispose() {
		Gdx.app.debug("GameMain", "game dispose...");
		Assets.instance.dispose();		
		super.dispose();
	}
}
