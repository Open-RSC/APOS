import com.aposbot._default.IClient;
import com.aposbot._default.IPaintListener;
import com.aposbot.utility.RasterOps;

import java.awt.*;
import java.text.DecimalFormat;

/**
 * Paints the main HUD.
 */
final class PaintListener implements IPaintListener {
	private static final PaintListener instance = new PaintListener();

	private static final int BAR_WIDTH = 100;
	private static final int BAR_HEIGHT = 20;
	private static final int HP_FRONT = 0x3CDC3C;
	private static final int HP_BACK = 0x3CDC3C;
	private static final int PRAY_FRONT = 0x3CBEDC;
	private static final int PRAY_BACK = 0x3CBEDC;
	private static final int BOTTOM_GRAY = 0x0A0A03;
	private static final int BOTTOM_DARK = 0x002831;
	private static final int BOTTOM_MID = 0x00394A;
	private static final int BOTTOM_LIGHT = 0x005D7B;

	public static volatile boolean renderSolid = true;
	public static volatile boolean renderTextures = true;
	public static volatile boolean interlaceMode = false;
	public static volatile boolean renderGraphics = true;

	private final DecimalFormat decimalFormat = new DecimalFormat("#,##0.0#");
	private final Dimension dimension = new Dimension();

	private final IClient client;

	private boolean enabled = true;
	private boolean resized = true;

	private PaintListener() {
		client = Extension.getInstance();
	}

	static void paintHook() {
		instance.onPaint();
	}

	@Override
	public void onPaint() {
		drawGradient();

		synchronized (dimension) {
			if (!resized) {
				client.resizeGame(dimension.width, dimension.height);
				resized = true;
			}
		}

		if (!enabled) return;

		final int hits = client.getCurrentLevel(3);
		final int base_hits = client.getBaseLevel(3);

		final int pray = client.getCurrentLevel(5);
		final int base_pray = client.getBaseLevel(5);

		final int x = 7;
		int y = 135;

		drawStatBar(x, y, hits, base_hits, HP_FRONT, HP_BACK);
		client.drawString(String.format("Hits: %d/%d",
			hits, base_hits), x + 2, y, 2, 0xFFFFFF);

		y += (BAR_HEIGHT + 7);

		drawStatBar(x, y, pray, base_pray, PRAY_FRONT, PRAY_BACK);
		client.drawString(String.format("Pray: %d/%d",
			pray, base_pray), x + 2, y, 2, 0xFFFFFF);

		y += (BAR_HEIGHT + 5);

		client.drawString(String.format("Tile: (%d,%d)",
				client.getLocalX() + client.getAreaX(),
				client.getLocalY() + client.getAreaY()),
			x, y, 2, 0xFFFFFF);

		y += 17;

		client.drawString(String.format("Fatigue: %.2f%%",
				client.getAccurateFatigue()),
			x, y, 2, 0xFFFFFF);

		y += 17;

		client.drawString("Pid: " + client.getMobServerIndex(client.getPlayer()),
			x, y, 2, 0xFFFFFF);

		ScriptListener.getInstance().onPaintTick();
	}

	private void drawGradient() {
		final int rw = client.getGameWidth();
		final int rh = client.getGameHeight();
		final int[] pixels = client.getPixels();
		final int x = 512;
		int y = client.getGameHeight() - 13;

		RasterOps.fillRect(pixels, rw, rh, x, y, rw, 1, 0x0);
		y += 1;
		RasterOps.fillRect(pixels, rw, rh, x, y, rw, 11, BOTTOM_GRAY);
		y += 2;
		RasterOps.fillRect(pixels, rw, rh, x, y, rw, 7, BOTTOM_DARK);
		y += 1;
		RasterOps.fillRect(pixels, rw, rh, x, y, rw, 5, BOTTOM_MID);
		y += 1;
		RasterOps.fillRect(pixels, rw, rh, x, y, rw, 3, BOTTOM_LIGHT);
	}

	private void drawStatBar(final int x, int y,
							 final double current, final double base, final int front, final int back) {
		final int rw = client.getGameWidth();
		final int rh = client.getGameHeight();
		final int[] pixels = client.getPixels();
		final int width = (int) (current / base * BAR_WIDTH);

		y -= (BAR_HEIGHT / 2) + 4;
		RasterOps.fillRectAlpha(pixels, rw, rh,
			x, y, width, BAR_HEIGHT,
			160, front);
		RasterOps.fillRectAlpha(pixels, rw, rh,
			x + width, y, BAR_WIDTH - width, BAR_HEIGHT,
			65, back);
	}

	@Override
	public boolean isPaintingEnabled() {
		return enabled;
	}

	@Override
	public void setPaintingEnabled(final boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public void doResize(final int width, final int height) {
		synchronized (dimension) {
			resized = false;
			dimension.width = width;
			dimension.height = height;
		}
	}

	@Override
	public void setRenderTextures(final boolean renderTextures) {
		PaintListener.renderTextures = renderTextures;
	}

	@Override
	public void setRenderSolid(final boolean renderSolid) {
		PaintListener.renderSolid = renderSolid;
	}

	@Override
	public void setInterlaceMode(final boolean interlaceMode) {
		PaintListener.interlaceMode = interlaceMode;
	}

	static IPaintListener getInstance() {
		return instance;
	}
}
