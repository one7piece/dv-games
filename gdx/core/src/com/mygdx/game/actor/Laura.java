package com.mygdx.game.actor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.mygdx.game.MyStage;

public class Laura extends Actor {
	TextureRegion image;
	float displayTimeSecs = 0f;
	long laughId;
	float laughVolume;
	Sound laughSound;
	VolumeControl volumeControl = new VolumeControl();
	
	public Laura(TextureAtlas atlas, Sound laugh) {
		this.laughSound = laugh;
		this.image = atlas.findRegion("laura-compact");
		if (image == null) {
			throw new RuntimeException("Cannot find laura image from texture pack!");
		}
		setBounds(0, 0, image.getRegionWidth(), image.getRegionHeight());
		setVisible(false);
	}
	
	@Override
	public void draw(Batch batch, float alpha) {		
		Color c = getColor();
		batch.setColor(c.r, c.g, c.b, c.a);
		batch.draw(image, getX(), getY());
	}
	
	public void start() {
		Gdx.app.log("Laura Laugh", "start");
		MyStage stage = (MyStage)getStage();
		stage.getBatch().enableBlending();
		stage.setAnimateLaura(true);
		float x = stage.getBucket().getX() + (stage.getBucket().getWidth()-getWidth())/2;
		float y = stage.getBucket().getY();
		setX(x);		
		setY(y);
		
		float dy = stage.getBucket().getHeight();		
		Action action = Actions.sequence(Actions.moveTo(x, y+dy-20, 10f), Actions.delay(5f), Actions.fadeOut(5));		
		addAction(action);
		
		laughVolume = 0.2f;
		volumeControl.start(laughVolume, 1.0f, 10f);
		laughId = laughSound.loop(laughVolume);
		displayTimeSecs = 0f;
		setVisible(true);
		// set color alpha to 1 at the start
		Color c = getColor();
		setColor(c.r, c.g, c.b, 1f);
		
	}

	public void end() {
		MyStage stage = (MyStage)getStage();
		stage.setAnimateLaura(false);		
		clearActions();
		laughSound.stop();
		setVisible(false);
	}
	
	public void act(float delta) {
		super.act(delta);
		
		MyStage stage = (MyStage)getStage(); 
		if (stage.getBucket() != null && stage.getBucket().getNumDropsCollected() == 3) {
			stage.getBucket().setNumDropsCollected(0);
			start();
		} else if (isVisible()) {
			displayTimeSecs += delta;
			if (displayTimeSecs > 20f) {
				end();
				return;
			} else if (displayTimeSecs >= 15f && volumeControl.volume == 1f) {
				volumeControl.start(1f, 0f, 5f);
			}
				
			volumeControl.act(delta);
			if (Math.abs(volumeControl.volume-laughVolume) > 0.1f) {
				laughVolume = volumeControl.volume;
				laughSound.setVolume(laughId, laughVolume);
				//Gdx.app.log("Laura Laugh", "set volume: " + laughVolume);
			}			
		}		
	}
	
	private class VolumeControl {
		float changeRate;	// amount volume change per seconds
		float volume;
		
		public void start(float startVolume, float endVolume, float duration) {
			volume = startVolume;
			changeRate = (endVolume - startVolume)/duration;
		}
				
		public void act(float delta) {
			volume += (changeRate*delta);
			if (volume < 0.0f) {
				volume = 0.0f;
			}
			if (volume > 1.0f) {
				volume = 1.0f;
			}
		}
	}
}
