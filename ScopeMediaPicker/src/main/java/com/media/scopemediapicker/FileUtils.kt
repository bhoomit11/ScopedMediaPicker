package com.media.scopemediapicker

import android.R.attr.data
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


private const val tag = "FileUtils"

suspend fun Activity.compressImageFile(
    path: String,
    shouldOverride: Boolean = true,
    uri: Uri
): String {
    return withContext(Dispatchers.IO) {
        var scaledBitmap: Bitmap? = null

        try {
            val (hgt, wdt) = getImageHgtWdt(uri)
            try {
                val bm = getBitmapFromUri(uri)
                Log.d(tag, "original bitmap height${bm?.height} width${bm?.width}")
                Log.d(tag, "Dynamic height$hgt width$wdt")
            } catch (e: Exception) {
                e.printStackTrace()
            }
            // Part 1: Decode image
            val unscaledBitmap = decodeFile(this@compressImageFile, uri, wdt, hgt, ScalingLogic.FIT)
            if (unscaledBitmap != null) {
                if (!(unscaledBitmap.width <= 800 && unscaledBitmap.height <= 800)) {
                    // Part 2: Scale image
                    scaledBitmap = createScaledBitmap(unscaledBitmap, wdt, hgt, ScalingLogic.FIT)
                } else {
                    scaledBitmap = unscaledBitmap
                }
            }

            // Store to tmp file
            val mFolder = File("$filesDir/Images")
            if (!mFolder.exists()) {
                mFolder.mkdir()
            }

            val tmpFile = File(mFolder.absolutePath, "IMG_${getTimestampString()}.png")

            var fos: FileOutputStream? = null
            try {
                fos = FileOutputStream(tmpFile)
                scaledBitmap?.compress(
                    Bitmap.CompressFormat.PNG,
                    getImageQualityPercent(tmpFile),
                    fos
                )
                fos.flush()
                fos.close()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()

            } catch (e: Exception) {
                e.printStackTrace()
            }

            var compressedPath = ""
            if (tmpFile.exists() && tmpFile.length() > 0) {
                compressedPath = tmpFile.absolutePath
                if (shouldOverride) {
                    val srcFile = File(path)
                    val result = tmpFile.copyTo(srcFile, true)
                    Log.d(tag, "copied file ${result.absolutePath}")
                    Log.d(tag, "Delete temp file ${tmpFile.delete()}")
                }
            }

            scaledBitmap?.recycle()

            return@withContext if (shouldOverride) path else compressedPath
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        return@withContext ""
    }

}


@Throws(IOException::class)
fun Context.getBitmapFromUri(uri: Uri, options: BitmapFactory.Options? = null): Bitmap? {
    val parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")
    val fileDescriptor = parcelFileDescriptor?.fileDescriptor
    val image: Bitmap? = if (options != null)
        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options)
    else
        BitmapFactory.decodeFileDescriptor(fileDescriptor)
    parcelFileDescriptor?.close()
    return image
}

fun Context.getApplicationName(): String {
    val applicationInfo = this.applicationInfo
    val stringId = applicationInfo.labelRes
    return if (stringId == 0) applicationInfo.nonLocalizedLabel.toString() else this.getString(
        stringId
    )
}

fun getTimestampString(): String {
    val date = Calendar.getInstance()
    return SimpleDateFormat("yyyy MM dd hh mm ss", Locale.US).format(date.time).replace(" ", "")
}


