package com.robotique.aevaweb.teamchatbuddy.utilis;

public interface GoogleSTTCallbacks {

    void onRequestSent();

    void onResponse(String text);

    void onResponseError(String error);

}
