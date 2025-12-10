package com.robotique.aevaweb.teamchatbuddy.utilis.apriltag;

import static android.content.Context.CAMERA_SERVICE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.robotique.aevaweb.teamchatbuddy.R;
import com.robotique.aevaweb.teamchatbuddy.activities.MainActivity;
import com.robotique.aevaweb.teamchatbuddy.application.TeamChatBuddyApplication;

import java.util.Collections;
import java.util.List;

public class CameraUtils {

    // Constants
    private static final String TAG = "CameraUtils";

    // Camera Resolution
    private int currentWidth = 320;
    private int currentHeight = 240;

    // Application
    private TeamChatBuddyApplication teamChatBuddyApplication;

    // Context
    private Context context;

    // Zoom Camera objects and variables
    private CameraDevice cameraDeviceZoom;
    private ImageReader imageReaderZoom;

    public CameraUtils(Context context, TeamChatBuddyApplication teamChatBuddyApplication) {
        this.context = context;
        this.teamChatBuddyApplication = teamChatBuddyApplication;
    }


    @SuppressLint("MissingPermission")
    public void readyCamera(List<String> types) {
        Log.i("MYA_QR", "readyCamera: ");
        CameraManager manager = (CameraManager) context.getSystemService(CAMERA_SERVICE);
        cameraView.setVisibility(View.VISIBLE);
        try {
            manager.openCamera(
                    "1",
                    new CameraDevice.StateCallback() {
                        @Override
                        public void onOpened(@NonNull CameraDevice camera) {
                            Log.d(TAG, "CameraDevice.StateCallback onOpened");
                            cameraDeviceZoom = camera;
                            getCameraFrames(types);
                        }

                        @Override
                        public void onDisconnected(@NonNull CameraDevice camera) {
                            Log.w(TAG, "CameraDevice.StateCallback onDisconnected");
                        }

                        @Override
                        public void onError(@NonNull CameraDevice camera, int error) {
                            Log.e(TAG, "CameraDevice.StateCallback onError " + error);
                            if (error == 4){

                                cameraDeviceZoom.close();
                                cameraDeviceZoom = null;
                                readyCamera(types);


                            }
                        }
                    },
                    null
            );

        } catch (Exception e){
            Log.e(TAG, "Exception readyCamera() : "+e);
        }
    }

    private void getCameraFrames(List<String> types) {
        try {

            final CaptureRequest.Builder captureBuilder = cameraDeviceZoom.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            imageReaderZoom = ImageReader.newInstance(currentWidth, currentHeight, ImageFormat.YUV_420_888, 2);
            imageReaderZoom.setOnImageAvailableListener(
                    new ImageReader.OnImageAvailableListener() {
                        @Override
                        public void onImageAvailable(ImageReader reader) {
                            //Image image = reader.acquireNextImage();
                            Image image = reader.acquireLatestImage();
                            if (image != null) {
                                final Bitmap bitmap = BitmapUtils.getBitmap(
                                        image,
                                        image.getPlanes()[0].getRowStride(),
                                        image.getPlanes()[1].getRowStride()
                                );

                                ImageView zoom_iv = ((MainActivity) context).findViewById(R.id.previewViewQr);
                                zoom_iv.setImageBitmap(bitmap);
                                teamChatBuddyApplication.processImage(context, bitmap, image, types);

                                //image.close();
                                //Log.i(TAG, "processImage: image.close(3) ");

                            }
                        }
                    },
                    null
            );
            captureBuilder.addTarget(imageReaderZoom.getSurface());

            cameraDeviceZoom.createCaptureSession(Collections.singletonList(imageReaderZoom.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.setRepeatingRequest(captureBuilder.build(), null, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void stopCamera() {
        try {
            Log.v("MYA_QR", "mouth stopCamera: releasing camera resources");

            if (cameraDeviceZoom != null) {
                cameraDeviceZoom.close();
                cameraDeviceZoom = null;
            }

            if (imageReaderZoom != null) {
                imageReaderZoom.close();
                imageReaderZoom = null;
            }
            if (cameraView != null) {
                cameraView.setVisibility(View.GONE);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error while stopping camera: ", e);
        }

    }
    private View cameraView;

    public void setCameraView(View cameraView) {
        this.cameraView = cameraView;
    }


}
