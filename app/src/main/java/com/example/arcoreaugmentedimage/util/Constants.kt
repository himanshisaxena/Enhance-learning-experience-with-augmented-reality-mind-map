package com.example.arcoreaugmentedimage.util

import android.Manifest

object Constants {

    val CAMERA_AND_READ_STORAGE_PERMISSION = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    const val SAVED_NODES_LIST = "saved_nodes_list"

}