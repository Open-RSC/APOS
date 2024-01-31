package com.aposbot;

import com.aposbot._default.IClientInit;
import com.aposbot.gui.BotFrame;
import com.aposbot.gui.EntryFrame;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

/**
 * Parses properties files, creates the built-in console, and instantiates the EntryFrame.
 */
public final class BotLoader {
	private static final File PROPERTIES_FILE = Paths.get("bot.properties").toFile();

	private TextArea cTextArea;
	private Frame cFrame;
	private String font;
	private static boolean log_captcha = true;

	private int defaultOCR;
	private boolean showConsole = true;
	private String cmdUsername = "";
	private static int cmdWindowHeight = 0;
	private static int cmdWindowWidth = 0;
	private static int cmdLocationX = 0;
	private static int cmdLocationY = 0;

	public BotLoader(final String[] argv, final IClientInit clientInit) {
		System.out.println("------------------------------------------------");
		System.out.println("Command-line args:");
		System.out.println("--no-console		Launches the bot without console");
		System.out.println("--username=username	Default account name for launcher. Must already be added as account.");
		System.out.println("--height=558		Also: --h. Window height. Height and Width must both be set together.");
		System.out.println("--width=670			Also: --w. Window width. Height and Width must both be set together.");
		System.out.println("--x=200				X Location on screen. X and Y must both be set together.");
		System.out.println("--y=200				Y Location on screen. X and Y must both be set together.");
		System.out.println("------------------------------------------------");

		for (String s : argv) {
			switch (s.toLowerCase().split("=")[0]) {
				case "--no-console":
					showConsole = false;
					System.out.println("Disabling console.");
					break;
				case "--username":
					cmdUsername = s.toLowerCase().split("=")[1];
					System.out.println("Account: " + cmdUsername);
					break;
				case "--h":
				case "--height":
					cmdWindowHeight = Integer.parseInt(s.toLowerCase().split("=")[1]);
					System.out.println("Height: " + cmdWindowHeight);
					break;
				case "--w":
				case "--width":
					cmdWindowWidth = Integer.parseInt(s.toLowerCase().split("=")[1]);
					System.out.println("Width: " + cmdWindowWidth);
					break;
				case "--x":
					cmdLocationX = Integer.parseInt(s.toLowerCase().split("=")[1]);
					System.out.println("X: " + cmdLocationX);
					break;
				case "--y":
					cmdLocationY = Integer.parseInt(s.toLowerCase().split("=")[1]);
					System.out.println("Y: " + cmdLocationY);
					break;
				default:
					System.out.println("Unknown command line arg: " + s);
					break;
			}
		}

		if (showConsole) {
			final TextArea cTextArea = new TextArea(null, 0, 0, TextArea.SCROLLBARS_VERTICAL_ONLY);
			BotFrame.setColours(cTextArea);
			cTextArea.setEditable(false);
			this.cTextArea = cTextArea;

			final Frame cFrame = new Frame("Console");
			cFrame.addWindowListener(new StandardCloseHandler(cFrame, StandardCloseHandler.EXIT));
			cFrame.add(cTextArea);
			final Insets in = cFrame.getInsets();
			cFrame.setSize(in.right + in.left + 545, in.top + in.bottom + 320);
			cFrame.setIconImages(Constants.ICONS);
			this.cFrame = cFrame;

			final PrintStream ps = new PrintStream(new TextAreaOutputStream(cTextArea));
			System.setOut(ps);
			System.setErr(ps);
		}

		final Properties p = getProperties();

		if (p != null) {
			try {
				font = p.getProperty("font");
				if (font != null && font.trim().isEmpty()) {
					font = null;
				}

				log_captcha = Boolean.parseBoolean(p.getProperty("captcha_logging", "true"));

				final String str = p.getProperty("default_ocr");
				defaultOCR = str == null ? 0 : Integer.parseInt(str);
			} catch (final Throwable t) {
				System.out.println("Settings error:");
				t.printStackTrace();
			}
		}

		EntryFrame entryFrame = new EntryFrame(this, clientInit);

		if (cmdUsername.isEmpty() || entryFrame.accountNames.length == 0) {
			entryFrame.setVisible(true);
		} else {
			if (!cmdUsername.toLowerCase().equals(entryFrame.account.toLowerCase())) {
				System.out.println("Auto load account isn't setup! You must add the account manually before setting up auto load account.");
				System.exit(1);
			}


			ActionEvent triggerOk = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "OKButtonClicked");
			for (ActionListener listener : entryFrame.okButton.getActionListeners()) {
				listener.actionPerformed(triggerOk);
			}
			entryFrame.setVisible(false);
			entryFrame.dispose();
		}

	}

	public static Dimension getHeightWidth() {
		if (cmdWindowHeight != 0 && cmdWindowWidth != 0)
			return new Dimension(cmdWindowWidth, cmdWindowHeight);

		return null;
	}

	public static Point getLocation() {
		if (cmdLocationX != 0 && cmdLocationY != 0)
			return new Point(cmdLocationX, cmdLocationY);

		return null;
	}

	public static Properties getProperties() {
		final Properties p = new Properties();
		try (final FileInputStream in = new FileInputStream(PROPERTIES_FILE)) {
			p.load(in);
			if (!p.containsKey("font")) {
				p.put("font", "");
			}
			if (!p.containsKey("captcha_logging")) {
				p.put("captcha_logging", "true");
			}
			return p;
		} catch (final Throwable ignored) {
		}
		return null;
	}

	public void storeProperties(final Properties props) {
		Properties p = props;
		if (p == null) {
			p = getProperties();
		}
		if (p != null) {
			p.put("default_ocr", String.valueOf(defaultOCR));
			p.put("font", font == null ? "" : font);
			try (final FileOutputStream out = new FileOutputStream(PROPERTIES_FILE)) {
				p.store(out, null);
			} catch (final Throwable t) {
				System.out.println("Error storing updated properties: " + t);
			}
		}
	}

	public String getFont() {
		return font;
	}

	public static boolean isCaptchaLogging() {
		return log_captcha;
	}

	public String getCmdUsername() {
		return cmdUsername;
	}

	public int getDefaultOCR() {
		return defaultOCR;
	}

	public void setDefaultOCR(final int i) {
		defaultOCR = i;
	}

	public TextArea getConsoleTextArea() {
		return cTextArea;
	}

	public void setConsoleFrameVisible() {
		if (cFrame != null) {
			cFrame.setVisible(false);
		}
	}

	private static class TextAreaOutputStream extends OutputStream {
		private final TextArea area;

		TextAreaOutputStream(final TextArea area) {
			this.area = area;
		}

		@Override
		public void write(final int b) {
			area.append(new String(new byte[]{
				(byte) b
			}).intern());
		}

		@Override
		public void write(final byte[] b) {
			area.append(new String(b));
		}

		@Override
		public void write(final byte[] b, final int off, final int len) {
			area.append(new String(b, off, len));
		}
	}
}
