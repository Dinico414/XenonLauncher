package com.xenonware.launcher.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.automirrored.rounded.TextSnippet
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.CoPresent
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContactPage
import androidx.compose.material.icons.rounded.DesignServices
import androidx.compose.material.icons.rounded.DesktopMac
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Poll
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.TableView
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class FileTypeInfo(
    val icon: ImageVector,
    val color: Color
)

fun getFileTypeInfo(mimeType: String, fileName: String): FileTypeInfo {
    val m = mimeType.lowercase()
    val name = fileName.lowercase()

    return when {
        // Android Installers (.apk, .apks, .xapk, .aab)
        m == "application/vnd.android.package-archive" || name.matches(".*\\.(apk|apks|xapk|aab)$".toRegex()) ->
            FileTypeInfo(Icons.Rounded.Android, Color(0xFF3DDC84))

        // Windows Executables & Installers (.exe, .msi, .com)
        name.matches(".*\\.(exe|msi|com)$".toRegex()) ->
            FileTypeInfo(Icons.Rounded.DesktopWindows, Color(0xFF00A4EF))

        // macOS Packages & Apps (.dmg, .pkg, .app, .ipa)
        name.matches(".*\\.(dmg|pkg|app|ipa)$".toRegex()) ->
            FileTypeInfo(Icons.Rounded.DesktopMac, Color(0xFF6E6E73))

        // Linux Packages & Binaries (.deb, .rpm, .appimage, .bin, .run, .elf)
        name.matches(".*\\.(deb|rpm|appimage|bin|run|elf)$".toRegex()) ->
            FileTypeInfo(Icons.Rounded.DeveloperBoard, Color(0xFFE95420))

        // Scripts & Shells (.bat, .cmd, .ps1, .sh, .bash, .zsh, .fish, .vbs, .wsf)
        name.matches(".*\\.(bat|cmd|ps1|sh|bash|zsh|fish|vbs|wsf|pl|rb)$".toRegex()) ->
            FileTypeInfo(Icons.Rounded.Terminal, Color(0xFF37474F))

        // Video
        m.startsWith("video/") || name.matches(".*\\.(mp4|mkv|avi|mov|wmv|flv|webm|m4v|3gp|mpeg|mpg)$".toRegex()) ->
            FileTypeInfo(Icons.Rounded.Movie, Color(0xFFEF5350))

        // Music / Audio
        m.startsWith("audio/") || name.matches(".*\\.(mp3|wav|flac|aac|ogg|m4a|wma|opus|mid|midi)$".toRegex()) ->
            FileTypeInfo(Icons.Rounded.AudioFile, Color(0xFFFFB74D))

        // Images / Pictures
        m.startsWith("image/") && !name.endsWith(".svg") && !name.endsWith(".ai") && !name.endsWith(".eps") ->
            FileTypeInfo(Icons.Rounded.Image, Color(0xFFB39DDB))

        // Vector / Vektor (SVG, AI, EPS, CDR)
        m == "image/svg+xml" || name.matches(".*\\.(svg|ai|eps|cdr)$".toRegex()) ->
            FileTypeInfo(Icons.Rounded.DesignServices, Color(0xFFFF7043))

        // PDF
        m == "application/pdf" || name.endsWith(".pdf") ->
            FileTypeInfo(Icons.Rounded.PictureAsPdf, Color(0xFFD32F2F))

        // Google Docs (.gdoc)
        m.contains("vnd.google-apps.document") || name.endsWith(".gdoc") ->
            FileTypeInfo(Icons.AutoMirrored.Rounded.Notes, Color(0xFF4285F4))

        // Microsoft Word / Document (.doc, .docx, .odt, .rtf)
        m.contains("wordprocessingml") || m.contains("msword") || name.matches(".*\\.(doc|docx|odt|rtf|dot|dotx)$".toRegex()) ->
            FileTypeInfo(Icons.AutoMirrored.Rounded.Article, Color(0xFF2B579A))

        // Google Sheets (.gsheet)
        m.contains("vnd.google-apps.spreadsheet") || name.endsWith(".gsheet") ->
            FileTypeInfo(Icons.Rounded.TableView, Color(0xFF0F9D58))

        // Microsoft Excel / Spreadsheet (.xls, .xlsx, .csv, .ods)
        m.contains("spreadsheet") || m.contains("excel") || m.contains("csv") || name.matches(".*\\.(xls|xlsx|csv|ods|xlsm)$".toRegex()) ->
            FileTypeInfo(Icons.Rounded.TableChart, Color(0xFF107C41))

        // Google Slides (.gslide)
        m.contains("vnd.google-apps.presentation") || name.endsWith(".gslide") ->
            FileTypeInfo(Icons.Rounded.CoPresent, Color(0xFFF4B400))

        // Microsoft PowerPoint (.ppt, .pptx, .odp)
        m.contains("presentation") || m.contains("powerpoint") || name.matches(".*\\.(ppt|pptx|odp|pps)$".toRegex()) ->
            FileTypeInfo(Icons.Rounded.Slideshow, Color(0xFFC43E1C))

        // Google Forms (.gform)
        m.contains("vnd.google-apps.form") || name.endsWith(".gform") ->
            FileTypeInfo(Icons.Rounded.Poll, Color(0xFF673AB7))

        // Google Drawings (.gdraw)
        m.contains("vnd.google-apps.drawing") || name.endsWith(".gdraw") ->
            FileTypeInfo(Icons.Rounded.Brush, Color(0xFFDB4437))

        // MyMap / Maps
        name.matches(".*\\.(kml|kmz|geojson|gpx)$".toRegex()) || name.contains("mymap") ->
            FileTypeInfo(Icons.Rounded.Map, Color(0xFF00897B))

        // Contact Cards (.vcf, contactcards)
        m.contains("vcard") || name.matches(".*\\.(vcf|contact|contacts)$".toRegex()) ->
            FileTypeInfo(Icons.Rounded.ContactPage, Color(0xFF7CB342))

        // Ebook (.epub, .mobi, .azw3, .fb2)
        m.contains("epub") || name.matches(".*\\.(epub|mobi|azw3|fb2)$".toRegex()) ->
            FileTypeInfo(Icons.AutoMirrored.Rounded.MenuBook, Color(0xFF8D6E63))

        // Archive (zip, rar, 7z, tar, gz)
        m.contains("zip") || m.contains("rar") || m.contains("7z") || m.contains("tar") || m.contains("compressed") ||
                name.matches(".*\\.(zip|rar|7z|tar|gz|bz2|xz|tgz)$".toRegex()) ->
            FileTypeInfo(Icons.Rounded.FolderZip, Color(0xFF78909C))

        // 3D Files (.obj, .stl, .gltf, .glb, .fbx, .3ds)
        name.matches(".*\\.(obj|stl|gltf|glb|fbx|3ds|dae|ply)$".toRegex()) ->
            FileTypeInfo(Icons.Rounded.ViewInAr, Color(0xFF3F51B5))

        // Code Files (.json, .xml, .yml, .yaml, .ini, .cfg, .conf, .properties)
        name.matches(".*\\.(json|xml|yml|yaml|ini|cfg|conf|properties|gradle|kt|java|py|html|css|js|ts)$".toRegex()) ->
            FileTypeInfo(Icons.Rounded.Code, Color(0xFF7C4DFF))

        // Plaintext / Text (.txt, .md, .log)
        m.startsWith("text/") || name.matches(".*\\.(txt|md|log)$".toRegex()) ->
            FileTypeInfo(Icons.AutoMirrored.Rounded.TextSnippet, Color(0xFF0288D1))

        // Generic document fallback
        else -> FileTypeInfo(Icons.AutoMirrored.Rounded.InsertDriveFile, Color(0xFF757575))
    }
}
