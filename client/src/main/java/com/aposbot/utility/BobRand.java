package com.aposbot.utility;

import java.security.SecureRandom;
import java.util.Random;

/**
 * http://burtleburtle.net/bob/rand/smallprng.html
 * Public domain.
 */
public final class BobRand extends Random {
	private static final long serialVersionUID = 3048370036096647657L;

	private int a = 0;
	private int b = 0;
	private int c = 0;
	private int d = 0;

	public BobRand() {
		setSeed(new SecureRandom().nextInt());
	}

	public void setSeed(final int seed) {
		a = 0xf1ea5eed;
		b = c = d = seed;
		for (int i = 0; i < 20; ++i) {
			next(32);
		}
	}

	private static int rot(final int x, final int k) {
		return ((x << k) | (x >>> (32 - k)));
	}

	public BobRand(final int seed) {
		setSeed(seed);
	}

	@Override
	public void setSeed(final long seed) {
		setSeed((int) seed);
	}

	@Override
	protected int next(final int bits) {
		final int e = a - rot(b, 27);
		a = b ^ rot(c, 17);
		b = c + d;
		c = d + e;
		d = e + a;
		return d >>> (32 - bits);
	}
}
