package com.dv.jump.game;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.dv.jump.game.objects.Player;
import com.dv.jump.util.CameraHelper;
import com.dv.jump.util.Constants;

public class WorldController extends InputAdapter {
	private static final String TAG = WorldController.class.getName();
	private Game game;
	private Level level;
	private int lives;
	private int score;
	private Player player;
	private IWorldChangeListener listener;
	private CameraHelper cameraHelper;
	private boolean jumpingPressed;
	private long jumpPressedTime;
	private float TILEWIDTH;

	private Array<Rectangle> tiles;
	private Pool<Rectangle> rectPool = new Pool<Rectangle>() {
		@Override
		protected Rectangle newObject() {
			return new Rectangle();
		}
	};

	public WorldController(Game game) {
		this.game = game;
		Gdx.input.setInputProcessor(this);
		this.cameraHelper = new CameraHelper();
		this.lives = Constants.LIVES_START;
		this.level = new Level(Constants.TILED_MAP_LEVELS[0]);
		this.player = new Player();

		tiles = new Array<Rectangle>();
		TILEWIDTH = level.getTileWidth() * Constants.UNIT_SCALE;
	}

	public void setListener(IWorldChangeListener listener) {
		this.listener = listener;
	}

	public boolean isGameOver() {
		return lives < 0;
	}

	public void update(float delta) {
		update1(delta);
	}
	
	/**
	 * update
	 * 
	 * @param delta
	 */
	public void update1(float delta) {
		if (delta == 0)
			return;

		if (Gdx.input.isKeyPressed(Keys.LEFT)) {

			player.getVelocity().x = -Player.MAX_VELOCITY;
			if (player.isGrounded()) {
				player.setState(Player.State.WALKING);
			}
			player.setFacingRight(false);
		}
		if (Gdx.input.isKeyPressed(Keys.RIGHT)) {
			player.getVelocity().x = Player.MAX_VELOCITY;
			if (player.isGrounded()) {
				player.setState(Player.State.WALKING);
			}
			player.setFacingRight(true);
		}

		if (player.getState() != Player.State.FALLING) {
			if (player.getVelocity().y < 0) {
				player.setState(Player.State.FALLING);
				player.setGrounded(false);
			}
		}

		player.getAcceleration().y = Player.GRAVITY;
		player.getAcceleration().scl(delta);
		player.getVelocity().add(player.getAcceleration().x,
				player.getAcceleration().y);

		// clamp the velocity to the maximum, x-axis only
		if (Math.abs(player.getVelocity().x) > Player.MAX_VELOCITY) {
			player.getVelocity().x = Math.signum(player.getVelocity().x)
					* Player.MAX_VELOCITY;
		}

		// clamp the velocity to 0 if it's < 1, and set the state to standing
		if (Math.abs(player.getVelocity().x) < 1) {
			player.getVelocity().x = 0;
			if (player.isGrounded()) {
				player.setState(Player.State.STANDING);
			}
		}

		player.getVelocity().scl(delta);

		// perform collision detection & response, on each axis, separately
		// if the koala is moving right, check the tiles to the right of it's
		// right bounding box edge, otherwise check the ones to the left
		Rectangle playerRect = rectPool.obtain();
		playerRect.set(player.getPosition().x,
				player.getPosition().y + player.getHeight() * 0.1f, player.getWidth(),
				player.getHeight());

		int startX, startY, endX, endY;
		if (player.getVelocity().x > 0) {
			startX = endX = (int) (player.getPosition().x + player.getWidth() + player
					.getVelocity().x);
		} else {
			startX = endX = (int) (player.getPosition().x + player.getVelocity().x);
		}

		startY = (int) (player.getPosition().y);
		endY = (int) (player.getPosition().y + player.getHeight());
		getTiles(startX, startY, endX, endY, tiles);

		playerRect.x += player.getVelocity().x;

		for (Rectangle tile : tiles) {

			if (playerRect.overlaps(tile)) {

				if (player.getVelocity().x > 0) {
					player.getPosition().x = tile.x - TILEWIDTH - TILEWIDTH * 0.40f;
				} else if (player.getVelocity().x < 0) {
					player.getPosition().x = tile.x + TILEWIDTH + TILEWIDTH * 0.05f;
				}

				player.getVelocity().x = 0;

				break;
			}
		}

		// playerRect.x = player.getPosition().x;
		playerRect.set(player.getPosition().x, player.getPosition().y,
				player.getWidth(), player.getHeight());

		// if the koala is moving upwards, check the tiles to the top of it's
		// top bounding box edge, otherwise check the ones to the bottom
		if (player.getVelocity().y > 0) {
			startY = endY = (int) (player.getPosition().y + player.getHeight() + player
					.getVelocity().y);
		} else {
			startY = endY = (int) (player.getPosition().y + player.getVelocity().y);
		}

		startX = (int) (player.getPosition().x);
		endX = (int) (player.getPosition().x + player.getWidth());
		getTiles(startX, startY, endX, endY, tiles);
		playerRect.y += player.getVelocity().y;
		for (Rectangle tile : tiles) {
			if (playerRect.overlaps(tile)) {
				// we actually reset the koala y-position here
				// so it is just below/above the tile we collided with
				// this removes bouncing :)
				if (player.getVelocity().y > 0) {
					player.getVelocity().y = tile.y - player.getHeight();
					// we hit a block jumping upwards, let's destroy it!
					// TiledMapTileLayer layer =
					// (TiledMapTileLayer)level.getMap().getLayers().get(1);
					// layer.setCell((int)tile.x, (int)tile.y, null);
				} else {
					player.getPosition().y = tile.y + tile.height;
					// if we hit the ground, mark us as grounded so we can jump
					player.setGrounded(true);
				}
				player.getVelocity().y = 0;
				break;
			}
		}
		rectPool.free(playerRect);

		// unscale the velocity by the inverse delta time and set
		// the latest position

		player.getPosition().add(player.getVelocity());

		player.getVelocity().scl(1 / delta);

		player.getVelocity().x *= Player.DAMPING;

		player.update(delta);

		// handleDebugInput(deltaTime);
		// cameraHelper.update(deltaTime);
	};

