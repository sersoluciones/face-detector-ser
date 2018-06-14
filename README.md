# face-detector-ser
Libreria que utiliza la camara para detectar rostros, posee varias funcionalidades entre ellas guardado en cache, guardado en galeria, recorte, etc powered by Ser Soluciones



Gradle

Add it in your root build.gradle at the end of repositories:

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
Copy

Step 2. Add the dependency

	dependencies {
        implementation 'com.github.sersoluciones:face-detector-ser:v1.1.1'
	}

In the manifest put:


    <uses-feature android:name="android.hardware.camera" />
    <uses-feature android:name="android.hardware.camera.autofocus" />
    
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
        
    <application
        ...
        android:largeHeap="true"
        android:hardwareAccelerated="false"
        ...>
                
        <activity
            android:name="co.com.sersoluciones.facedetectorser.FaceTrackerActivity"
            android:theme="@style/AppTheme.NoActionBar" />
        
        <activity android:name="com.theartofdev.edmodo.cropper.CropImageActivity" />
        
	</application>
        
Uso:

    new PhotoSer.ActivityBuilder()
         //Detect Face
         .setDetectFace(true)
         //Save in Galery or Cache
         .setSaveGalery(true)
         .setFixAspectRatio(true)
         //Enable crop image
         .setCrop(true)
         //Quality of image
         .setQuality(50)
         .start(this);
         
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
     super.onActivityResult(requestCode, resultCode, data);
     if (requestCode == PhotoSer.SER_IMAGE_ACTIVITY_REQUEST_CODE) {
         switch (resultCode) {
             case RESULT_OK:
                 String pathImage = data.getStringExtra(PATH_IMAGE_KEY);
                 Log.d(TAG,"pathImage: " + pathImage);
                 break;
             default:
                 break;
            }
        }
    }

Credits to com.theartofdev.edmodo.cropper.CropImageActivity