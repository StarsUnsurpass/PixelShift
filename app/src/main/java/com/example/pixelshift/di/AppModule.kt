package com.example.pixelshift.di

import com.example.pixelshift.data.KotlinImageProcessor
import com.example.pixelshift.domain.ImageProcessor

object AppModule {
    // In a real app, this should be scoped to ViewModel or Activity
    // using Hilt or similar. For now, a singleton is sufficient.
    val imageProcessor: ImageProcessor by lazy { KotlinImageProcessor() }
}
