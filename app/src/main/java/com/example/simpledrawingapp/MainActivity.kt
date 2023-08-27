package com.example.simpledrawingapp

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get

class MainActivity : AppCompatActivity() {

    private lateinit var drawingView: DrawingView
    private lateinit var mImageButtonCurrentPaint: ImageButton

    private val openGalleryLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val imageBackground: ImageView = findViewById(R.id.ivBackground)
            imageBackground.setImageURI(result.data?.data)
        }
    }

    private val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value

                if (isGranted) {
                    if (permissionName == Manifest.permission.READ_MEDIA_IMAGES) {
                        Toast.makeText(
                            this,
                            "Permission to read media images granted",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else if (permissionName == Manifest.permission.READ_EXTERNAL_STORAGE) {
                        Toast.makeText(
                            this,
                            "Permission to read external storage  granted",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    openImagePicker()
                } else {
                    if (permissionName == Manifest.permission.READ_MEDIA_IMAGES) {
                        Toast.makeText(
                            this,
                            "Permission to read media images not given",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else if (permissionName == Manifest.permission.READ_EXTERNAL_STORAGE) {
                        Toast.makeText(
                            this,
                            "Permission to read external storage not given",
                            Toast.LENGTH_SHORT
                        ).show()
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestStoragePermission()
            } else {
                requestStoragePermissionForApiBelow33()
            }
        }
        
        findViewById<ImageButton>(R.id.imageBtnUndo).setOnClickListener {
            drawingView.undoDrawing()
        }

        findViewById<ImageButton>(R.id.imageBtnRedo).setOnClickListener {
            drawingView.redoDrawing()
        }
    }

    private fun openImagePicker() {
        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        openGalleryLauncher.launch(pickIntent)
    }

    private fun showBrushSizeChooserDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size: ")
        val smallBtn = brushDialog.findViewById<ImageButton>(R.id.imageBtnSmallBrush)
        smallBtn.setOnClickListener {
            drawingView.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }
        val mediumBtn = brushDialog.findViewById<ImageButton>(R.id.imageBtnMediumBrush)
        mediumBtn.setOnClickListener {
            drawingView.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }
        val largeBtn = brushDialog.findViewById<ImageButton>(R.id.imageBtnLargeBrush)
        largeBtn.setOnClickListener {
            drawingView.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()
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

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        ) {
            showRationaleDialog(
                "Kids Drawing App",
                "Need access to external storage to import an image"
            )
        } else {
            requestPermission.launch(
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES
                )
            )
        }
    }

    private fun requestStoragePermissionForApiBelow33() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            showRationaleDialog(
                "Kids Drawing App",
                "Need access to external storage to import an image"
            )
        } else {
            requestPermission.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            )
        }
    }

    private fun showRationaleDialog(
        title: String,
        message: String
    ) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }
}