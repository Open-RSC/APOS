package com.aposbot._default;

public interface IAutoLogin {

    void onLoginTick();

    void onWelcomeBoxTick();

    void onWildernessWarning();

    boolean isEnabled();

    void setEnabled(boolean b);

    void setAccount(String username, String password);

    void setBanned(boolean b);

}
