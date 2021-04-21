package com.media.scopemediapicker

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Parcelable
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

public class ScopedImagePicker(
    val activity: AppCompatActivity? = null,
    val fragment: Fragment? = null,
    val requiresCompress: Boolean = true,
    val requiresCrop: Boolean = true,
    val allowMultipleImages: Boolean = false
) {


    companion object {
        const val REQ_CAPTURE = 1001
        const val RES_IMAGE = 1002
    }

    private var imageUri: Uri? = null
    private var imgPath: String = ""
    var onMediaChoose: (path: String) -> Unit = { path -> }

    private val permissions = arrayOf(Manifest.permission.CAMERA)

    fun start(onMediaChoose: (path: String) -> Unit) {
        this.onMediaChoose = onMediaChoose
        if (isPermissionsAllowed(permissions)) {
            chooseImage()
        }
    }

    private fun chooseImage() {
        activity?.startActivityForResult(getPickImageIntent(), RES_IMAGE)
    }


    private fun getPickImageIntent(): Intent? {
        var chooserIntent: Intent? = null

        var intentList: MutableList<Intent> = ArrayList()

        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

        val takePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, setImageUri())

        intentList = addIntentsToList(intentList, pickIntent)
        intentList = addIntentsToList(intentList, takePhotoIntent)

        if (intentList.size > 0) {
            chooserIntent = Intent.createChooser(
                intentList.removeAt(intentList.size - 1),
                activity?.getString(R.string.select_capture_image)
            )
            chooserIntent?.putExtra(
                Intent.EXTRA_INITIAL_INTENTS,
                intentList.toTypedArray<Parcelable>()
            )
        }

        return chooserIntent
    }

    /**
     * Add Intents to Intent list
     */
    private fun addIntentsToList(
        list: MutableList<Intent>,
        intent: Intent
    ): MutableList<Intent> {
        val resInfo = activity?.packageManager?.queryIntentActivities(intent, 0) ?: arrayListOf()
        for (resolveInfo in resInfo) {
            val packageName = resolveInfo.activityInfo.packageName
            val targetedIntent = Intent(intent)
            targetedIntent.setPackage(packageName)
            list.add(targetedIntent)
        }
        return list
    }

    /** Set default URI for capture image
     *
     */
    private fun setImageUri(): Uri? {
        val folder = File("${activity?.getExternalFilesDir(Environment.DIRECTORY_DCIM)}")
        folder.mkdirs()

        val file =
            File(folder, "${activity?.getApplicationName()}_${System.currentTimeMillis()}.png")

        if (file.exists())
            file.delete()
        file.createNewFile()
        imageUri = FileProvider.getUriForFile(
            activity!!,
            activity.applicationContext.packageName + activity.getString(R.string.file_provider_name),
            file
        )
        imgPath = file.absolutePath
        return imageUri
    }


    /**
     * Check if scoped permission is allowed or not
     */
    private fun isPermissionsAllowed(
        permissions: Array<String>
    ): Boolean {
        var isGranted = true
        if (activity != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                for (permission in permissions) {
                    isGranted = ContextCompat.checkSelfPermission(
                        activity,
                        permission
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!isGranted)
                        break
                }
            }
            if (!isGranted) {
                requestRequiredPermissions(permissions)
            }
        }
        return isGranted
    }

    private fun requestRequiredPermissions(permissions: Array<String>) {
        val pendingPermissions: ArrayList<String> = ArrayList()
        permissions.forEachIndexed { index, permission ->
            if (ContextCompat.checkSelfPermission(
                    activity!!,
                    permission
                ) == PackageManager.PERMISSION_DENIED
            )
                pendingPermissions.add(permission)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val array = arrayOfNulls<String>(pendingPermissions.size)
            pendingPermissions.toArray(array)
            activity?.requestPermissions(array, REQ_CAPTURE)
        }
    }


    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RES_IMAGE -> {
                if (resultCode == Activity.RESULT_OK) {
                    handleImageRequest(data)
                }
            }
        }
    }

    /*
     * Handle the data URI once image is back to user
     */
    private fun handleImageRequest(data: Intent?) {
        val exceptionHandler = CoroutineExceptionHandler { _, t ->
            t.printStackTrace()
        }
        var queryImageUrl: String
        var compressedPath: String

            GlobalScope.launch(Dispatchers.Main + exceptionHandler) {
                if (data?.data != null) {     //Photo from gallery
                    val imageUri = data.data
                    queryImageUrl = imageUri?.path?:""
                    compressedPath =
                        activity?.compressImageFile(queryImageUrl, false, imageUri!!) ?: ""
                } else {
                    queryImageUrl = imgPath
                    compressedPath = imgPath
                    activity?.compressImageFile(compressedPath, uri = imageUri!!) ?: ""
                }

                if (requiresCompress) {
                    onMediaChoose.invoke(compressedPath)
                } else {
                    onMediaChoose.invoke(queryImageUrl )
                }

            }


    }


    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQ_CAPTURE -> {
                if (isAllPermissionsGranted(grantResults)) {
                    chooseImage()
                } else {
                    Toast.makeText(
                        activity,
                        "Permission not granted",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun isAllPermissionsGranted(grantResults: IntArray): Boolean {
        var isGranted = true
        for (grantResult in grantResults) {
            isGranted = grantResult == PackageManager.PERMISSION_GRANTED
            if (!isGranted)
                break
        }
        return isGranted
    }
}