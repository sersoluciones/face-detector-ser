package co.com.sersoluciones.facedetectorser.fragments

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import co.com.sersoluciones.facedetectorser.FaceTrackerActivity
import co.com.sersoluciones.facedetectorser.databinding.FragmentSavePhotoBinding
import co.com.sersoluciones.facedetectorser.utilities.DebugLog.logW
import java.io.FileDescriptor

/**
 * Created by Ser Soluciones SAS on 28/12/2017.
 * www.sersoluciones.com - contacto@sersoluciones.com
 */
class SaveImageFragment : Fragment() {

    private var uriImage: Uri? = null
    private var mBitmap: Bitmap? = null
    private var rotateImage = 0

    private lateinit var binding: FragmentSavePhotoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rotateImage = 0
        mBitmap = null
        arguments?.let {
            if (it.containsKey(ARG_URI)) uriImage = Uri.parse(arguments!!.getString(ARG_URI))
            logW("uriImage: $uriImage")
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentSavePhotoBinding.inflate(inflater)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.submitButton.setOnClickListener { saveImage() }
        binding.fabRotate.setOnClickListener { rotateLogo() }

        binding.previousButton.setOnClickListener { removeFragment() }

        //BitmapFactory.Options options = new BitmapFactory.Options();
        //options.inSampleSize = 2;
//            bitmap = BitmapFactory.decodeFile(uriImage!!.path)

//            val openStream = URL(uriImage!!.toString()).openStream()
//            bitmap = BitmapFactory.decodeStream(openStream)
//
//            val ostream = ByteArrayOutputStream()
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, ostream)
//            val decoded = BitmapFactory.decodeStream(ByteArrayInputStream(ostream.toByteArray()))
//            binding.imagePreview.setImageBitmap(decoded)

        val readOnlyMode = "r"
        val image = activity?.contentResolver?.openFileDescriptor(uriImage!!, readOnlyMode).use { pfd ->
            pfd?.let {
                val fileDescriptor: FileDescriptor = it.fileDescriptor
                BitmapFactory.decodeFileDescriptor(fileDescriptor)
            }
        }
        binding.imagePreview.setImageBitmap(image)

    }

    private fun saveImage() {
        showProgress(true)
        if (activity != null) {
            if (rotateImage > 0) {
                SaveImageAsyncTask().execute()
            } else {
                (activity as FaceTrackerActivity?)!!.returnURIImage(uriImage!!)
            }
        }
    }

    private fun removeFragment() {
        if (activity != null) {
            (activity as FaceTrackerActivity?)!!.removeFragment()
        }
    }

    private fun rotateLogo() {
        showProgress(true)
        rotateImage += 90
        if (rotateImage == 360) rotateImage = 0
        mBitmap = (binding.imagePreview.drawable as BitmapDrawable).bitmap
        RotateAsyncTask().execute(90.0f)
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private fun showProgress(show: Boolean) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime)
        binding.progress.visibility = if (show) View.VISIBLE else View.GONE
        binding.progress.animate().setDuration(shortAnimTime.toLong()).alpha(
                if (show) 1f else 0f).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                binding.progress.visibility = if (show) View.VISIBLE else View.GONE
            }
        })
    }

    @SuppressLint("StaticFieldLeak")
    inner class SaveImageAsyncTask : AsyncTask<Void?, Void?, Void?>() {
        override fun doInBackground(vararg params: Void?): Void? {

            try {
                activity?.contentResolver!!.openOutputStream(uriImage!!).let { out ->
                    mBitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, out)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                logW("Failed to insert image")
            }

            return null
        }

        override fun onPostExecute(aVoid: Void?) {
            super.onPostExecute(aVoid)
            (activity as FaceTrackerActivity?)!!.returnURIImage(uriImage!!)
        }
    }

    @SuppressLint("StaticFieldLeak")
    inner class RotateAsyncTask : AsyncTask<Float?, Void?, Bitmap?>() {

        override fun doInBackground(vararg params: Float?): Bitmap? {
            return rotateImage(params[0]!!)
        }

        override fun onPostExecute(bitmap: Bitmap?) {
            super.onPostExecute(bitmap)
            mBitmap = bitmap
            binding.imagePreview.setImageBitmap(mBitmap)
            showProgress(false)
        }

        private fun rotateImage(angle: Float): Bitmap? {
            var bitmap: Bitmap? = null
            val matrix = Matrix()
            matrix.postRotate(angle)
            try {
                bitmap = Bitmap.createBitmap(mBitmap!!, 0, 0, mBitmap!!.width, mBitmap!!.height,
                        matrix, false)
            } catch (err: OutOfMemoryError) {
                err.printStackTrace()
            }
            return bitmap
        }


    }

    companion object {
        private const val ARG_URI = "uri_image"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param uri uri from image
         * @return A new instance of fragment ValidityFragment.
         */
        fun newInstance(uri: Uri): SaveImageFragment {
            val fragment = SaveImageFragment()
            val args = Bundle()
            args.putString(ARG_URI, uri.toString())
            fragment.arguments = args
            return fragment
        }
    }
}