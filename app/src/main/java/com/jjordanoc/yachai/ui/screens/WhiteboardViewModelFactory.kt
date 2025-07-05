package com.jjordanoc.yachai.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class WhiteboardViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WhiteboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WhiteboardViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 