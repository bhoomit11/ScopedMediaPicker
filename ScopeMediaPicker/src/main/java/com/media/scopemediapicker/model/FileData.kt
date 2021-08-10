package com.media.scopemediapicker.model

import android.net.Uri
import java.io.InputStream

data class FileData(
    // To open document use this URI
    var fileUri: Uri? = null,

    // To upload a file use this Inputstream
    var fileInputStream: InputStream? = null,

    var fileName: String? = "",
    var fileExtension: String? = "",
    var mimeType: String? = ""
)