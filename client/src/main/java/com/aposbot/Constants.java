package com.aposbot;

import com.aposbot.utility.BobRand;

import javax.imageio.ImageIO;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

public final class Constants {
	private static final Path PATH_ASSET = Paths.get("assets");

	public static final Path PATH_CONTENT = PATH_ASSET.resolve("content");
	public static final Path PATH_ICON = PATH_ASSET.resolve("icon");
	public static final Path PATH_LOGO = PATH_ASSET.resolve("logo");
	public static final Path PATH_MAP = PATH_ASSET.resolve("map");
	public static final Path PATH_SLEEP = PATH_ASSET.resolve("sleep");

	public static final Path PATH_LIB = Paths.get("lib");
	public static final Path PATH_ACCOUNT = Paths.get("accounts");
	public static final Path PATH_SCREENSHOT = Paths.get("screenshots");
	public static final Path PATH_SCRIPT = Paths.get("script", "out");
	public static final Path PATH_SCRIPT_SOURCE = Paths.get("script", "src", "main", "java");

	public static final Image ICON_16 = readImage("icon16.png");
	public static final Image ICON_22 = readImage("icon22.png");
	public static final Image ICON_32 = readImage("icon32.png");

	public static final List<Image> ICONS = new ArrayList<>();

	public static final Random RANDOM = new BobRand();

	public static final String RSAKEY_URANIUM_MEMB = "7112866275597968156550007489163685737528267584779959617759901583041864787078477876689003422509099353805015177703670715380710894892460637136582066351659813";
	public static final String RSAEXPONENT_URANIUM_MEMB = "65537";

	public static final int OP_NPC_ATTACK = 190;
	public static final int OP_NPC_ACTION = 202;
	public static final int OP_NPC_TALK = 153;
	public static final int OP_NPC_USEWITH = 135;
	public static final int OP_NPC_CAST = 50;

	public static final int OP_PLAYER_CAST = 229;
	public static final int OP_PLAYER_ATTACK = 171;
	public static final int OP_PLAYER_TRADE = 142;
	public static final int OP_PLAYER_FOLLOW = 165;
	public static final int OP_PLAYER_USEWITH = 113;

	public static final int OP_INV_ACTION = 90;
	public static final int OP_INV_DROP = 246;
	public static final int OP_INV_EQUIP = 169;
	public static final int OP_INV_UNEQUIP = 170;
	public static final int OP_INV_USEWITH = 91;
	public static final int OP_INV_CAST = 4;

	public static final int OP_GITEM_TAKE = 247;
	public static final int OP_GITEM_USEWITH = 53;
	public static final int OP_GITEM_CAST = 249;

	public static final int OP_OBJECT_USEWITH = 115;
	public static final int OP_OBJECT_ACTION1 = 136;
	public static final int OP_OBJECT_ACTION2 = 79;

	public static final int OP_BOUND_USEWITH = 161;
	public static final int OP_BOUND_ACTION1 = 14;
	public static final int OP_BOUND_ACTION2 = 127;

	public static final int OP_BANK_DEPOSIT = 23;
	public static final int OP_BANK_WITHDRAW = 22;
	public static final int OP_BANK_CLOSE = 212;

	public static final int OP_TRADE_ACCEPT = 55;
	public static final int OP_TRADE_CONFIRM = 104;
	public static final int OP_TRADE_DECLINE = 230;

	public static final int OP_SHOP_BUY = 236;
	public static final int OP_SHOP_SELL = 221;
	public static final int OP_SHOP_CLOSE = 166;

	public static final int OP_PRAYER_ENABLE = 60;
	public static final int OP_PRAYER_DISABLE = 254;

	public static final int OP_SELF_CAST = 137;
	public static final int OP_DIALOG_ANSWER = 116;
	public static final int OP_SET_COMBAT_STYLE = 29;
	public static final int OP_LOGOUT = 102;

	public static final Font UI_FONT = getUIFont();

	public static final String DEFAULT_JAR = "rsclassic.jar";

	static {
		ICONS.add(ICON_16);
		ICONS.add(ICON_22);
		ICONS.add(ICON_32);
	}

	private Constants() {
	}

	private static Image readImage(final String image) {
		try {
			return ImageIO.read(PATH_ICON.resolve(image).toFile());
		} catch (final Throwable t) {
			System.out.println("Error loading icon: " + t);
		}
		return null;
	}

	private static Font getUIFont() {
		final Font _default = new Canvas().getFont();
		int style = Font.PLAIN;
		int size = 12;
		if (_default != null) {
			style = _default.getStyle();
			size = _default.getSize();
		}
		final Properties p = BotLoader.getProperties();
		String name = null;
		if (p != null) {
			name = p.getProperty("font");
		}
		if (name == null || name.isEmpty()) {
			name = Font.SANS_SERIF;
		}
		return new Font(name, style, size);
	}
}
