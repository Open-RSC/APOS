import com.aposbot._default.IClient;
import com.aposbot._default.ILoginListener;
import com.aposbot.gui.BotFrame;

/**
 * Handles auto-login.
 */
final class LoginListener implements ILoginListener {
	private static final LoginListener instance = new LoginListener();

	private final IClient client;

	private BotFrame botFrame;

	private String username;
	private String password;

	private boolean autoLogin;
	private boolean initialized;

	private long next_attempt;

	private LoginListener() {
		client = Extension.getInstance();
	}

	static void loginScreenHook() {
		instance.onLoginScreen();
	}

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

		if (!autoLogin || username == null || System.currentTimeMillis() < next_attempt) return;

		client.login(username, password);
		System.out.printf("Logging into account: %s%n", username);
	}

	static void loginResponseHook(final String arg0, final String arg1) {
		instance.onLoginResponse(arg0, arg1);
	}

	public void onLoginResponse(final String arg0, final String arg1) {
		System.out.printf("%s %s%n", arg0, arg1);
		next_attempt = System.currentTimeMillis() + 5000L;
	}

	static LoginListener getInstance() {
		return instance;
	}

	@Override
	public boolean isEnabled() {
		return autoLogin;
	}

	@Override
	public void setEnabled(final boolean enabled) {
		autoLogin = enabled;
		botFrame.updateAutoLoginCheckBox(enabled);
		if (enabled) next_attempt = 0;
	}

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
