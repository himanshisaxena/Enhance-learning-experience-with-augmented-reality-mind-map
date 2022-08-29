package com.example.arcoreaugmentedimage.base

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.example.arcoreaugmentedimage.BR
import com.example.arcoreaugmentedimage.R

/**
 * Property of Company's Name, Inc @ 2022 All Rights Reserved.
 */

class BaseCustomDialog<V : ViewDataBinding>(context: Context) :
    Dialog(context, R.style.CustomDialog) {

    private var binding: V? = null

    @LayoutRes
    private var layoutId = 0
    private var listener: DialogListener? = null

    constructor(
        context: Context,
        @LayoutRes layoutId: Int,
        listener: DialogListener
    ) : this(context) {
        this.layoutId = layoutId
        this.listener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()
        setContentView(binding!!.root)
    }

    fun getBinding(): V {
        init()
        return binding!!
    }

    private fun init() {
        if (binding == null) binding =
            DataBindingUtil.inflate(LayoutInflater.from(context), layoutId, null, false)
        if (listener != null) binding!!.setVariable(BR.callback, listener)
    }

    interface DialogListener {
        fun onViewClick(view: View?)
    }
}