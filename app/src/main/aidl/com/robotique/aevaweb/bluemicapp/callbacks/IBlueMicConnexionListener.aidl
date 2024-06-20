// IBlueMicConnexionListener.aidl
package com.robotique.aevaweb.bluemicapp.callbacks;

// Declare any non-default types here with import statements
import com.robotique.aevaweb.bluemicapp.models.BlueMicDevice;

interface IBlueMicConnexionListener {

    //This method is called when the state of the connexion changes
    void onConnexionStateChange(in BlueMicDevice blueMicDevice, String newState, String prevState);

}
