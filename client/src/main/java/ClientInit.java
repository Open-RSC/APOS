import com.aposbot.BotLoader;
import com.aposbot._default.*;

import javax.swing.*;

/**
 * Main entry point of the application.
 * Provides accesors to instances of classes in the default package.
 */
public final class ClientInit implements IClientInit {
	private static BotLoader botLoader;

	private ClientInit() {
	}

	public static void main(final String[] argv) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (final Throwable ignored) {
		} finally {
			botLoader = new BotLoader(argv, new ClientInit());
		}
	}

	static BotLoader getBotLoader() {
		return botLoader;
	}

	@Override
	public IClient getClient() {
		return Extension.getInstance();
	}

	@Override
	public ILoginListener getLoginListener() {
		return LoginListener.getInstance();
	}

	@Override
	public ISleepListener getSleepListener() {
		return SleepListener.getInstance();
	}

	@Override
	public IScriptListener getScriptListener() {
		return ScriptListener.getInstance();
	}

	@Override
	public IPaintListener getPaintListener() {
		return PaintListener.getInstance();
	}

	@Override
	public IStaticAccess getStaticAccess() {
		return StaticAccess.getInstance();
	}

	@Override
	public IJokerFOCR getJoker() {
		return Joker.getInstance();
	}
}
