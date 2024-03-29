import com.aposbot.BotLoader;
import com.aposbot._default.IClient;
import com.aposbot._default.ISleepListener;
import com.aposbot.Constants;
import com.stormy.ocrlib.DictSearch;
import com.stormy.ocrlib.OCR;
import com.stormy.ocrlib.OCRException;
import com.stormy.ocrlib.SimpleImageIO;

import java.io.*;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Listens for sleepwords and solves the sleep CAPTCHA.
 */
final class SleepListener implements ISleepListener {
	private static final SleepListener instance = new SleepListener();

	private static final String HC_BMP = Paths.get("HC.BMP").toString();
	private static final String SLWORD_TXT = Paths.get("slword.txt").toString();
	private static final String DICT_TXT = Constants.PATH_SLEEP.resolve("dictionary.txt").toString();
	private static final String MODEL_TXT = Constants.PATH_SLEEP.resolve("model.txt").toString();
	private static final String HASHES = Constants.PATH_SLEEP.resolve("hashes.properties").toString();

	private static final int FNV1_32_INIT = 0x811c9dc5;
	private static final int FNV1_PRIME_32 = 16777619;

	private static final int MAXIMUM_INCORRECT_SLEEP_TRIES = 3;

	private final IClient client;

	private Properties hashes;

	private OCR ocr;
	private OCRType ocrType;
	private String sleepWord;

	private File hc;
	private File slword;

	private long lastModified;
	private boolean checkLastModified;


	private int incorrectSleepTries;

	private SleepListener() {
		client = Extension.getInstance();
	}

	static SleepListener getInstance() {
		return instance;
	}

	static void sleepStartHook() {
		instance.onSleepStart();
	}

	private void onSleepStart() {
	}

	/**
	 * Hook to call an instance of the onSleepWord(data) method
	 *
	 * @param  data  the byte array data
	 */
	static void sleepWordHook(final byte[] data) {
		instance.onSleepWord(data);
	}

