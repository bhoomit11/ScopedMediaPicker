package com.media.scopemediapicker

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Parcelable
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.media.scopemediapicker.model.FileData
import com.media.scopemediapicker.utils.*
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

class ScopedMediaPicker(
    val activity: AppCompatActivity?,
    val fragment: Fragment? = null,
    val requiresCrop: Boolean = false,
    val allowMultipleImages: Boolean = false,
    val aspectRatioX:Float = 1f,
    val aspectRatioY:Float  = 1f
) {


    companion object {
        const val REQ_CAPTURE = 1101
        const val RES_IMAGE = 1102
        const val RES_MULTI_IMAGE = 1103
        const val RES_VIDEO = 1104
        const val IMAGE_CROP_REQUEST_CODE = 1234
        const val RES_FILE = 1105
        const val RES_MULTI_FILE = 1106

        const val MEDIA_TYPE_IMAGE = 1
        const val MEDIA_TYPE_VIDEO = 2

        // Mime Types
        val PDF = Pair("PDF", arrayOf("application/pdf"))
        val DOC = Pair("DOC/DOCX", arrayOf("application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        val PPT = Pair("PPT/PPTX", arrayOf("application/vnd.ms-powerpoint,application/vnd.openxmlformats-officedocument.presentationml.presentation"))
        val XLS = Pair("XLS/XLSX", arrayOf("application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        val TXT = Pair("TEXT", arrayOf("text/plain"))
        val ZIP = Pair("ZIP", arrayOf("application/zip"))

    }

    private var imageUri: Uri? = null
    private var imgPath: String = ""
    var mediaType: Int = MEDIA_TYPE_IMAGE
    var fileTypes: ArrayList<Pair<String, Array<String>>> = arrayListOf()
    lateinit var onMediaChoose: (pathList: ArrayList<String>, type: Int) -> Unit
    lateinit var onFileChoose: (fileList: ArrayList<FileData>) -> Unit

    val filePaths by lazy {
        FilePaths(fragment?.requireContext() ?: activity as Context)
    }

    private val permissions = arrayOf(Manifest.permission.CAMERA)

    fun startMediaPicker(mediaType: Int, onMediaChooseMultiple: (pathList: ArrayList<String>, type: Int) -> Unit) {
        this.mediaType = mediaType
        this.onMediaChoose = onMediaChooseMultiple
        if (isPermissionsAllowed(permissions)) {

            if (mediaType and MEDIA_TYPE_IMAGE == MEDIA_TYPE_IMAGE && mediaType and MEDIA_TYPE_VIDEO == MEDIA_TYPE_VIDEO) {
                selectMediaDialog()
            } else {
                if (mediaType and MEDIA_TYPE_IMAGE == MEDIA_TYPE_IMAGE) {
                    chooseImage()
                }
                if (mediaType and MEDIA_TYPE_VIDEO == MEDIA_TYPE_VIDEO) {
                    chooseVideo()
                }

            }
        }
    }

    fun startFilePicker(fileTypes: ArrayList<Pair<String, Array<String>>> = arrayListOf(), onFileChoose: (fileList: ArrayList<FileData>) -> Unit) {
        this.fileTypes = fileTypes
        this.onFileChoose = onFileChoose
        chooseFile()
    }

    /*
     * Choose single or multiple files
     */
    private fun chooseFile() {
        val fragmentManager = fragment?.childFragmentManager ?: activity?.supportFragmentManager
        val activity = activity ?: fragment?.requireActivity() as Activity

        if (fileTypes.size == 1) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.type = "*/*"
            intent.putExtra(Intent.EXTRA_MIME_TYPES, fileTypes.first().second)
            intent.addFlags(intent.flags or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (allowMultipleImages) {
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            activity.startActivityForResult(intent, if (allowMultipleImages) RES_MULTI_FILE else RES_FILE)
        } else {
            BottomDialogSpinner.with(activity, fileTypes)
                .setMap { value: Pair<String, Array<String>> -> value.first }
                .setEnableSearch(false)
                .setTitle("Select File Type")
                .setOnValueSelectedCallback { model ->
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                    intent.type = "*/*"
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, model.second)
                    intent.addFlags(intent.flags or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    if (allowMultipleImages) {
                        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    }
                    activity.startActivityForResult(intent, if (allowMultipleImages) RES_MULTI_FILE else RES_FILE)
                }.build().show(fragmentManager!!)

        }

    }


    private fun selectMediaDialog() {
        if (activity != null || fragment != null) {
            BottomSheetDialogFragmentHelperView.with(
                R.layout.dialog_media_picker,
                isCancellable = true,
                isCancellableOnTouchOutSide = true
            ) { it, dialog ->

                val llImageCamera = it.findViewById<LinearLayout>(R.id.llImageCamera)
                val llVideoCamera = it.findViewById<LinearLayout>(R.id.llVideoCamera)

                llImageCamera.setOnClickListener {
                    chooseImage()
                    dialog.dismiss()
                }
                llVideoCamera.setOnClickListener {
                    chooseVideo()
                    dialog.dismiss()
                }

            }.show(
                activity?.supportFragmentManager ?: fragment!!.childFragmentManager,
                "filepicker"
            )
        } else {
            throw RuntimeException("It Seems activity is not set")
        }
    }

    private fun chooseImage() {
        activity?.startActivityForResult(getPickImageIntent(), if (allowMultipleImages) RES_MULTI_IMAGE else RES_IMAGE)
    }

    @SuppressWarnings("deprecation")
    private fun chooseVideo() {
        activity?.startActivityForResult(getPickVideoIntent(), RES_VIDEO)
    }

    private fun getPickVideoIntent(): Intent? {
        var chooserIntent: Intent? = null

        var intentList: MutableList<Intent> = ArrayList()

        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)

        val takeVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, setImageUri())

        intentList = addIntentsToList(intentList, pickIntent)
        intentList = addIntentsToList(intentList, takeVideoIntent)

        if (intentList.size > 0) {
            chooserIntent = Intent.createChooser(
                intentList.removeAt(intentList.size - 1),
                activity?.getString(R.string.select_capture_video)
            )
            chooserIntent?.putExtra(
                Intent.EXTRA_INITIAL_INTENTS,
                intentList.toTypedArray<Parcelable>()
            )
        }

        return chooserIntent
    }

    private fun getPickImageIntent(): Intent? {
        var chooserIntent: Intent? = null

        var intentList: MutableList<Intent> = ArrayList()

        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        if (allowMultipleImages) {
            pickIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }

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
        val resInfo = activity?.packageManager?.queryIntentActivities(intent, 0)
        for (resolveInfo in resInfo!!) {
            val packageName = resolveInfo.activityInfo.packageName
            val targetedIntent = Intent(intent)
            targetedIntent.setPackage(packageName)
            list.add(targetedIntent)
        }
        return list
    }

    /**
     * Set default URI for capture image
     */
    private fun setImageUri(): Uri? {
        val folder = File("${activity?.getExternalFilesDir(Environment.DIRECTORY_DCIM)}")
        if (!folder.exists()) {
            folder.mkdirs()
        }

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
        val activity = activity ?: fragment?.requireActivity() as Activity

        when (requestCode) {
            RES_IMAGE -> {
                if (resultCode == Activity.RESULT_OK) {
                    handleImageRequest(data)
                }
            }
            RES_MULTI_IMAGE -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        val imagePathList = activity.getMediaImagePaths(data)
                        imagePathList.let { onMediaChoose(it, MEDIA_TYPE_IMAGE) }
                    }
                }
            }
            RES_FILE -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        val docUri = data.data

                        activity.contentResolver.takePersistableUriPermission(docUri!!, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        val fileData = FileData()

                        val mimeType = activity.contentResolver.getType(docUri)
                        fileData.mimeType = mimeType
                        fileData.fileExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                        fileData.fileName = activity.getFileName(docUri)
                        fileData.fileUri = docUri
                        activity.contentResolver.openInputStream(docUri).use { inputStream ->
                            fileData.fileInputStream = inputStream
                        }
                        onFileChoose(arrayListOf(fileData))
                    }
                }
            }
            RES_MULTI_FILE -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (data?.clipData != null) {
                        val mClipData = data.clipData!!
                        val list: ArrayList<FileData> = arrayListOf()
                        for (i in 0 until mClipData.itemCount) {
                            val item = mClipData.getItemAt(i)
                            val docUri = item?.uri
                            val fileData = FileData()

                            if (docUri != null) {
                                val mimeType = activity.contentResolver.getType(docUri)
                                fileData.mimeType = mimeType
                                fileData.fileExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                                fileData.fileName = activity.getFileName(docUri)

                                activity.contentResolver.takePersistableUriPermission(docUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                activity.contentResolver.openInputStream(docUri).use { inputStream ->
                                    fileData.fileInputStream = inputStream
                                }
                            }
                            fileData.fileUri = docUri

                            list.add(fileData)
                        }

                        onFileChoose(list)
                    }
                }
            }
            RES_VIDEO -> {
                if (resultCode == Activity.RESULT_OK) {
                    handleVideoRequest(data)
                }
            }
            IMAGE_CROP_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val resultUri = UCrop.getOutput(data)
                    val imagePath = resultUri?.path ?: ""

                    onMediaChoose(arrayListOf(imagePath), MEDIA_TYPE_IMAGE)
                }
            }
        }
    }

    /*
     * Handle the data URI once video is back to user
     */
    private fun handleVideoRequest(data: Intent?) {
        val exceptionHandler = CoroutineExceptionHandler { _, t ->
            t.printStackTrace()
        }

        var videoPath = ""
        GlobalScope.launch(Dispatchers.Main + exceptionHandler) {
            if (data?.data != null) {     //Photo from gallery
                val videoUri = data.data!!
                videoPath = activity?.getVideoPath(videoUri) ?: ""

                onMediaChoose(arrayListOf(videoPath), MEDIA_TYPE_VIDEO)
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
                queryImageUrl = imageUri?.path ?: ""
                compressedPath =
                    activity?.compressImageFile(queryImageUrl, false, imageUri!!) ?: ""
            } else {
                compressedPath = getScaledImagePath(activity!!, 1024, 1024, imgPath)
            }


            if (requiresCrop) {
                val destinationFile = File(
                    filePaths.getLocalDirectory(
                        type = TYPES.LOCAL_CACHE_DIRECTORY
                    )?.path + "/" + File(compressedPath).name
                )
                destinationFile.createNewFile()
                //Cropping

                val options = UCrop.Options()
                options.setAllowedGestures(
                    UCropActivity.SCALE,
                    UCropActivity.NONE,
                    UCropActivity.SCALE
                )
                options.setToolbarColor(
                    ContextCompat.getColor(
                        activity ?: fragment?.requireContext()!!,
                        R.color.crop_toolbar_color
                    )
                )
                options.setStatusBarColor(
                    ContextCompat.getColor(
                        activity ?: fragment?.requireContext()!!,
                        R.color.crop_statusbar_color
                    )
                )
                options.setHideBottomControls(true)

                if (activity != null) {
                    activity.startActivityForResult(
                        UCrop.of(
                            Uri.fromFile(File(compressedPath)),
                            Uri.fromFile(destinationFile)
                        ).withOptions(options).withAspectRatio(aspectRatioX, aspectRatioY).getIntent(activity), IMAGE_CROP_REQUEST_CODE
                    )
                } else {
                    fragment?.startActivityForResult(
                        UCrop.of(
                            Uri.fromFile(File(compressedPath)),
                            Uri.fromFile(destinationFile)
                        ).withOptions(options).withAspectRatio(
                            aspectRatioX,
                            aspectRatioY
                        ).getIntent(fragment.requireContext()), IMAGE_CROP_REQUEST_CODE
                    )
                }
            } else {
                onMediaChoose.invoke(arrayListOf(compressedPath), MEDIA_TYPE_IMAGE)
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
                    if (mediaType and MEDIA_TYPE_IMAGE == MEDIA_TYPE_IMAGE && mediaType and MEDIA_TYPE_VIDEO == MEDIA_TYPE_VIDEO) {
                        selectMediaDialog()
                    } else {
                        if (mediaType and MEDIA_TYPE_IMAGE == MEDIA_TYPE_IMAGE) {
                            chooseImage()
                        }
                        if (mediaType and MEDIA_TYPE_VIDEO == MEDIA_TYPE_VIDEO) {
                            chooseVideo()
                        }

                    }
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