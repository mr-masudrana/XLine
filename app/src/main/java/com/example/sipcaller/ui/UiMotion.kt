package com.example.sipcaller.ui

import android.view.View
import android.view.animation.AnimationUtils

object UiMotion {
    fun enter(view: View) {
        view.startAnimation(AnimationUtils.loadAnimation(view.context, com.example.sipcaller.R.anim.fade_in))
    }
    fun press(view: View) {
        view.animate().cancel()
        view.animate().scaleX(.97f).scaleY(.97f).setDuration(55).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(110).start()
        }.start()
    }
    fun reveal(view: View) {
        view.alpha = 0f
        view.scaleX = .97f
        view.scaleY = .97f
        view.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(180).start()
    }
}
