package com.robotique.aevaweb.teamchatbuddy.chatbotresponse;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bfr.buddy.usb.shared.IUsbCommadRsp;
import com.bfr.buddysdk.BuddySDK;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.robotique.aevaweb.teamchatbuddy.R;
import com.robotique.aevaweb.teamchatbuddy.application.TeamChatBuddyApplication;
import com.robotique.aevaweb.teamchatbuddy.models.Langue;
import com.robotique.aevaweb.teamchatbuddy.models.Setting;
import com.robotique.aevaweb.teamchatbuddy.utilis.ApiEndpointInterface;
import com.robotique.aevaweb.teamchatbuddy.utilis.BIPlayer;
import com.robotique.aevaweb.teamchatbuddy.utilis.IBehaviourCallBack;
import com.robotique.aevaweb.teamchatbuddy.utilis.ImageGenerator;
import com.robotique.aevaweb.teamchatbuddy.utilis.NetworkClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class Commande {
    private String TAG="Commande";
    Activity activity;
    TeamChatBuddyApplication teamChatBuddyApplication;
    private Setting settingClass;
    ResponseFromChatbot responseClass;
    private String configFile ="TeamChatBuddy.properties";
    private String heart_rate;
    private String tensionS;
    private String tensionD;
    private String imeiLocation;
    private String imeiFeeder;
    private static MediaPlayer radioPlayer;
    private static MediaPlayer musicPlayer;


    public Commande(Activity activity){
        this.activity = activity;
        teamChatBuddyApplication = (TeamChatBuddyApplication) activity.getApplicationContext();
        settingClass = new Setting();
    }


    public String regex(String text){
        Matcher matcher = Pattern.compile("<(CMD_.*?)>|<(HEALYSA_.*?)>|<(SWITCHBOT_.*?)>").matcher(text);
        Log.i("text", text);
        if(matcher.find()){
            String match = matcher.group(1) != null ? matcher.group(1) :
                    matcher.group(2) != null ? matcher.group(2) :
                            matcher.group(3);

            Log.e("Commande", "match ="+match);
            return match;
        }
        return "INCONNUE";
    }

    public String getDescription(String text){
        int index = text.indexOf("CMD_");
        if(index != -1) {
            int cmdLength = 4; // Length of "CMD_"
            int endIndex = text.indexOf(" ", index + cmdLength); // Find the index of the next space after CMD_
            if (endIndex == -1) {
                Log.e(TAG, "Extract Description from : "+text + " --> INCONNUE");
                return "INCONNUE"; // No space found after CMD_
            }
            Log.i(TAG, "Extract Description from : "+text + " --> " + text.substring(endIndex + 1));
            return text.substring(endIndex + 1); // Return substring after the space
        }
        Log.e(TAG, "Extract Description from : "+text + " --> INCONNUE");
        return "INCONNUE"; // No CMD_ found
    }

    public String verifyCmdMessages(String message) {
        if (message.isEmpty()) {
            return "EMPTY";
        }
        else if (!message.matches(".*\\s*/\\s*(?:/\\s*)?.*")) {
            return "DO_NOT_CONTAIN_SPLIT_CHARACTER";
        }

        String[] parts = message.split("\\s*/\\s*(?:/\\s*)?");

        String before = parts[0].trim();
        String after = parts.length > 1 ? parts[1].trim() : "";

        boolean hasBefore = !before.isEmpty();
        boolean hasAfter = !after.isEmpty();

        if (hasBefore && hasAfter) {
            return "CONTAIN_BOTH_PARTS";
        }
        else if (hasBefore) {
            return "CONTAIN_ONLY_FIRST_PART";
        }
        else if (hasAfter) {
            return "CONTAIN_ONLY_SECOND_PART";
        }
        else {
            return "CONTAIN_ONLY_SPLIT_CHARACTER";
        }
    }


    public void translate(String message, ITranslationCallback iTranslationCallback){
        if (teamChatBuddyApplication.getLangue().getNom().equals("Anglais") ){
            iTranslationCallback.onTranslated(teamChatBuddyApplication.getParamFromFile(message+"_en", "TeamChatBuddy.properties"));
        }
        else if (teamChatBuddyApplication.getLangue().getNom().equals("Français") ) {
            iTranslationCallback.onTranslated(teamChatBuddyApplication.getParamFromFile(message+"_fr", "TeamChatBuddy.properties"));
        }
        else {
            teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(teamChatBuddyApplication.getParamFromFile(message+"_en", "TeamChatBuddy.properties"))
                    .addOnSuccessListener(new OnSuccessListener<String>() {
                        @Override
                        public void onSuccess(String translatedText) {
                            iTranslationCallback.onTranslated(translatedText);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            iTranslationCallback.onTranslated(teamChatBuddyApplication.getParamFromFile(message+"_en", "TeamChatBuddy.properties"));
                        }
                    });
        }
    }

    private Locale getCurrentLocale(){
        String language =new Gson().fromJson(teamChatBuddyApplication.getparam(teamChatBuddyApplication.getLangue().getNom()), Langue.class).getLanguageCode().replace("-","_");
        Locale[] locales = Locale.getAvailableLocales();
        for (Locale locale : locales) {
            if (locale.toString().equals(language)) {
                return locale;
            }
        }
        return Locale.ENGLISH;
    }

    boolean containsLanguageWithName(List<Langue> langues, String languageName) {
        for (Langue langue : langues) {
            if (langue.getNom().equalsIgnoreCase(languageName)) {
                return true;
            }
        }
        return false;
    }

    public interface ITranslationCallback {
        void onTranslated(String translatedText);
    }

    public boolean start_action(@NonNull String action){
        boolean is_command;
        switch (action.split( " " )[0]){
            case "CMD_MUSIC":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_MUSIC", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if(verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART") ){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_MUSIC(getDescription(action));
                                }
                            },2000);

                        }
                        else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_MUSIC(getDescription(action));
                                }
                            },2000);
                        }
                        else if(verifyMusicMessage.equals("EMPTY")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        }
                        else {
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_MUSIC(getDescription(action));
                                }
                            },2000);
                        }

                    }
                });
                break;
            case "CMD_DATE":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_DATE", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if(verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART") ){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_DATE();
                                }
                            },2000);

                        }
                        else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_DATE();
                                }
                            },2000);
                        }
                        else if(verifyMusicMessage.equals("EMPTY")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        }
                        else {
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_DATE();
                                }
                            },2000);
                        }
                    }
                });
                break;

            case "CMD_HOUR":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_HOUR", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if(verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART") ){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_HEURE();
                                }
                            },2000);

                        }
                        else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_HEURE();
                                }
                            },2000);
                        }
                        else if(verifyMusicMessage.equals("EMPTY")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        }
                        else {
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_HEURE();
                                }
                            },2000);
                        }
                    }
                });
                break;

            case "CMD_LANGUE":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_LANGUE", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if(verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART") ){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_LANGUE(action.split( " " )[1]);
                                }
                            },2000);

                        }
                        else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_LANGUE(action.split( " " )[1]);
                                }
                            },2000);
                        }
                        else if(verifyMusicMessage.equals("EMPTY")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        }
                        else {
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_LANGUE(action.split( " " )[1]);
                                }
                            },2000);
                        }
                    }
                });
                break;

            case "CMD_TEMP":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_TEMP", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if(verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART") ){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_TEMP(action.split( " " )[1]);
                                }
                            },2000);

                        }
                        else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_TEMP(action.split( " " )[1]);
                                }
                            },2000);
                        }
                        else if(verifyMusicMessage.equals("EMPTY")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        }
                        else {
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_TEMP(action.split( " " )[1]);
                                }
                            },2000);
                        }
                    }
                });
                break;

            case "CMD_BATTERIE":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_BATTERIE", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if(verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART") ){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_BATTERIE();
                                }
                            },2000);

                        }
                        else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_BATTERIE();
                                }
                            },2000);
                        }
                        else if(verifyMusicMessage.equals("EMPTY")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        }
                        else {
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_BATTERIE();
                                }
                            },2000);
                        }
                    }
                });
                break;

            case "CMD_SOUND":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_SOUND", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if(verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART") ){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_SOUND(action.split( " " )[1]);
                                }
                            },2000);

                        }
                        else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_SOUND(action.split( " " )[1]);
                                }
                            },2000);
                        }
                        else if(verifyMusicMessage.equals("EMPTY")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        }
                        else {
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_SOUND(action.split( " " )[1]);
                                }
                            },2000);
                        }
                    }
                });
                break;

            case "CMD_MOVE":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_MOVE", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if(verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART") ){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_MOVE(action.split( " " )[1]);
                                }
                            },2000);

                        }
                        else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_MOVE(action.split( " " )[1]);
                                }
                            },2000);
                        }
                        else if(verifyMusicMessage.equals("EMPTY")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        }
                        else {
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_MOVE(action.split( " " )[1]);
                                }
                            },2000);
                        }
                    }
                });
                break;

            case "CMD_TURN":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_TURN", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if(verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART") ){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_TURN(action.split( " " )[1]);
                                }
                            },2000);

                        }
                        else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_TURN(action.split( " " )[1]);
                                }
                            },2000);
                        }
                        else if(verifyMusicMessage.equals("EMPTY")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        }
                        else {
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_TURN(action.split( " " )[1]);
                                }
                            },2000);
                        }
                    }
                });
                break;

            case "CMD_HEAD":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_HEAD", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if(verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART") ){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_HEAD(action.split( " " )[1]);
                                }
                            },2000);

                        }
                        else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_HEAD(action.split( " " )[1]);
                                }
                            },2000);
                        }
                        else if(verifyMusicMessage.equals("EMPTY")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        }
                        else {
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_HEAD(action.split( " " )[1]);
                                }
                            },2000);
                        }
                    }
                });
                break;

            case "CMD_STOP":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_STOP", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if(verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART") ){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_STOP();
                                }
                            },2000);

                        }
                        else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_STOP();
                                }
                            },2000);
                        }
                        else if(verifyMusicMessage.equals("EMPTY")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        }
                        else {
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_STOP();
                                }
                            },2000);
                        }
                    }
                });
                break;

            case "CMD_DANCE":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_DANCE", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if(verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART") ){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_DANCE();
                                }
                            },2000);

                        }
                        else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_DANCE();
                                }
                            },2000);
                        }
                        else if(verifyMusicMessage.equals("EMPTY")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        }
                        else {
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_DANCE();
                                }
                            },2000);
                        }
                    }
                });
                break;

            case "CMD_METEO":
                is_command = true;
                Log.i( TAG, action );
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_METEO", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if(verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART") ){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0].replace("[1]",action.split( " " )[1]));
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_METEO(action.split( " " )[1]);
                                }
                            },2000);

                        }
                        else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.replace("[1]",action.split( " " )[1]));
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_METEO(action.split( " " )[1]);
                                }
                            },2000);
                        }
                        else if(verifyMusicMessage.equals("EMPTY")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        }
                        else {
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_METEO(action.split( " " )[1]);
                                }
                            },2000);
                        }
                    }
                });
                break;

            case "CMD_RADIO":
                is_command = true;
                Log.i( TAG, action );
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_RADIO", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if(verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART") ){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0].replace("[1]",getDescription(action)));
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_RADIO(getDescription(action));
                                }
                            },2000);

                        }
                        else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.replace("[1]",getDescription(action)));
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_RADIO(getDescription(action));
                                }
                            },2000);
                        }
                        else if(verifyMusicMessage.equals("EMPTY")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        }
                        else {
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_RADIO(getDescription(action));
                                }
                            },2000);
                        }
                    }
                });
                break;
            case "CMD_BI":
                is_command = true;
                Log.i( TAG, action );
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_BI", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyBIMessage = verifyCmdMessages(translatedText);
                        if(verifyBIMessage.equals("CONTAIN_BOTH_PARTS") || verifyBIMessage.equals("CONTAIN_ONLY_FIRST_PART") ){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0].replace("[1]",getDescription(action)));
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_BI(getDescription(action));
                                }
                            },5000);

                        }
                        else if (verifyBIMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.replace("[1]",getDescription(action)));
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_BI(getDescription(action));
                                }
                            },5000);
                        }
                        else if(verifyBIMessage.equals("EMPTY")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        }
                        else {
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_BI(getDescription(action));
                                }
                            },5000);
                        }
                    }
                });
                break;
            case "SWITCHBOT_LIGHT":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("SWITCHBOT_LIGHT", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if(verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART") ){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0].replace("[1]",action.split( " " )[1]));
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    SWITCHBOT_LIGHT(action.split( " " )[1]);
                                }
                            },2000);

                        }
                        else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    SWITCHBOT_LIGHT(action.split( " " )[1]);
                                }
                            },2000);
                        }
                        else if(verifyMusicMessage.equals("EMPTY")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        }
                        else {
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    SWITCHBOT_LIGHT(action.split( " " )[1]);
                                }
                            },2000);
                        }
                    }
                });
                break;

            case "HEALYSA_CONNECT":
                is_command = true;
                Log.i( TAG, action );
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("HEALYSA_CONNECT", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if(verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART") ){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    HEALYSA_CONNECT(action.split( " ")[1]);
                                }
                            },2000);

                        }
                        else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    HEALYSA_CONNECT(action.split( " ")[1]);
                                }
                            },2000);
                        }
                        else if(verifyMusicMessage.equals("EMPTY")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        }
                        else {
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    HEALYSA_CONNECT(action.split( " ")[1]);
                                }
                            },2000);
                        }
                    }
                });
                break;

            case "HEALYSA_HRV":
                is_command = true;
                Log.i( TAG, action );
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("HEALYSA_HRV", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if(verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART") ){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    HEALYSA_HRV();
                                }
                            },2000);

                        }
                        else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    HEALYSA_HRV();
                                }
                            },2000);
                        }
                        else if(verifyMusicMessage.equals("EMPTY")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        }
                        else {
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    HEALYSA_HRV();
                                }
                            },2000);
                        }
                    }
                });
                break;

            case "HEALYSA_BLOODP":
                is_command = true;
                Log.i( TAG, action );
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("HEALYSA_BLOODP", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if(verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART") ){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    HEALYSA_BLOODP();
                                }
                            },2000);

                        }
                        else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    HEALYSA_BLOODP();
                                }
                            },2000);
                        }
                        else if(verifyMusicMessage.equals("EMPTY")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        }
                        else {
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    HEALYSA_BLOODP();
                                }
                            },2000);
                        }
                    }
                });
                break;

            case "HEALYSA_SPO2":
                is_command = true;
                Log.i( TAG, action );
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("HEALYSA_SPO2", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if(verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART") ){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    HEALYSA_SPO2();
                                }
                            },2000);

                        }
                        else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    HEALYSA_SPO2();
                                }
                            },2000);
                        }
                        else if(verifyMusicMessage.equals("EMPTY")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        }
                        else {
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    HEALYSA_SPO2();
                                }
                            },2000);
                        }
                    }
                });
                break;

            case "HEALYSA_CHECKUP":
                is_command = true;
                Log.i( TAG, action );
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("HEALYSA_CHECKUP", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if(verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART") ){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    HEALYSA_CHECKUP();
                                }
                            },2000);

                        }
                        else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    HEALYSA_CHECKUP();
                                }
                            },2000);
                        }
                        else if(verifyMusicMessage.equals("EMPTY")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        }
                        else {
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    HEALYSA_CHECKUP();
                                }
                            },2000);
                        }
                    }
                });
                break;

            case "HEALYSA_CALL":
                is_command = true;
                Log.i( TAG, action );
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("HEALYSA_CALL", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if(verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART") ){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    HEALYSA_CALL( action.split( " " )[1]);
                                }
                            },2000);

                        }
                        else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    HEALYSA_CALL( action.split( " " )[1]);
                                }
                            },2000);
                        }
                        else if(verifyMusicMessage.equals("EMPTY")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        }
                        else {
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    HEALYSA_CALL( action.split( " " )[1]);
                                }
                            },2000);
                        }
                    }
                });
                break;

            case "HEALYSA_LOC":
                is_command = true;
                Log.i( TAG, action );
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("HEALYSA_LOC", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if(verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART") ){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    HEALYSA_LOC( action.split( " " )[1]);
                                }
                            },2000);

                        }
                        else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    HEALYSA_LOC( action.split( " " )[1]);
                                }
                            },2000);
                        }
                        else if(verifyMusicMessage.equals("EMPTY")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        }
                        else {
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    HEALYSA_LOC( action.split( " " )[1]);
                                }
                            },2000);
                        }
                    }
                });
                break;

            case "HEALYSA_FEEDCAT":
                is_command = true;
                Log.i( TAG, action );
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("HEALYSA_FEEDCAT", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if(verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART") ){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    HEALYSA_FEEDCAT(action.split( " " )[1]);
                                }
                            },2000);

                        }
                        else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    HEALYSA_FEEDCAT(action.split( " " )[1]);
                                }
                            },2000);
                        }
                        else if(verifyMusicMessage.equals("EMPTY")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        }
                        else {
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    HEALYSA_FEEDCAT(action.split( " " )[1]);
                                }
                            },2000);
                        }
                    }
                });
                break;

            case "CMD_IMAGE":
                is_command = true;
                Log.i(TAG, action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_IMAGE", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if(verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART") ){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_IMAGE(getDescription(action));
                                }
                            },2000);

                        }
                        else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_IMAGE(getDescription(action));
                                }
                            },2000);
                        }
                        else if(verifyMusicMessage.equals("EMPTY")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        }
                        else {
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_IMAGE(getDescription(action));
                                }
                            },2000);
                        }
                    }
                });
                break;

            case "CMD_CLOSE_IMAGE":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_CLOSE_IMAGE", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if(verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART") ){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[0]);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_CLOSE_IMAGE();
                                }
                            },2000);

                        }
                        else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_CLOSE_IMAGE();
                                }
                            },2000);
                        }
                        else if(verifyMusicMessage.equals("EMPTY")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        }
                        else {
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_CLOSE_IMAGE();
                                }
                            },2000);
                        }
                    }
                });
                break;
            case "CMD_PHOTO":
                is_command = true;
                Log.i(TAG,action);
                teamChatBuddyApplication.notifyObservers("CANCEL_RESPONSE_TIMEOUT");
                translate("CMD_PHOTO", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        String verifyMusicMessage = verifyCmdMessages(translatedText);
                        if(verifyMusicMessage.equals("CONTAIN_BOTH_PARTS") || verifyMusicMessage.equals("CONTAIN_ONLY_FIRST_PART") ){


                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split( "//" )[0]);

                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_TAKE_PHOTO();
                                }
                            },2000);

                        }
                        else if (verifyMusicMessage.equals("DO_NOT_CONTAIN_SPLIT_CHARACTER")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText);
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_TAKE_PHOTO();
                                }
                            },2000);
                        }
                        else if(verifyMusicMessage.equals("EMPTY")){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CANCEL");
                        }
                        else {
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CMD_TAKE_PHOTO();
                                }
                            },2000);
                        }
                    }
                });
                break;
            default:
                is_command = false;
                Log.i(TAG,"DEFAULT : "+ action);
        }
        return is_command;
    }

    public void CMD_MUSIC(String description){
        Log.i( TAG, "CMD MUSIC '"+ description +"' début." );

        //init parameters
        String music_URL = teamChatBuddyApplication.getParamFromFile("Music_URL",configFile);
        String music_endpoint = teamChatBuddyApplication.getParamFromFile("Music_endpoint",configFile);
        String music_model = teamChatBuddyApplication.getParamFromFile("Music_model",configFile);
        int music_duration = Integer.parseInt(teamChatBuddyApplication.getParamFromFile("Music_duration",configFile));
        if(music_URL == null || music_URL.isEmpty()) music_URL = "http://34.34.175.122:5000";
        if(music_endpoint == null || music_endpoint.isEmpty()) music_endpoint = "/generate_music";
        if(music_model == null || music_model.isEmpty()) music_model = "small";
        if(music_duration == 0) music_duration = 20;

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("text", description);
            jsonObject.put("model", music_model);
            jsonObject.put("duration", music_duration);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonObject.toString());

        Retrofit retrofit = NetworkClient.getRetrofitClient(teamChatBuddyApplication,music_URL, 50);
        ApiEndpointInterface api = retrofit.create(ApiEndpointInterface.class);
        Call<ResponseBody> call = api.generateMusic(music_endpoint,requestBody);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {

                        Log.e(TAG,"generateMusic response successful --> save audio file : generated_music.mp3");

                        //save audio file
                        InputStream inputStream = response.body().byteStream();
                        File directory = new File( teamChatBuddyApplication.getString(R.string.path), "TeamChatBuddy");
                        File outputFile = new File(directory, "generated_music.mp3");
                        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            if (inputStream != null) {
                                inputStream.close();
                            }
                        }

                        playMusic(outputFile);

                    } catch (Exception e) {
                        Log.e(TAG,"generateMusic ERROR "+e);
                        e.printStackTrace();
                    }
                }
                else {
                    if (response != null && response.errorBody() != null) {
                        Log.e(TAG, "generateMusic response [not successful] ");
                        String jsonString = null;
                        try {
                            jsonString = response.errorBody().string();
                            JSONObject jsonErrorContent = new JSONObject(jsonString);

                            String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= CMD_MUSIC, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                            logErrorAPIHealysa("CMD_MUSIC", errorTXT, "notOnFailure");
                        } catch (IOException | JSONException e) {
                            e.printStackTrace();
                            Log.e(TAG, "generateMusic response [not successful]1 catch" + e);
                        }
                    }
                    Log.e(TAG,"generateMusic response not successful");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG,"generateMusic onFailure : " + t);
                logErrorAPIHealysa("CMD_MUSIC",t.getMessage(),"onFailure");
                t.printStackTrace();
            }
        });



    }
    public void CMD_DATE(){
        String date = new SimpleDateFormat("EEEE d MMMM yyyy", getCurrentLocale()).format(new Date());
        Log.i(TAG, date);
        translate("CMD_DATE", new ITranslationCallback() {
            @Override
            public void onTranslated(String translatedText) {
                String verifyMessage = verifyCmdMessages(translatedText);
                if(verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART") ){
                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +translatedText.split("\\s*/\\s*(?:/\\s*)?")[1].replace("[1]",date));
                }
            }
        });
    }
    public void CMD_HEURE(){
        String heure = new SimpleDateFormat("h:mm", getCurrentLocale()).format(new Date());
        Log.i(TAG, heure);
        translate("CMD_HOUR", new ITranslationCallback() {
            @Override
            public void onTranslated(String translatedText) {
                String verifyMessage = verifyCmdMessages(translatedText);
                if(verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART") ){
                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +translatedText.split("\\s*/\\s*(?:/\\s*)?")[1].replace("[1]",heure));
                }
            }
        });
    }
    public void CMD_LANGUE(String languageName){
        List<Langue> langues = new ArrayList<>();
        List<String> langueDisponible = teamChatBuddyApplication.getDisponibleLangue();
        for (int i=1;i<langueDisponible.size();i++){
            langues.add(new Gson().fromJson(teamChatBuddyApplication.getparam(langueDisponible.get(i-1)), Langue.class));
            i++;
        }
        if (langues.isEmpty()){
            langues.add(new Gson().fromJson(teamChatBuddyApplication.getparam("Français"), Langue.class));
        }

        boolean containsLanguage = containsLanguageWithName(langues, languageName);
        if (containsLanguage) {
            for(Langue langue : langues ) {
                langue.setChosen(false);
                if (langue.getNom().equalsIgnoreCase(languageName)) {
                    langue.setChosen(true);
                    teamChatBuddyApplication.setLangue(langue);
                }
                if(langue.getNom().equals("Français")){
                    teamChatBuddyApplication.setparam("Français",new Gson().toJson(langue));
                }
                else if(langue.getNom().equals("Anglais")){
                    teamChatBuddyApplication.setparam("Anglais",new Gson().toJson(langue));
                }
                else if (langue.getNom().equals("Espagnol")){
                    teamChatBuddyApplication.setparam("Espagnol",new Gson().toJson(langue));
                }
                else if (langue.getNom().equals("Allemand")){
                    teamChatBuddyApplication.setparam("Allemand",new Gson().toJson(langue));
                }else{
                    teamChatBuddyApplication.setparam(langue.getNom(),new Gson().toJson(langue));
                }
                if (langue.getNom().equalsIgnoreCase(languageName)) {
                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;CHANGE_LANGUE");
                }
            }
        }
        else {
            //langue non disponible
        }
    }
    public void CMD_TEMP(String temp){
        teamChatBuddyApplication.setparam( "Temperature_chatgpt", temp );
        translate("CMD_TEMP", new ITranslationCallback() {
            @Override
            public void onTranslated(String translatedText) {
                String verifyMessage = verifyCmdMessages(translatedText);
                if(verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART") ){
                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                }
            }
        });
    }
    public void CMD_BATTERIE(){
        Log.i( TAG, "CMD BATTERIE début." );
        int batLevel = BuddySDK.Sensors.Battery().getBatteryLevel();
        Log.i(TAG, batLevel+"%");
        translate("CMD_BATTERIE", new ITranslationCallback() {
            @Override
            public void onTranslated(String translatedText) {
                String verifyMessage = verifyCmdMessages(translatedText);
                if(verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART") ){
                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +translatedText.split("\\s*/\\s*(?:/\\s*)?")[1].replace("[1]",batLevel+""));
                }
            }
        });
    }
    public void CMD_SOUND(String sound){
        teamChatBuddyApplication.setVolume( Integer.parseInt( sound ) );
        teamChatBuddyApplication.setparam("speak_volume", sound);
        teamChatBuddyApplication.setSpeakVolume( Integer.parseInt( sound ) );
        settingClass.setVolume(sound);
        translate("CMD_SOUND", new ITranslationCallback() {
            @Override
            public void onTranslated(String translatedText) {
                String verifyMessage = verifyCmdMessages(translatedText);
                if(verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART") ){
                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                }
            }
        });
    }
    public void CMD_MOVE(String move){

        teamChatBuddyApplication.notifyObservers("STOP_TRACKING");

        int no_speed = 30;
        int dist = Integer.parseInt( move );

        if(BuddySDK.Actuators.getLeftWheelStatus().toUpperCase().contains("DISABLE") || BuddySDK.Actuators.getRightWheelStatus().toUpperCase().contains("DISABLE")) {
            BuddySDK.USB.enableWheels( true,new IUsbCommadRsp.Stub() {
                @Override
                public void onSuccess(String success) throws RemoteException {
                    Log.i( "Motor Wheel", "Wheel motor Enabled" );
                }

                @Override
                public void onFailed(String error) throws RemoteException {
                    Log.i( "Motor Wheel", "Wheel motor Enabled Failed" );
                }
            } );
        }

        BuddySDK.USB.moveBuddy( no_speed, dist, new IUsbCommadRsp.Stub() {
            @Override
            public void onSuccess(String success) throws RemoteException {
                Log.i("No movement", success);
                if(success.equals("WHEEL_MOVE_FINISHED")){
                    translate("CMD_MOVE", new ITranslationCallback() {
                        @Override
                        public void onTranslated(String translatedText) {
                            String verifyMessage = verifyCmdMessages(translatedText);
                            if(verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART") ){
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                            }
                        }
                    });
                    teamChatBuddyApplication.notifyObservers("RESTART_TRACKING");
                }
            }

            @Override
            public void onFailed(String error) throws RemoteException {
                Log.e("No movement", error);
                teamChatBuddyApplication.notifyObservers("RESTART_TRACKING");
            }
        } );
    }
    public void CMD_TURN(String turn){

        teamChatBuddyApplication.notifyObservers("STOP_TRACKING");

        int no_speed = 100;
        int angle = Integer.parseInt(turn);

        if(BuddySDK.Actuators.getLeftWheelStatus().toUpperCase().contains("DISABLE") || BuddySDK.Actuators.getRightWheelStatus().toUpperCase().contains("DISABLE")) {
            BuddySDK.USB.enableWheels( true,new IUsbCommadRsp.Stub() {
                @Override
                public void onSuccess(String success) throws RemoteException {
                    Log.i( "Motor Wheel", "Wheel motor Enabled" );
                }

                @Override
                public void onFailed(String error) throws RemoteException {
                    Log.i( "Motor Wheel", "Wheel motor Enabled Failed" );
                }
            } );
        }

        BuddySDK.USB.rotateBuddy( no_speed, angle, new IUsbCommadRsp.Stub() {
            @Override
            public void onSuccess(String success) throws RemoteException {
                Log.i( "No movement", success );
                if(success.equals("WHEEL_MOVE_FINISHED")){
                    translate("CMD_TURN", new ITranslationCallback() {
                        @Override
                        public void onTranslated(String translatedText) {
                            String verifyMessage = verifyCmdMessages(translatedText);
                            if(verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART") ){
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                            }
                        }
                    });
                    teamChatBuddyApplication.notifyObservers("RESTART_TRACKING");
                }
            }

            @Override
            public void onFailed(String error) throws RemoteException {
                Log.e( "No movement", error );
                teamChatBuddyApplication.notifyObservers("RESTART_TRACKING");
            }
        } );
    }
    public void CMD_HEAD(String head){

        teamChatBuddyApplication.notifyObservers("STOP_TRACKING");

        int no_speed = 30;
        int angle = Integer.parseInt( head );
        if(BuddySDK.Actuators.getYesStatus().toUpperCase().contains("DISABLE")) {
            BuddySDK.USB.enableYesMove( true, new IUsbCommadRsp.Stub() {
                @Override
                public void onSuccess(String success) throws RemoteException {
                    Log.i( "Motor Yes", "Yes motor Enabled" );
                }

                @Override
                public void onFailed(String error) throws RemoteException {
                    Log.i( "Motor Yes", "Yes motor Enabled Failed" );
                }
            } );
        }

        BuddySDK.USB.buddySayYes( no_speed, angle, new IUsbCommadRsp.Stub() {
            @Override
            public void onSuccess(String success) throws RemoteException {
                Log.i("No movement", success);
                if(success.equals("YES_MOVE_FINISHED")){
                    translate("CMD_HEAD", new ITranslationCallback() {
                        @Override
                        public void onTranslated(String translatedText) {
                            String verifyMessage = verifyCmdMessages(translatedText);
                            if(verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART") ){
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                            }
                        }
                    });
                    teamChatBuddyApplication.notifyObservers("RESTART_TRACKING");
                }
            }

            @Override
            public void onFailed(String error) throws RemoteException {
                Log.e("No movement", error);
                teamChatBuddyApplication.notifyObservers("RESTART_TRACKING");
            }
        } );
    }
    public void CMD_STOP(){
        stopMediaPlayer();
        BIPlayer.getInstance().stopBehaviour();
        teamChatBuddyApplication.setQuestionNumber(teamChatBuddyApplication.getQuestionNumber() + 1);
        teamChatBuddyApplication.setActivityClosed(true);
        teamChatBuddyApplication.setSpeaking(false);
        teamChatBuddyApplication.setStartRecording(false);
        teamChatBuddyApplication.notifyObservers("end of timer");
        translate("CMD_STOP", new ITranslationCallback() {
            @Override
            public void onTranslated(String translatedText) {
                String verifyMessage = verifyCmdMessages(translatedText);
                if(verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART") ){
                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                }
            }
        });
    }
    public void CMD_DANCE(){

        //Get biToPlay from configFile
        String biToPlay;
        StringTokenizer st = new StringTokenizer(teamChatBuddyApplication.getParamFromFile("BI_danse", configFile), "/", false);
        List<String> listBehaviour = new ArrayList<>();
        while (st.hasMoreTokens()) {
            String result = st.nextToken();
            listBehaviour.add(result.toLowerCase());
        }
        if (listBehaviour.size() > 0) {
            biToPlay = listBehaviour.get(new Random().nextInt(listBehaviour.size())).trim();
        } else {
            biToPlay = "";
        }
        Log.w(TAG,"Get random from List 'BI_danse' --> "+biToPlay);
        if (!biToPlay.contains(".xml") && !biToPlay.equals("")){
            List<String> biNames = new ArrayList<>();
            File[] biFiles = new File("/storage/emulated/0/BI/Behaviour/").listFiles();
            if (biFiles != null) {
                for (File biFile : biFiles) {
                    if (biFile.isFile() && biFile.getName().toLowerCase().endsWith(".xml")) {
                        if (biFile.getName().toLowerCase().contains(biToPlay.toLowerCase())) {
                            biNames.add(biFile.getName());
                        }
                    }
                }
            }
            if (!biNames.isEmpty()) {
                biToPlay = biNames.get(new Random().nextInt(biNames.size()));
                Log.i(TAG,"BI choisi : " + biToPlay);
            }
        }

        if(biToPlay.isEmpty()){
            Log.i(TAG,"BI introuvable, nous allons utiliser : Dance01.xml");
            biToPlay = "Dance01.xml";
        }

        teamChatBuddyApplication.setBIExecution(true);
        BIPlayer.getInstance().playBI(activity, biToPlay, new IBehaviourCallBack() {
            @Override
            public void onEnd(boolean hasAborted, String reason) {
                teamChatBuddyApplication.setBIExecution(false);
                if(reason.equals("SUCCESS")) {
                    translate("CMD_DANCE", new ITranslationCallback() {
                        @Override
                        public void onTranslated(String translatedText) {
                            String verifyMessage = verifyCmdMessages(translatedText);
                            if(verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART") ){
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                            }
                        }
                    });
                }
            }

            @Override
            public void onRun(String s) {

            }
        });
    }
    public void CMD_METEO(String city){
        Log.e(TAG, "METEO : LA VILLE EST " + city);
        Retrofit retrofit = NetworkClient.getRetrofitClient(teamChatBuddyApplication,"https://api.openweathermap.org", 30);
        ApiEndpointInterface api = retrofit.create(ApiEndpointInterface.class);
        try {
            Call call = api.getMeteoResult(city, "060ec1f9ecb0a63e0045980f1cf480b9", "fr", "metric");;
            call.enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) {
                    if(response.isSuccessful()) {
                        Log.i(TAG, "Réponse Météo [successful] :"+response.toString());
                        try {
                            JSONObject jsonObj = new JSONObject( response.body().toString() );
                            JSONArray weatherArray = jsonObj.getJSONArray("weather");
                            JSONObject weatherObject = weatherArray.getJSONObject(0);
                            String description = weatherObject.getString("description");
                            JSONObject tempObject = jsonObj.getJSONObject("main");
                            double temp = tempObject.getDouble( "temp" );
                            int temperature = (int) Math.ceil(temp);
                            Log.i(TAG, "Réponse Météo : " +"Il fait " + description + " et " + temperature + " °C");
                            translate("CMD_METEO", new ITranslationCallback() {
                                @Override
                                public void onTranslated(String translatedText) {
                                    String verifyMessage = verifyCmdMessages(translatedText);
                                    if(verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART") ){
                                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +translatedText.split("\\s*/\\s*(?:/\\s*)?")[1].replace("[1]",city).replace("[2]",description).replace( "[3]", Integer.toString( temperature )));
                                    }
                                }
                            });
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    else{
                        if (response != null && response.errorBody() != null) {
                            Log.e(TAG, "Réponse meteo [not successful] ");
                            String jsonString = null;
                            try {
                                jsonString = response.errorBody().string();
                                JSONObject jsonErrorContent = new JSONObject(jsonString);

                                String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= CMD_METEO, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                                logErrorAPIHealysa("CMD_METEO", errorTXT, "notOnFailure");
                            } catch (IOException | JSONException e) {
                                e.printStackTrace();
                                Log.e(TAG, "Réponse meteo  [not successful]1 catch" + e);
                            }
                        }
                    }
                }
                @Override
                public void onFailure(Call call, Throwable t) {
                    Log.e(TAG, "Réponse Météo [Failure] : " + t);
                    logErrorAPIHealysa("CMD_METEO",t.getMessage(),"onFailure");
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception pendant la récupération de la réponse Météo : " + e);
        }

    }
    public void CMD_RADIO(String radio){
        final String[] accessToken = new String[1];
        Log.e(TAG, "RADIO : LA RADIO EST " + radio);
        Retrofit retrofit = NetworkClient.getRetrofitClient(teamChatBuddyApplication,"https:/bluefrogrobotics-bhqtj3.api.radioline.fr", 30);
        ApiEndpointInterface api = retrofit.create(ApiEndpointInterface.class);
        JSONObject jsonParams = new JSONObject();
        try {
            jsonParams.put("client_id","rjnefu93CdsUX4xXPH7P7RmcCbYMbFRHWiCTCtbg" );
            jsonParams.put("client_key","cuEkLJWVWp9Ee4oFvL9nfWdqWvwXddbaYWAbpWJd34FAEvy4fAUjUzaLCeUbJooE9dwf4bePg4qFsyps9HtwruNnaqhWv9KjKHnqvENALTmqMqJzrefNWyeVhRkqkvxK");
            jsonParams.put("device_serial","device_unique_serial_number");
            RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), jsonParams.toString());
            Call call = api.getRadioToken(body, "MGdbCYLTMspN84n4QMZOIAhrLcyTBTAcPEc9feb7xe05oBnQoDwOyLMmsHAppZVI");
            call.enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) {
                    if(response.isSuccessful()) {
                        Log.i(TAG, "Réponse Radio Token [successful] :"+response.toString());
                        try {
                            JSONObject jsonObj = new JSONObject( response.body().toString() );
                            JSONObject dataObject = jsonObj.getJSONObject("data");
                            JSONObject accessTokenObject = dataObject.getJSONObject("access_token");
                            accessToken[0] = accessTokenObject.getString("key");
                            Log.i(TAG, "Réponse Radio Token : " +"Token : " + accessToken[0]);
                            Log.i(TAG, "Radio : "  + radio );
                            Call callRadioName = api.getRadioName(1,30,radio.trim(), "Bearer "+ accessToken[0]);
                            callRadioName.enqueue( new Callback() {
                                @Override
                                public void onResponse(Call call, Response response) {
                                    if(response.isSuccessful()){
                                        Log.i(TAG, "Réponse Radio [successful] :"+response.toString());
                                        try {
                                            JSONObject jsonObject = new JSONObject( response.body().toString() );
                                            JSONObject bodyObject = jsonObject.getJSONObject( "body" );
                                            JSONArray contentObject = bodyObject.getJSONArray( "content" );
                                            JSONObject contentArray = contentObject.getJSONObject( 0 );
                                            String permalink = contentArray.getString("permalink");
                                            Log.i(TAG, "Radio Name : "  + permalink );
                                            Call callRadio = api.getRadioResponse(permalink.split("/")[1], "Bearer "+ accessToken[0]);
                                            callRadio.enqueue(new Callback() {
                                                @Override
                                                public void onResponse(Call call, Response response) {
                                                    if(response.isSuccessful()){
                                                        Log.i(TAG, "Réponse Radio [successful] :"+response.toString());
                                                        try {
                                                            JSONObject jsonObject = new JSONObject( response.body().toString() );
                                                            JSONObject bodyObject = jsonObject.getJSONObject( "body" );
                                                            JSONObject contentObject = bodyObject.getJSONObject( "content" );
                                                            JSONArray streamsArray = contentObject.getJSONArray( "streams" );
                                                            JSONObject streamsObject = streamsArray.getJSONObject( 0 );
                                                            String url = streamsObject.getString( "url" );
                                                            Log.i(TAG, "Réponse Radio : " +"URL : " + url );
                                                            playRadio( url );
                                                        } catch (JSONException e) {
                                                            throw new RuntimeException( e );
                                                        }
                                                    }
                                                    else{
                                                        if (response != null && response.errorBody() != null) {
                                                            Log.e(TAG, "Réponse Radio [not successful]  ");
                                                            String jsonString = null;
                                                            try {
                                                                jsonString = response.errorBody().string();
                                                                JSONObject jsonErrorContent = new JSONObject(jsonString);

                                                                String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= CMD_RADIO, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                                                                logErrorAPIHealysa("CMD_RADIO", errorTXT, "notOnFailure");
                                                            } catch (IOException | JSONException e) {
                                                                e.printStackTrace();
                                                                Log.e(TAG, "Réponse Radio [not successful]1 catch" + e);
                                                            }
                                                        }
                                                    }
                                                }

                                                @Override
                                                public void onFailure(Call call, Throwable t) {
                                                    logErrorAPIHealysa("CMD_RADIO",t.getMessage(),"onFailure");
                                                }
                                            });
                                        } catch (JSONException e) {
                                            Log.i(TAG, "Réponse Radio ERROR " + e );
                                        }
                                    }
                                    else{


                                        if (response != null && response.errorBody() != null) {
                                            Log.e(TAG, "Réponse Radio [not successful]  ");
                                            String jsonString = null;
                                            try {
                                                jsonString = response.errorBody().string();
                                                JSONObject jsonErrorContent = new JSONObject(jsonString);

                                                String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= CMD_RADIO, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                                                logErrorAPIHealysa("CMD_RADIO", errorTXT, "notOnFailure");
                                            } catch (IOException | JSONException e) {
                                                e.printStackTrace();
                                                Log.e(TAG, "Réponse Radio [not successful]1 catch" + e);
                                            }
                                        }

                                    }
                                }

                                @Override
                                public void onFailure(Call call, Throwable t) {
                                    Log.i(TAG, "Réponse Radio onFailure " + t);
                                    logErrorAPIHealysa("CMD_RADIO",t.getMessage(),"onFailure");
                                }
                            } );
                        } catch (JSONException e) {
                            Log.i(TAG, "Réponse Radio ERROR C " + e );
                            e.printStackTrace();
                        }
                    }
                    else{
                        if (response != null && response.errorBody() != null) {
                            Log.e(TAG, "Réponse Radio Token [not successful]  ");
                            String jsonString = null;
                            try {
                                jsonString = response.errorBody().string();
                                JSONObject jsonErrorContent = new JSONObject(jsonString);

                                String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= CMD_RADIO, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                                logErrorAPIHealysa("CMD_RADIO", errorTXT, "notOnFailure");
                            } catch (IOException | JSONException e) {
                                e.printStackTrace();
                                Log.e(TAG, "Réponse Radio [not successful]1 catch" + e);
                            }
                        }
                    }
                }
                @Override
                public void onFailure(Call call, Throwable t) {
                    Log.e(TAG, "Réponse Radio Token [Failure] : " + t);
                    logErrorAPIHealysa("CMD_RADIO",t.getMessage(),"onFailure");
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception pendant la récupération de la réponse Radio : " + e);
        }
    }
    public void CMD_BI(String bi){
        Log.e("TeamChat_BIPlayer", "BI : Le BI EST " + bi);
        final String[] biName = {""};
        teamChatBuddyApplication.setBIExecution(true);
        getTheRightBINameFromConfigFile(bi, new BINameCallback() {
            @Override
            public void onResult(String result) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (result.equals("ERROR")){
                            biName[0] = bi;
                        }
                        else {
                            biName[0] = result;
                        }
                        Log.e("TeamChat_BIPlayer", "BI : Le BI envoyé à la fonction playBI EST " + biName[0]);
                        BIPlayer.getInstance().playBI(activity, biName[0], new IBehaviourCallBack() {
                            @Override
                            public void onEnd(boolean hasAborted, String reason) {
                                teamChatBuddyApplication.setBIExecution(false);
                                if(reason.equals("SUCCESS")) {
                                    translate("CMD_BI", new ITranslationCallback() {
                                        @Override
                                        public void onTranslated(String translatedText) {
                                            String verifyMessage = verifyCmdMessages(translatedText);
                                            if(verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART") ){
                                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                            }
                                        }
                                    });
                                }
                                else if (reason.equals("ERROR_TASK")){
                                    translate("CMD_BI", new ITranslationCallback() {
                                        @Override
                                        public void onTranslated(String translatedText) {
                                            String verifyMessage = verifyCmdMessages(translatedText);
                                            if(verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART") ){
                                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +translatedText.split("\\s*/\\s*(?:/\\s*)?")[2].replace("[1]", biName[0]));
                                            }
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onRun(String s) {

                            }
                        });
                    }
                });
            }
        });


    }
    public interface BINameCallback {
        void onResult(String result);
    }
    private void getTheRightBINameFromConfigFile(String biName,BINameCallback biNameCallback){
        final String[] nomBIAfterTraduction = {""};
        // Chemin du dossier dans le stockage externe
        String dossierExterne = activity.getString(R.string.path) + "/BI/Behaviour";
        // Chemin complet du fichier
        String cheminFichier = dossierExterne + "/" + biName;

        // Création d'un objet File avec le chemin du fichier
        File fichier = new File(cheminFichier);
        if (!biName.contains(".xml") && !biName.equals("")){

            // Récupération des noms de fichiers XML correspondant à la catégorie recherchée
            List<String> nomsFichiers = getFilenamesForCategory(dossierExterne, biName.trim());

            // Choix aléatoire d'un nom de fichier parmi ceux trouvés
            if (!nomsFichiers.isEmpty()) {
                biName = nomsFichiers.get(new Random().nextInt(nomsFichiers.size()));
                Log.e("TeamChat_BIPlayer","Nom du fichier choisi : " + biName);
                biNameCallback.onResult(biName);
            } else {
                Log.e("TeamChat_BIPlayer","Aucun fichier trouvé pour la catégorie : " + biName);

                teamChatBuddyApplication.getLanguageSelectedEnglishTranslator().translate(biName)
                        .addOnSuccessListener(new OnSuccessListener<String>() {
                            @Override
                            public void onSuccess(String translatedText) {
                                Log.e("TeamChat_BIPlayer","Nom du fichier traduit: " + translatedText);
                                List<String> nomsFichiers1 = getFilenamesForCategory(dossierExterne, translatedText.trim());
                                if (!nomsFichiers1.isEmpty()) {
                                    translatedText = nomsFichiers1.get(new Random().nextInt(nomsFichiers1.size()));
                                    nomBIAfterTraduction[0] =translatedText;
                                    Log.e("TeamChat_BIPlayer","Nom du fichier choisi après traduction: " + translatedText);
                                    biNameCallback.onResult(translatedText);
                                }
                                else {
                                    Log.e("TeamChat_BIPlayer","Aucun fichier trouvé après traduction pour la catégorie : " + translatedText);
                                    biNameCallback.onResult(translatedText);
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                biNameCallback.onResult("ERROR");
                            }
                        });

            }

        }
        else  {


            biNameCallback.onResult(biName);
        }

    }
    private static List<String> getFilenamesForCategory(String dossier, String categorieRecherchee) {
        List<String> nomsFichiers = new ArrayList<>();
        File dossierBehaviours = new File(dossier);
        Log.e("TeamChat_BIPlayer","Check file NAME : categorieRecherchee.replaceAll(\"\\\\s+\", \"\").toLowerCase()) ="+categorieRecherchee.replaceAll("\\s+", "").toLowerCase() );
        // Liste des fichiers dans le dossier
        File[] fichiers = dossierBehaviours.listFiles();

        if (fichiers != null) {
            // Parcours des fichiers
            for (File fichier : fichiers) {
                if (fichier.isFile() && fichier.getName().toLowerCase().endsWith(".xml")) {
                    // Ajout du nom du fichier si la catégorie recherchée est présente dans le nom du fichier
                    if (fichier.getName().toLowerCase().contains(categorieRecherchee.replaceAll("\\s+", "").toLowerCase())) {
                        nomsFichiers.add(fichier.getName());
                    }
                }
            }
        }

        return nomsFichiers;
    }
    public void HEALYSA_CONNECT(String utilisateur){
        Log.e(TAG,"--RDA_CONNEXION---"+utilisateur+"---");
        Retrofit retrofit = NetworkClient.getRetrofitClient( teamChatBuddyApplication,teamChatBuddyApplication.getParamFromFile("Healysa_URL_PROD",configFile), 30 );
        ApiEndpointInterface api = retrofit.create(ApiEndpointInterface.class);
        JSONObject jsonParams = new JSONObject();
        try {
            jsonParams.put("email",teamChatBuddyApplication.getParamFromFile("Healysa_mail",configFile) );
            jsonParams.put("password",teamChatBuddyApplication.getParamFromFile("Healysa_password",configFile));
            RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), jsonParams.toString());
            Call<JsonObject> callAuth = api.getTokenHealysa(body);
            callAuth.enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) {
                    if(response.isSuccessful()) {
                        Log.i(TAG, "Réponse Auth Healysa [successful] :"+response.body().toString());
                        try {
                            JSONObject jsonObj = new JSONObject( response.body().toString() );
                            teamChatBuddyApplication.setTokenHealysa(jsonObj.getString( "token" ));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        Call<JsonObject> callIMEI = api.getIMEIHealya( "Bearer "+ teamChatBuddyApplication.getTokenHealysa() );

                        callIMEI.enqueue( new Callback<JsonObject>() {
                            @Override
                            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                                if(response.isSuccessful()){
                                    try {
                                        JSONObject jsonObj = new JSONObject( response.body().toString() );
                                        JSONArray devices = jsonObj.getJSONArray( "devices" );
                                        Boolean findConsumer =false;
                                        for (int i=0; i < devices.length(); i++) {
                                            Log.i(TAG, "Réponse Consumer Firstname Healysa [successful] :"+ devices.getJSONObject( i ).getJSONObject( "consumer" ).getString( "firstname" ));
                                            if(devices.getJSONObject( i ).getJSONObject( "consumer" ).getString( "firstname" ).toLowerCase().equals( utilisateur.toLowerCase() )) {
                                                findConsumer =true;
                                                teamChatBuddyApplication.setImeiDevice(devices.getJSONObject( i ).getString( "imei" ));
                                                Log.i(TAG, "Réponse Imei Healysa [successful] :"+teamChatBuddyApplication.getImeiDevice());
                                                translate("HEALYSA_CONNECT", new ITranslationCallback() {
                                                    @Override
                                                    public void onTranslated(String translatedText) {
                                                        String verifyMessage = verifyCmdMessages(translatedText);
                                                        if(verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART") ){
                                                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                                        }
                                                    }
                                                });
                                            }
                                        }
                                        if (!findConsumer){
                                            logErrorAPIHealysa("HEALYSA_CONNECT","Consumer not found","onFailure");
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                                else{
                                    if (response != null && response.errorBody() != null) {
                                        Log.e(TAG, "Réponse Imei Healysa [not successful] ");
                                        String jsonString = null;
                                        try {
                                            jsonString = response.errorBody().string();
                                            JSONObject jsonErrorContent = new JSONObject(jsonString);

                                            String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= HEALYSA_CONNECT, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                                            logErrorAPIHealysa("HEALYSA_CONNECT", errorTXT, "notOnFailure");
                                        } catch (IOException | JSONException e) {
                                            e.printStackTrace();
                                            Log.e(TAG, "Réponse Imei Healysa [not successful]1 catch" + e);
                                        }
                                    }
                                    Log.e(TAG, "Réponse Imei Healysa [not successful]");
                                }
                            }

                            @Override
                            public void onFailure(Call<JsonObject> call, Throwable throwable) {
                                Log.e(TAG, "Réponse Imei Healysa [Failure] : " + throwable);
                                logErrorAPIHealysa("HEALYSA_CONNECT",throwable.getMessage(),"onFailure");
                            }
                        } );

                    }
                    else{
                        Log.e(TAG, "Réponse Auth Healysa [not successful]");
                        if (response != null && response.errorBody() != null) {
                            Log.e(TAG, "Réponse Auth Healysa  [not successful] ");
                            String jsonString = null;
                            try {
                                jsonString = response.errorBody().string();
                                JSONObject jsonErrorContent = new JSONObject(jsonString);

                                String errorTXT = new Date().toString() +  ", COMMANDERRORAPI, Commande= HEALYSA_CONNECT, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                                logErrorAPIHealysa("HEALYSA_CONNECT", errorTXT, "notOnFailure");
                            } catch (IOException | JSONException e) {
                                e.printStackTrace();
                                Log.e(TAG, "Réponse Auth Healysa  [not successful]1 catch" + e);
                            }
                        }
                    }
                }
                @Override
                public void onFailure(Call call, Throwable t) {
                    Log.e(TAG, "Réponse Auth Healysa [Failure] : " + t);
                    logErrorAPIHealysa("HEALYSA_CONNECT",t.getMessage(),"onFailure");
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Exception pendant la récupération de la réponse Healysa : " + e);
        }
    }
    public void HEALYSA_HRV(){
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        Retrofit retrofit = NetworkClient.getRetrofitClient( teamChatBuddyApplication,teamChatBuddyApplication.getParamFromFile("Healysa_URL_PROD",configFile) , 30);
        ApiEndpointInterface api = retrofit.create(ApiEndpointInterface.class);
        Call<String> callHR = api.runCmdHealysa( "silver",teamChatBuddyApplication.getImeiDevice(),"IWBPXL,"+teamChatBuddyApplication.getImeiDevice()+",080835#", "Bearer " + teamChatBuddyApplication.getTokenHealysa() );
        callHR.enqueue( new Callback() {
            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()){
                    Log.i(TAG, "Réponse Fréquence Cardiaque Healysa [successful] :"+response.body().toString());
                    SystemClock.sleep(35000);
                    Log.i(TAG, "Réponse Fréquence Cardiaque Healysa [successful] : 30 secondes plus tard");

                    Call<JsonObject> callGetHR = api.getDataHealysa(teamChatBuddyApplication.getImeiDevice(), date + "T00:00:00.000Z", date + "T23:59:59.000Z", "HEART_RATE", "day", "Bearer " + teamChatBuddyApplication.getTokenHealysa());
                    callGetHR.enqueue( new Callback() {
                        @Override
                        public void onResponse(Call call, Response response) {
                            if (response.isSuccessful()) {
                                Log.i( TAG, "Réponse GET Fréquence Cardiaque Healysa [successful] :" + response.body().toString() );
                                try {
                                    JSONObject reponse = new JSONObject( response.body().toString() );
                                    JSONArray array = new JSONArray( reponse.getString( "HEART_RATE" ) );
                                    JSONObject data = array.getJSONObject( 0 );
                                    String heart_rate = data.getString( "dataValue" );
                                    Log.i( TAG, "Réponse GET Fréquence Cardiaque Healysa [successful] : " + heart_rate );
                                    translate("HEALYSA_HRV", new ITranslationCallback() {
                                        @Override
                                        public void onTranslated(String translatedText) {
                                            String verifyMessage = verifyCmdMessages(translatedText);
                                            if(verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART") ){
                                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +translatedText.split("\\s*/\\s*(?:/\\s*)?")[1].replace("[1]",heart_rate));
                                            }
                                        }
                                    });
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            else {
                                if (response != null && response.errorBody() != null) {
                                    Log.e(TAG, "Réponse Imei Healysa [not successful] response code " + response.code() + "response.body " + response.errorBody());
                                    String jsonString = null;
                                    try {
                                        jsonString = response.errorBody().string();
                                        JSONObject jsonErrorContent = new JSONObject(jsonString);
                                        Log.e(TAG, "Réponse Fréquence Cardiaque Healysa [not successful]1 jsonErrorContent " + jsonErrorContent);

                                        String errorTXT = new Date().toString() +  ", COMMANDERRORAPI, Commande= HEALYSA_HRV, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                                        logErrorAPIHealysa("HEALYSA_HRV", errorTXT, "notOnFailure");
                                    } catch (IOException | JSONException e) {
                                        e.printStackTrace();
                                        Log.e(TAG, "Réponse Fréquence Cardiaque Healysa [not successful]1 catch" + e);
                                    }
                                }
                                Log.e(TAG, "Réponse GET Fréquence Cardiaque Healysa [not successful]");
                            }
                        }

                        @Override
                        public void onFailure(Call call, Throwable throwable) {
                            Log.e(TAG, "Réponse GET Fréquence Cardiaque Healysa [not successful] :"+throwable);
                            logErrorAPIHealysa("HEALYSA_HRV",throwable.getMessage(),"onFailure");
                        }
                    } );
                }
                else{
                    String text = "Génère moi une phrase pour dire que je ne suis pas connecté à la plateforme Healysa";
                    Log.e(TAG, "Réponse Fréquence Cardiaque Healysa [not successful]");
                    if (response != null && response.errorBody() != null) {
                        Log.e(TAG, "Réponse Imei Healysa [not successful] response code " + response.code() + "response.body " + response.errorBody());
                        String jsonString = null;
                        try {
                            jsonString = response.errorBody().string();
                            JSONObject jsonErrorContent = new JSONObject(jsonString);
                            Log.e(TAG, "Réponse Fréquence Cardiaque Healysa [not successful]1 jsonErrorContent " + jsonErrorContent);

                            String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= HEALYSA_HRV, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                            logErrorAPIHealysa("HEALYSA_HRV", errorTXT, "notOnFailure");
                        } catch (IOException | JSONException e) {
                            e.printStackTrace();
                            Log.e(TAG, "Réponse Fréquence Cardiaque Healysa [not successful]1 catch" + e);
                        }
                    }
                }
            }
            @Override
            public void onFailure(Call call, Throwable throwable) {
                Log.e(TAG, "Réponse Fréquence Cardiaque Healysa [not successful] :"+ throwable);
                logErrorAPIHealysa("HEALYSA_HRV",throwable.getMessage(),"onFailure");
            }
        } );
    }
    public void HEALYSA_BLOODP(){
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        Retrofit retrofit = NetworkClient.getRetrofitClient( teamChatBuddyApplication,teamChatBuddyApplication.getParamFromFile("Healysa_URL_PROD",configFile) , 30);
        ApiEndpointInterface api = retrofit.create(ApiEndpointInterface.class);
        Log.i(TAG, "Tension Healysa imei:"+teamChatBuddyApplication.getImeiDevice());
        Call<String> callHR = api.runCmdHealysa( "silver",teamChatBuddyApplication.getImeiDevice(),"IWBPXY,"+teamChatBuddyApplication.getImeiDevice()+",080835#", "Bearer " + teamChatBuddyApplication.getTokenHealysa() );
        callHR.enqueue( new Callback() {
            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()){
                    Log.i(TAG, "Réponse Tension Healysa [successful] :"+response.body().toString());
                    SystemClock.sleep(35000);
                    Log.i(TAG, "Réponse Tension Healysa [successful] : 30 secondes plus tard");

                    Call<JsonObject> callGetTensionS = api.getDataHealysa(teamChatBuddyApplication.getImeiDevice(), date + "T00:00:00.000Z", date + "T23:59:59.000Z", "BLOOD_PRESSURE_SYSTOLIC", "day", "Bearer " + teamChatBuddyApplication.getTokenHealysa());
                    callGetTensionS.enqueue( new Callback() {
                        @Override
                        public void onResponse(Call call, Response response) {
                            if (response.isSuccessful()){
                                Log.i(TAG, "Réponse GET Tension Healysa [successful] :" + response.body().toString());
                                try {
                                    JSONObject reponse = new JSONObject(response.body().toString());
                                    JSONArray array = new JSONArray(reponse.getString("BLOOD_PRESSURE_SYSTOLIC"));
                                    JSONObject data = array.getJSONObject(0);
                                    tensionS = data.getString("dataValue");
                                    Log.i(TAG, "Réponse GET Tension Healysa [successful] : " + tensionS);
                                    Call<JsonObject> callGetTensionD = api.getDataHealysa(teamChatBuddyApplication.getImeiDevice(), date + "T00:00:00.000Z", date + "T23:59:59.000Z", "BLOOD_PRESSURE_DIASTOLIC", "day", "Bearer " + teamChatBuddyApplication.getTokenHealysa());
                                    callGetTensionD.enqueue(new Callback() {
                                        @Override
                                        public void onResponse(Call call, Response response) {
                                            Log.i(TAG, "Réponse GET Tension Healysa [successful] :" + response.body().toString());
                                            try {
                                                JSONObject reponse = new JSONObject(response.body().toString());
                                                JSONArray array = new JSONArray(reponse.getString("BLOOD_PRESSURE_DIASTOLIC"));
                                                JSONObject data = array.getJSONObject(0);
                                                tensionD = data.getString("dataValue");
                                                Log.i(TAG, "Réponse GET Tension Healysa [successful] : " + tensionD);
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                            Log.i(TAG, "Réponse GET Tension Healysa [successful] : " + tensionS + tensionD);
                                            translate("HEALYSA_BLOODP", new ITranslationCallback() {
                                                @Override
                                                public void onTranslated(String translatedText) {
                                                    String verifyMessage = verifyCmdMessages(translatedText);
                                                    if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                                        teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1].replace("[1]", tensionS).replace("[2]", tensionD));
                                                    }
                                                }
                                            });
                                        }

                                        @Override
                                        public void onFailure(Call call, Throwable throwable) {
                                            Log.e(TAG, "Réponse GET Tension Healysa [not successful] :" + throwable);
                                            logErrorAPIHealysa("HEALYSA_BLOODP", throwable.getMessage(), "onFailure");
                                        }
                                    });
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            else {
                                if (response != null && response.errorBody() != null) {
                                    Log.e(TAG, "Réponse Tension Healysa [not successful]");
                                    String jsonString = null;
                                    try {
                                        jsonString = response.errorBody().string();
                                        JSONObject jsonErrorContent = new JSONObject(jsonString);

                                        String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= HEALYSA_BLOODP, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                                        logErrorAPIHealysa("HEALYSA_BLOODP", errorTXT, "notOnFailure");
                                    } catch (IOException | JSONException e) {
                                        e.printStackTrace();
                                        Log.e(TAG, "Réponse Tension Healysa[not successful]1 catch" + e);
                                    }
                                }
                                Log.e(TAG, "Réponse Tension Healysa [not successful]");
                            }
                        }

                        @Override
                        public void onFailure(Call call, Throwable throwable) {
                            Log.e(TAG, "Réponse GET Tension Healysa [not successful] :"+throwable);
                            logErrorAPIHealysa("HEALYSA_BLOODP",throwable.getMessage(),"onFailure");
                        }
                    } );

                }
                else{
                    String text = "Génère moi une phrase pour dire que je ne suis pas connecté à la plateforme Healysa";
                    if (response != null && response.errorBody() != null) {
                        Log.e(TAG, "Réponse Tension Healysa [not successful]");
                        String jsonString = null;
                        try {
                            jsonString = response.errorBody().string();
                            JSONObject jsonErrorContent = new JSONObject(jsonString);

                            String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= HEALYSA_BLOODP, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                            logErrorAPIHealysa("HEALYSA_BLOODP", errorTXT, "notOnFailure");
                        } catch (IOException | JSONException e) {
                            e.printStackTrace();
                            Log.e(TAG, "Réponse Tension Healysa[not successful]1 catch" + e);
                        }
                    }
                    Log.e(TAG, "Réponse Tension Healysa [not successful]");
                }
            }

            @Override
            public void onFailure(Call call, Throwable throwable) {
                Log.e(TAG, "Réponse Tension Healysa [not successful] :"+ throwable);
                logErrorAPIHealysa("HEALYSA_BLOODP",throwable.getMessage(),"onFailure");
            }
        } );
    }
    public void HEALYSA_SPO2(){
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        Retrofit retrofit = NetworkClient.getRetrofitClient( teamChatBuddyApplication,teamChatBuddyApplication.getParamFromFile("Healysa_URL_PROD",configFile), 30 );
        ApiEndpointInterface api = retrofit.create(ApiEndpointInterface.class);
        Call<String> callHR = api.runCmdHealysa( "silver",teamChatBuddyApplication.getImeiDevice(),"IWBPXZ,"+teamChatBuddyApplication.getImeiDevice()+",080835#", "Bearer " + teamChatBuddyApplication.getTokenHealysa() );
        callHR.enqueue( new Callback() {
            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()){
                    Log.i(TAG, "Réponse SPO2 Healysa [successful] :"+response.body().toString());
                    SystemClock.sleep(35000);
                    Log.i(TAG, "Réponse SPO2 Healysa [successful] : 30 secondes plus tard");

                    Call<JsonObject> callGetHR = api.getDataHealysa(teamChatBuddyApplication.getImeiDevice(), date + "T00:00:00.000Z", date + "T23:59:59.000Z", "SPO2", "day", "Bearer " + teamChatBuddyApplication.getTokenHealysa());
                    callGetHR.enqueue( new Callback() {
                        @Override
                        public void onResponse(Call call, Response response) {
                            if (response.isSuccessful()) {
                                Log.i(TAG, "Réponse GET SPO2 Healysa [successful] :" + response.body().toString());
                                try {
                                    JSONObject reponse = new JSONObject(response.body().toString());
                                    JSONArray array = new JSONArray(reponse.getString("SPO2"));
                                    JSONObject data = array.getJSONObject(0);
                                    String spo2 = data.getString("dataValue");
                                    Log.i(TAG, "Réponse GET SPO2 Healysa [successful] : " + spo2);
                                    translate("HEALYSA_SPO2", new ITranslationCallback() {
                                        @Override
                                        public void onTranslated(String translatedText) {
                                            String verifyMessage = verifyCmdMessages(translatedText);
                                            if (verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART")) {
                                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" + translatedText.split("\\s*/\\s*(?:/\\s*)?")[1].replace("[1]", spo2));
                                            }
                                        }
                                    });
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }else {
                                if (response != null && response.errorBody() != null) {
                                    Log.e(TAG, "Réponse SPO2 Healysa [not successful]");
                                    String jsonString = null;
                                    try {
                                        jsonString = response.errorBody().string();
                                        JSONObject jsonErrorContent = new JSONObject(jsonString);

                                        String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= HEALYSA_SPO2, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                                        logErrorAPIHealysa("HEALYSA_SPO2", errorTXT, "notOnFailure");
                                    } catch (IOException | JSONException e) {
                                        e.printStackTrace();
                                        Log.e(TAG, "Réponse SPO2 Healysa [not successful]1 catch" + e);
                                    }
                                }
                                Log.e(TAG, "Réponse SPO2 Healysa [not successful]");
                            }
                        }

                        @Override
                        public void onFailure(Call call, Throwable throwable) {
                            Log.e(TAG, "Réponse GET SPO2 Healysa [not successful] :"+throwable);
                            logErrorAPIHealysa("HEALYSA_SPO2",throwable.getMessage(),"onFailure");
                        }
                    } );
                }
                else{
                    String text = "Génère moi une phrase pour dire que je ne suis pas connecté à la plateforme Healysa";
                    if (response != null && response.errorBody() != null) {
                        Log.e(TAG, "Réponse SPO2 Healysa [not successful]");
                        String jsonString = null;
                        try {
                            jsonString = response.errorBody().string();
                            JSONObject jsonErrorContent = new JSONObject(jsonString);

                            String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= HEALYSA_SPO2, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                            logErrorAPIHealysa("HEALYSA_SPO2", errorTXT, "notOnFailure");
                        } catch (IOException | JSONException e) {
                            e.printStackTrace();
                            Log.e(TAG, "Réponse SPO2 Healysa [not successful]1 catch" + e);
                        }
                    }
                    Log.e(TAG, "Réponse SPO2 Healysa [not successful]");
                }
            }

            @Override
            public void onFailure(Call call, Throwable throwable) {
                Log.e(TAG, "Réponse SPO2 Healysa [not successful] :"+ throwable);
                logErrorAPIHealysa("HEALYSA_SPO2",throwable.getMessage(),"onFailure");
            }
        } );
    }
    public void HEALYSA_CHECKUP(){
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        Retrofit retrofit = NetworkClient.getRetrofitClient( teamChatBuddyApplication,teamChatBuddyApplication.getParamFromFile("Healysa_URL_PROD",configFile) , 30);
        ApiEndpointInterface api = retrofit.create(ApiEndpointInterface.class);
        Call<String> callHR = api.runCmdHealysa( "silver",teamChatBuddyApplication.getImeiDevice(),"IWBPXZ,"+teamChatBuddyApplication.getImeiDevice()+",080835#", "Bearer " + teamChatBuddyApplication.getTokenHealysa() );
        callHR.enqueue( new Callback() {
            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()){
                    Log.i(TAG, "Réponse SANTE Healysa [successful] :"+response.body().toString());
                    SystemClock.sleep(35000);
                    Log.i(TAG, "Réponse SANTE Healysa [successful] : 30 secondes plus tard");


                    Call<JsonObject> callGetHR = api.getDataHealysa(teamChatBuddyApplication.getImeiDevice(), date + "T00:00:00.000Z", date + "T23:59:59.000Z", "HEART_RATE", "day", "Bearer " + teamChatBuddyApplication.getTokenHealysa());
                    callGetHR.enqueue( new Callback() {
                        @Override
                        public void onResponse(Call call, Response response) {
                            if (response.isSuccessful()) {
                                Log.i( TAG, "Réponse GET Fréquence Cardiaque Healysa [successful] :" + response.body().toString() );
                                try {
                                    JSONObject reponse = new JSONObject( response.body().toString() );
                                    JSONArray array = new JSONArray( reponse.getString( "HEART_RATE" ) );
                                    JSONObject data = array.getJSONObject( 0 );
                                    heart_rate = data.getString( "dataValue" );
                                    Log.i( TAG, "Réponse GET Fréquence Cardiaque Healysa [successful] : " + heart_rate );
                                    Call<JsonObject> callGetTension1 = api.getDataHealysa(teamChatBuddyApplication.getImeiDevice(), date + "T00:00:00.000Z", date + "T23:59:59.000Z", "BLOOD_PRESSURE_SYSTOLIC", "day", "Bearer " + teamChatBuddyApplication.getTokenHealysa());
                                    callGetTension1.enqueue( new Callback() {
                                        @Override
                                        public void onResponse(Call call, Response response) {
                                            Log.i(TAG, "Réponse GET Tension Healysa [successful] :"+response.body().toString());
                                            try{
                                                JSONObject reponse = new JSONObject(response.body().toString());
                                                JSONArray array = new JSONArray(reponse.getString( "BLOOD_PRESSURE_SYSTOLIC" ));
                                                JSONObject data = array.getJSONObject( 0 );
                                                tensionS = data.getString( "dataValue" );
                                                Log.i(TAG, "Réponse GET Tension Healysa [successful] : "+ tensionS);
                                                Call<JsonObject> callGetTension2 = api.getDataHealysa(teamChatBuddyApplication.getImeiDevice(), date + "T00:00:00.000Z", date + "T23:59:59.000Z", "BLOOD_PRESSURE_DIASTOLIC", "day", "Bearer " + teamChatBuddyApplication.getTokenHealysa());
                                                callGetTension2.enqueue( new Callback() {
                                                    @Override
                                                    public void onResponse(Call call, Response response) {
                                                        Log.i(TAG, "Réponse GET Tension Healysa [successful] :"+response.body().toString());
                                                        try{
                                                            JSONObject reponse = new JSONObject(response.body().toString());
                                                            JSONArray array = new JSONArray(reponse.getString( "BLOOD_PRESSURE_DIASTOLIC" ));
                                                            JSONObject data = array.getJSONObject( 0 );
                                                            tensionD = data.getString( "dataValue" );
                                                            Log.i(TAG, "Réponse GET Tension Healysa [successful] : "+ tensionD);
                                                            Call<JsonObject> callGetSPO2 = api.getDataHealysa(teamChatBuddyApplication.getImeiDevice(), date + "T00:00:00.000Z", date + "T23:59:59.000Z", "SPO2", "day", "Bearer " + teamChatBuddyApplication.getTokenHealysa());
                                                            callGetSPO2.enqueue( new Callback() {
                                                                @Override
                                                                public void onResponse(Call call, Response response) {
                                                                    Log.i(TAG, "Réponse GET SPO2 Healysa [successful] :"+response.body().toString());
                                                                    try{
                                                                        JSONObject reponse = new JSONObject(response.body().toString());
                                                                        JSONArray array = new JSONArray(reponse.getString( "SPO2" ));
                                                                        JSONObject data = array.getJSONObject( 0 );
                                                                        String spo2 = data.getString( "dataValue" );
                                                                        Log.i(TAG, "Réponse GET SPO2 Healysa [successful] : "+ spo2);
                                                                        translate("HEALYSA_CHECKUP", new ITranslationCallback() {
                                                                            @Override
                                                                            public void onTranslated(String translatedText) {
                                                                                String verifyMessage = verifyCmdMessages(translatedText);
                                                                                if(verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART") ){
                                                                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +translatedText.split("\\s*/\\s*(?:/\\s*)?")[1].replace("[1]",heart_rate).replace("[2]",tensionD).replace("[3]",tensionS).replace("[4]",spo2));
                                                                                }
                                                                            }
                                                                        });
                                                                    }
                                                                    catch (JSONException e){
                                                                        e.printStackTrace();
                                                                    }
                                                                }
                                                                @Override
                                                                public void onFailure(Call call, Throwable throwable) {
                                                                    Log.e(TAG, "Réponse GET SPO2 Healysa [not successful] :"+throwable);
                                                                    logErrorAPIHealysa("HEALYSA_CHECKUP",throwable.getMessage(),"onFailure");
                                                                }
                                                            } );
                                                        }
                                                        catch (JSONException e){
                                                            e.printStackTrace();
                                                        }
                                                    }
                                                    @Override
                                                    public void onFailure(Call call, Throwable throwable) {
                                                        Log.e(TAG, "Réponse GET Tension Healysa [not successful] :"+throwable);
                                                        logErrorAPIHealysa("HEALYSA_CHECKUP",throwable.getMessage(),"onFailure");
                                                    }
                                                } );
                                            }
                                            catch (JSONException e){
                                                e.printStackTrace();
                                            }
                                        }
                                        @Override
                                        public void onFailure(Call call, Throwable throwable) {
                                            Log.e(TAG, "Réponse GET Tension Healysa [not successful] :"+throwable);
                                            logErrorAPIHealysa("HEALYSA_CHECKUP",throwable.getMessage(),"onFailure");
                                        }
                                    } );
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            else {
                                Log.e(TAG, "Réponse GET Fréquence Cardiaque Healysa [not successful]");
                                if (response != null && response.errorBody() != null) {
                                    Log.e(TAG, "Réponse Imei Healysa [not successful] response code " + response.code() + "response.body " + response.errorBody());
                                    String jsonString = null;
                                    try {
                                        jsonString = response.errorBody().string();
                                        JSONObject jsonErrorContent = new JSONObject(jsonString);
                                        Log.e(TAG, "Réponse Fréquence Cardiaque Healysa [not successful]1 jsonErrorContent " + jsonErrorContent);

                                        String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= HEALYSA_CHECKUP, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                                        logErrorAPIHealysa("HEALYSA_CHECKUP", errorTXT, "notOnFailure");
                                    } catch (IOException | JSONException e) {
                                        e.printStackTrace();
                                        Log.e(TAG, "Réponse Fréquence Cardiaque Healysa [not successful]1 catch" + e);
                                    }
                                }
                            }
                        }
                        @Override
                        public void onFailure(Call call, Throwable throwable) {
                            Log.e(TAG, "Réponse GET Fréquence Cardiaque Healysa [not successful] :"+throwable);
                            logErrorAPIHealysa("HEALYSA_CHECKUP",throwable.getMessage(),"onFailure");
                        }
                    } );
                }
                else{
                    String text = "Génère moi une phrase pour dire que je ne suis pas connecté à la plateforme Healysa";
                    Log.e(TAG, "Réponse Santé Healysa [not successful]");
                    if (response != null && response.errorBody() != null) {
                        Log.e(TAG, "Réponse Santé Healysa [not successful]");
                        String jsonString = null;
                        try {
                            jsonString = response.errorBody().string();
                            JSONObject jsonErrorContent = new JSONObject(jsonString);

                            String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= HEALYSA_CHECKUP, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                            logErrorAPIHealysa("HEALYSA_CHECKUP", errorTXT, "notOnFailure");
                        } catch (IOException | JSONException e) {
                            e.printStackTrace();
                            Log.e(TAG, "Réponse Santé Healysa [not successful]1 catch" + e);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call call, Throwable throwable) {
                Log.e(TAG, "Réponse HR Healysa [not successful] :"+ throwable);
                logErrorAPIHealysa("HEALYSA_CHECKUP",throwable.getMessage(),"onFailure");
            }
        } );
    }
    public void HEALYSA_CALL(String destinataire){
        Log.i(TAG, "Réponse numéro de téléphone :"+ destinataire);
        Retrofit retrofit = NetworkClient.getRetrofitClient( teamChatBuddyApplication,teamChatBuddyApplication.getParamFromFile("Healysa_URL_PROD",configFile) , 30);
        String phone_number = teamChatBuddyApplication.getParamFromFile( destinataire.toLowerCase( Locale.ROOT ), "agenda.properties" );
        if(phone_number==null){
            String text = "Génère moi une phrase pour dire que aucun numéro de téléphone est attribué à " + destinataire;
            Log.e(TAG, "Aucun numéro de téléphone est attribué à " + destinataire);
            logErrorAPIHealysa("HEALYSA_CALL","No telephone number is assigned to "+destinataire,"onFailure");
            return;
        }
        Log.i(TAG, "Réponse numéro de téléphone :"+ phone_number);
        ApiEndpointInterface api = retrofit.create(ApiEndpointInterface.class);
        Call<String> callHR = api.runCmdHealysa( "silver", teamChatBuddyApplication.getImeiDevice(), "IWBP32,"+teamChatBuddyApplication.getImeiDevice()+",080835,"+phone_number+"#", "Bearer " + teamChatBuddyApplication.getTokenHealysa() );
        callHR.enqueue( new Callback() {
            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()){
                    Log.i(TAG, "Réponse de l'appel [successful] :"+ response.body().toString());
                    translate("HEALYSA_CALL", new ITranslationCallback() {
                        @Override
                        public void onTranslated(String translatedText) {
                            String verifyMessage = verifyCmdMessages(translatedText);
                            if(verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART") ){
                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                            }
                        }
                    });
                }
                else{
                    String text = "Génère moi une phrase pour dire que je ne suis pas connecté à la plateforme Healysa";
                    if (response != null && response.errorBody() != null) {
                        Log.e(TAG, "Réponse de l'appel [not successful]");
                        String jsonString = null;
                        try {
                            jsonString = response.errorBody().string();
                            JSONObject jsonErrorContent = new JSONObject(jsonString);

                            String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= HEALYSA_CALL, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                            logErrorAPIHealysa("HEALYSA_CALL", errorTXT, "notOnFailure");
                        } catch (IOException | JSONException e) {
                            e.printStackTrace();
                            Log.e(TAG, "Réponse de l'appel [not successful]1 catch" + e);
                        }
                    }
                    Log.e(TAG, "Réponse de l'appel [not successful]");
                }
            }

            @Override
            public void onFailure(Call call, Throwable throwable) {
                Log.e(TAG, "Réponse de l'appel [not successful] :"+ throwable);
                logErrorAPIHealysa("HEALYSA_CALL",throwable.getMessage(),"onFailure");
            }
        } );
    }
    public void HEALYSA_LOC(String prénom){
        Retrofit retrofit = NetworkClient.getRetrofitClient( teamChatBuddyApplication,teamChatBuddyApplication.getParamFromFile("Healysa_URL_PROD",configFile) , 30);
        ApiEndpointInterface api = retrofit.create(ApiEndpointInterface.class);
        JSONObject jsonParams = new JSONObject();
        try {
            jsonParams.put("email",teamChatBuddyApplication.getParamFromFile("Healysa_mail",configFile) );
            jsonParams.put("password",teamChatBuddyApplication.getParamFromFile("Healysa_password",configFile));
            RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), jsonParams.toString());
            Call<JsonObject> callAuth = api.getTokenHealysa(body);
            callAuth.enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) {
                    if(response.isSuccessful()) {
                        Log.i(TAG, "Réponse Auth Healysa [successful] :"+response.body().toString());
                        try {
                            JSONObject jsonObj = new JSONObject( response.body().toString() );
                            teamChatBuddyApplication.setTokenHealysa(jsonObj.getString( "token" ));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        Call<JsonObject> callIMEI = api.getIMEIHealya( "Bearer "+ teamChatBuddyApplication.getTokenHealysa() );

                        callIMEI.enqueue( new Callback<JsonObject>() {
                            @Override
                            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                                if(response.isSuccessful()){
                                    try {
                                        JSONObject jsonObj = new JSONObject( response.body().toString() );
                                        JSONArray devices = jsonObj.getJSONArray( "devices" );
                                        Boolean findConsumer =false;
                                        for (int i=0; i < devices.length(); i++) {
                                            Log.i(TAG, "Réponse Consumer Firstname Healysa [successful] :"+ devices.getJSONObject( i ).getJSONObject( "consumer" ).getString( "firstname" ));
                                            if(devices.getJSONObject( i ).getJSONObject( "consumer" ).getString( "firstname" ).toLowerCase().equals( prénom.toLowerCase() )) {
                                                findConsumer = true;
                                                imeiLocation = devices.getJSONObject( i ).getString( "imei" );
                                                Log.i(TAG, "Réponse Imei Healysa [successful] :"+imeiLocation);
                                                Log.i(TAG, "Réponse Location Healysa [successful] :"+ prénom);
                                                Call<JsonArray> callLocation = api.getLocationBeaconHealysa( imeiLocation, "Bearer " + teamChatBuddyApplication.getTokenHealysa() );
                                                callLocation.enqueue( new Callback() {
                                                    @Override
                                                    public void onResponse(Call call, Response response) {
                                                        if (response.isSuccessful()){
                                                            Log.i(TAG, "Réponse Location Healysa [successful] :"+response.body().toString());
                                                            String responseBody = response.body().toString();
                                                            try {
                                                                JSONArray localisation = new JSONArray(responseBody);
                                                                if (localisation.length()>0){
                                                                    JSONObject firstObject = localisation.getJSONObject(0);

                                                                    // Récupérer la valeur de la clé "description"
                                                                    String description = firstObject.getString("description");
                                                                    translate("HEALYSA_LOC", new ITranslationCallback() {
                                                                        @Override
                                                                        public void onTranslated(String translatedText) {
                                                                            String verifyMessage = verifyCmdMessages(translatedText);
                                                                            if(verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART") ){
                                                                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +translatedText.split("\\s*/\\s*(?:/\\s*)?")[1].replace("[1]",description));
                                                                            }
                                                                        }
                                                                    });
                                                                }
                                                            } catch (JSONException e) {
                                                                e.printStackTrace();
                                                            }
                                                            //todo : traitement de la réponse

                                                        }
                                                        else{
                                                            if (response != null && response.errorBody() != null) {
                                                                Log.e(TAG, "Réponse HR Healysa [not successful] ");
                                                                String jsonString = null;
                                                                try {
                                                                    jsonString = response.errorBody().string();
                                                                    JSONObject jsonErrorContent = new JSONObject(jsonString);

                                                                    String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= HEALYSA_LOC, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                                                                    logErrorAPIHealysa("HEALYSA_LOC", errorTXT, "notOnFailure");
                                                                } catch (IOException | JSONException e) {
                                                                    e.printStackTrace();
                                                                    Log.e(TAG, "Réponse HR Healysa [not successful]1 catch" + e);
                                                                }
                                                            }
                                                            Log.e(TAG, "Réponse HR Healysa [not successful]2 :"+response.body().toString());
                                                        }
                                                    }

                                                    @Override
                                                    public void onFailure(Call call, Throwable throwable) {
                                                        throwable.printStackTrace();
                                                        Log.e(TAG, "Réponse HR Healysa [not successful]1 :"+ throwable);
                                                        logErrorAPIHealysa("HEALYSA_LOC",throwable.getMessage(),"onFailure");
                                                    }
                                                } );
                                            }
                                        }
                                        if (!findConsumer){
                                            logErrorAPIHealysa("HEALYSA_LOC","Consumer not found","onFailure");
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                                else{
                                    Log.e(TAG, "Réponse Imei Healysa [not successful]");
                                    if (response != null && response.errorBody() != null) {
                                        Log.e(TAG, "Réponse Imei Healysa [not successful] ");
                                        String jsonString = null;
                                        try {
                                            jsonString = response.errorBody().string();
                                            JSONObject jsonErrorContent = new JSONObject(jsonString);

                                            String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= HEALYSA_LOC, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                                            logErrorAPIHealysa("HEALYSA_LOC", errorTXT, "notOnFailure");
                                        } catch (IOException | JSONException e) {
                                            e.printStackTrace();
                                            Log.e(TAG, "Réponse Imei Healysa [not successful]1 catch" + e);
                                        }
                                    }
                                }
                            }

                            @Override
                            public void onFailure(Call<JsonObject> call, Throwable throwable) {
                                Log.e(TAG, "Réponse Imei Healysa [Failure] : " + throwable);
                                logErrorAPIHealysa("HEALYSA_LOC",throwable.getMessage(),"onFailure");
                            }
                        } );

                    }
                    else{
                        Log.e(TAG, "Réponse Auth Healysa [not successful]");
                        if (response != null && response.errorBody() != null) {
                            Log.e(TAG, "Réponse Auth Healysa [not successful]");
                            String jsonString = null;
                            try {
                                jsonString = response.errorBody().string();
                                JSONObject jsonErrorContent = new JSONObject(jsonString);

                                String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= HEALYSA_LOC, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                                logErrorAPIHealysa("HEALYSA_LOC", errorTXT, "notOnFailure");
                            } catch (IOException | JSONException e) {
                                e.printStackTrace();
                                Log.e(TAG, "Réponse Auth Healysa [not successful]1 catch" + e);
                            }
                        }
                    }
                }
                @Override
                public void onFailure(Call call, Throwable t) {
                    Log.e(TAG, "Réponse Auth Healysa [Failure] : " + t);
                    logErrorAPIHealysa("HEALYSA_LOC",t.getMessage(),"onFailure");
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Exception pendant la récupération de la réponse Healysa : " + e);
        }
    }
    public void HEALYSA_FEEDCAT(String portion){
        Retrofit retrofit = NetworkClient.getRetrofitClient( teamChatBuddyApplication,teamChatBuddyApplication.getParamFromFile("Healysa_URL_PROD",configFile), 30 );
        ApiEndpointInterface api = retrofit.create(ApiEndpointInterface.class);
        JSONObject jsonParams = new JSONObject();
        try {
            jsonParams.put("email",teamChatBuddyApplication.getParamFromFile("Healysa_mail",configFile) );
            jsonParams.put("password",teamChatBuddyApplication.getParamFromFile("Healysa_password",configFile));
            RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), jsonParams.toString());
            Call<JsonObject> callAuth = api.getTokenHealysa(body);
            callAuth.enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) {
                    if(response.isSuccessful()) {
                        Log.i( TAG, "Réponse Auth Healysa [successful] :" + response.body().toString() );
                        try {
                            JSONObject jsonObj = new JSONObject( response.body().toString() );
                            teamChatBuddyApplication.setTokenHealysa(jsonObj.getString( "token" ));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        Call<JsonObject> callIMEI = api.getIMEIHealya( "Bearer "+ teamChatBuddyApplication.getTokenHealysa() );
                        callIMEI.enqueue( new Callback<JsonObject>() {
                            @Override
                            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                                if(response.isSuccessful()){
                                    JSONObject jsonObj = null;
                                    try {
                                        jsonObj = new JSONObject( response.body().toString() );
                                        JSONArray devices = jsonObj.getJSONArray( "devices" );
                                        for (int i=0; i < devices.length(); i++) {
                                            Log.i(TAG, "Réponse Consumer Firstname Healysa [successful] :"+ devices.getJSONObject( i ).getJSONObject( "consumer" ).getString( "firstname" ));
                                            if(devices.getJSONObject( i ).getJSONObject( "consumer" ).getString( "firstname" ).equals( "FEEDER USA" )){
                                                Log.i(TAG, "Réponse Consumer Firstname Healysa [successful] :"+ devices.getJSONObject( i ).getString( "imei" ));
                                                imeiFeeder = devices.getJSONObject( i ).getString( "imei" );
                                            }

                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }




                                    JSONObject jsonA = new JSONObject();
                                    JSONObject jsonBody = new JSONObject();
                                    try {
                                        jsonA.put( "command_type", "FEED_PET" );
                                        jsonA.put( "command_value", portion );
                                        jsonBody.put( "command", jsonA);
                                        jsonBody.put( "user_id", null );
                                        RequestBody reqBody = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), jsonBody.toString());
                                        Call<JsonObject> callFeed = api.postFeedCat( imeiFeeder, reqBody, "Bearer " + teamChatBuddyApplication.getTokenHealysa());
                                        callFeed.enqueue( new Callback<JsonObject>() {
                                            @Override
                                            public void onResponse(Call call, Response response) {
                                                if(response.isSuccessful()){
                                                    Log.i( TAG, "Réponse Feed Healysa [successful] :" + response.body().toString());
                                                    translate("HEALYSA_FEEDCAT", new ITranslationCallback() {
                                                        @Override
                                                        public void onTranslated(String translatedText) {
                                                            String verifyMessage = verifyCmdMessages(translatedText);
                                                            if(verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART") ){
                                                                teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                                            }
                                                        }
                                                    });
                                                }
                                                else {
                                                    if (response != null && response.errorBody() != null) {
                                                        Log.e(TAG, "Réponse Feed Healysa  [not successful]");
                                                        String jsonString = null;
                                                        try {
                                                            jsonString = response.errorBody().string();
                                                            JSONObject jsonErrorContent = new JSONObject(jsonString);

                                                            String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= HEALYSA_FEEDCAT, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                                                            logErrorAPIHealysa("HEALYSA_FEEDCAT", errorTXT, "notOnFailure");
                                                        } catch (IOException | JSONException e) {
                                                            e.printStackTrace();
                                                            Log.e(TAG, "Réponse Feed Healysa  [not successful]1 catch" + e);
                                                        }
                                                    }
                                                }
                                            }

                                            @Override
                                            public void onFailure(Call call, Throwable t) {
                                                Log.e(TAG, "Réponse Feed Healysa [Failure] : " + t);
                                                logErrorAPIHealysa("HEALYSA_FEEDCAT",t.getMessage(),"onFailure");
                                            }
                                        } );
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }


                                }
                            }

                            @Override
                            public void onFailure(Call<JsonObject> call, Throwable t) {
                                logErrorAPIHealysa("HEALYSA_FEEDCAT",t.getMessage(),"onFailure");
                            }
                        } );
                    }
                    else {
                        if (response != null && response.errorBody() != null) {
                            Log.e(TAG, "Réponse FeedCat [not successful]");
                            String jsonString = null;
                            try {
                                jsonString = response.errorBody().string();
                                JSONObject jsonErrorContent = new JSONObject(jsonString);

                                String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= HEALYSA_FEEDCAT, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                                logErrorAPIHealysa("HEALYSA_FEEDCAT", errorTXT, "notOnFailure");
                            } catch (IOException | JSONException e) {
                                e.printStackTrace();
                                Log.e(TAG, "Réponse FeedCat  [not successful]1 catch" + e);
                            }
                        }
                    }
                }
                @Override
                public void onFailure(Call call, Throwable t) {
                    Log.e(TAG, "Réponse Feed Healysa [Failure] : " + t);
                    logErrorAPIHealysa("HEALYSA_FEEDCAT",t.getMessage(),"onFailure");
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception pendant la récupération de la réponse Healysa : " + e);
        }

    }
    public void logErrorAPIHealysa(String commande,String message,String type){
        String errorTXT="";
        if (type.equals("onFailure")){
            errorTXT= new Date().toString()+", COMMANDERRORAPI,Commande= "+commande+ " ERROR Body{  message= "+message+"}"+System.getProperty("line.separator");
        }
        else {
            errorTXT= message;
        }

        File file2 = new File(Environment.getExternalStorageDirectory(), "TeamChatBuddy/ERROR-History.txt");


        try {

            FileWriter fileWriter = new FileWriter(file2,true);
            fileWriter.write(errorTXT);
            fileWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void SWITCHBOT_LIGHT(String state){
        Retrofit retrofit = NetworkClient.getRetrofitClient(teamChatBuddyApplication,"https://api.switch-bot.com", 30);
        ApiEndpointInterface api = retrofit.create(ApiEndpointInterface.class);
        JSONObject jsonParams = new JSONObject();
        try {
            jsonParams.put("command","turn"+state);
            jsonParams.put("parameter","default");
            jsonParams.put( "commandType", "command" );
            RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), jsonParams.toString());
            Call call = api.getSwitchBot1Result(body);
            call.enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) {
                    if(response.isSuccessful()) {
                        Log.i(TAG, "Réponse SwitchBot [successful] :"+response.toString());
                        translate("SWITCHBOT_LIGHT", new ITranslationCallback() {
                            @Override
                            public void onTranslated(String translatedText) {
                                String verifyMessage = verifyCmdMessages(translatedText);
                                if(verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART") ){
                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                }
                            }
                        });
                    }
                    else{
                        Log.e(TAG, "Réponse SwitchBot [not successful] :"+response.toString());
                        if (response != null && response.errorBody() != null) {
                            Log.e(TAG, "Réponse SwitchBot [not successful]");
                            String jsonString = null;
                            try {
                                jsonString = response.errorBody().string();
                                JSONObject jsonErrorContent = new JSONObject(jsonString);

                                String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= SWITCHBOT_LIGHT, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                                logErrorAPIHealysa("SWITCHBOT_LIGHT", errorTXT, "notOnFailure");
                            } catch (IOException | JSONException e) {
                                e.printStackTrace();
                                Log.e(TAG, "Réponse SwitchBot [not successful]1 catch" + e);
                            }
                        }
                    }
                }
                @Override
                public void onFailure(Call call, Throwable t) {
                    Log.e(TAG, "Réponse SwitchBot [Failure] : " + t);
                    logErrorAPIHealysa("SWITCHBOT_LIGHT",t.getMessage(),"onFailure");
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception pendant la récupération de la réponse SwitchBot : " + e);
        }

    }
    public void CMD_IMAGE(String description){
        Log.i( TAG, "CMD IMAGE "+ description +" début." );

        //init parameters
        String image_URL = teamChatBuddyApplication.getParamFromFile("Image_URL",configFile);
        String image_endpoint = teamChatBuddyApplication.getParamFromFile("Image_endpoint",configFile);
        String image_model = teamChatBuddyApplication.getParamFromFile("Image_model",configFile);
        String image_size = teamChatBuddyApplication.getParamFromFile("Image_size",configFile);
        if(image_URL == null || image_URL.isEmpty()) image_URL = "https://api.openai.com/";
        if(image_endpoint == null || image_endpoint.isEmpty()) image_endpoint = "v1/images/generations";
        if(image_model == null || image_model.isEmpty()) image_model = "dall-e-2";
        if(image_size == null || image_size.isEmpty()) image_size = "512x512";

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("model", image_model);
            jsonObject.put("prompt", description);
            jsonObject.put("n", 1);
            jsonObject.put("size", image_size);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonObject.toString());

        Retrofit retrofit = NetworkClient.getRetrofitClient(teamChatBuddyApplication,image_URL, 50);
        ApiEndpointInterface api = retrofit.create(ApiEndpointInterface.class);
        Call<ResponseBody> call = api.generateImage(
                image_endpoint,
                "Bearer " + teamChatBuddyApplication.getparam("openAI_API_Key"),
                requestBody
        );

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseBodyString = response.body().string();
                        JSONObject responseObject = new JSONObject(responseBodyString);
                        JSONArray dataArray = responseObject.getJSONArray("data");
                        if (dataArray.length() > 0) {
                            JSONObject imageObject = dataArray.getJSONObject(0);
                            String urlImage = imageObject.getString("url");

                            Log.i( TAG, "CMD IMAGE "+ description + " URL : " + urlImage  );
                            String filename = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date())+".png";
                            @SuppressLint("StaticFieldLeak") ImageGenerator imageDownloader = new ImageGenerator(teamChatBuddyApplication, filename ) {
                                @Override
                                protected void onPostExecute(Bitmap bitmap) {
                                    super.onPostExecute(bitmap);
                                    Log.i( TAG, "CMD IMAGE "+ description +" téléchargé!" );
                                    if (bitmap != null) {
                                        translate("CMD_IMAGE", new ITranslationCallback() {
                                            @Override
                                            public void onTranslated(String translatedText) {
                                                String verifyMessage = verifyCmdMessages(translatedText);
                                                if(verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART") ){
                                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                                }
                                                teamChatBuddyApplication.notifyObservers( "showImage;SPLIT;"+filename);
                                            }
                                        });
                                    }
                                }
                            };
                            imageDownloader.execute(urlImage);
                        }
                    } catch (Exception e) {
                        Log.e(TAG,"generateImage ERROR "+e);
                        e.printStackTrace();
                    }
                }
                else {
                    Log.e(TAG,"generateImage response not successful");
                    if (response != null && response.errorBody() != null) {
                        Log.e(TAG, "generateImage response [not successful] ");
                        String jsonString = null;
                        try {
                            jsonString = response.errorBody().string();
                            JSONObject jsonErrorContent = new JSONObject(jsonString);

                            String errorTXT = new Date().toString() + ", COMMANDERRORAPI, Commande= CMD_IMAGE, ERROR Body= " + jsonErrorContent + System.getProperty("line.separator");
                            logErrorAPIHealysa("CMD_IMAGE", errorTXT, "notOnFailure");
                        } catch (IOException | JSONException e) {
                            e.printStackTrace();
                            Log.e(TAG, "generateImage response [not successful]1 catch" + e);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG,"generateImage onFailure : " + t);
                logErrorAPIHealysa("CMD_IMAGE",t.getMessage(),"onFailure");
                t.printStackTrace();
            }
        });

    }
    public void CMD_CLOSE_IMAGE(){
        Log.i( TAG, "CMD CLOSE IMAGE début.");
        translate("CMD_CLOSE_IMAGE", new ITranslationCallback() {
            @Override
            public void onTranslated(String translatedText) {
                teamChatBuddyApplication.notifyObservers( "closeImage");
                String verifyMessage = verifyCmdMessages(translatedText);
                if(verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART") ){
                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                }
            }
        });
    }
    public void CMD_TAKE_PHOTO(){
        Log.e("CMD_Take_Picture","commande take photo fonction CMD_TAKE_PHOTO");

        teamChatBuddyApplication.notifyObservers("takePicture");

    }
    public void phototakedMessage(){
        translate("CMD_PHOTO", new ITranslationCallback() {
            @Override
            public void onTranslated(String translatedText) {
                String verifyMessage = verifyCmdMessages(translatedText);
                if(verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART") ){
                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                }
            }
        });
    }

    private void playRadio(String radioUrl){
        stopMediaPlayer();
        radioPlayer = new MediaPlayer();
        radioPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build());
        try {
            radioPlayer.setDataSource(radioUrl);
            radioPlayer.prepareAsync();
            radioPlayer.setVolume(0.5f,0.5f);
        } catch (IOException e) {
            Log.e(TAG, "Erreur lors de la configuration du lecteur audio", e);
        }

        radioPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                translate("CMD_RADIO", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        radioPlayer.start();
                        String verifyMessage = verifyCmdMessages(translatedText);
                        if(verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART") ){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                        }
                    }
                });
            }
        });

        radioPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                // Gérer les erreurs de lecture ici
                Log.e(TAG, "Erreur de lecture audio. Code : " + what + ", Extra : " + extra);
                return false;
            }
        });
    }

    private void playMusic(File audioFile){

        Log.i( TAG, "CMD MUSIC début." );

        stopMediaPlayer();
        musicPlayer = new MediaPlayer();
        musicPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build());
        try {
            Uri uri = Uri.parse(audioFile.getPath());
            musicPlayer.setDataSource(teamChatBuddyApplication, uri);
            musicPlayer.prepareAsync();
            musicPlayer.setVolume(0.5f,0.5f);
        } catch (IOException e) {
            Log.e(TAG, "Erreur lors de la configuration du lecteur audio", e);
            e.printStackTrace();
        }

        musicPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                translate("CMD_MUSIC", new ITranslationCallback() {
                    @Override
                    public void onTranslated(String translatedText) {
                        musicPlayer.start();
                        String verifyMessage = verifyCmdMessages(translatedText);
                        if(verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART") ){
                            teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                        }
                    }
                });
            }
        });

        musicPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.e(TAG, "Erreur de lecture audio. Code : " + what + ", Extra : " + extra);
                return false;
            }
        });

    }

    private void stopMediaPlayer(){
        if (radioPlayer != null) {
            Log.i( TAG, "stop radioPlayer");
            radioPlayer.stop();
            radioPlayer.release();
            radioPlayer = null;
        }
        if (musicPlayer != null) {
            Log.i( TAG, "stop musicPlayer");
            musicPlayer.stop();
            musicPlayer.release();
            musicPlayer = null;
        }
    }
}

