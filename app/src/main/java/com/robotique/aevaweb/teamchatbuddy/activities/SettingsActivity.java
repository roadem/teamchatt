package com.robotique.aevaweb.teamchatbuddy.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.bfr.buddy.utils.events.EventItem;
import com.bfr.buddysdk.BuddyActivity;
import com.bfr.buddysdk.BuddySDK;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.robotique.aevaweb.teamchatbuddy.R;
import com.robotique.aevaweb.teamchatbuddy.adapters.ChatbotSpinnerAdapter;
import com.robotique.aevaweb.teamchatbuddy.adapters.LangueSpinnerAdapter;
import com.robotique.aevaweb.teamchatbuddy.adapters.SttSpinnerAdapter;
import com.robotique.aevaweb.teamchatbuddy.application.TeamChatBuddyApplication;
import com.robotique.aevaweb.teamchatbuddy.models.ChatBot;
import com.robotique.aevaweb.teamchatbuddy.models.Langue;
import com.robotique.aevaweb.teamchatbuddy.models.Setting;
import com.robotique.aevaweb.teamchatbuddy.models.SttModel;
import com.robotique.aevaweb.teamchatbuddy.observers.IDBObserver;
import com.robotique.aevaweb.teamchatbuddy.utilis.IMLKitDownloadCallback;
import com.robotique.aevaweb.teamchatbuddy.utilis.LanguageDetailsChecker;
import com.robotique.aevaweb.teamchatbuddy.utilis.WifiBroadcastReceiver;

