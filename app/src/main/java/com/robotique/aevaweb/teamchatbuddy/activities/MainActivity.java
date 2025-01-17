package com.robotique.aevaweb.teamchatbuddy.activities;

import static com.bfr.buddysdk.BuddySDK.USB;

import androidx.annotation.NonNull;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bfr.buddy.ui.shared.FaceTouchData;
import com.bfr.buddy.ui.shared.FacialExpression;
import com.bfr.buddy.ui.shared.GazePosition;
import com.bfr.buddy.ui.shared.IUIFaceTouchCallback;
import com.bfr.buddy.ui.shared.LabialExpression;
import com.bfr.buddy.usb.shared.IUsbCommadRsp;
import com.bfr.buddy.utils.events.EventItem;
import com.bfr.buddy.utils.values.FloatingWidgetVisibility;
import com.bfr.buddy.vision.shared.IVisionRsp;
import com.bfr.buddysdk.BuddyCompatActivity;
import com.bfr.buddysdk.BuddySDK;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.robotique.aevaweb.teamchatbuddy.R;
import com.robotique.aevaweb.teamchatbuddy.application.TeamChatBuddyApplication;
import com.robotique.aevaweb.teamchatbuddy.chatbotresponse.ChatGptStreamMode;
import com.robotique.aevaweb.teamchatbuddy.chatbotresponse.Commande;
import com.robotique.aevaweb.teamchatbuddy.chatbotresponse.CustomGPTStreamMode;
import com.robotique.aevaweb.teamchatbuddy.chatbotresponse.ResponseFromChatbot;
import com.robotique.aevaweb.teamchatbuddy.models.Langue;
import com.robotique.aevaweb.teamchatbuddy.models.Replica;
import com.robotique.aevaweb.teamchatbuddy.models.Session;
import com.robotique.aevaweb.teamchatbuddy.models.Setting;
import com.robotique.aevaweb.teamchatbuddy.observers.IDBObserver;
import com.robotique.aevaweb.teamchatbuddy.utilis.BIPlayer;
import com.robotique.aevaweb.teamchatbuddy.utilis.BlueMic;
import com.robotique.aevaweb.teamchatbuddy.utilis.CustomToast;
import com.robotique.aevaweb.teamchatbuddy.utilis.IBehaviourCallBack;
import com.robotique.aevaweb.teamchatbuddy.utilis.IMLKitDownloadCallback;
import com.robotique.aevaweb.teamchatbuddy.utilis.ITTSCallbacks;
import com.robotique.aevaweb.teamchatbuddy.utilis.TtsGoogleC;
import com.robotique.aevaweb.teamchatbuddy.utilis.WifiBroadcastReceiver;
import com.robotique.aevaweb.teamchatbuddy.utilis.tracking.MainViewModel;
import com.robotique.aevaweb.teamchatbuddy.utilis.tracking.OverlayView;
import com.robotique.aevaweb.teamchatbuddy.utilis.tracking.PoseLandmarkerHelper;
import com.robotique.aevaweb.teamchatbuddy.utilis.tracking.PoseTracking;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import java.util.StringTokenizer;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import darren.googlecloudtts.model.VoicesList;

public class MainActivity extends BuddyCompatActivity implements IDBObserver {

    private static final String TAG = "TEAMCHAT_BUDDY_MainActivity";
    private static final String TAG_TRACKING = "TEAMCHAT_BUDDY_TRACKING_INFO";
    private static final String TAG_TRACKING_DEBUG = "TEAMCHAT_BUDDY_TRACKING_DEBUG";
    private static final String[] REQUESTED_PERMISSIONS = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA

    };
    private static final int PERMISSION_REQ_ID = 22;
    private TeamChatBuddyApplication teamChatBuddyApplication;
    private View decorView;

    //views
    private RelativeLayout buddy_texte_qst_lyt;
    private RelativeLayout buddy_texte_resp_lyt;
    private RelativeLayout lyt_open_menu_settings;
    private RelativeLayout lyt_open_menu_chat;
    private RelativeLayout view_face;
    private RelativeLayout launch_view;
    private RelativeLayout reGroup;
    private RelativeLayout preViewViewLyt;
    private RelativeLayout photo_timer_bg_rlyt;
    private RelativeLayout photo_timer_rlyt;
    private FrameLayout preview_container;
    private OverlayView overlay;
    private PreviewView previewView;
    private PreviewView previewViewphoto;
    private TextView photo_timer_txtView;
    private TextView buddy_texte_qst;
    private TextView buddy_texte_resp;
    private ImageView noNetwork;
    private ProgressBar downloadingBar;
    private ImageView photo_timer_imgview;
    private ImageView bi_imageView;
    private PlayerView bi_videoView;


    private double TRACKING_WELCOME_TEMPERATURE;

    private long lastVisibleTime = 0L;          // Track the time when person was last seen
    private long lastVisibleTime_saved = 0L;    // Track the time when person was last seen and do not reset it when re-track (useful for invitation check)
    private long firstVisibleTime = 0L;         // Track the time when person started being visible
    private long visibleDuration = 0L;          // How long a person remained visible
    private long lastLookingAtCameraTime = 0L;  // Track the time when the person last looked at the camera
    private long lastLookingAtCameraTimeToCloseApp = 0L;  // Track the time when the person last looked at the camera
    private long personDetectedTimeToCloseApp = 0L; // Track the time when the person last detected at the camera
    private long totalTimeLookingAtCamera = 0L; // Total time spent looking at the camera
    private long startLookingAtCameraTime = 0L; // Track the start time of the current interval when the person is looking directly at the camera

    private Float[] res = {(float) 0, (float) 0, (float) 0, (float) 0, (float) 0, (float) 0, (float) 0, (float) 0, (float) 0, (float) 0,(float) 0, (float) 0, (float) 0};
    private float initLang=190F;
    private float degx,degy,x0,x2,x5,y0,y5,y2,lang,dLeft,eog,Eod, dRight;

    private int path= R.string.path;
    private int pathLog=R.string.pathConfig;
    private int cameraFacing = CameraSelector.LENS_FACING_BACK;
    private int TRACKING_DELAY_NO_WATCH;
    //private int TRACKING_DELAY_NO_TRACK;
    private int TRACKING_DELAY_START_LISTEN;
    private int TRACKING_DELAY_STOP_LISTEN;
    private int TRACKING_REGARD_CENTER;
    private int TRACKING_DELAY_WELCOME;
    private int TRACKING_DURATION_WELCOME;
    private int TRACKING_WELCOME_MAX_TOKEN;
    private int TRACKING_TIMEOUT;
    private int WATCHING_timeout;

    private String TRACKING_WELCOME_FR;
    private String TRACKING_WELCOME_EN;
    private String TRACKING_WELCOME_PROMPT_FR;
    private String TRACKING_WELCOME_PROMPT_EN;
    private String TRACKING_WELCOME_MODEL;
    private String TRACKING_WATCH;
    private String directionRegardNez= "";
    private String langueFr = "Français";
    private String langueEn = "Anglais";
    private String header ="header";
    private String entete ="entete";
    private String openAIKey = "openAI_API_Key";
    private String info_toast = "";
    private String gptResponse;

    private boolean onSdkReadyIsAlreadyCalledOnce = false;
    private boolean isListeningFreeSpeech = false;
    private Boolean mlKitIsDownloading = false;
    private boolean english_is_downloaded = false;
    private boolean french_is_downloaded = false;
    private boolean languageToEnglish_is_downloaded = false;
    private boolean isCMDLangue = false;
    private boolean isSpeaking = false;
    private Boolean gptSend=false;
    private boolean isFirstLaunch = true;
    private boolean regarde_camera=false;
    private boolean direction=false;
    private boolean isRecenter=false;
    private boolean deFace=false;
    private boolean isPersonDetected = false;
    private boolean wasPersonDetected = false;
    private boolean personIsVisible = false;
    private boolean sendInvitationPending = false;
    private boolean isProcessingReTrack = false;
    private boolean isTrackingAlreadyInitialised = false;
    private boolean isReTrack = false;
    private boolean isFirstInvitaion = false;

    private CountDownTimer timerEcoute;
    private CountDownTimer responseTimeout;
    private CountDownTimer timerDownloading;
    private CountDownTimer timerPhoto;
    private CountDownTimer timerToApplyBI;
    private static final Random random = new Random();
    private WifiBroadcastReceiver wifiBroadCastReceiver = new WifiBroadcastReceiver();
    private ArrayList<Replica> listRep=new ArrayList();
    private AudioManager amanager;
    private Dialog dialog;
    private ResponseFromChatbot responseFromChatbot;
    private Setting settingClass;
    private Commande commande;
    private PoseTracking poseTracking;
    private ExecutorService backgroundExecutor;
    private CameraSelector cameraSelector;
    private MainViewModel viewModel;
    private PoseLandmarkerHelper poseLandmarkerHelper;
    private ProcessCameraProvider cameraProvider;
    private Preview preview;
    private ImageAnalysis imageAnalyzer;
    private Camera camera;
    private AnimationDrawable animationTimerPhoto;
    private ImageCapture imageCapture;
    private String initOrMajOrNone="";
    private Boolean useListeningNumberWithAutomaicListening=false;
    private Boolean applyBIAfterDelay=false;
    private Boolean currentlyBIExecuted = false;
    private Boolean initHasBeenLaunchedFromOnResume = true;

    boolean isConnected = true;
    private Toast currentToast;
    private IStartMessageCallback iStartMessageCallback;
    private IMouthMessageCallback iMouthMessageCallback;
    public interface IStartMessageCallback {
        void onEnd(String s);
    }
    public interface IMouthMessageCallback {
        void onEnd(String s);
    }
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
                    case "languageToEnglish":
                        languageToEnglish_is_downloaded = true;
                        break;
                }
                if (english_is_downloaded && french_is_downloaded && languageToEnglish_is_downloaded) {
                    mlKitIsDownloading = false;
                    handlerProgressBar.removeCallbacksAndMessages(null);
                    handlerProgressBar.removeCallbacks(runnableProgressBar);
                    launch_view.setVisibility(View.INVISIBLE);
                    if(timerDownloading!=null){
                        timerDownloading.cancel();
                    }
                    if(currentToast!=null){
                        currentToast.cancel();
                    }
                    switch (initOrMajOrNone) {
                        case "INIT":
                            if (teamChatBuddyApplication.getCurrentLanguage().equals("en")) {
                                info_toast = getString(R.string.toast_config_file_init_en);
                                CustomToast.getInstance().showInfo(MainActivity.this, info_toast,2000);
                            }
                            else if (teamChatBuddyApplication.getCurrentLanguage().equals("fr")){
                                info_toast = getString(R.string.toast_config_file_init_fr);
                                CustomToast.getInstance().showInfo(MainActivity.this, info_toast,2000);
                            }
                            else if (teamChatBuddyApplication.getCurrentLanguage().equals("de")) {
                                info_toast = getString(R.string.toast_config_file_init_de);
                                CustomToast.getInstance().showInfo(MainActivity.this, info_toast,2000);
                            }
                            else if (teamChatBuddyApplication.getCurrentLanguage().equals("es")) {
                                info_toast = getString(R.string.toast_config_file_init_es);
                                CustomToast.getInstance().showInfo(MainActivity.this, info_toast,2000);
                            }
                            else{
                                teamChatBuddyApplication.getEnglishLanguageSelectedTranslator()
                                        .translate(getString(R.string.toast_config_file_init_en))
                                        .addOnSuccessListener(new OnSuccessListener<String>() {
                                            @Override
                                            public void onSuccess(String translatedText) {
                                                info_toast = translatedText;
                                                CustomToast.getInstance().showInfo(MainActivity.this, info_toast,2000);
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                info_toast = getString(R.string.toast_config_file_init_en);
                                                CustomToast.getInstance().showInfo(MainActivity.this, info_toast,2000);
                                            }
                                        });
                            }

                            break;
                        case "MAJ":
                            //traduire l'info du configFile :
                            if (teamChatBuddyApplication.getCurrentLanguage().equals("en")) {
                                info_toast = getString(R.string.toast_config_file_maj_en);
                                CustomToast.getInstance().showInfo(MainActivity.this, info_toast,2000);
                            }
                            else if (teamChatBuddyApplication.getCurrentLanguage().equals("fr")){
                                info_toast = getString(R.string.toast_config_file_maj_fr);
                                CustomToast.getInstance().showInfo(MainActivity.this, info_toast,2000);
                            }
                            else if (teamChatBuddyApplication.getCurrentLanguage().equals("de")) {
                                info_toast = getString(R.string.toast_config_file_maj_de);
                                CustomToast.getInstance().showInfo(MainActivity.this, info_toast,2000);
                            }
                            else if (teamChatBuddyApplication.getCurrentLanguage().equals("es")) {
                                info_toast = getString(R.string.toast_config_file_maj_es);
                                CustomToast.getInstance().showInfo(MainActivity.this, info_toast,2000);
                            }
                            else{
                                teamChatBuddyApplication.getEnglishLanguageSelectedTranslator()
                                        .translate(getString(R.string.toast_config_file_maj_en))
                                        .addOnSuccessListener(new OnSuccessListener<String>() {
                                            @Override
                                            public void onSuccess(String translatedText) {
                                                info_toast = translatedText;
                                                CustomToast.getInstance().showInfo(MainActivity.this, info_toast,2000);
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                info_toast = getString(R.string.toast_config_file_maj_en);
                                                CustomToast.getInstance().showInfo(MainActivity.this, info_toast,2000);
                                            }
                                        });
                            }

                            break;
                        case "NONE":
                            break;
                    }

                    if (isCMDLangue){
                        isCMDLangue = false;
                        commande.translate("CMD_LANGUE", new Commande.ITranslationCallback() {
                            @Override
                            public void onTranslated(String translatedText) {
                                String verifyMessage = commande.verifyCmdMessages(translatedText);
                                if(verifyMessage.equals("CONTAIN_BOTH_PARTS") || verifyMessage.equals("CONTAIN_ONLY_SECOND_PART") ){
                                    if (teamChatBuddyApplication.getParamFromFile("COMMAND_histo","TeamChatBuddy.properties")!=null && teamChatBuddyApplication.getParamFromFile("COMMAND_histo","TeamChatBuddy.properties").trim().equalsIgnoreCase("yes")) {
                                        try {
                                            // get the historic commandes :
                                            Log.e("DLA", "get the historic commandes ");
                                            String jsonArrayString = teamChatBuddyApplication.getparam("commandes");
                                            Log.e("DLA", "get the historic commandes  " + jsonArrayString);
                                            Log.e("DLA", "get the historic commandes 1");
                                            JSONArray existingHistoryArray = new JSONArray(jsonArrayString);
                                            Log.e("DLA", "get the historic commandes 2");
                                            JSONObject history1 = new JSONObject();
                                            history1.put("role", "assistant");
                                            history1.put("content", translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);

                                            existingHistoryArray.put(history1);
                                            // Stocker la nouvelle version de l'historique
                                            if (existingHistoryArray.length() > Integer.parseInt(teamChatBuddyApplication.getParamFromFile("COMMAND_maxdialog", "TeamChatBuddy.properties"))) {

                                                existingHistoryArray.remove(1);
                                                existingHistoryArray.remove(1);

                                                teamChatBuddyApplication.setparam("commandes", existingHistoryArray.toString());

                                            } else {
                                                teamChatBuddyApplication.setparam("commandes", existingHistoryArray.toString());
                                            }
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    teamChatBuddyApplication.notifyObservers("commandResponse;SPLIT;" +translatedText.split("\\s*/\\s*(?:/\\s*)?")[1]);
                                }
                            }
                        });
                    }
                    else if (isFirstLaunch && teamChatBuddyApplication.getParamFromFile("Start_Message","TeamChatBuddy.properties") !=null && !teamChatBuddyApplication.getParamFromFile("Start_Message","TeamChatBuddy.properties").isEmpty() &&teamChatBuddyApplication.getParamFromFile("Start_Message","TeamChatBuddy.properties").trim().equalsIgnoreCase("Yes")){
                       mlKitIsDownloading=true;
                        playStartMessage(new IStartMessageCallback() {
                            @Override
                            public void onEnd(String s) {
                                Log.e(TAG,"end of playing Start Message");
                                mlKitIsDownloading=false;
                                if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("no")){
                                    teamChatBuddyApplication.startListeningHotwor(MainActivity.this);
                                    reGroup.setTranslationY(1000);
                                }
                                else if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes")){
//                        if( !Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Auto_Listen"))){
                                    teamChatBuddyApplication.startListeningHotwor(MainActivity.this);
//                        }
                                    isReTrack = false;
                                    initTracking();
                                }
                            }
                        });
                    }
                    else if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("no")){
                        teamChatBuddyApplication.startListeningHotwor(MainActivity.this);
                        reGroup.setTranslationY(1000);
                    }
                    else if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes")){
//                        if( !Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Auto_Listen"))){
                            teamChatBuddyApplication.startListeningHotwor(MainActivity.this);
