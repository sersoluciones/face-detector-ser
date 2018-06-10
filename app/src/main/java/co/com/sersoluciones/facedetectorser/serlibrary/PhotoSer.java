package co.com.sersoluciones.facedetectorser.serlibrary;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import co.com.sersoluciones.facedetectorser.FaceTrackerActivity;


/**
 * Created by Ser Soluciones SAS on 31/12/2017.
 * www.sersoluciones.com - contacto@sersoluciones.com
 **/
public final class PhotoSer {

    /** The key used to pass ser image bundle data to {@link FaceTrackerActivity}. */
    public static final String PHOTO_SER_EXTRA_BUNDLE = "PHOTO_SER_EXTRA_BUNDLE";
    /** The key used to pass ser image options to {@link FaceTrackerActivity}. */
    public static final String PHOTO_SER_EXTRA_OPTIONS = "PHOTO_SER_EXTRA_OPTIONS";
    /**
     * The request code used to start {@link FaceTrackerActivity} to be used on result to identify the
     * this specific request.
     */
    public static final int SER_IMAGE_ACTIVITY_REQUEST_CODE = 707;

    public PhotoSer() {
    }

    public static ActivityBuilder activity() {
        return new ActivityBuilder();
    }

    public static final class ActivityBuilder {

        /** Options for image PhotoSer UX */
        private final PhotoSerOptions mOptions;

        public ActivityBuilder() {
            mOptions = new PhotoSerOptions();
        }
        /**
         * Get {@link FaceTrackerActivity} intent to start the activity.
         */
        public Intent getIntent(@NonNull Context context) {
            return getIntent(context, FaceTrackerActivity.class);
        }

        /**
         * Get {@link FaceTrackerActivity} intent to start the activity.
         */
        public Intent getIntent(@NonNull Context context, @Nullable Class<?> cls) {
            //mOptions.validate();

            Intent intent = new Intent();
            intent.setClass(context, cls);
            Bundle bundle = new Bundle();
            bundle.putParcelable(PHOTO_SER_EXTRA_OPTIONS, mOptions);
            intent.putExtra(PHOTO_SER_EXTRA_BUNDLE, bundle);
            return intent;
        }

        /**
         * the option to detectFace.<br>
         * <i>Default: false</i>
         */
        public ActivityBuilder setDetectFace(boolean isDetectFace) {
            mOptions.setDetectFace(isDetectFace);
            return this;
        }

        /**
         * the option to save in galery.<br>
         * <i>Default: false</i>
         */
        public ActivityBuilder setSaveGalery(boolean saveGalery) {
            mOptions.setSaveGalery(saveGalery);
            return this;
        }

        /**
         * Start {@link FaceTrackerActivity}.
         *
         * @param activity activity to receive result
         */
        public void start(@NonNull Activity activity) {
            //mOptions.validate();
            activity.startActivityForResult(getIntent(activity), SER_IMAGE_ACTIVITY_REQUEST_CODE);
        }
    }
}
