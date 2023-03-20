package com.goodboys

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.goodboys.databinding.ActivityMainBinding
import com.google.android.material.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }

    override fun onStart() {
        super.onStart()

        val photoButton = binding.photoButton
        val videoButton = binding.videoButton
        val alrightButton = binding.alrightButton

        if(!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        alrightButton.setOnClickListener {
            if (allPermissionsGranted()) {
                val intent = Intent(this, CameraActivity::class.java)
                startActivity(intent)
            } else {
                ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
                )
            }
        }

    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                /* Intentionally left blank */
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }
    }

    companion object {
        const val TAG = "CameraX App"
        const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSSS"
            private const val REQUEST_CODE_PERMISSIONS = 10
            private val REQUIRED_PERMISSIONS =
                mutableListOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                ).apply {
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                        add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                }.toTypedArray()
    }
}