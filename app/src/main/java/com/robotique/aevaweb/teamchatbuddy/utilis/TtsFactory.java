package com.robotique.aevaweb.teamchatbuddy.utilis;

import android.content.Context;


public class TtsFactory {


    public static TtsGoogleC create(Context context, String apiKey) {
        return create(context);
    }

    public static TtsGoogleC create(Context context) {
        return new TtsGoogleC(context);
    }

}