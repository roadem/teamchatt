package com.robotique.aevaweb.teamchatbuddy.utilis.apriltag;


import android.graphics.Bitmap;

import com.google.mlkit.common.MlKitException;

/** An interface to process the images with different vision detectors and custom image models. */
public interface VisionImageProcessor {

    /** Processes ByteBuffer image data, e.g. used for Camera1 live preview case. */
    void processByteBuffer(Bitmap data, FrameMetadata frameMetadata)
            throws MlKitException;

    /** Stops the underlying machine learning model and release resources. */
    void stop();
}
