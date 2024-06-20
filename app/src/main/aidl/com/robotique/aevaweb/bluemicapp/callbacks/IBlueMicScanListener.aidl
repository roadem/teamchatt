// IBlueMicScanListener.aidl
package com.robotique.aevaweb.bluemicapp.callbacks;

// Declare any non-default types here with import statements
import com.robotique.aevaweb.bluemicapp.models.BlueMicDevice;

interface IBlueMicScanListener {

    //This method is called when a scan process start or stop --> "enabled" is true if a new scan start, false otherwise
    void onScanChange(in boolean enabled);

    //This method is called when the manager discover a new device --> "blueMicDevice" is the new device discovered
    void onScanDiscovered(in BlueMicDevice blueMicDevice);

}
