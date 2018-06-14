package co.com.sersoluciones.facedetectorser;

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
 */

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;

import co.com.sersoluciones.facedetectorser.camera.CameraSource;
import co.com.sersoluciones.facedetectorser.camera.FaceGraphic;
import co.com.sersoluciones.facedetectorser.fragments.SaveImageFragment;
import co.com.sersoluciones.facedetectorser.serlibrary.PhotoSerOptions;
import co.com.sersoluciones.facedetectorser.views.CameraSourcePreview;
import co.com.sersoluciones.facedetectorser.views.GraphicOverlay;

import static co.com.sersoluciones.facedetectorser.serlibrary.PhotoSer.PHOTO_SER_EXTRA_BUNDLE;
import static co.com.sersoluciones.facedetectorser.serlibrary.PhotoSer.PHOTO_SER_EXTRA_OPTIONS;
import static co.com.sersoluciones.facedetectorser.utilities.DebugLog.log;
import static co.com.sersoluciones.facedetectorser.utilities.DebugLog.logW;
import static co.com.sersoluciones.facedetectorser.utilities.DebugLog.logE;

/**
 * Activity for the face tracker app.  This app detects faces with the rear facing camera, and draws
 * overlay graphics to indicate the position, size, and ID of each face.
 * Created by Ser Soluciones SAS on 11/12/2017.
 * www.sersoluciones.com - contacto@sersoluciones.com
 **/
