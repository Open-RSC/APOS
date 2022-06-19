package com.aposbot.handler;

import com.aposbot._default.IClient;

/**
 * @author see <https://github.com/RSCPlus/rscplus>
 */
public final class CameraHandler {
	private static final int VIEW_DISTANCE = 20000; // Fog distance
	private static final int CAMERA_ROTATION_SPEED = 5;
	private static final int CAMERA_ZOOM_SPEED = 16;
	private static final float CAMERA_MOVE_SPEED = 48.0f;

	// Injected
	public static boolean fieldOfView = true; // Camera overrides field of view
	public static boolean movableRelative = true; // Camera moves relative to player
	public static boolean viewDistance = true; // Camera overrides fog distance

	// Injected
	public static int fov = 9;
	public static int zoom;
	public static int rotation;
	public static int lookAtX;
	public static int lookAtY;
	public static int distance1;
	public static int distance2;
	public static int distance3;
	public static int distance4;

	private static float lookAtXAdd;
	private static float lookAtYAdd;
	private static int lookAtXNew;
	private static int lookAtYNew;
	private static float lookAtXDelta;
	private static float lookAtYDelta;
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
		zoom = 750;
		zoomDelta = (float) zoom;

		setDistance(VIEW_DISTANCE);
		setFieldOfView(9);
	}

	private static void setDistance(final int distance) {
		viewDistance = (distance != 2300);
		distance1 = distance + 100;
		distance2 = distance + 100;
		distance3 = 1;
		distance4 = distance;
	}

	private static void setFieldOfView(int fov) {
		fieldOfView = (fov != 9);

		if (fov < 7) {
			fov = 7;
		} else if (fov > 16) {
			fov = 16;
		}

		CameraHandler.fov = fov;
	}

	public static void refocusHook() {
		lookAtX = client.getPlayerWaypointX();
		lookAtY = client.getPlayerWaypointY();

		lookAtXNew = lookAtX;
		lookAtYNew = lookAtY;

		lookAtXDelta = (float) lookAtXNew;
		lookAtYDelta = (float) lookAtYNew;

		lookAtXAdd = 0.0f;
		lookAtYAdd = 0.0f;
	}

	public static void updateHook() {
		lookAtXNew = client.getPlayerWaypointX();
		lookAtYNew = client.getPlayerWaypointY();

		final long update = System.nanoTime();
		final float delta = (float) (update - lastUpdate) / 1000000000.0f;
		lastUpdate = update;

		if (KeyboardHandler.keyLeft) {
			if (KeyboardHandler.keyShift) {
				move(-CAMERA_MOVE_SPEED * delta);
			} else {
				addRotation(CAMERA_ROTATION_SPEED * 50 * delta);
			}
		}
		if (KeyboardHandler.keyRight) {
			if (KeyboardHandler.keyShift) {
				move(CAMERA_MOVE_SPEED * delta);
			} else {
				addRotation(-CAMERA_ROTATION_SPEED * 50 * delta);
			}
		}
		if (KeyboardHandler.keyUp) {
			if (KeyboardHandler.keyShift) {
				strafe(-CAMERA_MOVE_SPEED * delta);
			} else {
				addZoom(-CAMERA_ZOOM_SPEED * 100 * delta);
			}
		}
		if (KeyboardHandler.keyDown) {
			if (KeyboardHandler.keyShift) {
				strafe(CAMERA_MOVE_SPEED * delta);
			} else {
				addZoom(CAMERA_ZOOM_SPEED * 100 * delta);
			}
		}

		if (!KeyboardHandler.keyShift) {
			final int tileX = ((int) lookAtXAdd / 128) * 128;
			final int tileY = ((int) lookAtYAdd / 128) * 128;

			lookAtXAdd = lerp(lookAtXAdd, tileX, 8.0f * delta);
			lookAtYAdd = lerp(lookAtYAdd, tileY, 8.0f * delta);

			if (!movableRelative) {
				if ((lookAtXAdd != 0.0f && lookAtYAdd != 0.0f)
					&& (lookAtXAdd < lookAtXNew + 128 && lookAtXAdd > lookAtXNew - 128)
					&& (lookAtYAdd < lookAtYNew + 128 && lookAtYAdd > lookAtYNew - 128)) {
					lookAtXAdd = 0.0f;
					lookAtYAdd = 0.0f;
				}
			}
		}

		lookAtXDelta = lookAtXNew;
		lookAtYDelta = lookAtYNew;

		if (!movableRelative) {
			if (lookAtXAdd == 0.0f) {
				lookAtX = (int) lookAtXDelta;
			} else {
				lookAtX = (int) lookAtXAdd;
			}

			if (lookAtYAdd == 0.0f) {
				lookAtY = (int) lookAtYDelta;
			} else {
				lookAtY = (int) lookAtYAdd;
			}
		} else {
			lookAtX = (int) lookAtXDelta + (int) lookAtXAdd;
			lookAtY = (int) lookAtYDelta + (int) lookAtYAdd;
		}
	}

	private static void move(final float speed) {
		final float rotation_degrees = ((float) rotation / 255.0f) * 360;
		final float xDiff = dirXLength(64, rotation_degrees);
		final float yDiff = dirYLength(64, rotation_degrees);

		addMovement(xDiff * speed, yDiff * speed);
	}

	static void addRotation(final float amount) {
		rotationDelta += amount;
		rotation = (int) rotationDelta & 0xFF;
	}

	private static void strafe(final float speed) {
		final float rotation_degrees = ((float) rotation / 255.0f) * 360 + 90;
		final float xDiff = dirXLength(64, rotation_degrees);
		final float yDiff = dirYLength(64, rotation_degrees);

		addMovement(xDiff * speed, yDiff * speed);
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

	private static float lerp(final float a, final float b, final float c) {
		return a + c * (b - a);
	}

	private static float dirXLength(final float dist, final float angle) {
		return dist * (float) Math.cos(Math.toRadians(angle));
	}

	private static float dirYLength(final float dist, final float angle) {
		return dist * (float) -Math.sin(Math.toRadians(angle));
	}

	private static void addMovement(final float x, final float y) {
		if (!movableRelative && ((lookAtXAdd == 0.0f && x != 0) || (lookAtYAdd == 0.0f && y != 0))) {
			lookAtXAdd = lookAtX;
			lookAtYAdd = lookAtY;
		}

		lookAtXAdd += x;
		lookAtYAdd += y;
	}

	static void reset() {
		lookAtXAdd = 0.0f;
		lookAtYAdd = 0.0f;
		zoom = 750;
		zoomDelta = (float) zoom;
		rotation = 126;
		rotationDelta = (float) rotation;
	}
}
