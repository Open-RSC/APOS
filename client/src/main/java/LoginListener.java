import com.aposbot._default.IClient;
import com.aposbot._default.ILoginListener;
import com.aposbot._default.IScriptListener;
import com.aposbot.gui.BotFrame;

/**
 * Handles auto-login.
 */
public final class LoginListener implements ILoginListener {
	private static long loginCount = 0;
	private static final LoginListener instance = new LoginListener();

	private final IClient client;

	private BotFrame botFrame;

	private String username;
	private String password;

	private boolean autoLogin;
	private boolean initialized;

	private long nextAttempt;

	private LoginListener() {
		client = Extension.getInstance();
	}

	/**
	 * Hooks the instance.onLoginScreen() method
	 */
	static void loginScreenHook() {
		instance.onLoginScreen();
	}
	/**
	 * Executes the logic when the user is on the login screen.
	 */
	public void onLoginScreen() {
		if (!initialized) {
			final String font = ClientInit.getBotLoader().getFont();

			if (font != null) {
				System.out.println("Setting font to " + font);
				client.setFont(font);
			}

			StaticAccess.setStrings();
			initialized = true;
		}

		if (!autoLogin || username == null || System.currentTimeMillis() < nextAttempt) return;

		client.login(username, password);
		System.out.printf("Logging into account: %s%n", username);
		if(client.isLoggedIn()) {
			loginCount = 0L;
		}
	}

	static void loginResponseHook(final String arg0, final String arg1) {
		instance.onLoginResponse(arg0, arg1);
	}
	/**
	 * Handles the bots response when a login attempt is made.
	 */
	public void onLoginResponse(final String arg0, final String arg1) {
		long loginDelay;
		long quickLoginDelay;
		if(System.currentTimeMillis() >= nextAttempt) {
			loginCount++;
			System.out.printf("%s %s%n", arg0, arg1);
			System.out.println("Current Login Attempts: " + loginCount);
			if (loginCount <= 10) { //wait 60s-80s or longer for autolog the first 10 times (i.e. for ~ 10 mins)
				quickLoginDelay = (long) (Math.random() * 20000L);
				nextAttempt = System.currentTimeMillis() + 60000L + quickLoginDelay;
				System.out.println("Waiting " + ((quickLoginDelay + 60000L) / 1000L) + " seconds for the next autologin attempt.");
			} else {
				long i = loginCount - 6L;
				loginDelay = (long) (Math.random() * 30000L)
					+ (((i * 15L) / (i + 60L)) * 30000L)
					+ 60000L; //Waits for ~ 120-150s the first few times. Approaches 5+ mins at higher "i" values. Average wait is about 5 minute
				nextAttempt = System.currentTimeMillis() + loginDelay;
				System.out.println("Waiting " + (loginDelay / 1000L) + " seconds for the next autologin attempt.");
			}
		}
	}
	public static LoginListener getInstance() {
		return instance;
	}

	@Override
	public boolean isEnabled() {
		return autoLogin;
	}
	/**
	 * Sets autologin enabled
	 *
	 * @param  enabled true to enable autologin
	 */
	@Override
	public void setEnabled(final boolean enabled) {
		autoLogin = enabled;
		botFrame.updateAutoLoginCheckBox(enabled);
		if (enabled) nextAttempt = 0;
	}
	/**
	 * Sets the account username and password.
	 *
	 * @param  username the username to set
	 * @param  password the password to set
	 */
	@Override
	public void setAccount(final String username, final String password) {
		this.username = username;
		this.password = password;
	}

	@Override
	public void setBotFrame(final BotFrame botFrame) {
		this.botFrame = botFrame;
	}
}
