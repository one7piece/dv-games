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
import com.badlogic.gdx.scenes.scene2d.actions.MoveToAction;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import com.mygdx.game.actor.Bucket;
import com.mygdx.game.actor.BucketFront;
import com.mygdx.game.actor.Drop;
import com.mygdx.game.actor.Laura;

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
	MyStage stage;

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
		
		stage = new MyStage();
		Gdx.input.setInputProcessor(stage);
		
		Image background = new Image(atlas.findRegion("background"));
		background.setFillParent(true);
		stage.addActor(background);
		
		bucket = new Bucket(camera, atlas);
		bucket.setTouchable(Touchable.enabled);		
		stage.addActor(bucket);
		
		laura = new Laura(atlas, laughSound);
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
		
		boolean oldValue = stage.isAnimateLaura();
		
		stage.act(Gdx.graphics.getDeltaTime());
		
		stage.draw();
		
		if (oldValue != stage.isAnimateLaura()) {
			if (stage.isAnimateLaura()) {
				rainMusic.setVolume(0.2f);				
			} else {
				rainMusic.setVolume(0.8f);				
			}
		}

		if (stage.isAnimateLaura()) {
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
					bucket.setNumDropsCollected(bucket.getNumDropsCollected()+1);
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
