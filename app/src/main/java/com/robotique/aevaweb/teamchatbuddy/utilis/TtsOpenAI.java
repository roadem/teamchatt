package com.robotique.aevaweb.teamchatbuddy.utilis;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.arthenica.mobileffmpeg.FFmpeg;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.gson.JsonObject;
import com.robotique.aevaweb.teamchatbuddy.application.TeamChatBuddyApplication;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Implémentation TTS OpenAI sans Retrofit.
 * Flux binaire direct + preprocessing FFmpeg.
 */
public class TtsOpenAI implements AutoCloseable {

    private final Context context;
    private MediaPlayer mMediaPlayer;
    private TtsOpenAIListener mTtsListener;

    private final TeamChatBuddyApplication app;

    private String model = null;
    private String voice = null;
    private String responseFormat = "mp3";
    private String instructions = null;
    private double speed ;
    private String streamFormat = "audio";

    private int mVoiceLength = -1;

    public TtsOpenAI(Context context, TeamChatBuddyApplication app) {
        this.context = context;
        this.app = app;
    }

    public TtsOpenAI setModel(String model) { this.model = model; return this; }
    public TtsOpenAI setVoice(String voice) { this.voice = voice; return this; }
    public TtsOpenAI setResponseFormat(String format) { this.responseFormat = format; return this; }
    public TtsOpenAI setInstructions(String instructions) { this.instructions = instructions; return this; }
    public TtsOpenAI setSpeed(double speed) { this.speed = speed; return this; }
    public TtsOpenAI setStreamFormat(String format) { this.streamFormat = format; return this; }
    public void setTtsListener(TtsOpenAIListener listener) { this.mTtsListener = listener; }


    /**
     * Lancement TTS OpenAI
     */
    public void start(String apiKey, String text, TtsOpenAIListener mTtsListener) {

        generateSpeech(apiKey,text,mTtsListener);
    }

    public void generateSpeech(
            String apiKey,
            String text,
            TtsOpenAIListener mTtsListener
    ) {
        OkHttpClient client = new OkHttpClient();

        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        try{
            // Corps JSON de la requête
            String apiUrl =
                    app.getParamFromFile("ChatGPT_url", "TeamChatBuddy.properties")
                            + app.getParamFromFile("TTS_OpenAI_ApiEndpoint", "TeamChatBuddy.properties");

            model = app.getParamFromFile("TTS_OpenAI_Model", "TeamChatBuddy.properties").trim();
            voice = app.getParamFromFile("TTS_OpenAI_Voice", "TeamChatBuddy.properties").trim();
            speed = Double.parseDouble(app.getParamFromFile("TTS_OpenAI_Speed", "TeamChatBuddy.properties").trim());
            instructions = app.getParamFromFile("TTS_OpenAI_Instructions", "TeamChatBuddy.properties").trim();
            JSONObject json = new JSONObject();
            json.put("model", model);
            json.put("voice", voice);
            json.put("input", text);
            json.put("format", "mp3");
            json.put("speed", speed);

            // Ajouter "instructions" uniquement si non vide et si le modèle les supporte
            if (instructions != null && !instructions.trim().isEmpty()) {
                json.put("instructions", instructions);
            }

            RequestBody body = RequestBody.create(json.toString(), JSON);

            Request request = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        mTtsListener.onError();
                        return;
                    }

                    // Le flux audio binaire
                    byte[] audioBytes = response.body().bytes();

                    // Écrire dans un fichier MP3
                    File temp = File.createTempFile("tts", ".mp3");
                    FileOutputStream fos = new FileOutputStream(temp);
                    fos.write(audioBytes);
                    fos.close();

                    playAudio(temp, mTtsListener);
                    //mTtsListener.onResponse(outputFile);
                }
            });
        }catch (Exception e) {
            mTtsListener.onError();
        }
    }

    /**
     * Lecture MediaPlayer
     */


    private void playAudio(File audioFile, TtsOpenAIListener listener) {
        try {
            MediaPlayer player = new MediaPlayer();

            player.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
            );

            player.setDataSource(audioFile.getAbsolutePath());

            // Timeout de sécurité
            final Handler handler = new Handler(Looper.getMainLooper());
            final Runnable safetyTimeout = () -> {
                if (player.isPlaying()) {
                    player.stop();
                }
                player.release();
                listener.onDone(); // ne pas bloquer le workflow
            };

            player.setOnPreparedListener(mp -> {
                listener.onStart();
                mp.start();

                // durée réelle + marge de sécurité
                int duration = mp.getDuration(); // ms
                int safetyMargin = 1000; // 1 sec

                handler.postDelayed(safetyTimeout, duration + safetyMargin);
            });

            player.setOnCompletionListener(mp -> {
                handler.removeCallbacks(safetyTimeout);
                mp.release();
                listener.onDone();
            });

            player.setOnErrorListener((mp, what, extra) -> {
                handler.removeCallbacks(safetyTimeout);
                mp.release();
                listener.onError();
                return true;
            });

            player.prepareAsync();

        } catch (Exception e) {
            listener.onError();
        }
    }


    /**
     * Lecture binaire utilitaire
     */
    private byte[] readAllBytes(InputStream is) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int n;
        while ((n = is.read(data)) != -1) {
            buffer.write(data, 0, n);
        }
        return buffer.toByteArray();
    }


    public void stop() {
        if (mMediaPlayer != null) {
            try {
                if (mMediaPlayer.isPlaying()) mMediaPlayer.stop();
            } catch (Exception ignored) {}

            try { mMediaPlayer.reset(); } catch (Exception ignored) {}

            try { mMediaPlayer.release(); } catch (Exception ignored) {}

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

    @Override
    public void close() {
        stop();
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }
}