//                        }
                        isReTrack = false;
                        initTracking();
                    }
                    teamChatBuddyApplication.setparam("previousLanguage",new Gson().toJson(teamChatBuddyApplication.getLangue()));
                }
            }
            else{
                mlKitIsDownloading = true;
                french_is_downloaded = false;
                english_is_downloaded = false;
                languageToEnglish_is_downloaded =false;
                List<String> mlkitLangueCode = teamChatBuddyApplication.getLanguageCodeForDisponibleLangue("Language_Code_Used_In_Mlkit");
                String codeLanguageMlkit = mlkitLangueCode.get(new Gson().fromJson(teamChatBuddyApplication.getparam(settingClass.getLangue()), Langue.class).getId()-1);
                teamChatBuddyApplication.downloadModel(imlKitDownloadCallback,codeLanguageMlkit.trim());
                handlerProgressBar.postDelayed(runnableProgressBar,500);
                timerDownloading.start();
            }
        }
    };
    private Handler handlerProgressBar = new Handler(Looper.getMainLooper());
    private Runnable runnableProgressBar = new Runnable() {
        @Override
        public void run() {
            teamChatBuddyApplication.stopTTS();
            stopListeningFreeSpeech();
            try {
                BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                BuddySDK.UI.stopListenAnimation();
            } catch (Exception e) {
                Log.e(TAG, "BuddySDK Exception  " + e);
            }
            launch_view.setVisibility(View.VISIBLE);
        }
    };
    private Handler handlerForSensor;
    private Runnable runnableForSensor;
    private Handler handlerTTSError = new Handler();
    private Runnable runnableTTSError;
    private Handler handlerPauseTime = new Handler();
    private Runnable runnablePauseTime;
    private Handler handler = new Handler();
    private Runnable runnable;
    private int clickCount = 0;
    private static final int CLICK_TIMEOUT = 2000;
    private Handler clickHandler = new Handler();
    private Runnable resetClickCountRunnable = new Runnable() {
        @Override
        public void run() {
            clickCount = 0;
        }
    };
    private Boolean isFirstResponse = true;
    private Boolean isQuestionAlreadyDetected = false;

    /**
     * ------------------ App LifeCycle ---------------------
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initHasBeenLaunchedFromOnResume = false;

        Log.d(TAG," --- onCreate() ---");
        teamChatBuddyApplication = (TeamChatBuddyApplication) getApplicationContext();
        teamChatBuddyApplication.setInitSharedpreferences(true);
        teamChatBuddyApplication.hideSystemUI(this);
        decorView=getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if(visibility==0){
                    decorView.setSystemUiVisibility(teamChatBuddyApplication.hideSystemUI(MainActivity.this));
                }
            }
        });

        //init views
        buddy_texte_qst = findViewById( R.id.buddy_texte_qst );
        buddy_texte_qst_lyt = findViewById( R.id.buddy_texte_qst_lyt );
        buddy_texte_resp = findViewById( R.id.buddy_texte_resp );
        buddy_texte_resp_lyt = findViewById( R.id.buddy_texte_resp_lyt );
        lyt_open_menu_settings = findViewById( R.id.lyt_open_menu_settings );
        lyt_open_menu_chat = findViewById( R.id.lyt_open_menu_chat );
        view_face = findViewById(R.id.view_face);
        launch_view = findViewById(R.id.launch_view);
        noNetwork = findViewById(R.id.noNetwork);
        bi_videoView = findViewById(R.id.bi_videoView);
        bi_imageView = findViewById(R.id.bi_imageView);
        downloadingBar = findViewById(R.id.progressBar_MLKitDownload);
        preview_container = findViewById(R.id.preview_container);
        reGroup = findViewById(R.id.reGroup);
        overlay = findViewById(R.id.overlay);
        previewView = findViewById(R.id.view_finder);
        photo_timer_bg_rlyt= findViewById(R.id.photo_timer_bg_rlyt);
        photo_timer_rlyt = findViewById(R.id.photo_timer_rlyt);
        photo_timer_imgview = findViewById(R.id.photo_timer_imgview);
        photo_timer_txtView = findViewById(R.id.photo_timer_txtView);

        amanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        amanager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true);

        Intent myIntent = getIntent();
        isFirstLaunch = true;
        if (myIntent != null) {
            if (myIntent.hasExtra("fromSettings")) {
                String fromString = myIntent.getStringExtra("fromSettings");
                if (fromString != null && fromString.equals("true")) {
                    isFirstLaunch= false;
                    Log.i(TAG_TRACKING,"is back from Settings");
                }
            }
            else if(myIntent.hasExtra("fromChatWindow")){
                String fromChatWindow = myIntent.getStringExtra("fromChatWindow");
                if (fromChatWindow != null && fromChatWindow.equals("true")) {
                    isFirstLaunch= false;
                    Log.i(TAG_TRACKING,"is back from ChatWindow");
                }
            }
            else {
                teamChatBuddyApplication.setSpeaking(false);
                teamChatBuddyApplication.setNotYet(true);
                teamChatBuddyApplication.setActivityClosed(false);
                teamChatBuddyApplication.setStartRecording(false);
                teamChatBuddyApplication.setUsingEmotions(false);
                teamChatBuddyApplication.setQuestionNumber(0);
                teamChatBuddyApplication.setCurrentQuestionNubmer(0);
                teamChatBuddyApplication.setAlreadyGetAnswer(false);
                teamChatBuddyApplication.setOpenaialreadySwitchEmotion(false);
                teamChatBuddyApplication.setTimeoutExpired(false);
                teamChatBuddyApplication.setQuestionTime(0);
                teamChatBuddyApplication.setStoredResponse("");
                teamChatBuddyApplication.setBuddyFaceisTired(false);
                teamChatBuddyApplication.setShouldPlayEmotion(false);
                teamChatBuddyApplication.setCurrentEmotion("");
                teamChatBuddyApplication.setMessageError(false);
                teamChatBuddyApplication.setCurrentIndexText(0);
                teamChatBuddyApplication.setAllTextPronoucedSuccess(true);
                teamChatBuddyApplication.setStop_TTS_ReadSpeaker(false);
                teamChatBuddyApplication.setInitSharedpreferences(true);
                teamChatBuddyApplication.setLanguageDetected("");
                teamChatBuddyApplication.setAlreadyCalled(false);
                teamChatBuddyApplication.setRecording(false);
                teamChatBuddyApplication.setCurrentState("");


                teamChatBuddyApplication.setStopProcessus(false);
                teamChatBuddyApplication.setAlReadyHadSpoke(false);

                teamChatBuddyApplication.setGetResponseTime(0);
                teamChatBuddyApplication.setAnswerHasExceededTimeOut(false);
                teamChatBuddyApplication.setPreviousVolume(Float.valueOf(0));
                teamChatBuddyApplication.setAppIsListeningToTheQuestion(false);
                teamChatBuddyApplication.setChosenTTS("");
                teamChatBuddyApplication.setAppIsCurrentlyDealingWithTheQuestion(false);
                teamChatBuddyApplication.setBIExecution(false);
                teamChatBuddyApplication.setAlreadyChatting(false);
                teamChatBuddyApplication.setShouldLaunchListeningAfterGetingHotWord(true);
                Log.i(TAG,"First launch of application myIntent!=null 2");
            }
        }
        else {
            teamChatBuddyApplication.setSpeaking(false);
            teamChatBuddyApplication.setNotYet(true);
           teamChatBuddyApplication.setActivityClosed(false);
            teamChatBuddyApplication.setStartRecording(false);
            teamChatBuddyApplication.setUsingEmotions(false);
            teamChatBuddyApplication.setQuestionNumber(0);
            teamChatBuddyApplication.setCurrentQuestionNubmer(0);
            teamChatBuddyApplication.setAlreadyGetAnswer(false);
            teamChatBuddyApplication.setOpenaialreadySwitchEmotion(false);
            teamChatBuddyApplication.setTimeoutExpired(false);
            teamChatBuddyApplication.setQuestionTime(0);
            teamChatBuddyApplication.setStoredResponse("");
            teamChatBuddyApplication.setBuddyFaceisTired(false);
            teamChatBuddyApplication.setShouldPlayEmotion(false);
            teamChatBuddyApplication.setCurrentEmotion("");
            teamChatBuddyApplication.setMessageError(false);
             teamChatBuddyApplication.setCurrentIndexText(0);
             teamChatBuddyApplication.setAllTextPronoucedSuccess(true);
            teamChatBuddyApplication.setStop_TTS_ReadSpeaker(false);
            teamChatBuddyApplication.setInitSharedpreferences(true);
            teamChatBuddyApplication.setLanguageDetected("");
            teamChatBuddyApplication.setAlreadyCalled(false);
            teamChatBuddyApplication.setRecording(false);
            teamChatBuddyApplication.setCurrentState("");

            teamChatBuddyApplication.setStopProcessus(false);
            teamChatBuddyApplication.setAlReadyHadSpoke(false);

            teamChatBuddyApplication.setGetResponseTime(0);
            teamChatBuddyApplication.setAnswerHasExceededTimeOut(false);
            teamChatBuddyApplication.setPreviousVolume(Float.valueOf(0));
            teamChatBuddyApplication.setAppIsListeningToTheQuestion(false);
            teamChatBuddyApplication.setChosenTTS("");
            teamChatBuddyApplication.setAppIsCurrentlyDealingWithTheQuestion(false);
            teamChatBuddyApplication.setBIExecution(false);
            teamChatBuddyApplication.setAlreadyChatting(false);
            teamChatBuddyApplication.setShouldLaunchListeningAfterGetingHotWord(true);
            Log.i(TAG,"First launch of application");
        }

        /**
         * init animated drawables for timer
         */
        animationTimerPhoto = new AnimationDrawable();
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0001), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0002), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0003), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0004), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0005), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0006), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0007), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0008), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0009), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0010), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0011), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0012), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0013), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0014), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0015), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0016), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0017), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0018), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0019), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0020), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0021), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0022), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0023), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0024), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0025), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0026), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0027), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0028), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0029), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0030), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0031), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0032), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0033), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0034), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0035), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0036), 1000/37);
        animationTimerPhoto.addFrame(getResources().getDrawable(R.drawable.loadingspin0037), 1000/37);

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG," --- onResume() ---");
        teamChatBuddyApplication.hideSystemUI(this);
        teamChatBuddyApplication.setAppIsCurrentlyDealingWithTheQuestion(false);
        if (! Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        currentTrackingListeningState = StateTrackingListening.PERSON_IS_NOT_VISIBLE_TIMEOUT;
        totalTimeLookingAtCamera = 0L;
        if (initHasBeenLaunchedFromOnResume){
            if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
                    checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID) &&
                    checkSelfPermission(REQUESTED_PERMISSIONS[2], PERMISSION_REQ_ID) &&
                    checkSelfPermission(REQUESTED_PERMISSIONS[3], PERMISSION_REQ_ID)

            ){
                init();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG," --- onPause() ---");
        initHasBeenLaunchedFromOnResume=true;
        if (responseTimeout!=null) responseTimeout.cancel();
        if (timerToApplyBI!=null) timerToApplyBI.cancel();
        if(teamChatBuddyApplication.getChatGptStreamMode() != null){
            teamChatBuddyApplication.getChatGptStreamMode().reset();
        }
        if(teamChatBuddyApplication.getCustomGPTStreamMode() != null){
            teamChatBuddyApplication.getCustomGPTStreamMode().reset();
        }
        if(handlerTTSError!=null && runnableTTSError!=null){
            handlerTTSError.removeCallbacks(runnableTTSError);
            handlerTTSError.removeCallbacksAndMessages(null);
        }
        if (handlerForSensor != null && runnableForSensor != null) {

            handlerForSensor.removeCallbacksAndMessages(null);
            handlerForSensor.removeCallbacks(runnableForSensor);

        }
        if(handler!=null && runnable!=null){
            handler.removeCallbacks(runnable);
            handler.removeCallbacksAndMessages(null);
        }
        onSdkReadyIsAlreadyCalledOnce = false;
        isListeningFreeSpeech = false;
        listRep=new ArrayList();
        gptSend=false;
        teamChatBuddyApplication.removeObserver(this);
        teamChatBuddyApplication.stopTTS();
        teamChatBuddyApplication.setActivityClosed(true);
        teamChatBuddyApplication.setStartRecording(false);
        teamChatBuddyApplication.setSpeaking(false);
        teamChatBuddyApplication.setShouldLaunchListeningAfterGetingHotWord(true);
        teamChatBuddyApplication.setModeContinuousListeningON(false);
        stopListeningFreeSpeech();
        CustomToast.getInstance().hideToast();
        try {
            BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
            BuddySDK.UI.removeFaceTouchListener(iuiFaceTouchCallback);
        }
        catch (Exception e){
            Log.e(TAG,"BuddySDK Exception  "+e);
        }
        if(cameraProvider != null) cameraProvider.unbindAll();
        isFirstLaunch= false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG," --- onDestroy() ---");
        closeImage();
        try {
            unregisterReceiver(wifiBroadCastReceiver);
        }catch(IllegalArgumentException e) {
            Log.i(TAG,"---unregisterReceiver wifiBroadcast:: IllegalArgumentException---"+e.getMessage());
        }
        teamChatBuddyApplication.setparam("firstLaunch","true");
        teamChatBuddyApplication.setFileCreate(true);
        teamChatBuddyApplication.notifyObservers("main destroy");
        handlerCheckPersonDetection.removeCallbacks(runnableCheckPersonDetection);
        handlerCheckPersonDetection.removeCallbacksAndMessages(null);
        if(poseTracking != null) poseTracking.stopMovingAndCancelRunnables();
        if(backgroundExecutor != null) backgroundExecutor.shutdownNow();
        try{
            if(!BuddySDK.Actuators.getLeftWheelStatus().toUpperCase().contains("DISABLE") || !BuddySDK.Actuators.getRightWheelStatus().toUpperCase().contains("DISABLE")) {
                BuddySDK.USB.enableWheels(false, iUsbCommadRspTracking);
            }
            if(!BuddySDK.Actuators.getYesStatus().toUpperCase().contains("DISABLE")) {
                BuddySDK.USB.enableYesMove(false, iUsbCommadRspTracking);
            }
            if(!BuddySDK.Actuators.getNoStatus().toUpperCase().contains("DISABLE")) {
                BuddySDK.USB.enableNoMove(false, iUsbCommadRspTracking);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        if (Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Head"))) {
            BuddySDK.USB.buddyStopNoMove(iUsbCommadRspTracking);
            BuddySDK.USB.buddyStopYesMove(iUsbCommadRspTracking);
        }
        downloadingBar.setVisibility(View.GONE);
        launch_view.setVisibility(View.GONE);
        if(currentToast!=null){
            currentToast.cancel();
        }
        if (SettingsActivity.modelDownloading || mlKitIsDownloading) {
            if(teamChatBuddyApplication.getparam("previousLanguage")!=null && !teamChatBuddyApplication.getparam("previousLanguage").trim().equals("")) {
                setPreviousLanguage();
            }
            if(timerDownloading!=null){
                timerDownloading.cancel();
            }
        }
    }

    /**
     * ------------------ Register to the SDK callbacks ---------------------
     */

    public void onSDKReady() {
        Log.w(TAG, "onSDKReady");
        initHasBeenLaunchedFromOnResume=false;
        if(!onSdkReadyIsAlreadyCalledOnce){
            //initialisation du visage de Buddy
            BuddySDK.UI.setFaceEnergy(1.0f);
            BuddySDK.UI.setFacePositivity(1.0f);
            BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL,1);
            BuddySDK.UI.lookAt(GazePosition.CENTER, true);
            BuddySDK.UI.stopListenAnimation();
            BuddySDK.UI.setViewAsFace(view_face);
            BuddySDK.UI.setMenuWidgetVisibility(FloatingWidgetVisibility.ALWAYS);
            BuddySDK.UI.setCloseWidgetVisibility(FloatingWidgetVisibility.ALWAYS);

            if(Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Head"))){
                USB.enableYesMove( true, iUsbCommadRspTracking);
                USB.enableNoMove(true, iUsbCommadRspTracking);
            }

            //Désactiver le trigger Companion de la bouche et de OK BUDDY
            BuddySDK.Companion.raiseEvent("disableOkBuddy");
            BuddySDK.Companion.raiseEvent("disableOnMouth");


            BuddySDK.Vision.stopCamera(0, new IVisionRsp.Stub() {
                @Override
                public void onSuccess(String s) throws RemoteException {
                    Log.i(TAG_TRACKING, "stopCamera(0) onSuccess : " + s);
                }
                @Override
                public void onFailed(String s) throws RemoteException {
                    Log.e(TAG_TRACKING, "stopCamera(0) onFailed : " + s);
                }
            });

            if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
                    checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID) &&
                    checkSelfPermission(REQUESTED_PERMISSIONS[2], PERMISSION_REQ_ID) &&
                    checkSelfPermission(REQUESTED_PERMISSIONS[3], PERMISSION_REQ_ID)

            ){
                init();
            }
        }
        onSdkReadyIsAlreadyCalledOnce = true;
    }

    public void onEvent(EventItem iEvent){
        Log.w(TAG, "onEvent : "+iEvent.toString());
    }

    private final IUsbCommadRsp iUsbCommadRspBI = new IUsbCommadRsp.Stub(){
        @Override
        public void onSuccess(String s) throws RemoteException {
            Log.e("DEBUG_BI","detectBI onSuccess");
            runnableForSensor= new Runnable() {
                public void run() {
                    detectBI();
                    handlerForSensor.postDelayed(this, 100);
                }
            };
            handlerForSensor.post(runnableForSensor);
        }
        @Override
        public void onFailed(String s) throws RemoteException {
            Log.e("DEBUG_BI","detectBI onFailed");
        }
    };

    private final IUsbCommadRsp iUsbCommadRspTracking = new IUsbCommadRsp.Stub(){
        @Override
        public void onSuccess(String s) throws RemoteException {}
        @Override
        public void onFailed(String s) throws RemoteException {}
    };

    private final IUIFaceTouchCallback iuiFaceTouchCallback = new IUIFaceTouchCallback.Stub() {
        @Override
        public void onTouch(FaceTouchData faceTouchData) throws RemoteException {

            //Right_Eyebrow
            if(faceTouchData.getX() < 460 &&  faceTouchData.getY() < 250){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (Integer.parseInt(teamChatBuddyApplication.getparam("speak_volume"))>0){
                            int curr = Integer.parseInt(teamChatBuddyApplication.getparam("speak_volume")) - 10;
                            if (curr<0){
                                curr=0;
                            }
                            teamChatBuddyApplication.setVolume(curr,AudioManager.FLAG_SHOW_UI);
                            teamChatBuddyApplication.setparam("speak_volume", String.valueOf(curr));
                            teamChatBuddyApplication.setSpeakVolume(curr);
                            settingClass.setVolume(String.valueOf(curr));
                            Log.d(TAG, "RIGHT_EYE update volume : " + curr);
                        }
                        else{
                            Log.d(TAG, "Volume MIN");
                        }
                    }
                });
            }

            //Left_Eyebrow
            else if(faceTouchData.getX() > 720 &&  faceTouchData.getY() < 250){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (Integer.parseInt(teamChatBuddyApplication.getparam("speak_volume"))<100){
                            int curr = Integer.parseInt(teamChatBuddyApplication.getparam("speak_volume")) + 10;
                            if (curr>100){
                                curr=100;
                            }
                            teamChatBuddyApplication.setVolume(curr,AudioManager.FLAG_SHOW_UI);
                            teamChatBuddyApplication.setparam("speak_volume", String.valueOf(curr));
                            teamChatBuddyApplication.setSpeakVolume(curr);
                            settingClass.setVolume(String.valueOf(curr));
                            Log.d(TAG, "LEFT_EYE update volume : " + curr);
                        }
                        else{
                            Log.d(TAG, "Volume MAX");
                        }
                    }
                });
            }

            //eyes
            else if (faceTouchData.getY() > 250 && faceTouchData.getY() < 568){
                Log.e("FCHH","click1");
                if (teamChatBuddyApplication.getparam("Stimulis").contains("yes")) {
                    if (!teamChatBuddyApplication.getAppIsCurrentlyDealingWithTheQuestion() && !currentlyBIExecuted) {
                        if (faceTouchData.getX()>200 && faceTouchData.getX()<565){
                            if (teamChatBuddyApplication.getParamFromFile("touchLeftEye_Behavior", "TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.getParamFromFile("touchLeftEye_Behavior", "TeamChatBuddy.properties").trim().equalsIgnoreCase("")) {
                                executeBI("touchLeftEye_Behavior");
                            }
                        }
                        else if (faceTouchData.getX()>773 && faceTouchData.getX()<1120){
                            if (teamChatBuddyApplication.getParamFromFile("touchRightEye_Behavior", "TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.getParamFromFile("touchRightEye_Behavior", "TeamChatBuddy.properties").trim().equalsIgnoreCase("")) {
                                executeBI("touchRightEye_Behavior");
                            }
                        }
                        else {
                            if (teamChatBuddyApplication.getParamFromFile("touchFace_Behavior", "TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.getParamFromFile("touchFace_Behavior", "TeamChatBuddy.properties").trim().equalsIgnoreCase("")) {
                                executeBI("touchFace_Behavior");
                            }
                        }
                    }
                }
            }

            //Mouth
            else if(faceTouchData.getX() > 400 && faceTouchData.getX() < 820 &&  faceTouchData.getY() > 568){
                runOnUiThread( new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "Mouth touched");
                        String mouth_messages = teamChatBuddyApplication.getParamFromFile("Mouth_messages", "TeamChatBuddy.properties");
                        isSpeaking =false;
                        if(iInvitationCallback != null) iInvitationCallback.onEnd("INVITATION_END");
                        if(handler!=null && runnable!=null){
                            handler.removeCallbacks(runnable);
                            handler.removeCallbacksAndMessages(null);
                        }
                        if (responseTimeout!=null) responseTimeout.cancel();
                        if(teamChatBuddyApplication.getChatGptStreamMode() != null){
                            teamChatBuddyApplication.getChatGptStreamMode().reset();
                        }
                        if(teamChatBuddyApplication.getCustomGPTStreamMode() != null){
                            teamChatBuddyApplication.getCustomGPTStreamMode().reset();
                        }
                        if(handlerTTSError!=null && runnableTTSError!=null){
                            handlerTTSError.removeCallbacks(runnableTTSError);
                            handlerTTSError.removeCallbacksAndMessages(null);
                        }
                        if (teamChatBuddyApplication.getBIExecution()){
                            BIPlayer.getInstance().stopBehaviour();
                        }
                       else if (!teamChatBuddyApplication.getSpeaking() && !mlKitIsDownloading){
                            teamChatBuddyApplication.setStartRecording(true);
                            teamChatBuddyApplication.setSpeaking(true);
                            teamChatBuddyApplication.setModeContinuousListeningON(false);
                            if(!isListeningFreeSpeech ) {
                                if(mouth_messages!=null && mouth_messages.equalsIgnoreCase("Yes")){
                                    speakMouthMessages("listen", new IMouthMessageCallback() {
                                        @Override
                                        public void onEnd(String s) {
                                            teamChatBuddyApplication.setSpeaking(true);
                                        }
                                    });
                                }
                                else{
                                    isListeningFreeSpeech=true;
                                    teamChatBuddyApplication.setActivityClosed(false);
                                    startListeningFreeSpeech(teamChatBuddyApplication.getListeningDuration());
                                }
                            }
                        }
                        else if (teamChatBuddyApplication.getSpeaking() && !mlKitIsDownloading) {
                            currentTrackingListeningState = StateTrackingListening.PERSON_IS_NOT_VISIBLE_TIMEOUT;
                            totalTimeLookingAtCamera = 0L;
                            if (teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Android") || teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Cerence") || !teamChatBuddyApplication.getAppIsListeningToTheQuestion() || teamChatBuddyApplication.getParamFromFile("Processing_the_audio_sequence","TeamChatBuddy.properties").trim().equalsIgnoreCase("No")) {
                                BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                                teamChatBuddyApplication.setStartRecording(false);
                                teamChatBuddyApplication.setSpeaking(false);
                                teamChatBuddyApplication.setActivityClosed(true);
                                isListeningFreeSpeech = false;
                                teamChatBuddyApplication.stopTTS();
                                teamChatBuddyApplication.setStoredResponse("");
                                if (buddy_texte_qst_lyt != null && buddy_texte_resp_lyt != null && buddy_texte_qst != null && buddy_texte_resp != null) {
                                    buddy_texte_qst_lyt.setVisibility(View.INVISIBLE);
                                    buddy_texte_resp_lyt.setVisibility(View.INVISIBLE);
                                    buddy_texte_qst.setMovementMethod(null);
                                    buddy_texte_resp.setMovementMethod(null);
                                }
                                if (teamChatBuddyApplication.getParamFromFile("Options_Access","TeamChatBuddy.properties")!=null &&
                                        teamChatBuddyApplication.getParamFromFile("Options_Access","TeamChatBuddy.properties").trim().equalsIgnoreCase("Yes")){
                                    if (teamChatBuddyApplication.getParamFromFile("Number_clicks_options","TeamChatBuddy.properties")!=null &&
                                    Integer.parseInt(teamChatBuddyApplication.getParamFromFile("Number_clicks_options","TeamChatBuddy.properties"))!=0){
                                        lyt_open_menu_settings.setVisibility(View.VISIBLE);
                                    }
                                    else if (teamChatBuddyApplication.getParamFromFile("Number_clicks_options","TeamChatBuddy.properties")==null){
                                        lyt_open_menu_settings.setVisibility(View.VISIBLE);
                                    }

                                }
                                lyt_open_menu_chat.setVisibility(View.VISIBLE);
                                try {
                                    BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                                } catch (Exception e) {
                                    Log.e(TAG, "BuddySDK Exception  " + e);
                                }
                                try {
                                    BuddySDK.UI.stopListenAnimation();
                                } catch (Exception e) {
                                    Log.e(TAG, "BuddySDK Exception  " + e);
                                }
                                teamChatBuddyApplication.notifyObservers("end of timer");
                            }
                            else{
                                teamChatBuddyApplication.setLed("Neutral");
                                BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                                try {
                                    BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                                    BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                                    BuddySDK.UI.stopListenAnimation();
                                } catch (Exception e) {
                                    Log.e(TAG, "BuddySDK Exception  " + e);
                                }
                                BuddySDK.UI.stopListenAnimation();
                                teamChatBuddyApplication.setAppIsListeningToTheQuestion(false);
                                teamChatBuddyApplication.traitementAudio(false);
                            }
                            if(mouth_messages!=null && mouth_messages.equalsIgnoreCase("Yes")){
                                speakMouthMessages("stop", new IMouthMessageCallback() {
                                    @Override
                                    public void onEnd(String s) {
                                    }
                                });
                            }
                        }
                    }
                });
            }
        }
        @Override
        public void onRelease(FaceTouchData faceTouchData) throws RemoteException {}
    };

    public void speakMouthMessages(String type, IMouthMessageCallback iMouthMessageCallback){
        this.iMouthMessageCallback = iMouthMessageCallback;
        if(type.equals("listen")){
            String mouth_listen_fr = teamChatBuddyApplication.getParamFromFile("Mouth_listen_fr", "TeamChatBuddy.properties");
            String mouth_listen_en =  teamChatBuddyApplication.getParamFromFile("Mouth_listen_en", "TeamChatBuddy.properties");

            if (teamChatBuddyApplication.getLangue().getNom().equals("Anglais")) {
                if(mouth_listen_en != null && !mouth_listen_en.isEmpty()){
                    String[] englishMessages = mouth_listen_en.substring(1, mouth_listen_en.length() - 1).split("/");
                    String randomMessageEN = englishMessages[random.nextInt(englishMessages.length)];
                    Log.d(TAG_TRACKING, "Random English Message: " + randomMessageEN);
                    teamChatBuddyApplication.setActivityClosed(false);
                    speak(randomMessageEN, "INVITATION");
                }
                else {
                    if(iMouthMessageCallback != null) iMouthMessageCallback.onEnd("ConfigFile do not contain English Invitation");
                }
            }
            else if (teamChatBuddyApplication.getLangue().getNom().equals("Français")) {
                if(mouth_listen_fr != null && !mouth_listen_fr.isEmpty()){
                    String[] frenchMessages = mouth_listen_fr.substring(1, mouth_listen_fr.length() - 1).split("/");
                    String randomMessagesFR = frenchMessages[random.nextInt(frenchMessages.length)];
                    Log.d(TAG_TRACKING, "Random French Invitation: " + randomMessagesFR);
                    teamChatBuddyApplication.setActivityClosed(false);
                    speak(randomMessagesFR, "INVITATION");
                }
                else {
                    if(iMouthMessageCallback != null) iMouthMessageCallback.onEnd("ConfigFile do not contain French Invitation");
                }
            }
            else {
                if(mouth_listen_en != null && !mouth_listen_en.isEmpty()){
                    String[] englishMessages = mouth_listen_en.substring(1, mouth_listen_en.length() - 1).split("/");
                    String randomMessagesEN = englishMessages[random.nextInt(englishMessages.length)];
                    teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(randomMessagesEN)
                            .addOnSuccessListener(new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(String translatedText) {
                                    Log.d(TAG_TRACKING, "Translated Invitation: " + translatedText);
                                    teamChatBuddyApplication.setActivityClosed(false);
                                    speak(translatedText, "INVITATION");
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.d(TAG_TRACKING, "Translation failed, using English message : " + randomMessagesEN);
                                    teamChatBuddyApplication.setActivityClosed(false);
                                    speak(randomMessagesEN, "INVITATION");
                                }
                            });
                }
                else {
                    if(iMouthMessageCallback != null) iMouthMessageCallback.onEnd("ConfigFile do not contain English Invitation");
                }
            }
        }
        else if( type.equals("stop")){
            String Mouth_messages_fr = teamChatBuddyApplication.getParamFromFile("Mouth_messages_fr", "TeamChatBuddy.properties");
            String Mouth_messages_en =  teamChatBuddyApplication.getParamFromFile("Mouth_messages_en", "TeamChatBuddy.properties");

            if (teamChatBuddyApplication.getLangue().getNom().equals("Anglais")) {
                if(Mouth_messages_en != null && !Mouth_messages_en.isEmpty()){
                    String[] englishMessages = Mouth_messages_en.substring(1, Mouth_messages_en.length() - 1).split("/");
                    String randomMessagesEN = englishMessages[random.nextInt(englishMessages.length)];
                    Log.d(TAG_TRACKING, "Random English Invitation: " + randomMessagesEN);
                    teamChatBuddyApplication.setActivityClosed(false);
                    speak(randomMessagesEN, "INVITATION");
                }
                else {
                    if(iMouthMessageCallback != null) iMouthMessageCallback.onEnd("ConfigFile do not contain English Invitation");
                }
            }
            else if (teamChatBuddyApplication.getLangue().getNom().equals("Français")) {
                if(Mouth_messages_fr != null && !Mouth_messages_fr.isEmpty()){
                    String[] frenchMessages = Mouth_messages_fr.substring(1, Mouth_messages_fr.length() - 1).split("/");
                    String randomMessagesFR = frenchMessages[random.nextInt(frenchMessages.length)];
                    teamChatBuddyApplication.setActivityClosed(false);
                    speak(randomMessagesFR, "INVITATION");
                }
                else {
                    if(iMouthMessageCallback != null) iMouthMessageCallback.onEnd("ConfigFile do not contain French Invitation");
                }
            }
            else {
                if(Mouth_messages_en != null && !Mouth_messages_en.isEmpty()){
                    String[] englishMessages = Mouth_messages_en.substring(1, Mouth_messages_en.length() - 1).split("/");
                    String randomMessagesEN = englishMessages[random.nextInt(englishMessages.length)];
                    teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(randomMessagesEN)
                            .addOnSuccessListener(new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(String translatedText) {
                                    Log.d(TAG_TRACKING, "Translated Invitation: " + translatedText);
                                    teamChatBuddyApplication.setActivityClosed(false);
                                    speak(translatedText, "INVITATION");
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.d(TAG_TRACKING, "Translation failed, using English Invitation: " + randomMessagesEN);
                                    teamChatBuddyApplication.setActivityClosed(false);
                                    speak(randomMessagesEN, "INVITATION");
                                }
                            });
                }
                else {
                    if(iMouthMessageCallback != null) iMouthMessageCallback.onEnd("ConfigFile do not contain English Invitation");
                }
            }
        }
    }
    /**
     * ----------------- Gestion de notifications ---------------------------
     */
    @Override
    public void update(String message) throws IOException {
        if (message != null) {

            if (message.contains("CANCEL_RESPONSE_TIMEOUT")) {
                if (responseTimeout!=null) responseTimeout.cancel();
            }

            else if (message.contains("MODE_STREAM_TEXT;SPLIT;")) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (message.split(";SPLIT;").length > 1) {
                            String response = message.split(";SPLIT;")[1];
                            //translate Response Title and show the response :
                            if (teamChatBuddyApplication.getCurrentLanguage().equals("en")) {
                                showStream("Response",response);
                            }
                            else if (teamChatBuddyApplication.getCurrentLanguage().equals("fr")){
                                showStream("Réponse",response);
                            }
                            else if (teamChatBuddyApplication.getCurrentLanguage().equals("de")) {
                                showStream("Antwort",response);
                            }
                            else if (teamChatBuddyApplication.getCurrentLanguage().equals("es")) {
                                showStream("Respuesta",response);
                            }
                            else{
                                teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate("Response")
                                        .addOnSuccessListener(new OnSuccessListener<String>() {
                                            @Override
                                            public void onSuccess(String translatedText) {
                                                showStream(translatedText,response);
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                showStream("Response",response);
                                            }
                                        });
                            }
                        }
                    }
                });
            }

            else if (message.contains("MODE_STREAM_SPEAK;SPLIT;")){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (message.split(";SPLIT;").length > 1){
                            String phraseToPronounce = message.split(";SPLIT;")[1];
                            speak(phraseToPronounce, "nothealysa");
                            if (teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes") &&Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Auto_Listen"))){
                                useListeningNumberWithAutomaicListening= true;
                            }
                        }
                    }
                });
            }

            else if (message.contains("STTHotword_success")) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e("TEST_HOT","hotword Success");
                        if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("no")){
                            teamChatBuddyApplication.setSpeaking(true);
                            isListeningFreeSpeech = true;
                            teamChatBuddyApplication.setActivityClosed(false);
                            teamChatBuddyApplication.setStartRecording(true);
                            startListeningFreeSpeech(teamChatBuddyApplication.getListeningDuration());
                        }
                        else if (teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes")){
                            Log.e("TEST_HOT","hotword Success tracking activé ");
                            if(teamChatBuddyApplication.getParamFromFile("WELCOME_hotword","TeamChatBuddy.properties").equalsIgnoreCase("Yes")){
                                Log.e("TEST_HOT"," welcome hotword yes  "+iInvitationCallback);
                                invitation(new IInvitationCallback() {
                                    @Override
                                    public void onEnd(String s) {
                                        Log.e("TEST_HOT", "Invitation onEnd Callback welcome hotword  : "+s);
                                        iInvitationCallback = null;
                                        startListeningQuestion();
                                    }
                                });
                            }
                            else{
                                if( !Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Auto_Listen"))){
                                    Log.e("TEST_HOT","hotword Success !Tracking_Auto_Listen");
                                    teamChatBuddyApplication.setSpeaking(true);
                                    isListeningFreeSpeech = true;
                                    teamChatBuddyApplication.setActivityClosed(false);
                                    teamChatBuddyApplication.setStartRecording(true);
                                    startListeningFreeSpeech(teamChatBuddyApplication.getListeningDuration());
                                }
                                else{
                                    if (teamChatBuddyApplication.isShouldLaunchListeningAfterGetingHotWord()){
                                        Log.e("TEST_HOT","hotword Success isShouldLaunchListeningAfterGetingHotWord()");
                                        teamChatBuddyApplication.setSpeaking(true);
                                        isListeningFreeSpeech = true;
                                        teamChatBuddyApplication.setActivityClosed(false);
                                        teamChatBuddyApplication.setStartRecording(true);
                                        startListeningFreeSpeech(teamChatBuddyApplication.getListeningDuration());
                                    }
                                    else{
                                        Log.e("TEST_HOT","hotword Success !isShouldLaunchListeningAfterGetingHotWord()");
                                        if (regarde_camera){
                                            Log.e("TEST_HOT","hotword Success regarde_camera");
                                            teamChatBuddyApplication.setSpeaking(true);
                                            isListeningFreeSpeech = true;
                                            teamChatBuddyApplication.setActivityClosed(false);
                                            teamChatBuddyApplication.setStartRecording(true);
                                            teamChatBuddyApplication.setShouldLaunchListeningAfterGetingHotWord(true);
                                            startListeningFreeSpeech(teamChatBuddyApplication.getListeningDuration());
                                        }
                                    }
                                }
                            }
                        }

                    }
                });
            }

            else if (message.contains("STTQuestion_success")) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e("Test_Continuous","STTQuestion_success receive");
                        stopListeningFreeSpeech();
                        teamChatBuddyApplication.setAppIsCurrentlyDealingWithTheQuestion(true);
                        SystemClock.sleep(200);
                        teamChatBuddyApplication.setAppIsListeningToTheQuestion(false);
                        Log.i("zomilito",""+message);
                        String detectedSTTMessage = message.split(";")[1].replaceAll("' ", "'");
                        if (!teamChatBuddyApplication.isActivityClosed()) {
                            if (!teamChatBuddyApplication.isModeContinuousListeningON()) {
                                teamChatBuddyApplication.setQuestionNumber(teamChatBuddyApplication.getQuestionNumber() + 1);
                                teamChatBuddyApplication.setQuestionTime(System.currentTimeMillis());
                                BuddySDK.UI.setFacialExpression(FacialExpression.THINKING, 1);
                                teamChatBuddyApplication.setLed("thinking");
                                if (settingClass.getSwitchVisibility().equals("true")) {
                                    if (teamChatBuddyApplication.getCurrentLanguage().equals("en")) {
                                        buddy_texte_qst.setText(String.format("I heard :  %s ", detectedSTTMessage));
                                    } else if (settingClass.getLangue().equals(langueFr)) {
                                        buddy_texte_qst.setText(String.format("J'ai entendu :  %s ", detectedSTTMessage));
                                    } else {
                                        teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate("I heard ").addOnSuccessListener(new OnSuccessListener<String>() {
                                            @Override
                                            public void onSuccess(String translatedText) {

                                                buddy_texte_qst.setText(String.format(translatedText + " :  %s ", detectedSTTMessage));
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.e(TAG, "translatedText exception  " + e);
                                            }
                                        });
                                    }
                                    buddy_texte_qst_lyt.setVisibility(View.VISIBLE);
                                    buddy_texte_qst.setMovementMethod(new ScrollingMovementMethod());
                                    buddy_texte_qst.scrollTo(0, 0);
                                    lyt_open_menu_settings.setVisibility(View.INVISIBLE);
                                    lyt_open_menu_chat.setVisibility(View.INVISIBLE);
                                }
                                String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
                                Replica question = new Replica();
                                question.setType("question");
                                question.setTime(time);
                                question.setValue(detectedSTTMessage);
                                listRep.add(question);
                            }
                            else {
                                teamChatBuddyApplication.listOfQuestionInContinuousListeningMode.add(detectedSTTMessage);
                                Log.e("Test_Continuous","question detected "+teamChatBuddyApplication.listOfQuestionInContinuousListeningMode.get(0));
                            }
                            lastLookingAtCameraTimeToCloseApp= System.currentTimeMillis();
                            personDetectedTimeToCloseApp= System.currentTimeMillis();
                            if (teamChatBuddyApplication.getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT")) {
                                responseFromChatbot.getCommandsResponseFromChatGPT(detectedSTTMessage, teamChatBuddyApplication.getQuestionNumber());
                            }
                            else {
                                responseFromChatbot.getSessionId(detectedSTTMessage);
                            }
                            if (
                                    ( Integer.parseInt(teamChatBuddyApplication.getParamFromFile("Response_Timeout_in_seconds","TeamChatBuddy.properties"))!=0 )
                                            && (
                                            (
                                                    teamChatBuddyApplication.getCurrentLanguage().equals("en")
                                                            && !teamChatBuddyApplication.getParamFromFile("Message_Timeout_NotRespected_en","TeamChatBuddy.properties").trim().isEmpty()
                                            )
                                                    ||
                                                    (
                                                            teamChatBuddyApplication.getCurrentLanguage().equals("fr")
                                                                    && !teamChatBuddyApplication.getParamFromFile("Message_Timeout_NotRespected_fr","TeamChatBuddy.properties").trim().isEmpty()
                                                    )
                                                    ||(
                                                    !teamChatBuddyApplication.getCurrentLanguage().equals("en")
                                                            && !teamChatBuddyApplication.getParamFromFile("Message_Timeout_NotRespected_en","TeamChatBuddy.properties").trim().isEmpty()
                                            )

                                    )
                            ) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        teamChatBuddyApplication.setAnswerHasExceededTimeOut(false);
                                        responseTimeout = new CountDownTimer(Integer.parseInt(teamChatBuddyApplication.getParamFromFile("Response_Timeout_in_seconds", "TeamChatBuddy.properties")) * 1000, 1000) {
                                            @Override
                                            public void onTick(long l) {}
                                            @Override
                                            public void onFinish() {
                                                if (teamChatBuddyApplication.isAlreadyGetAnswer()) {
                                                    Log.e(TAG, "app get the answer on time");
                                                } else {
                                                    teamChatBuddyApplication.setAnswerHasExceededTimeOut(true);
                                                    teamChatBuddyApplication.setTimeoutExpired(true);
                                                    if (!teamChatBuddyApplication.isOpenaialreadySwitchEmotion()) {
                                                        BuddySDK.UI.setFacialExpression(FacialExpression.TIRED,1);
                                                    }
                                                    if (teamChatBuddyApplication.getCurrentLanguage().equals("en")) {
                                                        String[] message_Timeout_NotRespected_en = teamChatBuddyApplication.getParamFromFile("Message_Timeout_NotRespected_en","TeamChatBuddy.properties").split("/");
                                                        int randomNumber_message_Timeout_NotRespected_en = new Random().nextInt(message_Timeout_NotRespected_en.length);
                                                        speak(message_Timeout_NotRespected_en[randomNumber_message_Timeout_NotRespected_en],"timeOutExpired");
                                                    }
                                                    else if (teamChatBuddyApplication.getCurrentLanguage().equals("fr")){
                                                        String[] message_Timeout_NotRespected_fr = teamChatBuddyApplication.getParamFromFile("Message_Timeout_NotRespected_fr","TeamChatBuddy.properties").split("/");
                                                        int randomNumber_message_Timeout_NotRespected_fr = new Random().nextInt(message_Timeout_NotRespected_fr.length);
                                                        speak(message_Timeout_NotRespected_fr[randomNumber_message_Timeout_NotRespected_fr],"timeOutExpired");
                                                    }
                                                    else {
                                                        String[] message_Timeout_NotRespected_en = teamChatBuddyApplication.getParamFromFile("Message_Timeout_NotRespected_en","TeamChatBuddy.properties").split("/");
                                                        int randomNumber_message_Timeout_NotRespected_en = new Random().nextInt(message_Timeout_NotRespected_en.length);
                                                        teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(message_Timeout_NotRespected_en[randomNumber_message_Timeout_NotRespected_en]).addOnSuccessListener(new OnSuccessListener<String>() {
                                                            @Override
                                                            public void onSuccess(String translatedText) {
                                                                speak(translatedText,"timeOutExpired");
                                                            }
                                                        }).addOnFailureListener(new OnFailureListener() {
                                                            @Override
                                                            public void onFailure(@NonNull Exception e) {
                                                                Log.e(TAG,"translatedText exception  "+e);
                                                            }
                                                        });

                                                    }
                                                }
                                            }
                                        };
                                        responseTimeout.start();
                                    }
                                });
                            }
                            if (teamChatBuddyApplication.getParamFromFile("Permanent_listening","TeamChatBuddy.properties")!=null
                                    &&teamChatBuddyApplication.getParamFromFile("Permanent_listening","TeamChatBuddy.properties").trim().equalsIgnoreCase("Yes")) {
                                teamChatBuddyApplication.setModeContinuousListeningON(true);
                                startListeningFreeSpeech(teamChatBuddyApplication.getListeningDuration());
                            }
                        }
                    }
                });
            }

            else if (message.contains("TTS_success")) {
                runOnUiThread(() ->{
                    Log.e(TAG," TTS_success");
                    lastLookingAtCameraTimeToCloseApp= System.currentTimeMillis();
                    personDetectedTimeToCloseApp= System.currentTimeMillis();
                    teamChatBuddyApplication.setAppIsCurrentlyDealingWithTheQuestion(false);
                    buddy_texte_qst_lyt.setVisibility(View.INVISIBLE);
                    buddy_texte_resp_lyt.setVisibility(View.INVISIBLE);
                    buddy_texte_qst.setMovementMethod(null);
                    buddy_texte_resp.setMovementMethod(null);
                    if (teamChatBuddyApplication.getParamFromFile("Options_Access","TeamChatBuddy.properties")!=null && teamChatBuddyApplication.getParamFromFile("Options_Access","TeamChatBuddy.properties").trim().equalsIgnoreCase("Yes")){
                        if (teamChatBuddyApplication.getParamFromFile("Number_clicks_options","TeamChatBuddy.properties")!=null &&
                                Integer.parseInt(teamChatBuddyApplication.getParamFromFile("Number_clicks_options","TeamChatBuddy.properties"))!=0){
                            lyt_open_menu_settings.setVisibility(View.VISIBLE);
                        }
                        else if (teamChatBuddyApplication.getParamFromFile("Number_clicks_options","TeamChatBuddy.properties")==null){
                            lyt_open_menu_settings.setVisibility(View.VISIBLE);
                        }
                    }
                    lyt_open_menu_chat.setVisibility(View.VISIBLE);
                    isSpeaking =false;
                    if(iInvitationCallback != null) iInvitationCallback.onEnd("INVITATION_END");
                    if(iStartMessageCallback != null) iStartMessageCallback.onEnd("STARTMESSAGE_END");
                    if(iMouthMessageCallback != null) iMouthMessageCallback.onEnd("MOUTHMESSAGE_END");
                });
                if(handler!=null && runnable!=null){
                    handler.removeCallbacks(runnable);
                    handler.removeCallbacksAndMessages(null);
                }
                 runnable =new Runnable() {
                    @Override
                    public void run() {
                        if (!teamChatBuddyApplication.getStoredResponse().equals("")){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    speak(teamChatBuddyApplication.getStoredResponse(),"storedResponse");
                                }
                            });
                        }
                        else {
                            if (!teamChatBuddyApplication.isModeContinuousListeningON()) {
                                if (!teamChatBuddyApplication.isMultiCommandsDetected()) {
                                    if (teamChatBuddyApplication.getStartRecording()) {
                                        Log.e(TAG, "startCycle TTS_success 2");
                                        teamChatBuddyApplication.setRemainingAttempts(teamChatBuddyApplication.getListeningAttempt() - 1);
                                        startCycle();
                                    }
                                }
                                else {

                                    if (teamChatBuddyApplication.isTimeToExecuteNextCommande()){
                                        teamChatBuddyApplication.setTimeToExecuteNextCommande(false);
                                        Log.e("MRA_TEST","executeCommand TTS_SUCCESS");
                                        responseFromChatbot.executeCommand();
                                    }

                                }
                            }
                            else {
                                continuePlayingResponses();
                            }
                        }
                    }
                };
                handler.postDelayed(runnable,500);
            }

            else if (message.contains("TTS_error") || message.contains("TTS_exception")) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String text = message.split(";")[1];
                        Log.w(TAG,"TTS_ERROR:"+text);
                        teamChatBuddyApplication.playUsingReadSpeakerCaseError(text, new ITTSCallbacks() {
                            @Override
                            public void onSuccess(String s) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        teamChatBuddyApplication.setAppIsCurrentlyDealingWithTheQuestion(false);
                                        try {
                                            BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                                        }
                                        catch (Exception e){
                                            Log.e(TAG,"BuddySDK Exception  "+e);
                                        }
                                        if (teamChatBuddyApplication.getparam("Mode_Stream").contains("yes") && teamChatBuddyApplication.getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT") && teamChatBuddyApplication.getChatGptStreamMode() != null) {
                                            Log.w("MODE_STREAM","TTS ERROR");
                                            teamChatBuddyApplication.getChatGptStreamMode().isReadyToSpeak = true;
                                        }
                                        else if(teamChatBuddyApplication.getparam("chatbot_chosen").equalsIgnoreCase("CustomGPT") && teamChatBuddyApplication.getCustomGPTStreamMode() != null){
                                            teamChatBuddyApplication.getCustomGPTStreamMode().isReadyToSpeak = true;
                                        }
                                        else{
                                            buddy_texte_qst_lyt.setVisibility(View.INVISIBLE);
                                            buddy_texte_resp_lyt.setVisibility(View.INVISIBLE);
                                            buddy_texte_qst.setMovementMethod(null);
                                            buddy_texte_resp.setMovementMethod(null);
                                            if (teamChatBuddyApplication.getParamFromFile("Options_Access","TeamChatBuddy.properties")!=null && teamChatBuddyApplication.getParamFromFile("Options_Access","TeamChatBuddy.properties").trim().equalsIgnoreCase("Yes")){
                                                if (teamChatBuddyApplication.getParamFromFile("Number_clicks_options","TeamChatBuddy.properties")!=null &&
                                                        Integer.parseInt(teamChatBuddyApplication.getParamFromFile("Number_clicks_options","TeamChatBuddy.properties"))!=0){
                                                    lyt_open_menu_settings.setVisibility(View.VISIBLE);
                                                }
                                                else if (teamChatBuddyApplication.getParamFromFile("Number_clicks_options","TeamChatBuddy.properties")==null){
                                                    lyt_open_menu_settings.setVisibility(View.VISIBLE);
                                                }
                                            }
                                            lyt_open_menu_chat.setVisibility(View.VISIBLE);
                                            isSpeaking =false;
                                            if(iInvitationCallback != null) iInvitationCallback.onEnd("INVITATION_END");
                                            if(iStartMessageCallback != null) iStartMessageCallback.onEnd("STARTMESSAGE_END");
                                            if(iMouthMessageCallback != null) iMouthMessageCallback.onEnd("MOUTHMESSAGE_END");
                                            if(handler!=null && runnable!=null){
                                                handler.removeCallbacks(runnable);
                                                handler.removeCallbacksAndMessages(null);
                                            }
                                            runnable =new Runnable() {
                                                @Override
                                                public void run() {
                                                    if (!teamChatBuddyApplication.getStoredResponse().equals("")){
                                                        runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                speak(teamChatBuddyApplication.getStoredResponse(),"storedResponse");
                                                            }
                                                        });
                                                    }
                                                    else {
                                                        if (!teamChatBuddyApplication.isMultiCommandsDetected()) {
                                                            if (teamChatBuddyApplication.getStartRecording()) {
                                                                Log.e(TAG, "startCycle TTS_success 2");
                                                                teamChatBuddyApplication.setRemainingAttempts(teamChatBuddyApplication.getListeningAttempt() - 1);
                                                                startCycle();
                                                            }
                                                        }
                                                        else {

                                                            if (teamChatBuddyApplication.isTimeToExecuteNextCommande()){
                                                                teamChatBuddyApplication.setTimeToExecuteNextCommande(false);
                                                                Log.e("MRA_TEST","executeCommand TTS_SUCCESS");
                                                                responseFromChatbot.executeCommand();
                                                            }

                                                        }
                                                    }
                                                }
                                            };
                                            handler.postDelayed(runnable,500);
                                        }
                                    }
                                });
                            }
                            @Override
                            public void onError(String s) {
                                int textLength = text.length();// Calculate the length of the pronounced text
                                int delayTime = (textLength / 20) * 1000; // 1 second for every 20 characters
                                if (delayTime==0){
                                    delayTime=1500;
                                }
                                if(teamChatBuddyApplication.getChosenTTS().trim().equalsIgnoreCase("ReadSpeaker") && (teamChatBuddyApplication.getCurrentLanguage().equals("en") || teamChatBuddyApplication.getCurrentLanguage().equals("fr")) && teamChatBuddyApplication.getUsingReadSpeaker() ){
                                    delayTime = 0;
                                    Log.e(TAG,"set 0 delay time  "+delayTime);
                                }
                                handlerTTSError.postDelayed(runnableTTSError = new Runnable() {
                                    @Override
                                    public void run() {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                teamChatBuddyApplication.setAppIsCurrentlyDealingWithTheQuestion(false);
                                                try {
                                                    BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                                                }
                                                catch (Exception e){
                                                    Log.e(TAG,"BuddySDK Exception  "+e);
                                                }
                                                if (teamChatBuddyApplication.getparam("Mode_Stream").contains("yes") && teamChatBuddyApplication.getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT") && teamChatBuddyApplication.getChatGptStreamMode() != null) {
                                                    Log.w("MODE_STREAM","TTS ERROR");
                                                    teamChatBuddyApplication.getChatGptStreamMode().isReadyToSpeak = true;
                                                }
                                                else if(teamChatBuddyApplication.getparam("chatbot_chosen").equalsIgnoreCase("CustomGPT") && teamChatBuddyApplication.getCustomGPTStreamMode() != null){
                                                    teamChatBuddyApplication.getCustomGPTStreamMode().isReadyToSpeak = true;
                                                }
                                                else{
                                                    buddy_texte_qst_lyt.setVisibility(View.INVISIBLE);
                                                    buddy_texte_resp_lyt.setVisibility(View.INVISIBLE);
                                                    buddy_texte_qst.setMovementMethod(null);
                                                    buddy_texte_resp.setMovementMethod(null);
                                                    if (teamChatBuddyApplication.getParamFromFile("Options_Access","TeamChatBuddy.properties")!=null && teamChatBuddyApplication.getParamFromFile("Options_Access","TeamChatBuddy.properties").trim().equalsIgnoreCase("Yes")){
                                                        if (teamChatBuddyApplication.getParamFromFile("Number_clicks_options","TeamChatBuddy.properties")!=null &&
                                                                Integer.parseInt(teamChatBuddyApplication.getParamFromFile("Number_clicks_options","TeamChatBuddy.properties"))!=0){
                                                            lyt_open_menu_settings.setVisibility(View.VISIBLE);
                                                        }
                                                        else if (teamChatBuddyApplication.getParamFromFile("Number_clicks_options","TeamChatBuddy.properties")==null){
                                                            lyt_open_menu_settings.setVisibility(View.VISIBLE);
                                                        }
                                                    }
                                                    lyt_open_menu_chat.setVisibility(View.VISIBLE);
                                                    isSpeaking =false;
                                                    if(iInvitationCallback != null) iInvitationCallback.onEnd("INVITATION_END");
                                                    if(iStartMessageCallback != null) iStartMessageCallback.onEnd("STARTMESSAGE_END");
                                                    if(iMouthMessageCallback != null) iMouthMessageCallback.onEnd("MOUTHMESSAGE_END");
                                                    if(handler!=null && runnable!=null){
                                                        handler.removeCallbacks(runnable);
                                                        handler.removeCallbacksAndMessages(null);
                                                    }
                                                    runnable =new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            if (!teamChatBuddyApplication.getStoredResponse().equals("")){
                                                                runOnUiThread(new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        speak(teamChatBuddyApplication.getStoredResponse(),"storedResponse");
                                                                    }
                                                                });
                                                            }
                                                            else {
                                                                if (!teamChatBuddyApplication.isMultiCommandsDetected()) {
                                                                    if (teamChatBuddyApplication.getStartRecording()) {
                                                                        Log.e(TAG, "startCycle TTS_success 2");
                                                                        teamChatBuddyApplication.setRemainingAttempts(teamChatBuddyApplication.getListeningAttempt() - 1);
                                                                        startCycle();
                                                                    }
                                                                }
                                                                else {

                                                                    if (teamChatBuddyApplication.isTimeToExecuteNextCommande()){
                                                                        teamChatBuddyApplication.setTimeToExecuteNextCommande(false);
                                                                        Log.e("MRA_TEST","executeCommand TTS_SUCCESS");
                                                                        responseFromChatbot.executeCommand();
                                                                    }

                                                                }
                                                            }
                                                        }
                                                    };
                                                    handler.postDelayed(runnable,500);
                                                }
                                            }
                                        });
                                    }
                                },delayTime);
                            }
                        });
                    }
                });
            }

            else if (message.contains("CHATBOTS_RETURN")) {
                Log.e("Test_Continuous","CHATBOTS_RETURN receive notif");
                runOnUiThread(() ->{
                    String action = message.split(";SPLIT;")[1];
                    Log.i(TAG,"action : "+action);
                    String value = message.split(";SPLIT;")[2];
                    Log.i(TAG,"value : "+value);
                    if (action.equals("speak")) {
                        if (message.split(";SPLIT;").length>3) {
                            if (!teamChatBuddyApplication.isModeContinuousListeningON() || isFirstResponse) {
                                Log.e("Test_Continuous","CHATBOTS_RETURN first response to get speaked");
                                isFirstResponse=false;
                                int numberOfQuestion = Integer.parseInt(message.split(";SPLIT;")[3]);
                                if (numberOfQuestion == teamChatBuddyApplication.getQuestionNumber()) {
                                    if (message.split(";SPLIT;").length > 4) {
                                        if (message.split(";SPLIT;")[4].equals("onError")) {
                                            teamChatBuddyApplication.setMessageError(true);
                                            if (!teamChatBuddyApplication.isOpenaialreadySwitchEmotion()) {
                                                BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
                                            }
                                        }
                                    }

                                    if (!teamChatBuddyApplication.isTimeoutExpired()) {
                                        speak(value, "nothealysa");
                                    } else {
                                        teamChatBuddyApplication.setStoredResponse(value);
                                    }
                                    if (teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes") && Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Auto_Listen"))) {
                                        useListeningNumberWithAutomaicListening = true;
                                    }
                                }
                            }
                            else {
                                if (message.split(";SPLIT;").length > 4) {
                                    if (message.split(";SPLIT;")[4].equals("onError")) {
                                        teamChatBuddyApplication.setMessageError(true);
//                                    if (!teamChatBuddyApplication.isOpenaialreadySwitchEmotion()) {
//                                        BuddySDK.UI.setFacialExpression(FacialExpression.TIRED, 1);
//                                    }
                                    }
                                    else{
                                        teamChatBuddyApplication.listOfDetectedLanguagesOfResponseInContinuousListeningMode.add(message.split(";SPLIT;")[4].trim());
                                    }
                                }

//                            if (!teamChatBuddyApplication.isTimeoutExpired()) {
//                                speak(value, "nothealysa");
//                            } else {
//                                teamChatBuddyApplication.setStoredResponse(value);
//                            }
                                teamChatBuddyApplication.listOfResponseInContinuousListeningMode.add(value);
                                Log.e("Test_Continuous","response added "+teamChatBuddyApplication.listOfResponseInContinuousListeningMode.get(0));
                                if (isQuestionAlreadyDetected){
                                    isQuestionAlreadyDetected = false;
                                    if (!teamChatBuddyApplication.isTimeoutExpired()) {
                                        speak(teamChatBuddyApplication.listOfResponseInContinuousListeningMode.get(0), "nothealysa");
                                    } else {
                                        teamChatBuddyApplication.setStoredResponse(teamChatBuddyApplication.listOfResponseInContinuousListeningMode.get(0));
                                    }
                                    teamChatBuddyApplication.listOfResponseInContinuousListeningMode.remove(0);
                                    if (teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes") && Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Auto_Listen"))) {
                                        useListeningNumberWithAutomaicListening = true;
                                    }
                                    if (!teamChatBuddyApplication.listOfDetectedLanguagesOfResponseInContinuousListeningMode.isEmpty()) {
                                        if (teamChatBuddyApplication.listOfDetectedLanguagesOfResponseInContinuousListeningMode.get(0).trim().equalsIgnoreCase("NotDetected")) {
                                            teamChatBuddyApplication.setLanguageDetected("");
                                        } else {
                                            teamChatBuddyApplication.setLanguageDetected(teamChatBuddyApplication.listOfDetectedLanguagesOfResponseInContinuousListeningMode.get(0));
                                        }
                                        teamChatBuddyApplication.listOfDetectedLanguagesOfResponseInContinuousListeningMode.remove(0);
                                    }
                                    if (!teamChatBuddyApplication.listOfEmotionsForQuestionInContinuousListeningMode.isEmpty()) {
                                        setAnimation(teamChatBuddyApplication.listOfEmotionsForQuestionInContinuousListeningMode.get(0));
                                        teamChatBuddyApplication.listOfEmotionsForQuestionInContinuousListeningMode.remove(0);
                                    }
                                }

//                            if (teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes") && Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Auto_Listen"))) {
//                                useListeningNumberWithAutomaicListening = true;
//                            }
                            }
                        }
                    }
                    else if (action.equals("INVITATION")) {
                        Log.e(TAG_TRACKING, "ChatGPT Invitation: " + value);
                        teamChatBuddyApplication.setActivityClosed(false);
                        speak(value, "INVITATION");
                        if (Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Auto_Listen"))){
                            useListeningNumberWithAutomaicListening= true;
                        }
                    }
                });
            }

            else if (message.contains("properties file done")) {
                teamChatBuddyApplication.setNotYet(false);
                getData();
            }

            else if (message.contains("end of timer")) {
                teamChatBuddyApplication.setAppIsListeningToTheQuestion(false);
                stopListeningFreeSpeech();
                SystemClock.sleep(200);
                if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("no")){
                    teamChatBuddyApplication.startListeningHotwor(this);
                }
                else if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes")){
