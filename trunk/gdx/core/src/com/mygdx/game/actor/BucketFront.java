package com.mygdx.game.actor;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.mygdx.game.MyStage;

public class BucketFront extends Actor {
	TextureRegion image;
	OrthographicCamera camera;

	public BucketFront(OrthographicCamera camera, TextureAtlas atlas) {
		this.camera = camera;
		image = atlas.findRegion("bucket-large-front");
		setBounds(0, 0, image.getRegionWidth(), image.getRegionHeight());
		setVisible(false);
	}

	@Override
	public void draw(Batch batch, float alpha) {
		batch.draw(image, getX(), getY());
	}

	public void act(float delta) {
		super.act(delta);
		
		if (((MyStage)getStage()).isAnimateLaura()) {
			 if (!isVisible()) {
				MyStage stage = (MyStage)getStage(); 
				 setX(stage.getBucket().getX());
				 setY(stage.getBucket().getY());
				 setVisible(true);
			 }
		} else {
			setVisible(false);
		}
	}
}
