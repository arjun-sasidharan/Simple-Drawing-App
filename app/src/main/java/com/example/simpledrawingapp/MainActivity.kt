package com.example.simpledrawingapp

import android.Manifest.permission
import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private lateinit var drawingView: DrawingView
    private lateinit var mImageButtonCurrentPaint: ImageButton
    private var currentBrushSize = 20.toFloat()

    // Callback when user selected a photo from gallery
    private val openGalleryLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val imageBackground: ImageView = findViewById(R.id.ivBackground)
            imageBackground.setImageURI(result.data?.data)
        }
    }

    private val permissionLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value
                if (permissionName == permission.WRITE_EXTERNAL_STORAGE) {
                    if (isGranted) {
                        savePhoto()
                    }
                } else if (isGranted) {
                    openImagePicker()
                } else {
                    if (permissionName == permission.READ_MEDIA_IMAGES) {
                        showToast("Permission to read media images not given")
                    } else if (permissionName == permission.READ_EXTERNAL_STORAGE) {
                        showToast("Permission to read external storage not given")
                    }
                }
            }

        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.drawingView)
        drawingView.setSizeForBrush(20.toFloat())

        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.linearLayoutPaintColors)
        mImageButtonCurrentPaint = linearLayoutPaintColors[1] as ImageButton
        mImageButtonCurrentPaint.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_selected)
        )

        findViewById<ImageButton>(R.id.imageBtnBrush).setOnClickListener {
            showBrushSizeChooserDialog()
        }

        findViewById<ImageButton>(R.id.imageBtnGallery).setOnClickListener {
            requestStorageReadPermission()
        }

        findViewById<ImageButton>(R.id.imageBtnUndo).setOnClickListener {
            drawingView.undoDrawing()
        }

        findViewById<ImageButton>(R.id.imageBtnRedo).setOnClickListener {
            drawingView.redoDrawing()
        }

        findViewById<ImageButton>(R.id.imageBtnSave).setOnClickListener {
            requestStorageWritePermission()
        }
    }


    // region Picking an Image from external storage

    private fun requestStorageReadPermission() {
        val minSdk33 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        val permission = if (minSdk33) permission.READ_MEDIA_IMAGES
        else permission.READ_EXTERNAL_STORAGE
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            showRationaleDialog(
                "Kids Drawing App",
                "Need access to external storage to import an image",
                "Request"
            ) {
                permissionLauncher.launch(kotlin.arrayOf(permission))
            }
        } else {
            permissionLauncher.launch(arrayOf(permission))
        }
    }

    private fun openImagePicker() {
        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        openGalleryLauncher.launch(pickIntent)
    }

    // endregion

    // region Saving an Image to external storage

    private fun requestStorageWritePermission() {
        val maxSdk28 = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
        if (maxSdk28) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    permission.WRITE_EXTERNAL_STORAGE
                )
            ) {
                showRationaleDialog(
                    "Kids Drawing App",
                    "Need access to external storage to save the image",
                    "Request"
                ) {
                    permissionLauncher.launch(kotlin.arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
                }
            } else {
                permissionLauncher.launch(arrayOf(permission.WRITE_EXTERNAL_STORAGE))
            }
        } else {
            // No permission required
            savePhoto()
        }
    }

    private fun savePhoto() {
        val flDrawingView: FrameLayout = findViewById(R.id.flDrawingViewContainer)
        val saved = savePhotoToExternalStorage(
            "Simple_Drawing_${System.currentTimeMillis() / 1000}",
            getBitmapFromView(flDrawingView)
        )
        showToast(if (saved) "Drawing saved" else "Something went wrong")
    }


    private fun getBitmapFromView(view: View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)
        return returnedBitmap
    }

    private fun savePhotoToExternalStorage(displayName: String, bmp: Bitmap): Boolean {
        val imageCollection = sdk29AndUp {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } ?: MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val contentValue = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.WIDTH, bmp.width)
            put(MediaStore.Images.Media.HEIGHT, bmp.height)
        }
        return try {
            contentResolver.insert(imageCollection, contentValue)?.also { uri ->
                contentResolver.openOutputStream(uri).use { outputStream ->
                    if (!bmp.compress(Bitmap.CompressFormat.PNG, 95, outputStream)) {
                        throw IOException("Couldn't save bitmap")
                    }
                }
            } ?: throw IOException("Couldn't create MediaStore entry")
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    // endregion

    // region Brush Setup
    private fun showBrushSizeChooserDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size: ")
        val smallBtn = brushDialog.findViewById<ImageButton>(R.id.imageBtnSmallBrush)
        val mediumBtn = brushDialog.findViewById<ImageButton>(R.id.imageBtnMediumBrush)
        val largeBtn = brushDialog.findViewById<ImageButton>(R.id.imageBtnLargeBrush)

        smallBtn.setOnClickListener {
            drawingView.setSizeForBrush(10.toFloat())
            currentBrushSize = 10.toFloat()
            setupSelectedBrushBackground(smallBtn, mediumBtn, largeBtn)
            brushDialog.dismiss()
        }
        mediumBtn.setOnClickListener {
            drawingView.setSizeForBrush(20.toFloat())
            currentBrushSize = 20.toFloat()
            setupSelectedBrushBackground(smallBtn, mediumBtn, largeBtn)
            brushDialog.dismiss()
        }
        largeBtn.setOnClickListener {
            drawingView.setSizeForBrush(30.toFloat())
            currentBrushSize = 30.toFloat()
            setupSelectedBrushBackground(smallBtn, mediumBtn, largeBtn)
            brushDialog.dismiss()
        }
        setupSelectedBrushBackground(smallBtn, mediumBtn, largeBtn)
        brushDialog.show()
    }

    private fun setupSelectedBrushBackground(
        smallBtn: ImageButton,
        mediumBtn: ImageButton,
        largeBtn: ImageButton
    ) {
        when (currentBrushSize) {
            10f -> {
                smallBtn.setColorFilter(androidx.appcompat.R.attr.colorAccent);
                mediumBtn.setColorFilter(androidx.appcompat.R.attr.colorPrimary);
                largeBtn.setColorFilter(androidx.appcompat.R.attr.colorPrimary);
            }

            20f -> {
                smallBtn.setColorFilter(androidx.appcompat.R.attr.colorPrimary);
                mediumBtn.setColorFilter(androidx.appcompat.R.attr.colorAccent);
                largeBtn.setColorFilter(androidx.appcompat.R.attr.colorPrimary);
            }

            30f -> {
                smallBtn.setColorFilter(androidx.appcompat.R.attr.colorPrimary);
                mediumBtn.setColorFilter(androidx.appcompat.R.attr.colorPrimary);
                largeBtn.setColorFilter(androidx.appcompat.R.attr.colorAccent);
            }
        }
    }

    fun paintColorClicked(view: View) {
        if (view != mImageButtonCurrentPaint) {
            val colorTag = view.tag.toString()
            drawingView.setColor(colorTag)
            // unselecting previous
            mImageButtonCurrentPaint.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_normal)
            )
            mImageButtonCurrentPaint = view as ImageButton
            mImageButtonCurrentPaint.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_selected)
            )
        }
    }

    // endregion brush setup


    private fun showRationaleDialog(
        title: String,
        message: String,
        negativeBtn: String? = null,
        negativeBtnOnClick: (() -> Unit)? = null
    ) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        if (negativeBtn != null && negativeBtnOnClick != null) {
            builder.setNegativeButton(negativeBtn) { dialog, _ ->
                negativeBtnOnClick()
                dialog.dismiss()
            }
            builder.create().show()
        }
    }
}