	public void update2(float delta) {
		if (delta == 0)
			return;

		if (Gdx.input.isKeyPressed(Keys.LEFT)) {
			player.getVelocity().x = -Player.MAX_VELOCITY;
			if (player.isGrounded()) {
				player.setState(Player.State.WALKING);
			}
			player.setFacingRight(false);
		}
		if (Gdx.input.isKeyPressed(Keys.RIGHT)) {
			player.getVelocity().x = Player.MAX_VELOCITY;
			if (player.isGrounded()) {
				player.setState(Player.State.WALKING);
			}
			player.setFacingRight(true);
		}

		if (player.getState() != Player.State.FALLING) {
			if (player.getVelocity().y < 0) {
				player.setState(Player.State.FALLING);
				player.setGrounded(false);
			}
		}

		player.getAcceleration().y = Player.GRAVITY;
		player.getAcceleration().scl(delta);
		player.getVelocity().add(player.getAcceleration().x,
				player.getAcceleration().y);

		// clamp the velocity to the maximum, x-axis only
		if (Math.abs(player.getVelocity().x) > Player.MAX_VELOCITY) {
			player.getVelocity().x = Math.signum(player.getVelocity().x)
					* Player.MAX_VELOCITY;
		}

		// clamp the velocity to 0 if it's < 1, and set the state to standing
		if (Math.abs(player.getVelocity().x) < 1) {
			player.getVelocity().x = 0;
			if (player.isGrounded()) {
				player.setState(Player.State.STANDING);
			}
		}

		player.getVelocity().scl(delta);

		// perform collision detection & response, on each axis, separately
		// if the koala is moving right, check the tiles to the right of it's
		// right bounding box edge, otherwise check the ones to the left
		Rectangle playerRect = rectPool.obtain();
		playerRect.set(player.getPosition().x,
				player.getPosition().y + player.getHeight() * 0.1f, player.getWidth(),
				player.getHeight());

		int startX, startY, endX, endY;
		if (player.getVelocity().x > 0) {
			startX = endX = (int) (player.getPosition().x + player.getWidth() + player
					.getVelocity().x);
		} else {
			startX = endX = (int) (player.getPosition().x + player.getVelocity().x);
		}

		startY = (int) (player.getPosition().y);
		endY = (int) (player.getPosition().y + player.getHeight());
		getTiles(startX, startY, endX, endY, tiles);

		playerRect.x += player.getVelocity().x;

		for (Rectangle tile : tiles) {

			if (playerRect.overlaps(tile)) {

				if (player.getVelocity().x > 0) {
					player.getPosition().x = tile.x - TILEWIDTH - TILEWIDTH * 0.40f;
				} else if (player.getVelocity().x < 0) {
					player.getPosition().x = tile.x + TILEWIDTH + TILEWIDTH * 0.05f;
				}

				player.getVelocity().x = 0;

				break;
			}
		}

		// playerRect.x = player.getPosition().x;
		playerRect.set(player.getPosition().x, player.getPosition().y,
				player.getWidth(), player.getHeight());

		// if the koala is moving upwards, check the tiles to the top of it's
		// top bounding box edge, otherwise check the ones to the bottom
		if (player.getVelocity().y > 0) {
			startY = endY = (int) (player.getPosition().y + player.getHeight() + player
					.getVelocity().y);
		} else {
			startY = endY = (int) (player.getPosition().y + player.getVelocity().y);
		}

		startX = (int) (player.getPosition().x);
		endX = (int) (player.getPosition().x + player.getWidth());
		getTiles(startX, startY, endX, endY, tiles);
		playerRect.y += player.getVelocity().y;
		for (Rectangle tile : tiles) {
			if (playerRect.overlaps(tile)) {
				// we actually reset the koala y-position here
				// so it is just below/above the tile we collided with
				// this removes bouncing :)
				if (player.getVelocity().y > 0) {
					player.getVelocity().y = tile.y - player.getHeight();
					// we hit a block jumping upwards, let's destroy it!
					// TiledMapTileLayer layer =
					// (TiledMapTileLayer)level.getMap().getLayers().get(1);
					// layer.setCell((int)tile.x, (int)tile.y, null);
				} else {
					player.getPosition().y = tile.y + tile.height;
					// if we hit the ground, mark us as grounded so we can jump
					player.setGrounded(true);
				}
				player.getVelocity().y = 0;
				break;
			}
		}
		rectPool.free(playerRect);

		// unscale the velocity by the inverse delta time and set
		// the latest position

		player.getPosition().add(player.getVelocity());

		player.getVelocity().scl(1 / delta);

		player.getVelocity().x *= Player.DAMPING;

		player.update(delta);

		// handleDebugInput(deltaTime);
		// cameraHelper.update(deltaTime);
	};

