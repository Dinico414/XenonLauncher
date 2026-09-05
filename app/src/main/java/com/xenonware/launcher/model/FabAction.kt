package com.xenonware.launcher.model

enum class FabAction {
    LOCK_DEVICE,
    TRIGGER_ASSISTANT,
    OPEN_APP,
    OPEN_LINK,
    TOGGLE_FLASHLIGHT,
    OPEN_APP_DRAWER,
    NONE;

    companion object {
        fun fromString(value: String?): FabAction {
            return entries.find { it.name == value } ?: NONE
        }
    }
}
