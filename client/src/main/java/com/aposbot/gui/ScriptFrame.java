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

	private String latestScriptSearchFieldText = ""; // Needed because the last char in keyPressed is not yet in scriptSearchField.getText()

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
				// Get the current search text (little complicated to get the last char)
				if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE && scriptSearchField.getSelectedText().length() > 0)
					latestScriptSearchFieldText = scriptSearchField.getText().substring(0, scriptSearchField.getSelectionStart());
				else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE && scriptSearchField.getText().length() > 0)
					latestScriptSearchFieldText = scriptSearchField.getText().substring(0, scriptSearchField.getText().length() - 1);
				else
					latestScriptSearchFieldText = scriptSearchField.getText() + e.getKeyChar();

				String currentSelection = displayed_list.getItemCount() > 0 ? displayed_list.getSelectedItem() : "";
				update();
				if (displayed_list.getItemCount() > 0) displayed_list.select(0);
				// if the current selection is still in the list, select it again
				for (int i = 0; i < displayed_list.getItemCount(); i++) {
					if (displayed_list.getItem(i).equals(currentSelection)) {
						displayed_list.select(i);
						break;
					}
				}

				// If Ctrl-A is pressed, select all the text
				if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_A) {
					scriptSearchField.selectAll();
				}

				// If Esc is pressed, close the window
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					setVisible(false);
				}

				// If Down is pressed, select the next item
				if (e.getKeyCode() == KeyEvent.VK_DOWN) {
					if (displayed_list.getSelectedIndex() < displayed_list.getItemCount() - 1) {
						displayed_list.select(displayed_list.getSelectedIndex() + 1);
					}
				}

				// If Up is pressed, select the previous item
				if (e.getKeyCode() == KeyEvent.VK_UP) {
					if (displayed_list.getSelectedIndex() > 0) {
						displayed_list.select(displayed_list.getSelectedIndex() - 1);
					}
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
        // Check if the OS is non-Windows
        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            pb.command("sh", "-c", "chmod +x compile_scripts-linux.sh && ./compile_scripts-linux.sh");
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
		final String[] files = Constants.PATH_SCRIPT.toFile().list();

		if (files == null) {
			return;
		}

		final java.util.List<String> list = new ArrayList<>();

		for (final String file : files) {
			process(file, list);
		}

		Collections.sort(list);

		displayed_list.removeAll();

		for (final String str : list) {
			displayed_list.add(str);
		}

		if (lastSelectedIndex > 0 && lastSelectedIndex < displayed_list.getItemCount())
			displayed_list.select(lastSelectedIndex);
	}

	private void process(final String fileName, final List<String> list) {
		if (!fileName.endsWith(".class") || fileName.indexOf('$') != -1) {
			return;
		}

		final File file = new File(fileName);

		if (file.isDirectory()) {
			final String[] fileList = file.list();

			if (fileList != null) {
				for (final String name : fileList) {
					process(name, list);
				}
			}
		}

		String search = latestScriptSearchFieldText;
		// If all the letters of search are in fileName, add it to the list
		// Create a dictionary of the letters in search, and subtract from it as we find them in fileName
		// If the dictionary is empty, all the letters in search were found in fileName
		final Map<Character, Integer> searchLetters = new HashMap<>();
		for (final char c : search.toLowerCase().toCharArray()) {
			if (!Character.isLetterOrDigit(c))
				continue;
			searchLetters.put(c, searchLetters.getOrDefault(c, 0) + 1);
		}
		String filenameBeforeExtension = fileName.toLowerCase().substring(0, fileName.lastIndexOf('.'));
		for (final char c : filenameBeforeExtension.toCharArray()) {
			if (!searchLetters.containsKey(c))
				continue;
			final int count = searchLetters.get(c);
			if (count == 1) {
				searchLetters.remove(c);
			} else {
				searchLetters.put(c, count - 1);
			}
		}
		if (searchLetters.isEmpty()) {
			list.add(fileName);
		}
	}
}
