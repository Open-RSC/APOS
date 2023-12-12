package com.aposbot.gui;

import com.aposbot.BotLoader;
import com.aposbot.Constants;
import com.aposbot.StandardCloseHandler;
import com.aposbot._default.IClientInit;
import com.aposbot._default.ISleepListener.OCRType;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/**
 * Presents the user with account and sleep solver selection.
 */
public final class EntryFrame extends Frame {
	private AuthFrame authFrame;
	public String[] accountNames;
	public String account;

	public Button okButton;

	public EntryFrame(final BotLoader botLoader, final IClientInit clientInit) {
		super("APOS");

		setFont(Constants.UI_FONT);
		addWindowListener(new StandardCloseHandler(this, StandardCloseHandler.EXIT));
		setIconImages(Constants.ICONS);
		setResizable(false);

		loadAccounts();

		final Panel accountPanel = new Panel();
		accountPanel.add(new Label("Autologin account:"));

		final Choice accountChoice = new Choice();
		accountChoice.setPreferredSize(new Dimension(150, 15));

		for (final String accountName : accountNames) {
			if (Objects.equals(botLoader.getCmdUsername().toLowerCase(), accountName.toLowerCase())) {
				account = accountName;
				accountChoice.add(accountName);
			}
		}

		for (final String accountName : accountNames) {
			if (!Objects.equals(botLoader.getCmdUsername(), accountName))
				accountChoice.add(accountName);
		}

		if ((account == null || account.isEmpty()) && accountNames.length > 0) {
			account = accountNames[0];
		}

		accountChoice.addItemListener(event -> account = String.valueOf(event.getItem()));

		accountPanel.add(accountChoice);

		final Button addButton = new Button("Add");
		addButton.addActionListener(e -> {
			if (authFrame == null) {
				final AuthFrame authFrame = new AuthFrame("Add an account", null, EntryFrame.this);
				authFrame.setFont(Constants.UI_FONT);
				authFrame.setIconImages(Constants.ICONS);
				authFrame.addActionListener(e1 -> {
					final Properties p = new Properties();
					final String u = EntryFrame.this.authFrame.getUsername();
					p.put("username", u);
					p.put("password", EntryFrame.this.authFrame.getPassword());
					final File file = Constants.PATH_ACCOUNT.resolve(u + ".properties").toFile();
					try (final FileOutputStream out = new FileOutputStream(file)) {
						p.store(out, null);
					} catch (final Throwable t) {
						System.out.println("Error saving account details: " + t);
					}
					accountChoice.add(u);
					accountChoice.select(u);
					account = u;
					EntryFrame.this.authFrame.setVisible(false);
				});
				EntryFrame.this.authFrame = authFrame;
			}
			authFrame.setVisible(true);
		});

		accountPanel.add(addButton);

		final Panel ocrPanel = new Panel(new GridLayout(0, 1, 2, 2));
		ocrPanel.add(new Label("OCR/Sleeper:"));
		final CheckboxGroup ocrGroup = new CheckboxGroup();
		final int defaultOcrIndex = botLoader.getDefaultOCR();
		for (final OCRType ocrType : OCRType.VALUES) {
			ocrPanel.add(new Checkbox(ocrType.getName(), ocrGroup, defaultOcrIndex == ocrType.getIndex()));
		}

		final Panel buttonPanel = new Panel();

		okButton = new Button("OK");
		okButton.addActionListener(e -> {
			if (authFrame != null) {
				authFrame.dispose();
			}
			try {
				loadUsername(clientInit, account);
				final OCRType ocrType = OCRType.fromName(ocrGroup.getSelectedCheckbox().getLabel());
				clientInit.getSleepListener().setOCRType(ocrType);
				if (ocrType != null && ocrType.getIndex() != botLoader.getDefaultOCR()) {
					botLoader.setDefaultOCR(ocrType.getIndex());
					botLoader.storeProperties(null);
				}
				dispose();
				final BotFrame botFrame = new BotFrame(clientInit, botLoader.getConsoleTextArea(), account);
				clientInit.getScriptListener().setBotFrame(botFrame);
				clientInit.getLoginListener().setBotFrame(botFrame);
				botFrame.setVisible(true);
				botLoader.setConsoleFrameVisible();
			} catch (final Throwable t) {
				t.printStackTrace();
			}
		});

		buttonPanel.add(okButton);

		final Button cancelButton = new Button("Cancel");
		cancelButton.addActionListener(e -> {
			dispose();
			System.exit(0);
		});

		buttonPanel.add(cancelButton);

		add(accountPanel, BorderLayout.NORTH);
		add(ocrPanel, BorderLayout.CENTER);
		add(buttonPanel, BorderLayout.SOUTH);

		pack();
	}

	private void loadAccounts() {
		try {
			final File dir = Constants.PATH_ACCOUNT.toFile();
			final String[] account_list = dir.list();
			final List<String> accounts = new ArrayList<>();
			if (account_list != null) {
				for (final String s : account_list) {
					if (s.endsWith("properties")) {
						accounts.add(s.replace(".properties", ""));
					}
				}
			}
			accountNames = new String[accounts.size()];
			accountNames = accounts.toArray(accountNames);
		} catch (final Throwable t) {
			System.out.println("Error loading accounts: " + t);
			accountNames = new String[0];
		}
	}

	private static void loadUsername(final IClientInit clientInit, final String name) {
		if (name == null) {
			System.out.println("You didn't enter an account to use with autologin.");
			System.out.println("You can still use APOS, but it won't be able to log you back in if you disconnect.");
			return;
		}
		final Properties p = new Properties();
		final File file = Constants.PATH_ACCOUNT.resolve(name + ".properties").toFile();
		try (final FileInputStream stream = new FileInputStream(file)) {
			p.load(stream);
			clientInit.getLoginListener().setAccount(p.getProperty("username"), p.getProperty("password"));
		} catch (final Throwable t) {
			System.out.println("Error loading account " + name + ": " + t);
		}
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
}
