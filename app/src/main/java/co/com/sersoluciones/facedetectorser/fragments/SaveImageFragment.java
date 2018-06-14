package co.com.sersoluciones.facedetectorser.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import co.com.sersoluciones.facedetectorser.FaceTrackerActivity;
import co.com.sersoluciones.facedetectorser.R;

/**
 * Created by Ser Soluciones SAS on 28/12/2017.
 * www.sersoluciones.com - contacto@sersoluciones.com
 **/
public class SaveImageFragment extends Fragment {

    Button previousButton;
    Button submitButton;

    private static final String ARG_PHOTO_PATH = "mPhotoPath";
    private static final String ARG_URI = "uri_image";
    ImageView imagePreview;
    private String mPhotoPath;
    private Uri uriImage;
    private Bitmap mBitmap;
    private int rotateImage;
    private View mProgressView;

    public SaveImageFragment() {
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param mPhotoPath Parameter 1.
     * @return A new instance of fragment ValidityFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SaveImageFragment newInstance(String mPhotoPath) {
        SaveImageFragment fragment = new SaveImageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PHOTO_PATH, mPhotoPath);
        fragment.setArguments(args);
        return fragment;
    }

    public static SaveImageFragment newInstance(Uri uri) {
        SaveImageFragment fragment = new SaveImageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_URI, uri.toString());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPhotoPath = "";
        rotateImage = 0;
        mBitmap = null;
        if (getArguments() != null) {
            if (getArguments().containsKey(ARG_PHOTO_PATH))
                mPhotoPath = getArguments().getString(ARG_PHOTO_PATH);
            else
                uriImage = Uri.parse(getArguments().getString(ARG_URI));
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_save_photo, container, false);

        imagePreview = view.findViewById(R.id.image_preview);
        submitButton = view.findViewById(R.id.submit_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveImage();
            }
        });
        previousButton = view.findViewById(R.id.previous_button);
        mProgressView = view.findViewById(R.id.progress);

        FloatingActionButton rotateButton = view.findViewById(R.id.fab_rotate);
        rotateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rotateLogo();
            }
        });
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeFragment();
            }
        });

        Bitmap bitmap;
        if (!mPhotoPath.isEmpty()) {
            bitmap = BitmapFactory.decodeFile(mPhotoPath);
            imagePreview.setImageBitmap(bitmap);
        } else {
            imagePreview.setImageURI(uriImage);
            if (mPhotoPath.isEmpty())
                mPhotoPath = uriImage.getPath();
            //bitmap = BitmapFactory.decodeFile(mPhotoPath);
            //imagePreview.setImageBitmap(bitmap);
        }
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    public void saveImage() {
        showProgress(true);
        if ((getActivity()) != null) {
            if (rotateImage > 0) {
               new SaveImageAsyncTask().execute();
            }else{
                ((FaceTrackerActivity) getActivity()).returnURIImage(mPhotoPath);
            }
        }
    }

    public void removeFragment() {
        if (getActivity() != null) {
            ((FaceTrackerActivity) getActivity()).removeFragment();
        }
    }

    public void rotateLogo() {
        showProgress(true);
        rotateImage += 90;
        if (rotateImage == 360) rotateImage = 0;
        mBitmap = ((BitmapDrawable) imagePreview.getDrawable()).getBitmap();
        new RotateAsyncTask().execute(90.0f);
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

    @SuppressLint("StaticFieldLeak")
    class SaveImageAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            ByteArrayOutputStream ostream = new ByteArrayOutputStream();
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, ostream);
            try {
                File imageFile = new File(mPhotoPath);
                FileOutputStream fout = new FileOutputStream(imageFile);
                fout.write(ostream.toByteArray());
                fout.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            ((FaceTrackerActivity) getActivity()).returnURIImage(mPhotoPath);
        }
    }

    @SuppressLint("StaticFieldLeak")
    class RotateAsyncTask extends AsyncTask<Float, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(Float... params) {
            return rotateImage(params[0]);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);

            mBitmap = bitmap;
            imagePreview.setImageBitmap(mBitmap);
            showProgress(false);
        }

        Bitmap rotateImage(float angle) {

            Bitmap bitmap = null;
            Matrix matrix = new Matrix();
            matrix.postRotate(angle);
            try {
                bitmap = Bitmap.createBitmap(mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(),
                        matrix, false);
            } catch (OutOfMemoryError err) {
                err.printStackTrace();
            }
            return bitmap;
        }
    }
}