@SuppressLint("NewApi")
fun Activity.getVideoPath(uri: Uri): String? {
    // check here to KITKAT or new version
    val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
    var selection: String? = null
    var selectionArgs: Array<String>? = null
    // DocumentProvider
    if (isKitKat) {
        // ExternalStorageProvider
        if (isExternalStorageDocument(uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":").toTypedArray()
            val type = split[0]
            val fullPath = getPathFromExtSD(split)
            return if (fullPath !== "") {
                fullPath
            } else {
                null
            }
        }


        // DownloadsProvider
        if (isDownloadsDocument(uri)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val id: String
                var cursor: Cursor? = null
                try {
                    cursor = this.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)
                    if (cursor != null && cursor.moveToFirst()) {
                        val fileName: String = cursor.getString(0)
                        val path: String = Environment.getExternalStorageDirectory().toString().toString() + "/Download/" + fileName
                        if (!TextUtils.isEmpty(path)) {
                            return path
                        }
                    }
                } finally {
                    cursor?.close()
                }
                id = DocumentsContract.getDocumentId(uri)
                if (!TextUtils.isEmpty(id)) {
                    if (id.startsWith("raw:")) {
                        return id.replaceFirst("raw:".toRegex(), "")
                    }
                    val contentUriPrefixesToTry = arrayOf(
                        "content://downloads/public_downloads",
                        "content://downloads/my_downloads"
                    )
                    for (contentUriPrefix in contentUriPrefixesToTry) {
                        return try {
                            val contentUri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), java.lang.Long.valueOf(id))
                            getDataColumn(this, contentUri, null, null)
                        } catch (e: NumberFormatException) {
                            //In Android 8 and Android P the id is not a number
                            uri.path!!.replaceFirst("^/document/raw:".toRegex(), "").replaceFirst("^raw:".toRegex(), "")
                        }
                    }
                }
            } else {
                var contentUri: Uri? = null
                val id = DocumentsContract.getDocumentId(uri)
                if (id.startsWith("raw:")) {
                    return id.replaceFirst("raw:".toRegex(), "")
                }
                try {
                    contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(id)
                    )
                } catch (e: NumberFormatException) {
                    e.printStackTrace()
                }
                if (contentUri != null) {
                    return getDataColumn(this, contentUri, null, null)
                }
            }
        }


        // MediaProvider
        if (isMediaDocument(uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":").toTypedArray()
            val type = split[0]
            var contentUri: Uri? = null
            if ("image" == type) {
                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            } else if ("video" == type) {
                contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else if ("audio" == type) {
                contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
            selection = "_id=?"
            selectionArgs = arrayOf(split[1])
            return getDataColumn(
                this, contentUri, selection,
                selectionArgs
            )
        }
        if (isGoogleDriveUri(uri)) {
            return getDriveFilePath(uri, this)
        }
        if (isWhatsAppFile(uri)) {
            return getFilePathForWhatsApp(uri, this)
        }
        if ("content".equals(uri.scheme, ignoreCase = true)) {
            if (isGooglePhotosUri(uri)) {
                return uri.lastPathSegment
            }
            if (isGoogleDriveUri(uri)) {
                return getDriveFilePath(uri, this)
            }
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                // return getFilePathFromURI(context,uri);
                copyFileToInternalStorage(uri, "userfiles", this)
                // return getRealPathFromURI(context,uri);
            } else {
                getDataColumn(this, uri, null, null)
            }
        }
        if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }
    } else {
        if (isWhatsAppFile(uri)) {
            return getFilePathForWhatsApp(uri, this)
        }
        if ("content".equals(uri.scheme, ignoreCase = true)) {
            val projection = arrayOf(
                MediaStore.Images.Media.DATA
            )
            var cursor: Cursor? = null
            try {
                cursor = this.contentResolver
                    .query(uri, projection, selection, selectionArgs, null)
                val column_index: Int = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA) ?: -1
                if (cursor?.moveToFirst() == true) {
                    return cursor.getString(column_index)
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }
    return null
}

private fun fileExists(filePath: String): Boolean {
    val file = File(filePath)
    return file.exists()
}

private fun getPathFromExtSD(pathData: Array<String>): String {
    val type = pathData[0]
    val relativePath = "/" + pathData[1]
    var fullPath = ""

    // on my Sony devices (4.4.4 & 5.1.1), `type` is a dynamic string
    // something like "71F8-2C0A", some kind of unique id per storage
    // don't know any API that can get the root path of that storage based on its id.
    //
    // so no "primary" type, but let the check here for other devices
    if ("primary".equals(type, ignoreCase = true)) {
        fullPath = Environment.getExternalStorageDirectory().toString() + relativePath
        if (fileExists(fullPath)) {
            return fullPath
        }
    }

    // Environment.isExternalStorageRemovable() is `true` for external and internal storage
    // so we cannot relay on it.
    //
    // instead, for each possible path, check if file exists
    // we'll start with secondary storage as this could be our (physically) removable sd card
    fullPath = System.getenv("SECONDARY_STORAGE") + relativePath
    if (fileExists(fullPath)) {
        return fullPath
    }
    fullPath = System.getenv("EXTERNAL_STORAGE") + relativePath
    return if (fileExists(fullPath)) {
        fullPath
    } else fullPath
}

private fun getDriveFilePath(uri: Uri, activity: Activity): String? {
    val returnCursor = activity.contentResolver.query(uri, null, null, null, null)
    /*
     * Get the column indexes of the data in the Cursor,
     *     * move to the first row in the Cursor, get the data,
     *     * and display it.
     * */
    val nameIndex: Int = returnCursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME) ?: -1
    val sizeIndex: Int = returnCursor?.getColumnIndex(OpenableColumns.SIZE) ?: -1
    returnCursor?.moveToFirst()
    val name: String = returnCursor?.getString(nameIndex) ?: ""
    val size = returnCursor?.getLong(sizeIndex).toString()
    val file = File(activity.cacheDir, name)
    try {
        val inputStream = activity.contentResolver.openInputStream(uri)
        val outputStream = FileOutputStream(file)
        var read = 0
        val maxBufferSize = 1 * 1024 * 1024
        val bytesAvailable = inputStream?.available()

        //int bufferSize = 1024;
        val bufferSize = bytesAvailable?.coerceAtMost(maxBufferSize)
        val buffers = ByteArray(bufferSize ?: -1)
        while (inputStream?.read(buffers)?.also { read = it } != -1) {
            outputStream.write(buffers, 0, read)
        }
        Log.e("File Size", "Size " + file.length())
        inputStream.close()
        outputStream.close()
        Log.e("File Path", "Path " + file.path)
        Log.e("File Size", "Size " + file.length())
    } catch (e: java.lang.Exception) {
        Log.e("Exception", e.message!!)
    }
    return file.path
}

