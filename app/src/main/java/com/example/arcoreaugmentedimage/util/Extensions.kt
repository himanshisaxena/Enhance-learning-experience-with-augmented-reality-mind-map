package com.example.arcoreaugmentedimage.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.Settings
import android.view.View
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.arcoreaugmentedimage.MainApplication
import com.example.arcoreaugmentedimage.R
import com.example.arcoreaugmentedimage.base.BaseCustomDialog
import com.example.arcoreaugmentedimage.databinding.DialogMessageBinding
import com.google.android.material.snackbar.Snackbar
import java.io.FileDescriptor
import java.io.IOException
import java.util.*


/**
 * Property of Company's Name, Inc @ 2022 All Rights Reserved.
 */

fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun View.showSnackBar(message: String) {
    Snackbar.make(this, message, Snackbar.LENGTH_LONG).also { snackBar ->
        snackBar.setAction("Ok") {
            snackBar.dismiss()
        }
    }.show()
}

fun Context.getPlayStoreAppLink(): String {
    return "https://play.google.com/store/apps/details?id=${packageName}"
}

fun Context.openPlayStore() {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
    } catch (e: ActivityNotFoundException) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getPlayStoreAppLink())))
    }
}

// hide keyboard
private var inputMethodManager: InputMethodManager? = null

fun hideSoftKeyboard(activity: Activity) {
    if (inputMethodManager == null) inputMethodManager =
        activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    if (activity.currentFocus != null) inputMethodManager!!.hideSoftInputFromWindow(
        activity.currentFocus!!.windowToken,
        0
    )
}

fun showSoftKeyboard(activity: Activity) {
    if (inputMethodManager == null) inputMethodManager =
        activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    if (activity.currentFocus != null) inputMethodManager!!.showSoftInput(activity.currentFocus, 0)
}

fun Activity.setStatusBarColor(color: Int) {
    window.statusBarColor = ContextCompat.getColor(this, color)
}

private var messageDialog: BaseCustomDialog<DialogMessageBinding>? = null

fun Context.showMessageDialog(message: String) {
    messageDialog?.dismiss()
    messageDialog = BaseCustomDialog(
        this,
        R.layout.dialog_message,
        object : BaseCustomDialog.DialogListener {
            override fun onViewClick(view: View?) {
                messageDialog?.dismiss()
            }
        })

    Objects.requireNonNull<Window>(messageDialog?.window).setBackgroundDrawable(
        ColorDrawable(
            Color.TRANSPARENT
        )
    )
    messageDialog?.getBinding()?.tvMessage?.text = message
    messageDialog?.setCancelable(false)
    messageDialog?.show()
}

// hide keyboard

fun Activity.hideSoftKeyboard(view: View) {
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
    imm?.hideSoftInputFromWindow(view.windowToken, 0)
}

fun Context.showPermissionsAlert() {
    val title = getString(R.string.permission_required)
    val message = getString(R.string.permission_msg)
    val positiveText = getString(R.string.permission_goto)
    val negativeText = getString(R.string.cancel)
    showAlert(
        title,
        message,
        positiveText,
        negativeText,
        "",
        object : OnPositive {
            override fun onYes() {
                openSettings()
            }
        })
}

private fun Context.openSettings() {
    val intent = Intent()
    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
    intent.data = Uri.fromParts("package", packageName, null)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    startActivity(intent)
}

@Throws(IOException::class)
fun getBitmapFromUri(uri: Uri): Bitmap {
    val contentResolver = MainApplication.getInstance()!!.contentResolver
    val parcelFileDescriptor: ParcelFileDescriptor =
        contentResolver.openFileDescriptor(uri, "r")!!
    val fileDescriptor: FileDescriptor = parcelFileDescriptor.fileDescriptor
    val image: Bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor)
    parcelFileDescriptor.close()
    return image
}

