package com.aposbot._default;

public interface ILoginListener {
	boolean isEnabled();

	void setEnabled(boolean b);

	void setAccount(String username, String password);
}
