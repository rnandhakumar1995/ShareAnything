package me.nandroid.shareanything

import java.io.File

sealed class ShareAnythingData {
    open class ShareAnythingFile(val data: File) : ShareAnythingData()
    open class ShareAnythingMultipleFiles(val data: List<ShareAnythingFile>) : ShareAnythingData()
    open class ShareAnythingText(val data: String) : ShareAnythingData()
    object ShareAnythingEmpty : ShareAnythingData()
}