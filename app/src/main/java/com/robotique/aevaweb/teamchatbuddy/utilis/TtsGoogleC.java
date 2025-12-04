package com.robotique.aevaweb.teamchatbuddy.utilis;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Base64;
import android.util.Log;

import com.arthenica.mobileffmpeg.FFmpeg;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.robotique.aevaweb.teamchatbuddy.application.TeamChatBuddyApplication;
import com.robotique.aevaweb.teamchatbuddy.models.HttpResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import darren.googlecloudtts.api.SynthesizeApi;
import darren.googlecloudtts.api.VoicesApi;
import darren.googlecloudtts.exception.ApiException;
import darren.googlecloudtts.model.VoicesList;
import darren.googlecloudtts.parameter.AudioConfig;
import darren.googlecloudtts.parameter.AudioEncoding;
import darren.googlecloudtts.parameter.SynthesisInput;
import darren.googlecloudtts.parameter.VoiceSelectionParams;
import darren.googlecloudtts.request.SynthesizeRequest;
import darren.googlecloudtts.response.SynthesizeResponse;
import darren.googlecloudtts.response.VoicesResponse;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TtsGoogleC implements AutoCloseable{

    private SynthesizeApi mSynthesizeApi;
    private VoicesApi mVoicesApi;
    private final Context context;


    private VoiceSelectionParams mVoiceSelectionParams;
    private AudioConfig mAudioConfig;

    private MediaPlayer mMediaPlayer;
    private TeamChatBuddyApplication teamChatBuddyApplication;

    private int mVoiceLength = -1;
    private TtsGoogleApiListener mTtsListener;

    public TtsGoogleC(Context context, SynthesizeApi synthesizeApi, VoicesApi voicesApi) {
        this.context = context;
        this.mSynthesizeApi = synthesizeApi;
        this.mVoicesApi = voicesApi;
    }

    public  TtsGoogleC setVoiceSelectionParams(VoiceSelectionParams voiceSelectionParams) {
        mVoiceSelectionParams = voiceSelectionParams;
        return this;
    }

    public TtsGoogleC setAudioConfig(AudioConfig audioConfig) {
        mAudioConfig = audioConfig;
        return this;
    }
    public void setTtsListener(TtsGoogleApiListener ttsListener) {
        mTtsListener = ttsListener;
    }


    public VoicesList load() {
        VoicesResponse response = mVoicesApi.get();
        VoicesList voicesList = new VoicesList();

        for (VoicesResponse.Voices voices : response.getVoices()) {
            String languageCode = voices.getLanguageCodes().get(0);
            VoiceSelectionParams params = new VoiceSelectionParams(
                    languageCode,
                    voices.getName(),
                    voices.getSsmlGender()
            );
            voicesList.add(languageCode, params);
        }

        return voicesList;
    }

    public void start(String key, String text) {
        if (mVoiceSelectionParams == null)
            throw new NullPointerException("You forget to setVoiceSelectionParams()");
        if (mAudioConfig == null)
            throw new NullPointerException("You forget to setAudioConfig()");

        stop();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                Log.i("MYA_API_Google", "=== Initialisation du processus TTS Google ===");

                Gson gson = new Gson();

                // ---- Construction du corps JSON ----
                JsonObject input = new JsonObject();
                if (text.contains("<speak>"))
                    input.addProperty("ssml", text);
                else
                    input.addProperty("text", text);

                JsonObject voice = new JsonObject();
                voice.addProperty("languageCode", mVoiceSelectionParams.getLanguageCode());
                voice.addProperty("name", mVoiceSelectionParams.getName());
                if (mVoiceSelectionParams.getSsmlGender() != null)
                    voice.addProperty("ssmlGender", mVoiceSelectionParams.getSsmlGender().name());
                else
                    voice.addProperty("ssmlGender", "NEUTRAL");

                // ---- LOGS VOICE ----
                Log.i("MYA_API_Google", "=== Paramètres voix sélectionnés ===");
                Log.i("MYA_API_Google", "Langue        : " + mVoiceSelectionParams.getLanguageCode());
                Log.i("MYA_API_Google", "Voix          : " + mVoiceSelectionParams.getName());
                Log.i("MYA_API_Google", "Genre SSML    : " +
                        (mVoiceSelectionParams.getSsmlGender() != null
                                ? mVoiceSelectionParams.getSsmlGender().name()
                                : "NEUTRAL"));

                JsonObject audioConfig = new JsonObject();
                audioConfig.addProperty("audioEncoding", mAudioConfig.getAudioEncoding().name());
                audioConfig.addProperty("speakingRate", mAudioConfig.getSpeakingRate());
                audioConfig.addProperty("pitch", mAudioConfig.getPitch());
                // ---- LOGS AUDIO CONFIG ----
                Log.i("MYA_API_Google", "=== Configuration audio appliquée ===");
                Log.i("MYA_API_Google", "Encodage      : " + mAudioConfig.getAudioEncoding().name());
                Log.i("MYA_API_Google", "Vitesse       : " +mAudioConfig.getSpeakingRate());
                Log.i("MYA_API_Google", "Pitch       : " +mAudioConfig.getPitch());
                JsonObject body = new JsonObject();
                body.add("input", input);
                body.add("voice", voice);
                body.add("audioConfig", audioConfig);

                // ---- Préparation URL + headers ----
                String TTS_URL = "https://texttospeech.googleapis.com/v1/text:synthesize?key=" + key;
                Log.i("MYA_API_Google", "URL API TTS : " + TTS_URL);

                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json; charset=UTF-8");

                // ---- Envoi HTTP ----
                Log.i("MYA_API_Google", "Envoi requête TTS via HttpClientUtils...");
                HttpResponse httpResponse = HttpClientUtils.sendPost(TTS_URL, gson.toJson(body), headers, 50000);

                Log.i("MYA_API_Google", "Réponse HTTP reçue : " + httpResponse.responseCode);

                if (httpResponse.responseCode >= 200 && httpResponse.responseCode < 300 && httpResponse.body != null) {

                    JsonObject respJson = gson.fromJson(httpResponse.body, JsonObject.class);
                    if (!respJson.has("audioContent")) {
                        Log.e("MYA_API_Google", "Aucun champ 'audioContent' trouvé");
                        if (mTtsListener != null) mTtsListener.onError();
                        return;
                    }

                    // ---- Décodage audio ----
                    String base64EncodedString = respJson.get("audioContent").getAsString();
                    byte[] audioData = Base64.decode(base64EncodedString, Base64.DEFAULT);

                    File outputDir = context.getCacheDir();
                    File audioFile = File.createTempFile("tts_audio_", ".wav", outputDir);
                    try (FileOutputStream fos = new FileOutputStream(audioFile)) {
                        fos.write(audioData);
                    }

                    // ---- Nettoyage du silence ----
                    File trimmedAudioFile = File.createTempFile("tts_audio_trimmed_", ".wav", outputDir);
                    String cmd = "-y -i \"" + audioFile.getAbsolutePath() + "\" " +
                            "-af \"areverse,silenceremove=start_periods=1:start_duration=0.01:start_threshold=-40dB:detection=peak,areverse\" " +
                            "-c:a pcm_s16le \"" + trimmedAudioFile.getAbsolutePath() + "\"";

                    long startTime = System.currentTimeMillis();
                    FFmpeg.executeAsync(cmd, (executionId, returnCode) -> {
                        long duration = System.currentTimeMillis() - startTime;
                        if (returnCode == 0) {
                            Log.i("MYA_API_Google", "Silence supprimé en " + duration + " ms");
                            playAudioFile(trimmedAudioFile);
                        } else {
                            Log.e("MYA_API_Google", "Échec FFmpeg (" + returnCode + ")");
                            playAudioFile(audioFile);
                        }
                    });

                } else {
                    Log.e("MYA_API_Google", "Erreur API TTS - code: " + httpResponse.responseCode + " - corps: " + httpResponse.body);
                    if (mTtsListener != null) mTtsListener.onError();
                }

            } catch (Exception e) {
                Log.e("MYA_API_Google", "Exception pendant l'appel TTS : " + e.getMessage(), e);
                if (mTtsListener != null) mTtsListener.onError();
            }
        });
    }

    private void playAudioFile(File audioFile) {
        try {
            mMediaPlayer = new MediaPlayer();
            FileInputStream fis = new FileInputStream(audioFile);
            mMediaPlayer.setDataSource(fis.getFD());
            fis.close();

            mMediaPlayer.setOnPreparedListener(mp -> {
                if (mTtsListener != null) {
                    mTtsListener.onStart();
                }
                mp.start();
            });

            mMediaPlayer.setOnCompletionListener(mp -> {
                if (mTtsListener != null) {
                    Log.d("MYA_TTS", "retard onDone");
                    mTtsListener.onDone();
                }
                mp.release();
            });

            mMediaPlayer.prepareAsync();

        } catch (Exception e) {
            if (mTtsListener != null) {
                mTtsListener.onError();
            }
            throw new ApiException(e);
        }
    }

    public void stop() {
        if (mMediaPlayer != null) {
            try {
                mMediaPlayer.stop();
            } catch (IllegalStateException ignored) {
                // Already stopped or not initialized
            }
            try {
                mMediaPlayer.reset();
            } catch (IllegalStateException ignored) {}
            mMediaPlayer.release();
            mMediaPlayer = null;
            mVoiceLength = -1;
        }
    }

    public void pause() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            mVoiceLength = mMediaPlayer.getCurrentPosition();
        }
    }

    public void resume() {
        if (mMediaPlayer != null && !mMediaPlayer.isPlaying() && mVoiceLength != -1) {
            mMediaPlayer.seekTo(mVoiceLength);
            mMediaPlayer.start();
        }
    }

    private void playAudio(String base64EncodedString) throws IOException {
        stop();

        String url = "data:audio/mp3;base64," + base64EncodedString;
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setDataSource(url);
        mMediaPlayer.prepare();
        mMediaPlayer.start();
    }

    public void close() {
        stop();
        if (mMediaPlayer !=null){
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

    }

}
