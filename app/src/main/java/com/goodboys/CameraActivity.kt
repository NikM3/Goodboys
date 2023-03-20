package com.goodboys

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.goodboys.databinding.ActivityCameraBinding
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding
    private lateinit var cameraExecutor: ExecutorService

    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var cameraDirection = 1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startCamera(CameraSelector.DEFAULT_BACK_CAMERA)

    }

    override fun onStart() {
        super.onStart()

        val photoButton = binding.takePhotoButton
        val videoButton = binding.takeVideoButton
        val switchButton = binding.switchCameraButton
        cameraExecutor = Executors.newSingleThreadExecutor()

        photoButton.setOnClickListener() {
            takePhoto()
        }

        videoButton.setOnClickListener() {
            captureVideo()
        }

        switchButton.setOnClickListener() {
            switchCamera()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    /**
     * Take a photo, save it, and go to the results screen
     */
    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(MainActivity.FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(MainActivity.TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun
                        onImageSaved(output: ImageCapture.OutputFileResults){
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Log.d(MainActivity.TAG, msg)
                    gotoImageResults(output)
                }
            }
        )
    }

    /**
     * Take a video, save it, and go to the results screen
     */
    private fun captureVideo() {
        val videoCapture = this.videoCapture ?: return

        binding.takeVideoButton.isEnabled = false

        val curRecording = recording
        if (curRecording != null) {
            // Stop the current recording session.
            curRecording.stop()
            recording = null
            return
        }

        // create and start a new recording session
        val name = SimpleDateFormat(MainActivity.FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()
        recording = videoCapture.output
            .prepareRecording(this, mediaStoreOutputOptions)
            .apply {
                if (PermissionChecker.checkSelfPermission(
                        this@CameraActivity,
                        Manifest.permission.RECORD_AUDIO
                    ) ==
                    PermissionChecker.PERMISSION_GRANTED
                ) {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        binding.takeVideoButton.apply {
                            setImageResource(R.drawable.stop_icon)
                            isEnabled = true
                        }
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val msg = "Video capture succeeded: " +
                                    "${recordEvent.outputResults.outputUri}"
                            Log.d(MainActivity.TAG, msg)
                            gotoVideoResults(recordEvent.outputResults)
                        } else {
                            recording?.close()
                            recording = null
                            Log.e(
                                MainActivity.TAG, "Video capture ends with error: " +
                                        "${recordEvent.error}"
                            )
                        }
                        binding.takeVideoButton.apply {
                            setImageResource(R.drawable.video_camera_icon)
                            isEnabled = true
                        }
                    }
                }
            }
    }

    private fun switchCamera() {
        cameraDirection *= -1
        if (cameraDirection == 1)
            startCamera(CameraSelector.DEFAULT_BACK_CAMERA)
        else
            startCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
    }

    private fun startCamera(cameraDirection: CameraSelector) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()
            val recorder = Recorder.Builder()
                .setQualitySelector(
                    QualitySelector.from(
                        Quality.HIGHEST,
                        FallbackStrategy.higherQualityOrLowerThan(Quality.SD)))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)
            val cameraSelector = cameraDirection
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, videoCapture
                )
            } catch(e: Exception) {
                Log.e(MainActivity.TAG, "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun gotoImageResults(file: ImageCapture.OutputFileResults) {
        val intent = Intent(this, ResultsActivity::class.java)
        intent.putExtra(EXTRA_TYPE, IMAGE)
        intent.putExtra(EXTRA_URI, file.savedUri.toString())
        intent.putExtra(EXTRA_CAMERA, cameraDirection.toString())
        startActivity(intent)
    }

    private fun gotoVideoResults(file: OutputResults) {
        val intent = Intent(this, ResultsActivity::class.java)
        intent.putExtra(EXTRA_TYPE, VIDEO)
        intent.putExtra(EXTRA_URI, file.outputUri.toString())
        intent.putExtra(EXTRA_CAMERA, cameraDirection.toString())
        startActivity(intent)
    }

    companion object {
        const val IMAGE = "image"
        const val VIDEO = "video"
        const val EXTRA_TYPE = "file type"
        const val EXTRA_URI = "passed image uri"
        const val EXTRA_CAMERA = "front or back camera"
    }
}