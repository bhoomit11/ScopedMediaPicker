package com.media.scopemediapicker
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


/**
 * Utility function for decoding an image resource. The decoded bitmap will
 * be optimized for further scaling to the requested destination dimensions
 * and scaling logic.
 *
 * @param dstWidth
 * Width of destination area
 * @param dstHeight
 * Height of destination area
 * @param scalingLogic
 * Logic to use to avoid image stretching
 * @return Decoded bitmap
 */
fun decodeResource(
    filePath: String,
    dstWidth: Int,
    dstHeight: Int,
    scalingLogic: ScalingLogic
): Bitmap {

    val bmOptions = BitmapFactory.Options()
    bmOptions.inJustDecodeBounds = true
    BitmapFactory.decodeFile(filePath, bmOptions)

    bmOptions.inJustDecodeBounds = false
    bmOptions.inSampleSize = calculateSampleSize(
        bmOptions.outWidth,
        bmOptions.outHeight,
        dstWidth,
        dstHeight,
        scalingLogic
    )

    return BitmapFactory.decodeFile(filePath, bmOptions)
}

fun decodeFile(
    context: Context,
    uri: Uri,
    dstWidth: Int,
    dstHeight: Int,
    scalingLogic: ScalingLogic
): Bitmap? {
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    context.getBitmapFromUri(uri, options)
    options.inJustDecodeBounds = false

    options.inSampleSize = calculateSampleSize(
        options.outWidth,
        options.outHeight,
        dstWidth,
        dstHeight,
        scalingLogic
    )

    return context.getBitmapFromUri(uri, options)
}

/**
 * Utility function for creating a scaled version of an existing bitmap
 *
 * @param unscaledBitmap Bitmap to scale
 * @param dstWidth Wanted width of destination bitmap
 * @param dstHeight Wanted height of destination bitmap
 * @param scalingLogic Logic to use to avoid image stretching
 * @return New scaled bitmap object
 */
