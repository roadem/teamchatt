package com.robotique.aevaweb.teamchatbuddy.utilis;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

import com.arthenica.mobileffmpeg.FFmpeg;
import com.google.gson.JsonObject;
import com.robotique.aevaweb.teamchatbuddy.application.TeamChatBuddyApplication;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private String responseFormat = "wav";
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
    public void start(String apiKey, String text) {
        stop();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {

                model = app.getParamFromFile("model_openai_tts", "TeamChatBuddy.properties").trim();
                voice = app.getParamFromFile("openai_tts_voice", "TeamChatBuddy.properties").trim();
                speed = Double.parseDouble(app.getParamFromFile("openai_tts_speed", "TeamChatBuddy.properties").trim());
                instructions = app.getParamFromFile("openai_tts_instructions", "TeamChatBuddy.properties").trim();

                Log.i("MYA_TTS_OpenAI", "=== TTS OpenAI Start ===");
                Log.i("MYA_TTS_OpenAI", "Texte : " + text);
                Log.i("MYA_TTS_OpenAI", "model : " + model);
                Log.i("MYA_TTS_OpenAI", "voice : " + voice);
                Log.i("MYA_TTS_OpenAI", "response_format : " + responseFormat);
                Log.i("MYA_TTS_OpenAI", "speed : " + speed);
                Log.i("MYA_TTS_OpenAI", "stream_format : " + streamFormat);
                Log.i("MYA_TTS_OpenAI", "instructions : " + instructions);

                // JSON payload
                JsonObject body = new JsonObject();
                body.addProperty("model", model);
                body.addProperty("input", text);
                body.addProperty("voice", voice);
                body.addProperty("response_format", responseFormat);
                body.addProperty("speed", speed);
                body.addProperty("stream_format", streamFormat);

                if (instructions != null && !instructions.isEmpty()) {
                    body.addProperty("instructions", instructions);
                }

                byte[] jsonBytes = body.toString().getBytes(StandardCharsets.UTF_8);

                // Connexion HTTP brute
                URL url = new URL("https://api.openai.com/v1/audio/speech");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setDoInput(true);

                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "audio/*");

                // Envoi JSON
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonBytes);
                }

                int code = conn.getResponseCode();
                Log.i("MYA_TTS_OpenAI", "HTTP status : " + code);

                InputStream is = (code >= 200 && code < 300)
                        ? conn.getInputStream()
                        : conn.getErrorStream();

                if (is == null) {
                    Log.e("MYA_TTS_OpenAI", "Réponse nulle");
                    if (mTtsListener != null) mTtsListener.onError();
                    return;
                }

                byte[] audioBytes = readAllBytes(is);

                if (code < 200 || code >= 300) {
                    Log.e("MYA_TTS_OpenAI", "Erreur API : " + new String(audioBytes));
                    if (mTtsListener != null) mTtsListener.onError();
                    return;
                }

                // Stockage fichier
                File outputDir = context.getCacheDir();
                String extension = "." + responseFormat;
                File audioFile = File.createTempFile("openai_tts_", extension, outputDir);

                try (FileOutputStream fos = new FileOutputStream(audioFile)) {
                    fos.write(audioBytes);
                }

                Log.i("MYA_TTS_OpenAI", "Fichier audio : " + audioFile.getAbsolutePath());

                // Post-traitement FFmpeg
                File trimmedAudioFile = File.createTempFile("tts_trimmed_", extension, outputDir);

                String cmd = "-y -i \"" + audioFile.getAbsolutePath() + "\" " +
                        "-af \"areverse,silenceremove=start_periods=1:start_duration=0.01:start_threshold=-40dB:detection=peak,areverse\" " +
                        "-c:a pcm_s16le \"" + trimmedAudioFile.getAbsolutePath() + "\"";

                long startTime = System.currentTimeMillis();

                FFmpeg.executeAsync(cmd, (executionId, returnCode) -> {
                    long duration = System.currentTimeMillis() - startTime;

                    if (returnCode == 0) {
                        Log.i("MYA_TTS_OpenAI", "Silence trimming OK (" + duration + " ms)");
                        playAudioFile(trimmedAudioFile);
                    } else {
                        Log.e("MYA_TTS_OpenAI", "FFmpeg fail → lecture brute");
                        playAudioFile(audioFile);
                    }
                });

            } catch (Exception e) {
                Log.e("MYA_TTS_OpenAI", "Erreur TTS : " + e.getMessage(), e);
                if (mTtsListener != null) mTtsListener.onError();
            }
        });
    }


    /**
     * Lecture MediaPlayer
     */
    private void playAudioFile(File audioFile) {
        try {
            mMediaPlayer = new MediaPlayer();

            FileInputStream fis = new FileInputStream(audioFile);
            mMediaPlayer.setDataSource(fis.getFD());
            fis.close();

            mMediaPlayer.setOnPreparedListener(mp -> {
                if (mTtsListener != null) mTtsListener.onStart();
                mp.start();
            });

            mMediaPlayer.setOnCompletionListener(mp -> {
                if (mTtsListener != null) mTtsListener.onDone();
                mp.release();
            });

            mMediaPlayer.prepareAsync();

        } catch (Exception e) {
            Log.e("MYA_TTS_OpenAI", "Erreur lecture : " + e.getMessage());
            if (mTtsListener != null) mTtsListener.onError();
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
            try { mMediaPlayer.stop(); } catch (Exception ignored) {}
            try { mMediaPlayer.reset(); } catch (Exception ignored) {}
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

    @Override
    public void close() {
        stop();
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }
}
