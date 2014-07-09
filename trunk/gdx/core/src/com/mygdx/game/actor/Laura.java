package com.mygdx.game.actor;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.MoveToAction;
import com.mygdx.game.MyStage;

public class Laura extends Actor {
	TextureRegion image;
	float displayTimeSecs = 0.0f;
	//MoveToAction action;
	Sound laugh;
	
	public Laura(TextureAtlas atlas, Sound laugh) {
		this.laugh = laugh;
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
	
	public void start() {
		MyStage stage = (MyStage)getStage();
		stage.setAnimateLaura(true);
		float x = stage.getBucket().getX() + (stage.getBucket().getWidth()-getWidth())/2;
		float y = stage.getBucket().getY();
		float dy = stage.getBucket().getHeight();
		setX(x);		
		setY(y);
		
		//MoveToAction action = new MoveToAction();
		//action.setPosition(x, 300);
		//action.setDuration(10f);
		//addAction(action);
		
		long id = laugh.play(1.0f);
		laugh.setLooping(id, true);
		displayTimeSecs = 0;
		setVisible(true);
	}

	public void end() {
		MyStage stage = (MyStage)getStage();
		stage.setAnimateLaura(false);		
		clearActions();
		laugh.stop();
		setVisible(false);
	}
	
	public void act(float delta) {
		MyStage stage = (MyStage)getStage(); 
		if (stage.getBucket() != null && stage.getBucket().getNumDropsCollected() == 3) {
			stage.getBucket().setNumDropsCollected(0);
			start();
		} else if (isVisible()) {
			displayTimeSecs += delta;
			if (displayTimeSecs > 20) {
				end();
			} else {
				// move dy pixels in 10 seconds
				float dy = stage.getBucket().getHeight();
				if (getY() < stage.getBucket().getY()+dy) {
					moveBy(0, (dy*delta)/10f);
				}
			}
		}		
	}
}
