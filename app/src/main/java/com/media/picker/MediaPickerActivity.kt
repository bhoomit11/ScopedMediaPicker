package com.media.picker

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.MediaController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.media.scopemediapicker.ScopedMediaPicker
import com.media.scopemediapicker.model.FileData
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class MediaPickerActivity : AppCompatActivity() {

    private val scopedMediaPicker by lazy {
        ScopedMediaPicker(
            activity = this@MediaPickerActivity,
            requiresCrop = true,
            allowMultipleFiles = true
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        videoView.visibility = View.GONE
        iv_img.visibility = View.GONE

        btnCapture.setOnClickListener {
            scopedMediaPicker.startMediaPicker(
                mediaType = ScopedMediaPicker.MEDIA_TYPE_IMAGE,
                actionType = ScopedMediaPicker.ACTION_TYPE_GALLERY
            ) { pathList, type ->
                when (type) {
                    ScopedMediaPicker.MEDIA_TYPE_IMAGE -> {
                        Glide.with(this@MediaPickerActivity)
                            .asBitmap()
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .load(pathList.first())
                            .into(iv_img)

                        iv_img.visibility = View.VISIBLE
                    }
                    ScopedMediaPicker.MEDIA_TYPE_VIDEO -> {
                        previewVideo(File(pathList.first()))
                    }
                }

                Log.e("FilePath", pathList.toString())
            }
        }
        btnFile.setOnClickListener {
            scopedMediaPicker.startFilePicker(
            ) { list ->
                Log.e("FilePath", list.toString())
                showFile(list.first())
            }
        }
    }

    private fun previewVideo(file: File) {
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

    private fun showFile(fileData: FileData) {
        try {

            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.setDataAndType(fileData.fileUri, fileData.mimeType)
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No activity found to open this attachment.", Toast.LENGTH_LONG).show()
        }
    }
}