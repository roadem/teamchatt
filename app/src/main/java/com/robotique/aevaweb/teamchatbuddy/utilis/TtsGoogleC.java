package com.robotique.aevaweb.teamchatbuddy.utilis;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Base64;
import android.util.Log;

import com.arthenica.mobileffmpeg.FFmpeg;
import com.robotique.aevaweb.teamchatbuddy.application.TeamChatBuddyApplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

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

    public void start(String text) {
        if (mVoiceSelectionParams == null) {
            throw new NullPointerException("You forget to setVoiceSelectionParams()");
        }

        if (mAudioConfig == null) {
            throw new NullPointerException("You forget to setAudioConfig()");
        }

        SynthesizeRequest request = new SynthesizeRequest(new SynthesisInput(text), mVoiceSelectionParams, mAudioConfig);

        try {
            SynthesizeResponse response = mSynthesizeApi.get(request);
            //playAudio(response.getAudioContent());
            stop();
            String base64EncodedString = response.getAudioContent();
            byte[] audioData = Base64.decode(base64EncodedString, Base64.DEFAULT);

            File outputDir = context.getCacheDir(); // ou getExternalFilesDir(null)
            File audioFile = File.createTempFile("tts_audio_", ".wav", outputDir);
            try (FileOutputStream fos = new FileOutputStream(audioFile)) {
                fos.write(audioData);
            }

            File trimmedAudioFile = File.createTempFile("tts_audio_trimmed_", ".wav", outputDir);

            String cmd = "-y -i \"" + audioFile.getAbsolutePath() + "\" " +
                    "-af \"areverse,silenceremove=start_periods=1:start_duration=0.01:start_threshold=-40dB:detection=peak,areverse\" " +
                    "-c:a pcm_s16le \"" + trimmedAudioFile.getAbsolutePath() + "\"";

            FFmpeg.executeAsync(cmd, (executionId, returnCode) -> {
                if (returnCode == 0) {
                    // Succès, jouer trimmedAudioFile
                    playAudioFile(trimmedAudioFile);
                } else {
                    // Échec, jouer audio original
                    Log.e("FFmpeg", "Failed to remove silence, return code: " + returnCode);
                    playAudioFile(audioFile);
                }
            });

        } catch (Exception e) {
            if (mTtsListener != null) {
                mTtsListener.onError();
            }
            throw new ApiException(e);
        }
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
