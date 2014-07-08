package com.mygdx.game;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;

public class Laura extends Actor {
	TextureRegion image;
	float displayTimeSecs = 0.0f;
	
	public Laura(TextureAtlas atlas) {
		this.image = atlas.findRegion("laura-compact");
		if (image == null) {
			throw new RuntimeException("Cannot find laura image from texture pack!");
		}
		setBounds(0, 0, image.getRegionWidth(), image.getRegionHeight());
		setVisible(false);
	}
	
	@Override
	public void draw(Batch batch, float alpha) {
		batch.draw(image, getX(), getY());
	}
	
	public void act(float delta) {
		if (isVisible()) {
			displayTimeSecs += delta;
			if (getY() <= 100) {
				moveBy(0, 10*delta);
			}			
		}
	}

}
