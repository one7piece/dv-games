package com.dv.jump;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.dv.jump.game.Assets;
import com.dv.jump.screens.MenuScreen;

public class GameMain extends Game {
	@Override
	public void create () {
		// Set Libgdx log level
		Gdx.app.setLogLevel(Application.LOG_DEBUG);

		// Load assets
		Assets.instance.init(new AssetManager());

		// Start game at menu screen
		setScreen(new MenuScreen(this));
	}
}
