// IBlueMicAudioDataListener.aidl
package com.robotique.aevaweb.bluemicapp.callbacks;

// Declare any non-default types here with import statements

interface IBlueMicAudioDataListener {

    //This method is called when a new audioData is available
    void onNewAudioData(in String[] audioSample);

    //This method is called when stateVAD changes stateVAD = "NOISE" or "SPEECH"
    void onNewStateVAD(in String stateVAD);

    /*
     * This method is called when the request has generated a response
     * text : the generated result (can be null)
     */
    void onSTTResponse(in String text);

    /*
     * This method is called when the request has generated an error
     * type :
     *  0 NO_ERROR
     *  1 IO_CONNECTION_ERROR
     *  2 RESPONSE_ERROR
     *  3 REQUEST_FAILED
     *  4 NOT_RECOGNIZED
     *  5 NETWORK_PROBLEM
     *  6 LOW_CONFIDENCE
     */
    void onSTTError(in int type);

}
