package com.github.radupana.featherweight.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

class ExportHandler(
    private val context: Context,
) {
    fun copyFileContent(
        sourceFile: File,
        destinationUri: android.net.Uri,
    ) {
        context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
            sourceFile.inputStream().use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }

    fun copyJsonToClipboard(file: File) {
        val json = file.readText()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Workout Data", json)
        clipboard.setPrimaryClip(clip)
    }

    fun shareJsonFile(file: File) {
        val uri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )

        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Workout Export - ${file.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

        val chooserIntent =
            Intent.createChooser(intent, "Share workout data").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        context.startActivity(chooserIntent)
    }
}
