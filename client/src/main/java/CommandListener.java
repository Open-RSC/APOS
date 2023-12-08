import com.aposbot._default.IClient;
import com.aposbot.gui.BotFrame;

/**
 * Listens for ::commands typed in-game and toggles client features.
 */
final class CommandListener {
	static boolean renderRoofs;
	static boolean debug;
	static boolean showCombatStyleMenu;
	static boolean hideCombatStyleMenu;
	static boolean showReportPlayer;
	static boolean showWindowDebug;

	private CommandListener() {
	}

	static boolean commandHook(final String command) {
		final IClient client = Extension.getInstance();

		switch (command) {
			case "roofs":
				renderRoofs = !renderRoofs;
				client.displayMessage("@cya@roofs: " + renderRoofs);
				return false;
			case "debug":
				debug = !debug;
				client.displayMessage("@cya@debug: " + debug);
				return false;
			case "menu":
				showCombatStyleMenu = !showCombatStyleMenu;
				client.displayMessage("@cya@menu: " + showCombatStyleMenu);
				return false;
			case "window":
				BotFrame.showWindowDebug = !BotFrame.showWindowDebug;
				client.displayMessage(BotFrame.showWindowDebug
					? "@cya@window: move or resize window to display debug values"
					: "@cya@window: hidden");
				return false;
			default:
				return ScriptListener.getInstance().onCommand(command);
		}
	}
}
