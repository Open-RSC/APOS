package com.aposbot.gui;

import com.aposbot._default.IClient;
import com.aposbot._default.IClientInit;
import com.aposbot._default.IScript;
import com.aposbot._default.IScriptListener;
import com.aposbot.Constants;
import com.aposbot.utility.ScriptClassLoader;
import com.aposbot.StandardCloseHandler;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;

/**
 * Displays compiled scripts in a GUI and loads selected scripts.
 */
public final class ScriptFrame extends Frame {
	private static final long serialVersionUID = -7439187170235715096L;
	private static final String PROCESSING_ERROR = "Error processing script. Send this output to the script's author:";

	private final IClient client;
	private final IClientInit clientInit;

	private final java.awt.List displayed_list;

	private final TextField field;
	private final TextField scriptSearchField;

	private ScriptEngineManager manager;

	private int lastSelectedIndex;

	public ScriptFrame(final IClient client, final IClientInit clientInit) {
		super("Scripts");
		setFont(Constants.UI_FONT);

		this.client = client;
		this.clientInit = clientInit;

		setIconImages(Constants.ICONS);
		addWindowListener(new StandardCloseHandler(this, StandardCloseHandler.HIDE));

		final Panel fpanel = new Panel();
		fpanel.setLayout(new GridLayout(1, 0));
		final Label l = new Label("Parameters:");
		l.setFont(Constants.UI_FONT);
		fpanel.add(l);
		fpanel.add(field = new TextField(""));
		field.setFont(Constants.UI_FONT);
		add(fpanel, BorderLayout.NORTH);

		displayed_list = new java.awt.List();
		displayed_list.setFont(Constants.UI_FONT);
		final ScrollPane scroll = new ScrollPane();
		scroll.setFont(Constants.UI_FONT);
		scroll.add(displayed_list);
		add(scroll, BorderLayout.CENTER);

		final Panel searchPanel = new Panel();
		searchPanel.setLayout(new GridLayout(1, 0));
		final Label searchLabel = new Label("Search:");
		searchLabel.setFont(Constants.UI_FONT);
		searchPanel.add(searchLabel);

		scriptSearchField = new TextField("");
		scriptSearchField.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(final KeyEvent e) {
			}

			@Override
			public void keyPressed(final KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					displayed_list.select(getMatchedScriptIndex());
				}
			}

