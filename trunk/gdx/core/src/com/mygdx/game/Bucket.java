package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;

public class Bucket extends Actor {
	TextureRegion image;
	int numDropsCollected;
	Vector3 touchPos; // to reduce java GC
	OrthographicCamera camera;

	public Bucket(OrthographicCamera camera, TextureAtlas atlas) {
		this.camera = camera;
		touchPos = new Vector3();
		image = atlas.findRegion("bucket-large");
		// center the bucket horizontally bottom left corner of the
		// bucket is 20 pixels above the bottom screen edge
		setBounds(800 / 2 - image.getRegionWidth() / 2, 20, image.getRegionWidth(),
				image.getRegionHeight());
/*		
		addListener(new InputListener() {
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				Bucket.this.setX(x - 64/2);
				return true;
			}
		});
*/		
	}

	@Override
	public void draw(Batch batch, float alpha) {
		batch.draw(image, getX(), getY());
	}

	public void act(float delta) {
		
		if (Gdx.input.isTouched()) {
			touchPos.set(Gdx.input.getX(), Gdx.input.getY(), 0);
			// transform to camera coordinates
			camera.unproject(touchPos);
			setX(touchPos.x - image.getRegionWidth() / 2);
		}

		if (Gdx.input.isKeyPressed(Keys.LEFT)) {
			setX(getX() - 200 * delta);
		}
		if (Gdx.input.isKeyPressed(Keys.RIGHT)) {
			setX(getX() + 200 * delta);
		}

		// make sure the bucket stays within the screen bounds
		if (getX() < 0)
			setX(0);
		if (getX() > 800 - image.getRegionWidth())
			setX(800 - image.getRegionWidth());
	}
}
