package co.com.sersoluciones.facedetectorser.serlibrary;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Ser Soluciones SAS on 31/12/2017.
 * www.sersoluciones.com - contacto@sersoluciones.com
 **/
public class PhotoSerOptions implements Parcelable {

    private boolean isDetectFace;
    private boolean fixAspectRatio;
    private boolean saveGalery;

    public PhotoSerOptions() {
        isDetectFace = false;
        fixAspectRatio = true;
        saveGalery = false;
    }

    protected PhotoSerOptions(Parcel in) {
        isDetectFace = in.readByte() != 0;
        fixAspectRatio = in.readByte() != 0;
        saveGalery = in.readByte() != 0;
    }

    public static final Creator<PhotoSerOptions> CREATOR = new Creator<PhotoSerOptions>() {
        @Override
        public PhotoSerOptions createFromParcel(Parcel in) {
            return new PhotoSerOptions(in);
        }

        @Override
        public PhotoSerOptions[] newArray(int size) {
            return new PhotoSerOptions[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeByte((byte) (isDetectFace ? 1 : 0));
        parcel.writeByte((byte) (fixAspectRatio ? 1 : 0));
        parcel.writeByte((byte) (saveGalery ? 1 : 0));
    }

    public boolean isDetectFace() {
        return isDetectFace;
    }

    public void setDetectFace(boolean detectFace) {
        isDetectFace = detectFace;
    }

    public boolean isFixAspectRatio() {
        return fixAspectRatio;
    }

    public void setFixAspectRatio(boolean fixAspectRatio) {
        this.fixAspectRatio = fixAspectRatio;
    }

    public boolean isSaveGalery() {
        return saveGalery;
    }

    public void setSaveGalery(boolean saveGalery) {
        this.saveGalery = saveGalery;
    }
}
