import com.aposbot._default.IClient;

/**
 * Listens for ::commands typed in-game and toggles client features.
 */
final class CommandListener {
	static boolean debug;
	static boolean showCombatStyleMenu;
	static boolean hideCombatStyleMenu;
	static boolean showReportPlayer;

	private CommandListener() {
	}

	static boolean commandHook(final String command) {
		final IClient client = Extension.getInstance();

		switch (command) {
			case "debug":
				debug = !debug;
				client.displayMessage("@cya@debug: " + debug);
				return false;
			case "menu":
				showCombatStyleMenu = !showCombatStyleMenu;
				client.displayMessage("@cya@menu: " + showCombatStyleMenu);
				return false;
			default:
				return ScriptListener.getInstance().onCommand(command);
		}
	}
}
