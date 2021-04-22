package com.media.scopemediapicker

import android.content.Context
import android.os.Build
import android.os.Environment
import java.io.File
import java.net.MalformedURLException
import java.net.URL

enum class TYPES {
    LOCAL_FILE_DIRECTORY,
    LOCAL_CACHE_DIRECTORY,

    PUBLIC_OBB_DIRECTORY,
    PUBLIC_CACHE_DIRECTORY,
    PUBLIC_MEDIA_DIRECTORY,
    PUBLIC_IMAGE_DIRECTORY,
    PUBLIC_VIDEO_DIRECTORY,
    PUBLIC_AUDIO_DIRECTORY,
    PUBLIC_PDF_DIRECTORY,
    PUBLIC_OTHER_DIRECTORY,

    GENERAL_PUBLIC_DIRECTORY,
    GENERAL_PUBLIC_DOWNLOAD_DIRECTORY
}

class FilePaths(val context: Context) {

    fun getLocalDirectory(type: TYPES = TYPES.LOCAL_FILE_DIRECTORY): File? {
        val folder = when (type) {

            TYPES.LOCAL_FILE_DIRECTORY -> {
//                    /data/user/0/com.atp/files
                context.filesDir
            }
            TYPES.LOCAL_CACHE_DIRECTORY -> {
//                    /data/user/0/com.atp/cache
                context.cacheDir
            }

            TYPES.PUBLIC_OBB_DIRECTORY -> {
//                    /storage/emulated/0/Android/obb/com.atp
                context.obbDir
            }
            TYPES.PUBLIC_CACHE_DIRECTORY -> {
//                    /storage/emulated/0/Android/data/com.atp/cache
                context.externalCacheDir
            }
            TYPES.PUBLIC_MEDIA_DIRECTORY -> {
//                    /storage/emulated/0/Android/media/com.atp
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (context.externalMediaDirs?.isEmpty() == false) context.filesDir else context.externalMediaDirs.first()
                } else {
                    return null
                }
            }

            TYPES.PUBLIC_IMAGE_DIRECTORY -> {
                //                    /storage/emulated/0/Android/data/com.atp/files/Image
                context.getExternalFilesDir("Image")
            }
            TYPES.PUBLIC_VIDEO_DIRECTORY -> {
                //                    /storage/emulated/0/Android/data/com.atp/files/Video
                context.getExternalFilesDir("Video")
            }
            TYPES.PUBLIC_AUDIO_DIRECTORY -> {
                //                    /storage/emulated/0/Android/data/com.atp/files/Audio
                context.getExternalFilesDir("Audio")
            }
            TYPES.PUBLIC_PDF_DIRECTORY -> {
                //                    /storage/emulated/0/Android/data/com.atp/files/Pdf
                context.getExternalFilesDir("Pdf")
            }
            TYPES.PUBLIC_OTHER_DIRECTORY -> {
                //                    /storage/emulated/0/Android/data/com.atp/files/Other
                context.getExternalFilesDir("Other")
            }

            TYPES.GENERAL_PUBLIC_DIRECTORY -> {
                //                    /storage/emulated/0
                Environment.getExternalStorageDirectory()
            }
            TYPES.GENERAL_PUBLIC_DOWNLOAD_DIRECTORY -> {
                //                    /storage/emulated/0/Download
                Environment.getExternalStoragePublicDirectory("Download")
            }
            else -> {
                Environment.getExternalStoragePublicDirectory("Download")
            }
        }
        if (folder != null && !folder.exists()) {
            folder.mkdirs()
        }
        return folder
    }

    /*fun getLocalTempFileFromURL(
        context: Context,
        url: String?,
        type: TYPES = TYPES.LOCAL_FILE_DIRECTORY
    ): File {
        return File(getLocalDirectory(context, type = type), getFileNameFromURL(url) + ".part")
    }

    fun getLocalFileFromURL(
        context: Context,
        url: String?,
        type: TYPES = TYPES.LOCAL_FILE_DIRECTORY
    ): File {
        return File(getLocalDirectory(context, type = type), getFileNameFromURL(url))
    }*/

    fun getFileNameFromURL(url: String?): String {
        if (url == null) {
            return ""
        }
        try {
            val resource = URL(url)
            val host = resource.getHost()
            if (host.length > 0 && url.endsWith(host)) {
                // handle ...example.com
                return ""
            }
        } catch (e: MalformedURLException) {
            return ""
        }

        val startIndex = url.lastIndexOf('/') + 1
        val length = url.length

        // find end index for ?
        var lastQMPos = url.lastIndexOf('?')
        if (lastQMPos == -1) {
            lastQMPos = length
        }

        // find end index for #
        var lastHashPos = url.lastIndexOf('#')
        if (lastHashPos == -1) {
            lastHashPos = length
        }

        // calculate the end index
        val endIndex = Math.min(lastQMPos, lastHashPos)
        val fileName = url.substring(startIndex, endIndex)
        return fileName
    }
}