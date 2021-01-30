package com.aposbot;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import com.aposbot._default.IAutoLogin;
import com.aposbot._default.IClient;
import com.aposbot._default.IClientInit;
import com.aposbot._default.IJokerFOCR;
import com.aposbot._default.IPaintListener;
import com.aposbot._default.IScriptListener;
import com.aposbot.applet.AVStub;

public final class BotFrame extends Frame {

	private static final long serialVersionUID = -2847514806687135697L;
	private Checkbox loginCheck;
	private Checkbox gfxCheck;
	private Button startButton;
	private Debugger debugger;
	private ScriptFrame scriptFrame;
	private Choice worldChoice;
	private AVStub stub;
	private IClient client;

	BotFrame(IClientInit init, final TextArea cTextArea, String account) {
		super("APOS (" + account + ")");
		setFont(Constants.UI_FONT);
		setIconImages(Constants.ICONS);

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				quit();
			}
		});

		if (SystemTray.isSupported()) {
			final TrayIcon icon = new TrayIcon(Constants.ICON_16, "APOS (" + account + ")");
			icon.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					final BotFrame t = BotFrame.this;
					t.setVisible(!t.isVisible());
				}
			});
			try {
				SystemTray.getSystemTray().add(icon);
			} catch (final Throwable t) {
			}
		}
		
		//final int defaultWorld = Constants.RANDOM.nextBoolean() ? 2 : 3;
		final int defaultWorld = 2 + Constants.RANDOM.nextInt(4);

		final String str = "http://classic" + defaultWorld + ".runescape.com/";

		Map<String, String> params = getBaseParameters();

		client = init.createClient(this);
		((Component)client).setBackground(Color.BLACK);

		try {
			final URL url = new URL(str);
			stub = new AVStub((Applet) client, url, url, params);
		} catch (final Throwable t) {
			t.printStackTrace();
			dispose();
			return;
		}

		client.setStub(stub);

		BufferedImage image = null;
		try {
			image = ImageIO.read(new File("." + File.separator + "lib" + File.separator + "logo.png"));
		} catch (final Throwable t) {
			System.out.println("Error loading logo: " + t.toString());
		}

		final Panel sidePanel = new ImagePanel(image);
		setColours(sidePanel, true);
		sidePanel.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));

		final Dimension buttonSize = new Dimension(120, 23);

		final Choice worldChoice = new Choice();
		worldChoice.setPreferredSize(buttonSize);
		worldChoice.setForeground(SystemColor.textText);
		worldChoice.setBackground(SystemColor.text);
		addModernAllowedWorlds(worldChoice);
		worldChoice.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent event) {
				updateWorld(worldChoice.getSelectedIndex() + 1);
			}
		});
		this.worldChoice = worldChoice;

		final Button chooseButton = new Button("Choose script");
		chooseButton.setPreferredSize(buttonSize);
		setButtonColours(chooseButton);
		chooseButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (scriptFrame == null) {
					scriptFrame = new ScriptFrame(client);
				}
				scriptFrame.setLocationRelativeTo(BotFrame.this);
				scriptFrame.setVisible(true);
			}
		});

		startButton = new Button("Start script");
		startButton.setPreferredSize(buttonSize);
		setButtonColours(startButton);
		startButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (client.getScriptListener().isScriptRunning()) {
					stopScript();
				} else {
					startScript();
				}
			}
		});

		final Button debugButton = new Button("Debugger");
		debugButton.setPreferredSize(buttonSize);
		setButtonColours(debugButton);
		debugButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (debugger == null) {
					debugger = new Debugger(client);
				}
				debugger.setLocationRelativeTo(BotFrame.this);
				debugger.setVisible(true);
			}
		});

		final Button scrButton = new Button("Screenshot");
		scrButton.setPreferredSize(buttonSize);
		setButtonColours(scrButton);
		scrButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						takeScreenshot(String.valueOf(System.currentTimeMillis()));
					}
				}, "ScreenshotThread").start();
			}
		});

		final Button exitButton = new Button("Exit");
		exitButton.setPreferredSize(buttonSize);
		setButtonColours(exitButton);
		exitButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				quit();
			}
		});

		sidePanel.add(worldChoice);
		sidePanel.add(chooseButton);
		sidePanel.add(startButton);
		sidePanel.add(debugButton);
		sidePanel.add(scrButton);
		sidePanel.add(exitButton);

		final Panel checkPanel = new Panel();
		setColours(checkPanel, true);
		checkPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 0));

		loginCheck = new Checkbox("Autologin");
		setColours(loginCheck, true);
		loginCheck.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				final IAutoLogin al = client.getAutoLogin();
				al.setEnabled(loginCheck.getState());
			}
		});

		gfxCheck = new Checkbox("Rendering", true);
		setColours(gfxCheck, true);
		gfxCheck.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				client.setRendering(gfxCheck.getState());
			}
		});
		gfxCheck.setEnabled(false);

		final Checkbox paintCheck = new Checkbox("Show bot layer",
		    true);
		setColours(paintCheck, true);
		paintCheck.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				final IPaintListener paint = client.getPaintListener();
				paint.setPaintingEnabled(paintCheck.getState());
			}
		});

		final Checkbox r3d = new Checkbox("Plain 3D", true);
		setColours(r3d, true);
		r3d.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				final IPaintListener paint = client.getPaintListener();
				paint.setRenderSolid(r3d.getState());
			}
		});

		final Checkbox t3d = new Checkbox("Textured 3D", true);
		setColours(t3d, true);
		t3d.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				final IPaintListener paint = client.getPaintListener();
				paint.setRenderTextures(t3d.getState());
			}
		});

		checkPanel.add(loginCheck);
		checkPanel.add(gfxCheck);
		checkPanel.add(paintCheck);
		checkPanel.add(r3d);
		checkPanel.add(t3d);

		((Component)client)
		    .addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				int w = ((Component)client).getWidth();
				int h = ((Component)client).getHeight();
				client.getPaintListener().doResize(w, h);
			}
		});

		add((Component)client, BorderLayout.CENTER);
		add(sidePanel, BorderLayout.EAST);

		if (cTextArea != null) {
			final Panel bottomPanel = new Panel();
			setColours(bottomPanel, true);
			bottomPanel.setLayout(new BorderLayout());
			cTextArea.setPreferredSize(new Dimension(0, 150));
			bottomPanel.add(cTextArea, BorderLayout.CENTER);
			bottomPanel.add(checkPanel, BorderLayout.SOUTH);
			add(bottomPanel, BorderLayout.SOUTH);
		} else {
			add(checkPanel, BorderLayout.SOUTH);
		}

		pack();
		setMinimumSize(getSize());
		ExecutorService executorService = Executors.newSingleThreadExecutor();

		client.init();
		stub.setActive(true);
		client.start();
		
		
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
		
		// original jar doesnt want to load jar if doesn't come from *.runescape.com
		// so here we set a timeout after it has done that check to change the
		// world
		ScheduledFuture<?> countdown = scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                // do the thing
            	updateWorld(0);
            }}, 2, TimeUnit.SECONDS);

        while (!countdown.isDone()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        scheduler.shutdown();
		
	}

	private void quit() {
		client.getScriptListener().setScriptRunning(false);
		if (stub != null) {
			stub.setActive(false);
		}
		if (client != null) {
			client.stop();
		}
		final IJokerFOCR joker = client.getJoker();
		if (joker.isLibraryLoaded()) {
			joker.close();
		}
		dispose();
		System.exit(0);
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			setLocationRelativeTo(null);
			toFront();
			requestFocus();
		}
		super.setVisible(visible);
	}

	static void setColours(Component c, boolean invert) {
		c.setFont(Constants.UI_FONT);
		if (invert) {
			c.setBackground(Color.BLACK);
			c.setForeground(Color.WHITE);
		} else {
			c.setBackground(Color.WHITE);
			c.setForeground(Color.BLACK);
		}
	}

	private static void setButtonColours(Button b) {
		b.setFont(Constants.UI_FONT);
		b.setForeground(SystemColor.controlText);
		b.setBackground(SystemColor.control);
	}

	void startScript() {
		final IScriptListener listener = client.getScriptListener();
		if (listener.hasScript()) {
			listener.setScriptRunning(true);
			startButton.setLabel("Stop script");
			System.out.println(listener.getScriptName() + " started.");
		} else {
			System.out.println("No script selected!");
		}
	}

	public void stopScript() {
		final IScriptListener listener = client.getScriptListener();
		listener.setScriptRunning(false);
		startButton.setLabel("Start script");
		client.setKeysDisabled(false);
		System.out.println(listener.getScriptName() + " stopped.");
	}

	public void takeScreenshot(String fileName) {
		final String name = "." + File.separator + "Screenshots" + File.separator + fileName + ".png";
		final Image image = client.getImage();
		final BufferedImage b = new BufferedImage(image.getWidth(null),
		image.getHeight(null), BufferedImage.TYPE_INT_RGB);
		final Graphics g = b.createGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();
		try {
			ImageIO.write(b, "png", new File(name));
			System.out.println("Saved " + name + ".");
		} catch (final Throwable t) {
			System.out.println("Error taking screenshot: " + t.toString());
		}
	}

	public String getCodeBase() {
		return stub.getCodeBase().toString();
	}
	
	/* Gets parameters for use in RSC Uranium (Membs) */
	public Map<String, String> getBaseParameters() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("nodeid", "3235");
		params.put("modewhere", "1");
		params.put("modewhat", "0");
		params.put("servertype", "1");
		params.put("js", "1");
		params.put("settings", "wwGlrZHF5gKN6D3mDdihco3oPeYN2KFybL9hUUFqOvk");
		return params;
	}
	
	/* Gets game parameters from the old classic.runescape.com url */
	public Map<String, String> getParameters(String classicUrl) {
		final String rsc_page;
		try {
			byte[] b = HTTPClient.load(classicUrl +
				"plugin.js?param=o0,a1,s0", classicUrl, true);
			rsc_page = new String(b, Constants.UTF_8);
		} catch (final Throwable t) {
				System.out.println("Error fetching RSC page: " + t.toString());
				dispose();
				return new HashMap<String, String>();
		}
		
		return HTTPClient.getParameters(rsc_page);
	}

	public void updateWorld(int i) {
		String wanted = worldChoice.getItem(i);
		URL url;
		String nodeid;
		String serverType;
		try {
			if (wanted.contains("RSC Uranium")) {
				url = new URL("http://game.openrsc.com/");
				nodeid = "3235";
				serverType = "1";
				client.getParentInit().setRSAKey(Constants.RSAKEY_URANIUM_MEMB);
				client.getParentInit().setRSAExponent(Constants.RSAEXPONENT_URANIUM_MEMB);
			} else {
				int j = 2;
				Pattern pattern = Pattern.compile("\"([0-9]+)\"");
				Matcher matcher = pattern.matcher(wanted);

		        if (matcher.find()) {
		        	j = Integer.parseInt(matcher.group());
		        }
		        url = new URL("http://classic" + j + ".runescape.com/");
		        nodeid = String.valueOf(5000 + j);
		        serverType = j == 1 ? "3" : "1";
			}
			
			stub.setDocumentBase(url);
			stub.setCodeBase(url);
			
			stub.setParameter("nodeid", nodeid);
			stub.setParameter("servertype", serverType);
			worldChoice.select(i);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void addJagexClassicWorlds(Choice worlds) {
		worlds.add("World # 1");
		worlds.add("World # 2");
		worlds.add("World # 3");
		worlds.add("World # 4");
		worlds.add("World # 5");
	}
	
	public void addModernAllowedWorlds(Choice worlds) {
		worlds.add("RSC Uranium (Membs)");
	}

	public void setAutoLogin(boolean b) {
		loginCheck.setState(b);
		client.getAutoLogin().setEnabled(b);
	}

	public void enableRenderingToggle() {
		gfxCheck.setEnabled(true);
	}
}
