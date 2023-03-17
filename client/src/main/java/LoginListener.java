import com.aposbot._default.IClient;
import com.aposbot._default.ILoginListener;
import com.aposbot.gui.BotFrame;

/**
 * Handles auto-login.
 */
public final class LoginListener implements ILoginListener {
	int loginCount = 0;
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
		if(client.isLoggedIn()) {
			loginCount = 0;
		}
	}

	static void loginResponseHook(final String arg0, final String arg1) {
		instance.onLoginResponse(arg0, arg1);
	}

	public void onLoginResponse(final String arg0, final String arg1) {
		long loginDelay;
		long quickLoginDelay;
		long fastLoginDelay;
		System.out.printf("%s %s%n", arg0, arg1);
		System.out.println("Current Login Attempts: " + loginCount);
		if (loginCount <= 1) { //wait 5s for autolog the first 2 times it relogs (preserves current behavior)
			fastLoginDelay = (long) (Math.random() * 1000L);
			System.out.println("Waiting " + ((fastLoginDelay + 5000L)/1000L) + " seconds for the next autologin attempt.");
			next_attempt = System.currentTimeMillis() + 5000L + fastLoginDelay;
			loginCount++;
		} else if (loginCount > 1 && loginCount <= 10) { //wait 15-20s for autolog the first 10 times (i.e. for ~ 2-3 mins)
			quickLoginDelay = (long) (Math.random() * 5000L);
			System.out.println("Waiting " + ((quickLoginDelay + 15000L)/1000L) + " seconds for the next autologin attempt.");
			next_attempt = System.currentTimeMillis() + 15000L + quickLoginDelay;
			loginCount++;
		} else if (loginCount > 8) {
			int i = loginCount - 9;
			loginDelay = (long) (Math.random() * 30000L)
				+ (((i * 15L) / (i + 60L)) * 15000L)
				+ 35000L; //calculate our delay. Approaches 4.6 mins at very high "i" values. Average wait value at i = 9 is about 1 minute
			System.out.println("Waiting " + (loginDelay/1000L) + " seconds for the next autologin attempt.");
			next_attempt = System.currentTimeMillis() + loginDelay;
			loginCount++;
		}
	}
	public static LoginListener getInstance() {
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
