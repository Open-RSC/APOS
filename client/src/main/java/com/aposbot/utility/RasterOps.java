package com.aposbot.utility;

public final class RasterOps {
	private RasterOps() {
	}

	public static void fillRectAlpha(final int[] pixels,
									 final int rw, final int rh,
									 int x, int y, int width, int height,
									 final int trans, final int colour) {
		if (x < 0) {
			width -= -x;
			x = 0;
		}
		if (y < 0) {
			height -= -y;
			y = 0;
		}
		if (x + width > rw) {
			width = rw - x;
		}
		if (y + height > rh) {
			height = rh - y;
		}
		final int a = 256 - trans;
		final int r = (colour >> 16 & 0xff) * trans;
		final int g = (colour >> 8 & 0xff) * trans;
		final int b = (colour & 0xff) * trans;
		final int l1 = rw - width;
		int p = x + y * rw;
		for (int j2 = 0; j2 < height; j2++ /* or skip */) {
			for (int k2 = -width; k2 < 0; k2++) {
				final int r_ = (pixels[p] >> 16 & 0xff) * a;
				final int g_ = (pixels[p] >> 8 & 0xff) * a;
				final int b_ = (pixels[p] & 0xff) * a;
				pixels[p++] = ((r + r_ >> 8) << 16) |
					((g + g_ >> 8) << 8) |
					(b + b_ >> 8);
			}
			p += l1;
		}
	}

	public static void fillRect(final int[] pixels,
								final int rw, final int rh,
								int x, int y,
								int width, int height, final int colour) {
		if (x < 0) {
			width -= -x;
			x = 0;
		}
		if (y < 0) {
			height -= -y;
			y = 0;
		}
		if (x + width > rw) {
			width = rw - x;
		}
		if (y + height > rh) {
			height = rh - y;
		}
		final int i = rw - width;
		int j = x + y * rw;
		for (int k = -height; k < 0; k++ /* or skip */) {
			for (int l = -width; l < 0; l++) {
				pixels[j++] = colour;
			}
			j += i;
		}
	}

	public static void drawHLine(final int[] pixels,
								 final int rw, final int rh,
								 int x, final int y, int length, final int colour) {
		if (y < 0 || y >= rh) {
			return;
		}
		if (x < 0) {
			length -= -x;
			x = 0;
		}
		if (x + length > rw) {
			length = rw - x;
		}
		final int i = x + y * rw;
		for (int j = 0; j < length; j++) {
			pixels[i + j] = colour;
		}
	}

	public static void drawVLine(final int[] pixels,
								 final int rw, final int rh,
								 final int x, int y,
								 int length, final int colour) {
		if (x < 0 || x >= rw) {
			return;
		}
		if (y < 0) {
			length -= -y;
			y = 0;
		}
		if (y + length > rw) {
			length = rh - y;
		}
		final int i = x + y * rw;
		for (int j = 0; j < length; j++) {
			pixels[i + j * rw] = colour;
		}
	}

	public static void fillCircle(final int[] pixels,
								  final int rw, final int rh,
								  final int x, final int y,
								  final int radius, final int colour, final int trans) {
		final int a = 256 - trans;
		final int r = (colour >> 16 & 0xff) * trans;
		final int g = (colour >> 8 & 0xff) * trans;
		final int b = (colour & 0xff) * trans;
		int start_y = y - radius;
		if (start_y < 0) {
			start_y = 0;
		}
		int end_y = y + radius;
		if (end_y >= rh) {
			end_y = rh - 1;
		}
		for (int _y = start_y; _y <= end_y; _y++ /* or skip */) {
			final int k2 = _y - y;
			final int l2 = (int) Math.sqrt(radius * radius - k2 * k2);
			int i3 = x - l2;
			if (i3 < 0) {
				i3 = 0;
			}
			int j3 = x + l2;
			if (j3 >= rw) {
				j3 = rw - 1;
			}
			int p = i3 + _y * rw;
			for (int l3 = i3; l3 <= j3; l3++) {
				final int r_ = (pixels[p] >> 16 & 0xff) * a;
				final int g_ = (pixels[p] >> 8 & 0xff) * a;
				final int b_ = (pixels[p] & 0xff) * a;
				pixels[p++] = ((r + r_ >> 8) << 16) |
					((g + g_ >> 8) << 8) |
					(b + b_ >> 8);
			}
		}
	}

	public static void setPixel(final int[] pixels,
								final int rw, final int rh,
								final int x, final int y, final int colour) {
		if (x >= 0 && y >= 0 && x < rw && y < rh) {
			pixels[x + y * rw] = colour;
		}
	}
}