			@Override
			public void keyReleased(final KeyEvent e) {
			}
		});

		searchPanel.add(scriptSearchField);
		scriptSearchField.setFont(Constants.UI_FONT);

		final Panel okCancelButtonPanel = new Panel();

		final Button okButton = new Button("OK");
		okButton.setFont(Constants.UI_FONT);
		okButton.addActionListener(e -> {
			if (displayed_list.getSelectedIndex() == -1) {
				System.out.println("Script not selected.");
			} else {
				new Thread(this::initScript, "ScriptInit").start();
			}
		});
		okCancelButtonPanel.add(okButton);

		final Button cancelButton = new Button("Cancel");
		cancelButton.setFont(Constants.UI_FONT);
		cancelButton.addActionListener(e -> setVisible(false));
		okCancelButtonPanel.add(cancelButton);

		final Panel openCompileScriptsPanel = new Panel();

		final Button openScriptsButton = new Button("Scripts Folder");
		openScriptsButton.setFont(Constants.UI_FONT);
		openScriptsButton.addActionListener(e -> {
			try {
				Desktop.getDesktop().open(Constants.PATH_SCRIPT_SOURCE.toFile());
			} catch (final Exception ex) {
				ex.printStackTrace();
			}
		});
		openCompileScriptsPanel.add(openScriptsButton);

		final Button compileScriptsButton = new Button("Compile Scripts");
		compileScriptsButton.setFont(Constants.UI_FONT);
		compileScriptsButton.addActionListener(e -> {
			try {
				final ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "compile_scripts.cmd");
				// If we're on Linux, use bash instead of cmd
				if (System.getProperty("os.name").toLowerCase().contains("linux")) {
					pb.command("bash", "-c", "./compile_scripts-linux.sh");
				}
				// execute
				final Process p = pb.start();
				// get both the output and the error stream
				final SequenceInputStream merged = new SequenceInputStream(p.getInputStream(), p.getErrorStream());
				final BufferedReader br = new BufferedReader(new InputStreamReader(merged));
				// read the output
				br.lines().forEach(System.out::println);
				// wait for the process to finish
				p.waitFor();
				// get the exit code
				final int exitCode = p.exitValue();
				// Close the streams
				br.close();
				System.out.println("Exit code: " + exitCode);

				// Close the frame
				setVisible(false);
			} catch (final Exception ex) {
				ex.printStackTrace();
			}
		});
		openCompileScriptsPanel.add(compileScriptsButton);

		final Panel southPanel = new Panel();
		southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));
		southPanel.add(searchPanel);
		southPanel.add(okCancelButtonPanel);
		southPanel.add(openCompileScriptsPanel);
		add(southPanel, BorderLayout.SOUTH);

		pack();
		setMinimumSize(getSize());

		final Insets in = getInsets();

		setSize(in.right + in.left + 310, in.top + in.bottom + 240);
	}

	@Override
	public void setVisible(final boolean visible) {
		if (visible) {
			update();
			toFront();
			requestFocus();
		}
		super.setVisible(visible);
	}

	public IScript initJavaScript(final String name) {
		final Class<?> c;

		try {
			c = new ScriptClassLoader().loadClass(name.substring(0, name.indexOf(".class")));
		} catch (final ClassNotFoundException | MalformedURLException e) {
			System.out.println("Error loading script:");
			e.printStackTrace();
			return null;
		}

		if (!IScript.class.isAssignableFrom(c)) {
			System.out.println("Error: " + name + " is not a valid Java script.");
			return null;
		}

		try {
			return (IScript) c.getConstructor(Class.forName("Extension")).newInstance(client);
		} catch (final Throwable t) {
			System.out.println("Failed to load script " + name + ":");
			t.printStackTrace();
		}

		return null;
	}

	private int getMatchedScriptIndex() {
		final String searchedScriptName = scriptSearchField.getText().toLowerCase();
		final String[] availableScriptNames = displayed_list.getItems();
		if (availableScriptNames.length == 0)
			return -1;

		final Map<String, Integer> scriptSearchMatches = new TreeMap<>((s1, s2) -> {
			if (s1.length() > s2.length()) {
				return 1;
			} else if (s1.length() < s2.length()) {
				return -1;
			} else {
				return s1.compareTo(s2);
			}
		});

		for (int i = 0; i < availableScriptNames.length; i++) {
			final String availableScriptName = availableScriptNames[i].toLowerCase();

			if (searchedScriptName.equals(availableScriptName)) {
				return i;
			}

			if (availableScriptName.contains(searchedScriptName)) {
				scriptSearchMatches.put(availableScriptName, i);
			}
		}

		if (scriptSearchMatches.isEmpty())
			return -1;

		return scriptSearchMatches.values().stream().findFirst().get();
	}

	private void initScript() {
		final IScriptListener listener = clientInit.getScriptListener();

		if (listener.isScriptRunning()) {
			System.out.println("Stop the script you are running first.");
			return;
		}

		listener.setIScript(null);
		System.gc();

		lastSelectedIndex = displayed_list.getSelectedIndex();

		final String selected_name = displayed_list.getSelectedItem();

		final IScript script;

		if (selected_name.endsWith(".class")) {
			script = initJavaScript(selected_name);
		} else {
			script = initJSEScript(selected_name);
		}

		if (script == null) return;

		System.out.println("Selected script: " + script);

		try {
			script.init(field.getText());
		} catch (final Throwable t) {
			if (t.getMessage() != null) {
				System.err.println(t.getMessage());
			} else {
				System.err.println(PROCESSING_ERROR);
				t.printStackTrace();
			}
			return;
		}

		clientInit.getScriptListener().setIScript(script);
		System.out.println("Press the \"Start script\" button to start it.");
		setVisible(false);
	}

	private IScript initJSEScript(final String name) {
		if (manager == null) {
			manager = new ScriptEngineManager();
		}

		final int dot = name.indexOf('.');

		if (dot == -1) {
			System.out.println("Error: " + name + " has no file extension.");
			return null;
		}

		final String ext = name.substring(dot + 1);

		final ScriptEngine engine = manager.getEngineByExtension(ext);

		if (engine == null) {
			System.out.println("Error: No script engine found for " + ext);
			return null;
		}

		if (!(engine instanceof Invocable)) {
			System.out.println("Error: Engine for " + ext + " is not invocable.");
		}

		final File file = Constants.PATH_SCRIPT.resolve(name).toFile();

		try (final FileInputStream fileInputStream = new FileInputStream(file);
			 final InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
			 final BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
			engine.eval(bufferedReader);
		} catch (final Throwable t) {
			System.out.println("Error loading script:");
			t.printStackTrace();
			return null;
		}

		return client.createInvocableScript((Invocable) engine, name);
	}

	private void update() {
		final File[] files = Constants.PATH_SCRIPT.toFile().listFiles();

		if (files == null) {
			return;
		}

		final java.util.List<String> list = new ArrayList<>();

		for (final File file : files) {
			process(file, list);
		}

		Collections.sort(list);

		displayed_list.removeAll();

		for (final String str : list) {
			displayed_list.add(str);
		}

		if (lastSelectedIndex >= 0 && lastSelectedIndex < displayed_list.getItemCount())
			displayed_list.select(lastSelectedIndex);

		if (list.isEmpty()) {
			System.err.println("Did yah forget to compile yer scripts!?");
		}
	}

	private void process(final File file, final List<String> list) {
		if (file.isDirectory()) {
			final File[] fileList = file.listFiles();
			if (fileList != null) {
				for (final File subfile : fileList) {
					process(subfile, list);
				}
			}
		}

		if (!file.getName().endsWith(".class") || file.getName().indexOf('$') != -1) {
			return;
		}

		list.add(file.getName());
	}
}