	private void getTiles(int startX, int startY, int endX, int endY,
			Array<Rectangle> tiles) {
		TiledMapTileLayer layer = (TiledMapTileLayer) level.getMap().getLayers().get(1);
		rectPool.freeAll(tiles);
		tiles.clear();
		for (int y = startY; y <= endY; y++) {
			for (int x = startX; x <= endX; x++) {
				Cell cell = layer.getCell(x, y);
				if (cell != null) {
					Rectangle rect = rectPool.obtain();
					rect.set(x, y, 1, 1);
					tiles.add(rect);
				}
			}
		}
	}

	@Override
	public boolean keyDown(int keycode) {

		// check input and apply to velocity & state
		if (keycode == Keys.SPACE && player.isGrounded()
				&& player.getState() != Player.State.FALLING) {
			if (!player.getState().equals(Player.State.JUMPING)) {
				jumpingPressed = true;
				player.setGrounded(false);
				jumpPressedTime = System.currentTimeMillis();
				player.setState(Player.State.JUMPING);
				player.getVelocity().y = Player.MAX_JUMP_SPEED;
			} else {
				if ((jumpingPressed && ((System.currentTimeMillis() - jumpPressedTime) >= Player.LONG_JUMP_PRESS))) {
					jumpingPressed = false;
				} else {
					if (jumpingPressed) {
						player.getVelocity().y = Player.MAX_JUMP_SPEED;
						Gdx.app.debug("player", "long jump pressed");
					}
				}
			}
		}

		return true;
	}

	@Override
	public boolean keyUp(int keycode) {
		if (keycode == Keys.SPACE && player.getState() == Player.State.JUMPING) {
			player.getVelocity().y -= Player.MAX_JUMP_SPEED / 1.5f;
			player.setState(Player.State.FALLING);
			jumpingPressed = false;
		}
		return true;
	}

	private void handleDebugInput(float deltaTime) {
		if (Gdx.app.getType() != ApplicationType.Desktop)
			return;
		// Selected Sprite Controls
		float sprMoveSpeed = 5 * deltaTime;

		// Camera Controls (move)
		float camMoveSpeed = 5 * deltaTime;
		float camMoveSpeedAccelerationFactor = 5;
		if (Gdx.input.isKeyPressed(Keys.SHIFT_LEFT))
			camMoveSpeed *= camMoveSpeedAccelerationFactor;
		if (Gdx.input.isKeyPressed(Keys.LEFT))
			moveCamera(-camMoveSpeed, 0);
		if (Gdx.input.isKeyPressed(Keys.RIGHT))
			moveCamera(camMoveSpeed, 0);
		if (Gdx.input.isKeyPressed(Keys.UP))
			moveCamera(0, camMoveSpeed);
		if (Gdx.input.isKeyPressed(Keys.DOWN))
			moveCamera(0, -camMoveSpeed);
		if (Gdx.input.isKeyPressed(Keys.BACKSPACE))
			cameraHelper.setPosition(0, 0);
		// Camera Controls (zoom)
		float camZoomSpeed = 1 * deltaTime;
		float camZoomSpeedAccelerationFactor = 5;
		if (Gdx.input.isKeyPressed(Keys.SHIFT_LEFT))
			camZoomSpeed *= camZoomSpeedAccelerationFactor;
		if (Gdx.input.isKeyPressed(Keys.COMMA))
			cameraHelper.addZoom(camZoomSpeed);
		if (Gdx.input.isKeyPressed(Keys.PERIOD))
			cameraHelper.addZoom(-camZoomSpeed);
		if (Gdx.input.isKeyPressed(Keys.SLASH))
			cameraHelper.setZoom(1);
	}

	private void moveCamera(float x, float y) {
		x += cameraHelper.getPosition().x;
		y += cameraHelper.getPosition().y;
		cameraHelper.setPosition(x, y);
	}

	public Sprite[] getTestSprites() {
		return new Sprite[0];
	}

	public Level getLevel() {
		return level;
	}

	public CameraHelper getCameraHelper() {
		return cameraHelper;
	}

	public Player getPlayer() {
		return player;
	}
}