//                    if( !Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Auto_Listen"))){
                        teamChatBuddyApplication.startListeningHotwor(MainActivity.this);
//                    }
                }
            }

            else if (message.contains("end of cycle")){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL,1);
                        BuddySDK.UI.stopListenAnimation();
                        teamChatBuddyApplication.setLed("neutral");
                    }
                });
            }

            else if (message.contains("restartNewCycle")){
                runnablePauseTime =new Runnable() {
                    @Override
                    public void run() {
                        startNextCycle();
                        Log.e(TAG,"startNextCycle  after handler ");
                    }
                };
                handlerPauseTime.postDelayed(runnablePauseTime,1000);
            }

            else if (message.contains("Obtain audio transcription after the listening time has elapsed")){
                String shouldRestartNewCycle = message.split(";SPLIT;")[1];
                Log.e(TAG,"Obtain audio transcription after the listening time has elapsed "+shouldRestartNewCycle);
                BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL,1);
                BuddySDK.UI.stopListenAnimation();
                teamChatBuddyApplication.setLed("neutral");
                teamChatBuddyApplication.setAppIsListeningToTheQuestion(false);
                if (shouldRestartNewCycle.equals("true")) {
                    teamChatBuddyApplication.traitementAudio(true);
                }
                else {
                    teamChatBuddyApplication.traitementAudio(false);
                }
            }

            else if (message.contains("restartListeningHotword")){
                BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                teamChatBuddyApplication.setAppIsCurrentlyDealingWithTheQuestion(false);
                teamChatBuddyApplication.setStartRecording(false);
                teamChatBuddyApplication.setSpeaking(false);
                teamChatBuddyApplication.setActivityClosed(true);
                isListeningFreeSpeech = false;
                teamChatBuddyApplication.stopTTS();
                teamChatBuddyApplication.setStoredResponse("");
                if (buddy_texte_qst_lyt != null && buddy_texte_resp_lyt != null && buddy_texte_qst != null && buddy_texte_resp != null) {
                    buddy_texte_qst_lyt.setVisibility(View.INVISIBLE);
                    buddy_texte_resp_lyt.setVisibility(View.INVISIBLE);
                    buddy_texte_qst.setMovementMethod(null);
                    buddy_texte_resp.setMovementMethod(null);
                }
                if (teamChatBuddyApplication.getParamFromFile("Options_Access","TeamChatBuddy.properties")!=null && teamChatBuddyApplication.getParamFromFile("Options_Access","TeamChatBuddy.properties").trim().equalsIgnoreCase("Yes")){
                    if (teamChatBuddyApplication.getParamFromFile("Number_clicks_options","TeamChatBuddy.properties")!=null &&
                            Integer.parseInt(teamChatBuddyApplication.getParamFromFile("Number_clicks_options","TeamChatBuddy.properties"))!=0){
                        lyt_open_menu_settings.setVisibility(View.VISIBLE);
                    }
                    else if (teamChatBuddyApplication.getParamFromFile("Number_clicks_options","TeamChatBuddy.properties")==null){
                        lyt_open_menu_settings.setVisibility(View.VISIBLE);
                    }
                }
                lyt_open_menu_chat.setVisibility(View.VISIBLE);
                try {
                    BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                } catch (Exception e) {
                    Log.e(TAG, "BuddySDK Exception  " + e);
                }
                teamChatBuddyApplication.notifyObservers("end of timer");
            }

            else if (message.contains("getResponseF")) {
                if (message.split(";SPLIT;")[1].equalsIgnoreCase("gpt")) {
                    gptResponse = message.split(";SPLIT;")[2];
                    gptSend = true;
                }
                if (gptSend ) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (Integer.parseInt(message.split(";SPLIT;")[3])== teamChatBuddyApplication.getQuestionNumber()) {
                                if (!teamChatBuddyApplication.isModeContinuousListeningON()) {
                                    if (!teamChatBuddyApplication.getAnswerHasExceededTimeOut()) {
                                        setAnimation(gptResponse);
                                    }
                                }
                                else {
                                    teamChatBuddyApplication.listOfEmotionsForQuestionInContinuousListeningMode.add(gptResponse);
                                }
                                teamChatBuddyApplication.setOpenaialreadySwitchEmotion(true);
                            }
                            gptSend = false;
                        }
                    });
                }
            }

            else if (message.contains("playStoredResponse")){
                if (!teamChatBuddyApplication.getStoredResponse().equals("")){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            speak(teamChatBuddyApplication.getStoredResponse(),"storedResponse");
                        }
                    });
                }
            }

            else if (message.contains("makeBuddyFaceNeutral")){
                BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL,1);
            }

            else if (message.contains("playEmotion")){
                if (!teamChatBuddyApplication.getCurrentEmotion().equals("")){
                    if (!teamChatBuddyApplication.getAnswerHasExceededTimeOut()){
                        setAnimation(teamChatBuddyApplication.getCurrentEmotion());
                    }
                    teamChatBuddyApplication.setOpenaialreadySwitchEmotion(true);
                }
            }

            else if (message.contains("ChatDestroy")){
                teamChatBuddyApplication.setparam("firstLaunch","false");
            }

            else if (message.contains("isConnected")){
                isConnected = true;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        downloadingBar.setVisibility(View.VISIBLE);
                        noNetwork.setVisibility(View.GONE);
                    }
                });
            }

            else if (message.contains("isNotConnected")){
                isConnected = false;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        downloadingBar.setVisibility(View.GONE);
                        noNetwork.setVisibility(View.VISIBLE);
                        if(timerDownloading!=null){
                            timerDownloading.cancel();
                        }
                    }
                });
            }

            else if (message.contains("changeDetected")){
                int speakVolume = teamChatBuddyApplication.getVolume();
                int max = teamChatBuddyApplication.getMaxVolume();
                int defaultVolume = teamChatBuddyApplication.getClosestInt((double) (speakVolume * 100) / max);
                Log.e(TAG,"volumeMedia  "+String.valueOf(defaultVolume));
                teamChatBuddyApplication.setparam("speak_volume", String.valueOf(defaultVolume));
            }

            else if (message.contains( "commandResponse" )){
                if(message.split( ";SPLIT;" )[1].equals("CANCEL")){
                    if(!isSpeaking){
                        if (responseTimeout!=null) responseTimeout.cancel();
                        if (!teamChatBuddyApplication.isMultiCommandsDetected()) {
                            if (!teamChatBuddyApplication.getUsingEmotions()) {
                                Log.d(TAG, "FacialExpression NEUTRAL");
                                BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                            }
                            teamChatBuddyApplication.notifyObservers("TTS_success");
                        }
                        else {
                            Log.e("MRA_TEST","executeCommand commandResponse");
                            responseFromChatbot.executeCommand();
                        }
                    }
                }
                else if(message.split( ";SPLIT;" )[1].equals("CHANGE_LANGUE")){
                    isCMDLangue = true;
                    settingClass.setLangue(teamChatBuddyApplication.getLangue().getNom());
                    mlKitIsDownloading = true;
                    List<String> mlkitLangueCode = teamChatBuddyApplication.getLanguageCodeForDisponibleLangue("Language_Code_Used_In_Mlkit");
                    String codeLanguageMlkit = mlkitLangueCode.get(teamChatBuddyApplication.getLangue().getId()-1);
                    teamChatBuddyApplication.downloadModel(imlKitDownloadCallback,codeLanguageMlkit.trim());
                    handlerProgressBar.postDelayed(runnableProgressBar,500);
                    timerDownloading.start();
                }
                else{
                    if(!isSpeaking)
                    {
                        stopListeningFreeSpeech();
                        try {
                            BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                            BuddySDK.UI.stopListenAnimation();
                        } catch (Exception e) {
                            Log.e(TAG, "BuddySDK Exception  " + e);
                        }
                        speak(message.split( ";SPLIT;" )[1], "commande");
                    }
                    else
                        teamChatBuddyApplication.setStoredResponse( message.split( ";SPLIT;" )[1] );
                }
            }

            else if (message.contains( "showImage" )){
                Log.i( TAG, "J'affiche une image" );
                afficherPopupAvecBitmap( "/storage/emulated/0/"+message.split( ";SPLIT;" )[1]);
            }

            else if (message.equals( "closeImage" )){
                closeImage();
            }

            else if (message.contains("takePicture")){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e("MRRM","prompt CMD_TAKE_PHOTO"+message.split(";SPLIT;")[1]);
                        startCameraForCommand(message.split(";SPLIT;")[1]);
                    }
                });
            }

            else if(message.contains("STOP_TRACKING")){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes")){
                            stopTracking();
                        }
                    }
                });
            }

            else if(message.contains("RESTART_TRACKING")){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes")){
                            isReTrack = true;
                            initTracking();
                        }
                    }
                });
            }
            else if (message.contains("ExecuteCMDPROMPT")){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                            if (teamChatBuddyApplication.getCurrentLanguage().equals("en")) {
                                if (teamChatBuddyApplication.getParamFromFile("Response_format_en","TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.isStringEmptyOrNoWords(teamChatBuddyApplication.getParamFromFile("Response_format_en","TeamChatBuddy.properties").trim())){
                                    responseFromChatbot.getResponseFromChatGPT(message.split(";SPLIT;")[1]+" "+teamChatBuddyApplication.getParamFromFile("Response_format_en","TeamChatBuddy.properties"),Integer.parseInt(message.split(";SPLIT;")[2]));
                                }
                                else {
                                    responseFromChatbot.getResponseFromChatGPT(message.split(";SPLIT;")[1],Integer.parseInt(message.split(";SPLIT;")[2]));
                                }
                            }
                            else if (settingClass.getLangue().equals(langueFr)) {
                                if (teamChatBuddyApplication.getParamFromFile("Response_format_fr","TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.isStringEmptyOrNoWords(teamChatBuddyApplication.getParamFromFile("Response_format_fr","TeamChatBuddy.properties").trim())){
                                    responseFromChatbot.getResponseFromChatGPT(message.split(";SPLIT;")[1]+" "+teamChatBuddyApplication.getParamFromFile("Response_format_fr","TeamChatBuddy.properties"),Integer.parseInt(message.split(";SPLIT;")[2]));
                                }
                                else {
                                    responseFromChatbot.getResponseFromChatGPT(message.split(";SPLIT;")[1],Integer.parseInt(message.split(";SPLIT;")[2]));
                                }
                            }
                            else {
                                if (teamChatBuddyApplication.getParamFromFile("Response_format_en","TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.isStringEmptyOrNoWords(teamChatBuddyApplication.getParamFromFile("Response_format_en","TeamChatBuddy.properties").trim())){
                                    teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(teamChatBuddyApplication.getParamFromFile("Response_format_en","TeamChatBuddy.properties")).addOnSuccessListener(new OnSuccessListener<String>() {
                                        @Override
                                        public void onSuccess(String translatedText) {

                                            responseFromChatbot.getResponseFromChatGPT(message.split(";SPLIT;")[1]+" "+translatedText,Integer.parseInt(message.split(";SPLIT;")[2]));
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.e(TAG,"translatedText exception  "+e);
                                        }
                                    });

                                }
                                else {
                                    responseFromChatbot.getResponseFromChatGPT(message.split(";SPLIT;")[1],Integer.parseInt(message.split(";SPLIT;")[2]));
                                }
                            }
                    }
                });
            }
            else if (message.contains("TTSAndroidIsInitialized")){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mlKitIsDownloading = true;
                        french_is_downloaded = false;
                        english_is_downloaded = false;
                        languageToEnglish_is_downloaded =false;
                        List<String> mlkitLangueCode = teamChatBuddyApplication.getLanguageCodeForDisponibleLangue("Language_Code_Used_In_Mlkit");
                        String codeLanguageMlkit = mlkitLangueCode.get(new Gson().fromJson(teamChatBuddyApplication.getparam(settingClass.getLangue()), Langue.class).getId()-1);
                        teamChatBuddyApplication.downloadModel(imlKitDownloadCallback,codeLanguageMlkit.trim());
                        handlerProgressBar.postDelayed(runnableProgressBar,500);

                        timerDownloading.start();
                    }
                });
            }
            else if (message.equals("SpeechRecognizerTimeout")){
                if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes") && Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Auto_Listen")) && regarde_camera){
                    Log.i(TAG_TRACKING, "timerEcoute onFinish --> Do not stop listening because tracking auto listen is enabled and user is looking directly at camera --> restart timer");
                    teamChatBuddyApplication.startListeningQuestion(MainActivity.this,"FirstListening");
                }
                else{
                    Log.i(TAG, "timerEcoute onFinish");
                    teamChatBuddyApplication.setAppIsListeningToTheQuestion(false);
                    stopListeningFreeSpeech();
                    SystemClock.sleep(200);
                    if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("no")){
                        teamChatBuddyApplication.startListeningHotwor(this);
                    }
                    else if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes")){
