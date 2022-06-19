package com.aposbot._default;

public interface ISleepListener {
	void onGameTick();

	void setOCRType(OCRType ocrType);

	enum OCRType {
		NUM3(0, "Num3l (Internal)"),
		JOKER(1, "Joker (Internal/Win32/JNI)"),
		EXTERNAL(2, "External (HC.BMP/slword.txt)"),
		MANUAL(3, "Manual");

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
