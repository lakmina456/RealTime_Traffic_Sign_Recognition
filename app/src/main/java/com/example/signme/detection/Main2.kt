package com.example.signme.detection

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.ImageButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.signme.R
import com.example.signme.TrafficSignAlertSystem
import com.example.signme.databinding.ActivityCamera2Binding
import com.example.signme.detection.Constants.LABELS_PATH
import com.example.signme.detection.Constants.MODEL_PATH
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Main2 : AppCompatActivity(), Detector.DetectorListener {
    private lateinit var binding: ActivityCamera2Binding
    private val isFrontCamera = false
    private var alertSystem: TrafficSignAlertSystem? = null
    private var firstSignDetected: String? = null
    private var secondSignDetected: String? = null
    private var previousSign = ""
    private val currentSign = ""
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var detector: Detector? = null
    private var textToSpeech: TextToSpeech? = null
    private var contextualRules: JSONObject? = null
    private var isTtsInitialized = false
    private var isMuted = false
    private val audioManager: AudioManager? = null
    private val muteUnmuteButton: ImageButton? = null
    private val handler2 = Handler()

    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCamera2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        cameraExecutor = Executors.newSingleThreadExecutor()
        TrafficSignAlertSystem(this)
        cameraExecutor.execute {
            detector = Detector(baseContext, MODEL_PATH, LABELS_PATH, this)
        }

        binding.muteUnmute1.setOnClickListener { toggleMute() }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        bindListeners()
        // Start updating the date and time
        updateDateTime()

    }
    private fun toggleMute() {
        if (isMuted) {
            // Unmute
            audioManager?.setStreamMute(AudioManager.STREAM_MUSIC, false)
            isMuted = false
        } else {
            // Mute
            audioManager?.setStreamMute(AudioManager.STREAM_MUSIC, true)
            isMuted = true
        }
        updateButtonImage()
    }
    private fun updateDateTime() {
        // Create a Runnable to update the TextView every second
        val runnable: Runnable = object : Runnable {
            override fun run() {
                // Get the current date and time
                var now: LocalDateTime? = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    now = LocalDateTime.now()
                    val formatter =
                        DateTimeFormatter.ofPattern("yyyy MMM d hh:mm:ss a", Locale.getDefault())
                    val currentDateTime = now.format(formatter)
                    binding.dateTimeView.setText(currentDateTime)
                    handler2.postDelayed(this, 1000)
                }
            }
        }

        // Start the first update
        handler2.post(runnable)
    }
    private fun updateButtonImage() {
        if (isMuted) {
            muteUnmuteButton?.setImageResource(R.drawable.volume_off) // Replace with your mute icon
        } else {
            muteUnmuteButton?.setImageResource(R.drawable.volume) // Replace with your unmute icon
        }
    }

    private fun bindListeners() {
        binding.apply {
            isGpu.setOnCheckedChangeListener { buttonView, isChecked ->
                cameraExecutor.submit {
                    detector?.restart(isGpu = isChecked)
                }
                if (isChecked) {
                    buttonView.setBackgroundColor(ContextCompat.getColor(baseContext, R.color.orange))
                } else {
                    buttonView.setBackgroundColor(ContextCompat.getColor(baseContext, R.color.gray))
                }
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider  = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        val rotation = binding.viewFinder.display.rotation

        val cameraSelector = CameraSelector
            .Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview =  Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(rotation)
            .build()

        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(binding.viewFinder.display.rotation)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        imageAnalyzer?.setAnalyzer(cameraExecutor) { imageProxy ->
            val bitmapBuffer =
                Bitmap.createBitmap(
                    imageProxy.width,
                    imageProxy.height,
                    Bitmap.Config.ARGB_8888
                )
            imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
            imageProxy.close()

            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())

                if (isFrontCamera) {
                    postScale(
                        -1f,
                        1f,
                        imageProxy.width.toFloat(),
                        imageProxy.height.toFloat()
                    )
                }
            }

            val rotatedBitmap = Bitmap.createBitmap(
                bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
                matrix, true
            )

            detector?.detect(rotatedBitmap)
        }

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalyzer
            )

            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        } catch(exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) {
        if (it[Manifest.permission.CAMERA] == true) { startCamera() }
    }

    override fun onDestroy() {
        super.onDestroy()
        detector?.close()
        cameraExecutor.shutdown()
    }

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()){
            startCamera()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    companion object {
        private const val TAG = "Camera"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf (
            Manifest.permission.CAMERA
        ).toTypedArray()
    }

    override fun onEmptyDetect() {
        runOnUiThread {
            binding.overlay.clear()
        }
    }
    private fun formatToDrawableName(className: String): String {
        // Convert to lowercase and replace spaces with underscores
        var formattedName = className.lowercase(Locale.getDefault()).replace(" ", "_")

        // Replace slashes with an empty string (to remove /) and remove all non-alphanumeric characters except underscore and digits
        formattedName = formattedName.replace("/", "").replace("[^a-z0-9_]".toRegex(), "")

        // Specifically handle "km/h" to become "kmh"
        formattedName = formattedName.replace("km_h", "kmh")

        return formattedName
    }

    fun speak(message: String?) {
        if (isTtsInitialized) {
            textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }
    fun TrafficSignAlertSystem(context: Context?) {
        textToSpeech = TextToSpeech(context) { status: Int ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech!!.setLanguage(Locale.US)
                isTtsInitialized = true
            } else {
                Log.e("TTS", "Initialization failed")
            }
        }


        if (context != null) {
            loadContextualRules(context)
        }

    }
    private fun alertBasedOnContext(sign1: String?, sign2: String?) {
        try {
            if (contextualRules != null && contextualRules!!.has(sign1)) {
                val rulesForFirstSign = contextualRules!!.getJSONObject(sign1)
                if (rulesForFirstSign.has(sign2)) {
                    val message = rulesForFirstSign.getString(sign2)
                    speak(message)
                }
            }
        } catch (e: java.lang.Exception) {
            Log.e("TrafficSignAlertSystem", "Error checking contextual rules", e)
        }
    }
    fun loadContextualRules(context: Context) {
        try {
            val inputStream = context.assets.open("contextual_rules.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            val jsonString = String(buffer, StandardCharsets.UTF_8)
            contextualRules = JSONObject(jsonString)
        } catch (e: java.lang.Exception) {
            Log.e("TrafficSignAlertSystem", "Error loading contextual rules", e)
        }
    }
    private fun updatePreviousAndCurrentSign1(className: String) {
        if (firstSignDetected == null) {
            firstSignDetected = className
            speak(className)
            binding.imageView1.setImageResource(
                resources.getIdentifier(
                    formatToDrawableName(className), "drawable",
                    packageName
                )
            )
        } else if (className != firstSignDetected && secondSignDetected == null) {
            secondSignDetected = className
            speak(className)
            alertBasedOnContext(firstSignDetected, secondSignDetected)
            binding.imageView2.setImageResource(
                resources.getIdentifier(
                    formatToDrawableName(firstSignDetected!!), "drawable",
                    packageName
                )
            )
            binding.imageView1.setImageResource(
                resources.getIdentifier(
                    formatToDrawableName(secondSignDetected!!), "drawable",
                    packageName
                )
            )

        } else if (className != firstSignDetected && className != secondSignDetected) {
            previousSign = firstSignDetected as String
            firstSignDetected = secondSignDetected
            secondSignDetected = className
            alertBasedOnContext(previousSign, secondSignDetected)
            previousSign = secondSignDetected as String
            secondSignDetected = className
            alertBasedOnContext(previousSign, secondSignDetected)
            binding.imageView2.setImageResource(
                resources.getIdentifier(
                    firstSignDetected?.let { formatToDrawableName(it) }, "drawable",
                    packageName
                )
            )
            binding.imageView1.setImageResource(
                resources.getIdentifier(
                    formatToDrawableName(secondSignDetected!!), "drawable",
                    packageName
                )
            )
        }
    }
    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        runOnUiThread {
            binding.inferenceTime.text = "${inferenceTime}ms"
            binding.overlay.apply {
                setResults(boundingBoxes)
                invalidate()
            }
            if (boundingBoxes.isNotEmpty()) {
                val firstBoundingBox = boundingBoxes[0]
                // Set the label_view to the class name of the first detected bounding box
                binding.labelView.text = firstBoundingBox.clsName
                binding.signView.setImageResource(
                    resources.getIdentifier(
                        formatToDrawableName(firstBoundingBox.clsName), "drawable",
                        packageName
                    )
                )
                updatePreviousAndCurrentSign1(firstBoundingBox.clsName)
            } else {
                binding.labelView.text = ""
            }

        }
    }
}
