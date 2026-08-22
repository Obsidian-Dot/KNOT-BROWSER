package com.wormhole.browser.core.settings

enum class ThemeMode(val id: String, val displayName: String) {
    SYSTEM(id = "system", displayName = "System default"),
    DARK(id = "dark", displayName = "Dark");

    companion object {
        val DEFAULT = SYSTEM

        fun fromId(id: String?): ThemeMode =
            entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
