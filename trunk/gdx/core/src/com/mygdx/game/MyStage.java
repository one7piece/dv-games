package com.mygdx.game;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.mygdx.game.actor.Bucket;

public class MyStage extends Stage {
	private boolean animateLaura = false;
	private Bucket bucket;
	
	@Override
	public void act (float delta) {		
		super.act(delta);
	}
		
	@Override
	public void addActor(Actor actor) {
		super.addActor(actor);
		if (actor instanceof Bucket) {
			bucket = (Bucket)actor;
		}
	}
	public boolean isAnimateLaura() {
		return animateLaura;
	}

	public void setAnimateLaura(boolean animateLaura) {
		this.animateLaura = animateLaura;
	}

	public Bucket getBucket() {
		return bucket;
	}
		
}
