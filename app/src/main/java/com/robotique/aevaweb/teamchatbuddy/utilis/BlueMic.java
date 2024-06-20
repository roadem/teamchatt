package com.robotique.aevaweb.teamchatbuddy.utilis;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.robotique.aevaweb.bluemicapp.IBlueMicService;
import com.robotique.aevaweb.bluemicapp.callbacks.IBlueMicConnexionListener;
import com.robotique.aevaweb.bluemicapp.callbacks.IBlueMicScanListener;
import com.robotique.aevaweb.teamchatbuddy.R;
import com.robotique.aevaweb.teamchatbuddy.application.TeamChatBuddyApplication;
import com.robotique.aevaweb.teamchatbuddy.models.BlueMicDevice;

import java.util.ArrayList;
import java.util.List;

public class BlueMic {

    private static final String TAG = "TeamChatBuddy_BlueMic";

    private final TeamChatBuddyApplication teamChatBuddyApplication;

    public List<BlueMicDevice> blueMicDeviceList;
    public BlueMicDevice selectedBlueMic;


    public BlueMic(TeamChatBuddyApplication teamChatBuddyApplication) {
        this.teamChatBuddyApplication = teamChatBuddyApplication;
        blueMicDeviceList = new ArrayList<>();
    }



    /**
     *  Connexion au service externe BlueMicService
     */
    public boolean bindBlueMicService(){
        Intent intent = new Intent("services.BlueMicService");
        intent.setClassName("com.robotique.aevaweb.bluemicapp","com.robotique.aevaweb.bluemicapp.services.BlueMicService");
        return teamChatBuddyApplication.getApplicationContext().bindService(intent, mConnectionBlueMicService, Context.BIND_AUTO_CREATE);
    }

    /**
     *  Callback de connexion au service externe BlueMicService
     */
    private IBlueMicService mBlueMicService;
    private final ServiceConnection mConnectionBlueMicService = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBlueMicService = IBlueMicService.Stub.asInterface(service);
            switch (teamChatBuddyApplication.getLangue().getNom()) {
                case "Français":
                    Toast.makeText(teamChatBuddyApplication, teamChatBuddyApplication.getString(R.string.connected_service_bluemic_fr), Toast.LENGTH_LONG).show();
                    break;
                case "Espagnol":
                    Toast.makeText(teamChatBuddyApplication, teamChatBuddyApplication.getString(R.string.connected_service_bluemic_es), Toast.LENGTH_LONG).show();
                    break;
                case "Allemand":
                    Toast.makeText(teamChatBuddyApplication, teamChatBuddyApplication.getString(R.string.connected_service_bluemic_de), Toast.LENGTH_LONG).show();
                    break;
                default:
                    Toast.makeText(teamChatBuddyApplication, teamChatBuddyApplication.getString(R.string.connected_service_bluemic_en), Toast.LENGTH_LONG).show();
                    break;
            }
        }
        public void onServiceDisconnected(ComponentName className) {
            mBlueMicService = null;
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
    };

    public IBlueMicService getmBlueMicService() {
        return mBlueMicService;
    }





    /**
     *  Listeners
     */

    public IBlueMicScanListener iBlueMicScanListener = new IBlueMicScanListener.Stub() {
        @Override
        public void onScanChange(boolean enabled) throws RemoteException {
            teamChatBuddyApplication.notifyObservers("iBlueMicScanListener;onScanChange;"+enabled);
        }

        @Override
        public void onScanDiscovered(BlueMicDevice blueMicDevice) throws RemoteException {
            Log.i(TAG,"onScanDiscovered : "+ blueMicDevice.toString());
            teamChatBuddyApplication.notifyObservers(
                    "iBlueMicScanListener;"
                            + "onScanDiscovered;"
                            + blueMicDevice.getName()
                            + ";" + blueMicDevice.getTag()
                            + ";" + blueMicDevice.getType()
                            + ";" + blueMicDevice.getState()
            );
        }
    };

    public IBlueMicConnexionListener iBlueMicConnexionListener = new IBlueMicConnexionListener.Stub() {
        @Override
        public void onConnexionStateChange(BlueMicDevice blueMicDevice, String newState, String prevState) throws RemoteException {
            selectedBlueMic = blueMicDevice;
            teamChatBuddyApplication.notifyObservers(
                    "iBlueMicConnexionListener;"
                            + "onConnexionStateChange;"
                            + blueMicDevice.getName()
                            + ";" + blueMicDevice.getTag()
                            + ";" + blueMicDevice.getType()
                            + ";" + blueMicDevice.getState()
                            + ";" + newState
                            + ";" + prevState
            );
        }
    };



}

