// IBlueMicService.aidl
package com.robotique.aevaweb.bluemicapp;

// Declare any non-default types here with import statements
import com.robotique.aevaweb.bluemicapp.models.BlueMicDevice;
import com.robotique.aevaweb.bluemicapp.callbacks.IBlueMicScanListener;
import com.robotique.aevaweb.bluemicapp.callbacks.IBlueMicConnexionListener;
import com.robotique.aevaweb.bluemicapp.callbacks.IBlueMicAudioDataListener;

interface IBlueMicService {

    void startScan(int timeoutMs, IBlueMicScanListener iBlueMicScanListener);
    void stopScan();
    void connect(in BlueMicDevice blueMicDevice, IBlueMicConnexionListener iBlueMicConnexionListener);
    void disconnect(in BlueMicDevice blueMicDevice);
    void startStreaming(in BlueMicDevice blueMicDevice, in boolean activateSTT, in String language, in String googleAuthKey, IBlueMicAudioDataListener iBlueMicAudioDataListener);
    void stopStreaming();
    void releaseAllRessources();
    BlueMicDevice getLastBlueMic();

}
