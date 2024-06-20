package com.robotique.aevaweb.teamchatbuddy.activities;

import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bfr.buddysdk.BuddyCompatActivity;
import com.robotique.aevaweb.teamchatbuddy.R;
import com.robotique.aevaweb.teamchatbuddy.adapters.DiscoveredBlueMicAdapter;
import com.robotique.aevaweb.teamchatbuddy.application.TeamChatBuddyApplication;
import com.robotique.aevaweb.teamchatbuddy.models.BlueMicDevice;
import com.robotique.aevaweb.teamchatbuddy.observers.IDBObserver;
import com.robotique.aevaweb.teamchatbuddy.utilis.BlueMic;

import java.io.IOException;

public class TeamChatBlueMicActivity extends BuddyCompatActivity implements IDBObserver {

    private static final String TAG = "TeamChatBlueMicAct";
    private static final String TAG_LIFE_CYCLE_DEBUG = "TeamChatBlueMicAct_LIFE_CYCLE";

    private TeamChatBuddyApplication teamChatBuddyApplication;
    private View decorView;

    private DiscoveredBlueMicAdapter discoveredBlueMicAdapter;

    private final int TIMEOUT_SCAN = 30000;

    //views
    private RelativeLayout start_scan_btn_lyt;
    private RelativeLayout stop_scan_btn_lyt;
    private RelativeLayout connect_btn_lyt;
    private RelativeLayout disconnect_btn_lyt;
    private TextView selected_blue_mic_text;
    private TextView layout_blue_mic_title;
    private ListView list_blue_mic_discovered;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG_LIFE_CYCLE_DEBUG,"onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_chat_blue_mic);

        teamChatBuddyApplication = (TeamChatBuddyApplication) getApplicationContext();

