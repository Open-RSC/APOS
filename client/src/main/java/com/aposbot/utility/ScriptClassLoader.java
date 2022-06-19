package com.aposbot.utility;

import com.aposbot.Constants;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public final class ScriptClassLoader extends URLClassLoader {
	public ScriptClassLoader() throws MalformedURLException {
		super(new URL[]{Constants.PATH_SCRIPT.toUri().toURL()});
	}
}
