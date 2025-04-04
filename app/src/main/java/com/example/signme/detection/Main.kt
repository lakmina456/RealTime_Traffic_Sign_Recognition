package com.example.signme.detection

import android.graphics.Bitmap
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.signme.R
import com.example.signme.databinding.ActivityDetectionBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Main : AppCompatActivity(), Detector.DetectorListener {
    private lateinit var binding: ActivityDetectionBinding
    private lateinit var detector: Detector
    private lateinit var videoDecoder: VideoDecoder
    private lateinit var executor: ExecutorService

    // Resized width and height for faster processing (adjust based on your model's input size)
    private val inputWidth = 640  // Modify this based on your model's input size
    private val inputHeight = 640  // Modify this based on your model's input size

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize the detector
        detector = Detector(this, Constants.MODEL_PATH, Constants.LABELS_PATH, this)

        // Initialize video decoder for video playback
        videoDecoder = VideoDecoder(this, R.raw.v3)

        // Create a background thread executor
        executor = Executors.newFixedThreadPool(4)  // Optimized thread pool

        // Start video playback and detection
        playVideoWithDetection()
    }

    private fun playVideoWithDetection() {
        executor.execute {
            val durationUs = videoDecoder.getDuration() * 1000
            var currentTimeUs: Long = 0

            while (currentTimeUs < durationUs) {
                val frame: Bitmap? = videoDecoder.getFrameAtTime(currentTimeUs)

                frame?.let {
                    runOnUiThread {
                        // Update UI with the current frame
                        binding.viewFinder.setImageBitmap(it)
                    }

                    // Run detection on a separate thread
                    executor.execute {
                        // Resize the frame to the desired input size for the model
                        val resizedFrame = Bitmap.createScaledBitmap(it, inputWidth, inputHeight, false)
                        detector.detect(resizedFrame)
                    }
                }

                currentTimeUs += 33_000  // Increment by ~33ms for 30 FPS
                Thread.sleep(30)  // Adjust for smoother playback
            }

            videoDecoder.release()
        }
    }

    override fun onEmptyDetect() {
        runOnUiThread { binding.overlay.clear() }
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        runOnUiThread {
            // Update inference time and overlay bounding boxes
            binding.inferenceTime.text = "${inferenceTime}ms"
            binding.overlay.setResults(boundingBoxes)
            binding.overlay.invalidate()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        videoDecoder.release()
        detector.close()
        executor.shutdown()
    }
}


