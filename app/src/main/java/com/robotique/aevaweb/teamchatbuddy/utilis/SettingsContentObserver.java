package com.robotique.aevaweb.teamchatbuddy.utilis;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;

import com.robotique.aevaweb.teamchatbuddy.application.TeamChatBuddyApplication;


/*
 * cette classe permet d"écouter les changements du volume
 */
public class SettingsContentObserver extends ContentObserver {
    public Context context;
    public TeamChatBuddyApplication app;
    public SettingsContentObserver(Handler handler ,Context cntx) {
        super(handler);
        this.context = cntx;
    }

    @Override
    public boolean deliverSelfNotifications() {
        return super.deliverSelfNotifications();
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        app = (TeamChatBuddyApplication) context;
        /*
         * Notifier le changement de volume
         */
        app.notifyObservers("changeDetected");
    }

}