import java.io.IOException;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends BuddyActivity implements IDBObserver,LanguageDetailsChecker.LanguageDetailsListener {
    private static final String TAG = "TEAMCHATBUDDY_SettingsActivity";

    private TeamChatBuddyApplication teamChatBuddyApplication;
    private View decorView;
    private RelativeLayout launch_view;
    private ImageView noNetwork;
    private ProgressBar downloadingBar;

    private LangueSpinnerAdapter langueSpinnerAdapter;
    private SttSpinnerAdapter sttSpinnerAdapter;
    private ChatbotSpinnerAdapter chatbotSpinnerAdapter;

    private LinearLayout menu_option_tracking_activation_lyt;
    private LinearLayout menu_option_tracking_camera_display_lyt;
    private LinearLayout menu_option_tracking_head_lyt;
    private LinearLayout menu_option_tracking_body_lyt;
    private LinearLayout menu_option_tracking_auto_listen_lyt;
    private LinearLayout menu_option_tracking_invitation_lyt;
    private LinearLayout menu_option_tracking_invitation_chatGpt_lyt;
    private LinearLayout menu_option_tracking_timeout_lyt;
    private LinearLayout menu_option_projectID_lyt;
    private LinearLayout menu_option_stt_lyt;
    private LinearLayout menu_option_chatbot_lyt;
    private ImageView affichage_Languages_List_lyt;

    private TextView menu_title;

    private TextView menu_option_projectID_textView;

    private TextView menu_option_langue_textView;
    private TextView menu_option_stt_textView;
    private TextView menu_option_chatbot_textView;

    private TextView menu_option_volume_textView;
    private TextView menu_option_affichage_textView;
    private TextView menu_option_emotion_textView;
    private TextView menu_option_detectLanguage_textView;
    private TextView menu_option_mode_stream_textView;
    private TextView menu_header_textView;
    private TextView menu_apiKey_textView;


    private TextView  menu_option_commande_textView;

    // Tracking text view
    private TextView menu_option_tracking_activation_textView;
    private TextView menu_option_tracking_camera_display_textView;
    private TextView menu_option_tracking_head_textView;
    private TextView menu_option_tracking_body_textView;
    private TextView menu_option_tracking_auto_listen_textView;
    private TextView menu_option_tracking_invitation_textView;
    private TextView menu_option_tracking_invitation_chatGpt_textView;
    private TextView menu_option_tracking_timeout_textView;
    private TextView option_tracking_timeout_textView;


    private Spinner menu_option_langue_spinner;
    private Spinner menu_option_stt_spinner;
    private Spinner menu_option_chatbot_spinner;


    private EditText menu_header_editText;
    private EditText menu_apiKey_editText;
    private EditText menu_option_projectID_editText;
    private EditText option_tracking_timeout;


    private TextView volume_seekbar_value;
    private SeekBar volume_seekbar;

    private Setting set;
    private Setting setting;
    private List<Langue> langues;

    private Switch switchVisibility;
    private Switch switchEmotion;
    private Switch switchLanguageDetection;
    private Switch switchModeStream;
    private Switch switchCommande;
    private Switch switchBIDisplay;
    private Switch switchTrackingActivation;
    private Switch switchTrackingCameraDisplay;
    private Switch switchTrackingHead;
    private Switch switchTrackingBody;
    private Switch switchTrackingAutoListen;
    private Switch switchTrackingInvitation;
    private Switch switchTrackingInvitationChatGpt;
    private Switch switchTrackingTimeout;

    private String french= "Français";
    private String english = "Anglais";
    private String speakVolume ="speak_volume";
    private String visibilityString = "switch_visibility";
    private String emotionString = "switch_emotion";
    private String detectionLanguageString = "Detection_de_langue";
    private String modeStreamString = "Mode_Stream";
    private String commandeString = "Commands";
    private String langueFR ="Français";
    private String langueEN ="Anglais";
    private String header ="header";
    private String entete ="entete";
    private String openAIKey = "openAI_API_Key";
    static Boolean modelDownloading = false;
    private boolean english_is_downloaded = false;
    private boolean french_is_downloaded = false;
    private WifiBroadcastReceiver wifiBroadCastReceiver = new WifiBroadcastReceiver();

    private int chosenLanguagePos = -1;
    private int chosenSTTPos = -1;
    private int chosenChatBotPos = -1;
    private CountDownTimer timerEcoute;
    private LanguageDetailsChecker languageDetailsChecker;
    private RelativeLayout popupLanguageList;
    private LinearLayout popupLanguageListContent;
    private ImageView dollar_icon;

    private LinearLayout menuOptionBIlyt;
    private LinearLayout menu_option_detection_language_lyt;
    private LinearLayout menu_option_stream_mode_lyt;
    private LinearLayout menu_option_commande_lyt;
    private LinearLayout menu_option_affichage_lyt;
    private LinearLayout option_tracking_lyt;


    private boolean isConnected = true;
    private boolean isClosingSettings = false;
    private Toast currentToast;
    private Handler handler= new Handler();
    private Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Log.d(TAG," --- onCreate() ---");

        teamChatBuddyApplication = (TeamChatBuddyApplication) getApplicationContext();
        teamChatBuddyApplication.hideSystemUI(this);
        teamChatBuddyApplication.setInitSharedpreferences(false);
        decorView=getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if(visibility==0){
                    decorView.setSystemUiVisibility(teamChatBuddyApplication.hideSystemUI(SettingsActivity.this));
                }
            }
        });


        menu_title = findViewById(R.id.menu_title);
        menu_option_commande_textView = findViewById(R.id.menu_option_commande_textView);
        menu_option_projectID_textView = findViewById(R.id.menu_option_projectID_textView);
        popupLanguageList= findViewById(R.id.popup_Languages_List);
        popupLanguageListContent= findViewById(R.id.popup_Languages_List_linearLayout);
        menu_option_projectID_lyt = findViewById(R.id.menu_option_projectID_lyt);
        affichage_Languages_List_lyt = findViewById(R.id.Android_STT_language_lyt);
        menu_option_stt_lyt = findViewById(R.id.menu_option_stt_lyt);
        menu_option_chatbot_lyt = findViewById(R.id.menu_option_chatbot_lyt);

        menu_option_langue_textView = findViewById(R.id.menu_option_langue_textView);
        menu_option_chatbot_textView = findViewById(R.id.menu_option_chatbot_textView);
        menu_option_stt_textView =findViewById(R.id.menu_option_stt_textView);
        option_tracking_timeout_textView = findViewById(R.id.option_tracking_timeout_textView);

        menu_option_volume_textView = findViewById(R.id.menu_option_volume_textView);
        menu_option_affichage_textView = findViewById(R.id.menu_option_affichage_textView);
        menu_option_emotion_textView = findViewById(R.id.menu_option_emotion_textView);
        menu_option_detectLanguage_textView = findViewById(R.id.menu_option_language_detection_textView);
        menu_option_mode_stream_textView = findViewById(R.id.menu_option_mode_stream_textView);
        menu_apiKey_textView = findViewById(R.id.api_key_txt);
        menu_header_textView = findViewById(R.id.header_txt);
        menu_option_langue_spinner = findViewById(R.id.menu_option_langue_spinner);
        menu_option_stt_spinner = findViewById(R.id.menu_option_stt_spinner);
        menu_option_chatbot_spinner = findViewById(R.id.menu_option_chatbot_spinner);
        option_tracking_timeout = findViewById(R.id.option_tracking_timeout);
        option_tracking_lyt = findViewById(R.id.option_tracking_lyt);

        menu_option_projectID_editText = findViewById(R.id.menu_option_projectID_editText);
        menu_apiKey_editText = findViewById(R.id.api_key_editText);
        menu_header_editText = findViewById(R.id.header_editText);

        volume_seekbar=findViewById(R.id.volume_seekbar);
        volume_seekbar_value=findViewById(R.id.volume_seekbar_value);
        switchVisibility=findViewById(R.id.switchVisibility);
        switchEmotion = findViewById(R.id.switchEmotion);
        switchBIDisplay = findViewById(R.id.switchBI);
        switchLanguageDetection = findViewById(R.id.switchLanguageDetection);
        switchModeStream = findViewById(R.id.switchModeStream);
        switchCommande = findViewById(R.id.switchCommande);
        launch_view = findViewById(R.id.launch_view);
        noNetwork = findViewById(R.id.noNetwork);
        downloadingBar = findViewById(R.id.progressBar_MLKitDownload);

        menu_option_tracking_activation_lyt = findViewById(R.id.menu_option_tracking_activation_lyt);
        menu_option_tracking_camera_display_lyt = findViewById(R.id.menu_option_tracking_camera_display_lyt);
        menu_option_tracking_head_lyt = findViewById(R.id.menu_option_tracking_head_lyt);
        menu_option_tracking_body_lyt = findViewById(R.id.menu_option_tracking_body_lyt);
        menu_option_tracking_auto_listen_lyt = findViewById(R.id.menu_option_tracking_auto_listen_lyt);
        menu_option_tracking_invitation_lyt = findViewById(R.id.menu_option_tracking_invitation_lyt);
        menu_option_tracking_timeout_lyt = findViewById(R.id.menu_option_tracking_timeout_lyt);
        menu_option_tracking_invitation_chatGpt_lyt = findViewById(R.id.menu_option_tracking_invitation_chatGpt_lyt);
        menu_option_tracking_activation_textView = findViewById(R.id.menu_option_tracking_activation_textView);
        menu_option_tracking_camera_display_textView = findViewById(R.id.menu_option_tracking_camera_display_textView);
        menu_option_tracking_head_textView = findViewById(R.id.menu_option_tracking_head_textView);
        menu_option_tracking_body_textView = findViewById(R.id.menu_option_tracking_body_textView);
        menu_option_tracking_auto_listen_textView = findViewById(R.id.menu_option_tracking_auto_listen_textView);
        menu_option_tracking_invitation_textView = findViewById(R.id.menu_option_tracking_invitation_textView);
        menu_option_tracking_invitation_chatGpt_textView = findViewById(R.id.menu_option_tracking_invitation_chatGpt_textView);
        menu_option_tracking_timeout_textView = findViewById(R.id.menu_option_tracking_timeout_textView);
        switchTrackingActivation = findViewById(R.id.switchTrackingActivation);
        switchTrackingCameraDisplay = findViewById(R.id.switchTrackingCameraDisplay);
        switchTrackingHead = findViewById(R.id.switchTrackingHead);
        switchTrackingBody = findViewById(R.id.switchTrackingBody);
        switchTrackingAutoListen = findViewById(R.id.switchTrackingAutoListen);
        switchTrackingInvitation = findViewById(R.id.switchTrackingInvitation);
        switchTrackingInvitationChatGpt = findViewById(R.id.switchTrackingInvitationChatGpt);
        switchTrackingTimeout = findViewById(R.id.switchTrackingTimeout);
        dollar_icon = findViewById(R.id.dollar_icon);

        menuOptionBIlyt = findViewById(R.id.menu_option_BI_lyt);
        menu_option_detection_language_lyt = findViewById(R.id.menu_option_detection_language_lyt);
        menu_option_stream_mode_lyt = findViewById(R.id.menu_option_stream_mode_lyt);
        menu_option_commande_lyt = findViewById(R.id.menu_option_commande_lyt);
        menu_option_affichage_lyt = findViewById(R.id.menu_option_affichage_lyt);

        set=new Setting();
        setting=new Setting();
        teamChatBuddyApplication.registerObserver(this);
        wifiBroadCastReceiver.setAct(getApplicationContext());
        IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(wifiBroadCastReceiver, intentFilter);
        wifiBroadCastReceiver.forceCheckConnexState(getApplicationContext());


        ImageView blue_mic_lyt = findViewById(R.id.blue_mic_lyt);

        String blueMic_Disponibility = teamChatBuddyApplication.getParamFromFile("BlueMic_Disponibility", "TeamChatBuddy.properties");

        if(blueMic_Disponibility != null && blueMic_Disponibility.trim().equalsIgnoreCase("Yes")){
            blue_mic_lyt.setVisibility(View.VISIBLE);
        }
        else{
            blue_mic_lyt.setVisibility(View.INVISIBLE);
        }


        /**
         *  Gestion du choix de la durée d'écoute (secondes)
         */

        handlerListeningDuration();



        /**
         *  Gestion du choix de du nombre de tentatives
         */

        handlerListeningAttempt();

        /**
         *  Gestion des chatbots
         */
        String can_change_chatBot = teamChatBuddyApplication.getParamFromFile("Change_chatBot", "TeamChatBuddy.properties");
        if(can_change_chatBot != null && can_change_chatBot.trim().equalsIgnoreCase("Yes")){
            menu_option_chatbot_lyt.setVisibility(View.VISIBLE);
        }
        else{
            menu_option_chatbot_lyt.setVisibility(View.GONE);
        }
        handlerChatbot();


        /**
         *  Gestion du projectID
         */
        handlerProjectID();


        /**
         *  Gestion du seekbar de volume de parole
         */


        handlerSpeakVolume();
        /**
         *  Gestion de la liste déroulante pour le choix de langue [ Français | Anglais ]
         */

        teamChatBuddyApplication.setparam("previousLanguage",new Gson().toJson(teamChatBuddyApplication.getLangue()));
        handlerLangue();

        /**
         *  Gestion de l'affichage des paroles
         */

        if(teamChatBuddyApplication.getparam(visibilityString).contains("yes")){
            switchVisibility.setChecked(true);
            set.setSwitchVisibility("true");
            setting.setSwitchVisibility("true");
            teamChatBuddyApplication.setSwitchVisibility("true");
            if(teamChatBuddyApplication.getparam(visibilityString).equals("yeshid")){
                menu_option_affichage_textView.setVisibility(View.GONE);
                switchVisibility.setVisibility(View.GONE);
            }
            else{
                menu_option_affichage_textView.setVisibility(View.VISIBLE);
                switchVisibility.setVisibility(View.VISIBLE);
            }
        }
        else{
            switchVisibility.setChecked(false);
            set.setSwitchVisibility("false");
            setting.setSwitchVisibility("false");
            teamChatBuddyApplication.setSwitchVisibility("false");
            if(teamChatBuddyApplication.getparam(visibilityString).equals("nohid")){
                menu_option_affichage_textView.setVisibility(View.GONE);
                switchVisibility.setVisibility(View.GONE);
            }
            else{
                menu_option_affichage_textView.setVisibility(View.VISIBLE);
                switchVisibility.setVisibility(View.VISIBLE);
            }
        }
        switchVisibility.setOnCheckedChangeListener((CompoundButton compoundButton, boolean b) ->{
            teamChatBuddyApplication.setSwitchVisibility(String.valueOf(b));
            if(b){
                teamChatBuddyApplication.setparam(visibilityString,"yes");
            }else{
                teamChatBuddyApplication.setparam(visibilityString,"no");
            }
            set.setSwitchVisibility(String.valueOf(b));

        });

        /**
         *  Gestion de la lecture des BI
         */
        if(teamChatBuddyApplication.getparam("Stimulis").contains("yes")){
            switchBIDisplay.setChecked(true);
            set.setSwitchBIDisplay("true");
            setting.setSwitchBIDisplay("true");
            teamChatBuddyApplication.setSwitchBIDisplay("true");
            if(teamChatBuddyApplication.getparam("Stimulis").equals("yeshid")){
                menuOptionBIlyt.setVisibility(View.GONE);
            }
            else{
                menuOptionBIlyt.setVisibility(View.VISIBLE);
            }
        }
        else{
            switchBIDisplay.setChecked(false);
            set.setSwitchBIDisplay("false");
            setting.setSwitchBIDisplay("false");
            teamChatBuddyApplication.setSwitchBIDisplay("false");
            if(teamChatBuddyApplication.getparam("Stimulis").equals("nohid")){
                menuOptionBIlyt.setVisibility(View.GONE);
            }
            else{
                menuOptionBIlyt.setVisibility(View.VISIBLE);
            }
        }
        switchBIDisplay.setOnCheckedChangeListener((CompoundButton compoundButton, boolean b) ->{
            teamChatBuddyApplication.setSwitchBIDisplay(String.valueOf(b));
            if(b){
                teamChatBuddyApplication.setparam("Stimulis","yes");
                BuddySDK.Companion.raiseEvent("disableRightEye");
                BuddySDK.Companion.raiseEvent("disableLeftEye");
                BuddySDK.Companion.raiseEvent("disableHeadSensors");
                BuddySDK.Companion.raiseEvent("disableBodySensors");
            }
            else{
                teamChatBuddyApplication.setparam("Stimulis","no");
                if (teamChatBuddyApplication.getParamFromFile("use_companion_when_stimulis_disabled","TeamChatBuddy.properties").trim().equalsIgnoreCase("Yes")){
                    Log.e("MRARA","enable Raise event Yes");
                    BuddySDK.Companion.raiseEvent("enableRightEye");
                    BuddySDK.Companion.raiseEvent("enableLeftEye");
                    BuddySDK.Companion.raiseEvent("enableHeadSensors");
                    BuddySDK.Companion.raiseEvent("enableBodySensors");
                    BuddySDK.Companion.raiseEvent("disableOnMouth");
                }else {
                    Log.e("MRARA","disable Raise event NO");
                    BuddySDK.Companion.raiseEvent("disableRightEye");
                    BuddySDK.Companion.raiseEvent("disableLeftEye");
                    BuddySDK.Companion.raiseEvent("disableHeadSensors");
                    BuddySDK.Companion.raiseEvent("disableBodySensors");
                }
            }
            set.setSwitchBIDisplay(String.valueOf(b));
        });


        /**
         *  Gestion de l'affichage des émotions
         */
        if(teamChatBuddyApplication.getparam(emotionString).contains("yes")){
            switchEmotion.setChecked(true);
            set.setSwitchEmotion("true");
            setting.setSwitchEmotion("true");
            teamChatBuddyApplication.setSwitchEmotion("true");

            if(teamChatBuddyApplication.getparam(emotionString).equals("yeshid")){
                menu_option_emotion_textView.setVisibility(View.GONE);
                switchEmotion.setVisibility(View.GONE);
            }
            else{
                menu_option_emotion_textView.setVisibility(View.VISIBLE);
                switchEmotion.setVisibility(View.VISIBLE);
            }
        }
        else{
            switchEmotion.setChecked(false);
            set.setSwitchEmotion("false");
            setting.setSwitchEmotion("false");
            teamChatBuddyApplication.setSwitchEmotion("false");

            if(teamChatBuddyApplication.getparam(emotionString).equals("nohid")){
                menu_option_emotion_textView.setVisibility(View.GONE);
                switchEmotion.setVisibility(View.GONE);
            }
            else{
                menu_option_emotion_textView.setVisibility(View.VISIBLE);
                switchEmotion.setVisibility(View.VISIBLE);
            }
        }

        switchEmotion.setOnCheckedChangeListener((CompoundButton compoundButton, boolean b) ->{
            teamChatBuddyApplication.setSwitchEmotion(String.valueOf(b));
            if(b){
                teamChatBuddyApplication.setparam(emotionString,"yes");
            }
            else{
                teamChatBuddyApplication.setparam(emotionString,"no");
            }
            set.setSwitchEmotion(String.valueOf(b));
        });
        /**
         *  Gestion de la detection des langues
         */
        if(teamChatBuddyApplication.getparam(detectionLanguageString).contains("yes")){
            switchLanguageDetection.setChecked(true);
            set.setSwitchLanguageDetection("true");
            setting.setSwitchLanguageDetection("true");
            teamChatBuddyApplication.setSwitchdetectLanguage("true");
            if(teamChatBuddyApplication.getparam(detectionLanguageString).equals("yeshid")){
                menu_option_detection_language_lyt.setVisibility(View.GONE);
            }else{
                menu_option_detection_language_lyt.setVisibility(View.VISIBLE);
            }
        }
        else{
            switchLanguageDetection.setChecked(false);
            set.setSwitchLanguageDetection("false");
            setting.setSwitchLanguageDetection("false");
            teamChatBuddyApplication.setSwitchdetectLanguage("false");
            if(teamChatBuddyApplication.getparam(detectionLanguageString).equals("nohid")){
                menu_option_detection_language_lyt.setVisibility(View.GONE);
            }else{
                menu_option_detection_language_lyt.setVisibility(View.VISIBLE);
            }
        }
        switchLanguageDetection.setOnCheckedChangeListener((CompoundButton compoundButton, boolean b) ->{
            teamChatBuddyApplication.setSwitchdetectLanguage(String.valueOf(b));
            if(b){
                teamChatBuddyApplication.setparam(detectionLanguageString,"yes");
            }
            else{
                teamChatBuddyApplication.setparam(detectionLanguageString,"no");
            }

            set.setSwitchLanguageDetection(String.valueOf(b));
        });



        /**
         *  Gestion du switch mode stream
         */
        if(teamChatBuddyApplication.getparam(modeStreamString).contains("yes")){
            switchModeStream.setChecked(true);
            set.setSwitchModeStream("true");
            setting.setSwitchModeStream("true");
            teamChatBuddyApplication.setSwitchModeStream("true");
            if(teamChatBuddyApplication.getparam(modeStreamString).equals("yeshid")){
                menu_option_stream_mode_lyt.setVisibility(View.GONE);
            }else{
                menu_option_stream_mode_lyt.setVisibility(View.VISIBLE);
            }
        }
        else{
            switchModeStream.setChecked(false);
            set.setSwitchModeStream("false");
            setting.setSwitchModeStream("false");
            teamChatBuddyApplication.setSwitchModeStream("false");
            if(teamChatBuddyApplication.getparam(modeStreamString).equals("nohid")){
                menu_option_stream_mode_lyt.setVisibility(View.GONE);
            }else{
                menu_option_stream_mode_lyt.setVisibility(View.VISIBLE);
            }
        }

        switchModeStream.setOnCheckedChangeListener((CompoundButton compoundButton, boolean b) ->{
            teamChatBuddyApplication.setSwitchModeStream(String.valueOf(b));
            if(b){
                teamChatBuddyApplication.setparam(modeStreamString,"yes");
            }
            else{
                teamChatBuddyApplication.setparam(modeStreamString,"no");
            }
            set.setSwitchModeStream(String.valueOf(b));
        });

        /**
         *  Gestion du switch commande
         */
        if(teamChatBuddyApplication.getparam(commandeString).contains("yes")){
            switchCommande.setChecked(true);
            set.setSwitchCommande("true");
            setting.setSwitchCommande("true");
            teamChatBuddyApplication.setSwitchCommande("true");
            if(teamChatBuddyApplication.getparam(commandeString).equals("yeshid")){
                menu_option_commande_lyt.setVisibility(View.GONE);
            }else{
                menu_option_commande_lyt.setVisibility(View.VISIBLE);
            }
        }
        else {
            switchCommande.setChecked(false);
            set.setSwitchCommande("false");
            setting.setSwitchCommande("false");
            teamChatBuddyApplication.setSwitchCommande("false");
            if(teamChatBuddyApplication.getparam(commandeString).equals("nohid")){
                menu_option_commande_lyt.setVisibility(View.GONE);
            }else{
                menu_option_commande_lyt.setVisibility(View.VISIBLE);
            }
        }

        switchCommande.setOnCheckedChangeListener((CompoundButton compoundButton, boolean b) ->{
            teamChatBuddyApplication.setSwitchCommande(String.valueOf(b));
            if(b){
                teamChatBuddyApplication.setparam(commandeString,"yes");
            }
            else{
                teamChatBuddyApplication.setparam(commandeString,"no");
            }

            set.setSwitchCommande(String.valueOf(b));
        });



        if(teamChatBuddyApplication.getparam(visibilityString).contains("hid") && teamChatBuddyApplication.getparam(emotionString).contains("hid")){
            menu_option_affichage_lyt.setVisibility(View.GONE);
        }
        else{
            menu_option_affichage_lyt.setVisibility(View.VISIBLE);
        }


        /**
         *  Gestion de l'entete
         */
        handlerHeader();
        /**
         *  Gestion de l'api key
         */
        handlerApiKey();

        /**
         * gestion du timeout
         */
        handlerTrackingTimeout();

        /**
         *  Gestion du choix STT
         */
        String can_change_stt = teamChatBuddyApplication.getParamFromFile("Change_STT", "TeamChatBuddy.properties");
        if(can_change_stt != null && can_change_stt.trim().equalsIgnoreCase("Yes")){
            menu_option_stt_lyt.setVisibility(View.VISIBLE);
        }
        else{
            menu_option_stt_lyt.setVisibility(View.GONE);
        }
        handlerSTT();

        /**
         *  Gestion de l'affichage du bouton permettant d'accéder à l'interface de consommation d'openai
         */
        String show_openAI_prices = teamChatBuddyApplication.getParamFromFile("show_openAI_prices", "TeamChatBuddy.properties");
        if(show_openAI_prices != null && show_openAI_prices.trim().equalsIgnoreCase("yes")){
            dollar_icon.setVisibility(View.VISIBLE);
        }
        else{
            dollar_icon.setVisibility(View.INVISIBLE);
        }

        /**
         * Gestion Tracking
         */
        handlerTracking();

        popupLanguageList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Vérifier si le popup_add_mail est visible et si le clic est en dehors de celui-ci
                if (popupLanguageList.getVisibility() == View.VISIBLE) {
                    MotionEvent event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0, 0, 0);
                    if (!isViewInsideBounds(popupLanguageListContent, (int) event.getRawX(), (int) event.getRawY())) {
                        // Si le clic est en dehors, rendre le popup invisible
                        popupLanguageList.setVisibility(View.INVISIBLE);
                    }
                }
            }
        });
        popupLanguageListContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Ne rien faire pour empêcher la propagation du clic aux éléments enfants du popup
            }
        });

        Long mLkit_timeout_in_seconds = (long) Integer.parseInt(teamChatBuddyApplication.getParamFromFile("MLkit_timeout_in_seconds", "TeamChatBuddy.properties")) * 1000;

        timerEcoute = new CountDownTimer(mLkit_timeout_in_seconds, 1000) {
            @Override
            public void onTick(long l) {
                if(l < mLkit_timeout_in_seconds - 1000) {
                    if (modelDownloading) {
                        if (isConnected) {
                            showToastMessage();
                        }

                    }
                }
            }
            @Override
            public void onFinish() {
                if(currentToast != null) currentToast.cancel();
                launch_view.setVisibility(View.INVISIBLE);
                teamChatBuddyApplication.setLangue(new Gson().fromJson(teamChatBuddyApplication.getparam("previousLanguage"), Langue.class));
                setPreviousLanguage();
                Log.i("HHO","--- onfinish s---"+teamChatBuddyApplication.getLangue().getNom());
                Toast.makeText(getApplicationContext(),R.string.toast_message_error_downloading_en, Toast.LENGTH_LONG).show();
                modelDownloading = false;
                setLanguageText();
            }
        };

    }

    private Runnable runnableProgressBar = new Runnable() {
        @Override
        public void run() {
            launch_view.setVisibility(View.VISIBLE);
            timerEcoute.start();
        }
    };

    public void setPreviousLanguage(){

        if(langues!=null && !langues.isEmpty()){
            for (int index = 0; index < langues.size(); index++) {
                langues.get(index).setChosen(false);
                if (langues.get(index).getNom().equalsIgnoreCase(new Gson().fromJson(teamChatBuddyApplication.getparam("previousLanguage"), Langue.class).getNom())) {
                    langues.get(index).setChosen(true);
                    teamChatBuddyApplication.setLangue(langues.get(index));
                    menu_option_langue_spinner.setSelection(index);
                    menu_option_langue_spinner.setEnabled(true);
                }
                if(langues.get(index).getNom().equals("Français")){
                    teamChatBuddyApplication.setparam("Français",new Gson().toJson(langues.get(index)));
                }
                else if(langues.get(index).getNom().equals("Anglais")){
                    teamChatBuddyApplication.setparam("Anglais",new Gson().toJson(langues.get(index)));
                }
                else{
                    teamChatBuddyApplication.setparam(langues.get(index).getNom(),new Gson().toJson(langues.get(index)));
                }
            }
        }
    }


    // Vérifie si les coordonnées de l'événement sont à l'intérieur de la vue spécifiée
    private boolean isViewInsideBounds(View view, int x, int y) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int viewX = location[0];
        int viewY = location[1];
        return !(x < viewX || x > viewX + view.getWidth() || y < viewY || y > viewY + view.getHeight());
    }


    private void handlerTracking(){

        //Tracking activation
        if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes")){
            if(!teamChatBuddyApplication.getparam("Tracking_Activation").equals("yeshid")){
                menu_option_tracking_camera_display_lyt.setVisibility(View.VISIBLE);
                menu_option_tracking_head_lyt.setVisibility(View.VISIBLE);
                menu_option_tracking_body_lyt.setVisibility(View.VISIBLE);
                menu_option_tracking_auto_listen_lyt.setVisibility(View.VISIBLE);
                menu_option_tracking_invitation_lyt.setVisibility(View.VISIBLE);
                menu_option_tracking_activation_lyt.setVisibility(View.VISIBLE);
                menu_option_tracking_timeout_lyt.setVisibility(View.VISIBLE);
                option_tracking_lyt.setVisibility(View.VISIBLE);
                if(Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Invitation"))){
                    menu_option_tracking_invitation_chatGpt_lyt.setVisibility(View.VISIBLE);
                }
                else{
                    menu_option_tracking_invitation_chatGpt_lyt.setVisibility(View.GONE);
                }
            }else{
                menu_option_tracking_camera_display_lyt.setVisibility(View.GONE);
                menu_option_tracking_head_lyt.setVisibility(View.GONE);
                menu_option_tracking_body_lyt.setVisibility(View.GONE);
                menu_option_tracking_auto_listen_lyt.setVisibility(View.GONE);
                menu_option_tracking_invitation_lyt.setVisibility(View.GONE);
                menu_option_tracking_timeout_lyt.setVisibility(View.GONE);
                menu_option_tracking_invitation_chatGpt_lyt.setVisibility(View.GONE);
                menu_option_tracking_activation_lyt.setVisibility(View.GONE);
                option_tracking_lyt.setVisibility(View.GONE);
            }
        }
        else{
            if(!teamChatBuddyApplication.getparam("Tracking_Activation").equals("nohid")){
                menu_option_tracking_activation_lyt.setVisibility(View.VISIBLE);
            }
            else{
                menu_option_tracking_activation_lyt.setVisibility(View.GONE);
            }
            menu_option_tracking_camera_display_lyt.setVisibility(View.GONE);
            menu_option_tracking_head_lyt.setVisibility(View.GONE);
            menu_option_tracking_body_lyt.setVisibility(View.GONE);
            menu_option_tracking_auto_listen_lyt.setVisibility(View.GONE);
            menu_option_tracking_invitation_lyt.setVisibility(View.GONE);
            menu_option_tracking_timeout_lyt.setVisibility(View.GONE);
            menu_option_tracking_invitation_chatGpt_lyt.setVisibility(View.GONE);
            option_tracking_lyt.setVisibility(View.GONE);
        }
        if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes")){
            switchTrackingActivation.setChecked(true);
        }
        else{
            switchTrackingActivation.setChecked(false);
        }
        switchTrackingActivation.setOnCheckedChangeListener((CompoundButton compoundButton, boolean b) ->{
            if(b){
                teamChatBuddyApplication.setparam("Tracking_Activation","yes");
            }
            else{
                teamChatBuddyApplication.setparam("Tracking_Activation","no");
            }
            if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes")){
                menu_option_tracking_camera_display_lyt.setVisibility(View.VISIBLE);
                menu_option_tracking_head_lyt.setVisibility(View.VISIBLE);
                menu_option_tracking_body_lyt.setVisibility(View.VISIBLE);
                menu_option_tracking_auto_listen_lyt.setVisibility(View.VISIBLE);
                menu_option_tracking_invitation_lyt.setVisibility(View.VISIBLE);
                menu_option_tracking_timeout_lyt.setVisibility(View.VISIBLE);
                option_tracking_lyt.setVisibility(View.VISIBLE);
                if(Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Invitation"))){
                    menu_option_tracking_invitation_chatGpt_lyt.setVisibility(View.VISIBLE);
                }
                else{
                    menu_option_tracking_invitation_chatGpt_lyt.setVisibility(View.GONE);
                }
            }
            else{
                menu_option_tracking_camera_display_lyt.setVisibility(View.GONE);
                menu_option_tracking_head_lyt.setVisibility(View.GONE);
                menu_option_tracking_body_lyt.setVisibility(View.GONE);
                menu_option_tracking_auto_listen_lyt.setVisibility(View.GONE);
                menu_option_tracking_invitation_lyt.setVisibility(View.GONE);
                menu_option_tracking_timeout_lyt.setVisibility(View.GONE);
                menu_option_tracking_invitation_chatGpt_lyt.setVisibility(View.GONE);
                option_tracking_lyt.setVisibility(View.GONE);
            }
        });

        //Tracking camera display
        switchTrackingCameraDisplay.setChecked(Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Camera_Display")));
        switchTrackingCameraDisplay.setOnCheckedChangeListener((CompoundButton compoundButton, boolean b) ->{
            teamChatBuddyApplication.setparam("Tracking_Camera_Display",String.valueOf(b));
        });

        //Tracking head
        switchTrackingHead.setChecked(Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Head")));
        switchTrackingHead.setOnCheckedChangeListener((CompoundButton compoundButton, boolean b) ->{
            teamChatBuddyApplication.setparam("Tracking_Head",String.valueOf(b));
        });

        //Tracking body
        switchTrackingBody.setChecked(Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Body")));
        switchTrackingBody.setOnCheckedChangeListener((CompoundButton compoundButton, boolean b) ->{
            teamChatBuddyApplication.setparam("Tracking_Body",String.valueOf(b));
        });

        //Tracking auto listen
        switchTrackingAutoListen.setChecked(Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Auto_Listen")));
        switchTrackingAutoListen.setOnCheckedChangeListener((CompoundButton compoundButton, boolean b) ->{
            teamChatBuddyApplication.setparam("Tracking_Auto_Listen",String.valueOf(b));
        });

        //Tracking invitation
        switchTrackingInvitation.setChecked(Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Invitation")));
        switchTrackingInvitation.setOnCheckedChangeListener((CompoundButton compoundButton, boolean b) ->{
            teamChatBuddyApplication.setparam("Tracking_Invitation",String.valueOf(b));
            if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes")){
                if(Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Invitation"))){
                    menu_option_tracking_invitation_chatGpt_lyt.setVisibility(View.VISIBLE);
                }
                else{
                    menu_option_tracking_invitation_chatGpt_lyt.setVisibility(View.GONE);
                }
            }
            else menu_option_tracking_invitation_chatGpt_lyt.setVisibility(View.GONE);
        });

        //Tracking invitation chatGpt
        switchTrackingInvitationChatGpt.setChecked(Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Invitation_ChatGpt")));
        switchTrackingInvitationChatGpt.setOnCheckedChangeListener((CompoundButton compoundButton, boolean b) ->{
            teamChatBuddyApplication.setparam("Tracking_Invitation_ChatGpt",String.valueOf(b));
        });
        //Tracking Timeout
        switchTrackingTimeout.setChecked(Boolean.parseBoolean(teamChatBuddyApplication.getparam("TRACKING_timeout_Switch")));
        switchTrackingTimeout.setOnCheckedChangeListener((CompoundButton compoundButton, boolean b) ->{
            teamChatBuddyApplication.setparam("TRACKING_timeout_Switch",String.valueOf(b));
        });


    }

    private void handlerTrackingTimeout() {

        option_tracking_timeout.setImeOptions(EditorInfo.IME_FLAG_NO_FULLSCREEN);
        option_tracking_timeout.setText(teamChatBuddyApplication.getparam("trackingTimeout"));

        option_tracking_timeout.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Method left empty intentionally because no specific action is needed for this update.
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                teamChatBuddyApplication.setparam("trackingTimeout", charSequence.toString());
                Log.i("Finiche","onTextChanged "+charSequence.toString());

            }
            @Override
            public void afterTextChanged(Editable editable) {
                // Method left empty intentionally because no specific action is needed for this update.
            }
        });

        option_tracking_timeout.setOnFocusChangeListener((v,hasFocus) -> {
            if (hasFocus) {
                View decorView = getWindow().getDecorView();
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN);
            } else {
                teamChatBuddyApplication.hideSystemUI(SettingsActivity.this);
            }
        });
    }

    private void handlerLangue() {

        langues = new ArrayList<>();

        List<String> langueDisponible = teamChatBuddyApplication.getDisponibleLangue();
        for (int i=0;i<langueDisponible.size();i++){

            langues.add(new Gson().fromJson(teamChatBuddyApplication.getparam(langueDisponible.get(i)), Langue.class));
        }
        if (langues.isEmpty()){
            langues.add(new Gson().fromJson(teamChatBuddyApplication.getparam(french), Langue.class));
        }

        langueSpinnerAdapter = new LangueSpinnerAdapter(getApplicationContext(),
                R.layout.spinner_item_layout_resource,
                R.id.item_name,
                R.id.checked_item_checked,
                langues);
        menu_option_langue_spinner.setAdapter(langueSpinnerAdapter);


        avoidSpinnerDropdownFocus(menu_option_langue_spinner);


        //get the position, of the chosen language
        for (int index = 0; index < langues.size(); index++) {
            if (langues.get(index).isChosen()) {
                chosenLanguagePos = index;
                teamChatBuddyApplication.setLangue(langues.get(index));
                setting.setLangue(langues.get(index).getNom());
                break;
            }
            if ( index == langues.size()-1){
                chosenLanguagePos = index;
                teamChatBuddyApplication.setLangue(langues.get(index));
                setting.setLangue(langues.get(index).getNom());

            }
        }
        menu_option_langue_spinner.setSelection(chosenLanguagePos);
        menu_option_langue_spinner.setEnabled(true);
        setLanguageText();
        menu_option_langue_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                chosenLanguagePos = position;
                for(Langue langue : langues ) {
                    langue.setChosen(false);
                    if (langue.equals(parent.getSelectedItem())) {
                        langue.setChosen(true);
                        teamChatBuddyApplication.setLangue(langue);
                        set.setLangue(langue.getNom());
                        modelDownloading = true;
                        List<String> mlkitLangueCode = teamChatBuddyApplication.getLanguageCodeForDisponibleLangue("Language_Code_Used_In_Mlkit");
                        String codeLanguageMlkit = mlkitLangueCode.get(teamChatBuddyApplication.getLangue().getId()-1);
                        teamChatBuddyApplication.downloadModel(imlKitDownloadCallback,codeLanguageMlkit.trim());
                        handlerProgressBar.postDelayed(runnableProgressBar,500);

                    }
                    if(langue.getNom().equals(langueFR)){
                        teamChatBuddyApplication.setparam(french,new Gson().toJson(langue));
                    }
                    else if(langue.getNom().equals(langueEN)){
                        teamChatBuddyApplication.setparam(english,new Gson().toJson(langue));
                    }
                    else{
                        teamChatBuddyApplication.setparam(langue.getNom(),new Gson().toJson(langue));
                    }
                }
                langueSpinnerAdapter.updateDataSet(langues);
                setLanguageText();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Method left empty intentionally because no specific action is needed for this update.
            }
        });
    }
    private void handlerChatbot(){
        final List<ChatBot> chatbotList = new ArrayList<>();
        chatbotList.add(new ChatBot(1,"ChatGPT",false));
        chatbotList.add(new ChatBot(2,"CustomGPT",false));

        chatbotSpinnerAdapter= new ChatbotSpinnerAdapter(getApplicationContext(),
                R.layout.spinner_item_layout_resource,
                R.id.item_name,
                R.id.checked_item_checked,
                chatbotList);
        menu_option_chatbot_spinner.setAdapter(chatbotSpinnerAdapter);
        avoidSpinnerDropdownFocus(menu_option_chatbot_spinner);
        for (int i=0;i<chatbotList.size();i++){
            if (chatbotList.get(i).getNom().equalsIgnoreCase(teamChatBuddyApplication.getparam("chatbot_chosen"))){
                chosenChatBotPos=i;
                setting.setChatbot(teamChatBuddyApplication.getparam("chatbot_chosen"));
                set.setChatbot(teamChatBuddyApplication.getparam("chatbot_chosen"));
                if (chatbotList.get(i).getNom().equalsIgnoreCase("ChatGPT")){
                    menu_option_projectID_editText.setEnabled(false);
                    menu_option_projectID_lyt.setVisibility(View.GONE);
                    if(teamChatBuddyApplication.getparam(modeStreamString).contains("yes")){
                        switchModeStream.setChecked(true);
                    }else{
                        switchModeStream.setChecked(false);
                    }
                    switchModeStream.setEnabled(true);
                    switchModeStream.setAlpha(1f);
                }
                else{
                    menu_option_projectID_lyt.setVisibility(View.VISIBLE);
                    menu_option_projectID_editText.setEnabled(true);
                    switchModeStream.setChecked(true);
                    switchModeStream.setEnabled(false);
                    switchModeStream.setAlpha(0.4f);
                }
                break;
            }
        }
        menu_option_chatbot_spinner.setSelection(chosenChatBotPos);
        menu_option_chatbot_spinner.setEnabled(true);

        menu_option_chatbot_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                chosenChatBotPos = position;
                for(ChatBot chatBot : chatbotList ) {
                    chatBot.setChosen(false);
                    if (chatBot.equals(parent.getSelectedItem())) {
                        chatBot.setChosen(true);
                        teamChatBuddyApplication.setparam("chatbot_chosen", chatBot.getNom());
                        set.setChatbot(chatBot.getNom());
                        if (chatBot.getNom().equalsIgnoreCase("ChatGPT")){
                            menu_apiKey_editText.setText(teamChatBuddyApplication.getparam(openAIKey));
                            menu_option_projectID_editText.setEnabled(false);
                            menu_option_projectID_lyt.setVisibility(View.GONE);

                            if(teamChatBuddyApplication.getparam(modeStreamString).contains("yes")){
                                switchModeStream.setChecked(true);
                            }else{
                                switchModeStream.setChecked(false);
                            }
                            switchModeStream.setEnabled(true);
                            switchModeStream.setAlpha(1f);
                            if (teamChatBuddyApplication.getLangue().getNom().equals("Anglais")){
                                menu_header_editText.setText(teamChatBuddyApplication.getparam(header));
                            }
                            else if(teamChatBuddyApplication.getLangue().getNom().equals("Français")){
                                menu_header_editText.setText(teamChatBuddyApplication.getparam(entete));
                            }
                            else{
                                menu_header_editText.setText(teamChatBuddyApplication.getparam(teamChatBuddyApplication.getLangue().getNom()+"entete"));
                            }
                        }
                        else{
                            menu_apiKey_editText.setText(teamChatBuddyApplication.getparam("CustomGPT_API_Key"));
                            menu_option_projectID_lyt.setVisibility(View.VISIBLE);
                            menu_option_projectID_editText.setEnabled(true);
                            switchModeStream.setChecked(true);
                            switchModeStream.setEnabled(false);
                            switchModeStream.setAlpha(0.4f);
                            if (teamChatBuddyApplication.getLangue().getNom().equals("Anglais")){
                                menu_header_editText.setText(teamChatBuddyApplication.getparam("CustomGPT_header"));
                            }
                            else if(teamChatBuddyApplication.getLangue().getNom().equals("Français")){
                                menu_header_editText.setText(teamChatBuddyApplication.getparam("CustomGPT_entete"));
                            }
                            else{
                                menu_header_editText.setText(teamChatBuddyApplication.getparam(teamChatBuddyApplication.getLangue().getNom()+"CustomGPT_entete"));
                            }

                        }
                    }
                }
                chatbotSpinnerAdapter.updateDataSet(chatbotList);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    private void handlerSTT(){


        final List<SttModel> sttList = new ArrayList<>();


        sttList.add(new SttModel(1,"Android",false));
        sttList.add(new SttModel(2,"Google",false));
        sttList.add(new SttModel(3,"Whisper",false));
        sttList.add(new SttModel(4,"Cerence",false));



        sttSpinnerAdapter= new SttSpinnerAdapter(getApplicationContext(),
                R.layout.spinner_item_layout_resource,
                R.id.item_name,
                R.id.checked_item_checked,
                sttList);
        menu_option_stt_spinner.setAdapter(sttSpinnerAdapter);
        avoidSpinnerDropdownFocus(menu_option_stt_spinner);
        for (int i=0;i<sttList.size();i++){
            if (sttList.get(i).getNom().equalsIgnoreCase(teamChatBuddyApplication.getparam("STT_chosen"))){
                chosenSTTPos=i;
                if (sttList.get(i).getNom().equalsIgnoreCase("Android")){
                    affichage_Languages_List_lyt.setVisibility(View.VISIBLE);
                }
                else {
                    affichage_Languages_List_lyt.setVisibility(View.INVISIBLE);
                }
                break;
            }
        }

        menu_option_stt_spinner.setSelection(chosenSTTPos);
        menu_option_stt_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                chosenLanguagePos = position;
                for(SttModel stt : sttList ) {
                    stt.setChosen(false);
                    if (stt.equals(parent.getSelectedItem())) {
                        stt.setChosen(true);
                        if (stt.getNom().equalsIgnoreCase("Android")){
                            affichage_Languages_List_lyt.setVisibility(View.VISIBLE);
                        }
                        else {
                            affichage_Languages_List_lyt.setVisibility(View.INVISIBLE);
                        }
                        teamChatBuddyApplication.setparam("STT_chosen",stt.getNom());
                    }

                }
                sttSpinnerAdapter.updateDataSet(sttList);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

    }
    private void handlerSpeakVolume() {
        volume_seekbar_value.setText(teamChatBuddyApplication.getparam(speakVolume)+"%");
        volume_seekbar.setProgress(Integer.parseInt(teamChatBuddyApplication.getparam(speakVolume)));
        set.setVolume(teamChatBuddyApplication.getparam(speakVolume));
        setting.setVolume(teamChatBuddyApplication.getparam(speakVolume));
        teamChatBuddyApplication.setSpeakVolume(Integer.parseInt(teamChatBuddyApplication.getparam(speakVolume)));

        volume_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                teamChatBuddyApplication.setVolume(progress, AudioManager.FLAG_SHOW_UI);
                volume_seekbar_value.setText(progress + " %");
                teamChatBuddyApplication.setparam(speakVolume, Integer.toString(progress));
                set.setVolume(Integer.toString(progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Method left empty intentionally because no specific action is needed for this update.
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                // Method left empty intentionally because no specific action is needed for this update.
            }
        });
    }



    private void handlerListeningAttempt() {
        String listeningAttempt = teamChatBuddyApplication.getParamFromFile("Number_listens","TeamChatBuddy.properties");
        if(listeningAttempt.equals("")||Integer.parseInt(listeningAttempt)<=0){
            listeningAttempt = "1";
        }
        teamChatBuddyApplication.setListeningAttempt(Integer.parseInt(listeningAttempt));
        set.setAttempt(listeningAttempt);
        setting.setAttempt(listeningAttempt);

    }

    private void handlerProjectID() {
        set.setProjectID(teamChatBuddyApplication.getparam("CustomGPT_Project_ID"));
        setting.setProjectID(teamChatBuddyApplication.getparam("CustomGPT_Project_ID"));

        menu_option_projectID_editText.setImeOptions(EditorInfo.IME_FLAG_NO_FULLSCREEN);

        menu_option_projectID_editText.setText(teamChatBuddyApplication.getparam("CustomGPT_Project_ID"));

        menu_option_projectID_editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Method left empty intentionally because no specific action is needed for this update.
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(!charSequence.toString().isEmpty()){
                    teamChatBuddyApplication.setparam("CustomGPT_Project_ID",charSequence.toString());
                    set.setProjectID(teamChatBuddyApplication.getparam("CustomGPT_Project_ID"));
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {
                // Method left empty intentionally because no specific action is needed for this update.
            }
        });

        menu_option_projectID_editText.setOnFocusChangeListener((v,hasFocus) -> {
            if (hasFocus) {
                View decorView = getWindow().getDecorView();
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN);
            } else {
                teamChatBuddyApplication.hideSystemUI(SettingsActivity.this);
            }
        });
    }

    private void handlerListeningDuration() {
        String listeningDuration =teamChatBuddyApplication.getParamFromFile("Listening_time","TeamChatBuddy.properties");
        if(listeningDuration.equals("")||Integer.parseInt(listeningDuration)<=0){
            listeningDuration= "10";
        }
        teamChatBuddyApplication.setListeningDuration(Integer.parseInt(listeningDuration));
        set.setDuration(listeningDuration);
        setting.setDuration(listeningDuration);
    }


    private void handlerHeader() {
        menu_header_editText.setImeOptions(EditorInfo.IME_FLAG_NO_FULLSCREEN);

        if (teamChatBuddyApplication.getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT")) {
            if (teamChatBuddyApplication.getLangue().getNom().equals("Anglais")){
                menu_header_editText.setText(teamChatBuddyApplication.getparam(header));
                set.setHeader(teamChatBuddyApplication.getparam(header));
                setting.setHeader(teamChatBuddyApplication.getparam(header));
            }
            else if(teamChatBuddyApplication.getLangue().getNom().equals("Français")){
                menu_header_editText.setText(teamChatBuddyApplication.getparam(entete));
                set.setHeader(teamChatBuddyApplication.getparam(entete));
                setting.setHeader(teamChatBuddyApplication.getparam(entete));
            }
            else{
                menu_header_editText.setText(teamChatBuddyApplication.getparam(teamChatBuddyApplication.getLangue().getNom()+"entete"));
                set.setHeader(teamChatBuddyApplication.getparam(teamChatBuddyApplication.getLangue().getNom()+"entete"));
                setting.setHeader(teamChatBuddyApplication.getparam(teamChatBuddyApplication.getLangue().getNom()+"entete"));
            }
        }
        else{
            if (teamChatBuddyApplication.getLangue().getNom().equals("Anglais")){
                menu_header_editText.setText(teamChatBuddyApplication.getparam("CustomGPT_header"));
                set.setHeader(teamChatBuddyApplication.getparam("CustomGPT_header"));
                setting.setHeader(teamChatBuddyApplication.getparam("CustomGPT_header"));
            }
            else if(teamChatBuddyApplication.getLangue().getNom().equals("Français")){
                menu_header_editText.setText(teamChatBuddyApplication.getparam("CustomGPT_entete"));
                set.setHeader(teamChatBuddyApplication.getparam("CustomGPT_entete"));
                setting.setHeader(teamChatBuddyApplication.getparam("CustomGPT_entete"));
            }
            else{
                menu_header_editText.setText(teamChatBuddyApplication.getparam(teamChatBuddyApplication.getLangue().getNom()+"CustomGPT_entete"));
                set.setHeader(teamChatBuddyApplication.getparam(teamChatBuddyApplication.getLangue().getNom()+"CustomGPT_entete"));
                setting.setHeader(teamChatBuddyApplication.getparam(teamChatBuddyApplication.getLangue().getNom()+"CustomGPT_entete"));
            }
        }

        menu_header_editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Method left empty intentionally because no specific action is needed for this update.
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                if (teamChatBuddyApplication.getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT")) {
                    if (teamChatBuddyApplication.getLangue().getNom().equals("Anglais")){
                        teamChatBuddyApplication.setparam(header,charSequence.toString());
                        set.setHeader(teamChatBuddyApplication.getparam(header));
                    }
                    else if(teamChatBuddyApplication.getLangue().getNom().equals("Français")){
                        teamChatBuddyApplication.setparam(entete,charSequence.toString());
                        set.setHeader(teamChatBuddyApplication.getparam(entete));
                    }
                    else {
                        teamChatBuddyApplication.setparam(teamChatBuddyApplication.getLangue().getNom()+"entete",charSequence.toString());
                        set.setHeader(teamChatBuddyApplication.getparam(teamChatBuddyApplication.getLangue().getNom()+"entete"));
                    }
                }
                else{
                    if (teamChatBuddyApplication.getLangue().getNom().equals("Anglais")){
                        teamChatBuddyApplication.setparam("CustomGPT_header",charSequence.toString());
                        set.setHeader(teamChatBuddyApplication.getparam("CustomGPT_header"));
                    }
                    else if(teamChatBuddyApplication.getLangue().getNom().equals("Français")){
                        teamChatBuddyApplication.setparam("CustomGPT_entete",charSequence.toString());
                        set.setHeader(teamChatBuddyApplication.getparam("CustomGPT_entete"));
                    }
                    else {
                        teamChatBuddyApplication.setparam(teamChatBuddyApplication.getLangue().getNom()+"CustomGPT_entete",charSequence.toString());
                        set.setHeader(teamChatBuddyApplication.getparam(teamChatBuddyApplication.getLangue().getNom()+"CustomGPT_entete"));
                    }
                }
                menu_header_editText.post(() -> menu_header_editText.requestLayout());
            }
            @Override
            public void afterTextChanged(Editable editable) {
                // Method left empty intentionally because no specific action is needed for this update.
            }
        });

        menu_header_editText.setOnFocusChangeListener((v,hasFocus) -> {
            if (hasFocus) {
                View decorView = getWindow().getDecorView();
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN);
            } else {
                teamChatBuddyApplication.hideSystemUI(SettingsActivity.this);
            }
        });
    }
    private void handlerApiKey() {



        menu_apiKey_editText.setImeOptions(EditorInfo.IME_FLAG_NO_FULLSCREEN);


        if (teamChatBuddyApplication.getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT")) {
            menu_apiKey_editText.setText(teamChatBuddyApplication.getparam(openAIKey));
            set.setApiKey(teamChatBuddyApplication.getparam(openAIKey));
            setting.setApiKey(teamChatBuddyApplication.getparam(openAIKey));
        }
        else{
            menu_apiKey_editText.setText(teamChatBuddyApplication.getparam("CustomGPT_API_Key"));
            set.setApiKey(teamChatBuddyApplication.getparam("CustomGPT_API_Key"));
            setting.setApiKey(teamChatBuddyApplication.getparam("CustomGPT_API_Key"));
        }

        menu_apiKey_editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Method left empty intentionally because no specific action is needed for this update.
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                if (teamChatBuddyApplication.getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT")) {
                    teamChatBuddyApplication.setparam(openAIKey, charSequence.toString());
                    set.setApiKey(teamChatBuddyApplication.getparam(openAIKey));
                }else{
                    teamChatBuddyApplication.setparam("CustomGPT_API_Key", charSequence.toString());
                    set.setApiKey(teamChatBuddyApplication.getparam("CustomGPT_API_Key"));
                }

            }
            @Override
            public void afterTextChanged(Editable editable) {
                // Method left empty intentionally because no specific action is needed for this update.
            }
        });

        menu_apiKey_editText.setOnFocusChangeListener((v,hasFocus) -> {
            if (hasFocus) {
                View decorView = getWindow().getDecorView();
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN);
            } else {
                teamChatBuddyApplication.hideSystemUI(SettingsActivity.this);
            }
        });
    }
    public void setLanguageText(){
        if(teamChatBuddyApplication.getLangue().getNom().equals("Anglais")){
            menu_title.setText(R.string.menu_title_en);
            menu_option_commande_textView.setText(R.string.menu_option_commande_en);
            menu_option_projectID_textView.setText(R.string.menu_option_projectID_en);
            menu_option_langue_textView.setText(R.string.menu_option_langue_en);
            menu_option_stt_textView.setText(R.string.menu_option_stt_en);
            menu_option_chatbot_textView.setText(R.string.menu_option_chatbot_en);
            menu_option_volume_textView.setText(R.string.menu_option_volume_en);
            menu_option_affichage_textView.setText(R.string.menu_option_affichage_en);
            menu_option_emotion_textView.setText(R.string.menu_option_emotion_en);
            menu_option_detectLanguage_textView.setText(R.string.menu_option_detectionLanguage_en);
            menu_option_mode_stream_textView.setText(R.string.menu_option_mode_stream_en);
            menu_apiKey_textView.setText(R.string.menu_api_key_en);
            menu_header_textView.setText(R.string.menu_header_en);
            menu_option_tracking_activation_textView.setText(R.string.menu_option_tracking_activation_en);
            menu_option_tracking_camera_display_textView.setText(R.string.menu_option_tracking_camera_display_en);
            menu_option_tracking_head_textView.setText(R.string.menu_option_tracking_head_en);
            menu_option_tracking_body_textView.setText(R.string.menu_option_tracking_body_en);
            menu_option_tracking_auto_listen_textView.setText(R.string.menu_option_tracking_auto_listen_en);
            menu_option_tracking_invitation_textView.setText(R.string.menu_option_tracking_invitation_en);
            menu_option_tracking_invitation_chatGpt_textView.setText(R.string.menu_option_tracking_invitation_chatGpt_en);
            menu_option_tracking_timeout_textView.setText(R.string.menu_option_tracking_timeout_en);
            option_tracking_timeout_textView.setText(R.string.tracking_delay_en);

            menu_option_projectID_editText.setHint(R.string.menu_option_projectID_hint_en);
            if (teamChatBuddyApplication.getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT")) {
                menu_header_editText.setText(teamChatBuddyApplication.getparam(header));
            }
            else{
                menu_header_editText.setText(teamChatBuddyApplication.getparam("CustomGPT_header"));
            }
            menu_header_editText.setEnabled(true);
        }
        else if(teamChatBuddyApplication.getLangue().getNom().equals("Français")){
            menu_title.setText(R.string.menu_title_fr);
            menu_option_commande_textView.setText(R.string.menu_option_commande_fr);
            menu_option_projectID_textView.setText(R.string.menu_option_projectID_fr);
            menu_option_langue_textView.setText(R.string.menu_option_langue_fr);
            menu_option_stt_textView.setText(R.string.menu_option_stt_fr);
            menu_option_chatbot_textView.setText(R.string.menu_option_chatbot_fr);
            menu_option_volume_textView.setText(R.string.menu_option_volume_fr);
            menu_option_affichage_textView.setText(R.string.menu_option_affichage_fr);
            menu_option_emotion_textView.setText(R.string.menu_option_emotion_fr);
            menu_option_detectLanguage_textView.setText(R.string.menu_option_detectionLanguage_fr);
            menu_option_mode_stream_textView.setText(R.string.menu_option_mode_stream_fr);
            menu_apiKey_textView.setText(R.string.menu_api_key_fr);
            menu_header_textView.setText(R.string.menu_header_fr);
            menu_option_projectID_editText.setHint(R.string.menu_option_projectID_hint_fr);
            menu_option_tracking_activation_textView.setText(R.string.menu_option_tracking_activation_fr);
            menu_option_tracking_camera_display_textView.setText(R.string.menu_option_tracking_camera_display_fr);
            menu_option_tracking_head_textView.setText(R.string.menu_option_tracking_head_fr);
            menu_option_tracking_body_textView.setText(R.string.menu_option_tracking_body_fr);
            menu_option_tracking_auto_listen_textView.setText(R.string.menu_option_tracking_auto_listen_fr);
            menu_option_tracking_invitation_textView.setText(R.string.menu_option_tracking_invitation_fr);
            menu_option_tracking_invitation_chatGpt_textView.setText(R.string.menu_option_tracking_invitation_chatGpt_fr);
            menu_option_tracking_timeout_textView.setText(R.string.menu_option_tracking_timeout_fr);
            option_tracking_timeout_textView.setText(R.string.tracking_delay_fr);

            if (teamChatBuddyApplication.getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT")) {
                menu_header_editText.setText(teamChatBuddyApplication.getparam(entete));
            }
            else{
                menu_header_editText.setText(teamChatBuddyApplication.getparam("CustomGPT_entete"));
            }
            menu_header_editText.setEnabled(true);
        }
        else {
            if (!modelDownloading){
                translateAndSetTextView(R.string.menu_title_en, menu_title,"");
                translateAndSetTextView(R.string.menu_option_commande_en,menu_option_commande_textView,"");
                translateAndSetTextView(R.string.menu_option_langue_en,menu_option_langue_textView,"");
                translateAndSetTextView(R.string.menu_option_stt_en,menu_option_stt_textView,"");
                translateAndSetTextView(R.string.menu_option_chatbot_en,menu_option_chatbot_textView,"");
                translateAndSetTextView(R.string.menu_option_projectID_en,menu_option_projectID_textView,"");
                translateAndSetTextView(R.string.menu_option_volume_en,menu_option_volume_textView,"");
                translateAndSetTextView(R.string.menu_option_affichage_en,menu_option_affichage_textView,"");
                translateAndSetTextView(R.string.menu_option_emotion_en,menu_option_emotion_textView,"");
                translateAndSetTextView(R.string.menu_option_detectionLanguage_en,menu_option_detectLanguage_textView,"");
                translateAndSetTextView(R.string.menu_option_mode_stream_en,menu_option_mode_stream_textView,"");
                translateAndSetTextView(R.string.menu_api_key_en,menu_apiKey_textView,"");
                translateAndSetTextView(R.string.menu_header_en,menu_header_textView,"");
                translateAndSetTextView(R.string.menu_option_projectID_hint_en,menu_option_projectID_editText,"");
                translateAndSetTextView(R.string.menu_option_tracking_activation_en,menu_option_tracking_activation_textView,"");
                translateAndSetTextView(R.string.menu_option_tracking_camera_display_en,menu_option_tracking_camera_display_textView,"");
                translateAndSetTextView(R.string.menu_option_tracking_head_en,menu_option_tracking_head_textView,"");
                translateAndSetTextView(R.string.menu_option_tracking_body_en,menu_option_tracking_body_textView,"");
                translateAndSetTextView(R.string.menu_option_tracking_auto_listen_en,menu_option_tracking_auto_listen_textView,"");
                translateAndSetTextView(R.string.menu_option_tracking_invitation_en,menu_option_tracking_invitation_textView,"");
                translateAndSetTextView(R.string.menu_option_tracking_invitation_chatGpt_en,menu_option_tracking_invitation_chatGpt_textView,"");
                translateAndSetTextView(R.string.menu_option_tracking_timeout_en,menu_option_tracking_timeout_textView,"");
                translateAndSetTextView(R.string.tracking_delay_en,option_tracking_timeout_textView,"");

                if (teamChatBuddyApplication.getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT")) {
                    if(teamChatBuddyApplication.getparam(teamChatBuddyApplication.getLangue().getNom()+"entete").equals("")){
                        translateAndSetTextView(0,menu_header_editText,teamChatBuddyApplication.getparam(header));
                    }
                    else{
                        menu_header_editText.setText(teamChatBuddyApplication.getparam(teamChatBuddyApplication.getLangue().getNom()+"entete"));
                    }
                }
                else{
                    if(teamChatBuddyApplication.getparam(teamChatBuddyApplication.getLangue().getNom()+"CustomGPT_entete").equals("")){
                        translateAndSetTextView(0,menu_header_editText,teamChatBuddyApplication.getparam("CustomGPT_header"));
                    }
                    else{
                        menu_header_editText.setText(teamChatBuddyApplication.getparam(teamChatBuddyApplication.getLangue().getNom()+"CustomGPT_entete"));
                    }
                }
            }
        //    modelDownloading = true;

        }
    }
    private void translateAndSetTextView(int stringResId, final View view,String texteAtraduire) {
        String text="";
        if (stringResId!=0){
            text= getResources().getString(stringResId);
        }
        else {
            text=texteAtraduire;
            Log.e("TEST","header = "+text);
        }
        teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(text)
                .addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String translatedText) {
                        if (view instanceof EditText) {
                            ((EditText) view).setHint(translatedText);
                            if (stringResId==0) {
                                ((EditText) view).setText(translatedText);

                                Log.e("TEST","header traduit = "+translatedText);
                                if (teamChatBuddyApplication.getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT")) {
                                    teamChatBuddyApplication.setparam(teamChatBuddyApplication.getLangue().getNom()+"entete",translatedText);
                                    set.setHeader(teamChatBuddyApplication.getparam(teamChatBuddyApplication.getLangue().getNom()+"entete"));
                                }
                                else{
                                    teamChatBuddyApplication.setparam(teamChatBuddyApplication.getLangue().getNom()+"CustomGPT_entete",translatedText);
                                    set.setHeader(teamChatBuddyApplication.getparam(teamChatBuddyApplication.getLangue().getNom()+"CustomGPT_entete"));
                                }
                            }
                        } else  if (view instanceof TextView) {
                            ((TextView) view).setText(translatedText);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "translatedText exception  " + e);
                    }
                });
    }
    private Handler handlerProgressBar = new Handler(Looper.getMainLooper());


    private IMLKitDownloadCallback imlKitDownloadCallback = new IMLKitDownloadCallback() {
        @Override
        public void onDownloadEnd(boolean success,String english_or_french) {
            if(success){
                switch (english_or_french) {
                    case "english":
                        english_is_downloaded = true;
                        break;
                    case "french":
                        french_is_downloaded = true;
                        break;
                }
                if (english_is_downloaded && french_is_downloaded) {
                    handlerProgressBar.removeCallbacksAndMessages(null);
                    handlerProgressBar.removeCallbacks(runnableProgressBar);

                    launch_view.setVisibility(View.INVISIBLE);
                    modelDownloading = false;
                    setLanguageText();
                    langueSpinnerAdapter.notifyDataSetChanged();
                    if(currentToast!=null){
                        currentToast.cancel();
                    }
                    if(timerEcoute!=null){
                        timerEcoute.cancel();
                    }
                    teamChatBuddyApplication.setparam("previousLanguage",new Gson().toJson(teamChatBuddyApplication.getLangue()));
                }
            }
            else{
                french_is_downloaded = false;
                english_is_downloaded = false;
                List<String> mlkitLangueCode = teamChatBuddyApplication.getLanguageCodeForDisponibleLangue("Language_Code_Used_In_Mlkit");
                String codeLanguageMlkit = mlkitLangueCode.get(teamChatBuddyApplication.getLangue().getId()-1);
                teamChatBuddyApplication.downloadModel(imlKitDownloadCallback,codeLanguageMlkit.trim());
                handlerProgressBar.postDelayed(runnableProgressBar,500);
            }

        }
    };
    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG," --- onResume() ---");

        teamChatBuddyApplication.hideSystemUI(this);
        teamChatBuddyApplication.setVolume(Integer.parseInt(teamChatBuddyApplication.getparam("speak_volume")),AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!teamChatBuddyApplication.getInitSharedpreferences()){
            teamChatBuddyApplication.setparam("firstLaunch","true");
            teamChatBuddyApplication.notifyObservers("ChatDestroy");
        }
        try {
            unregisterReceiver(wifiBroadCastReceiver);
        }catch(IllegalArgumentException e) {
            Log.i(TAG,"---unregisterReceiver wifiBroadcast:: IllegalArgumentException---"+e.getMessage());
        }
        teamChatBuddyApplication.removeObserver(this);
        Log.d(TAG," --- onDestroy() ---");
        if(timerEcoute!=null){
            timerEcoute.cancel();
        }
        if(currentToast != null) currentToast.cancel();
        if(modelDownloading && !isClosingSettings){
            //set previous langue
            setPreviousLanguage();
            downloadingBar.setVisibility(View.GONE);

        }
    }
    @Override
    public void update(String message) throws IOException {

        if (message != null) {

            if(message.contains("main destroy")){
                teamChatBuddyApplication.setFileCreate(false);
                teamChatBuddyApplication.setparam("firstLaunch","false");
            }
            if (message.contains("isConnected")){
                isConnected = true;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        noNetwork.setVisibility(View.GONE);
                        if(launch_view.getVisibility() ==  View.VISIBLE){
                            if(modelDownloading && timerEcoute!=null){
                                timerEcoute.start();
                                downloadingBar.setVisibility(View.VISIBLE);
                            }
                        }

                    }
                });
            }
            if (message.contains("isNotConnected")){
                isConnected = false;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        downloadingBar.setVisibility(View.GONE);
                        noNetwork.setVisibility(View.VISIBLE);
                        if(timerEcoute!=null){
                            timerEcoute.cancel();
                        }
                        if(currentToast != null) currentToast.cancel();

                    }
                });
            }
            if (message.contains("changeDetected")){
                int speakVolume = teamChatBuddyApplication.getVolume();
                int max = teamChatBuddyApplication.getMaxVolume();
                int defaultVolume = teamChatBuddyApplication.getClosestInt((double) (speakVolume * 100) / max);
                Log.e("FCH","volumeMedia  "+String.valueOf(defaultVolume));
                teamChatBuddyApplication.setparam("speak_volume", String.valueOf(defaultVolume));
                teamChatBuddyApplication.setVolume(defaultVolume,AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                volume_seekbar_value.setText(defaultVolume + " %");
                set.setVolume(Integer.toString(defaultVolume));
                volume_seekbar.setProgress(defaultVolume);
            }


        }
    }
    @Override
    public void onSDKReady() {
        Log.w(TAG, "onSDKReady");
    }

    @Override
    public void onEvent(EventItem iEvent) {
        Log.w(TAG, "onEvent : "+iEvent.toString());
    }
    public void btnCloseSettings(View view) {
        if (runnable!=null){
            handler.removeCallbacks(runnable);
            runnable = null;
        }
        runnable= new Runnable() {
            @Override
            public void run() {
            teamChatBuddyApplication.setSetting(set);
            isClosingSettings = true;
            if(!set.getChatbot().equals(setting.getChatbot())) {
                teamChatBuddyApplication.setFileCreate(true);
            }

            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
            intent.putExtra("fromSettings", "true");
            finish();
            startActivity(intent);
            overridePendingTransition(0, 0);
            }
        };
        handler.postDelayed(runnable,300);

    }

    public void btnOpenBlueMic(View view) {
        Intent intentTeamChatBlueMicActivity = new Intent(SettingsActivity.this, TeamChatBlueMicActivity.class);
        startActivity(intentTeamChatBlueMicActivity);
        overridePendingTransition(0, 0);
    }
    public void btnAfficheLanguageList(View view){
        Intent detailsIntent = new Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS);
        detailsIntent.setPackage("com.google.android.googlequicksearchbox");
        languageDetailsChecker = new LanguageDetailsChecker(SettingsActivity.this);
        sendOrderedBroadcast(detailsIntent, null, languageDetailsChecker, null, Activity.RESULT_OK, null, null);
    }
    public void btnOpenAi(View view) {
        Intent intentOpenAiActivity = new Intent(getApplicationContext(), OpenAiActivity.class);
        startActivity(intentOpenAiActivity);
        overridePendingTransition(0, 0);
    }
    public void btnRefreshHeader(View view){
        if (teamChatBuddyApplication.getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT")) {
            if (teamChatBuddyApplication.getLangue().getNom().equals("Anglais")){
                teamChatBuddyApplication.setparam(header,teamChatBuddyApplication.getParamFromFile("Chatgpt_header","TeamChatBuddy.properties"));
                set.setHeader(teamChatBuddyApplication.getparam(header));
                menu_header_editText.setText(teamChatBuddyApplication.getparam(header));
            }
            else if(teamChatBuddyApplication.getLangue().getNom().equals("Français")){
                teamChatBuddyApplication.setparam(entete,teamChatBuddyApplication.getParamFromFile("Chatgpt_entete","TeamChatBuddy.properties"));
                set.setHeader(teamChatBuddyApplication.getparam(entete));
                menu_header_editText.setText(teamChatBuddyApplication.getparam(entete));
            }
            else {
                teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(teamChatBuddyApplication.getParamFromFile("Chatgpt_header","TeamChatBuddy.properties"))
                        .addOnSuccessListener(new OnSuccessListener<String>() {
                            @Override
                            public void onSuccess(String translatedText) {
                                teamChatBuddyApplication.setparam(teamChatBuddyApplication.getLangue().getNom()+"entete",translatedText);
                                set.setHeader(teamChatBuddyApplication.getparam(teamChatBuddyApplication.getLangue().getNom()+"entete"));
                                menu_header_editText.setText(teamChatBuddyApplication.getparam(teamChatBuddyApplication.getLangue().getNom()+"entete"));
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "translatedText exception  " + e);
                            }
                        });
            }
        }
        else{
            if (teamChatBuddyApplication.getLangue().getNom().equals("Anglais")){
                teamChatBuddyApplication.setparam("CustomGPT_header",teamChatBuddyApplication.getParamFromFile("CustomGPT_header","TeamChatBuddy.properties"));
                set.setHeader(teamChatBuddyApplication.getparam("CustomGPT_header"));
                menu_header_editText.setText(teamChatBuddyApplication.getparam("CustomGPT_header"));
            }
            else if(teamChatBuddyApplication.getLangue().getNom().equals("Français")){
                teamChatBuddyApplication.setparam("CustomGPT_entete",teamChatBuddyApplication.getParamFromFile("CustomGPT_entete","TeamChatBuddy.properties"));
                set.setHeader(teamChatBuddyApplication.getparam("CustomGPT_entete"));
                menu_header_editText.setText(teamChatBuddyApplication.getparam("CustomGPT_entete"));
            }
            else {
                teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(teamChatBuddyApplication.getParamFromFile("CustomGPT_header","TeamChatBuddy.properties"))
                        .addOnSuccessListener(new OnSuccessListener<String>() {
                            @Override
                            public void onSuccess(String translatedText) {
                                teamChatBuddyApplication.setparam(teamChatBuddyApplication.getLangue().getNom()+"CustomGPT_entete",translatedText);
                                set.setHeader(teamChatBuddyApplication.getparam(teamChatBuddyApplication.getLangue().getNom()+"CustomGPT_entete"));
                                menu_header_editText.setText(teamChatBuddyApplication.getparam(teamChatBuddyApplication.getLangue().getNom()+"CustomGPT_entete"));
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "translatedText exception  " + e);
                            }
                        });

            }
        }
    }
    public void btnDeleteHeader(View view){
        if (teamChatBuddyApplication.getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT")) {
            if (teamChatBuddyApplication.getLangue().getNom().equals("Anglais")){
                teamChatBuddyApplication.setparam(header,"");
                set.setHeader(teamChatBuddyApplication.getparam(header));
                menu_header_editText.setText(teamChatBuddyApplication.getparam(header));
            }
            else if(teamChatBuddyApplication.getLangue().getNom().equals("Français")){
                teamChatBuddyApplication.setparam(entete,"");
                set.setHeader(teamChatBuddyApplication.getparam(entete));
                menu_header_editText.setText(teamChatBuddyApplication.getparam(entete));
            }
            else {

                 teamChatBuddyApplication.setparam(teamChatBuddyApplication.getLangue().getNom()+"entete"," ");
                 Log.e("MRA_idetifyLanguage"," teamChatBuddyApplication.getparam("+teamChatBuddyApplication.getLangue().getNom()+"entete)= "+teamChatBuddyApplication.getparam(teamChatBuddyApplication.getLangue().getNom()+"entete"));
                  set.setHeader(teamChatBuddyApplication.getparam(teamChatBuddyApplication.getLangue().getNom()+"entete"));
                  menu_header_editText.setText(" ");
            }
        }
        else{
            if (teamChatBuddyApplication.getLangue().getNom().equals("Anglais")){
                teamChatBuddyApplication.setparam("CustomGPT_header","");
                set.setHeader(teamChatBuddyApplication.getparam("CustomGPT_header"));
                menu_header_editText.setText(teamChatBuddyApplication.getparam("CustomGPT_header"));
            }
            else if(teamChatBuddyApplication.getLangue().getNom().equals("Français")){
                teamChatBuddyApplication.setparam("CustomGPT_entete","");
                set.setHeader(teamChatBuddyApplication.getparam("CustomGPT_entete"));
                menu_header_editText.setText(teamChatBuddyApplication.getparam("CustomGPT_entete"));
            }
            else {

                teamChatBuddyApplication.setparam(teamChatBuddyApplication.getLangue().getNom()+"CustomGPT_entete"," ");
                set.setHeader(teamChatBuddyApplication.getparam(teamChatBuddyApplication.getLangue().getNom()+"CustomGPT_entete"));
                menu_header_editText.setText(" ");
            }
        }
    }
    @Override
    public void onLanguagesReceived(ArrayList<String> languages) {
        translateTitle(languages);
    }
    private void translateTitle(ArrayList<String> languages){
        if(teamChatBuddyApplication.getLangue().getNom().equals("Anglais")){
            showLanguageDialog(languages,getResources().getString(R.string.menu_dialog_supported_languages_Android_STT_en));
        }
        else if(teamChatBuddyApplication.getLangue().getNom().equals("Français")){
            showLanguageDialog(languages,getResources().getString(R.string.menu_dialog_supported_languages_Android_STT_fr));
        }
        else if(teamChatBuddyApplication.getLangue().getNom().equals("Espagnol")){
            showLanguageDialog(languages,getResources().getString(R.string.menu_dialog_supported_languages_Android_STT_es));
        }
        else if(teamChatBuddyApplication.getLangue().getNom().equals("Allemand")){
            showLanguageDialog(languages,getResources().getString(R.string.menu_dialog_supported_languages_Android_STT_de));
        }
        else {
            teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(getResources().getString(R.string.menu_dialog_supported_languages_Android_STT_en))
                    .addOnSuccessListener(new OnSuccessListener<String>() {
                        @Override
                        public void onSuccess(String translatedText) {
                            showLanguageDialog(languages,translatedText);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "translatedText exception  " + e);
                            showLanguageDialog(languages,getResources().getString(R.string.menu_dialog_supported_languages_Android_STT_en));

                        }
                    });
        }
    }
    private void showLanguageDialog(ArrayList<String> languages,String title) {


        TextView titleView = findViewById(R.id.dialogTitle);
        ListView listView = findViewById(R.id.dialogListView);


        titleView.setText(title);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, languages);
        listView.setAdapter(adapter);

        popupLanguageList.setVisibility(View.VISIBLE);
    }
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if ( v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent( event );
    }

    public static void avoidSpinnerDropdownFocus(Spinner spinner) {
        try {
            Field listPopupField = Spinner.class.getDeclaredField("mPopup");
            listPopupField.setAccessible(true);
            Object listPopup = listPopupField.get(spinner);
            if (listPopup instanceof ListPopupWindow) {
                Field popupField = ListPopupWindow.class.getDeclaredField("mPopup");
                popupField.setAccessible(true);
                Object popup = popupField.get((ListPopupWindow) listPopup);
                if (popup instanceof PopupWindow) {
                    ((PopupWindow) popup).setFocusable(false);
                }
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
    /**
     *   -------------------------------  Gestion d'affichage des barres du systemUI  ----------------------------------------------
     */

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            teamChatBuddyApplication.hideSystemUI(this);
        }
    }

    private void showToastMessage() {
        String language = teamChatBuddyApplication.getLangue().getNom();
        if (language.equals("en")) {
            currentToast = Toast.makeText(SettingsActivity.this, R.string.mlkit_model_is_downloading_en, Toast.LENGTH_SHORT);
        } else if (language.equals("fr")) {
            currentToast = Toast.makeText(SettingsActivity.this, R.string.mlkit_model_is_downloading_fr, Toast.LENGTH_SHORT);
        } else if (language.equals("es")) {
            currentToast = Toast.makeText(SettingsActivity.this, R.string.mlkit_model_is_downloading_es, Toast.LENGTH_SHORT);
        } else if (language.equals("de")) {
            currentToast = Toast.makeText(SettingsActivity.this, R.string.mlkit_model_is_downloading_de, Toast.LENGTH_SHORT);
        } else {
            currentToast = Toast.makeText(SettingsActivity.this, R.string.mlkit_model_is_downloading_en, Toast.LENGTH_SHORT);
        }
        currentToast.show();
    }
}