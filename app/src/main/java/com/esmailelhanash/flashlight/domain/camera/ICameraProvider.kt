package com.esmailelhanash.flashlight.domain.camera

import androidx.camera.core.Camera

interface ICameraProvider {
    fun getCamera(): Camera

    fun destroyCamera()
}