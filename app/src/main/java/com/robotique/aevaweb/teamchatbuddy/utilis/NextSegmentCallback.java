package com.robotique.aevaweb.teamchatbuddy.utilis;

public interface NextSegmentCallback {

    void onSegmentDone();

    void onError(Exception e);
}

