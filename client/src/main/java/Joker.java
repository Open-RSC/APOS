import com.aposbot.Constants;
import com.aposbot._default.IJokerFOCR;

final class Joker implements IJokerFOCR {
	private static final Joker instance = new Joker();

	private boolean loaded;

	private Joker() {
	}

	static Joker getInstance() {
		return instance;
	}

	@Override
	public void close() {
		closeOCR();
	}

	private static native void closeOCR();

	@Override
	public void setFilePaths(final String file_model, final String file_dict) {
		initOCR(file_model, file_dict);
	}

	private static native void initOCR(String file_model, String file_dict);

	@Override
	public String getGuess() {
		return getSleepWord();
	}

	private static native String getSleepWord();

	@Override
	public boolean loadNativeLibrary() {
		try {
			System.load(Constants.PATH_LIB.resolve("Joker.dll").toAbsolutePath().toString());
		} catch (final Throwable t) {
			System.out.println("Error loading Joker/FOCR:");
			t.printStackTrace();
			return false;
		}
		loaded = true;
		return true;
	}

	@Override
	public boolean isLibraryLoaded() {
		return loaded;
	}
}
