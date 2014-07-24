package com.dv.jump.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetErrorListener;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.utils.Disposable;
import com.dv.jump.util.Constants;

public class Assets implements Disposable, AssetErrorListener {

	public static final String TAG = Assets.class.getName();
	public static final Assets instance = new Assets();
	private AssetManager assetManager;
	private AssetFonts fonts;
	private AssetPlayer assetPlayer;

	// singleton: prevent instantiation from other classes
	private Assets () {
	}

	public void init (AssetManager assetManager) {
		synchronized (this) {
			if (this.assetManager != null) {
				return;
			} else {
				this.assetManager = assetManager;
			}
		}
		
		fonts = new AssetFonts();
		
		// set asset manager error handler
		assetManager.setErrorListener(this);
		// load texture atlas
		for (String name: Constants.TEXTURE_ATLAS_OBJECTS) {
			Gdx.app.debug(TAG, "loading atlas: " + name);
			assetManager.load(name, TextureAtlas.class);
		}
		
		// load all map levels
		assetManager.setLoader(TiledMap.class, new TmxMapLoader(new InternalFileHandleResolver()));
		
		for (String name: Constants.TILED_MAP_LEVELS) {
			Gdx.app.debug(TAG, "loading tiled map: " + name);
			assetManager.load(name, TiledMap.class);
		}		
	}

	@Override
	public void dispose () {
		assetManager.dispose();
	}

	@Override
	public void error(AssetDescriptor desc, Throwable ex) {
		Gdx.app.error(TAG, "Couldn't load asset '" + desc.fileName + "'", ex);		
	}

	public boolean isInitialised() {
		return assetManager.update();
	}
	
	public TextureRegion getAtlasRegion(String atlasName, String regionName, float scale) {
		if (!isInitialised()) {
			throw new RuntimeException("Assets has not finished loading!");
		}
		TextureAtlas atlas = assetManager.get(atlasName);
		AtlasRegion region = atlas.findRegion(regionName);
		return region;
	}
		
	public TiledMap getTiledMap(String tileMapName) {
		if (!isInitialised()) {
			throw new RuntimeException("Assets has not finished loading!");
		}
		return assetManager.get(tileMapName);
	}
	
	public AssetFonts getFonts() {
		return fonts;
	}
	
	public class AssetPlayer {
		public final AtlasRegion head;

		public AssetPlayer (TextureAtlas atlas) {
			head = atlas.findRegion("bunny_head");
			if (head != null) {
				//Gdx.app.debug("Assets", "head loaded: " + head.getRegionWidth() + "x" + head.getRegionHeight());
			}
		}
	}

	public class AssetGoldCoin {
		public final AtlasRegion goldCoin;

		public AssetGoldCoin (TextureAtlas atlas) {
			goldCoin = atlas.findRegion("item_gold_coin");
		}
	}

	public class AssetFeather {
		public final AtlasRegion feather;

		public AssetFeather (TextureAtlas atlas) {
			feather = atlas.findRegion("item_feather");
		}
	}
	
	public class AssetFonts {
		public final BitmapFont defaultSmall;
		public final BitmapFont defaultNormal;
		public final BitmapFont defaultBig;

		public AssetFonts () {
			// create three fonts using Libgdx's 15px bitmap font
			defaultSmall = new BitmapFont(Gdx.files.internal("images/arial-15.fnt"), true);
			defaultNormal = new BitmapFont(Gdx.files.internal("images/arial-15.fnt"), true);
			defaultBig = new BitmapFont(Gdx.files.internal("images/arial-15.fnt"), true);
			// set font sizes
			defaultSmall.setScale(0.75f);
			defaultNormal.setScale(1.0f);
			defaultBig.setScale(2.0f);
			// enable linear texture filtering for smooth fonts
			defaultSmall.getRegion().getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
			defaultNormal.getRegion().getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
			defaultBig.getRegion().getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
		}
	}
	
}
