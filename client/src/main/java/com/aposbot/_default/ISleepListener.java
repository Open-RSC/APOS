package com.aposbot._default;

public interface ISleepListener {
	void onGameTick();

	void setOCRType(OCRType ocrType);

	enum OCRType {
		HASH(0, "Image Hashes (OpenRSC)"),
		NUM3(1, "Num3l (cross-platform)"),
		JOKER(2, "Joker (Windows only)"),
		EXTERNAL(3, "External (HC.BMP + slword.txt)"),
		MANUAL(4, "Manual");

		public static final OCRType[] VALUES = OCRType.values();

		private final int index;
		private final String name;

		OCRType(final int index, final String name) {
			this.index = index;
			this.name = name;
		}

		public static OCRType fromName(final String name) {
			for (final OCRType ocrType : VALUES) {
				if (ocrType.name.equals(name)) {
					return ocrType;
				}
			}

			return null;
		}

		public int getIndex() {
			return index;
		}

		public String getName() {
			return name;
		}

		@Override
		public String toString() {
			return name;
		}
	}
}
