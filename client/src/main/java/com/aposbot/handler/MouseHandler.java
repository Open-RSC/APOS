package com.aposbot.handler;

import java.awt.*;
import java.awt.event.*;

/**
 * @author see <https://github.com/RSCPlus/rscplus>
 */
public final class MouseHandler implements MouseListener, MouseMotionListener, MouseWheelListener {
	private static final MouseHandler instance = new MouseHandler();

	private static final int MOUSE_WHEEL_SPEED_FACTOR = 128;

	// Injected
	public static MouseListener mouseListener;
	public static MouseMotionListener mouseMotionListener;

	private boolean mouseRotating;
	private Point mouseRotatePosition;
	private float mouseRotateX;

	private MouseHandler() {
	}

	public static MouseHandler getInstance() {
		return instance;
	}

	@Override
	public void mouseClicked(final MouseEvent e) {
		if (mouseListener == null) {
			return;
		}

		if (!e.isConsumed()) {
			mouseListener.mouseClicked(e);
		}
	}

	@Override
	public void mousePressed(final MouseEvent e) {
		if (mouseListener == null) {
			return;
		}

		if (e.getButton() == MouseEvent.BUTTON2) {
			mouseRotating = true;
			mouseRotatePosition = e.getPoint();
			e.consume();
		}

		if (!e.isConsumed()) {
			mouseListener.mousePressed(e);
		}
	}

	@Override
	public void mouseReleased(final MouseEvent e) {
		if (mouseListener == null) {
			return;
		}

		if (e.getButton() == MouseEvent.BUTTON2) {
			mouseRotating = false;
			e.consume();
		}

		if (!e.isConsumed()) {
			mouseListener.mouseReleased(e);
		}
	}

	@Override
	public void mouseEntered(final MouseEvent e) {
		if (mouseListener == null) {
			return;
		}

		if (!e.isConsumed()) {
			mouseListener.mouseEntered(e);
		}
	}

	@Override
	public void mouseExited(final MouseEvent e) {
		if (mouseListener == null) {
			return;
		}

		if (!e.isConsumed()) {
			mouseListener.mouseExited(e);
		}
	}

	@Override
	public void mouseDragged(final MouseEvent e) {
		if (mouseMotionListener == null) {
			return;
		}

		if (mouseRotating) {
			mouseRotateX += (float) (e.getX() - mouseRotatePosition.x) / 2.0f;

			final int xDist = (int) mouseRotateX;

			CameraHandler.addRotation(xDist);
			mouseRotateX -= xDist;

			mouseRotatePosition = e.getPoint();
		}

		if (!e.isConsumed()) {
			mouseMotionListener.mouseDragged(e);
		}
	}

	@Override
	public void mouseMoved(final MouseEvent e) {
		if (mouseMotionListener == null) {
			return;
		}

		if (!e.isConsumed()) {
			mouseMotionListener.mouseMoved(e);
		}
	}

	@Override
	public void mouseWheelMoved(final MouseWheelEvent e) {
		CameraHandler.addZoom(e.getWheelRotation() * MOUSE_WHEEL_SPEED_FACTOR);
	}
}
