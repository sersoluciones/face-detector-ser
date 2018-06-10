package co.com.sersoluciones.facedetectorser.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

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
        if ((getActivity()) != null) {
            ((FaceTrackerActivity) getActivity()).returnURIImage(mPhotoPath);
        }
    }

    public void removeFragment() {
        if (getActivity() != null) {
            ((FaceTrackerActivity) getActivity()).removeFragment();
        }
    }


}