package com.example.arcoreaugmentedimage.util

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.example.arcoreaugmentedimage.models.NodeModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class PreferenceProvider(context: Context) {

    private val appContext = context.applicationContext

    private val preferences: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(appContext)

    fun clear(): Boolean {
        return preferences.edit().clear().commit()
    }

    fun setNodesList(list: MutableList<NodeModel>) {
        val gson = Gson()
        val json = gson.toJson(list)
        return preferences.edit().putString(
            Constants.SAVED_NODES_LIST,
            json
        ).apply()
    }

    fun getNodesList(): MutableList<NodeModel> {
        var arrayItems: MutableList<NodeModel> = mutableListOf()
        val serializedObject: String? = preferences.getString(Constants.SAVED_NODES_LIST, null)
        val gson = Gson()
        val type: Type = object : TypeToken<MutableList<NodeModel>>() {}.type
        serializedObject?.let {
            arrayItems = gson.fromJson(serializedObject, type)
        }
        return arrayItems
    }
}