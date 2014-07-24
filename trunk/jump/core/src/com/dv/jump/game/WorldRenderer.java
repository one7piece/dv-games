package com.dv.jump.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.dv.jump.util.Constants;

public class WorldRenderer implements Disposable, IWorldChangeListener {
	private static final String TAG = WorldRenderer.class.getName();
	private OrthographicCamera camera;
	private OrthographicCamera cameraGUI;
	private Batch batch;
	private WorldController worldController;
	private OrthogonalTiledMapRenderer renderer = null;

	public WorldRenderer(WorldController controller) {
		this.worldController = controller;
		init();
	}

	@Override
	public void dispose() {
		batch.dispose();
	}

	private void init() {
		// world camera, 5x5 meters viewport
		camera = new OrthographicCamera();
		//camera.position.set(0, 0, 0);
		camera.setToOrtho(false, Constants.VIEWPORT_WIDTH, Constants.VIEWPORT_HEIGHT);
		camera.update();

		// gui camera, 800x480 pixels
		cameraGUI = new OrthographicCamera(Constants.VIEWPORT_GUI_WIDTH,
				Constants.VIEWPORT_GUI_HEIGHT);
		cameraGUI.position.set(0, 0, 0);
		cameraGUI.setToOrtho(true); // flip y-axis
		cameraGUI.update();		
		
		batch = new SpriteBatch();
		renderer = new OrthogonalTiledMapRenderer(worldController.getLevel().getMap(), Constants.UNIT_SCALE, batch);		
		//batch = renderer.getSpriteBatch();
	}

	public void levelChanged(String levelName) {
		// TODO synchornize?
		if (renderer != null) {
			renderer.dispose();
		}
		renderer = new OrthogonalTiledMapRenderer(worldController.getLevel().getMap(), Constants.UNIT_SCALE);				
		batch = renderer.getSpriteBatch();
	}
	
	public void render() {
		renderWorld();
		renderGui();		
	}

	private void renderWorld () {					
		// render level
		camera.position.x = worldController.getPlayer().getPosition().x;
		renderer.setView(camera);
		camera.update();
		renderer.render();
		
		// render player
		batch.begin();
		worldController.getPlayer().render(batch);
		batch.end();
	}
	
	private void renderGui() {
		batch.setProjectionMatrix(cameraGUI.combined);
		batch.begin();

		// draw FPS text (anchored to bottom right edge)
		renderGuiFpsCounter();
		// daw game over text
		renderGuiGameOverMessage();

		batch.end();
	}

	private void renderGuiGameOverMessage() {
		float x = cameraGUI.viewportWidth / 2;
		float y = cameraGUI.viewportHeight / 2;
		if (worldController.isGameOver()) {
			BitmapFont fontGameOver = Assets.instance.getFonts().defaultBig;
			fontGameOver.setColor(1, 0.75f, 0.25f, 1);
			fontGameOver.drawMultiLine(batch, "GAME OVER", x, y, 1,
					BitmapFont.HAlignment.CENTER);
			fontGameOver.setColor(1, 1, 1, 1);
		}
	}

	public void resize(int width, int height) {
		//camera.viewportWidth = (Constants.VIEWPORT_HEIGHT / height) * width;
		//camera.update();

		cameraGUI.viewportHeight = Constants.VIEWPORT_GUI_HEIGHT;
		cameraGUI.viewportWidth = (Constants.VIEWPORT_GUI_HEIGHT / (float) height)
				* (float) width;
		cameraGUI.position.set(cameraGUI.viewportWidth / 2,
				cameraGUI.viewportHeight / 2, 0);
		cameraGUI.update();
	}

	private void renderGuiFpsCounter() {
		float x = cameraGUI.viewportWidth - 55;
		float y = cameraGUI.viewportHeight - 15;
		int fps = Gdx.graphics.getFramesPerSecond();
		BitmapFont fpsFont = Assets.instance.getFonts().defaultNormal;
		if (fps >= 45) {
			// 45 or more FPS show up in green
			fpsFont.setColor(0, 1, 0, 1);
		} else if (fps >= 30) {
			// 30 or more FPS show up in yellow
			fpsFont.setColor(1, 1, 0, 1);
		} else {
			// less than 30 FPS show up in red
			fpsFont.setColor(1, 0, 0, 1);
		}

		fpsFont.draw(batch, "FPS: " + fps, x, y);
		fpsFont.setColor(1, 1, 1, 1); // white
	}

	
}