	/**
	 * Processes the given byte array data to handle sleep words.
	 *
	 * @param  data  the byte array data to process
	 */
	private void onSleepWord(final byte[] data) {
		if (client.isFatigueTraining() &&
				client.getFatigue() == 750 &&
				client.getSleepFatigue() == 0) {
			client.setSleeping(false);
			return;
		}

		switch (ocrType) {
			case NUM3:
				try (final ByteArrayOutputStream out = new ByteArrayOutputStream(4096)) {
					saveBitmap(out, convertImage(data));
					sleepWord = ocr.guess(SimpleImageIO.readBMP(out.toByteArray()), true);
					if (client.getSleepFatigue() == 0) onSleepFatigueUpdate(0);
				} catch (final IOException ex) {
					ex.printStackTrace();
					sleepWord = null;
				}
				break;
			case HASH:
				byte[] image = convertImage(data);
				int hash = hash32(image);
				sleepWord = hashes.getProperty(Integer.toString(hash));
				if (sleepWord == null)
					sleepWord = "unknown";

				if (client.getSleepFatigue() == 0)
					onSleepFatigueUpdate(0);
				break;
			case JOKER:
				try (final FileOutputStream out = new FileOutputStream(hc)) {
					saveBitmap(out, convertImage(data));
					sleepWord = Joker.getInstance().getGuess();
					if (client.getSleepFatigue() == 0) onSleepFatigueUpdate(0);
				} catch (final IOException ex) {
					ex.printStackTrace();
					sleepWord = null;
				}
				break;
			case EXTERNAL:
				try (final FileOutputStream out = new FileOutputStream(hc)) {
					saveBitmap(out, convertImage(data));
					checkLastModified = true;
				} catch (final IOException ex) {
					ex.printStackTrace();
					sleepWord = null;
				}
				break;
			case MANUAL:
			default:
		}
	}
	/**
	 * Saves the given bitmap data to the specified output stream.
	 *
	 * @param  out   the output stream to write the bitmap data to
	 * @param  data  the byte array containing the bitmap data
	 * @throws IOException  if an I/O error occurs while writing the data to the output stream
	 */
	private static void saveBitmap(final OutputStream out, final byte[] data) throws IOException {
		out.write(66);
		out.write(77);
		short var3 = 1342;
		out.write(var3 & 255);
		out.write(var3 >> 8 & 255);
		out.write(0);
		out.write(0);
		out.write(0);
		out.write(0);
		out.write(0);
		out.write(0);
		byte var10 = 62;
		out.write(var10 & 255);
		out.write(0);
		out.write(0);
		out.write(0);
		var10 = 40;
		out.write(var10 & 255);
		out.write(0);
		out.write(0);
		out.write(0);
		var3 = 256;
		out.write(0);
		out.write(var3 >> 8 & 255);
		out.write(0);
		out.write(0);
		out.write(var10 & 255);
		out.write(0);
		out.write(0);
		out.write(0);
		var10 = 1;
		out.write(var10 & 255);
		out.write(0);
		out.write(var10 & 255);
		out.write(0);
		out.write(0);
		out.write(0);
		out.write(0);
		out.write(0);
		out.write(0);
		out.write(0);
		out.write(0);
		out.write(0);
		out.write(0);
		out.write(0);
		out.write(0);
		out.write(0);
		out.write(0);
		out.write(0);
		out.write(0);
		out.write(0);
		out.write(0);
		out.write(0);
		out.write(0);
		out.write(0);
		out.write(0);
		out.write(0);
		out.write(0);
		out.write(0);
		out.write(0);
		out.write(0);
		out.write(0);
		out.write(0);
		out.write(255);
		out.write(255);
		out.write(255);
		out.write(0);
		int var4 = 9945;
		for (int var5 = 0; var5 < 40; ++var5) {
			for (int var6 = 0; var6 < 32; ++var6) {
				byte var7 = 0;
				for (int var8 = 0; var8 < 8; ++var8) {
					var7 = (byte) (2 * var7);
					if (var6 != 31 || var8 != 7) {
						if (data[var4] != 0) {
							++var7;
						}
						++var4;
					}
				}
				out.write(var7);
			}
			var4 -= 510;
		}
	}
	/**
	 * Convert an image represented as a byte array.
	 *
	 * @param  data  the byte array representing the image data
	 * @return       the converted image as a byte array
	 */
	private static byte[] convertImage(final byte[] data) {
		int var1 = 1;
		byte var2 = 0;
		final byte[] var4 = new byte[10200];
		int var3;
		int var5;
		int var6;
		for (var3 = 0; var3 < 255; var2 = (byte) (255 - var2)) {
			var5 = data[var1++] & 255;
			for (var6 = 0; var6 < var5; ++var6) {
				var4[var3++] = var2;
			}
		}
		for (var5 = 1; var5 < 40; ++var5) {
			var6 = 0;
			while (var6 < 255) {
				final int var7 = data[var1++] & 255;
				for (int var8 = 0; var8 < var7; ++var8) {
					var4[var3] = var4[var3 - 255];
					++var3;
					++var6;
				}
				if (var6 < 255) {
					var4[var3] = (byte) (255 - var4[var3 - 255]);
					++var3;
					++var6;
				}
			}
		}
		return var4;
	}

	/**
	 * FNV1a 32 bit variant.
	 *
	 * @param data - input byte array
	 * @return - hashcode
	 */
	private static int hash32(byte[] data) {
		return hash32(data, data.length);
	}

	/**
	 * FNV1a 32 bit variant.
	 *
	 * @param data   - input byte array
	 * @param length - length of array
	 * @return - hashcode
	 */
	private static int hash32(byte[] data, int length) {
		int hash = FNV1_32_INIT;
		for (int i = 0; i < length; i++) {
			hash ^= (data[i] & 0xff);
			hash *= FNV1_PRIME_32;
		}

		return hash;
	}

