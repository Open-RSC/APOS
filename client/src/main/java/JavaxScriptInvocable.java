import javax.script.Invocable;
import javax.script.ScriptException;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.math.BigInteger;

final class JavaxScriptInvocable extends Script {
	private static final String ERROR_MESSAGE = "Error processing script. Send this output to the script's author:";

	private final Invocable invocable;
	private final String name;

	JavaxScriptInvocable(final Extension ex, final Invocable invocable, final String name) {
		super(ex);
		this.invocable = invocable;
		this.name = name;
	}

	@Override
	public void init(final String params) {
		try {
			invocable.invokeFunction("init", params);
		} catch (final NoSuchMethodException ignored) {
		} catch (final ScriptException ex) {
			System.err.println(ERROR_MESSAGE);
			ex.printStackTrace();
		}
	}

	@Override
	public int main() {
		try {
			return looseToInt(invocable.invokeFunction("main", this));
		} catch (final NoSuchMethodException ignored) {
		} catch (final ScriptException ex) {
			System.err.println(ERROR_MESSAGE);
			ex.printStackTrace();
		}
		return 1000;
	}

	private static int looseToInt(final Object object) {
		// needlessly excessive?
		final Class<?> c = object.getClass();
		if (Integer.class.equals(c)) {
			return (Integer) object;
		} else if (Long.class.equals(c)) {
			return ((Long) object).intValue();
		} else if (Short.class.equals(c)) {
			return ((Short) object).intValue();
		} else if (Byte.class.equals(c)) {
			return ((Byte) object).intValue();
		} else if (Float.class.equals(c)) {
			return ((Float) object).intValue();
		} else if (Double.class.equals(c)) {
			return ((Double) object).intValue();
		} else if (String.class.equals(c)) {
			return Integer.parseInt((String) object);
		} else if (BigInteger.class.equals(c)) {
			return ((BigInteger) object).intValue();
		} else if (BigDecimal.class.equals(c)) {
			return ((BigDecimal) object).intValue();
		} else {
			return 0;
		}
	}

	@Override
	public void paint() {
		try {
			invocable.invokeFunction("paint", this);
		} catch (final NoSuchMethodException ignored) {
		} catch (final ScriptException ex) {
			System.err.println(ERROR_MESSAGE);
			ex.printStackTrace();
		}
	}

	@Override
	public void onServerMessage(final String str) {
		try {
			invocable.invokeFunction("onServerMessage", str);
		} catch (final NoSuchMethodException ignored) {
		} catch (final ScriptException ex) {
			System.err.println(ERROR_MESSAGE);
			ex.printStackTrace();
		}
	}

	@Override
	public void onTradeRequest(final String name) {
		try {
			invocable.invokeFunction("onTradeRequest", name);
		} catch (final NoSuchMethodException ignored) {
		} catch (final ScriptException ex) {
			System.err.println(ERROR_MESSAGE);
			ex.printStackTrace();
		}
	}

	@Override
	public void onChatMessage(final String msg, final String name, final boolean mod,
							  final boolean admin) {

		try {
			invocable.invokeFunction("onChatMessage", msg, name, mod, admin);
		} catch (final NoSuchMethodException ignored) {
		} catch (final ScriptException ex) {
			System.err.println(ERROR_MESSAGE);
			ex.printStackTrace();
		}
	}

	@Override
	public void onPrivateMessage(final String msg, final String name, final boolean mod,
								 final boolean admin) {

		try {
			invocable.invokeFunction("onPrivateMessage", msg, name, mod, admin);
		} catch (final NoSuchMethodException ignored) {
		} catch (final ScriptException ex) {
			System.err.println(ERROR_MESSAGE);
			ex.printStackTrace();
		}
	}

	@Override
	public void onKeyPress(final KeyEvent e) {
		try {
			invocable.invokeFunction("onKeyPress", e.getKeyCode());
		} catch (final NoSuchMethodException ignored) {
		} catch (final ScriptException ex) {
			System.err.println(ERROR_MESSAGE);
			ex.printStackTrace();
		}
	}

	@Override
	public String toString() {
		return name;
	}
}
