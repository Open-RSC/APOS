package com.aposbot.applet;

import java.applet.Applet;
import java.applet.AppletContext;
import java.applet.AudioClip;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

class AVContext implements AppletContext {
	private final Applet applet;

	private Map<String, InputStream> streamMap;

	private Toolkit toolkit;

	AVContext(final Applet applet) {
		this.applet = applet;
	}

	@Override
	public AudioClip getAudioClip(final URL url) {
		return new AVClip(url);
	}

	@Override
	public Image getImage(final URL url) {
		if (toolkit == null) {
			toolkit = Toolkit.getDefaultToolkit();
		}
		return toolkit.getImage(url);
	}

	@Override
	public Applet getApplet(final String name) {
		if (applet.getName().equals(name)) {
			return applet;
		}
		return null;
	}

	@Override
	public Enumeration<Applet> getApplets() {
		final Vector<Applet> applets = new Vector<Applet>();
		applets.add(applet);
		return applets.elements();
	}

	@Override
	public void showDocument(final URL url) {
		try {
			Desktop.getDesktop().browse(url.toURI());
		} catch (final Throwable t) {
			t.printStackTrace();
		}
	}

	@Override
	public void showDocument(final URL url, final String target) {
		try {
			Desktop.getDesktop().browse(url.toURI());
		} catch (final Throwable t) {
			t.printStackTrace();
		}
	}

	@Override
	public void showStatus(final String status) {
	}

	@Override
	public void setStream(final String key, final InputStream stream) throws IOException {
		if (streamMap == null) {
			streamMap = new HashMap<String, InputStream>();
		}
		streamMap.put(key, stream);
	}

	@Override
	public InputStream getStream(final String key) {
		if (streamMap == null) {
			streamMap = new HashMap<String, InputStream>();
		}
		return streamMap.get(key);
	}

	@Override
	public Iterator<String> getStreamKeys() {
		return streamMap.keySet().iterator();
	}
}
