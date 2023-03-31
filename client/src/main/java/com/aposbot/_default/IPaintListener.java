package com.aposbot._default;

public interface IPaintListener {
	void onPaint();

	boolean isPaintingEnabled();

	void setPaintingEnabled(boolean b);

	void doResize(int w, int h);

	void setRenderTextures(boolean b);

	void setRenderSolid(boolean b);

	void setInterlaceMode(boolean b);

}
