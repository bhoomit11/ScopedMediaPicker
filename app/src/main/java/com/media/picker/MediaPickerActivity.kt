package com.media.picker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.MediaController
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.media.scopemediapicker.ScopedMediaPicker
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class MediaPickerActivity : AppCompatActivity() {

    private val scopedMediaPicker by lazy {
        ScopedMediaPicker(
            activity = this@MediaPickerActivity,
            requiresCrop = true,
            allowMultipleImages = true,
            mediaType = ScopedMediaPicker.MEDIA_TYPE_IMAGE or ScopedMediaPicker.MEDIA_TYPE_VIDEO
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        videoView.visibility = View.GONE
        iv_img.visibility = View.GONE

        btn_capture.setOnClickListener {
            scopedMediaPicker.start { path, type ->
                if (type == ScopedMediaPicker.MEDIA_TYPE_IMAGE) {
                    Glide.with(this@MediaPickerActivity)
                        .asBitmap()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .load(path)
                        .into(iv_img)

                    iv_img.visibility = View.VISIBLE
                } else {
                    previewVideo(File(path))
                }
            }

//            scopedMediaPicker.startForMultiple { pathList, type ->
//                Log.e("List",pathList.toString())
//            }
        }
    }

    fun previewVideo(file: File) {
        videoView.setVideoPath(file.absolutePath)
        val mediaController = MediaController(this)
        videoView.setMediaController(mediaController)
        mediaController.setMediaPlayer(videoView)
        videoView.visibility = View.VISIBLE
        videoView.start()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        scopedMediaPicker.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        scopedMediaPicker.onActivityResult(requestCode, resultCode, data)
    }
}