package com.xenonware.launcher.util

import com.xenonware.launcher.model.AppInfo

fun String.normalizeForSearch(): String = this.lowercase().filter { it.isLetterOrDigit() }

fun String.matchesSearch(query: String): Boolean {
    val normalizedQuery = query.normalizeForSearch()
    if (normalizedQuery.isEmpty()) return query.isBlank()
    return this.normalizeForSearch().contains(normalizedQuery)
}

fun AppInfo.matches(query: String): Boolean {
    val normalizedQuery = query.normalizeForSearch()
    if (normalizedQuery.isEmpty()) return query.isBlank()
    
    // Check app name and custom label
    if (this.name.matchesSearch(query)) return true
    if (this.label.matchesSearch(query)) return true
    
    // Check package name parts to support "settings" finding "com.android.settings"
    val packageParts = this.packageName.split('.')
    val ignoredParts = setOf("com", "android", "google", "net", "org", "launcher", "apps")
    return packageParts.any { part -> 
        val normalizedPart = part.normalizeForSearch()
        if (normalizedPart in ignoredParts && normalizedQuery != normalizedPart) {
            false
        } else {
            normalizedPart.contains(normalizedQuery)
        }
    }
}
