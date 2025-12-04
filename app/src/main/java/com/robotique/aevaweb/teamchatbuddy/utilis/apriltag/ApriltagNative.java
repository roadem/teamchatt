package com.robotique.aevaweb.teamchatbuddy.utilis.apriltag;

import com.robotique.aevaweb.teamchatbuddy.models.ApriltagDetection;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Interface to native C AprilTag library.
 */

public class ApriltagNative {
    static {
        System.loadLibrary("apriltag");
        native_init_zoom();
    }

    public static native void native_init_zoom();

    public static native ArrayList<ApriltagDetection> apriltag_detect_yuv_zoom(ByteBuffer src, int width, int height, int stride);

}
