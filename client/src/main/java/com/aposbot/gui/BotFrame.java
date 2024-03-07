package com.aposbot.gui;

import com.aposbot.BotLoader;
import com.aposbot._default.*;
import com.aposbot.applet.AVStub;
import com.aposbot.handler.CameraHandler;
import com.aposbot.Constants;

import javax.imageio.ImageIO;
import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The main GUI wrapper for the mudclient Extension applet.
 */
public final class BotFrame extends Frame {
	private static final long serialVersionUID = -2847514806687135697L;

	private final IClientInit clientInit;
	private final IClient client;

	private Checkbox loginCheck;
	private Checkbox gfxCheck;

	private Button startButton;

	private DebugFrame debugFrame;

	private ScriptFrame scriptFrame;

	private Choice worldChoice;

	private AVStub stub;
	public static boolean showWindowDebug = false;

	BotFrame(final IClientInit clientInit, final TextArea cTextArea, final String account) {
		super("APOS (" + account + ")");

		this.clientInit = clientInit;

		setFont(Constants.UI_FONT);
		setIconImages(Constants.ICONS);

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent e) {
				quit();
			}
		});

		if (SystemTray.isSupported()) {
			TrayIcon icon = null;
			if (Constants.ICON_16 != null) {
				icon = new TrayIcon(Constants.ICON_16, "APOS (" + account + ")");
			}
			if (icon != null) {
				icon.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(final MouseEvent e) {
						final BotFrame t = BotFrame.this;
						t.setVisible(!t.isVisible());
					}
				});
			}
			try {
				SystemTray.getSystemTray().add(icon);
			} catch (final Throwable t) {
				t.printStackTrace();
			}
		}

		client = clientInit.getClient();
		((Component) client).setBackground(Color.BLACK);

		try {
			final URL url = new URL("http://game.openrsc.com/");
			stub = new AVStub((Applet) client, url, url, getBaseParameters());
		} catch (final MalformedURLException e) {
			e.printStackTrace();
			dispose();
			return;
		}

		client.setStub(stub);

		BufferedImage image = null;
		try {
			final File file = Constants.PATH_LOGO.resolve("logo.png").toFile();
			image = ImageIO.read(file);
		} catch (final Throwable t) {
			System.out.println("Error loading logo: " + t);
		}

		final Panel sidePanel = new ImagePanel(image);
		setColours(sidePanel);
		if (image != null) {
			sidePanel.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
		}

		final Dimension buttonSize = new Dimension(120, 23);

		final Choice worldChoice = new Choice();
		worldChoice.setPreferredSize(buttonSize);
		worldChoice.setForeground(SystemColor.textText);
		worldChoice.setBackground(SystemColor.text);
		worldChoice.add("RSC Uranium");
		worldChoice.addItemListener(event -> updateWorld(worldChoice.getSelectedIndex() + 1));
		this.worldChoice = worldChoice;

		final Button chooseButton = new Button("Choose script");
		chooseButton.setPreferredSize(buttonSize);
		setButtonColours(chooseButton);
		chooseButton.addActionListener(e -> {
			if (scriptFrame == null) {
				scriptFrame = new ScriptFrame(client, clientInit);
			}
			scriptFrame.setLocationRelativeTo(BotFrame.this);
			scriptFrame.setVisible(true);
		});

		startButton = new Button("Start script");
		startButton.setPreferredSize(buttonSize);
		setButtonColours(startButton);
		startButton.addActionListener(e -> {
			if (clientInit.getScriptListener().isScriptRunning()) {
				stopScript();
			} else {
				startScript();
			}
		});

		final Button debugButton = new Button("Debugger");
		debugButton.setPreferredSize(buttonSize);
		setButtonColours(debugButton);
		debugButton.addActionListener(e -> {
			if (debugFrame == null) {
				debugFrame = new DebugFrame(client, clientInit);
			}
			debugFrame.setLocationRelativeTo(BotFrame.this);
			debugFrame.setVisible(true);
		});

		final Button pwbutton = new Button("PathWalker");
		pwbutton.setPreferredSize(buttonSize);
		setButtonColours(pwbutton);
		pwbutton.addActionListener(e -> {
			if (scriptFrame == null) {
				scriptFrame = new ScriptFrame(client, clientInit);
			}
			final IScript script = scriptFrame.initJavaScript("PathWalker.class");
			try {
				script.init("");
			} catch (final Throwable t) {
				System.out.println("issue with script.");

			}
			clientInit.getScriptListener().setIScript(script);

		});

		final Button depositButton = new Button("Deposit all");
		depositButton.setPreferredSize(buttonSize);
		setButtonColours(depositButton);
		depositButton.addActionListener(e -> {
			if (client.isBankVisible()) {
				final int invCount = client.getInventorySize();
				for (int invItem = 0; invItem < invCount; invItem++) {
					int itemStack = 1;
					final int itemID = client.getInventoryId(invItem);
					if (clientInit.getStaticAccess().isItemStackable(itemID)) {
						itemStack = client.getInventoryStack(invItem);
					}
					client.createPacket(Constants.OP_BANK_DEPOSIT);
					client.put2(itemID);
					client.put4(itemStack);
					client.put4(-2023406815);
					client.finishPacket();
				}
			} else {
				System.out.println("Not in bank.");
			}
		});

		final Button scrButton = new Button("Screenshot");
		scrButton.setPreferredSize(buttonSize);
		setButtonColours(scrButton);
		scrButton.addActionListener(e -> new Thread(() -> takeScreenshot(""), "ScreenshotThread").start());

		final Button exitButton = new Button("Exit");
		exitButton.setPreferredSize(buttonSize);
		setButtonColours(exitButton);
		exitButton.addActionListener(e -> quit());

		//sidePanel.add(worldChoice);
		sidePanel.add(chooseButton);
		sidePanel.add(startButton);
		sidePanel.add(debugButton);
		sidePanel.add(pwbutton);
		sidePanel.add(depositButton);
		sidePanel.add(scrButton);
		sidePanel.add(exitButton);

		final Panel checkPanel = new Panel();
		setColours(checkPanel);
		checkPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 0));

		loginCheck = new Checkbox("Autologin");
		setColours(loginCheck);
		loginCheck.addItemListener(e -> {
			final ILoginListener al = clientInit.getLoginListener();
			al.setEnabled(loginCheck.getState());
		});

		gfxCheck = new Checkbox("Rendering", true);
		setColours(gfxCheck);
		gfxCheck.addItemListener(e -> client.setRendering(gfxCheck.getState()));

		final Checkbox paintCheck = new Checkbox("Show bot layer",
			true);
		setColours(paintCheck);
		paintCheck.addItemListener(e -> {
			final IPaintListener paint = clientInit.getPaintListener();
			paint.setPaintingEnabled(paintCheck.getState());
		});

		final Checkbox r3d = new Checkbox("Plain 3D", true);
		setColours(r3d);
		r3d.addItemListener(e -> {
			final IPaintListener paint = clientInit.getPaintListener();
			paint.setRenderSolid(r3d.getState());
		});

		final Checkbox t3d = new Checkbox("Textured 3D", true);
		setColours(t3d);
		t3d.addItemListener(e -> {
			final IPaintListener paint = clientInit.getPaintListener();
			paint.setRenderTextures(t3d.getState());
		});

		final Checkbox fowCheck = new Checkbox("Fog of War", true);
		setColours(fowCheck);
		fowCheck.addItemListener(e -> {
			CameraHandler.setFogOfWar(fowCheck.getState());
		});

		final Button clearLogButton = new Button("Clear log");
		setButtonColours(clearLogButton);
		clearLogButton.addActionListener(e -> cTextArea.setText(""));

		checkPanel.add(loginCheck);
		checkPanel.add(gfxCheck);
		checkPanel.add(paintCheck);
		checkPanel.add(r3d);
		checkPanel.add(t3d);
		checkPanel.add(fowCheck);
		checkPanel.add(clearLogButton);

		add((Component) client, BorderLayout.CENTER);
		add(sidePanel, BorderLayout.EAST);

		if (cTextArea != null) {
			final Panel bottomPanel = new Panel();
			setColours(bottomPanel);
			bottomPanel.setLayout(new BorderLayout());
			cTextArea.setPreferredSize(new Dimension(0, 150));
			bottomPanel.add(cTextArea, BorderLayout.CENTER);
			bottomPanel.add(checkPanel, BorderLayout.SOUTH);
			add(bottomPanel, BorderLayout.SOUTH);
		} else {
			add(checkPanel, BorderLayout.SOUTH);
		}

		if (BotLoader.getHeightWidth() == null) {
			pack();
			setMinimumSize(getSize());
		}

		client.init();
		stub.setActive(true);
		client.start();

		addComponentListener(new ComponentAdapter() {
			private boolean initialSizeSet = false;

			@Override
			public void componentResized(final ComponentEvent e) {

				if (isVisible() && isDisplayable()) {
					final Point location = getLocationOnScreen();

					if (location != null) {
						final int windowWidth = getWidth();
						final int windowHeight = getHeight();
						final int x = location.x;
						final int y = location.y;
						setTitle(showWindowDebug
							? String.format("APOS (%s) - Window [H: %d, W: %d], Location [X: %d, Y: %d]", account, windowHeight, windowWidth, x, y)
							: String.format("APOS (%s)", account));
					}
				}

				if (!initialSizeSet) {
					initialSizeSet = true;
					pack();
					setMinimumSize(getSize());

					if (BotLoader.getHeightWidth() != null) {
						System.out.println("Setting window dimensions");
						setSize(BotLoader.getHeightWidth().width, BotLoader.getHeightWidth().height);
					}

					if (BotLoader.getLocation() != null) {
						System.out.println("Setting location");
						setBounds(BotLoader.getLocation().x, BotLoader.getLocation().y, getWidth(), getHeight());
					}

				}
			}

			@Override
			public void componentMoved(final ComponentEvent e) {
				if (isVisible() && ((Component) client).isDisplayable() && ((Applet) client).isVisible()) {
					final Point location = ((Component) client).getLocationOnScreen();

					if (location != null) {
						final int windowWidth = getWidth();
						final int windowHeight = getHeight();
						final int x = location.x;
						final int y = location.y;
						setTitle(showWindowDebug
							? String.format("APOS (%s) - Window [H: %d, W: %d], Location [X: %d, Y: %d]", account, windowHeight, windowWidth, x, y)
							: String.format("APOS (%s)", account));
					}
				}
			}
		});

		((Component) client)
			.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentResized(final ComponentEvent e) {
					final int w = ((Component) client).getWidth();
					final int h = ((Component) client).getHeight();

					clientInit.getPaintListener().doResize(w, h);
				}
			});

	}

	private void quit() {
		clientInit.getScriptListener().setScriptRunning(false);
		if (stub != null) {
			stub.setActive(false);
		}
		client.stop();
		final IJokerFOCR joker = clientInit.getJoker();
		if (joker.isLibraryLoaded()) {
			joker.close();
		}
		dispose();
		System.exit(0);
	}

	@Override
	public void setVisible(final boolean visible) {
		if (visible) {
			setLocationRelativeTo(null);
			toFront();
			requestFocus();
		}
		super.setVisible(visible);
	}

	private static Map<String, String> getBaseParameters() {
		final Map<String, String> params = new HashMap<>();
		params.put("nodeid", "3601"); //was 3235
		params.put("modewhere", "1");
		params.put("modewhat", "0");
		params.put("servertype", "1");
		params.put("js", "1");
		params.put("settings", "wwGlrZHF5gKN6D3mDdihco3oPeYN2KFybL9hUUFqOvk");
		return params;
	}

	public static void setColours(final Component c) {
		c.setFont(Constants.UI_FONT);
		c.setBackground(Color.BLACK);
		c.setForeground(Color.WHITE);
	}

	private void updateWorld(final int i) {
		final String wanted = worldChoice.getItem(i);
		final URL url;
		final String nodeid;
		final String serverType;
		try {
			url = new URL("http://game.openrsc.com/");
			nodeid = "3601"; //was 3235
			serverType = "1";

			stub.setDocumentBase(url);
			stub.setCodeBase(url);

			stub.setParameter("nodeid", nodeid);
			stub.setParameter("servertype", serverType);
			worldChoice.select(i);
		} catch (final MalformedURLException e) {
			e.printStackTrace();
		}
	}

	private static void setButtonColours(final Button b) {
		b.setFont(Constants.UI_FONT);
		b.setForeground(SystemColor.controlText);
		b.setBackground(SystemColor.control);
	}

	public void updateStartButton(final boolean scriptEnabled) {
		startButton.setLabel(scriptEnabled ? "Stop script" : "Start script");
		System.out.println(clientInit.getScriptListener().getScriptName() + (scriptEnabled ? " started" : " stopped") + ".");
	}

	public void updateAutoLoginCheckBox(final boolean enabled) {
		loginCheck.setState(enabled);
		System.out.println("Autologin " + (enabled ? "enabled." : "disabled."));
	}

	private void stopScript() {
		final IScriptListener listener = clientInit.getScriptListener();
		listener.setScriptRunning(false);
	}

	private void startScript() {
		final IScriptListener listener = clientInit.getScriptListener();
		if (listener.hasScript()) {
			listener.setScriptRunning(true);
		} else {
			System.out.println("No script selected!");
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

	private static class ImagePanel extends Panel {
		private static final long serialVersionUID = 4557522767188007469L;

		private final Image image;

		ImagePanel(final Image image) {
			this.image = image;
		}

		@Override
		public void paint(final Graphics g) {
			super.paint(g);
			if (image != null) {
				g.drawImage(image, 0, 0, null);
			}
		}
	}


//	/**
//	 * Gets game parameters from the old classic.runescape.com url
//	 *
//	 * @param classicUrl
//	 * @return
//	 */
//    public Map<String, String> getParameters(String classicUrl) {
//        final String rsc_page;
//        try {
//            byte[] b = HTTPClient.load(classicUrl +
//                    "plugin.js?param=o0,a1,s0", classicUrl, true);
//            rsc_page = new String(b, Constants.UTF_8);
//        } catch (final Throwable t) {
//            System.out.println("Error fetching RSC page: " + t.toString());
//            dispose();
//            return new HashMap<>();
//        }
//
//        return HTTPClient.getParameters(rsc_page);
//    }
}
