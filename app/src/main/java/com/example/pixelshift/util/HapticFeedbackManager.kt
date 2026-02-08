package com.example.pixelshift.util

import android.view.HapticFeedbackConstants
import android.view.View

object HapticFeedbackManager {
    fun performHapticFeedback(view: View, enabled: Boolean) {
        if (enabled) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }
}
