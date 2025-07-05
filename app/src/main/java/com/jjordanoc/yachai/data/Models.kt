package com.jjordanoc.yachai.data

object Models {
    val GEMMA_3N_E2B_VISION = Model(
        name = "Gemma 3n E2B Vision",
        url = "https://huggingface.co/google/gemma-3n-E2B-it-litert-preview/resolve/main/gemma-3n-E2B-it-int4.task",
        downloadFileName = "gemma-3n-E2B-it-int4.task",
        sizeInBytes = 3_000_000_000L // 3GB approximate size
    )
} 