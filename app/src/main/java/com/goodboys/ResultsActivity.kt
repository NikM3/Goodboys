package com.goodboys

import android.opengl.Visibility
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.net.toUri
import androidx.core.view.isVisible
import com.goodboys.databinding.ActivityResultsBinding

class ResultsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageView = binding.imageView
        val videoView = binding.videoView
        val text = binding.textView
        val again = binding.againButton

        val type = intent.extras!!.getString(CameraActivity.EXTRA_TYPE)
        val uri = intent.extras!!.getString(CameraActivity.EXTRA_URI)!!.toUri()
        val camera = intent.extras!!.getString(CameraActivity.EXTRA_CAMERA)!!.toInt()

        // Check what type of media to display. Hide and reveal the relevant view
        if (type == CameraActivity.IMAGE) {
            if (camera == 1) {
                text.text = getText(R.string.back_cam_message)
            } else {
                text.text = getText(R.string.front_cam_message)
            }
            imageView.visibility = View.VISIBLE
            videoView.visibility = View.GONE
            imageView.setImageURI(uri)
        } else {
            if (camera == 1) {
                text.text = getText(R.string.back_cam_message)
            } else {
                text.text = getText(R.string.front_cam_message)
            }
            imageView.visibility = View.GONE
            videoView.visibility = View.VISIBLE
            
            videoView.setVideoURI(uri)
            videoView.start()
        }

        again.setOnClickListener() {
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
    }
}