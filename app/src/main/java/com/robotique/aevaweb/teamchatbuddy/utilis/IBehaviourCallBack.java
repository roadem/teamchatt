package com.robotique.aevaweb.teamchatbuddy.utilis;

public interface IBehaviourCallBack {
    void onEnd(boolean hasAborted, String reason);
    void onRun(String s);
}
