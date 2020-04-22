package co.com.sersoluciones.facedetectorser

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.Bitmap.CompressFormat
import android.hardware.Camera
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.OnScaleGestureListener
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import co.com.sersoluciones.facedetectorser.camera.CameraSource
import co.com.sersoluciones.facedetectorser.camera.FaceGraphic
import co.com.sersoluciones.facedetectorser.databinding.FaceTrackerBinding
import co.com.sersoluciones.facedetectorser.fragments.SaveImageFragment
import co.com.sersoluciones.facedetectorser.serlibrary.PhotoSer
import co.com.sersoluciones.facedetectorser.serlibrary.PhotoSerOptions
import co.com.sersoluciones.facedetectorser.utilities.DebugLog
import co.com.sersoluciones.facedetectorser.utilities.RealPathUtil
import co.com.sersoluciones.facedetectorser.views.GraphicOverlay
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.vision.Detector.Detections
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.MultiProcessor
import com.google.android.gms.vision.Tracker
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector
import com.google.android.material.snackbar.Snackbar
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import java.io.*
import java.sql.Timestamp
import java.util.*

/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ /**
 * Activity for the face tracker app.  This app detects faces with the rear facing camera, and draws
 * overlay graphics to indicate the position, size, and ID of each face.
 * Created by Ser Soluciones SAS on 11/12/2017.isOperational
 * www.sersoluciones.com - contacto@sersoluciones.com
 */
class FaceTrackerActivity : AppCompatActivity(), CameraSource.ShutterCallback, CameraSource.PictureCallback {

    private var mCameraSource: CameraSource? = null
    private var matrix: Matrix? = null
    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var gestureDetector: GestureDetector? = null
    private var toggle = false
    private var isChecked = false
    private var shutterCallback: CameraSource.ShutterCallback? = null
    private var pictureCallback: CameraSource.PictureCallback? = null
    private var isDetectFace = false
    private var isTakePhoto = false
    private var mPhotoPath: String? = null
    private var fragment: Fragment? = null

    private var mOptions: PhotoSerOptions? = null
    private lateinit var binding: FaceTrackerBinding
    //==============================================================================================
    // Activity Methods
    //==============================================================================================
    /**
     * Initializes the UI and initiates the creation of a face detector.
     */
    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        // Optional: Hide the status bar at the top of the window
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        setContentView(R.layout.face_tracker)
        binding = DataBindingUtil.setContentView(
                this, R.layout.face_tracker
        )
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        val bundle = intent.getBundleExtra(PhotoSer.PHOTO_SER_EXTRA_BUNDLE)
        mOptions = bundle.getParcelable(PhotoSer.PHOTO_SER_EXTRA_OPTIONS)
        toggle = false
        isChecked = false