	/**
	 * Updates the sleep fatigue and takes appropriate actions based on the fatigue level.
	 *
	 * @param  fatigue	the fatigue level to update
	 */
	private void onSleepFatigueUpdate(final int fatigue) {
		if (sleepWord == null) {
			if (fatigue == 0 && ocrType != OCRType.MANUAL && ocrType != OCRType.EXTERNAL) client.setSleeping(false);
			return;
		}

		if (client.isFatigueTraining() && client.getFatigue() == 750) {
			if (fatigue == 750) return;

			if (fatigue == 708) {
				sendSleepWord();
				return;
			}

			client.setSleeping(false);
			sleepWord = null;
			return;
		}

		if (fatigue == 0) sendSleepWord();
	}

	/**
	 * Sends the sleep word using the client's CAPTCHA service.
	 */
	private void sendSleepWord() {
		client.sendCAPTCHA(sleepWord);

		if (BotLoader.isCaptchaLogging())
			System.out.println("Sent CAPTCHA: " + sleepWord);

		sleepWord = null;
	}

	/**
	 * Hook that is called when sleep word is entered incorrectly
	 */
	static void sleepWordIncorrectHook() {
		instance.onSleepWordIncorrect();
	}

	/**
	 * Called when sleep word is entered incorrectly, if greater than maximum tries, it will log out.
	 */
	private void onSleepWordIncorrect() {
		if (!client.isFatigueTraining() || client.getFatigue() < 750) {
			return;
		}
		if (++incorrectSleepTries > MAXIMUM_INCORRECT_SLEEP_TRIES) {
			onSleepStop();
			client.logout();
		}
	}

	static void sleepFatigueUpdateHook(final int fatigue) {
		instance.onSleepFatigueUpdate(fatigue);
	}

	static void sleepStopHook() {
		instance.onSleepStop();
	}

	private void onSleepStop() {
		incorrectSleepTries = 0;
	}
/**
 * Overrides the onGameTick method.
 * This method is called every game tick. It checks if the lastModified flag is true and if so, it retrieves the
 * last modified time of the slword file.
 * If the last modified time is the same as the lastModified field of the class, it returns without
 */
	@Override
	public void onGameTick() {
		if (!checkLastModified) return;
		final long lastModified = slword.lastModified();
		if (this.lastModified == lastModified) return;
		this.lastModified = lastModified;
		checkLastModified = false;
		sleepWord = readLine(slword);
		onSleepFatigueUpdate(client.getSleepFatigue());
	}
	/**
	 * Sets the OCR type for the client
	 *
	 * @param  ocrType	the OCRType to be set
	 */
	@Override
	public void setOCRType(final OCRType ocrType) {
		this.ocrType = ocrType;

		switch (ocrType) {
			case NUM3:
				try (final BufferedReader mr = new BufferedReader(new FileReader(MODEL_TXT));
					 final BufferedReader dr = new BufferedReader(new FileReader(DICT_TXT))) {
					ocr = new OCR(new DictSearch(dr), mr);
				} catch (final IOException | OCRException e) {
					e.printStackTrace();
					this.ocrType = OCRType.MANUAL;
				}
				break;
			case HASH:
				try (FileInputStream fs = new FileInputStream(HASHES)) {
					hashes = new Properties();
					hashes.load(fs);
				} catch (final IOException e) {
					e.printStackTrace();
					this.ocrType = OCRType.MANUAL;
				}
				break;
			case JOKER:
				hc = new File(HC_BMP);
				final Joker joker = Joker.getInstance();
				if (joker.loadNativeLibrary()) {
					joker.setFilePaths(MODEL_TXT, DICT_TXT);
				} else {
					this.ocrType = OCRType.MANUAL;
				}
				break;
			case EXTERNAL:
				hc = new File(HC_BMP);
				slword = new File(SLWORD_TXT);
				lastModified = slword.lastModified();
				break;
			case MANUAL:
			default:
				break;
		}
	}
	/**
	 * Reads a line from the specified file.
	 *
	 * @param  file  the file to read from
	 * @return       the line read from the file, or null if an exception occurred
	 */
	private static String readLine(final File file) {
		try (final BufferedReader reader = new BufferedReader(new FileReader(file))) {
			return reader.readLine().trim();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