public class FaceTrackerActivity extends AppCompatActivity implements CameraSource.ShutterCallback,
        CameraSource.PictureCallback {
    private static final String TAG = "FaceTracker";

    public static final String PATH_IMAGE_KEY = "image_path";
    FloatingActionButton buttonAttachGalery;

    private CameraSource mCameraSource = null;

    private CameraSourcePreview mPreview;
    private GraphicOverlay<FaceGraphic> mGraphicOverlay;

    private static final int RC_HANDLE_GMS = 9001;
    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;
    private Matrix matrix;
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;
    private boolean toggle;
    private boolean isChecked;
    private CameraSource.ShutterCallback shutterCallback;
    private CameraSource.PictureCallback pictureCallback;
    private boolean isDetectFace;
    private int widthImage;
    private boolean isTakePhoto;
    private String mPhotoPath;
    private Fragment fragment;
    private FloatingActionButton buttonTakePhoto;
    private FloatingActionButton buttonLight;
    private FloatingActionButton buttonSwitchCamera;
    private static final int REQUEST_IMAGE_SELECTOR = 199;
    private PhotoSerOptions mOptions;
    private View mProgressView;
    private int mOverlayWidth, mOverlayHeight;

    //==============================================================================================
    // Activity Methods
    //==============================================================================================

    /**
     * Initializes the UI and initiates the creation of a face detector.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        // Optional: Hide the status bar at the top of the window
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.face_tracker);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Bundle bundle = getIntent().getBundleExtra(PHOTO_SER_EXTRA_BUNDLE);
        mOptions = bundle.getParcelable(PHOTO_SER_EXTRA_OPTIONS);

        toggle = false;
        isChecked = false;
        mPreview = findViewById(R.id.preview);
        mGraphicOverlay = findViewById(R.id.faceOverlay);

        buttonAttachGalery = findViewById(R.id.fab_attach);
        mProgressView = findViewById(R.id.frame_progress);

        shutterCallback = this;
        pictureCallback = this;
        widthImage = 0;
        isTakePhoto = false;
        mPhotoPath = "";
        isDetectFace = false;
        mOverlayWidth = 0;
        mOverlayHeight = 0;
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        buttonTakePhoto = findViewById(R.id.fab_camera);
        buttonTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePhoto();
            }
        });
        buttonLight = findViewById(R.id.fab_light);
        buttonLight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isChecked = !isChecked;
                if (isChecked)
                    buttonLight.setImageResource(R.drawable.ic_flash_on_white_36dp);
                else
                    buttonLight.setImageResource(R.drawable.ic_flash_off_white_36dp);
                flashOnButton(isChecked);
            }
        });
        buttonSwitchCamera = findViewById(R.id.fab_switch_camera);
        buttonSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mPreview.stop();
                mCameraSource.release();
                toggle = !toggle;
                if (toggle)
                    buttonLight.setEnabled(false);
                else
                    buttonLight.setEnabled(true);
                createCameraSource(toggle);
                startCameraSource();
                isDetectFace = false;
            }
        });

        int widthPixels = metrics.widthPixels;
        int heightPixels = metrics.heightPixels;
        logW(String.format("FaceTrackerActivity width: %s, height: %s", widthPixels, heightPixels));

        matrix = new Matrix();

        gestureDetector = new GestureDetector(this, new CaptureGestureListener());
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

        Snackbar.make(mGraphicOverlay, "Toque para autoenfocar. Pellizque/estire para zoom",
                Snackbar.LENGTH_SHORT)
                .show();

        buttonAttachGalery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attachImageFromGalery();
            }
        });
    }

    public void attachImageFromGalery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        this.startActivityForResult(galleryIntent, REQUEST_IMAGE_SELECTOR);
    }

    private void addFragment(Fragment fragment) {
        if (mPreview != null)
            mPreview.stop();
        if (mCameraSource != null) {
            mCameraSource.release();
            mCameraSource = null;
        }
        findViewById(R.id.preview).setVisibility(View.GONE);
        buttonSwitchCamera.setVisibility(View.GONE);
        buttonLight.setVisibility(View.GONE);
        buttonTakePhoto.setVisibility(View.GONE);
        buttonAttachGalery.setVisibility(View.GONE);

        this.fragment = fragment;
        findViewById(R.id.container).setVisibility(View.VISIBLE);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.container, fragment);
        fragmentTransaction.commit();
    }

    public void removeFragment() {
        if (mCameraSource == null) {
            createCameraSource(toggle);
            startCameraSource();
        }
        findViewById(R.id.preview).setVisibility(View.VISIBLE);
        buttonSwitchCamera.setVisibility(View.VISIBLE);
        buttonLight.setVisibility(View.VISIBLE);
        buttonTakePhoto.setVisibility(View.VISIBLE);
        buttonAttachGalery.setVisibility(View.VISIBLE);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.remove(fragment);
        fragmentTransaction.commit();
        findViewById(R.id.container).setVisibility(View.GONE);
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void takePhoto() {

        if (!mOptions.isDetectFace()) {
            mCameraSource.takePicture(shutterCallback, pictureCallback);
            showProgress(true);
        } else {
            if (isDetectFace) {
                isTakePhoto = true;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mCameraSource.takePicture(shutterCallback, pictureCallback);
                        showProgress(true);
                    }
                }, 1000);

            } else
                Toast.makeText(this, "Ningun rostro detectado", Toast.LENGTH_SHORT).show();
        }

    }

    public void returnURIImage(String path) {
        mPhotoPath = path;
        if (mOptions.isSaveGalery()) {
            new SaveImageInGaleryAsyncTask().execute(mPhotoPath);
        } else {
            Intent intent = new Intent();
            intent.putExtra(PATH_IMAGE_KEY, mPhotoPath);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    @Override
    public void onPictureTaken(byte[] bytes) {
        if (mPreview != null)
            mPreview.stop();

        //decodeBytes(bytes);
        showProgress(false);
        File imageFile;
        int rotation = 0;
        if (toggle)
            rotation = 360;
        try {
            // convert byte array into bitmap
            Bitmap loadedImage;
            Bitmap scaledBitmap;
            loadedImage = BitmapFactory.decodeByteArray(bytes, 0,
                    bytes.length);

            if (rotation > 0) {
                Matrix rotateMatrix = new Matrix();
                rotateMatrix.postRotate(rotation);
                // Scale down to the output size
                scaledBitmap = Bitmap.createBitmap(loadedImage, 0, 0,
                        loadedImage.getWidth(), loadedImage.getHeight(),
                        rotateMatrix, false);
            } else {
                scaledBitmap = loadedImage;
            }
            new SaveImageAsyncTask().execute(scaledBitmap, loadedImage);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void launchInstance(Uri outputFileUri) {
        if (!mOptions.isCrop()) {
            addFragment(SaveImageFragment.newInstance(outputFileUri));
            return;
        }

        if (mOptions.isDetectFace()) {
            CropImage.activity(outputFileUri)
                    .setMinCropResultSize(500, 500)
                    //.setRequestedSize(500, 500, CropImageView.RequestSizeOptions.RESIZE_INSIDE)
                    .setBorderLineColor(Color.BLUE)
                    .setBorderCornerColor(Color.GREEN)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setFixAspectRatio(mOptions.isFixAspectRatio())
                    .start(this);
        } else {
            CropImage.activity(outputFileUri)
                    .setMinCropResultSize(500, 500)
                    //.setRequestedSize(500, 500, CropImageView.RequestSizeOptions.RESIZE_INSIDE)
                    .setBorderLineColor(Color.BLUE)
                    .setBorderCornerColor(Color.GREEN)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setFixAspectRatio(mOptions.isFixAspectRatio())
                    .start(this);
        }
    }

    @SuppressLint("StaticFieldLeak")
    class SaveImageAsyncTask extends AsyncTask<Bitmap, Void, Uri> {

        @Override
        protected Uri doInBackground(Bitmap... params) {
            Uri outputFileUri = writeTempStateStoreBitmap(FaceTrackerActivity.this, params[0], null);
            isTakePhoto = false;
            isDetectFace = false;
            params[1].recycle();
            return outputFileUri;
        }

        @Override
        protected void onPostExecute(Uri outputFileUri) {
            super.onPostExecute(outputFileUri);
            launchInstance(outputFileUri);
        }
    }

    @SuppressLint("StaticFieldLeak")
    class SaveImageInGaleryAsyncTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String photoPath = "";
            try {
                photoPath = saveImageInGalery(params[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return photoPath;
        }

        @Override
        protected void onPostExecute(String photoPath) {
            super.onPostExecute(photoPath);
            mPhotoPath = photoPath;
            Intent intent = new Intent();
            intent.putExtra(PATH_IMAGE_KEY, mPhotoPath);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    private String saveImageInGalery(String pathImage) throws IOException {

        File photo = new File(pathImage);
        Bitmap bitmap = BitmapFactory.decodeFile(pathImage);
        File imageFile;
        File dir = new File(
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES), "FaceDetectorSER");

        boolean success = true;
        if (!dir.exists()) {
            success = dir.mkdirs();
        }
        if (success) {
            Date date = new Date();
            imageFile = new File(dir.getAbsolutePath()
                    + File.separator
                    + new Timestamp(date.getTime()).toString()
                    + "photo.jpg");

            imageFile.createNewFile();
        } else {
            Toast.makeText(getBaseContext(), "Image Not saved",
                    Toast.LENGTH_SHORT).show();
            return null;
        }
        // save image into gallery
        ByteArrayOutputStream ostream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, mOptions.getQuality(), ostream);

        FileOutputStream fout = new FileOutputStream(imageFile);
        fout.write(ostream.toByteArray());
        fout.close();
        ContentValues values = new ContentValues();

        values.put(MediaStore.Images.Media.DATE_TAKEN,
                System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.DATA,
                imageFile.getAbsolutePath());

        mPhotoPath = imageFile.getPath();

        this.getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        photo.delete();
        return mPhotoPath;
    }

    @SuppressWarnings("unused")
    private void decodeBytes(byte[] data) {

        Camera mCamera = mCameraSource.getCamera();
        if (mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            int width = parameters.getPreviewSize().width;
            int height = parameters.getPreviewSize().height;

            YuvImage yuv = new YuvImage(data, parameters.getPreviewFormat(), width, height, null);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuv.compressToJpeg(new Rect(0, 0, width, height), mOptions.getQuality(), out);

            byte[] bytes = out.toByteArray();
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
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
    private Uri writeTempStateStoreBitmap(Context context, Bitmap bitmap, Uri uri) {
        try {
            boolean needSave = true;
            if (uri == null) {
                uri =
                        Uri.fromFile(
                                File.createTempFile("aic_state_store_temp", ".jpg", context.getCacheDir()));
            } else if (new File(uri.getPath()).exists()) {
                needSave = false;
            }
            if (needSave) {
                writeBitmapToUri(context, bitmap, uri, Bitmap.CompressFormat.JPEG, mOptions.getQuality());
            }
            return uri;
        } catch (Exception e) {
            Log.w("AIC", "Failed to write bitmap to temp file for image-cropper save instance state", e);
            return null;
        }
    }

    /**
     * Write the given bitmap to the given uri using the given compression.
     */
    public void writeBitmapToUri(
            Context context,
            Bitmap bitmap,
            Uri uri,
            Bitmap.CompressFormat compressFormat,
            int compressQuality)
            throws FileNotFoundException {
        OutputStream outputStream = null;
        try {
            outputStream = context.getContentResolver().openOutputStream(uri);
            bitmap.compress(compressFormat, compressQuality, outputStream);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // Get the preview size
        //widthPixels = mPreview.getMeasuredWidth();
        //heightPixels = mPreview.getMeasuredHeight();
    }

    /**
     * Metodo para controlar los eventos de la actividad
     * scaleGestureDetector, gestureDetector
     *
     * @param e
     * @return boolean
     */
    @Override
    public boolean onTouchEvent(MotionEvent e) {

        boolean b = scaleGestureDetector.onTouchEvent(e);

        boolean c = gestureDetector.onTouchEvent(e);

        return b || c || super.onTouchEvent(e);
    }


    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     */
    private void createCameraSource(boolean front) {

        Context context = getApplicationContext();
        FaceDetector detector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setLandmarkType(FaceDetector.NO_LANDMARKS)
                .setMode(FaceDetector.FAST_MODE)
                .build();

        detector.setProcessor(
                new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory())
                        .build());

        if (!detector.isOperational()) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Log.w(TAG, "Face detector dependencies are not yet available.");
        }

        if (mOptions.isDetectFace()) {
            mCameraSource = new CameraSource.Builder(context, detector)
                    .setRequestedPreviewSize(640, 480)
                    .setFacing(front ? CameraSource.CAMERA_FACING_FRONT : CameraSource.CAMERA_FACING_BACK)
                    .setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)
                    //.setRequestedFps(30.0f)
                    .build();
        } else {
            mCameraSource = new CameraSource.Builder(context)
                    .setRequestedPreviewSize(640, 480)
                    .setFacing(front ? CameraSource.CAMERA_FACING_FRONT : CameraSource.CAMERA_FACING_BACK)
                    .setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)
                    //.setRequestedFps(30.0f)
                    .build();
        }
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();
        logW("onResume");
        if (mCameraSource != null)
            startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        logW("onPause");
        if (mPreview != null)
            mPreview.stop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        logW("onStart");
        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (rc == PackageManager.PERMISSION_GRANTED && result == PackageManager.PERMISSION_GRANTED) {
            createCameraSource(toggle);
        } else {
            requestCameraPermission();
        }
