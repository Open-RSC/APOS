import java.lang.reflect.Field;
import java.math.BigInteger;

import javax.swing.UIManager;

import com.aposbot.BotFrame;
import com.aposbot.BotLoader;
import com.aposbot._default.IAutoLogin;
import com.aposbot._default.IClient;
import com.aposbot._default.IClientInit;
import com.aposbot._default.IPaintListener;
import com.aposbot._default.IScriptListener;
import com.aposbot._default.ISleepListener;

public final class ClientInit
        implements IClientInit {

    private static BotLoader loader;

    public static void main(String[] argv) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (final Throwable ignored) {
        } finally {
            loader = new BotLoader(argv, new ClientInit());
        }
    }

    private ClientInit() {
    }

    static BotLoader getBotLoader() {
        return loader;
    }

    @Override
    public IClient createClient(BotFrame frame) {
        client.il[237] = "Welcome to @cya@APOS";
        final Extension ex = new Extension(frame);
        ex.setParentInit(this);
        ScriptListener.init(ex);
        AutoLogin.init(ex);
        PaintListener.init(ex);
        return ex;
    }

    @Override
    public IAutoLogin getAutoLogin() {
        return AutoLogin.get();
    }

    @Override
    public ISleepListener getSleepListener() {
        return SleepListener.get();
    }

    @Override
    public IScriptListener getScriptListener() {
        return ScriptListener.get();
    }

    @Override
    public IPaintListener getPaintListener() {
        return PaintListener.get();
    }

    @Override
    public void setRSAKey(String key) {
        ja.K = new BigInteger(key);
    }

    @Override
    public void setRSAExponent(String exponent) {
        s.c = new BigInteger(exponent);
    }
}
