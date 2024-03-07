package com.aposbot.handler;

import com.aposbot._default.IClient;

/**
 * @author see <https://github.com/RSCPlus/rscplus>
 * @author Stormy heavily modified to make more vanilla
 */
public final class CameraHandler {
	private static final int CLIP_DEFAULT = 2400;
	private static final int CLIP_INTERLACE = 2200;
	private static final int CLIP_ENDLESS = 20000;
	private static final int ZOOM_OUTDOORS_DEFAULT = 750;
	private static final int VANILLA_HEIGHT = 346;
	private static final int CAMERA_ROTATION_SPEED = 2;
	private static final int CAMERA_ZOOM_SPEED = 8;
	private static final float CAMERA_MOVE_SPEED = 48.0f;

	// Injected
	public static boolean fieldOfView = false; // Camera overrides field of view
	public static boolean viewDistance = true; // Camera overrides fog distance
	public static boolean auto;

	// Injected
	public static int fov = 9;
	public static int angle;
	public static int zoom;
	public static int rotation;
	public static int rotationY;
	public static int lookAtX;
	public static int lookAtY;
	public static int distance1;
	public static int distance2;
	public static int distance3;
	public static int distance4;

	private static boolean fow = true;
	private static float zoomDelta;
	private static float rotationDelta;
	private static long lastUpdate;

	private static IClient client;

	private CameraHandler() {
	}

	public static void init(final IClient client) {
		CameraHandler.client = client;
		initHook();
	}

	public static void initHook() {
		zoom = ZOOM_OUTDOORS_DEFAULT;
		zoomDelta = (float) zoom;

		client.setCameraEWOffset(0);
		client.setCameraNSOffset(0);
		viewDistance = false;
		fow = true;

		rotation = (client.getCameraRotation() + (int) rotationDelta) & 0xFF;
		rotationY = client.getCameraEWOffset();
		lookAtX = client.getCameraPosX();
		lookAtY = client.getCameraPosY();
	}

	public static void setFogOfWar(boolean on) {
		fow = on;
	}

	private static void setDistance(final double distance) {
		int clip_far = (int) ((distance / (double)ZOOM_OUTDOORS_DEFAULT) * zoom);
		if (client.getGameHeight() > VANILLA_HEIGHT) {
			double n = VANILLA_HEIGHT / (double)client.getGameHeight();
			if (n > 0) {
				clip_far = (int)(clip_far / n);
			}
		}
		distance1 = clip_far;
		distance2 = clip_far;
		distance3 = 1;
		distance4 = clip_far - 100;
	}

	public static void refocusHook(final boolean loading) {
		if (!loading) return;
	}

	public static void updateHook() {
		final long update = System.nanoTime();
		final float delta = (float) (update - lastUpdate) / 1000000000.0f;
		lastUpdate = update;

		if (client.isLoggedIn()) {
			viewDistance = true;
		}

		rotation = (client.getCameraRotation() + (int) rotationDelta) & 0xFF;
		rotationY = client.getCameraEWOffset();
		lookAtX = client.getCameraPosX();
		lookAtY = client.getCameraPosY();

		/*
		 * silly hack, shouldn't be necessary - client already does this
		 * but prevents the camera snapping with the current injection
		 * situation
		 */
		if (Math.hypot(Math.abs(lookAtX - client.getPlayerWaypointX()),
			Math.abs(lookAtY - client.getPlayerWaypointY())) >= 500) {
			client.setCameraPosX(client.getPlayerWaypointX());
			client.setCameraPosY(client.getPlayerWaypointY());

			lookAtX = client.getPlayerWaypointX();
			lookAtY = client.getPlayerWaypointY();
		}

		if (KeyboardHandler.keyLeft) {
			if (KeyboardHandler.keyShift) {
				client.setCameraEWOffset(client.getCameraEWOffset() + 15);
			} else {
				addRotation(CAMERA_ROTATION_SPEED * 50 * delta);
			}
		}
		if (KeyboardHandler.keyRight) {
			if (KeyboardHandler.keyShift) {
				client.setCameraEWOffset(client.getCameraEWOffset() - 15);
			} else {
				addRotation(-CAMERA_ROTATION_SPEED * 50 * delta);
			}
		}
		if (KeyboardHandler.keyUp) {
			if (KeyboardHandler.keyShift) {
				client.setCameraNSOffset(client.getCameraNSOffset() - 15);
			} else {
				addZoom(-CAMERA_ZOOM_SPEED * 100 * delta);
			}
		}
		if (KeyboardHandler.keyDown) {
			if (KeyboardHandler.keyShift) {
				client.setCameraNSOffset(client.getCameraNSOffset() + 15);
			} else {
				addZoom(CAMERA_ZOOM_SPEED * 100 * delta);
			}
		}
		if (!fow) {
			setDistance(CLIP_ENDLESS);
		} else if (client.isSkipLines()) {
			setDistance(CLIP_INTERLACE);
		} else {
			setDistance(CLIP_DEFAULT);
		}
	}

	static void addRotation(final float amount) {
		rotationDelta += amount;
	}

	static void addZoom(final float amount) {
		if (amount == 0) {
			return;
		}

		zoomDelta += amount;

		if (zoomDelta > 2030.0f) {
			zoomDelta = 2030.0f;
		} else if (zoomDelta < 366.0f) {
			zoomDelta = 366.0f;
		}

		zoom = (int) zoomDelta;
	}

	static void reset() {
		client.setCameraEWOffset(0);
		client.setCameraNSOffset(0);
		zoom = ZOOM_OUTDOORS_DEFAULT;
		zoomDelta = (float) zoom;
	}
}
