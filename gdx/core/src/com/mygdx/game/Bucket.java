package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputListener;

public class Bucket extends Actor {
	TextureRegion image;
	float posX = 0, posY = 0;
	int numDropsCollected;
	Vector3 touchPos;	// to reduce java GC
	
	public Bucket(TextureAtlas atlas) {
		image = atlas.findRegion("bucket");
		setBounds(posX, posY, image.getRegionWidth(), image.getRegionHeight());
		//addListener(new InputListener() {			
		//});
	}
	
	@Override
	public void draw(Batch batch, float alpha) {
		batch.draw(image, posX, posY);
	}
	
	public void act(float delta) {
		if (Gdx.input.isKeyPressed(Keys.LEFT)) {
			setX(getX() - 200 * delta);
		}
		if (Gdx.input.isKeyPressed(Keys.RIGHT)) {
			setX(getX() + 200 * delta);
		}

		// make sure the bucket stays within the screen bounds
		if (getX() < 0)
			setX(0);
		if (getX() > 800 - 64)
			setX(800 - 64);
	}
}
