package com.robotique.aevaweb.teamchatbuddy.utilis;

import android.content.Context;

import darren.googlecloudtts.GoogleCloudAPIConfig;
import darren.googlecloudtts.api.SynthesizeApi;
import darren.googlecloudtts.api.SynthesizeApiImpl;
import darren.googlecloudtts.api.VoicesApi;
import darren.googlecloudtts.api.VoicesApiImpl;

public class TtsFactory {


    public static TtsGoogleC create(Context context, String apiKey) {
        GoogleCloudAPIConfig config = new GoogleCloudAPIConfig(apiKey);
        return create(context, config);
    }

    public static TtsGoogleC create(Context context,GoogleCloudAPIConfig config) {
        SynthesizeApi synthesizeApi = new SynthesizeApiImpl(config);
        VoicesApi voicesApi = new VoicesApiImpl(config);
        return new TtsGoogleC(context,synthesizeApi, voicesApi);
    }

}