//                    if( !Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Auto_Listen"))){
                        teamChatBuddyApplication.startListeningHotwor(MainActivity.this);
//                    }
                    }
                }
            }
            else if (message.equals("SpeechRecognizerAttemptTimeout")){
                if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes") && Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Auto_Listen")) && regarde_camera){
                    Log.i(TAG_TRACKING, "timerEcoute onFinish --> Do not stop listening because tracking auto listen is enabled and user is looking directly at camera --> restart timer");
                    teamChatBuddyApplication.startListeningQuestion(MainActivity.this,"startCycle");
                }
                else{
                    Log.i(TAG, "timerEcoute onFinish");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL,1);
                            BuddySDK.UI.stopListenAnimation();
                            teamChatBuddyApplication.setLed("neutral");
                        }
                    });
                    runnablePauseTime =new Runnable() {
                        @Override
                        public void run() {
                            startNextCycle();
                            Log.e(TAG,"startNextCycle  after handler ");
                        }
                    };
                    handlerPauseTime.postDelayed(runnablePauseTime,1000);
                }
            }
        }
    }
    public void continuePlayingResponses(){
        if (!teamChatBuddyApplication.listOfQuestionInContinuousListeningMode.isEmpty()) {
            if (!teamChatBuddyApplication.isActivityClosed()) {
                teamChatBuddyApplication.setQuestionNumber(teamChatBuddyApplication.getQuestionNumber() + 1);
                teamChatBuddyApplication.setQuestionTime(System.currentTimeMillis());
                BuddySDK.UI.setFacialExpression(FacialExpression.THINKING, 1);
                if (settingClass.getSwitchVisibility().equals("true")) {
                    if (teamChatBuddyApplication.getCurrentLanguage().equals("en")) {
                        buddy_texte_qst.setText(String.format("I heard :  %s ", teamChatBuddyApplication.listOfQuestionInContinuousListeningMode.get(0)));
                    } else if (settingClass.getLangue().equals(langueFr)) {
                        buddy_texte_qst.setText(String.format("J'ai entendu :  %s ",  teamChatBuddyApplication.listOfQuestionInContinuousListeningMode.get(0)));
                    } else {
                        teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate("I heard ").addOnSuccessListener(new OnSuccessListener<String>() {
                            @Override
                            public void onSuccess(String translatedText) {

                                buddy_texte_qst.setText(String.format(translatedText + " :  %s ",  teamChatBuddyApplication.listOfQuestionInContinuousListeningMode.get(0)));
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "translatedText exception  " + e);
                            }
                        });
                    }
                    buddy_texte_qst_lyt.setVisibility(View.VISIBLE);
                    buddy_texte_qst.setMovementMethod(new ScrollingMovementMethod());
                    buddy_texte_qst.scrollTo(0, 0);
                    lyt_open_menu_settings.setVisibility(View.INVISIBLE);
                    lyt_open_menu_chat.setVisibility(View.INVISIBLE);
                }
                String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
                Replica question = new Replica();
                question.setType("question");
                question.setTime(time);
                question.setValue( teamChatBuddyApplication.listOfQuestionInContinuousListeningMode.get(0));
                listRep.add(question);
                teamChatBuddyApplication.listOfQuestionInContinuousListeningMode.remove(0);
            }
            if (!teamChatBuddyApplication.listOfResponseInContinuousListeningMode.isEmpty()) {
                if (!teamChatBuddyApplication.isTimeoutExpired()) {
                    speak(teamChatBuddyApplication.listOfResponseInContinuousListeningMode.get(0), "nothealysa");
                } else {
                    teamChatBuddyApplication.setStoredResponse(teamChatBuddyApplication.listOfResponseInContinuousListeningMode.get(0));
                }
                teamChatBuddyApplication.listOfResponseInContinuousListeningMode.remove(0);
                if (teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes") && Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Auto_Listen"))) {
                    useListeningNumberWithAutomaicListening = true;
                }
                if (!teamChatBuddyApplication.listOfDetectedLanguagesOfResponseInContinuousListeningMode.isEmpty()) {
                    if (teamChatBuddyApplication.listOfDetectedLanguagesOfResponseInContinuousListeningMode.get(0).trim().equalsIgnoreCase("NotDetected")) {
                        teamChatBuddyApplication.setLanguageDetected("");
                    } else {
                        teamChatBuddyApplication.setLanguageDetected(teamChatBuddyApplication.listOfDetectedLanguagesOfResponseInContinuousListeningMode.get(0));
                    }
                    teamChatBuddyApplication.listOfDetectedLanguagesOfResponseInContinuousListeningMode.remove(0);
                }
                if (!teamChatBuddyApplication.listOfEmotionsForQuestionInContinuousListeningMode.isEmpty()) {
                    setAnimation(teamChatBuddyApplication.listOfEmotionsForQuestionInContinuousListeningMode.get(0));
                    teamChatBuddyApplication.listOfEmotionsForQuestionInContinuousListeningMode.remove(0);
                }
            }
            else {
                isQuestionAlreadyDetected= true;
            }

        }
        else{
                teamChatBuddyApplication.setModeContinuousListeningON(false);
                isFirstResponse=true;
                if (teamChatBuddyApplication.getStartRecording()) {
                    Log.e(TAG, "startCycle TTS_success 2");
                    teamChatBuddyApplication.setRemainingAttempts(teamChatBuddyApplication.getListeningAttempt() - 1);
                    startCycle();
                }

        }
    }

    /**
     * ----------------- Utils ---------------------------
     */

    private void init() {
        Log.e(TAG,"init() ");
        BuddySDK.UI.addFaceTouchListener(iuiFaceTouchCallback);
        if (teamChatBuddyApplication.getparam("Stimulis").contains("yes")){
            handlerForSensor = new Handler();
            BuddySDK.USB.enableSensorModule(true,iUsbCommadRspBI);
        }

        teamChatBuddyApplication.registerObserver(this);

        buddy_texte_qst_lyt.setVisibility(View.INVISIBLE);
        buddy_texte_resp_lyt.setVisibility(View.INVISIBLE);
        buddy_texte_qst.setMovementMethod(null);
        buddy_texte_resp.setMovementMethod(null);
        lyt_open_menu_settings.setVisibility(View.INVISIBLE);
        lyt_open_menu_chat.setVisibility(View.VISIBLE);

        commande = new Commande( this );

        //init config file
        initOrMajOrNone = teamChatBuddyApplication.createPropertiesFile();

        checkParametersValues();
        migrateBooleanToString();
    }


    private void migrateBooleanToString() {
        if(teamChatBuddyApplication.getparam("switch_emotion").equals("true")){
            teamChatBuddyApplication.setparam("switch_emotion","yes");
        }else if(teamChatBuddyApplication.getparam("switch_emotion").equals("false")){
            teamChatBuddyApplication.setparam("switch_emotion","no");
        }

        if(teamChatBuddyApplication.getparam("switch_visibility").equals("true")){
            teamChatBuddyApplication.setparam("switch_visibility","yes");
        }else if(teamChatBuddyApplication.getparam("switch_visibility").equals("false")){
            teamChatBuddyApplication.setparam("switch_visibility","no");
        }


        if(teamChatBuddyApplication.getparam("Stimulis").equals("true")){
            teamChatBuddyApplication.setparam("Stimulis","yes");
        }else if(teamChatBuddyApplication.getparam("Stimulis").equals("false")){
            teamChatBuddyApplication.setparam("Stimulis","no");
        }


        if(teamChatBuddyApplication.getparam("Detection_de_langue").equals("true")){
            teamChatBuddyApplication.setparam("Detection_de_langue","yes");
        }else if(teamChatBuddyApplication.getparam("Detection_de_langue").equals("false")){
            teamChatBuddyApplication.setparam("Detection_de_langue","no");
        }

        if(teamChatBuddyApplication.getparam("Commands").equals("true")){
            teamChatBuddyApplication.setparam("Commands","yes");
        }else if(teamChatBuddyApplication.getparam("Commands").equals("false")){
            teamChatBuddyApplication.setparam("Commands","no");
        }

        if(teamChatBuddyApplication.getparam("Tracking_Activation").equals("true")){
            teamChatBuddyApplication.setparam("Tracking_Activation","yes");
        }else if(teamChatBuddyApplication.getparam("Tracking_Activation").equals("false")){
            teamChatBuddyApplication.setparam("Tracking_Activation","no");
        }

        if(teamChatBuddyApplication.getparam("Mode_Stream").equals("true")){
            teamChatBuddyApplication.setparam("Mode_Stream","yes");
        }else if(teamChatBuddyApplication.getparam("Mode_Stream").equals("false")){
            teamChatBuddyApplication.setparam("Mode_Stream","no");
        }

    }


    private void checkParametersValues() {
        Log.i("HHO"," check parameteres values ");

        // *** Activation_of_emotions
        if(teamChatBuddyApplication.getParamFromFile("Activation_of_emotions","TeamChatBuddy.properties").toLowerCase().contains("hid")){
            teamChatBuddyApplication.setparam("switch_emotion",teamChatBuddyApplication.getParamFromFile("Activation_of_emotions","TeamChatBuddy.properties").toLowerCase());

        }
        else{
            if(teamChatBuddyApplication.getparam("switch_emotion").contains("hid")){
                teamChatBuddyApplication.setparam("switch_emotion",teamChatBuddyApplication.getParamFromFile("Activation_of_emotions","TeamChatBuddy.properties").toLowerCase());
            }
        }
        // *** Display of speech
        if(teamChatBuddyApplication.getParamFromFile("Display_of_speech","TeamChatBuddy.properties").toLowerCase().contains("hid")){
            teamChatBuddyApplication.setparam("switch_visibility",teamChatBuddyApplication.getParamFromFile("Display_of_speech","TeamChatBuddy.properties").toLowerCase());
        }
        else{
            if(teamChatBuddyApplication.getparam("switch_visibility").contains("hid")){
                teamChatBuddyApplication.setparam("switch_visibility",teamChatBuddyApplication.getParamFromFile("Display_of_speech","TeamChatBuddy.properties").toLowerCase());
            }
        }

        // *** Stimulis
        if(teamChatBuddyApplication.getParamFromFile("Stimulis","TeamChatBuddy.properties").toLowerCase().contains("hid")){
            teamChatBuddyApplication.setparam("Stimulis",teamChatBuddyApplication.getParamFromFile("Stimulis","TeamChatBuddy.properties").toLowerCase());
        }
        else{
            if(teamChatBuddyApplication.getparam("Stimulis").contains("hid")){
                teamChatBuddyApplication.setparam("Stimulis",teamChatBuddyApplication.getParamFromFile("Stimulis","TeamChatBuddy.properties").toLowerCase());
            }
        }

        // *** Detection_de_langue
        if(teamChatBuddyApplication.getParamFromFile("Language_detection","TeamChatBuddy.properties").toLowerCase().contains("hid")){
            teamChatBuddyApplication.setparam("Detection_de_langue",teamChatBuddyApplication.getParamFromFile("Language_detection","TeamChatBuddy.properties").toLowerCase());
        }
        else{
            if(teamChatBuddyApplication.getparam("Detection_de_langue").contains("hid")){
                teamChatBuddyApplication.setparam("Detection_de_langue",teamChatBuddyApplication.getParamFromFile("Language_detection","TeamChatBuddy.properties").toLowerCase());
            }
        }
        // *** Commands
        if(teamChatBuddyApplication.getParamFromFile("Commands","TeamChatBuddy.properties").toLowerCase().contains("hid")){
            teamChatBuddyApplication.setparam("Commands",teamChatBuddyApplication.getParamFromFile("Commands","TeamChatBuddy.properties").toLowerCase());
            Log.i("HHO"," Commands "+teamChatBuddyApplication.getparam("Commands"));
        }
        else{
            if(teamChatBuddyApplication.getparam("Commands").contains("hid")){
                teamChatBuddyApplication.setparam("Commands",teamChatBuddyApplication.getParamFromFile("Commands","TeamChatBuddy.properties").toLowerCase());
            }
        }
        // *** tracking
        if(teamChatBuddyApplication.getParamFromFile("Tracking","TeamChatBuddy.properties").toLowerCase().contains("hid")){
            teamChatBuddyApplication.setparam("Tracking_Activation",teamChatBuddyApplication.getParamFromFile("Tracking","TeamChatBuddy.properties").toLowerCase());
        }
        else{
            if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("hid")){
                teamChatBuddyApplication.setparam("Tracking_Activation",teamChatBuddyApplication.getParamFromFile("Tracking","TeamChatBuddy.properties").toLowerCase());
            }
        }

        // *** Mode stream
        if(teamChatBuddyApplication.getParamFromFile("Stream_mode","TeamChatBuddy.properties").toLowerCase().contains("hid")){
            teamChatBuddyApplication.setparam("Mode_Stream",teamChatBuddyApplication.getParamFromFile("Stream_mode","TeamChatBuddy.properties").toLowerCase());
        }
        else{
            if(teamChatBuddyApplication.getparam("Mode_Stream").contains("hid")){
                teamChatBuddyApplication.setparam("Mode_Stream",teamChatBuddyApplication.getParamFromFile("Stream_mode","TeamChatBuddy.properties").toLowerCase());
            }
        }
    }


    private void getData(){

        //Visibility of Settings button
        if (teamChatBuddyApplication.getParamFromFile("Options_Access","TeamChatBuddy.properties")!=null && teamChatBuddyApplication.getParamFromFile("Options_Access","TeamChatBuddy.properties").trim().equalsIgnoreCase("Yes")){
            if (teamChatBuddyApplication.getParamFromFile("Number_clicks_options","TeamChatBuddy.properties")!=null &&
                    Integer.parseInt(teamChatBuddyApplication.getParamFromFile("Number_clicks_options","TeamChatBuddy.properties"))!=0){
                lyt_open_menu_settings.setVisibility(View.VISIBLE);
            }
            else if (teamChatBuddyApplication.getParamFromFile("Number_clicks_options","TeamChatBuddy.properties")==null){
                lyt_open_menu_settings.setVisibility(View.VISIBLE);
            }
        }
        else {
            lyt_open_menu_settings.setVisibility(View.INVISIBLE);
        }
        //init Settings
        settingClass=new Setting();
        String listeningDuration =teamChatBuddyApplication.getParamFromFile("Listening_time","TeamChatBuddy.properties");
        String listeningAttempt = teamChatBuddyApplication.getParamFromFile("Number_listens","TeamChatBuddy.properties");
        if(listeningDuration.equals("")||Integer.parseInt(listeningDuration)<=0){
            settingClass.setDuration("10");
        }
        else{
            settingClass.setDuration(listeningDuration);
        }
        if(listeningAttempt.equals("")||Integer.parseInt(listeningAttempt)<=0){
            settingClass.setAttempt("1");
        }
        else{
            settingClass.setAttempt(listeningAttempt);
        }
        settingClass.setChatbot("ChatGPT");
        settingClass.setLangue(teamChatBuddyApplication.getLangue().getNom());
        settingClass.setVolume(teamChatBuddyApplication.getparam("speak_volume"));
        if(teamChatBuddyApplication.getparam("switch_visibility").contains("yes")){
            settingClass.setSwitchVisibility("true");
        }else{
            settingClass.setSwitchVisibility("false");
        }

        if(teamChatBuddyApplication.getparam("switch_emotion").contains("yes")){
            settingClass.setSwitchEmotion("true");
        }else{
            settingClass.setSwitchEmotion("false");
        }

        Log.i(TAG, settingClass.toString());
        if (teamChatBuddyApplication.getparam("Stimulis").contains("yes")){
            Log.e("MRARA","disnable Raise event Stimilus");
            BuddySDK.Companion.raiseEvent("disableRightEye");
            BuddySDK.Companion.raiseEvent("disableLeftEye");
            BuddySDK.Companion.raiseEvent("disableHeadSensors");
            BuddySDK.Companion.raiseEvent("disableBodySensors");
            timerToApplyBI=new CountDownTimer(10*1000,1000) {
                @Override
                public void onTick(long l) {

                }

                @Override
                public void onFinish() {
                    applyBIAfterDelay=true;
                }
            };
            timerToApplyBI.start();
        }else {
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

        wifiBroadCastReceiver.setAct(getApplicationContext());
        IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(wifiBroadCastReceiver, intentFilter);
        wifiBroadCastReceiver.forceCheckConnexState(getApplicationContext());

        refreshSTTLangue();

        //set volume
        teamChatBuddyApplication.setVolume(Integer.parseInt(teamChatBuddyApplication.getparam("speak_volume")),AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);

        //create Log file
        if (teamChatBuddyApplication.getFileCreate()) {
            teamChatBuddyApplication.listSessionClear();
            listRep.clear();
            teamChatBuddyApplication.setFileCreate(false);
        }

        //init chatbots
        responseFromChatbot = new ResponseFromChatbot(teamChatBuddyApplication,this);


        Long mLkit_timeout_in_seconds = (long) Integer.parseInt(teamChatBuddyApplication.getParamFromFile("MLkit_timeout_in_seconds", "TeamChatBuddy.properties")) * 1000;

        timerDownloading = new CountDownTimer(mLkit_timeout_in_seconds, 1000) {
            @Override
            public void onTick(long l) {
                if(l < mLkit_timeout_in_seconds - 1000) {
                    if (mlKitIsDownloading) {
                        if (isConnected) {
                            showToastMessage();
                        }

                    }
                }
            }

            @Override
            public void onFinish() {
                if(teamChatBuddyApplication.getparam("previousLanguage")!=null && !teamChatBuddyApplication.getparam("previousLanguage").trim().equals("")) {
                    if (currentToast != null) currentToast.cancel();
                    launch_view.setVisibility(View.GONE);
                    Toast.makeText(getApplicationContext(), R.string.toast_message_error_downloading_en, Toast.LENGTH_LONG).show();
                    teamChatBuddyApplication.setLangue(new Gson().fromJson(teamChatBuddyApplication.getparam("previousLanguage"), Langue.class));
                    setPreviousLanguage();
                }
                else{
                    timerDownloading.start();
                }

            }
        };

        teamChatBuddyApplication.setActivityClosed(false);

        teamChatBuddyApplication.initTTSAndroid();
        teamChatBuddyApplication.initTTSGoogleCoud();

    }

    public void setPreviousLanguage() {
        List<Langue> langues = new ArrayList<>();
        List<String> langueDisponible = teamChatBuddyApplication.getDisponibleLangue();
        for (int i = 0; i < langueDisponible.size(); i++) {
            langues.add(new Gson().fromJson(teamChatBuddyApplication.getparam(langueDisponible.get(i)), Langue.class));
        }
        if (langues.isEmpty()) {
            langues.add(new Gson().fromJson(teamChatBuddyApplication.getparam("Français"), Langue.class));
        }

        if (langues != null && !langues.isEmpty()) {
            for (Langue langue : langues) {
                langue.setChosen(false);
                if (langue.getNom().equalsIgnoreCase(new Gson().fromJson(teamChatBuddyApplication.getparam("previousLanguage"), Langue.class).getNom())) {
                    langue.setChosen(true);
                    teamChatBuddyApplication.setLangue(langue);
                }
                if (langue.getNom().equals("Français")) {
                    teamChatBuddyApplication.setparam("Français", new Gson().toJson(langue));
                } else if (langue.getNom().equals("Anglais")) {
                    teamChatBuddyApplication.setparam("Anglais", new Gson().toJson(langue));
                } else if (langue.getNom().equals("Espagnol")) {
                    teamChatBuddyApplication.setparam("Espagnol", new Gson().toJson(langue));
                } else if (langue.getNom().equals("Allemand")) {
                    teamChatBuddyApplication.setparam("Allemand", new Gson().toJson(langue));
                } else {
                    teamChatBuddyApplication.setparam(langue.getNom(), new Gson().toJson(langue));
                }
            }
        }
    }


    private void showToastMessage() {
        if (teamChatBuddyApplication.getLangue().getNom().equals(langueEn)) {
            currentToast = Toast.makeText(MainActivity.this, R.string.mlkit_model_is_downloading_en, Toast.LENGTH_SHORT);
        } else if (teamChatBuddyApplication.getLangue().getNom().equals(langueFr)) {
            currentToast = Toast.makeText(MainActivity.this, R.string.mlkit_model_is_downloading_fr, Toast.LENGTH_SHORT);
        } else {
            currentToast = Toast.makeText(MainActivity.this, R.string.mlkit_model_is_downloading_en, Toast.LENGTH_SHORT);
        }
        currentToast.show();
    }


    private void showStream(String responseTitle,String response) {
        if (!teamChatBuddyApplication.isActivityClosed()) {
            buddy_texte_resp.setText(String.format(responseTitle + " :  %s ", response));
            buddy_texte_resp_lyt.setVisibility(View.VISIBLE);
            buddy_texte_resp.setMovementMethod(new ScrollingMovementMethod());
            // Scroll to the end
            buddy_texte_resp.post(new Runnable() {
                @Override
                public void run() {
                    int scrollAmount = buddy_texte_resp.getLayout().getLineTop(buddy_texte_resp.getLineCount())
                            - buddy_texte_resp.getHeight() + buddy_texte_resp.getLineHeight();
                    if (scrollAmount > 0) {
                        buddy_texte_resp.scrollTo(0, scrollAmount);
                    } else {
                        buddy_texte_resp.scrollTo(0, 0);
                    }
                }
            });
        }
    }

    private void afficherPopupAvecBitmap(String imagePath) {
        if(dialog != null && dialog.isShowing()) dialog.dismiss();
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_layout);
        ImageView imageView = dialog.findViewById(R.id.imageView);
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        imageView.setImageBitmap(bitmap);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().getDecorView().setPadding(0, 0, 0, 0);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(dialog.getWindow().getAttributes());
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.gravity = Gravity.CENTER;
        layoutParams.horizontalMargin = 0;
        layoutParams.verticalMargin = 0;
        dialog.getWindow().setAttributes(layoutParams);
        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        dialog.show();
    }

    private void closeImage(){
        if(dialog != null && dialog.isShowing()) dialog.dismiss();
    }

    private void startCameraForCommand(String promptPhoto) {
        Log.e("CameraX", " starting camera1");
        preViewViewLyt = findViewById(R.id.previewView_lyt);
        previewViewphoto = findViewById(R.id.previewView);
        cameraSelector = new CameraSelector.Builder().requireLensFacing(cameraFacing).build();
        previewViewphoto.post(() -> {
            try {
                if (teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes")) {
                    stopTracking();
                }
                cameraProvider = ProcessCameraProvider.getInstance(this).get();
                // Récupérer la vue PreviewView avec findViewById()

                preViewViewLyt.setVisibility(View.VISIBLE);

                previewViewphoto.setTranslationX(0);

                preview = new Preview.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .build();

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY) // Assurez-vous d'avoir un mode de capture
                        .setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation()) // Définir la rotation correcte
                        .build();

                cameraProvider.unbindAll();

                // Utiliser previewView pour définir la SurfaceProvider
                preview.setSurfaceProvider(previewViewphoto.getSurfaceProvider());

                // Liaison de la caméra aux use cases
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
                capturePhoto(promptPhoto);
                Log.i(TAG, "Camera use cases bound successfully");
            } catch (Exception e) {
                Log.e(TAG, "Use case binding failed", e);
            }
        });
    }

    public void capturePhoto(String promptPhoto) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                timerPhoto = new CountDownTimer(3*1000L, 1000) {
                    public void onTick(long millisUntilFinished) {
                        showTimer(millisUntilFinished);
                    }
                    public void onFinish() {
                        hideTimer();
                        imageCapture.takePicture(ContextCompat.getMainExecutor(MainActivity.this), new ImageCapture.OnImageCapturedCallback() {
                            @Override
                            public void onCaptureSuccess(@NonNull ImageProxy image) {
                                commande.phototakedMessage();
                                Bitmap bitmap = imageProxyToBitmap(image);
                                showImageInDialog(bitmap);
                                responseFromChatbot.getQuestionToDescribePicture(bitmap,promptPhoto);
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        stopCamera();
                                    }
                                },1000);
                                if(timerPhoto != null) timerPhoto.cancel();
                                if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes")){
                                    isReTrack = true;
                                    initTracking();
                                }
                            }
                            @Override
                            public void onError(@NonNull ImageCaptureException exception) {
                                Toast.makeText(MainActivity.this, "Erreur lors de la capture de l'image", Toast.LENGTH_SHORT).show();
                                Log.e("CameraX", "Image capture error", exception);
                                if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes")){
                                    isReTrack = true;
                                    initTracking();
                                }
                            }
                        });
                    }
                };
                timerPhoto.start();
            }
        });
    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
    }

    private void showImageInDialog(Bitmap imageBitmap) {
        if(dialog != null && dialog.isShowing()) dialog.dismiss();
        dialog = new Dialog(this);

        dialog.setContentView(R.layout.dialog_layout);

        ImageView imageView = dialog.findViewById(R.id.imageView);
        imageView.setImageBitmap(imageBitmap);

        // Ajoutez un écouteur de clic à l'image pour fermer la boîte de dialogue lorsqu'elle est cliquée
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        // To remove padding/margin/color of dialog (full screen image)
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().getDecorView().setPadding(0, 0, 0, 0);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(dialog.getWindow().getAttributes());
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.gravity = Gravity.CENTER;
        layoutParams.horizontalMargin = 0;
        layoutParams.verticalMargin = 0;
        dialog.getWindow().setAttributes(layoutParams);

        // Affichez la boîte de dialogue
        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        dialog.show();
    }

    public void stopCamera() {
        if (cameraProvider != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    cameraProvider.unbindAll();
                    if (previewViewphoto != null) {
                        previewViewphoto.setTranslationX(1000000000);

                    }
                }
            });
        }
    }

    private void showTimer(long millisUntilFinished){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                photo_timer_rlyt.setVisibility(View.VISIBLE);
                photo_timer_bg_rlyt.setVisibility(View.VISIBLE);
                photo_timer_imgview.setImageDrawable(animationTimerPhoto);
                String millisUntilFinishedString = Long.toString(( millisUntilFinished / 1000 ) + 1);
                photo_timer_txtView.setText(millisUntilFinishedString);
                animationTimerPhoto.start();
            }
        });

    }

    private void hideTimer(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                photo_timer_rlyt.setVisibility(View.GONE);
                photo_timer_bg_rlyt.setVisibility(View.GONE);
                photo_timer_txtView.setText("");
            }
        });

    }

    private void detectBI(){
        boolean head_top_touched = BuddySDK.Sensors.HeadTouchSensors().Top().isTouched();
        boolean head_left_touched = BuddySDK.Sensors.HeadTouchSensors().Left().isTouched();
        boolean head_right_touched = BuddySDK.Sensors.HeadTouchSensors().Right().isTouched();
        boolean body_torso_touched = BuddySDK.Sensors.BodyTouchSensors().Torso().isTouched();
        boolean body_left_touched = BuddySDK.Sensors.BodyTouchSensors().LeftShoulder().isTouched();
        boolean body_right_touched = BuddySDK.Sensors.BodyTouchSensors().RightShoulder().isTouched();
        Log.i("DEBUG_BI","detectBI : "+head_top_touched+"   -   "+head_left_touched+"   -   "+head_right_touched+"   -   "+body_torso_touched+"   -   "+body_left_touched+"   -   "+body_right_touched);
        if (!teamChatBuddyApplication.getAppIsCurrentlyDealingWithTheQuestion() && !currentlyBIExecuted){
            if (head_top_touched){
                if (teamChatBuddyApplication.getParamFromFile("touchCenterHead_Behavior", "TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.getParamFromFile("touchCenterHead_Behavior", "TeamChatBuddy.properties").trim().equalsIgnoreCase("")){
                    executeBI("touchCenterHead_Behavior");
                }
            }
            else if (head_left_touched){
                if (teamChatBuddyApplication.getParamFromFile("touchLeftHead_Behavior", "TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.getParamFromFile("touchLeftHead_Behavior", "TeamChatBuddy.properties").trim().equalsIgnoreCase("")){
                    executeBI("touchLeftHead_Behavior");
                }
            }
            else if (head_right_touched){
                if (teamChatBuddyApplication.getParamFromFile("touchRightHead_Behavior", "TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.getParamFromFile("touchRightHead_Behavior", "TeamChatBuddy.properties").trim().equalsIgnoreCase("")){
                    executeBI("touchRightHead_Behavior");
                }
            }
            else if (body_torso_touched){
                if (teamChatBuddyApplication.getParamFromFile("touchHeart_Behavior", "TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.getParamFromFile("touchHeart_Behavior", "TeamChatBuddy.properties").trim().equalsIgnoreCase("")){
                    executeBI("touchHeart_Behavior");
                }
            }
            else if (body_left_touched){
                if (teamChatBuddyApplication.getParamFromFile("touchLeftShoulder_Behavior", "TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.getParamFromFile("touchLeftShoulder_Behavior", "TeamChatBuddy.properties").trim().equalsIgnoreCase("")){
                    executeBI("touchLeftShoulder_Behavior");
                }
            }
            else if (body_right_touched){
                if (teamChatBuddyApplication.getParamFromFile("touchRightShoulder_Behavior", "TeamChatBuddy.properties")!=null && !teamChatBuddyApplication.getParamFromFile("touchRightShoulder_Behavior", "TeamChatBuddy.properties").trim().equalsIgnoreCase("")){
                    executeBI("touchRightShoulder_Behavior");
                }
            }
        }
    }

    private String getTheRightBINameFromConfigFile(String propertyName){
        String xmlBehaviour;
        if (!propertyName.equals("") && teamChatBuddyApplication.getParamFromFile(propertyName, "TeamChatBuddy.properties")!=null) {
            StringTokenizer st = new StringTokenizer(teamChatBuddyApplication.getParamFromFile(propertyName, "TeamChatBuddy.properties"), "/", false);
            List<String> listBehaviour = new ArrayList<>();
            while (st.hasMoreTokens()) {
                String result = st.nextToken();
                listBehaviour.add(result.toLowerCase());
            }
            if (listBehaviour.size() > 0) {
                xmlBehaviour = listBehaviour.get(new Random().nextInt(listBehaviour.size())).trim();
            } else {
                xmlBehaviour = "";
            }
        }
        else xmlBehaviour = "";
        Log.i("DEBUG_BI","xmlBehaviour récupéré depuis le fichier de configuration  "+xmlBehaviour);
        String dossierExterne = getString(R.string.path) + "/BI/Behaviour";
        if (!xmlBehaviour.contains(".xml") && !xmlBehaviour.equals("")){
            List<String> nomsFichiers = getFilenamesForCategory(dossierExterne, xmlBehaviour);
            if (!nomsFichiers.isEmpty()) {
                xmlBehaviour = nomsFichiers.get(new Random().nextInt(nomsFichiers.size()));
                Log.i("DEBUG_BI","Nom du fichier choisi : " + xmlBehaviour);
            } else {
                Log.i("DEBUG_BI","Aucun fichier trouvé pour la catégorie : " + xmlBehaviour);
            }
            Log.i("DEBUG_BI","Nom du fichier selon la  catégorie choisi : " + xmlBehaviour);
            return xmlBehaviour;
        }
        else if (xmlBehaviour.equals("")) {
            Log.i("DEBUG_BI", "Le fichier n'existe pas.");
            return "";
        }
        else {
            Log.i("DEBUG_BI", "xmlBehaviour : "+xmlBehaviour);
            return xmlBehaviour;
        }
    }

    private static List<String> getFilenamesForCategory(String dossier, String categorieRecherchee) {
        List<String> nomsFichiers = new ArrayList<>();
        File dossierBehaviours = new File(dossier);
        File[] fichiers = dossierBehaviours.listFiles();
        if (fichiers != null) {
            for (File fichier : fichiers) {
                if (fichier.isFile() && fichier.getName().toLowerCase().endsWith(".xml")) {
                    if (fichier.getName().toLowerCase().contains(categorieRecherchee.toLowerCase())) {
                        nomsFichiers.add(fichier.getName());
                    }
                }
            }
        }
        return nomsFichiers;
    }

    private void executeBI(String behaviour){
        if (applyBIAfterDelay) {
            Log.i("DEBUG_BI", "executeBI : " + behaviour);
            teamChatBuddyApplication.setBIExecution(true);
            currentlyBIExecuted= true;
            BIPlayer.getInstance().playBI(this, getTheRightBINameFromConfigFile(behaviour), new IBehaviourCallBack() {
                @Override
                public void onEnd(boolean hasAborted, String reason) {
                    Log.e("DEBUG_BI", "on END BI execution");
                    BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                    BuddySDK.UI.lookAt(GazePosition.CENTER, true);
                    currentlyBIExecuted = false;
                    teamChatBuddyApplication.setBIExecution(false);
                    currentTrackingListeningState = StateTrackingListening.PERSON_IS_NOT_VISIBLE_TIMEOUT;
                    totalTimeLookingAtCamera = 0L;
                    teamChatBuddyApplication.setSpeaking(false);
                }

                @Override
                public void onRun(String s) {
                }
            });
        }
    }
    private void playStartMessage(IStartMessageCallback iStartMessageCallback){
        this.iStartMessageCallback = iStartMessageCallback;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                    //Get invitation from config File
                    if (teamChatBuddyApplication.getLangue().getNom().equals("Anglais")) {
                        if(teamChatBuddyApplication.getParamFromFile("Start_Message_en","TeamChatBuddy.properties") != null && !teamChatBuddyApplication.getParamFromFile("Start_Message_en","TeamChatBuddy.properties") .isEmpty()){
                            teamChatBuddyApplication.setActivityClosed(false);
                            speak(teamChatBuddyApplication.getParamFromFile("Start_Message_en","TeamChatBuddy.properties"), "STARTMESSAGE");
                        }
                        else {
                            if(iStartMessageCallback != null) iStartMessageCallback.onEnd("ConfigFile do not contain English Start Message");
                        }
                    }
                    else if (teamChatBuddyApplication.getLangue().getNom().equals("Français")) {
                        if(teamChatBuddyApplication.getParamFromFile("Start_Message_fr","TeamChatBuddy.properties") != null && !teamChatBuddyApplication.getParamFromFile("Start_Message_fr","TeamChatBuddy.properties").isEmpty()){
                            teamChatBuddyApplication.setActivityClosed(false);
                            speak(teamChatBuddyApplication.getParamFromFile("Start_Message_fr","TeamChatBuddy.properties"), "STARTMESSAGE");
                        }
                        else {
                            if(iStartMessageCallback != null) iStartMessageCallback.onEnd("ConfigFile do not contain French Start Message");
                        }
                    }
                    else {
                        if(teamChatBuddyApplication.getParamFromFile("Start_Message_en","TeamChatBuddy.properties") != null && !teamChatBuddyApplication.getParamFromFile("Start_Message_en","TeamChatBuddy.properties").isEmpty()){
                            teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(teamChatBuddyApplication.getParamFromFile("Start_Message_en","TeamChatBuddy.properties"))
                                    .addOnSuccessListener(new OnSuccessListener<String>() {
                                        @Override
                                        public void onSuccess(String translatedText) {
                                            Log.d(TAG_TRACKING, "Translated Invitation: " + translatedText);
                                            teamChatBuddyApplication.setActivityClosed(false);
                                            speak(translatedText, "STARTMESSAGE");
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            teamChatBuddyApplication.setActivityClosed(false);
                                            speak(teamChatBuddyApplication.getParamFromFile("Start_Message_en","TeamChatBuddy.properties"), "INVITATION");
                                        }
                                    });
                        }
                        else {
                            if(iStartMessageCallback != null) iStartMessageCallback.onEnd("ConfigFile do not contain English Start Message");
                        }
                    }

            }
        });


    }

    private void refreshSTTLangue() {
        List<String> STTAndroidLangueCode = teamChatBuddyApplication.getLanguageCodeForDisponibleLangue("Language_Code_Used_In_STT_Android");
        String codeLanguageSTTAndroid = STTAndroidLangueCode.get(new Gson().fromJson(teamChatBuddyApplication.getparam(settingClass.getLangue()), Langue.class).getId()-1);
        teamChatBuddyApplication.refresh(codeLanguageSTTAndroid,this);
    }

    private void setAnimation(String emotion){
        teamChatBuddyApplication.setCurrentEmotion("");
        if (teamChatBuddyApplication.separator(teamChatBuddyApplication.getParamFromFile("BuddyFace_Happy", "TeamChatBuddy.properties").trim().toLowerCase()).contains(emotion)){
            BuddySDK.UI.setFacialExpression(FacialExpression.HAPPY,1);
        }
        else if (teamChatBuddyApplication.separator(teamChatBuddyApplication.getParamFromFile("BuddyFace_Thinking", "TeamChatBuddy.properties").trim().toLowerCase()).contains(emotion)){
            BuddySDK.UI.setFacialExpression(FacialExpression.THINKING,1);
            teamChatBuddyApplication.setLed("thinking");
        }
        else if (teamChatBuddyApplication.separator(teamChatBuddyApplication.getParamFromFile("BuddyFace_Sick", "TeamChatBuddy.properties").trim().toLowerCase()).contains(emotion)){
            BuddySDK.UI.setFacialExpression(FacialExpression.SICK,1);
        }
        else if (teamChatBuddyApplication.separator(teamChatBuddyApplication.getParamFromFile("BuddyFace_Love", "TeamChatBuddy.properties").trim().toLowerCase()).contains(emotion)){
            BuddySDK.UI.setFacialExpression(FacialExpression.LOVE,1);
        }
        else if (teamChatBuddyApplication.separator(teamChatBuddyApplication.getParamFromFile("BuddyFace_Tired", "TeamChatBuddy.properties").trim().toLowerCase()).contains(emotion)){
            BuddySDK.UI.setFacialExpression(FacialExpression.TIRED,1);
        }
        else if (teamChatBuddyApplication.separator(teamChatBuddyApplication.getParamFromFile("BuddyFace_Listening", "TeamChatBuddy.properties").trim().toLowerCase()).contains(emotion)){
            BuddySDK.UI.setFacialExpression(FacialExpression.LISTENING,1);
            teamChatBuddyApplication.setLed("listening");
        }
        else if (teamChatBuddyApplication.separator(teamChatBuddyApplication.getParamFromFile("BuddyFace_Surprised", "TeamChatBuddy.properties").trim().toLowerCase()).contains(emotion)){
            BuddySDK.UI.setFacialExpression(FacialExpression.SURPRISED,1);
        }
        else if (teamChatBuddyApplication.separator(teamChatBuddyApplication.getParamFromFile("BuddyFace_Grumpy", "TeamChatBuddy.properties").trim().toLowerCase()).contains(emotion)){
            BuddySDK.UI.setFacialExpression(FacialExpression.GRUMPY,1);
        }
        else if (teamChatBuddyApplication.separator(teamChatBuddyApplication.getParamFromFile("BuddyFace_Scared", "TeamChatBuddy.properties").trim().toLowerCase()).contains(emotion)){
            BuddySDK.UI.setFacialExpression(FacialExpression.SCARED,1);
        }
        else if (teamChatBuddyApplication.separator(teamChatBuddyApplication.getParamFromFile("BuddyFace_Angry", "TeamChatBuddy.properties").trim().toLowerCase()).contains(emotion)){
            BuddySDK.UI.setFacialExpression(FacialExpression.ANGRY,1);
        }
        else if (teamChatBuddyApplication.separator(teamChatBuddyApplication.getParamFromFile("BuddyFace_Sad", "TeamChatBuddy.properties").trim().toLowerCase()).contains(emotion)){
            BuddySDK.UI.setFacialExpression(FacialExpression.SAD,1);
        }
        else{
            BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL,1);
        }
    }

    public void btnOpenSettings(View view) {
        // Réinitialiser le timeout précédent
        if (teamChatBuddyApplication.getParamFromFile("Number_clicks_options","TeamChatBuddy.properties")!=null) {
            clickHandler.removeCallbacks(resetClickCountRunnable);
            clickCount++;
            if (clickCount >= Integer.parseInt(teamChatBuddyApplication.getParamFromFile("Number_clicks_options", "TeamChatBuddy.properties"))) {
                if (!mlKitIsDownloading) {
                    Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                    intent.putExtra("activity_name", "main");
                    startActivity(intent);
                    finish();

                    overridePendingTransition(0, 0);
                } else if (teamChatBuddyApplication.getBIExecution()) {
                    BIPlayer.getInstance().stopBehaviour();
                    Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                    intent.putExtra("activity_name", "main");
                    startActivity(intent);
                    finish();

                    overridePendingTransition(0, 0);
                }
            } else {
                // Redémarrer le délai avant de réinitialiser le compteur
                clickHandler.postDelayed(resetClickCountRunnable, CLICK_TIMEOUT);
            }
        }
        else {
            if (!mlKitIsDownloading) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                intent.putExtra("activity_name", "main");
                startActivity(intent);
                finish();

                overridePendingTransition(0, 0);
            } else if (teamChatBuddyApplication.getBIExecution()) {
                BIPlayer.getInstance().stopBehaviour();
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                intent.putExtra("activity_name", "main");
                startActivity(intent);
                finish();

                overridePendingTransition(0, 0);
            }
        }
    }

    public void btnOpenChat(View view) {
        if ( !mlKitIsDownloading) {
            Intent intent = new Intent(MainActivity.this, ChatWindow.class);
            startActivity(intent);
            finish();
            overridePendingTransition(0, 0);
        }
        else if (teamChatBuddyApplication.getBIExecution()){
            BIPlayer.getInstance().stopBehaviour();
            Intent intent = new Intent(MainActivity.this, ChatWindow.class);
            startActivity(intent);
            finish();
            overridePendingTransition(0, 0);
        }
    }


    /**
     * ------------------------------------------ STT  -------------------------------------------
     */

    private void startListeningFreeSpeech(int duration) {
        Boolean notUsingSpeechRecognizer = true;
        isListeningFreeSpeech = true;
        teamChatBuddyApplication.setMessageError(false);
        teamChatBuddyApplication.setOpenaialreadySwitchEmotion(false);
        teamChatBuddyApplication.setAppIsListeningToTheQuestion(true);
        teamChatBuddyApplication.setAlreadyChatting(false);

        Log.d(TAG," --- startListeningFreeSpeech("+duration+") ---");

        if(teamChatBuddyApplication.getBlueMic() != null
                && teamChatBuddyApplication.getBlueMic().getmBlueMicService() != null
                && teamChatBuddyApplication.getBlueMic().selectedBlueMic != null
                && teamChatBuddyApplication.getBlueMic().selectedBlueMic.getState().equals("Connected")){
            teamChatBuddyApplication.startListeningBlueMic(false, this);
        }
        else{
            if (teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Android")){
                notUsingSpeechRecognizer=false;
                teamChatBuddyApplication.startListeningQuestion(this,"FirstListening");
            }
            else if (teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Whisper")) {
                teamChatBuddyApplication.startWhisperRecording(this);
            }
            else if (teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Cerence")){
                if (teamChatBuddyApplication.getCurrentLanguage().equals("fr") || teamChatBuddyApplication.getCurrentLanguage().equals("en")){
                    teamChatBuddyApplication.startListeningCerence(this);
                }
                else{
                    notUsingSpeechRecognizer=false;
                    teamChatBuddyApplication.startListeningQuestion(this,"FirstListening");
                }
            }
            else teamChatBuddyApplication.startListeningQuestionWithGoogleApi(this);
        }
        if (notUsingSpeechRecognizer) {
            if (!teamChatBuddyApplication.isModeContinuousListeningON()) {
                if (timerEcoute != null) timerEcoute.cancel();
                timerEcoute = new CountDownTimer(duration * 1000L, 1000) {
                    @Override
                    public void onTick(long l) {
                        Log.d(TAG, "timerEcoute onTick");
                    }

                    @Override
                    public void onFinish() {
                        if (teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes") && Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Auto_Listen")) && regarde_camera) {
                            Log.i(TAG_TRACKING, "timerEcoute onFinish --> Do not stop listening because tracking auto listen is enabled and user is looking directly at camera --> restart timer");
                            timerEcoute.start();
                        } else {
                            Log.i(TAG, "timerEcoute onFinish");
                            if (teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Android") || teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Cerence")) {
                                teamChatBuddyApplication.notifyObservers("end of timer");
                            } else {
                                teamChatBuddyApplication.notifyObservers("Obtain audio transcription after the listening time has elapsed;SPLIT;false");
                            }
                        }
                    }
                };
                timerEcoute.start();
            }
        }
        else {
            notUsingSpeechRecognizer = true;
        }

    }

    private void stopListeningFreeSpeech() {
        isListeningFreeSpeech = false;
        Log.d(TAG," --- stopListeningFreeSpeech() ---");
        if (timerEcoute!=null) timerEcoute.cancel();
        teamChatBuddyApplication.stopListening(this);
    }

    private void startCycle() {
        Log.e(TAG,"startCycle  after handler ");
        Boolean notUsingSpeechRecognizer =true;
        isListeningFreeSpeech = true;
        teamChatBuddyApplication.setMessageError(false);
        teamChatBuddyApplication.setOpenaialreadySwitchEmotion(false);
        teamChatBuddyApplication.setAppIsListeningToTheQuestion(true);
        teamChatBuddyApplication.setAlreadyChatting(false);

        if(teamChatBuddyApplication.getBlueMic() != null
                && teamChatBuddyApplication.getBlueMic().getmBlueMicService() != null
                && teamChatBuddyApplication.getBlueMic().selectedBlueMic != null
                && teamChatBuddyApplication.getBlueMic().selectedBlueMic.getState().equals("Connected")){
            teamChatBuddyApplication.startListeningBlueMic(false, this);
        }
        else{
            if (teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Android")){
                notUsingSpeechRecognizer=false;
                teamChatBuddyApplication.startListeningQuestion(this,"startCycle");
            }
            else if (teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Whisper")) {
                teamChatBuddyApplication.startWhisperRecording(this);
            }
            else if (teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Cerence")){
                if (teamChatBuddyApplication.getCurrentLanguage().equals("fr") || teamChatBuddyApplication.getCurrentLanguage().equals("en")){
                    teamChatBuddyApplication.startListeningCerence(this);
                }
                else{
                    notUsingSpeechRecognizer=false;
                    teamChatBuddyApplication.startListeningQuestion(this,"startCycle");
                }
            }
            else teamChatBuddyApplication.startListeningQuestionWithGoogleApi(this);
        }
        if (notUsingSpeechRecognizer){
            if(timerEcoute != null) timerEcoute.cancel();
            timerEcoute = new CountDownTimer(teamChatBuddyApplication.getListeningDuration() * 1000L,1000) {
                @Override
                public void onTick(long l) {
                    Log.d(TAG, "timerEcoute onTick");
                }
                @Override
                public void onFinish() {
                    if(teamChatBuddyApplication.getparam("Tracking_Activation").contains("yes") && Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Auto_Listen")) && regarde_camera){
                        Log.i(TAG_TRACKING, "timerEcoute onFinish --> Do not stop listening because tracking auto listen is enabled and user is looking directly at camera --> restart timer");
                        timerEcoute.start();
                    }
                    else{
                        Log.i(TAG, "timerEcoute onFinish");
                        if (teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Android") || teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Cerence")){
                            teamChatBuddyApplication.notifyObservers("end of cycle");
                            runnablePauseTime =new Runnable() {
                                @Override
                                public void run() {
                                    startNextCycle();
                                    Log.e(TAG,"startNextCycle  after handler ");
                                }
                            };
                            handlerPauseTime.postDelayed(runnablePauseTime,1000);
                        }
                        else{
                            teamChatBuddyApplication.notifyObservers("Obtain audio transcription after the listening time has elapsed;SPLIT;true");
                        }
                    }
                }
            };
            timerEcoute.start();
        }

    }

    private void startNextCycle() {
        Log.e(TAG,"startNextCycle  remainingattempts= "+teamChatBuddyApplication.getRemainingAttempts());
        if (teamChatBuddyApplication.getRemainingAttempts() > 0) {
            teamChatBuddyApplication.setRemainingAttempts(teamChatBuddyApplication.getRemainingAttempts()-1);
            startCycle();
            Log.e(TAG,"startNextCycle  after handler ");
        }
        else {
            teamChatBuddyApplication.notifyObservers("end of timer");
        }
    }


    /**
     * ------------------------------------------ TTS  -------------------------------------------
     */

    private void speak(final String texte, String type) {
        Log.e("TEAMCHAT_BUDDY_TRACKING"," --- speak("+texte+") ---type="+type+" isActivityClosed="+teamChatBuddyApplication.isActivityClosed());
        isSpeaking = true;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                buddy_texte_resp_lyt.setTranslationY(0);
                if (responseTimeout!=null) responseTimeout.cancel();

                if (!teamChatBuddyApplication.getUsingEmotions()){
                    Log.d(TAG,"FacialExpression NEUTRAL");
                    BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL,1);
                }
                if (!teamChatBuddyApplication.isActivityClosed()) {
                    if (type.equals("nothealysa") || type.equals("storedResponse")) {
                        teamChatBuddyApplication.setAlreadyGetAnswer(true);
                        if ( (teamChatBuddyApplication.getparam("Mode_Stream").contains("no") && teamChatBuddyApplication.getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT") )
                                || ( teamChatBuddyApplication.getparam("Mode_Stream").contains("yes") && teamChatBuddyApplication.getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT") && teamChatBuddyApplication.getChatGptStreamMode() == null )
                        ){
                            if (settingClass.getSwitchVisibility().equals("true")) {
                                if (teamChatBuddyApplication.getCurrentLanguage().equals("en")) {
                                    buddy_texte_resp.setText(String.format("Response :  %s ", texte));
                                } else if (teamChatBuddyApplication.getCurrentLanguage().equals("fr")){
                                    buddy_texte_resp.setText(String.format("Réponse :  %s ", texte));
                                }
                                else if (teamChatBuddyApplication.getCurrentLanguage().equals("de")) {
                                    buddy_texte_resp.setText(String.format("Antwort :  %s ", texte));
                                } else if (teamChatBuddyApplication.getCurrentLanguage().equals("es")) {
                                    buddy_texte_resp.setText(String.format("Respuesta :  %s ", texte));
                                }
                                else{
                                    teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate("Response").addOnSuccessListener(new OnSuccessListener<String>() {
                                        @Override
                                        public void onSuccess(String translatedText) {
                                            buddy_texte_resp.setText(String.format(translatedText+" :  %s ", texte));
                                        }

                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.e(TAG,"translatedText exception  "+e);
                                        }
                                    });

                                }
                                if(type.equals("storedResponse")){
                                    lyt_open_menu_settings.setVisibility(View.INVISIBLE);
                                    lyt_open_menu_chat.setVisibility(View.INVISIBLE);
                                    if(buddy_texte_qst_lyt.getVisibility() != View.VISIBLE) buddy_texte_resp_lyt.setTranslationY(-155);
                                    else buddy_texte_resp_lyt.setTranslationY(0);
                                }
                                buddy_texte_resp_lyt.setVisibility(View.VISIBLE);
                                buddy_texte_resp.setMovementMethod(new ScrollingMovementMethod());
                                buddy_texte_resp.scrollTo(0, 0);
                            }
                        }

                        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
                        if ( (teamChatBuddyApplication.getparam("Mode_Stream").contains("no") && teamChatBuddyApplication.getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT") )
                                || ( teamChatBuddyApplication.getparam("Mode_Stream").contains("yes") && teamChatBuddyApplication.getparam("chatbot_chosen").equalsIgnoreCase("ChatGPT") && teamChatBuddyApplication.getChatGptStreamMode() == null )
                        ){
                            Replica reponse = new Replica();
                            reponse.setValue(texte);
                            reponse.setTime(time);
                            long responseTime = teamChatBuddyApplication.getGetResponseTime()- teamChatBuddyApplication.getQuestionTime();
                            DecimalFormat df = new DecimalFormat("#,###");
                            String formattedTime= df.format(responseTime);
                            reponse.setType("reponse");
                            reponse.setDuration(formattedTime + " ms");
                            DecimalFormat decimalFormatter = new DecimalFormat("0.00");
                            String formattedValue = decimalFormatter.format(Double.parseDouble(teamChatBuddyApplication.getparam("Total_cons")));
                            reponse.setPrix(formattedValue+" $");
                            listRep.add(reponse);
                            ArrayList<Replica> ll = new ArrayList<>();
                            for (int t = 0; t < listRep.size(); t++) {
                                ll.add(listRep.get(t));
                            }
                            Session session = new Session(ll);
                            ArrayList<Session> sessionTemp= teamChatBuddyApplication.getListSession();
                            sessionTemp.add(session);
                            teamChatBuddyApplication.setListSession(sessionTemp);
                            listRep.clear();
                        }
                        else{
                            if (!listRep.isEmpty()) {
                                //--> this function is called right after a question : we should create a new Replica for the response
                                Replica reponse = new Replica();
                                reponse.setValue(texte);
                                reponse.setTime(time);
                                long responseTime = teamChatBuddyApplication.getGetResponseTime() - teamChatBuddyApplication.getQuestionTime();
                                DecimalFormat df = new DecimalFormat("#,###");
                                String formattedTime= df.format(responseTime);
                                reponse.setType("reponse");
                                reponse.setDuration(formattedTime + " ms");
                                DecimalFormat decimalFormatter = new DecimalFormat("0.00");
                                String formattedValue = decimalFormatter.format(Double.parseDouble(teamChatBuddyApplication.getparam("Total_cons")));
                                reponse.setPrix(formattedValue+" $");
                                listRep.add(reponse);
                                Session session = new Session(new ArrayList<>(listRep));
                                teamChatBuddyApplication.getListSession().add(session);
                                listRep.clear();
                            }
                            else{
                                //---> this function is called after finishing pronouncing a phrase from the response : we should add the new phrase to the already existing Replica
                                ArrayList<Session> listSessions = teamChatBuddyApplication.getListSession();
                                ArrayList<Replica> lastSession = listSessions.get(listSessions.size() - 1).getSession();
                                Replica lastReplica = lastSession.get(lastSession.size() - 1);
                                if (lastReplica.getType().equals("reponse")) {
                                    if (teamChatBuddyApplication.getChatGptStreamMode()!=null){
                                        if(!teamChatBuddyApplication.getChatGptStreamMode().isError) lastReplica.setValue(lastReplica.getValue() + texte);
                                    }
                                    else if (teamChatBuddyApplication.getCustomGPTStreamMode()!=null) {
                                        if (!teamChatBuddyApplication.getCustomGPTStreamMode().isError)
                                            lastReplica.setValue(lastReplica.getValue() + texte);
                                    }
                                    else lastReplica.setValue(texte);
                                    DecimalFormat decimalFormatter = new DecimalFormat("0.00");
                                    String formattedValue = decimalFormatter.format(Double.parseDouble(teamChatBuddyApplication.getparam("Total_cons")));
                                    lastReplica.setPrix(formattedValue+" $");
                                }
                            }
                        }

                        teamChatBuddyApplication.speakTTS(texte, LabialExpression.SPEAK_NEUTRAL,type);
                    }
                    else if (type.equals("timeOutExpired")){
                        teamChatBuddyApplication.speakTTS(texte, LabialExpression.SPEAK_NEUTRAL,type);
                    }
                    else if (type.equals("commande")) {
                        teamChatBuddyApplication.setAlreadyGetAnswer( true );
                        if (!teamChatBuddyApplication.getMessageError() && !teamChatBuddyApplication.getUsingEmotions()) {
                            BuddySDK.UI.setFacialExpression( FacialExpression.NEUTRAL, 1 );
                        }

                        if (settingClass.getSwitchVisibility().equals( "true" )) {
                            if (teamChatBuddyApplication.getCurrentLanguage().equals( "en" )) {
                                buddy_texte_resp.setText( String.format( "Response :  %s ", texte ) );
                            } else if (teamChatBuddyApplication.getCurrentLanguage().equals( "fr" )) {
                                buddy_texte_resp.setText( String.format( "Réponse :  %s ", texte ) );
                            } else if (teamChatBuddyApplication.getCurrentLanguage().equals( "de" )) {
                                buddy_texte_resp.setText( String.format( "Antwort :  %s ", texte ) );
                            } else if (teamChatBuddyApplication.getCurrentLanguage().equals( "es" )) {
                                buddy_texte_resp.setText( String.format( "Respuesta :  %s ", texte ) );
                            } else {
                                teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate( "Response" ).addOnSuccessListener( new OnSuccessListener<String>() {
                                    @Override
                                    public void onSuccess(String translatedText) {
                                        buddy_texte_resp.setText( String.format( translatedText + " :  %s ", texte ) );
                                    }

                                } ).addOnFailureListener( new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.e( TAG, "translatedText exception  " + e );
                                    }
                                } );

                            }
                            lyt_open_menu_settings.setVisibility(View.INVISIBLE);
                            lyt_open_menu_chat.setVisibility(View.INVISIBLE);
                            if(buddy_texte_qst_lyt.getVisibility() != View.VISIBLE) buddy_texte_resp_lyt.setTranslationY(-155);
                            else buddy_texte_resp_lyt.setTranslationY(0);
                            buddy_texte_resp_lyt.setVisibility(View.VISIBLE);
                            buddy_texte_resp.setMovementMethod( new ScrollingMovementMethod() );
                            buddy_texte_resp.scrollTo( 0, 0 );
                        }

                        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
                        Replica reponse = new Replica();
                        reponse.setValue(texte);
                        reponse.setTime(time);
                        long responseTime = teamChatBuddyApplication.getGetResponseTime()- teamChatBuddyApplication.getQuestionTime();
                        DecimalFormat df = new DecimalFormat("#,###");
                        String formattedTime= df.format(responseTime);
                        reponse.setType("reponse");
                        reponse.setDuration(formattedTime + " ms");
                        DecimalFormat decimalFormatter = new DecimalFormat("0.00");
                        String formattedValue = decimalFormatter.format(Double.parseDouble(teamChatBuddyApplication.getparam("Total_cons")));
                        reponse.setPrix(formattedValue+" $");
                        listRep.add(reponse);
                        ArrayList<Replica> ll = new ArrayList<>();
                        for (int t = 0; t < listRep.size(); t++) {
                            ll.add(listRep.get(t));
                        }
                        Session session = new Session(ll);
                        ArrayList<Session> sessionTemp= teamChatBuddyApplication.getListSession();
                        sessionTemp.add(session);
                        teamChatBuddyApplication.setListSession(sessionTemp);
                        listRep.clear();

                        teamChatBuddyApplication.speakTTS( texte, LabialExpression.SPEAK_NEUTRAL, type );
                    }
                    else if(type.equals("INVITATION")){
                        Log.e("TEAMCHAT_BUDDY_TRACKING"," --- speakTTS from speak Main");
                        if (settingClass.getSwitchVisibility().equals( "true" )) {
                            if (teamChatBuddyApplication.getCurrentLanguage().equals( "en" )) {
                                buddy_texte_resp.setText( String.format( "Response :  %s ", texte ) );
                            } else if (teamChatBuddyApplication.getCurrentLanguage().equals( "fr" )) {
                                buddy_texte_resp.setText( String.format( "Réponse :  %s ", texte ) );
                            } else if (teamChatBuddyApplication.getCurrentLanguage().equals( "de" )) {
                                buddy_texte_resp.setText( String.format( "Antwort :  %s ", texte ) );
                            } else if (teamChatBuddyApplication.getCurrentLanguage().equals( "es" )) {
                                buddy_texte_resp.setText( String.format( "Respuesta :  %s ", texte ) );
                            } else {
                                teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate( "Response" ).addOnSuccessListener( new OnSuccessListener<String>() {
                                    @Override
                                    public void onSuccess(String translatedText) {
                                        buddy_texte_resp.setText( String.format( translatedText + " :  %s ", texte ) );
                                    }

                                } ).addOnFailureListener( new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.e( TAG, "translatedText exception  " + e );
                                    }
                                } );

                            }
                            lyt_open_menu_settings.setVisibility(View.INVISIBLE);
                            lyt_open_menu_chat.setVisibility(View.INVISIBLE);
                            if(buddy_texte_qst_lyt.getVisibility() != View.VISIBLE) buddy_texte_resp_lyt.setTranslationY(-155);
                            else buddy_texte_resp_lyt.setTranslationY(0);
                            buddy_texte_resp_lyt.setVisibility(View.VISIBLE);
                            buddy_texte_resp.setMovementMethod( new ScrollingMovementMethod() );
                            buddy_texte_resp.scrollTo( 0, 0 );
                        }
                        teamChatBuddyApplication.speakTTS(texte, LabialExpression.SPEAK_NEUTRAL,type);
                    }
                    else if (type.equals("STARTMESSAGE")){
                        teamChatBuddyApplication.speakTTS(texte, LabialExpression.SPEAK_NEUTRAL,type);
                    }
                }
            }
        });
    }


    /**
     * ----------------------------------------- Tracking ---------------------------------------
     */

    private IInvitationCallback iInvitationCallback;
    public interface IInvitationCallback {
        void onEnd(String s);
    }

    private enum StateTrackingListening {
        NONE,
        PERSON_IS_VISIBLE_AND_IS_LOOKING_AT_CAMERA_TIMEOUT,
        PERSON_IS_VISIBLE_BUT_IS_NOT_LOOKING_AT_CAMERA_TIMEOUT,
        PERSON_IS_NOT_VISIBLE_TIMEOUT
    }
    private StateTrackingListening currentTrackingListeningState = StateTrackingListening.NONE;

    private enum StateTrackingWelcome {
        NONE,
        PERSON_IS_NOT_VISIBLE_TIMEOUT,
        PERSON_IS_VISIBLE_AND_IS_LOOKING_AT_CAMERA_TIMEOUT
    }
    private StateTrackingWelcome currentTrackingWelcomeState = StateTrackingWelcome.NONE;

    private Handler handlerCheckPersonDetection = new Handler();
    private Runnable runnableCheckPersonDetection = new Runnable() {
        public void run() {

            long currentTime = System.currentTimeMillis();

            if(isPersonDetected){
                personDetectedTimeToCloseApp = currentTime;
                if(!personIsVisible){
                    firstVisibleTime = currentTime;  // Update the time when person started being visible
                }
                personIsVisible =true;
                lastVisibleTime = currentTime;  // Update the time when person was last seen
                lastVisibleTime_saved = currentTime;  // Update the time when person was last seen
                if (regarde_camera) {
                    lastLookingAtCameraTime = currentTime;  // Update the time when the person last looked at the camera
                    lastLookingAtCameraTimeToCloseApp= currentTime;
                    // If starting to look at the camera, set the start time
                    if (startLookingAtCameraTime == 0L) {
                        startLookingAtCameraTime = currentTime;
                        totalTimeLookingAtCamera = 0L; // Reset total time when starting to look at the camera
                    }
                    else {
                        // Calculate the interval time looking at the camera
                        totalTimeLookingAtCamera += currentTime - startLookingAtCameraTime;
                        // Update the start time for the next interval calculation
                        startLookingAtCameraTime = currentTime;
                    }
                }
                else {
                    // Reset the start time if not looking at the camera
                    startLookingAtCameraTime = 0L;
                    totalTimeLookingAtCamera = 0L;
                }
            }
            else{
                if(personIsVisible){
                    visibleDuration = currentTime - firstVisibleTime;
                    Log.e(TAG_TRACKING, "Person was detected for " + visibleDuration + " milliseconds");
                }
                personIsVisible =false;
                totalTimeLookingAtCamera = 0L;
                startLookingAtCameraTime = 0L;
            }


            //#region arrêt et lancement d'écoute
            if(Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Auto_Listen")) && !isSpeaking){
                /**
                 *  Lorsque personne ne regarde la camera pendant (TRACKING_DELAY_STOP_LISTEN secondes) il arrête d’écouter y compris les hotwords.
                 *  Si une personne regarde la caméra pendant (TRACKING_DELAY_START_LISTEN secondes), il se met à l’écoute sans attendre un hotword.
                 */
                if (isPersonDetected) {
                    if (regarde_camera) {
                        Log.w(TAG_TRACKING_DEBUG, "A person is visible again and is looking directly at the CAMERA totalTimeLookingAtCamera= "+totalTimeLookingAtCamera+" currentTrackingListeningState= "+currentTrackingListeningState);
                        useListeningNumberWithAutomaicListening = false;
                        if (totalTimeLookingAtCamera  >= TRACKING_DELAY_START_LISTEN * 1000L) {
                            if (currentTrackingListeningState != StateTrackingListening.PERSON_IS_VISIBLE_AND_IS_LOOKING_AT_CAMERA_TIMEOUT) {
                                currentTrackingListeningState = StateTrackingListening.PERSON_IS_VISIBLE_AND_IS_LOOKING_AT_CAMERA_TIMEOUT;

                                Log.w(TAG_TRACKING_DEBUG, "A person has been looking directly at the camera for TRACKING_DELAY_START_LISTEN="+TRACKING_DELAY_START_LISTEN+" seconds (or more) --> start listening");
                                Log.e(TAG_TRACKING_DEBUG,"isFirstInvitaion = "+isFirstInvitaion+"Tracking_Invitation= "+Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Invitation")));
                                if (!isFirstInvitaion && Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Invitation"))){
                                    startListeningQuestion();
                                }
                                else if (!Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Invitation"))){
                                    startListeningQuestion();
                                }
                                Log.w(TAG_TRACKING, "isFirstInvitaion= "+isFirstInvitaion);
                            }
                        }
                    }
                    else {
                        Log.w(TAG_TRACKING_DEBUG, "A person is visible again BUT is not looking at the CAMERA");
                        if (currentTime - lastLookingAtCameraTime >= TRACKING_DELAY_STOP_LISTEN * 1000L) {
                            if (currentTrackingListeningState != StateTrackingListening.PERSON_IS_VISIBLE_BUT_IS_NOT_LOOKING_AT_CAMERA_TIMEOUT) {
                                currentTrackingListeningState = StateTrackingListening.PERSON_IS_VISIBLE_BUT_IS_NOT_LOOKING_AT_CAMERA_TIMEOUT;
                                Log.w(TAG_TRACKING, "No person has been looking directly at the camera for TRACKING_DELAY_STOP_LISTEN="+TRACKING_DELAY_STOP_LISTEN+" seconds (or more) --> stop listening");
                                if (!useListeningNumberWithAutomaicListening){
                                    stopListeningEverything();
                                }

                            }
                        }
                    }
                }
                else if (currentTime - lastVisibleTime >= TRACKING_DELAY_STOP_LISTEN * 1000L) {
                    Log.w(TAG_TRACKING_DEBUG, "No person is visible");
                    if (currentTrackingListeningState != StateTrackingListening.PERSON_IS_NOT_VISIBLE_TIMEOUT) {
                        currentTrackingListeningState = StateTrackingListening.PERSON_IS_NOT_VISIBLE_TIMEOUT;
                        Log.w(TAG_TRACKING_DEBUG, "No person has been visible for TRACKING_DELAY_STOP_LISTEN="+TRACKING_DELAY_STOP_LISTEN+" seconds (or more) --> stop listening");
                        if (!useListeningNumberWithAutomaicListening){
                            stopListeningEverything();
                        }
                    }
                }
            }
            //#endregion arrêt et lancement d'écoute


            //#region Invitation
            if(Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Invitation"))){
                /**
                 * S’il n’a pas vu de personnes depuis (TRACKING_DELAY_WELCOME minutes)
                 * et qu’il détecte qu'une personne le regarde pendant (TRACKING_DURATION_WELCOME secondes),
                 * alors il prononce une invitation
                 */
                if (!isPersonDetected && currentTime - lastVisibleTime_saved >= TRACKING_DELAY_WELCOME * 60L * 1000L) {
                    if (currentTrackingWelcomeState != StateTrackingWelcome.PERSON_IS_NOT_VISIBLE_TIMEOUT) {
                        currentTrackingWelcomeState = StateTrackingWelcome.PERSON_IS_NOT_VISIBLE_TIMEOUT;
                        Log.w(TAG_TRACKING, "No person has been visible for TRACKING_DELAY_WELCOME="+TRACKING_DELAY_WELCOME+" minutes (or more)");
                        sendInvitationPending = true;
                    }
                }
                if (sendInvitationPending && isPersonDetected && regarde_camera) {
                    if (totalTimeLookingAtCamera >= TRACKING_DURATION_WELCOME * 1000L) {
                        if (currentTrackingWelcomeState != StateTrackingWelcome.PERSON_IS_VISIBLE_AND_IS_LOOKING_AT_CAMERA_TIMEOUT) {
                            currentTrackingWelcomeState = StateTrackingWelcome.PERSON_IS_VISIBLE_AND_IS_LOOKING_AT_CAMERA_TIMEOUT;
                            Log.e(TAG_TRACKING, "A person has been looking directly at the camera for TRACKING_DURATION_WELCOME="+TRACKING_DURATION_WELCOME+" seconds (or more) --> Invitation");
                            sendInvitationPending = false;
                            if(!teamChatBuddyApplication.isAlreadyChatting()){
                                stopListeningFreeSpeech();
                                teamChatBuddyApplication.setStartRecording(false);
                                teamChatBuddyApplication.setSpeaking(false);
                                try {
                                    BuddySDK.UI.stopListenAnimation();
                                } catch (Exception e) {
                                    Log.e(TAG, "BuddySDK Exception  " + e);
                                }
//                                invitation(null);
                                if (isFirstLaunch && isFirstInvitaion) {
                                    isFirstInvitaion = false;
                                }
                                    invitation(new IInvitationCallback() {
                                        @Override
                                        public void onEnd(String s) {
                                            Log.e(TAG_TRACKING, "Invitation onEnd Callback : "+s);
                                            iInvitationCallback = null;
                                            if(Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Auto_Listen"))){
                                                startListeningQuestion();
                                            }
                                            else{
                                                teamChatBuddyApplication.setAlreadyChatting(false);
                                                teamChatBuddyApplication.startListeningHotwor(MainActivity.this);
                                            }

                                        }
                                    });

                            }
                            else{
                                Log.e(TAG_TRACKING, "Do not say invitation because the person is already chatting");
                            }
                        }
                    }
                }
            }
            //#endregion Invitation


            //#region re-tracking, re-centering gaze and head
            if(TRACKING_DELAY_NO_WATCH!=0&&isRecenter){
                Log.i(TAG_TRACKING," +++++++++++++++++++++++++re-tracking++++++++++++++++++++++++++");
                if (isPersonDetected && !regarde_camera && currentTime - lastLookingAtCameraTime >= TRACKING_DELAY_NO_WATCH * 1000L) {
                    Log.w(TAG_TRACKING, "No person has been looking directly at the camera for TRACKING_DELAY_NO_WATCH=" + TRACKING_DELAY_NO_WATCH + " seconds --> re-tracking + re-centering the gaze and head");
                    //re_track_and_center_head();

                    isRecenter=false;
                }
                if (!isPersonDetected && !regarde_camera && currentTime - lastLookingAtCameraTime >= TRACKING_REGARD_CENTER * 1000L) {
                    Log.w(TAG_TRACKING, "No person has been looking directly at the camera for TRACKING_REGARD_CENTER=" + TRACKING_REGARD_CENTER + " seconds --> refocus the pupils");
                    //re_track_and_center_head();
                    isRecenter=false;
                }
            }else{
                Log.i(TAG_TRACKING," +++++++++++++++++++++++pas de re-tracking++++++++++++++++++++++++++");
            }
            //#endregion re-tracking, re-centering gaze and head

            //#region Timer to exit the application
            Log.i("Finiche"," currentTime : "+currentTime+" /lastLookingAtCameraTimeToCloseApp "+lastLookingAtCameraTimeToCloseApp );
            Log.i("Finiche"," currentTime - lastLookingAtCameraTimeToCloseApp : "+(currentTime - lastLookingAtCameraTimeToCloseApp));
            if ( WATCHING_timeout!=0 && !regarde_camera && currentTime - lastLookingAtCameraTimeToCloseApp >= WATCHING_timeout * 1000L){
                Log.i("Finiche","closed app ");
                finishAffinity();
                System.exit(0);
            }
            //#region Timer to exit the application
            Log.i("Finiche"," currentTime : "+currentTime+" /personDetectedTimeToCloseApp "+personDetectedTimeToCloseApp );
            Log.i("Finiche"," currentTime - personDetectedTimeToCloseApp : "+(currentTime - personDetectedTimeToCloseApp));
            if (teamChatBuddyApplication.getparam("TRACKING_timeout_Switch").trim().equalsIgnoreCase("true") && TRACKING_TIMEOUT!=0 && !regarde_camera && currentTime - personDetectedTimeToCloseApp >= TRACKING_TIMEOUT * 1000L){
                Log.i("Finiche","closed app ");
                finishAffinity();
                System.exit(0);
            }

            //#endregion Timer to exit the application

        }
    };

    private void initTracking(){
        Log.d(TAG_TRACKING, "initTracking(isReTrack="+isReTrack+")");

        if(!isFirstLaunch && !isReTrack){
            try{
                if(BuddySDK.Actuators.getLeftWheelStatus().toUpperCase().contains("DISABLE") || BuddySDK.Actuators.getRightWheelStatus().toUpperCase().contains("DISABLE")) {
                    BuddySDK.USB.enableWheels(true, iUsbCommadRspTracking);
                }
                if(BuddySDK.Actuators.getYesStatus().toUpperCase().contains("DISABLE")) {
                    BuddySDK.USB.enableYesMove(true, iUsbCommadRspTracking);
                }
                if(BuddySDK.Actuators.getNoStatus().toUpperCase().contains("DISABLE")) {
                    BuddySDK.USB.enableNoMove(true, iUsbCommadRspTracking);
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }

        if(!isReTrack){
            //récupération des paramètres TRACKING du fichier de config:
            TRACKING_WATCH = teamChatBuddyApplication.getParamFromFile("TRACKING_watch", "TeamChatBuddy.properties");
            TRACKING_DELAY_NO_WATCH = Integer.parseInt(teamChatBuddyApplication.getParamFromFile("TRACKING_delay_nowatching", "TeamChatBuddy.properties"));
            TRACKING_DELAY_START_LISTEN = Integer.parseInt(teamChatBuddyApplication.getParamFromFile("TRACKING_delay_startlisten", "TeamChatBuddy.properties"));
            TRACKING_DELAY_STOP_LISTEN = Integer.parseInt(teamChatBuddyApplication.getParamFromFile("TRACKING_delay_stoplisten", "TeamChatBuddy.properties"));
            TRACKING_REGARD_CENTER = Integer.parseInt(teamChatBuddyApplication.getParamFromFile("TRACKING_regard_center", "TeamChatBuddy.properties"));
            TRACKING_DELAY_WELCOME = Integer.parseInt(teamChatBuddyApplication.getParamFromFile("WELCOME_delay", "TeamChatBuddy.properties"));
            TRACKING_DURATION_WELCOME = Integer.parseInt(teamChatBuddyApplication.getParamFromFile("WELCOME_duration_tracking", "TeamChatBuddy.properties"));
            TRACKING_WELCOME_FR = teamChatBuddyApplication.getParamFromFile("WELCOME_messages_FR", "TeamChatBuddy.properties");
            TRACKING_WELCOME_EN =teamChatBuddyApplication.getParamFromFile("WELCOME_messages_EN", "TeamChatBuddy.properties");
            TRACKING_WELCOME_MODEL = teamChatBuddyApplication.getParamFromFile("WELCOME_model", "TeamChatBuddy.properties");
            TRACKING_WELCOME_TEMPERATURE = Double.parseDouble(teamChatBuddyApplication.getParamFromFile("WELCOME_temperature", "TeamChatBuddy.properties"));
            TRACKING_WELCOME_PROMPT_FR = teamChatBuddyApplication.getParamFromFile("WELCOME_prompt_FR", "TeamChatBuddy.properties");
            TRACKING_WELCOME_PROMPT_EN = teamChatBuddyApplication.getParamFromFile("WELCOME_prompt_EN", "TeamChatBuddy.properties");
            TRACKING_WELCOME_MAX_TOKEN = Integer.parseInt(teamChatBuddyApplication.getParamFromFile("WELCOME_maxtoken", "TeamChatBuddy.properties"));
            try {
                TRACKING_TIMEOUT=Integer.parseInt(teamChatBuddyApplication.getparam("trackingTimeout"));
            }
            catch (Exception e){
                TRACKING_TIMEOUT=0;
            }
            try {
                WATCHING_timeout=Integer.parseInt(teamChatBuddyApplication.getParamFromFile("WATCHING_timeout","TeamChatBuddy.properties"));
            }
            catch (Exception e){
                WATCHING_timeout=0;
            }
        }

        if(isFirstLaunch && !isReTrack && Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Invitation"))){
            sendInvitationPending = true;
            isFirstInvitaion = true;
            startTracking();
        }
        else {
            startTracking();
        }
    }
    private void stopTrackingTest() {
        // Arrêter l'analyse d'image
        if (imageAnalyzer != null) {
            imageAnalyzer.clearAnalyzer();
            imageAnalyzer = null;
        }

        // Arrêter et libérer les ressources de la caméra
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            cameraProvider = null;
        }
        if (preview != null) {
            preview.setSurfaceProvider(null);
            preview = null;
        }

        // Stop and release resources from the PoseLandmarkerHelper
        if (poseLandmarkerHelper != null) {
            poseLandmarkerHelper.clearPoseLandmarker();
            poseLandmarkerHelper = null;
        }
        // Arrêter l'exécuteur de thread
        if (backgroundExecutor != null && !backgroundExecutor.isShutdown()) {
            backgroundExecutor.shutdownNow();
            backgroundExecutor = null;
        }
    }

    private void startTracking(){
        Log.d(TAG_TRACKING, "startTracking(isReTrack="+isReTrack+")");

        stopTrackingTest();
        if (poseTracking != null) {
            poseTracking.stopMovingAndCancelRunnables();
            poseTracking = null;
        }
        poseTracking= new PoseTracking();
        if (backgroundExecutor != null) {
            backgroundExecutor.shutdownNow();
            backgroundExecutor = null;
        }
        backgroundExecutor = Executors.newSingleThreadExecutor();
        cameraSelector = new CameraSelector.Builder().requireLensFacing(cameraFacing).build();
        viewModel = new ViewModelProvider(MainActivity.this).get(MainViewModel.class);
        viewModel.setMinPoseDetectionConfidence(PoseLandmarkerHelper.DEFAULT_POSE_DETECTION_CONFIDENCE);
        viewModel.setMinPoseTrackingConfidence(PoseLandmarkerHelper.DEFAULT_POSE_TRACKING_CONFIDENCE);
        viewModel.setMinPosePresenceConfidence(PoseLandmarkerHelper.DEFAULT_POSE_PRESENCE_CONFIDENCE);
        viewModel.setDelegate(PoseLandmarkerHelper.DELEGATE_GPU);
        viewModel.set_model(PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_FULL);

        setUpCamera();
    }

    private void setUpCamera() {
        Log.e(TAG_TRACKING_DEBUG,"setUpCamera(isReTrack="+isReTrack+")");
        isTrackingAlreadyInitialised = false;
        previewView.post(() -> {
            try {
                cameraProvider = ProcessCameraProvider.getInstance(this).get();
                if (preview != null) {
                    preview.setSurfaceProvider(null);
                    preview = null;
                }

                if (imageAnalyzer != null) {
                    imageAnalyzer.clearAnalyzer();
                    imageAnalyzer = null;
                }
                preview = new Preview.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .build();
                imageAnalyzer = new ImageAnalysis.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build();
                imageAnalyzer.setAnalyzer(backgroundExecutor, this::detectPose);
                cameraProvider.unbindAll();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer);
                Log.i(TAG, "Camera bound successfully");
            } catch (Exception e) {
                Log.e(TAG, "Camera binding failed", e);
            }
        });
        backgroundExecutor.execute(() -> {
            Context context = this;
            poseLandmarkerHelper = new PoseLandmarkerHelper(
                    context,
                    RunningMode.LIVE_STREAM,
                    PoseLandmarkerHelper.DEFAULT_POSE_DETECTION_CONFIDENCE,
                    PoseLandmarkerHelper.DEFAULT_POSE_TRACKING_CONFIDENCE,
                    PoseLandmarkerHelper.DEFAULT_POSE_PRESENCE_CONFIDENCE,
                    PoseLandmarkerHelper.DELEGATE_CPU,
                    PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_FULL,
                    new PoseLandmarkerHelper.LandmarkerListener() {
                        @Override
                        public void onError(String error, int errorCode) {}
                        @Override
                        public void onResults(PoseLandmarkerHelper.ResultBundle resultBundle) {

                            if(!isTrackingAlreadyInitialised){
                                isTrackingAlreadyInitialised = true;
                                //initialisations
                                lastVisibleTime = System.currentTimeMillis();
                                if(!isReTrack){
                                    lastVisibleTime_saved = System.currentTimeMillis(); //do not reset it when re-track (useful for invitation check)
                                    lastLookingAtCameraTimeToCloseApp = System.currentTimeMillis();
                                    personDetectedTimeToCloseApp = System.currentTimeMillis();
                                }
                                firstVisibleTime = System.currentTimeMillis();
                                lastLookingAtCameraTime = System.currentTimeMillis();
                                visibleDuration = 0;
                                isPersonDetected = false;
                                personIsVisible = false;
                                isProcessingReTrack = false;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Camera_Display"))) {
                                            reGroup.setTranslationY(0);

                                        } else {
                                            reGroup.setTranslationY(1000);
                                        }
                                    }
                                });
                            }

                            if(!isProcessingReTrack){

                                if(dLeft < initLang){
                                    regarde_camera =  direction && deFace && directionRegardNez.equals("CAMERA");
                                }
                                else {
                                    regarde_camera =   deFace && directionRegardNez.equals("CAMERA");
                                }

                                PoseLandmarkerResult poseLandmarkerResult = resultBundle.results.get(0);
                                isPersonDetected = PoseLandmarkerHelper.extractLandmarks(poseLandmarkerResult);

                                //Log.i(TAG_TRACKING_DEBUG, "regarde_camera : "+regarde_camera);
                                //Log.i(TAG_TRACKING_DEBUG, "direction : "+direction);
                                //Log.i(TAG_TRACKING_DEBUG, "deFace : "+deFace);
                                //Log.i(TAG_TRACKING_DEBUG, "directionRegardNez : "+directionRegardNez);
                                //Log.i(TAG_TRACKING_DEBUG, "isPersonDetected : "+ isPersonDetected);

                                res = poseTracking.suivi(poseLandmarkerResult);
                                eog = res[0];
                                Eod = res[1];
                                degx = res[2];
                                degy = res[3];
                                x0 = res[4];
                                x2 = res[5];
                                x5 = res[6];
                                y0 = res[7];
                                y2 = res[8];
                                y5 = res[9];
                                lang = res[12];
                                dLeft = res[10];
                                dRight = res[11];

                                if (isPersonDetected != wasPersonDetected) {
                                    if (!isPersonDetected) {
                                        poseTracking.stopMovingAndCancelRunnables();
                                    }
                                    // Update the previous state
                                    wasPersonDetected = isPersonDetected;
                                }

                                if(isPersonDetected){
                                    poseTracking.look_at(degx, degy);

                                    if (Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Body")) || Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Head"))) {
                                        if (TRACKING_WATCH.trim().equalsIgnoreCase("Yes")) {
                                            if (regarde_camera) {
                                                isRecenter=true;
                                                poseTracking.Rotation(degx, Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Body")));
                                            }else{
                                                if(Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Head"))) {
                                                    BuddySDK.USB.buddyStopNoMove(iUsbCommadRspTracking);
                                                }if(isRecenter) {
                                                    BuddySDK.USB.buddyStopNoMove(iUsbCommadRspTracking);
                                                }else {
                                                    int No_position = BuddySDK.Actuators.getNoPosition();
                                                    if(No_position!=0){
                                                        BuddySDK.USB.buddySayNo(100,0,iUsbCommadRspBI);

                                                    }else {
                                                        BuddySDK.USB.buddyStopNoMove(iUsbCommadRspTracking);
                                                    }
                                                }
                                                if(Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Body"))){
                                                    BuddySDK.USB.emergencyStopMotors(iUsbCommadRspTracking);
                                                }

                                            }
                                        }
                                        else {
                                            if(Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Head"))) {
                                                if(isRecenter){
                                                    poseTracking.Rotation(degx, Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Body")));
                                                }
                                                if (regarde_camera) {
                                                    isRecenter = true;
                                                } else {
                                                    if (!isRecenter) {
                                                        int No_position = BuddySDK.Actuators.getNoPosition();
                                                        if (No_position != 0) {
                                                            BuddySDK.USB.buddySayNo(100, 0, iUsbCommadRspBI);

                                                        } else {
                                                            BuddySDK.USB.buddyStopNoMove(iUsbCommadRspTracking);

                                                        }
                                                    }
                                                }
                                            }else{
                                                poseTracking.Rotation(degx, Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Body")));
                                            }
                                        }
                                    }

                                    if (Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Head"))) {
                                        poseTracking.yesTracking(degy);
//                                        if (TRACKING_WATCH.trim().equalsIgnoreCase("Yes")) {
//                                            if (regarde_camera) {
//                                                isRecenter=true;
//                                                poseTracking.yesTracking(degy);
//                                            }else{
//                                                if(isRecenter) {
//                                                    BuddySDK.USB.buddyStopYesMove(iUsbCommadRspTracking);
//                                                }else {
//                                                    int No_position = BuddySDK.Actuators.getYesPosition();
//                                                    if(No_position!=6){
//                                                        BuddySDK.USB.buddySayYes(100,6,iUsbCommadRspBI);
//
//                                                    }else {
//                                                        BuddySDK.USB.buddyStopYesMove(iUsbCommadRspTracking);
//                                                    }
//                                                }
//                                            }
//                                        } else {
//                                            poseTracking.yesTracking(degy);
//                                            if (regarde_camera) {
//                                                isRecenter = true;
//                                            } else{
//                                                if(isRecenter) {
//                                                    BuddySDK.USB.buddyStopYesMove(iUsbCommadRspTracking);
//                                                }else {
//                                                    int No_position = BuddySDK.Actuators.getYesPosition();
//                                                    if(No_position!=6){
//                                                        BuddySDK.USB.buddySayYes(100,6,iUsbCommadRspBI);
//
//                                                    }else {
//                                                        BuddySDK.USB.buddyStopYesMove(iUsbCommadRspTracking);
//                                                    }
//                                                }
//                                            }
//                                        }
                                    }
                                }

                                directionRegardNez = poseTracking.directionVisage(x2, y2, x5, y5, x0, y0, lang);

                                if ((x2 - x5) > 0 ) {
                                    deFace = true;
                                }
                                else {
                                    deFace = false;
                                }
                                if (eog > Eod * 2) {
                                    direction = false;
                                }
                                else {
                                    if (eog * 1.5 < Eod) {
                                        direction = false;
                                    } else {
                                        direction = true;
                                    }
                                }

                                runOnUiThread(() -> {
                                    if (overlay != null) {
                                        overlay.setResults(resultBundle.results.get(0), resultBundle.inputImageHeight, resultBundle.inputImageWidth, RunningMode.LIVE_STREAM);
                                        overlay.setNbrLandmarks(poseTracking.getLandmarksCamera(poseLandmarkerResult));
                                    }
                                });

                                handlerCheckPersonDetection.removeCallbacks(runnableCheckPersonDetection);
                                handlerCheckPersonDetection.removeCallbacksAndMessages(null);
                                handlerCheckPersonDetection.post(runnableCheckPersonDetection);
                            }
                            else{
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Camera_Display"))) {
                                            reGroup.setTranslationY(0);

                                        } else {
                                            reGroup.setTranslationY(1000);
                                        }
                                    }
                                });
                            }
                        }
                    }
            );
        });
    }
    private void detectPose(ImageProxy imageProxy) {
        poseLandmarkerHelper.detectLiveStream(imageProxy);
    }

    private void re_track_and_center_head(){
        //Log.d(TAG_TRACKING, "re_track_and_center_head");
        //isProcessingReTrack = true;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //stopTracking();
                Log.d(TAG_TRACKING, "re_track_and_center_eys");
                poseTracking.lookAtCenter();
                if(Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Head"))){
                    Log.d(TAG_TRACKING, "re_track_and_center_head");
                    poseTracking.centerHead();
                }

            }
        });
    }

    private void stopTracking(){
        reGroup.setTranslationY(1000);
        cameraProvider.unbindAll();
        handlerCheckPersonDetection.removeCallbacks(runnableCheckPersonDetection);
        handlerCheckPersonDetection.removeCallbacksAndMessages(null);
        poseTracking.stopMovingAndCancelRunnables();
    }

    private void startListeningQuestion(){
        Log.d(TAG_TRACKING, "startListeningQuestion()");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    isSpeaking =false;
                    if(iInvitationCallback != null) iInvitationCallback.onEnd("INVITATION_END");
                    if(handler!=null && runnable!=null){
                        handler.removeCallbacks(runnable);
                        handler.removeCallbacksAndMessages(null);
                    }
                    if (responseTimeout!=null) responseTimeout.cancel();
                    if(teamChatBuddyApplication.getChatGptStreamMode() != null){
                        teamChatBuddyApplication.getChatGptStreamMode().reset();
                    }
                    if(teamChatBuddyApplication.getCustomGPTStreamMode() != null){
                        teamChatBuddyApplication.getCustomGPTStreamMode().reset();
                    }
                    if(handlerTTSError!=null && runnableTTSError!=null){
                        handlerTTSError.removeCallbacks(runnableTTSError);
                        handlerTTSError.removeCallbacksAndMessages(null);
                    }
                    Log.d(TAG_TRACKING, "startListeningQuestion() if first");
                    if (!teamChatBuddyApplication.getSpeaking() && !mlKitIsDownloading){
                        teamChatBuddyApplication.setStartRecording(true);
                        teamChatBuddyApplication.setSpeaking(true);
                        if(!isListeningFreeSpeech ) {
                            Log.d(TAG_TRACKING, "startListeningQuestion() if second");
                            isListeningFreeSpeech=true;
                            teamChatBuddyApplication.setActivityClosed(false);
                            startListeningFreeSpeech(teamChatBuddyApplication.getListeningDuration());
                        }
                        Log.d(TAG_TRACKING, "startListeningQuestion() isListeningFreeSpeech="+isListeningFreeSpeech);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void stopListeningEverything(){
        Log.e(TAG_TRACKING, "stopListeningEverything()");
        runOnUiThread( new Runnable() {
            @Override
            public void run() {
                try{
                    isSpeaking =false;
                    if(iInvitationCallback != null) iInvitationCallback.onEnd("INVITATION_END");
                    if(handler!=null && runnable!=null){
                        handler.removeCallbacks(runnable);
                        handler.removeCallbacksAndMessages(null);
                    }
                    if (responseTimeout!=null) responseTimeout.cancel();
                    if(teamChatBuddyApplication.getChatGptStreamMode() != null){
                        teamChatBuddyApplication.getChatGptStreamMode().reset();
                    }
                    if(teamChatBuddyApplication.getCustomGPTStreamMode() != null){
                        teamChatBuddyApplication.getCustomGPTStreamMode().reset();
                    }
                    if(handlerTTSError!=null && runnableTTSError!=null){
                        handlerTTSError.removeCallbacks(runnableTTSError);
                        handlerTTSError.removeCallbacksAndMessages(null);
                    }
                    if (teamChatBuddyApplication.getSpeaking() && !mlKitIsDownloading) {
                        if (teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Android") || teamChatBuddyApplication.getparam("STT_chosen").trim().equalsIgnoreCase("Cerence") || !teamChatBuddyApplication.getAppIsListeningToTheQuestion() || teamChatBuddyApplication.getParamFromFile("Processing_the_audio_sequence","TeamChatBuddy.properties").trim().equalsIgnoreCase("No")) {
                            BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                            teamChatBuddyApplication.setStartRecording(false);
                            teamChatBuddyApplication.setSpeaking(false);
                            teamChatBuddyApplication.setActivityClosed(true);
                            isListeningFreeSpeech = false;
                            teamChatBuddyApplication.stopTTS();
                            teamChatBuddyApplication.setStoredResponse("");
                            if (buddy_texte_qst_lyt != null && buddy_texte_resp_lyt != null && buddy_texte_qst != null && buddy_texte_resp != null) {
                                buddy_texte_qst_lyt.setVisibility(View.INVISIBLE);
                                buddy_texte_resp_lyt.setVisibility(View.INVISIBLE);
                                buddy_texte_qst.setMovementMethod(null);
                                buddy_texte_resp.setMovementMethod(null);
                            }
                            if (teamChatBuddyApplication.getParamFromFile("Options_Access","TeamChatBuddy.properties")!=null && teamChatBuddyApplication.getParamFromFile("Options_Access","TeamChatBuddy.properties").trim().equalsIgnoreCase("Yes")){
                                if (teamChatBuddyApplication.getParamFromFile("Number_clicks_options","TeamChatBuddy.properties")!=null &&
                                        Integer.parseInt(teamChatBuddyApplication.getParamFromFile("Number_clicks_options","TeamChatBuddy.properties"))!=0){
                                    lyt_open_menu_settings.setVisibility(View.VISIBLE);
                                }
                                else if (teamChatBuddyApplication.getParamFromFile("Number_clicks_options","TeamChatBuddy.properties")==null){
                                    lyt_open_menu_settings.setVisibility(View.VISIBLE);
                                }
                            }
                            lyt_open_menu_chat.setVisibility(View.VISIBLE);
                            try {
                                BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                            } catch (Exception e) {
                                Log.e(TAG, "BuddySDK Exception  " + e);
                            }
                            teamChatBuddyApplication.setAppIsListeningToTheQuestion(false);
                            stopListeningFreeSpeech();
                            try {
                                BuddySDK.UI.stopListenAnimation();
                            } catch (Exception e) {
                                Log.e(TAG, "BuddySDK Exception  " + e);
                            }
                            teamChatBuddyApplication.notifyObservers("end of timer");
                        }
                        else{
                            teamChatBuddyApplication.setLed("Neutral");
                            BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                            try {
                                BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL, 1);
                                BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                                BuddySDK.UI.stopListenAnimation();
                            } catch (Exception e) {
                                Log.e(TAG, "BuddySDK Exception  " + e);
                            }
                            BuddySDK.UI.stopListenAnimation();
                            teamChatBuddyApplication.setAppIsListeningToTheQuestion(false);
                            teamChatBuddyApplication.traitementAudio(false);
                        }
                    }
                }
                catch (Exception e){
                    Log.e(TAG,"Exception  "+e);
                    e.printStackTrace();
                }
            }
        } );
    }

    private void invitation(IInvitationCallback iInvitationCallback){
        Log.e(TAG_TRACKING, "invitation()");
        this.iInvitationCallback = iInvitationCallback;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(Boolean.parseBoolean(teamChatBuddyApplication.getparam("Tracking_Invitation_ChatGpt"))){
                    //Get invitation from ChatGPT
                    if (teamChatBuddyApplication.getLangue().getNom().equals("Anglais")) {
                        if(TRACKING_WELCOME_PROMPT_EN != null && !TRACKING_WELCOME_PROMPT_EN.isEmpty()){
                            responseFromChatbot.getInvitationFromChatGPT(TRACKING_WELCOME_MODEL,TRACKING_WELCOME_TEMPERATURE,TRACKING_WELCOME_MAX_TOKEN,TRACKING_WELCOME_PROMPT_EN);
                        }
                        else {
                            if(iInvitationCallback != null) iInvitationCallback.onEnd("ConfigFile do not contain English ChatGPT Invitation prompt");
                        }
                    }
                    else if (teamChatBuddyApplication.getLangue().getNom().equals("Français")) {
                        if(TRACKING_WELCOME_PROMPT_FR != null && !TRACKING_WELCOME_PROMPT_FR.isEmpty()){
                            responseFromChatbot.getInvitationFromChatGPT(TRACKING_WELCOME_MODEL,TRACKING_WELCOME_TEMPERATURE,TRACKING_WELCOME_MAX_TOKEN,TRACKING_WELCOME_PROMPT_FR);
                        }
                        else {
                            if(iInvitationCallback != null) iInvitationCallback.onEnd("ConfigFile do not contain French ChatGPT Invitation prompt");
                        }
                    }
                    else {
                        if(TRACKING_WELCOME_PROMPT_EN != null && !TRACKING_WELCOME_PROMPT_EN.isEmpty()){
                            teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(TRACKING_WELCOME_PROMPT_EN)
                                    .addOnSuccessListener(new OnSuccessListener<String>() {
                                        @Override
                                        public void onSuccess(String translatedText) {
                                            responseFromChatbot.getInvitationFromChatGPT(TRACKING_WELCOME_MODEL,TRACKING_WELCOME_TEMPERATURE,TRACKING_WELCOME_MAX_TOKEN,translatedText);
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            responseFromChatbot.getInvitationFromChatGPT(TRACKING_WELCOME_MODEL,TRACKING_WELCOME_TEMPERATURE,TRACKING_WELCOME_MAX_TOKEN,TRACKING_WELCOME_PROMPT_EN);
                                        }
                                    });
                        }
                        else {
                            if(iInvitationCallback != null) iInvitationCallback.onEnd("ConfigFile do not contain English ChatGPT Invitation prompt");
                        }
                    }
                }
                else{
                    //Get invitation from config File
                    if (teamChatBuddyApplication.getLangue().getNom().equals("Anglais")) {
                        if(TRACKING_WELCOME_EN != null && !TRACKING_WELCOME_EN.isEmpty()){
                            String[] englishInvitations = TRACKING_WELCOME_EN.substring(1, TRACKING_WELCOME_EN.length() - 1).split("/");
                            String randomInvitationEN = englishInvitations[random.nextInt(englishInvitations.length)];
                            Log.d(TAG_TRACKING, "Random English Invitation: " + randomInvitationEN);
                            teamChatBuddyApplication.setActivityClosed(false);
                            speak(randomInvitationEN, "INVITATION");
                        }
                        else {
                            if(iInvitationCallback != null) iInvitationCallback.onEnd("ConfigFile do not contain English Invitation");
                        }
                    }
                    else if (teamChatBuddyApplication.getLangue().getNom().equals("Français")) {
                        if(TRACKING_WELCOME_FR != null && !TRACKING_WELCOME_FR.isEmpty()){
                            String[] frenchInvitations = TRACKING_WELCOME_FR.substring(1, TRACKING_WELCOME_FR.length() - 1).split("/");
                            String randomInvitationFR = frenchInvitations[random.nextInt(frenchInvitations.length)];
                            Log.d(TAG_TRACKING, "Random French Invitation: " + randomInvitationFR);
                            teamChatBuddyApplication.setActivityClosed(false);
                            speak(randomInvitationFR, "INVITATION");
                        }
                        else {
                            if(iInvitationCallback != null) iInvitationCallback.onEnd("ConfigFile do not contain French Invitation");
                        }
                    }
                    else {
                        if(TRACKING_WELCOME_EN != null && !TRACKING_WELCOME_EN.isEmpty()){
                            String[] englishInvitations = TRACKING_WELCOME_EN.substring(1, TRACKING_WELCOME_EN.length() - 1).split("/");
                            String randomInvitationEN = englishInvitations[random.nextInt(englishInvitations.length)];
                            teamChatBuddyApplication.getEnglishLanguageSelectedTranslator().translate(randomInvitationEN)
                                    .addOnSuccessListener(new OnSuccessListener<String>() {
                                        @Override
                                        public void onSuccess(String translatedText) {
                                            Log.d(TAG_TRACKING, "Translated Invitation: " + translatedText);
                                            teamChatBuddyApplication.setActivityClosed(false);
                                            speak(translatedText, "INVITATION");
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.d(TAG_TRACKING, "Translation failed, using English Invitation: " + randomInvitationEN);
                                            teamChatBuddyApplication.setActivityClosed(false);
                                            speak(randomInvitationEN, "INVITATION");
                                        }
                                    });
                        }
                        else {
                            if(iInvitationCallback != null) iInvitationCallback.onEnd("ConfigFile do not contain English Invitation");
                        }
                    }
                }
            }
        });
    }


    /**
     *   -------------------------------  Gestion des permissions  ---------------------------------------------------------------
     */

    private boolean checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, requestCode);
            return false;
        }
        return true;
    }

    private boolean checkPermission(@NonNull int[] grantResults){
        return grantResults[0] != PackageManager.PERMISSION_GRANTED ||
                grantResults[1] != PackageManager.PERMISSION_GRANTED ||
                grantResults[2] != PackageManager.PERMISSION_GRANTED ||
                grantResults[3] != PackageManager.PERMISSION_GRANTED;

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_ID && checkPermission(grantResults)) {
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Need permissions " + Manifest.permission.READ_EXTERNAL_STORAGE +
                                    "/" + Manifest.permission.WRITE_EXTERNAL_STORAGE +
                                    "/" + Manifest.permission.RECORD_AUDIO +
                                    "/" + Manifest.permission.CAMERA
                            , Toast.LENGTH_LONG).show();
                }
            });
            /*
             * Terminer l'activité si l'utilisateur n'a pas activé une autorisation
             */
            finish();
            return;
        }
        isFirstLaunch = true;
        init();
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

}