        //Gestion d'affichage des barres du systemUI
        teamChatBuddyApplication.hideSystemUI(this);
        decorView=getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if(visibility==0){
                    decorView.setSystemUiVisibility(teamChatBuddyApplication.hideSystemUI(TeamChatBlueMicActivity.this));
                }
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }


        //init views

        start_scan_btn_lyt = findViewById(R.id.start_scan_btn_lyt);
        stop_scan_btn_lyt = findViewById(R.id.stop_scan_btn_lyt);
        connect_btn_lyt = findViewById(R.id.connect_btn_lyt);
        disconnect_btn_lyt = findViewById(R.id.disconnect_btn_lyt);
        selected_blue_mic_text = findViewById(R.id.selected_blue_mic_text);
        list_blue_mic_discovered = findViewById(R.id.list_blue_mic_discovered);
        layout_blue_mic_title = findViewById(R.id.layout_blue_mic_title);

        //initialisations

        if(teamChatBuddyApplication.getBlueMic() == null || teamChatBuddyApplication.getBlueMic().getmBlueMicService() == null){
            teamChatBuddyApplication.setBlueMic(new BlueMic(teamChatBuddyApplication));
            boolean result = teamChatBuddyApplication.getBlueMic().bindBlueMicService();
            Log.i(TAG,"bindBlueMicService() result : "+ result);
            setViews("init");
        }
        else{
            setViews("currentState");
        }

        teamChatBuddyApplication.registerObserver(this);
        discoveredBlueMicAdapter = new DiscoveredBlueMicAdapter(getApplicationContext(),
                R.layout.bluemic_item_layout_resource,
                teamChatBuddyApplication.getBlueMic().blueMicDeviceList);
        list_blue_mic_discovered.setAdapter(discoveredBlueMicAdapter);

        list_blue_mic_discovered.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if(!selected_blue_mic_text.getText().toString().isEmpty()
                        && (
                        selected_blue_mic_text.getText().toString().contains("Connected")
                                || selected_blue_mic_text.getText().toString().contains("Connecting")
                )
                ){
                    disconnect(disconnect_btn_lyt);
                }

                teamChatBuddyApplication.getBlueMic().selectedBlueMic = (BlueMicDevice) adapterView.getItemAtPosition(position);
                selected_blue_mic_text.setText(
                        String.format("%s ; %s ; %s",
                                teamChatBuddyApplication.getBlueMic().selectedBlueMic.getName(),
                                teamChatBuddyApplication.getBlueMic().selectedBlueMic.getTag(),
                                teamChatBuddyApplication.getBlueMic().selectedBlueMic.getState()
                        )
                );
                if(teamChatBuddyApplication.getBlueMic().selectedBlueMic.getState().equals("Idle")){
                    setViews("onIdleNodeDiscovered");
                }
            }
        });

    }

    @Override
    protected void onPause() {
        Log.i(TAG_LIFE_CYCLE_DEBUG,"onPause()");
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.i(TAG_LIFE_CYCLE_DEBUG,"onResume()");
        super.onResume();
        teamChatBuddyApplication.hideSystemUI(this);
    }

    @Override
    protected void onStart() {
        Log.i(TAG_LIFE_CYCLE_DEBUG,"onStart()");
        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.i(TAG_LIFE_CYCLE_DEBUG,"onStop()");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG_LIFE_CYCLE_DEBUG,"onDestroy()");
        super.onDestroy();
        stopScan(stop_scan_btn_lyt);
        teamChatBuddyApplication.removeObserver(this);
    }



    /**
     *   -------------------------------  Fonctions utiles  ----------------------------------------------
     */

    public void closeBlueMicPage(View view) {
        finish();
        overridePendingTransition(0, 0);
    }

    public void startScan(View view) {
        Log.i(TAG,"startScan()");
        if(teamChatBuddyApplication.getBlueMic().getmBlueMicService() != null){
            try {
                teamChatBuddyApplication.getBlueMic().getmBlueMicService().startScan(TIMEOUT_SCAN, teamChatBuddyApplication.getBlueMic().iBlueMicScanListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        else{
            switch (teamChatBuddyApplication.getLangue().getNom()) {
                case "Français":
                    Toast.makeText(teamChatBuddyApplication, teamChatBuddyApplication.getString(R.string.disconnected_service_bluemic_fr), Toast.LENGTH_LONG).show();
                    break;
                case "Espagnol":
                    Toast.makeText(teamChatBuddyApplication, teamChatBuddyApplication.getString(R.string.disconnected_service_bluemic_es), Toast.LENGTH_LONG).show();
                    break;
                case "Allemand":
                    Toast.makeText(teamChatBuddyApplication, teamChatBuddyApplication.getString(R.string.disconnected_service_bluemic_de), Toast.LENGTH_LONG).show();
                    break;
                default:
                    Toast.makeText(teamChatBuddyApplication, teamChatBuddyApplication.getString(R.string.disconnected_service_bluemic_en), Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    public void stopScan(View view) {
        Log.i(TAG,"stopScan()");
        if(teamChatBuddyApplication.getBlueMic().getmBlueMicService() != null){
            try {
                teamChatBuddyApplication.getBlueMic().getmBlueMicService().stopScan();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        else{
            switch (teamChatBuddyApplication.getLangue().getNom()) {
                case "Français":
                    Toast.makeText(teamChatBuddyApplication, teamChatBuddyApplication.getString(R.string.disconnected_service_bluemic_fr), Toast.LENGTH_LONG).show();
                    break;
                case "Espagnol":
                    Toast.makeText(teamChatBuddyApplication, teamChatBuddyApplication.getString(R.string.disconnected_service_bluemic_es), Toast.LENGTH_LONG).show();
                    break;
                case "Allemand":
                    Toast.makeText(teamChatBuddyApplication, teamChatBuddyApplication.getString(R.string.disconnected_service_bluemic_de), Toast.LENGTH_LONG).show();
                    break;
                default:
                    Toast.makeText(teamChatBuddyApplication, teamChatBuddyApplication.getString(R.string.disconnected_service_bluemic_en), Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    public void connect(View view) {
        Log.i(TAG,"connect()");
        if(teamChatBuddyApplication.getBlueMic().getmBlueMicService() != null){
            setViews("connect");
            try {
                teamChatBuddyApplication.getBlueMic().getmBlueMicService().connect(
                        teamChatBuddyApplication.getBlueMic().selectedBlueMic,
                        teamChatBuddyApplication.getBlueMic().iBlueMicConnexionListener
                );
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        else{
            switch (teamChatBuddyApplication.getLangue().getNom()) {
                case "Français":
                    Toast.makeText(teamChatBuddyApplication, teamChatBuddyApplication.getString(R.string.disconnected_service_bluemic_fr), Toast.LENGTH_LONG).show();
                    break;
                case "Espagnol":
                    Toast.makeText(teamChatBuddyApplication, teamChatBuddyApplication.getString(R.string.disconnected_service_bluemic_es), Toast.LENGTH_LONG).show();
                    break;
                case "Allemand":
                    Toast.makeText(teamChatBuddyApplication, teamChatBuddyApplication.getString(R.string.disconnected_service_bluemic_de), Toast.LENGTH_LONG).show();
                    break;
                default:
                    Toast.makeText(teamChatBuddyApplication, teamChatBuddyApplication.getString(R.string.disconnected_service_bluemic_en), Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    public void disconnect(View view) {
        Log.i(TAG,"disconnect()");
        if(teamChatBuddyApplication.getBlueMic().getmBlueMicService() != null){
            setViews("disconnect");
            try {
                teamChatBuddyApplication.getBlueMic().getmBlueMicService().disconnect(teamChatBuddyApplication.getBlueMic().selectedBlueMic);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        else{
            switch (teamChatBuddyApplication.getLangue().getNom()) {
                case "Français":
                    Toast.makeText(teamChatBuddyApplication, teamChatBuddyApplication.getString(R.string.disconnected_service_bluemic_fr), Toast.LENGTH_LONG).show();
                    break;
                case "Espagnol":
                    Toast.makeText(teamChatBuddyApplication, teamChatBuddyApplication.getString(R.string.disconnected_service_bluemic_es), Toast.LENGTH_LONG).show();
                    break;
                case "Allemand":
                    Toast.makeText(teamChatBuddyApplication, teamChatBuddyApplication.getString(R.string.disconnected_service_bluemic_de), Toast.LENGTH_LONG).show();
                    break;
                default:
                    Toast.makeText(teamChatBuddyApplication, teamChatBuddyApplication.getString(R.string.disconnected_service_bluemic_en), Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    private void setViews(String cas){
        Log.i(TAG,"setViews("+cas+")");
        switch (cas){
            case "currentState":
                if(teamChatBuddyApplication.getBlueMic().getmBlueMicService() != null){
                    try {
                        setViews("isNotScanning");
                        BlueMicDevice blueMicDevice = teamChatBuddyApplication.getBlueMic().getmBlueMicService().getLastBlueMic();
                        if(blueMicDevice != null){
                            teamChatBuddyApplication.getBlueMic().selectedBlueMic = blueMicDevice;
                            selected_blue_mic_text.setText(
                                    String.format("%s ; %s ; %s",
                                            teamChatBuddyApplication.getBlueMic().selectedBlueMic.getName(),
                                            teamChatBuddyApplication.getBlueMic().selectedBlueMic.getTag(),
                                            teamChatBuddyApplication.getBlueMic().selectedBlueMic.getState()
                                    )
                            );
                            if(teamChatBuddyApplication.getBlueMic().selectedBlueMic.getState().equals("Idle")){
                                setViews("onIdleNodeDiscovered");
                            }
                            else if(teamChatBuddyApplication.getBlueMic().selectedBlueMic.getState().equals("Connected")){
                                setViews("isConnected");
                            }
                            else{
                                setViews("isNotConnected");
                            }
                        }
                        else{
                            setViews("init");
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                else{
                    switch (teamChatBuddyApplication.getLangue().getNom()) {
                        case "Français":
                            Toast.makeText(teamChatBuddyApplication, teamChatBuddyApplication.getString(R.string.disconnected_service_bluemic_fr), Toast.LENGTH_LONG).show();
                            break;
                        case "Espagnol":
                            Toast.makeText(teamChatBuddyApplication, teamChatBuddyApplication.getString(R.string.disconnected_service_bluemic_es), Toast.LENGTH_LONG).show();
                            break;
                        case "Allemand":
                            Toast.makeText(teamChatBuddyApplication, teamChatBuddyApplication.getString(R.string.disconnected_service_bluemic_de), Toast.LENGTH_LONG).show();
                            break;
                        default:
                            Toast.makeText(teamChatBuddyApplication, teamChatBuddyApplication.getString(R.string.disconnected_service_bluemic_en), Toast.LENGTH_LONG).show();
                            break;
                    }
                }
                break;
            case "init":
                start_scan_btn_lyt.setBackgroundResource(R.drawable.ellipse_black);
                stop_scan_btn_lyt.setBackgroundResource(R.drawable.ellipse_green);
                connect_btn_lyt.setBackgroundResource(R.drawable.ellipse_black);
                disconnect_btn_lyt.setBackgroundResource(R.drawable.ellipse_green);

                start_scan_btn_lyt.setClickable(true);
                stop_scan_btn_lyt.setClickable(false);
                connect_btn_lyt.setClickable(false);
                disconnect_btn_lyt.setClickable(false);

                start_scan_btn_lyt.setAlpha(1f);
                stop_scan_btn_lyt.setAlpha(0.3f);
                connect_btn_lyt.setAlpha(0.3f);
                disconnect_btn_lyt.setAlpha(0.3f);
                break;
            case "isScanning":
                start_scan_btn_lyt.setBackgroundResource(R.drawable.ellipse_green);
                start_scan_btn_lyt.setClickable(false);
                start_scan_btn_lyt.setAlpha(0.3f);

                stop_scan_btn_lyt.setBackgroundResource(R.drawable.ellipse_black);
                stop_scan_btn_lyt.setClickable(true);
                stop_scan_btn_lyt.setAlpha(1f);
                break;
            case "isNotScanning":
                start_scan_btn_lyt.setBackgroundResource(R.drawable.ellipse_black);
                start_scan_btn_lyt.setClickable(true);
                start_scan_btn_lyt.setAlpha(1f);

                stop_scan_btn_lyt.setBackgroundResource(R.drawable.ellipse_green);
                stop_scan_btn_lyt.setClickable(false);
                stop_scan_btn_lyt.setAlpha(0.3f);
                break;
            case "onIdleNodeDiscovered":
                connect_btn_lyt.setBackgroundResource(R.drawable.ellipse_black);
                connect_btn_lyt.setClickable(true);
                connect_btn_lyt.setAlpha(1f);
                break;
            case "isConnected":
                connect_btn_lyt.setBackgroundResource(R.drawable.ellipse_green);
                connect_btn_lyt.setClickable(false);
                connect_btn_lyt.setAlpha(0.3f);

                disconnect_btn_lyt.setBackgroundResource(R.drawable.ellipse_black);
                disconnect_btn_lyt.setClickable(true);
                disconnect_btn_lyt.setAlpha(1f);
                break;
            case "isNotConnected":
                connect_btn_lyt.setBackgroundResource(R.drawable.ellipse_black);
                connect_btn_lyt.setClickable(true);
                connect_btn_lyt.setAlpha(1f);

                disconnect_btn_lyt.setBackgroundResource(R.drawable.ellipse_green);
                disconnect_btn_lyt.setClickable(false);
                disconnect_btn_lyt.setAlpha(0.3f);
                break;
            case "connect":
                connect_btn_lyt.setBackgroundResource(R.drawable.ellipse_green);
                connect_btn_lyt.setClickable(false);
                connect_btn_lyt.setAlpha(0.3f);

                disconnect_btn_lyt.setBackgroundResource(R.drawable.ellipse_black);
                disconnect_btn_lyt.setClickable(false);
                disconnect_btn_lyt.setAlpha(0.3f);
                break;
            case "disconnect":
                connect_btn_lyt.setBackgroundResource(R.drawable.ellipse_black);
                connect_btn_lyt.setClickable(false);
                connect_btn_lyt.setAlpha(0.3f);

                disconnect_btn_lyt.setBackgroundResource(R.drawable.ellipse_green);
                disconnect_btn_lyt.setClickable(false);
                disconnect_btn_lyt.setAlpha(0.3f);
                break;
        }
    }


    @Override
    public void update(String message) throws IOException {
        if(message != null){

            Log.i(TAG,"message : "+ message);

            //SCAN RESULT - STATE CHANGE
            if(message.contains("iBlueMicScanListener;onScanChange;")){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String enabled = message.split(";")[2];
                        if(enabled.equals("true")){
                            setViews("isScanning");
                        }
                        else{
                            setViews("isNotScanning");
                        }
                    }
                });
            }

            //SCAN RESULT - NEW DISCOVERY
            if(message.contains("iBlueMicScanListener;onScanDiscovered;")){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        BlueMicDevice blueMicDevice = new BlueMicDevice(
                                message.split(";")[2],
                                message.split(";")[3],
                                message.split(";")[4],
                                message.split(";")[5]
                        );
                        if(!teamChatBuddyApplication.getBlueMic().blueMicDeviceList.contains(blueMicDevice)) teamChatBuddyApplication.getBlueMic().blueMicDeviceList.add(blueMicDevice);
                        discoveredBlueMicAdapter.notifyDataSetChanged();
                    }
                });
            }

            //CONNEXION RESULT
            if(message.contains("iBlueMicConnexionListener;onConnexionStateChange;")){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        BlueMicDevice blueMicDevice = new BlueMicDevice(
                                message.split(";")[2],
                                message.split(";")[3],
                                message.split(";")[4],
                                message.split(";")[5]
                        );
                        String newState = message.split(";")[6];
                        String prevState = message.split(";")[7];
                        if(newState.equals("Connected")){
                            setViews("isConnected");
                        }
                        else {
                            setViews("isNotConnected");
                        }
                        selected_blue_mic_text.setText(String.format("%s ; %s ; %s", blueMicDevice.getName(), blueMicDevice.getTag(), newState));
                    }
                });
            }
        }
    }


    /**
     * Cacher les barres systemUI
     */

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            teamChatBuddyApplication.hideSystemUI(this);
        }
    }
}