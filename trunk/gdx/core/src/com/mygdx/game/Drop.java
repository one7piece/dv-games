package com.mygdx.game;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;

public class Drop extends Actor {
	TextureRegion image;
	
	public Drop(TextureAtlas atlas) {
		this.image = atlas.findRegion("droplet");
		if (image == null) {
			throw new RuntimeException("Cannot find drop image from texture pack!");
		}
		int x = MathUtils.random(0, 800 - 64);
		int y = 480;
		setBounds(x, y, 64, 64);
	}
	
	@Override
	public void draw(Batch batch, float alpha) {
		if (getY() + 64 >= 0) {
			batch.draw(image, getX(), getY());
		}
	}
	
	public void act(float delta) {
		// move drop down 200px per seconds
		moveBy(0, -200*delta);
	}

}
