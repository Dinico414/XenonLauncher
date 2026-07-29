package com.xenonware.launcher.model

data class AppOverride(
    val customName: String? = null,
    val iconPackPackage: String? = null,
    val iconResourceName: String? = null,
    val zoom: Float = 1.0f,
    val backgroundColor: Int? = null,
    val borderColor: Int? = null,
    val borderWidth: Float = 0f
)
