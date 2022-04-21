package com.example.collagedisplay


import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "MAIN_ACTIVITY"

class MainActivity : AppCompatActivity() {

    private lateinit var mainImageButton: ImageButton
    private lateinit var mainView: View
    private lateinit var uploadImageFab: FloatingActionButton
    private lateinit var uploadProgressBar: ProgressBar

    private var newImagePath: String? = null
    private var visibleImagePath: String? = null
    private var imageFileName: String? = null
    private var imageUri: Uri? = null

    private val storage = Firebase.storage

    private val NEW_IMAGE_PATH_KEY = "new image path key"
    private val VISIBLE_IMAGE_PATH_KEY = "visible image path key"

    private val cameraActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        result -> handleImage(result)
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        newImagePath = savedInstanceState?.getString(NEW_IMAGE_PATH_KEY)
        visibleImagePath = savedInstanceState?.getString(VISIBLE_IMAGE_PATH_KEY)

        mainImageButton = findViewById(R.id.main_image_button)
        uploadProgressBar = findViewById(R.id.uploadProgressBar)
        uploadImageFab = findViewById(R.id.uploadImageButton)
        mainView = findViewById(R.id.content_layout)

        mainImageButton.setOnClickListener{
            takePicture()
        }

        uploadImageFab.setOnClickListener {
            uploadImage()
        }
    }

    private fun uploadImage() {
        if (imageUri != null && imageFileName != null) {

            uploadProgressBar.visibility = View.VISIBLE

            val imageStorageRootReference = storage.reference
            val imageCollectionReference = imageStorageRootReference.child("images")
            val imageFileReference = imageCollectionReference.child(imageFileName!!)

            imageFileReference.putFile(imageUri!!).addOnCompleteListener {
                Snackbar.make(mainView, "Image uploaded!", Snackbar.LENGTH_SHORT).show()
                uploadProgressBar.visibility = View.GONE
            }
                .addOnFailureListener { error ->
                    Snackbar.make(mainView, "Error uploading image", Snackbar.LENGTH_LONG).show()
                    Log.e(TAG, "error uploading image $imageFileName", error)
                    uploadProgressBar.visibility = View.GONE
                }
        } else {
            Snackbar.make(mainView, "Take a picture first!", Snackbar.LENGTH_LONG).show()
        }
    }

    // Sends the the image paths as an outState bundle to be read on View being recreated.
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(NEW_IMAGE_PATH_KEY, newImagePath)
        outState.putString(VISIBLE_IMAGE_PATH_KEY, visibleImagePath)
    }

    private fun takePicture() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val (imageFile, imageFilePath) = createImageFile()
        if (imageFile != null) {
            // This is a refrence to the image file.
            newImagePath = imageFilePath
            imageUri = FileProvider.getUriForFile(this,
                "com.example.collagedisplay.fileprovider",
                imageFile) // Creates a URI for the new image file that was created.

            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            // We now create a file to write the image to and store the location of where that file is on the device
            // and then provide that information as an extra to the native camera app.
            cameraActivityLauncher.launch(takePictureIntent)
        }

    }

    private fun createImageFile(): Pair<File?, String?>{
        try {
            val dateTime = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date()) // Used to create a unique file name
            imageFileName = "COLLAGE_$dateTime" // Adds our unique timeStamp to the photo name.
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES) // Checks the default image storage
            val file = File.createTempFile(imageFileName, ".jpg", storageDir)
            val filePath = file.absolutePath

            return file to filePath // a refrence to the file and the string repersentation of the files location.

        } catch (ex: IOException) {
            return null to null
        }
    }

    private fun handleImage(result: ActivityResult) {
        when (result.resultCode) {
            RESULT_OK -> {
                Log.d(TAG, "Result ok, user took a picture, image at $newImagePath")
                visibleImagePath = newImagePath // Runs once UI is set up and checks ImagePath if it has an image

            }
            RESULT_CANCELED -> {
                Log.d(TAG, "Result cancelled, no picture taken")
            }
        }
    }

    // OnWindowChanged is a callback function that is called when a device is rotated
    // when the user interface has been completely inflated, and gets called when returing from
    // another activity.
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        Log.d(TAG, "on window focus changed $hasFocus visible image at $visibleImagePath")
        if (hasFocus) {
            visibleImagePath?.let { imagePath ->
                loadImage(mainImageButton, imagePath)
            }
        }
    }

    private fun loadImage(imageButton: ImageButton, imagePath: String) {
        Picasso.get()
            .load(File(imagePath))
            .error(android.R.drawable.stat_notify_error)
            .fit()
            .centerCrop()
            .into(imageButton, object: Callback {
                override fun onSuccess() {
                    Log.d(TAG, "loaded image $imagePath")
                }

                override fun onError(e: Exception?) {
                    Log.e(TAG, "Error loading image $imagePath", e)
                }
            })
    }
}