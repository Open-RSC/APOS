package com.aposbot.handler;

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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Objects;

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
				takeScreenshot("");
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
	/**
	 * Makes the "unique" file name for each screenshot to prevent screenshot file over writing. could be shortened?
	 * Theoretically allows 1 screenshot to be saved per second forever, also screenshots will autosort by date/time when sorted by filename.
	 * This uses an almost identical save file structure and (I assume) similar method as to Runelite.
	 */
	private static final SimpleDateFormat screenshotNameFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
	public void takeScreenshot(final String fileName) {
		boolean temporaryToggledInterlacing = false;
		boolean temporaryToggledGFX = false;
		if (client.isSkipLines()) { // to be uncommented 2.4
			client.setSkipLines(false);
			temporaryToggledInterlacing = true;
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		if (!client.isRendering()) {
			client.setRendering(true); //If it's off, turn it on for the screenshot
			temporaryToggledGFX = true;
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		String playerTime = screenshotNameFormat.format(timestamp);
		String playerName = client.getPlayerName(client.getPlayer());
		String directory = "";
		String saveLocPath = "";


		final Image image = client.getImage();
		final BufferedImage b = new BufferedImage(image.getWidth(null),
			image.getHeight(null), BufferedImage.TYPE_INT_RGB);
		final Graphics g = b.createGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();
		try {
			if (!Objects.equals(playerName, "")) {
				directory = "screenshots/" + playerName + "/";
			} else {
				directory = "screenshots/";
			}
			if (!Objects.equals(fileName, "")) {
				saveLocPath = directory + fileName + "_" + playerName + "_" + playerTime + ".png";
			} else {
				saveLocPath = directory + playerName + "_" + playerTime + ".png";
			}
			Files.createDirectories(Paths.get(directory));
			ImageIO.write( b,"png", new File(saveLocPath));
			boolean newImageExists = Files.exists(Paths.get(saveLocPath));
			if (newImageExists) {
				client.displayMessage(
					"@cya@Screenshot successfully saved to ./APOS/" + saveLocPath);
				System.out.println(
					"Screenshot successfully saved to ./APOS/" + saveLocPath);
			} else {
				client.displayMessage(
					"@red@Error: @cya@Screenshot not detected at ./APOS/" + saveLocPath);
				System.out.println(
					"Error: @cya@Screenshot not detected at ./APOS/" + saveLocPath);
			}
		} catch (final Throwable t) {
			System.out.println("Error taking screenshot: " + t);
		}
		if (temporaryToggledGFX) {
			client.setRendering(false);
		}
		if (temporaryToggledInterlacing) {
			client.setSkipLines(true);
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
}
