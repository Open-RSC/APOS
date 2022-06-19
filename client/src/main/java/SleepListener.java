import com.aposbot._default.IClient;
import com.aposbot._default.ISleepListener;
import com.aposbot.Constants;
import com.stormy.ocrlib.DictSearch;
import com.stormy.ocrlib.OCR;
import com.stormy.ocrlib.OCRException;
import com.stormy.ocrlib.SimpleImageIO;

import java.io.*;
import java.nio.file.Paths;

/**
 * Listens for sleepwords and solves the sleep CAPTCHA.
 */
final class SleepListener implements ISleepListener {
	private static final SleepListener instance = new SleepListener();

	private static final String HC_BMP = Paths.get("HC.BMP").toString();
	private static final String SLWORD_TXT = Paths.get("slword.txt").toString();
	private static final String DICT_TXT = Constants.PATH_SLEEP.resolve("dictionary.txt").toString();
	private static final String MODEL_TXT = Constants.PATH_SLEEP.resolve("model.txt").toString();

	private static final int MAXIMUM_INCORRECT_SLEEP_TRIES = 3;

	private final IClient client;

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

	static void sleepWordHook(final byte[] data) {
		instance.onSleepWord(data);
	}

	private void onSleepWord(final byte[] data) {
		if (client.isFatigueTraining() && client.getSleepFatigue() == 0) {
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

	private void onSleepFatigueUpdate(final int fatigue) {
		if (sleepWord == null) {
			if (fatigue == 0 && ocrType != OCRType.MANUAL && ocrType != OCRType.EXTERNAL) client.setSleeping(false);
			return;
		}

		if (client.isFatigueTraining()) {
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

	private void sendSleepWord() {
		client.sendCAPTCHA(sleepWord);
		System.out.println("Sent CAPTCHA: " + sleepWord);
		sleepWord = null;
	}

	static void sleepWordIncorrectHook() {
		instance.onSleepWordIncorrect();
	}

	private void onSleepWordIncorrect() {
		if (client.isFatigueTraining() && ++incorrectSleepTries > MAXIMUM_INCORRECT_SLEEP_TRIES) {
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

	private static String readLine(final File file) {
		try (final BufferedReader reader = new BufferedReader(new FileReader(file))) {
			return reader.readLine().trim();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
