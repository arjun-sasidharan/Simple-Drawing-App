package com.example.simpledrawingapp

import android.widget.Toast
import androidx.activity.ComponentActivity

fun ComponentActivity.showToast(text: String) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}