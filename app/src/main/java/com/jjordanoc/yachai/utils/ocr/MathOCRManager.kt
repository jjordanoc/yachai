package com.jjordanoc.yachai.utils.ocr

import android.content.Context
import android.graphics.Bitmap
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class MathOCRManager(context: Context) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun extractMathFromImage(bitmap: Bitmap): Task<Text> {
        val image = InputImage.fromBitmap(bitmap, 0)
        return recognizer.process(image)
    }
} 