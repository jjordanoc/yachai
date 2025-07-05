package com.jjordanoc.yachai.ui.screens

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class WhiteboardViewModelFactory(
    private val application: Application,
    private val useGpu: Boolean
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WhiteboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WhiteboardViewModel(application, useGpu) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 