//        if (mCameraSource == null)
//            createCameraSource(toggle);
    }

    @Override
    protected void onStop() {
        super.onStop();
        logW("onStop");
        if (mCameraSource != null) {
            mCameraSource.release();
            mCameraSource = null;
        }
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA) && !ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        final ArrayList<String> permissionsNotGranted = new ArrayList<>();
        boolean mPermissionGranted = true;
        //Checking the request code of our request
        if (requestCode == RC_HANDLE_CAMERA_PERM) {

            int count = 0;
            for (String permission : permissions) {
                if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                        permission.equals(Manifest.permission.CAMERA)) {
                    if (grantResults[count] == PackageManager.PERMISSION_DENIED) {
                        log("permiso: " + permission);
                        permissionsNotGranted.add(permission);
                        mPermissionGranted = false;
                    }
                }
                count++;
            }

            if (!mPermissionGranted) {

                final String[] permisos = new String[permissionsNotGranted.size()];
                permissionsNotGranted.toArray(permisos);

                for (final String permiso : permisos) {

                    boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(FaceTrackerActivity.this, permiso);
                    if (!showRationale) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", FaceTrackerActivity.this.getPackageName(), null);
                        intent.setData(uri);
                        FaceTrackerActivity.this.startActivity(intent);
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

                        Snackbar snackbar = Snackbar.make(getView(), getResources().getString(R.string.permission_camera_rationale),
                                Snackbar.LENGTH_INDEFINITE);
                        snackbar.setAction(getResources().getString(R.string.no_camera_permission), new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ActivityCompat.requestPermissions(FaceTrackerActivity.this, permisos, RC_HANDLE_CAMERA_PERM);
                            }
                        });
                        snackbar.show();
                    }
                }
            } else {
                createCameraSource(false);
            }
        }
    }

    private View getView() {
        return findViewById(android.R.id.content);
    }

    //==============================================================================================
    // Camera Source Preview
    //==============================================================================================

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    @SuppressLint("MissingPermission")
    private void startCameraSource() {

        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                //mPreview.start(mCameraSource, mGraphicOverlay, heightPixels, widthPixels);
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }


    /**
     * onTap is called to capture the oldest barcode currently detected and
     * return it to the caller.
     *
     * @param e - MotionEvent
     * @return true if the mPreview is ending.
     */
    private boolean onTap(MotionEvent e) {

        if (mPreview != null) {

            if (mCameraSource != null) {
                Rect focusRect = calculateTapArea(e.getX(), e.getY(), 1f);
                Rect meteringRect = calculateTapArea(e.getX(), e.getY(), 1.5f);
                log(String.format("posX: %s, posY: %s", e.getX(), e.getY()));
                mCameraSource.focusOnTouch(focusRect, meteringRect);
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
        return mPreview != null;
    }

    private Rect calculateTapArea(float x, float y, float coefficient) {
        int focusAreaSize = getResources().getDimensionPixelSize(R.dimen.camera_focus_area_size);
        int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();

        int left = clamp((int) x - areaSize / 2, 0, mCameraSource.getPreviewSize().getWidth() - areaSize);
        int top = clamp((int) y - areaSize / 2, 0, mCameraSource.getPreviewSize().getHeight() - areaSize);

        RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);
        matrix.mapRect(rectF);

        return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom));
    }

    private int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

    @Override
    public void onShutter() {

    }

    private class CaptureGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return onTap(e) || super.onSingleTapConfirmed(e);
        }
    }

    private class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener {

        /**
         * Responds to scaling events for a gesture in progress.
         * Reported by pointer motion.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         * @return Whether or not the detector should consider this event
         * as handled. If an event was not handled, the detector
         * will continue to accumulate movement until an event is
         * handled. This can be useful if an application, for example,
         * only wants to update scaling factors if the change is
         * greater than 0.01.
         */
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return false;
        }

        /**
         * Responds to the beginning of a scaling gesture. Reported by
         * new pointers going down.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         * @return Whether or not the detector should continue recognizing
         * this gesture. For example, if a gesture is beginning
         * with a focal point outside of a region where it makes
         * sense, onScaleBegin() may return false to ignore the
         * rest of the gesture.
         */
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        /**
         * Responds to the end of a scale gesture. Reported by existing
         * pointers going up.
         * <p/>
         * Once a scale has ended, {@link ScaleGestureDetector#getFocusX()}
         * and {@link ScaleGestureDetector#getFocusY()} will return focal point
         * of the pointers remaining on the screen.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         */
        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            if (mCameraSource != null)
                mCameraSource.doZoom(detector.getScaleFactor());
        }
    }

    private void flashOnButton(boolean flashmode) {
        Camera camera = getCamera(mCameraSource);
        if (camera != null) {
            try {
                Camera.Parameters param = camera.getParameters();
                param.setFlashMode(flashmode ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF);
                camera.setParameters(param);
                if (flashmode) {
                    log("Flash Switched ON");
                } else {
                    logE("Flash Switched Off");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private static Camera getCamera(@NonNull CameraSource cameraSource) {
        Field[] declaredFields = CameraSource.class.getDeclaredFields();

        for (Field field : declaredFields) {
            if (field.getType() == Camera.class) {
                field.setAccessible(true);
                try {
                    Camera camera = (Camera) field.get(cameraSource);
                    if (camera != null) {
                        return camera;
                    }
                    return null;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        return null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (resultCode) {
            case RESULT_OK:
                if (requestCode == REQUEST_IMAGE_SELECTOR) {
                    File mCurrentPhoto;
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};
                    Cursor cursor = getContentResolver().query(data.getData(), filePathColumn, null, null, null);
                    if (cursor == null || cursor.getCount() < 1) {
                        break;
                    }
                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    if (columnIndex < 0) { // no column index
                        break;
                    }
                    mCurrentPhoto = new File(cursor.getString(columnIndex));

                    if (mOptions.isDetectFace()) {
                        if (isFoundFace(BitmapFactory.decodeFile(mCurrentPhoto.getPath()))) {
                            mPhotoPath = mCurrentPhoto.getPath();
                            //addFragment(SaveImageFragment.newInstance(mPhotoPath));
                            // start picker to get image for cropping and then use the image in cropping activity
                            CropImage.activity(Uri.fromFile(mCurrentPhoto))
                                    .setMinCropResultSize(500, 500)
                                    //.setRequestedSize(500, 500, CropImageView.RequestSizeOptions.RESIZE_INSIDE)
                                    .setBorderLineColor(Color.BLUE)
                                    .setBorderCornerColor(Color.GREEN)
                                    .setGuidelines(CropImageView.Guidelines.ON)
                                    .setFixAspectRatio(mOptions.isFixAspectRatio())
                                    .start(this);
                        }
                    } else {
                        CropImage.activity(Uri.fromFile(mCurrentPhoto))
                                .setMinCropResultSize(500, 500)
                                //.setRequestedSize(500, 500, CropImageView.RequestSizeOptions.RESIZE_INSIDE)
                                .setBorderLineColor(Color.BLUE)
                                .setBorderCornerColor(Color.GREEN)
                                .setGuidelines(CropImageView.Guidelines.ON)
                                .setFixAspectRatio(mOptions.isFixAspectRatio())
                                .start(this);
                    }
                    cursor.close();
                } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {

                    CropImage.ActivityResult result = CropImage.getActivityResult(data);
                    Uri uri = result.getUri();
                    log("entra aca CROP_IMAGE_ACTIVITY_REQUEST_CODE " + result.getUri());

                    Bitmap bitmap = null;
                    try {
                        if (mOptions.isDetectFace()) {
                            bitmap = BitmapFactory.decodeStream(
                                    getContentResolver().openInputStream(uri));
                            log("imagen decodificada en bitmap");
                            if (isFoundFace(bitmap))
                                addFragment(SaveImageFragment.newInstance(uri.getPath()));
                        } else {
                            addFragment(SaveImageFragment.newInstance(uri));
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE:

                break;
        }
    }

    private boolean isFoundFace(Bitmap bitmap) {

        //Bitmap icon = BitmapFactory.decodeResource(getResources(),R.drawable.DSC_1344);

        //log("entra 1 detector");
        FaceDetector faceDetector = new FaceDetector.Builder(this)
                .setProminentFaceOnly(true)
                .setClassificationType(FaceDetector.NO_CLASSIFICATIONS)
                .setLandmarkType(FaceDetector.NO_LANDMARKS)
                .setMode(FaceDetector.FAST_MODE)
                .setTrackingEnabled(false)
                .build();
        //log("entra 2 detector");
        if (!faceDetector.isOperational()) {
            View v = findViewById(android.R.id.content);
            new AlertDialog.Builder(v.getContext()).setMessage("Could not set up the face detector!").show();
            return false;
        }
        //log("entra 3 detector");
        Frame myFrame = new Frame.Builder()
                .setBitmap(bitmap)
                .build();
        //log("entra 4 detector");
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();
        SparseArray<Face> faces = faceDetector.detect(myFrame);
        //log("entra 5 detector");
        progressDialog.dismiss();
        faceDetector.release();
        //log("entra 6 detector");
        // Check if at least one barcode was detected
        if (faces.size() != 0) {
            log("Face found!!! " + faces.valueAt(0).getIsLeftEyeOpenProbability());

            return true;
        } else {
            Toast.makeText(this, "Ningun rostro detectado", Toast.LENGTH_SHORT).show();
            return false;
        }
    }
    //==============================================================================================
    // Graphic Face Tracker
    //==============================================================================================

    /**
     * Factory for creating a face tracker to be associated with a new face.  The multiprocessor
     * uses this factory to create face trackers as needed -- one for each individual.
     */
    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        @Override
        public Tracker<Face> create(Face face) {
            return new GraphicFaceTracker(mGraphicOverlay);
        }
    }

    /**
     * Face tracker for each detected individual. This maintains a face graphic within the app's
     * associated face overlay.
     */
    private class GraphicFaceTracker extends Tracker<Face> {
        private GraphicOverlay<FaceGraphic> mOverlay;
        private FaceGraphic mFaceGraphic;

        GraphicFaceTracker(GraphicOverlay<FaceGraphic> overlay) {
            mOverlay = overlay;
            mFaceGraphic = new FaceGraphic(overlay);
            mOverlayWidth = mOverlay.getWidth();
            mOverlayHeight = mOverlay.getHeight();
        }

        /**
         * Start tracking the detected face instance within the face overlay.
         */
        @Override
        public void onNewItem(int faceId, Face item) {
            mFaceGraphic.setId(faceId);
        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
            mOverlay.add(mFaceGraphic);
            mFaceGraphic.updateFace(face);
            isDetectFace = true;
        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            mOverlay.remove(mFaceGraphic);
            isDetectFace = false;
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        @Override
        public void onDone() {
            mOverlay.remove(mFaceGraphic);
            isDetectFace = false;
        }
    }
}