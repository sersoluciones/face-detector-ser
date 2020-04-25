package co.com.sersoluciones.facedetectorser.utilities

import android.app.Activity
import android.content.ContentUris
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


private const val tag = "FileUtils"

fun Activity.compressImageFile(uri: Uri): Bitmap? {
    var scaledBitmap: Bitmap? = null
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
    return scaledBitmap
}

suspend fun Activity.getRealURI(
        uri: Uri
): Uri? {
    return withContext(Dispatchers.IO) {

        var bitmap: Bitmap? = null

        try {
            bitmap = compressImageFile(uri)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (bitmap == null) {
            bitmap = getBitmapFromUri(applicationContext, uri)
        }

        return@withContext getRealUriImage(applicationContext, bitmap!!)

    }
}

fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    val readOnlyMode = "r"
    return context.contentResolver?.openFileDescriptor(uri, readOnlyMode).use { pfd ->
        pfd?.let {
            val fileDescriptor: FileDescriptor = it.fileDescriptor
            BitmapFactory.decodeFileDescriptor(fileDescriptor)
        }
    }
}

fun getRealUriImage(context: Context, bitmap: Bitmap, override: Boolean = true): Uri {

    // Add a media item that other apps shouldn't see until the item is
    // fully written to the media store.
    val resolver = context.applicationContext.contentResolver
    var titleTempFile = "temp_pick_image.jpg"

    val now = System.currentTimeMillis() / 1000
    if (!override)
        titleTempFile = now.toString() + "_temp_pick_image.jpg"

    // Find all image files on the primary external storage device.
    // On API <= 28, use VOLUME_EXTERNAL instead.
    val imagesCollection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME)
    val query = context.contentResolver.query(imagesCollection, projection, null, null, null)

    var uriTempImage: Uri? = null

    query?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val name = cursor.getString(nameColumn)
            if (name == titleTempFile) {
                uriTempImage = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                )
                break
            }
        }
    }

    if (uriTempImage != null)
        Log.d(TAG, "uri from image in gallery uriOldImage $uriTempImage")


    val imageDetails = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, titleTempFile)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    if (uriTempImage == null || !override)
        uriTempImage = resolver.insert(imagesCollection, imageDetails)

    resolver.openFileDescriptor(uriTempImage!!, "w", null).use { pfd ->
        // Write data into the pending file.

        pfd?.let {
            val fileDescriptor: FileDescriptor = it.fileDescriptor
            val fileOutputStream = FileOutputStream(fileDescriptor)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStream)
            fileOutputStream.close()
        }
    }

    // Now that we're finished, release the "pending" status, and allow other apps
    // to use this image
    imageDetails.clear()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        imageDetails.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uriTempImage!!, imageDetails, null, null)
    }

    Log.d(TAG, "uri from image in gallery $uriTempImage")
    return uriTempImage!!
}


fun saveImageInGallery(context: Context, uri: Uri, quality: Int): Uri? {
    val readOnlyMode = "r"
    val bitmap = context.contentResolver?.openFileDescriptor(uri, readOnlyMode).use { pfd ->
        pfd?.let {
            val fileDescriptor: FileDescriptor = it.fileDescriptor
            BitmapFactory.decodeFileDescriptor(fileDescriptor)
        }
    }

    val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "TempSER")

    if (!dir.exists()) {
        dir.mkdirs()
    }

    val now = System.currentTimeMillis() / 1000
    val imageFile = File(dir.absolutePath
            + File.separator
            + now.toString()
            + "_image.jpg")

    val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    val resolver = context.contentResolver
    val imageDetails = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, imageFile.name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        imageFile.createNewFile()
        val imageUri = Uri.fromFile(imageFile)
        // save image into gallery
        writeBitmapToUri(context.applicationContext, bitmap!!, imageUri, Bitmap.CompressFormat.JPEG, quality)
        imageDetails.put(MediaStore.MediaColumns.DATA, imageFile.path)
        resolver.insert(collection, imageDetails)
        return imageUri
    }


    imageDetails.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
    imageDetails.put(MediaStore.MediaColumns.IS_PENDING, 1)
    imageDetails.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + "TempSER")


    val pendingUri = resolver.insert(collection, imageDetails)

    resolver.openFileDescriptor(pendingUri!!, "w", null).use { pfd ->
        // Write data into the pending file.
        pfd?.let {
            val fileDescriptor: FileDescriptor = it.fileDescriptor
            val fileOutputStream = FileOutputStream(fileDescriptor)
            bitmap!!.compress(Bitmap.CompressFormat.JPEG, quality, fileOutputStream)
            fileOutputStream.close()
        }
    }

    // Now that we're finished, release the "pending" status, and allow other apps
    // to use this image
    imageDetails.clear()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        imageDetails.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(pendingUri, imageDetails, null, null)
    }
    return pendingUri
}

suspend fun Activity.saveImageInGalery(uri: Uri, quality: Int): Uri? {

    return withContext(Dispatchers.IO) {
//        photo.delete()
        return@withContext saveImageInGallery(applicationContext, uri, quality)
    }
}


suspend fun Activity.storeInternalMemory(path: String,
                                         scaledBitmap: Bitmap,
                                         shouldOverride: Boolean = false): String {
    return withContext(Dispatchers.IO) {
        // Store to tmp file
        val mFolder = File("$filesDir/Images")
        if (!mFolder.exists()) {
            mFolder.mkdir()
        }

        val tmpFile = File(mFolder.absolutePath, "IMG_${getTimestampString()}.png")

        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(tmpFile)
            scaledBitmap.compress(
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

        scaledBitmap.recycle()
        return@withContext if (shouldOverride) path else compressedPath
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

fun getTimestampString(): String {
    val date = Calendar.getInstance()
    return SimpleDateFormat("yyyy MM dd hh mm ss", Locale.US).format(date.time).replace(" ", "")
}

/**
 * Write the given bitmap to the given uri using the given compression.
 */
@Throws(FileNotFoundException::class)
fun writeBitmapToUri(
        context: Context,
        bitmap: Bitmap,
        uri: Uri?,
        compressFormat: Bitmap.CompressFormat,
        compressQuality: Int) {
    var outputStream: OutputStream? = null
    try {
        outputStream = context.contentResolver.openOutputStream(uri!!)
        bitmap.compress(compressFormat, compressQuality, outputStream)
    } finally {
        if (outputStream != null) {
            try {
                outputStream.close()
            } catch (ignored: IOException) {
            }
        }
    }
}