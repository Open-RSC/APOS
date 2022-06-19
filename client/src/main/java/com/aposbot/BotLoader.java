package com.aposbot;

import com.aposbot._default.IClientInit;
import com.aposbot.gui.BotFrame;
import com.aposbot.gui.EntryFrame;

import java.awt.*;
import java.io.*;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Parses properties files, creates the built-in console, and instantiates the EntryFrame.
 */
public final class BotLoader {
	private static final File PROPERTIES_FILE = Paths.get("bot.properties").toFile();

	private TextArea cTextArea;
	private Frame cFrame;
	private String font;

	private int defaultOCR;

	public BotLoader(final String[] argv, final IClientInit clientInit) {
		new EntryFrame(this, clientInit).setVisible(true);

		if (argv.length == 0) {
			System.out.println("To launch the bot without the built-in console, use at least one command-line argument.");

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
				final String str = p.getProperty("default_ocr");
				defaultOCR = str == null ? 0 : Integer.parseInt(str);
			} catch (final Throwable t) {
				System.out.println("Settings error:");
				t.printStackTrace();
			}
		}

	}

	public static Properties getProperties() {
		final Properties p = new Properties();
		try (final FileInputStream in = new FileInputStream(PROPERTIES_FILE)) {
			p.load(in);
			if (!p.containsKey("font")) {
				p.put("font", "");
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
