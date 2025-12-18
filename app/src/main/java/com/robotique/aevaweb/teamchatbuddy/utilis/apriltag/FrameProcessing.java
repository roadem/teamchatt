package com.robotique.aevaweb.teamchatbuddy.utilis.apriltag;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.IdentityHashMap;

public class FrameProcessing {
    private static final String TAG = "MIDemoApp:CameraSource";
    FrameProcessingRunnable processingRunnable;
    private Thread processingThread;

    public FrameProcessing() {
        processingRunnable = new FrameProcessingRunnable();
    }

    public static final int IMAGE_FORMAT = ImageFormat.JPEG;
    public static final int DEFAULT_REQUESTED_CAMERA_PREVIEW_WIDTH = 640;
    public static final int DEFAULT_REQUESTED_CAMERA_PREVIEW_HEIGHT = 480;
    private static final float REQUESTED_FPS = 30.0f;
    private final IdentityHashMap<byte[], ByteBuffer> bytesToByteBuffer = new IdentityHashMap<>();

    private final Object processorLock = new Object();
    private VisionImageProcessor frameProcessor;

    public void setMachineLearningFrameProcessor(VisionImageProcessor processor) {
        synchronized (processorLock) {
            if (frameProcessor != null) {
                frameProcessor.stop();
            }
            frameProcessor = processor;
            processingThread = new Thread(processingRunnable);
            processingRunnable.setActive(true);
            processingThread.start();
        }
    }

    public void setNextFrame(Bitmap data){
        processingRunnable.setNextFrame(data);
    }

    public synchronized void stop() {
        processingRunnable.setActive(false);
        if (processingThread != null) {
            try {
                // Wait for the thread to complete to ensure that we can't have multiple threads
                // executing at the same time (i.e., which would happen if we called start too
                // quickly after stop).
                processingThread.join();
            } catch (InterruptedException e) {
                Log.d(TAG, "Frame processing thread interrupted on release.");
            }
            processingThread = null;
        }
    }

    private class FrameProcessingRunnable implements Runnable {

        // This lock guards all of the member variables below.
        private final Object lock = new Object();
        private boolean active = true;

        // These pending variables hold the state associated with the new frame awaiting processing.
        private Bitmap pendingFrameData;

        FrameProcessingRunnable() {}

        /** Marks the runnable as active/not active. Signals any blocked threads to continue. */
        void setActive(boolean active) {
            synchronized (lock) {
                this.active = active;
                lock.notifyAll();
            }
        }

        /**
         * Sets the frame data received from the camera. This adds the previous unused frame buffer (if
         * present) back to the camera, and keeps a pending reference to the frame data for future use.
         */
        @SuppressWarnings("ByteBufferBackingArray")
        void setNextFrame(Bitmap data) {
            synchronized (lock) {
                if (pendingFrameData != null) {
                    pendingFrameData = null;
                }

                pendingFrameData = data;

                // Notify the processor thread if it is waiting on the next frame (see below).
                lock.notifyAll();
            }
        }

        /**
         * As long as the processing thread is active, this executes detection on frames continuously.
         * The next pending frame is either immediately available or hasn't been received yet. Once it
         * is available, we transfer the frame info to local variables and run detection on that frame.
         * It immediately loops back for the next frame without pausing.
         *
         * <p>If detection takes longer than the time in between new frames from the camera, this will
         * mean that this loop will run without ever waiting on a frame, avoiding any context switching
         * or frame acquisition time latency.
         *
         * <p>If you find that this is using more CPU than you'd like, you should probably decrease the
         * FPS setting above to allow for some idle time in between frames.
         */
        @SuppressLint("InlinedApi")
        @SuppressWarnings({"GuardedBy", "ByteBufferBackingArray"})
        @Override
        public void run() {
            Bitmap data;

            while (true) {
                synchronized (lock) {
                    while (active && (pendingFrameData == null)) {
                        try {
                            // Wait for the next frame to be received from the camera, since we
                            // don't have it yet.
                            lock.wait();
                        } catch (InterruptedException e) {
                            Log.d(TAG, "Frame processing loop terminated.", e);
                            return;
                        }
                    }

                    if (!active) {
                        // Exit the loop once this camera source is stopped or released.  We check
                        // this here, immediately after the wait() above, to handle the case where
                        // setActive(false) had been called, triggering the termination of this
                        // loop.
                        return;
                    }

                    // Hold onto the frame data locally, so that we can use this for detection
                    // below.  We need to clear pendingFrameData to ensure that this buffer isn't
                    // recycled back to the camera before we are done using that data.
                    data = pendingFrameData;
                    pendingFrameData = null;
                }

                // The code below needs to run outside of synchronization, because this will allow
                // the camera to add pending frame(s) while we are running detection on the current
                // frame.

                try {
                    synchronized (processorLock) {
                        frameProcessor.processByteBuffer(
                                data,
                                new FrameMetadata.Builder()
                                        .setWidth(DEFAULT_REQUESTED_CAMERA_PREVIEW_WIDTH)
                                        .setHeight(DEFAULT_REQUESTED_CAMERA_PREVIEW_HEIGHT)
                                        .build());
                    }
                } catch (Exception t) {
                    Log.e(TAG, "Exception thrown from receiver.", t);
                }
            }
        }
    }
}