fun createScaledBitmap(
    unscaledBitmap: Bitmap, dstWidth: Int, dstHeight: Int,
    scalingLogic: ScalingLogic
): Bitmap {
    val srcRect = calculateSrcRect(
        unscaledBitmap.width, unscaledBitmap.height,
        dstWidth, dstHeight, scalingLogic
    )
    val dstRect = calculateDstRect(
        unscaledBitmap.width,
        unscaledBitmap.height,
        dstWidth,
        dstHeight,
        scalingLogic
    )
    val scaledBitmap =
        Bitmap.createBitmap(dstRect.width(), dstRect.height(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(scaledBitmap)
    canvas.drawBitmap(unscaledBitmap, srcRect, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))

    return scaledBitmap
}


fun getScaledImagePath(
    mContext: Context,
    imageUploadMaxWidth: Int,
    imageUploadMaxHeight: Int,
    path: String
): String {
    val previewBitmap = getResizeImage(
        mContext,
        imageUploadMaxWidth,
        imageUploadMaxHeight,
        ScalingLogic.FIT,
        false,
        path,
        null
    )
    val file = bitmapToFile(
        previewBitmap,
        mContext.cacheDir.path + "/" + +System.currentTimeMillis() + ".jpg"
    )
    return if (file != null) file.path else path
}


/**
 * to convert bitmap to file.
 */
fun bitmapToFile(bitmap: Bitmap?, dstPath: String): File? {
    return try {
        val file = File(dstPath)
        if (file.exists()) file.delete()
        file.createNewFile()
        val fOut: FileOutputStream
        fOut = FileOutputStream(file)
        bitmap!!.compress(Bitmap.CompressFormat.JPEG, 70, fOut)
        fOut.flush()
        fOut.close()
        file
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

}


/**
 * This method is used to resize image
 *
 * @param context
 * @param dstWidth
 * @param dstHeight
 * @param scalingLogic
 * @param rotationNeeded
 * @param currentPhotoPath
 * @return scaledBitmap
 */
fun getResizeImage(
    context: Context,
    dstWidth: Int,
    dstHeight: Int,
    scalingLogic: ScalingLogic,
    rotationNeeded: Boolean,
    currentPhotoPath: String,
    IMAGE_CAPTURE_URI: Uri?
): Bitmap? {
    var rotate = 0
    try {
        val imageFile = File(currentPhotoPath)

        val exif = ExifInterface(imageFile.absolutePath)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.STREAM_TYPE_FULL_IMAGE_DATA)

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_270 -> rotate = 270
            ExifInterface.ORIENTATION_ROTATE_180 -> rotate = 180
            ExifInterface.ORIENTATION_ROTATE_90 -> rotate = 90
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }


    try {
        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true
        BitmapFactory.decodeFile(currentPhotoPath, bmOptions)
        bmOptions.inJustDecodeBounds = false
        return if (bmOptions.outWidth < dstWidth && bmOptions.outHeight < dstHeight) {
            val bitmap: Bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions)
            getRotatedBitmap(
                setSelectedImage(
                    bitmap,
                    context,
                    currentPhotoPath,
                    IMAGE_CAPTURE_URI!!
                ), rotate
            )
        } else {
            var unscaledBitmap = decodeResource(currentPhotoPath, dstWidth, dstHeight, scalingLogic)
            val matrix = Matrix()
            if (rotationNeeded) {
                matrix.setRotate(
                    getCameraPhotoOrientation(
                        context,
                        Uri.fromFile(File(currentPhotoPath)),
                        currentPhotoPath
                    ).toFloat()
                )
                unscaledBitmap = Bitmap.createBitmap(
                    unscaledBitmap,
                    0,
                    0,
                    unscaledBitmap.width,
                    unscaledBitmap.height,
                    matrix,
                    false
                )
            }
            val scaledBitmap = createScaledBitmap(unscaledBitmap, dstWidth, dstHeight, scalingLogic)
            unscaledBitmap.recycle()
            getRotatedBitmap(scaledBitmap, rotate)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

/**
 * This method is used get orientation of camera photo
 *
 * @param context
 * @param imageUri  This parameter is Uri type
 * @param imagePath This parameter is String type
 * @return rotate
 */
private fun getCameraPhotoOrientation(context: Context, imageUri: Uri?, imagePath: String): Int {
    var rotate = 0
    try {
        try {
            if (imageUri != null) context.contentResolver.notifyChange(imageUri, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val imageFile = File(imagePath)
        val exif = ExifInterface(imageFile.absolutePath)
        val orientation =
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_270 -> rotate = 270
            ExifInterface.ORIENTATION_ROTATE_180 -> rotate = 180
            ExifInterface.ORIENTATION_ROTATE_90 -> rotate = 90
            ExifInterface.ORIENTATION_NORMAL -> rotate = 0
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return rotate
}


fun getRotatedBitmap(bitmap: Bitmap, rotate: Int): Bitmap {
    return if (rotate == 0) {
        bitmap
    } else {
        val mat = Matrix()
        mat.postRotate(rotate.toFloat())
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, mat, true)
    }
}


fun setSelectedImage(
    orignalBitmap: Bitmap,
    context: Context,
    imagePath: String,
    IMAGE_CAPTURE_URI: Uri
): Bitmap {
    try {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (manufacturer.equals("samsung", ignoreCase = true) || model.equals(
                "samsung",
                ignoreCase = true
            )
        ) {
            rotateBitmap(context, orignalBitmap, imagePath, IMAGE_CAPTURE_URI)
        } else {
            orignalBitmap
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return orignalBitmap
    }

}


fun rotateBitmap(context: Context, bit: Bitmap, imagePath: String, IMAGE_CAPTURE_URI: Uri): Bitmap {

    val rotation = getCameraPhotoOrientation(context, IMAGE_CAPTURE_URI, imagePath)
    val matrix = Matrix()
    matrix.postRotate(rotation.toFloat())
    return Bitmap.createBitmap(bit, 0, 0, bit.width, bit.height, matrix, true)
}


/**
 * ScalingLogic defines how scaling should be carried out if source and
 * destination image has different aspect ratio.
 *
 * CROP: Scales the image the minimum amount while making sure that at least
 * one of the two dimensions fit inside the requested destination area.
 * Parts of the source image will be cropped to realize this.
 *
 * FIT: Scales the image the minimum amount while making sure both
 * dimensions fit inside the requested destination area. The resulting
 * destination dimensions might be adjusted to a smaller size than
 * requested.
 */
enum class ScalingLogic {
    CROP, FIT
}

/**
 * Calculate optimal down-sampling factor given the dimensions of a source
 * image, the dimensions of a destination area and a scaling logic.
 *
 * @param srcWidth Width of source image
 * @param srcHeight Height of source image
 * @param dstWidth Width of destination area
 * @param dstHeight Height of destination area
 * @param scalingLogic Logic to use to avoid image stretching
 * @return Optimal down scaling sample size for decoding
 */
fun calculateSampleSize(
    srcWidth: Int, srcHeight: Int, dstWidth: Int, dstHeight: Int,
    scalingLogic: ScalingLogic
): Int {
    if (scalingLogic == ScalingLogic.FIT) {
        val srcAspect = srcWidth.toFloat() / srcHeight.toFloat()
        val dstAspect = dstWidth.toFloat() / dstHeight.toFloat()

        return if (srcAspect > dstAspect) {
            srcWidth / dstWidth
        } else {
            srcHeight / dstHeight
        }
    } else {
        val srcAspect = srcWidth.toFloat() / srcHeight.toFloat()
        val dstAspect = dstWidth.toFloat() / dstHeight.toFloat()

        return if (srcAspect > dstAspect) {
            srcHeight / dstHeight
        } else {
            srcWidth / dstWidth
        }
    }
}

/**
 * Calculates source rectangle for scaling bitmap
 *
 * @param srcWidth Width of source image
 * @param srcHeight Height of source image
 * @param dstWidth Width of destination area
 * @param dstHeight Height of destination area
 * @param scalingLogic Logic to use to avoid image stretching
 * @return Optimal source rectangle
 */
fun calculateSrcRect(
    srcWidth: Int, srcHeight: Int, dstWidth: Int, dstHeight: Int,
    scalingLogic: ScalingLogic
): Rect {
    if (scalingLogic == ScalingLogic.CROP) {
        val srcAspect = srcWidth.toFloat() / srcHeight.toFloat()
        val dstAspect = dstWidth.toFloat() / dstHeight.toFloat()

        return if (srcAspect > dstAspect) {
            val srcRectWidth = (srcHeight * dstAspect).toInt()
            val srcRectLeft = (srcWidth - srcRectWidth) / 2
            Rect(srcRectLeft, 0, srcRectLeft + srcRectWidth, srcHeight)
        } else {
            val srcRectHeight = (srcWidth / dstAspect).toInt()
            val scrRectTop = (srcHeight - srcRectHeight) / 2
            Rect(0, scrRectTop, srcWidth, scrRectTop + srcRectHeight)
        }
    } else {
        return Rect(0, 0, srcWidth, srcHeight)
    }
}

/**
 * Calculates destination rectangle for scaling bitmap
 *
 * @param srcWidth Width of source image
 * @param srcHeight Height of source image
 * @param dstWidth Width of destination area
 * @param dstHeight Height of destination area
 * @param scalingLogic Logic to use to avoid image stretching
 * @return Optimal destination rectangle
 */
fun calculateDstRect(
    srcWidth: Int, srcHeight: Int, dstWidth: Int, dstHeight: Int,
    scalingLogic: ScalingLogic
): Rect {
    return if (scalingLogic == ScalingLogic.FIT) {
        val srcAspect = srcWidth.toFloat() / srcHeight.toFloat()
        val dstAspect = dstWidth.toFloat() / dstHeight.toFloat()

        if (srcAspect > dstAspect) {
            Rect(0, 0, dstWidth, (dstWidth / srcAspect).toInt())
        } else {
            Rect(0, 0, (dstHeight * srcAspect).toInt(), dstHeight)
        }
    } else {
        Rect(0, 0, dstWidth, dstHeight)
    }
}

fun getImageQualityPercent(file: File): Int {
    val sizeInBytes = file.length()
    val sizeInKB = sizeInBytes / 1024
    val sizeInMB = sizeInKB / 1024

    return when {
        sizeInMB <= 1 -> 80
        sizeInMB <= 2 -> 60
        else -> 40
    }
}

fun Context.getImageHgtWdt(uri: Uri): Pair<Int, Int> {
    val opt = BitmapFactory.Options()

    /* by setting this field as true, the actual bitmap pixels are not loaded in the memory. Just the bounds are loaded.
    If you try the use the bitmap here, you will get null.*/
    opt.inJustDecodeBounds = true
    val bm = getBitmapFromUri(uri, opt)

    var actualHgt = (opt.outHeight).toFloat()
    var actualWdt = (opt.outWidth).toFloat()

    /*val maxHeight = 816.0f
    val maxWidth = 612.0f*/
    val maxHeight = 720f
    val maxWidth = 1280f
    var imgRatio = actualWdt / actualHgt
    val maxRatio = maxWidth / maxHeight

//    width and height values are set maintaining the aspect ratio of the image
    if (actualHgt > maxHeight || actualWdt > maxWidth) {
        when {
            imgRatio < maxRatio -> {
                imgRatio = maxHeight / actualHgt
                actualWdt = (imgRatio * actualWdt)
                actualHgt = maxHeight
            }
            imgRatio > maxRatio -> {
                imgRatio = maxWidth / actualWdt
                actualHgt = (imgRatio * actualHgt)
                actualWdt = maxWidth
            }
            else -> {
                actualHgt = maxHeight
                actualWdt = maxWidth
            }
        }
    }

    return Pair(actualHgt.toInt(), actualWdt.toInt())
}