/***
 * Used for Android Q+
 * @param uri
 * @param newDirName if you want to create a directory, you can set this variable
 * @return
 */
private fun copyFileToInternalStorage(uri: Uri, newDirName: String, activity: Activity): String? {
    val returnCursor = activity.contentResolver.query(
        uri, arrayOf(
            OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE
        ), null, null, null
    )


    /*
     * Get the column indexes of the data in the Cursor,
     *     * move to the first row in the Cursor, get the data,
     *     * and display it.
     * */
    val nameIndex = returnCursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME) ?: -1
    val sizeIndex = returnCursor?.getColumnIndex(OpenableColumns.SIZE) ?: -1
    returnCursor?.moveToFirst()
    val name = returnCursor?.getString(nameIndex)
    val size = returnCursor?.getLong(sizeIndex).toString()
    val output: File
    if (newDirName != "") {
        val dir: File = File(activity.filesDir.toString() + "/" + newDirName)
        if (!dir.exists()) {
            dir.mkdir()
        }
        output = File(activity.filesDir.toString() + "/" + newDirName + "/" + name)
    } else {
        output = File(activity.filesDir.toString() + "/" + name)
    }
    try {
        val inputStream = activity.contentResolver.openInputStream(uri)
        val outputStream = FileOutputStream(output)
        var read = 0
        val bufferSize = 1024
        val buffers = ByteArray(bufferSize)
        while (inputStream?.read(buffers)?.also { read = it } != -1) {
            outputStream.write(buffers, 0, read)
        }
        inputStream.close()
        outputStream.close()
    } catch (e: java.lang.Exception) {
        Log.e("Exception", e.message!!)
    }
    return output.path
}

private fun getFilePathForWhatsApp(uri: Uri, activity: Activity): String? {
    return copyFileToInternalStorage(uri, "whatsapp", activity)
}

private fun getDataColumn(context: Context, uri: Uri?, selection: String?, selectionArgs: Array<String>?): String? {
    var cursor: Cursor? = null
    val column = "_data"
    val projection = arrayOf(column)
    try {
        cursor = context.contentResolver.query(
            uri!!, projection,
            selection, selectionArgs, null
        )
        if (cursor != null && cursor.moveToFirst()) {
            val index: Int = cursor.getColumnIndexOrThrow(column)
            return cursor.getString(index)
        }
    } finally {
        cursor?.close()
    }
    return null
}

private fun isExternalStorageDocument(uri: Uri): Boolean {
    return "com.android.externalstorage.documents" == uri.authority
}

private fun isDownloadsDocument(uri: Uri): Boolean {
    return "com.android.providers.downloads.documents" == uri.authority
}

private fun isMediaDocument(uri: Uri): Boolean {
    return "com.android.providers.media.documents" == uri.authority
}

private fun isGooglePhotosUri(uri: Uri): Boolean {
    return "com.google.android.apps.photos.content" == uri.authority
}

fun isWhatsAppFile(uri: Uri): Boolean {
    return "com.whatsapp.provider.media" == uri.authority
}

private fun isGoogleDriveUri(uri: Uri): Boolean {
    return "com.google.android.apps.docs.storage" == uri.authority || "com.google.android.apps.docs.storage.legacy" == uri.authority
}

fun Activity.getMediaImagePaths(data: Intent): ArrayList<String> {
    try {
        // When an Image is picked
        // Get the Image from data
        val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
        val imagesEncodedList = ArrayList<String>()
        var imageEncoded = ""

        if (data.clipData != null) {
            val mClipData = data.clipData!!
            val mArrayUri = ArrayList<Uri>()
            for (i in 0 until mClipData.itemCount) {
                val item = mClipData.getItemAt(i)
                val uri = item?.uri!!
                mArrayUri.add(uri)
                // Get the cursor
                val cursor = contentResolver.query(uri, filePathColumn, null, null, null)
                // Move to first row
                cursor?.moveToFirst()
                val columnIndex = cursor?.getColumnIndex(filePathColumn[0]) ?: 0
                imageEncoded = cursor?.getString(columnIndex) ?: ""
                imagesEncodedList.add(imageEncoded)
                cursor?.close()
            }
            Log.v("LOG_TAG", "Selected Images" + mArrayUri.size)
        }


        return imagesEncodedList

    } catch (e: java.lang.Exception) {
        Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG)
            .show()

        return arrayListOf()
    }
}