        shutterCallback = this
        pictureCallback = this
        isTakePhoto = false
        mPhotoPath = ""
        isDetectFace = mOptions!!.isDetectFace
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)

        binding.fabCamera.setOnClickListener { takePhoto() }

        binding.fabLight.setOnClickListener {
            isChecked = !isChecked
            if (isChecked) binding.fabLight.setImageResource(R.drawable.ic_flash_on_white_36dp) else binding.fabLight.setImageResource(R.drawable.ic_flash_off_white_36dp)
            flashOnButton(isChecked)
        }

        binding.fabSwitchCamera.setOnClickListener {
            binding.preview.stop()
            mCameraSource!!.release()
            toggle = !toggle
            if (toggle) binding.fabLight.setEnabled(false) else binding.fabLight.setEnabled(true)
            createCameraSource(toggle)
            startCameraSource()
            isDetectFace = false
        }
        val widthPixels = metrics.widthPixels
        val heightPixels = metrics.heightPixels
        DebugLog.logW(String.format("FaceTrackerActivity width: %s, height: %s", widthPixels, heightPixels))
        matrix = Matrix()
        gestureDetector = GestureDetector(this, CaptureGestureListener())
        scaleGestureDetector = ScaleGestureDetector(this, ScaleListener())
        Snackbar.make(binding.faceOverlay, "Toque para autoenfocar. Pellizque/estire para zoom",
                Snackbar.LENGTH_SHORT)
                .show()
        binding.fabAttach.setOnClickListener(View.OnClickListener { attachImageFromGalery() })
    }

    private fun attachImageFromGalery() {
        try {
            val galleryIntent = Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            this.startActivityForResult(galleryIntent, REQUEST_IMAGE_SELECTOR)
        } catch (e: Exception) {
            e.printStackTrace()
            val getIntent = Intent(Intent.ACTION_GET_CONTENT)
            getIntent.type = "image/*"
            val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickIntent.type = "image/*"
            val chooserIntent = Intent.createChooser(getIntent, "Select Image")
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(pickIntent))
            startActivityForResult(chooserIntent, 200)
        }
    }

    private fun addFragment(fragment: Fragment) {
        binding.preview.stop()
        if (mCameraSource != null) {
            mCameraSource!!.release()
            mCameraSource = null
        }
        binding.preview.visibility = View.GONE
        binding.fabSwitchCamera.visibility = View.GONE
        binding.fabLight.visibility = View.GONE
        binding.fabCamera.visibility = View.GONE
        binding.fabAttach.visibility = View.GONE
        this.fragment = fragment
        binding.container.visibility = View.VISIBLE
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(R.id.container, fragment)
        fragmentTransaction.commit()
    }

    fun removeFragment() {
        if (mCameraSource == null) {
            createCameraSource(toggle)
            startCameraSource()
        }
        binding.preview.visibility = View.VISIBLE
        binding.fabSwitchCamera.visibility = View.VISIBLE
        binding.fabLight.visibility = View.VISIBLE
        binding.fabCamera.visibility = View.VISIBLE
        binding.fabAttach.visibility = View.VISIBLE
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.remove(fragment!!)
        fragmentTransaction.commit()
        binding.container.visibility = View.GONE
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private fun showProgress(show: Boolean) {

        val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime)
        binding.frameProgress.visibility = if (show) View.VISIBLE else View.GONE
        binding.frameProgress.animate().setDuration(shortAnimTime.toLong()).alpha(
                if (show) 1f else 0f).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                binding.frameProgress.visibility = if (show) View.VISIBLE else View.GONE
            }
        })
        binding.fabAttach.isEnabled = !show
        binding.fabCamera.isEnabled = !show
    }

    private fun takePhoto() {
        if (isTakePhoto || mCameraSource == null) return
        if (!mOptions!!.isDetectFace || !isDetectFace) {
            isTakePhoto = true
            mCameraSource!!.takePicture(shutterCallback, pictureCallback)
            showProgress(true)
        } else {
            isTakePhoto = true
            Handler().postDelayed({
                if (mCameraSource != null) {
                    mCameraSource!!.takePicture(shutterCallback, pictureCallback)
                    showProgress(true)
                }
            }, 1000)
        }
    }

    fun returnURIImage(path: String?) {
        mPhotoPath = path
        if (mOptions!!.isSaveGalery) {
            SaveImageInGaleryAsyncTask().execute(mPhotoPath)
        } else {
            val intent = Intent()
            intent.putExtra(PATH_IMAGE_KEY, mPhotoPath)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    override fun onPictureTaken(bytes: ByteArray) {
        if (binding.preview != null) binding.preview!!.stop()

        //decodeBytes(bytes);
        // showProgress(false);
        val rotation = mCameraSource!!.rotation
        try {
            // convert byte array into bitmap
            val loadedImage: Bitmap
            val scaledBitmap: Bitmap
            loadedImage = BitmapFactory.decodeByteArray(bytes, 0,
                    bytes.size)
            scaledBitmap = if (rotation > 0) {
                val rotateMatrix = Matrix()
                rotateMatrix.postRotate(rotation.toFloat())
                // Scale down to the output size
                Bitmap.createBitmap(loadedImage, 0, 0,
                        loadedImage.width, loadedImage.height,
                        rotateMatrix, false)
            } else {
                loadedImage
            }
            SaveImageAsyncTask().execute(scaledBitmap, loadedImage)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun launchInstance(outputFileUri: Uri?) {
        if (!mOptions!!.isCrop) {
            addFragment(SaveImageFragment.newInstance(outputFileUri))
            return
        }
        if (mOptions!!.isDetectFace || !isDetectFace) {
            CropImage.activity(outputFileUri)
                    .setMinCropResultSize(100, 100) //.setRequestedSize(500, 500, CropImageView.RequestSizeOptions.RESIZE_INSIDE)
                    .setBorderLineColor(Color.BLUE)
                    .setBorderCornerColor(Color.GREEN)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setFixAspectRatio(mOptions!!.isFixAspectRatio)
                    .start(this)
        } else {
            CropImage.activity(outputFileUri)
                    .setMinCropResultSize(100, 100) //.setRequestedSize(500, 500, CropImageView.RequestSizeOptions.RESIZE_INSIDE)
                    .setBorderLineColor(Color.BLUE)
                    .setBorderCornerColor(Color.GREEN)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setFixAspectRatio(mOptions!!.isFixAspectRatio)
                    .start(this)
        }
    }

    @SuppressLint("StaticFieldLeak")
    inner class SaveImageAsyncTask : AsyncTask<Bitmap?, Void?, Uri?>() {

        override fun onPostExecute(outputFileUri: Uri?) {
            super.onPostExecute(outputFileUri)
            launchInstance(outputFileUri)
            showProgress(false)
        }

        override fun doInBackground(vararg params: Bitmap?): Uri? {

            val outputFileUri = writeTempStateStoreBitmap(this@FaceTrackerActivity, params[0]!!, null)
            isTakePhoto = false
            isDetectFace = false
            params[1]!!.recycle()
            return outputFileUri
        }
    }

    inner class SaveImageInGaleryAsyncTask : AsyncTask<String?, Void?, String?>() {

        override fun onPostExecute(photoPath: String?) {
            super.onPostExecute(photoPath)
            if (photoPath.isNullOrEmpty()) setResult(Activity.RESULT_CANCELED, intent)
            else {
                mPhotoPath = photoPath
                val intent = Intent()
                intent.putExtra(PATH_IMAGE_KEY, mPhotoPath)
                setResult(Activity.RESULT_OK, intent)
            }
            showProgress(false)
            finish()
        }

        override fun doInBackground(vararg params: String?): String? {
            var photoPath: String? = ""
            try {
                photoPath = saveImageInGalery(params[0]!!)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return photoPath
        }
    }

    @Throws(IOException::class)
    private fun saveImageInGalery(pathImage: String): String? {
        val photo = File(pathImage)
        val bitmap = BitmapFactory.decodeFile(pathImage)
        val imageFile: File
        var success = true
        val dir: File

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "FaceDetectorSER")
            // this.getExternalFilesDir(Environment.DIRECTORY_PICTURES + File.separator.toString() + "FaceDetectorSER")
        } else {
            dir = File(
                    Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES), "FaceDetectorSER")
        }

        if (!dir.exists()) {
            success = dir.mkdirs()
        }

        if (success) {
            val date = Date()
            imageFile = File(dir!!.absolutePath
                    + File.separator
                    + Timestamp(date.time).toString()
                    + "photo.jpg")
            imageFile.createNewFile()
        } else {
            DebugLog.logE("Image Not saved")
            return null
        }
        // save image into gallery
        val ostream = ByteArrayOutputStream()
        bitmap.compress(CompressFormat.JPEG, mOptions!!.quality, ostream)
        val fout = FileOutputStream(imageFile)
        fout.write(ostream.toByteArray())
        fout.close()

        mPhotoPath = imageFile.path

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, imageFile.name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            }
            put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
        }
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)


        photo.delete()
        return mPhotoPath
    }

    private fun decodeBytes(data: ByteArray) {
        val mCamera = mCameraSource!!.camera
        if (mCamera != null) {
            val parameters = mCamera.parameters
            val width = parameters.previewSize.width
            val height = parameters.previewSize.height
            val yuv = YuvImage(data, parameters.previewFormat, width, height, null)
            val out = ByteArrayOutputStream()
            yuv.compressToJpeg(Rect(0, 0, width, height), mOptions!!.quality, out)
            val bytes = out.toByteArray()
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }

    /**
     * Write given bitmap to a temp file. If file already exists no-op as we already saved the file in
     * this session. Uses JPEG 95% compression.
     *
     * @param uri the uri to write the bitmap to, if null
     * @return the uri where the image was saved in, either the given uri or new pointing to temp
     * file.
     */
    private fun writeTempStateStoreBitmap(context: Context, bitmap: Bitmap, uri: Uri?): Uri? {
        var uri = uri
        return try {
            var needSave = true
            if (uri == null) {
                uri = Uri.fromFile(
                        File.createTempFile("aic_state_store_temp", ".jpg", context.cacheDir))
            } else if (File(uri.path).exists()) {
                needSave = false
            }
            if (needSave) {
                writeBitmapToUri(context, bitmap, uri, CompressFormat.JPEG, mOptions!!.quality)
            }
            uri
        } catch (e: Exception) {
            Log.w("AIC", "Failed to write bitmap to temp file for image-cropper save instance state", e)
            null
        }
    }

    /**
     * Write the given bitmap to the given uri using the given compression.
     */
    @Throws(FileNotFoundException::class)
    private fun writeBitmapToUri(
            context: Context,
            bitmap: Bitmap,
            uri: Uri?,
            compressFormat: CompressFormat,
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

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Get the preview size
        //widthPixels = binding.preview.getMeasuredWidth();
        //heightPixels = binding.preview.getMeasuredHeight();
    }

    /**
     * Metodo para controlar los eventos de la actividad
     * scaleGestureDetector, gestureDetector
     *
     * @param e
     * @return boolean
     */
    override fun onTouchEvent(e: MotionEvent): Boolean {
        val b = scaleGestureDetector!!.onTouchEvent(e)
        val c = gestureDetector!!.onTouchEvent(e)
        return b || c || super.onTouchEvent(e)
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     */
    private fun createCameraSource(front: Boolean) {
        val context = applicationContext
        val detector = FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setLandmarkType(FaceDetector.NO_LANDMARKS)
                .setMode(FaceDetector.FAST_MODE)
                .build()
        detector.setProcessor(
                MultiProcessor.Builder(GraphicFaceTrackerFactory())
                        .build())
        isDetectFace = if (!detector.isOperational) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Log.w(TAG, "Face detector dependencies are not yet available.")
            false
        } else {
            DebugLog.logW("Face Detector dependencies loaded.")
            mOptions!!.isDetectFace
        }
        mCameraSource = if (!mOptions!!.isDetectFace || !isDetectFace) {
            CameraSource.Builder(context)
                    .setRequestedPreviewSize(640, 480)
                    .setFacing(if (front) CameraSource.CAMERA_FACING_FRONT else CameraSource.CAMERA_FACING_BACK)
                    .setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) //.setRequestedFps(30.0f)
                    .build()
        } else {
            CameraSource.Builder(context, detector)
                    .setRequestedPreviewSize(640, 480)
                    .setFacing(if (front) CameraSource.CAMERA_FACING_FRONT else CameraSource.CAMERA_FACING_BACK)
                    .setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) //.setRequestedFps(30.0f)
                    .build()
        }
    }

    /**
     * Restarts the camera.
     */
    override fun onResume() {
        super.onResume()
        DebugLog.logW("onResume")
        if (mCameraSource != null) startCameraSource()
    }

    /**
     * Stops the camera.
     */
    override fun onPause() {
        super.onPause()
        DebugLog.logW("onPause")
        if (binding.preview != null) binding.preview!!.stop()
    }

    override fun onStart() {
        super.onStart()
        DebugLog.logW("onStart")
        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        val rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (rc == PackageManager.PERMISSION_GRANTED && result == PackageManager.PERMISSION_GRANTED) {
            createCameraSource(toggle)
        } else {
            requestCameraPermission()
        }
        //        if (mCameraSource == null)
//            createCameraSource(toggle);
    }

    override fun onStop() {
        super.onStop()
        DebugLog.logW("onStop")
        if (mCameraSource != null) {
            mCameraSource!!.release()
            mCameraSource = null
        }
        isTakePhoto = false
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    override fun onDestroy() {
        super.onDestroy()
        if (mCameraSource != null) {
            mCameraSource!!.release()
        }
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private fun requestCameraPermission() {
        Log.w(TAG, "Permission is not granted. Requesting permission")
        val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.CAMERA) && !ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM)
            return
        }
        val thisActivity: Activity = this
        val listener = View.OnClickListener {
            ActivityCompat.requestPermissions(thisActivity, permissions,
                    RC_HANDLE_CAMERA_PERM)
        }
        Snackbar.make(binding.faceOverlay!!, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show()
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on [.requestPermissions].
     *
     *
     * **Note:** It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     *
     *
     * @param requestCode  The request code passed in [.requestPermissions].
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     * which is either [PackageManager.PERMISSION_GRANTED]
     * or [PackageManager.PERMISSION_DENIED]. Never null.
     * @see .requestPermissions
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: $requestCode")
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }
        val permissionsNotGranted = ArrayList<String>()
        var mPermissionGranted = true
        //Checking the request code of our request
        if (requestCode == RC_HANDLE_CAMERA_PERM) {
            var count = 0
            for (permission in permissions) {
                if (permission == Manifest.permission.WRITE_EXTERNAL_STORAGE || permission == Manifest.permission.CAMERA) {
                    if (grantResults[count] == PackageManager.PERMISSION_DENIED) {
                        DebugLog.log("permiso: $permission")
                        permissionsNotGranted.add(permission)
                        mPermissionGranted = false
                    }
                }
                count++
            }
            if (!mPermissionGranted) {
                val permisos = arrayOfNulls<String>(permissionsNotGranted.size)
                permissionsNotGranted.toArray(permisos)
                for (permiso in permisos) {
                    val showRationale = ActivityCompat.shouldShowRequestPermissionRationale(this@FaceTrackerActivity, permiso!!)
                    if (!showRationale) {
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        val uri = Uri.fromParts("package", this@FaceTrackerActivity.packageName, null)
                        intent.data = uri
                        this@FaceTrackerActivity.startActivity(intent)
                    } else {
                        /*Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

                        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                finish();
                            }
                        };

                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("Face Tracker sample")
                                .setMessage(R.string.no_camera_permission)
                                .setPositiveButton(R.string.ok, listener)
                                .show();*/
                        val snackbar = Snackbar.make(view, resources.getString(R.string.permission_camera_rationale),
                                Snackbar.LENGTH_INDEFINITE)
                        snackbar.setAction(resources.getString(R.string.no_camera_permission)) { ActivityCompat.requestPermissions(this@FaceTrackerActivity, permisos, RC_HANDLE_CAMERA_PERM) }
                        snackbar.show()
                    }
                }
            } else {
                createCameraSource(false)
            }
        }
    }

    private val view: View
        get() = findViewById(android.R.id.content)
    //==============================================================================================
    // Camera Source Preview
    //==============================================================================================
    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    @SuppressLint("MissingPermission")
    private fun startCameraSource() {

        // check that the device has play services available.
        val code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                applicationContext)
        if (code != ConnectionResult.SUCCESS) {
            val dlg = GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS)
            dlg.show()
        }
        if (mCameraSource != null) {
            try {
                //binding.preview.start(mCameraSource, binding.faceOverlay, heightPixels, widthPixels);
                binding.preview!!.start(mCameraSource, binding.faceOverlay)
            } catch (e: IOException) {
                Log.e(TAG, "Unable to start camera source.", e)
                mCameraSource!!.release()
                mCameraSource = null
            }
        }
    }

    /**
     * onTap is called to capture the oldest barcode currently detected and
     * return it to the caller.
     *
     * @param e - MotionEvent
     * @return true if the binding.preview is ending.
     */
    private fun onTap(e: MotionEvent): Boolean {
        if (binding.preview != null) {
            if (mCameraSource != null) {
                val focusRect = calculateTapArea(e.x, e.y, 1f)
                val meteringRect = calculateTapArea(e.x, e.y, 1.5f)
                DebugLog.log(String.format("posX: %s, posY: %s", e.x, e.y))
                mCameraSource!!.focusOnTouch(focusRect, meteringRect)
            }

            /*cameraRectGraphicOverlay.add(cameraRectGraphic);
            cameraRectGraphic.updateItem(calculateTapArea(e.getX(), e.getY(), 4.0f));

            MediaActionSound sound = new MediaActionSound();
            sound.play(MediaActionSound.FOCUS_COMPLETE);

            new Handler().postDelayed(new Runnable() {
                public void run() {
                    cameraRectGraphicOverlay.clear();
                }
            }, 2000);*/
        }
        return binding.preview != null
    }

    private fun calculateTapArea(x: Float, y: Float, coefficient: Float): Rect {
        val focusAreaSize = resources.getDimensionPixelSize(R.dimen.camera_focus_area_size)
        val areaSize = java.lang.Float.valueOf(focusAreaSize * coefficient).toInt()
        val left = clamp(x.toInt() - areaSize / 2, mCameraSource!!.previewSize.width - areaSize)
        val top = clamp(y.toInt() - areaSize / 2, mCameraSource!!.previewSize.height - areaSize)
        val rectF = RectF(left.toFloat(), top.toFloat(), (left + areaSize).toFloat(), (top + areaSize).toFloat())
        matrix!!.mapRect(rectF)
        return Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom))
    }

    private fun clamp(x: Int, max: Int): Int {
        if (x > max) {
            return max
        }
        return if (x < 0) {
            0
        } else x
    }

    override fun onShutter() {}
    private inner class CaptureGestureListener : SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            return onTap(e) || super.onSingleTapConfirmed(e)
        }
    }

    private inner class ScaleListener : OnScaleGestureListener {
        /**
         * Responds to scaling events for a gesture in progress.
         * Reported by pointer motion.
         *
         * @param detector The detector reporting the event - use this to
         * retrieve extended info about event state.
         * @return Whether or not the detector should consider this event
         * as handled. If an event was not handled, the detector
         * will continue to accumulate movement until an event is
         * handled. This can be useful if an application, for example,
         * only wants to update scaling factors if the change is
         * greater than 0.01.
         */
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            return false
        }

        /**
         * Responds to the beginning of a scaling gesture. Reported by
         * new pointers going down.
         *
         * @param detector The detector reporting the event - use this to
         * retrieve extended info about event state.
         * @return Whether or not the detector should continue recognizing
         * this gesture. For example, if a gesture is beginning
         * with a focal point outside of a region where it makes
         * sense, onScaleBegin() may return false to ignore the
         * rest of the gesture.
         */
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            return true
        }

        /**
         * Responds to the end of a scale gesture. Reported by existing
         * pointers going up.
         *
         *
         * Once a scale has ended, [ScaleGestureDetector.getFocusX]
         * and [ScaleGestureDetector.getFocusY] will return focal point
         * of the pointers remaining on the screen.
         *
         * @param detector The detector reporting the event - use this to
         * retrieve extended info about event state.
         */
        override fun onScaleEnd(detector: ScaleGestureDetector) {
            if (mCameraSource != null) mCameraSource!!.doZoom(detector.scaleFactor)
        }
    }

    private fun flashOnButton(flashmode: Boolean) {
        val camera = getCamera(mCameraSource!!)
        if (camera != null) {
            try {
                val param = camera.parameters
                param.flashMode = if (flashmode) Camera.Parameters.FLASH_MODE_TORCH else Camera.Parameters.FLASH_MODE_OFF
                camera.parameters = param
                if (flashmode) {
                    DebugLog.log("Flash Switched ON")
                } else {
                    DebugLog.logE("Flash Switched Off")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            Activity.RESULT_OK -> if (requestCode == REQUEST_IMAGE_SELECTOR) {
                var mCurrentPhoto: File? = null
                val uri = data!!.data ?: return
                if (uri.toString().startsWith("content://com.google.android.apps.photos.content")) {
                    try {
                        val `is` = contentResolver.openInputStream(uri)
                        if (`is` != null) {
                            val pictureBitmap = BitmapFactory.decodeStream(`is`)
                            try {
                                mCurrentPhoto = File(RealPathUtil.getRealPath(this, getImageUri(this, pictureBitmap)))
                            } catch (e: IOException) {
                                e.printStackTrace()
                                DebugLog.logE(e.message)
                            }
                        }
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                    }
                } else {
                    val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
                    val cursor = contentResolver.query(uri, filePathColumn, null, null, null)
                    if (cursor == null || cursor.count < 1) {
                        return
                    }
                    cursor.moveToFirst()
                    val columnIndex = cursor.getColumnIndex(filePathColumn[0])
                    if (columnIndex < 0) { // no column index
                        return
                    }
                    mCurrentPhoto = File(cursor.getString(columnIndex))
                    cursor.close()
                }

//                    if (mOptions.isDetectFace()) {
//                        if (isFoundFace(BitmapFactory.decodeFile(mCurrentPhoto.getPath()))) {
//                            mPhotoPath = mCurrentPhoto.getPath();
//                            //addFragment(SaveImageFragment.newInstance(mPhotoPath));
//                            // start picker to get image for cropping and then use the image in cropping activity
//                            CropImage.activity(Uri.fromFile(mCurrentPhoto))
//                                    //.setMinCropResultSize(10, 10)
//                                    //.setRequestedSize(500, 500, CropImageView.RequestSizeOptions.RESIZE_INSIDE)
//                                    .setBorderLineColor(Color.BLUE)
//                                    .setBorderCornerColor(Color.GREEN)
//                                    .setGuidelines(CropImageView.Guidelines.ON)
//                                    .setFixAspectRatio(mOptions.isFixAspectRatio())
//                                    .start(this);
//                        }
//                    } else {
                CropImage.activity(Uri.fromFile(mCurrentPhoto))
                        .setMinCropResultSize(100, 100) //.setRequestedSize(500, 500, CropImageView.RequestSizeOptions.RESIZE_INSIDE)
                        .setBorderLineColor(Color.BLUE)
                        .setBorderCornerColor(Color.GREEN)
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setFixAspectRatio(mOptions!!.isFixAspectRatio)
                        .start(this)
                //                    }
            } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
                val result = CropImage.getActivityResult(data)
                val uri = result.uri
                DebugLog.log("entra aca CROP_IMAGE_ACTIVITY_REQUEST_CODE " + result.uri)

//                        if (mOptions.isDetectFace()) {
//                            bitmap = BitmapFactory.decodeStream(
//                                    getContentResolver().openInputStream(uri));
//                            log("imagen decodificada en bitmap");
//                            if (isFoundFace(bitmap))
//                                addFragment(SaveImageFragment.newInstance(uri.getPath()));
//                        } else {
                addFragment(SaveImageFragment.newInstance(uri))
                //                        }
            } else if (requestCode == 200) {
                val selectedimg = data!!.data ?: return
                CropImage.activity(selectedimg)
                        .setMinCropResultSize(100, 100) //.setRequestedSize(500, 500, CropImageView.RequestSizeOptions.RESIZE_INSIDE)
                        .setBorderLineColor(Color.BLUE)
                        .setBorderCornerColor(Color.GREEN)
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setFixAspectRatio(mOptions!!.isFixAspectRatio)
                        .start(this)
            }
            CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE -> {
            }
        }
    }

    private fun getImageUri(inContext: Context, inImage: Bitmap): Uri {

        val uri: Uri?
//        var path = MediaStore.Images.Media.insertImage(inContext.contentResolver, inImage, "temp-image", null)
        val relativeLocation = Environment.DIRECTORY_PICTURES + File.pathSeparator + "temp-image"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.TITLE, "temp-image")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.IS_PENDING, 1)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativeLocation)
            }
        }

        uri = inContext.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        try {
            uri?.let { it ->
                val stream = inContext.contentResolver.openOutputStream(it)

                stream?.let { outStream ->
                    if (!inImage.compress(CompressFormat.JPEG, 80, outStream)) {
                        throw IOException("Failed to save bitmap.")
                    }
                } ?: throw IOException("Failed to get output stream.")

            } ?: throw IOException("Failed to create new MediaStore record")
        } catch (e: IOException) {
            if (uri != null) {
                inContext.contentResolver.delete(uri, null, null)
            }
            throw IOException(e)
        } finally {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
        }
        return uri!!
    }

    private fun isFoundFace(bitmap: Bitmap): Boolean {

        //Bitmap icon = BitmapFactory.decodeResource(getResources(),R.drawable.DSC_1344);

        //log("entra 1 detector");
        val faceDetector = FaceDetector.Builder(this)
                .setProminentFaceOnly(true)
                .setClassificationType(FaceDetector.NO_CLASSIFICATIONS)
                .setLandmarkType(FaceDetector.NO_LANDMARKS)
                .setMode(FaceDetector.FAST_MODE)
                .setTrackingEnabled(false)
                .build()
        //log("entra 2 detector");
        if (!faceDetector.isOperational) {
            val v = findViewById<View>(android.R.id.content)
            AlertDialog.Builder(v.context).setMessage("Could not set up the face detector!").show()
            return false
        }
        //log("entra 3 detector");
        val myFrame = Frame.Builder()
                .setBitmap(bitmap)
                .build()
        //log("entra 4 detector");
        val progressDialog = ProgressDialog(this)
        progressDialog.show()
        val faces = faceDetector.detect(myFrame)
        //log("entra 5 detector");
        progressDialog.dismiss()
        faceDetector.release()
        //log("entra 6 detector");
        // Check if at least one barcode was detected
        return if (faces.size() != 0) {
            DebugLog.log("Face found!!! " + faces.valueAt(0).isLeftEyeOpenProbability)
            true
        } else {
            DebugLog.logE("Ningun rostro detectado")
            false
        }
    }
    //==============================================================================================
    // Graphic Face Tracker
    //==============================================================================================
    /**
     * Factory for creating a face tracker to be associated with a new face.  The multiprocessor
     * uses this factory to create face trackers as needed -- one for each individual.
     */
    private inner class GraphicFaceTrackerFactory : MultiProcessor.Factory<Face> {
        override fun create(face: Face): Tracker<Face> {
            return GraphicFaceTracker(binding.faceOverlay as GraphicOverlay<FaceGraphic>)
        }
    }

    /**
     * Face tracker for each detected individual. This maintains a face graphic within the app's
     * associated face overlay.
     */
    private inner class GraphicFaceTracker constructor(private val mOverlay: GraphicOverlay<FaceGraphic>) : Tracker<Face>() {
        private val mFaceGraphic: FaceGraphic = FaceGraphic(mOverlay)

        /**
         * Start tracking the detected face instance within the face overlay.
         */
        override fun onNewItem(faceId: Int, item: Face?) {
            mFaceGraphic.setId(faceId)
        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        override fun onUpdate(detectionResults: Detections<Face?>, face: Face?) {
            mOverlay.add(mFaceGraphic)
            mFaceGraphic.updateFace(face)
            isDetectFace = true
        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        override fun onMissing(detectionResults: Detections<Face?>) {
            mOverlay.remove(mFaceGraphic)
            isDetectFace = false
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        override fun onDone() {
            mOverlay.remove(mFaceGraphic)
            isDetectFace = false
        }

        init {
            //            mOverlayWidth = mOverlay.getWidth();
//            mOverlayHeight = mOverlay.getHeight();
        }
    }

    companion object {
        private const val TAG = "FaceTracker"
        const val PATH_IMAGE_KEY = "image_path"
        private const val RC_HANDLE_GMS = 9001

        // permission request codes need to be < 256
        private const val RC_HANDLE_CAMERA_PERM = 2
        private const val REQUEST_IMAGE_SELECTOR = 199
        private fun getCamera(cameraSource: CameraSource): Camera? {
            val declaredFields = CameraSource::class.java.declaredFields
            for (field in declaredFields) {
                if (field.type == Camera::class.java) {
                    field.isAccessible = true
                    try {
                        return field[cameraSource] as Camera
                    } catch (e: IllegalAccessException) {
                        e.printStackTrace()
                    }
                    break
                }
            }
            return null
        }
    }
}