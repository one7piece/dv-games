package com.dv.jump.game;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;

public class Level {
	private TiledMap map;
	private float tileWidth;
  private float tileHeight;	
	
	public Level(String tileMapName) {
		map = Assets.instance.getTiledMap(tileMapName);
		TiledMapTileLayer layer = (TiledMapTileLayer)map.getLayers().get(0);
    tileWidth = layer.getTileWidth();
    tileHeight = layer.getTileHeight();		
	}

	public TiledMap getMap() {
		return map;
	}

	public float getTileWidth() {
		return tileWidth;
	}

	public float getTileHeight() {
		return tileHeight;
	}	
}
