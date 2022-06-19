package com.aposbot.handler;

import com.aposbot.Constants;
import com.aposbot._default.IClient;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;

/**
 * @author see <https://github.com/RSCPlus/rscplus>
 */
public final class KeyboardHandler implements KeyListener {
	// Injected
	public static KeyListener keyListener;

	public static boolean keyLeft = false;
	public static boolean keyRight = false;
	public static boolean keyUp = false;
	public static boolean keyDown = false;
	public static boolean keyShift = false;

	private final IClient client;

	private KeyboardHandler(final IClient client) {
		this.client = client;
	}

	public static KeyboardHandler createInstance(final IClient client) {
		return new KeyboardHandler(client);
	}

	public static void release() {
		KeyboardHandler.keyUp = false;
		KeyboardHandler.keyDown = false;
		KeyboardHandler.keyLeft = false;
		KeyboardHandler.keyRight = false;
		KeyboardHandler.keyShift = false;
	}

	@Override
	public void keyTyped(final KeyEvent e) {
		if (client.isKeysDisabled()) {
			return;
		}

		switch (e.getKeyCode()) {
			case KeyEvent.VK_LEFT:
				keyLeft = true;
				e.consume();
				break;
			case KeyEvent.VK_RIGHT:
				keyRight = true;
				e.consume();
				break;
			case KeyEvent.VK_UP:
				keyUp = true;
				e.consume();
				break;
			case KeyEvent.VK_DOWN:
				keyDown = true;
				e.consume();
				break;
		}

		keyShift = e.isShiftDown();

		if (keyListener != null && !e.isConsumed()) {
			keyListener.keyTyped(e);
		}
	}

	@Override
	public void keyPressed(final KeyEvent e) {
		if (client.isKeysDisabled()) {
			client.keyPressed(e);
			return;
		}

		switch (e.getKeyCode()) {
			case KeyEvent.VK_ESCAPE:
				CameraHandler.reset();
				e.consume();
				break;
			case KeyEvent.VK_LEFT:
				keyLeft = true;
				e.consume();
				break;
			case KeyEvent.VK_RIGHT:
				keyRight = true;
				e.consume();
				break;
			case KeyEvent.VK_UP:
				keyUp = true;
				e.consume();
				break;
			case KeyEvent.VK_DOWN:
				keyDown = true;
				e.consume();
				break;
			case KeyEvent.VK_V:
				if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0) {
					paste();
					e.consume();
					return;
				}
				break;
			case KeyEvent.VK_F12:
			case KeyEvent.VK_PRINTSCREEN:
				screenshot();
				e.consume();
				return;
		}

		keyShift = e.isShiftDown();

		if (keyListener != null && !e.isConsumed()) {
			keyListener.keyPressed(e);
		}
	}

	@Override
	public void keyReleased(final KeyEvent e) {
		if (client.isKeysDisabled()) {
			return;
		}

		switch (e.getKeyCode()) {
			case KeyEvent.VK_LEFT:
				keyLeft = false;
				e.consume();
				break;
			case KeyEvent.VK_RIGHT:
				keyRight = false;
				e.consume();
				break;
			case KeyEvent.VK_UP:
				keyUp = false;
				e.consume();
				break;
			case KeyEvent.VK_DOWN:
				keyDown = false;
				e.consume();
				break;
		}

		keyShift = e.isShiftDown();

		if (keyListener != null && !e.isConsumed()) {
			keyListener.keyReleased(e);
		}
	}

	private void paste() {
		if (!client.isLoggedIn()) {
			return;
		}

		final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		final Transferable t = clipboard.getContents(null);
		final DataFlavor f = DataFlavor.stringFlavor;

		if (!t.isDataFlavorSupported(f)) {
			return;
		}

		try {
			for (final char c : ((String) t.getTransferData(f)).toCharArray()) {
				client.typeChar(c);
			}
		} catch (final UnsupportedFlavorException | IOException e) {
			e.printStackTrace();
		}
	}

	private void screenshot() {
		if (!client.isLoggedIn()) {
			return;
		}

		Executors.newSingleThreadExecutor().execute(() -> {
			final String fileName = String.valueOf(System.currentTimeMillis());

			final File file = Constants.PATH_SCREENSHOT.resolve(fileName + ".png").toFile();

			final Image screenImage = client.getImage();

			final BufferedImage bufferedImage = new BufferedImage(screenImage.getWidth(null),
				screenImage.getHeight(null), BufferedImage.TYPE_INT_RGB);

			final Graphics g = bufferedImage.createGraphics();
			g.drawImage(screenImage, 0, 0, null);
			g.dispose();

			try {
				ImageIO.write(bufferedImage, "png", file);
				System.out.printf("Screenshot saved: %s%n", fileName);
				client.displayMessage(String.format("@mag@Screenshot saved: %s", fileName));
			} catch (final IOException e) {
				e.printStackTrace();
			}
		});
	}
}
