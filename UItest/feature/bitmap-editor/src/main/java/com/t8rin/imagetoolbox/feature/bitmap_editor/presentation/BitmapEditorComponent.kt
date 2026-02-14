package com.t8rin.imagetoolbox.feature.bitmap_editor.presentation

import com.arkivanov.decompose.ComponentContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class BitmapEditorComponent @AssistedInject internal constructor(
    @Assisted componentContext: ComponentContext
) : ComponentContext by componentContext {

    @AssistedFactory
    fun interface Factory {
        operator fun invoke(
            componentContext: ComponentContext
        ): BitmapEditorComponent
    }

}