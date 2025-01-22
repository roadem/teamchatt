package com.robotique.aevaweb.teamchatbuddy.utilis;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.util.Log;

import com.robotique.aevaweb.teamchatbuddy.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;


public class ImageGenerator extends AsyncTask<String, Void, Bitmap> {

    public interface ImageSaveCallback {
        void onImageSaved(String savedFileName);
    }

    private WeakReference<Context> contextRef;
    private String filename;
    private final ImageSaveCallback callback;

    public ImageGenerator(Context context, String filename, ImageSaveCallback callback) {
        contextRef = new WeakReference<>(context);
        this.filename = filename;
        this.callback = callback;
    }

    @Override
    protected Bitmap doInBackground(String... params) {
        String imageUrl = params[0];
        Bitmap bitmap = null;

        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            bitmap = BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);
        if (bitmap != null) {
            String storedFileName = saveImageToInternalStorage(bitmap, filename);
            if (storedFileName != null) {
                if (callback != null) {
                    Log.i( "HHO", "callback ImageSaved "+ storedFileName);
                    callback.onImageSaved(storedFileName); // Notify the caller with the final filename
                }
            } else {
                Log.e("HHO", "Failed to save image.");
            }
        }
    }

    private String saveImageToInternalStorage(Bitmap bitmap, String fileName) {
        Context context = contextRef.get();
        if (context != null) {
            File directory = new File( context.getString(R.string.path), "TeamChatBuddy/images/recv");

            // Vérifiez si le répertoire existe, sinon, créez-le
            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    Log.e("saveImageToCustomDirectory", "Erreur: Impossible de créer le répertoire");
                    return null;
                }
            }

            File file = new File(directory, fileName);
            String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
            String extension = fileName.substring(fileName.lastIndexOf('.'));
            int counter = 1;

            while (file.exists()) {
                file = new File(directory, baseName + "_" + counter + extension);
                counter++;
            }

            try {
                FileOutputStream outputStream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.flush();
                outputStream.close();
                // Mettre à jour la galerie pour que l'image soit visible
                MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()}, null, null);
                return file.getName();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }
}



