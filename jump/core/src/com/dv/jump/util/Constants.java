package com.dv.jump.util;

public class Constants {
	// Visible game world is 5 meters wide
	public static final float VIEWPORT_WIDTH = 30.0f;
	// Visible game world is 5 meters tall
	public static final float VIEWPORT_HEIGHT = 20.0f;

	public static final float UNIT_SCALE = 1/16f;		// tile 16x16
	
	// tiled map file
	public static final String TILED_MAP_LEVEL1 = "levels/level1.tmx";
	public static final String[] TILED_MAP_LEVELS = new String[] {TILED_MAP_LEVEL1};
	
	// texture atlas file
	public static final String[] TEXTURE_ATLAS_OBJECTS = new String[] {"images/monkey.pack"};
	
	public static final String TEXTURE_ATLAS_UI = "images/canyonbunny-ui.pack";
	public static final String TEXTURE_ATLAS_LIBGDX_UI = "images/uiskin.atlas";

	// Duration of feather power-up in seconds
	public static final float ITEM_FEATHER_POWERUP_DURATION = 9;

	// GUI Width
	public static final float VIEWPORT_GUI_WIDTH = 800.0f;
	// GUI Height
	public static final float VIEWPORT_GUI_HEIGHT = 480.0f;
	
	// Amount of extra lives at level start
	public static final int LIVES_START = 3;
	
	// Location of description file for skins
	public static final String SKIN_LIBGDX_UI = "images/uiskin.json";
	public static final String SKIN_CANYONBUNNY_UI = "images/canyonbunny-ui.json";	

	public static final String PREFERENCES = "jump.prefs";	
}
