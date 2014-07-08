package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;

public class GameScreen implements Screen {
	final MyGdxGame game;

	TextureAtlas atlas;
	Sound dropSound;
	Sound laughSound;
	Music rainMusic;
	OrthographicCamera camera;
	long lastDropTime;
	Bucket bucket;
	BucketFront bucketFront;
	Array<Drop> raindrops;
	Laura laura;
	Stage stage;

	public GameScreen(final MyGdxGame game) {
		this.game = game;
		
		atlas = new TextureAtlas(Gdx.files.internal("images/texture.pack"));
		
		// load the drop sound effect and the rain background "music"
		dropSound = Gdx.audio.newSound(Gdx.files.internal("drop.wav"));
		rainMusic = Gdx.audio.newMusic(Gdx.files.internal("rain.mp3"));
		laughSound = Gdx.audio.newSound(Gdx.files.internal("laugh.mp3"));
		rainMusic.setVolume(0.8f);				
		rainMusic.setLooping(true);

		// create the camera and the SpriteBatch
		camera = new OrthographicCamera();
		camera.setToOrtho(false, 800, 480);
		
		stage = new Stage();
		Gdx.input.setInputProcessor(stage);
		
		Image background = new Image(atlas.findRegion("background"));
		background.setFillParent(true);
		stage.addActor(background);
		
		bucket = new Bucket(camera, atlas);
		bucket.setTouchable(Touchable.enabled);		
		stage.addActor(bucket);
		
		laura = new Laura(atlas);
		stage.addActor(laura);
		
		bucketFront = new BucketFront(camera, atlas);
		stage.addActor(bucketFront);
		
		Drop raindrop = new Drop(atlas);
		stage.addActor(raindrop);
		lastDropTime = TimeUtils.nanoTime();			
	}

	@Override
	public void render(float delta) {
		// clear the screen with a dark blue color. The
		// arguments to glClearColor are the red, green
		// blue and alpha component in the range [0,1]
		// of the color to be used to clear the screen.
		Gdx.gl.glClearColor(0, 0, 0.2f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		// tell the camera to update its matrices.
		camera.update();

		// tell the SpriteBatch to render in the
		// coordinate system specified by the camera.
		game.batch.setProjectionMatrix(camera.combined);

		if (!laura.isVisible()) {
			game.batch.begin();
			game.font.draw(game.batch, "Drops Collected: " + bucket.numDropsCollected, 0, 480);
			game.batch.end();
			
			stage.act(Gdx.graphics.getDeltaTime());
			
			if (bucket.numDropsCollected == 3) {
				bucket.numDropsCollected = 0;
				laura.setVisible(true);
				laura.setX(bucket.getX() + (bucket.getWidth()-laura.getWidth())/2);
				laura.setY(bucket.getY());
				
				bucketFront.setVisible(true);
				bucketFront.setX(bucket.getX());
				bucketFront.setY(bucket.getY());
				
				rainMusic.setVolume(0.2f);				
				long id = laughSound.play(1.0f);
				laughSound.setLooping(id, true);
			}
		} else {
			laura.act(Gdx.graphics.getDeltaTime());
			if (laura.displayTimeSecs > 20) {
				laura.setVisible(false);
				laura.displayTimeSecs = 0;
				bucketFront.setVisible(false);
				laughSound.stop();
				rainMusic.setVolume(0.8f);				
			}
		}
		
		stage.draw();

		if (laura.isVisible()) {
			return;
		}
		if (TimeUtils.nanoTime() - lastDropTime > 1000000000) {
			Drop raindrop = new Drop(atlas);
			stage.addActor(raindrop);
			lastDropTime = TimeUtils.nanoTime();	
		}
		
		Rectangle bucketBound = new Rectangle(bucket.getX(), bucket.getY(), bucket.getWidth(), bucket.getHeight());
		for (Actor actor : stage.getActors()) {
			if (actor instanceof Drop) {
				Drop drop = (Drop)actor;
				Rectangle dropBound = new Rectangle(drop.getX(), drop.getY(), drop.getWidth(), bucket.getHeight());
				if (dropBound.overlaps(bucketBound)) {
					bucket.numDropsCollected++;
					dropSound.play();
					drop.remove();
				} else if (drop.getY() + 64 < 0) {
					drop.remove();
				}
			}
		}
				
		
	}

	@Override
	public void resize(int width, int height) {
	}

	@Override
	public void show() {
		// start the playback of the background music
		// when the screen is shown
		rainMusic.play();
	}

	@Override
	public void hide() {
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}

	@Override
	public void dispose() {
		atlas.dispose();
		dropSound.dispose();
		rainMusic.dispose();
		laughSound.dispose();
	}

}
