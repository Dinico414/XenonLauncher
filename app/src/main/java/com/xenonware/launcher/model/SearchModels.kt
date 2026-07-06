package com.xenonware.launcher.model

import android.graphics.Bitmap
import android.net.Uri

sealed class SearchResult {
    data class App(val appInfo: AppInfo) : SearchResult()
    data class Contact(val id: String, val name: String, val phoneNumber: String, val photoUri: Uri?) : SearchResult()
    data class File(val name: String, val path: String, val uri: Uri, val mimeType: String, val preview: Bitmap? = null) : SearchResult()
    data class Web(val query: String, val isUrl: Boolean) : SearchResult()
}
