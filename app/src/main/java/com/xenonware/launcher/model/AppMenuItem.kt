package com.xenonware.launcher.model

enum class AppMenuItem(val id: String) {
    UNINSTALL("uninstall"),
    APP_INFO("app_info"),
    EDIT("edit"),
    HIDE("hide"),
    SPLIT_SCREEN("split_screen");

    companion object {
        fun fromId(id: String): AppMenuItem? = entries.find { it.id == id }
        val DEFAULT_ORDER = listOf(UNINSTALL, APP_INFO, EDIT, HIDE, SPLIT_SCREEN)
    }
}
