package com.example.signme;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import android.widget.VideoView;


import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.android.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import androidx.appcompat.app.AppCompatActivity;
public class Test_Video2 extends AppCompatActivity {

    private static final String TAG = "Test_Video2";

    private VideoCapture videoCapture;
    private boolean isPlaying = false;
    private Handler handler;
    private Runnable videoRunnable;
    private ImageView videoView;
    private final double alpha = 1.20; // Fixed contrast
    private final double beta = 20.0;  // Fixed brightness

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test0);

        // Initialize OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "OpenCV initialization failed");
        } else {
            Log.d(TAG, "OpenCV initialized successfully");
        }

        videoView = findViewById(R.id.imgGlide);

        // Copy video from res/raw to internal storage
        String videoPath = copyVideoToInternalStorage();

        videoCapture = new VideoCapture();
        if (!videoCapture.open(videoPath)) {
            Log.e(TAG, "Cannot open video: " + videoPath);
            return;
        }

        handler = new Handler();

        // Runnable to display video frames
        videoRunnable = new Runnable() {
            @Override
            public void run() {
                Mat frame = new Mat();
                if (videoCapture.read(frame)) {
                    // Apply brightness and contrast adjustments
                    Mat adjustedFrame = new Mat();
                    frame.convertTo(adjustedFrame, -1, alpha, beta);

                    // Convert frame to Bitmap
                    android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(
                            adjustedFrame.cols(), adjustedFrame.rows(), android.graphics.Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(adjustedFrame, bitmap);

                    // Display the frame
                    runOnUiThread(() -> videoView.setImageBitmap(bitmap));

                    // Schedule the next frame
                    handler.postDelayed(this, 33); // ~30fps
                } else {
                    // Stop if video ends
                    isPlaying = false;
                }
            }
        };

        // Start playing the video immediately
        isPlaying = true;
        handler.post(videoRunnable);
    }

    private String copyVideoToInternalStorage() {
        try {
            // Access video from res/raw
            Resources resources = getResources();
            InputStream inputStream = resources.openRawResource(R.raw.v3);

            // Copy to internal storage
            File outputFile = new File(getFilesDir(), "v3.mp4");
            FileOutputStream outputStream = new FileOutputStream(outputFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();

            Log.d(TAG, "Video copied to: " + outputFile.getAbsolutePath());
            return outputFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "Error copying video: " + e.getMessage());
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoCapture != null) {
            videoCapture.release();
        }
        if (handler != null) {
            handler.removeCallbacks(videoRunnable);
        }
    }
}