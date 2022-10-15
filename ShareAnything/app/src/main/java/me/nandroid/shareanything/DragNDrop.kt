package me.nandroid.shareanything

import android.app.Activity
import android.net.Uri
import android.view.DragEvent
import android.view.View

sealed class ClipDataContent {
    open class FileClipData(val data: Uri) : ClipDataContent()
    data class ImageClipData(val imageData: Uri) : FileClipData(imageData)
    open class TextClipData(val data: String) : ClipDataContent()
    data class HtmlClipData(val htmlData: String) : TextClipData(htmlData)
}

fun DragEvent.toClipDataContents(): Array<ClipDataContent?> {
    return Array(clipData.itemCount) { index ->
        val item = clipData.getItemAt(index)
        if (clipDescription.hasMimeType("image/*")) {
            return@Array ClipDataContent.ImageClipData(item.uri)
        } else if (clipDescription.hasMimeType("text/plain")) {
            return@Array if (item.text != null)
                ClipDataContent.TextClipData(item.text.toString())
            else
                ClipDataContent.FileClipData(item.uri)
        } else if (clipDescription.hasMimeType("text/html")) {
            return@Array ClipDataContent.HtmlClipData(item.htmlText)
        } else if (item.uri != null) {
            return@Array ClipDataContent.FileClipData(item.uri)
        }
        return@Array null
    }
}

abstract class DragEvents {
    open fun onDragStarted(event: DragEvent) = Unit
    fun onDragLocation(event: DragEvent) = Unit
    fun onDragEntered(event: DragEvent) = Unit
    fun onDragExited(event: DragEvent) = Unit
    open fun onDragEnded(event: DragEvent) = Unit
    fun onDrop(event: DragEvent, activity: Activity) {
        val dragAndDropPermissions = activity.requestDragAndDropPermissions(event)
        onClipDataDropped(event)
        dragAndDropPermissions?.release()
    }

    abstract fun onClipDataDropped(event: DragEvent)
}

fun View.prepareAsDropTarget(activity: Activity, dragEvents: DragEvents) {
    setOnDragListener { _, event ->
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> dragEvents.onDragStarted(event)
            DragEvent.ACTION_DRAG_LOCATION -> dragEvents.onDragLocation(event)
            DragEvent.ACTION_DRAG_ENTERED -> dragEvents.onDragEntered(event)
            DragEvent.ACTION_DRAG_EXITED -> dragEvents.onDragExited(event)
            DragEvent.ACTION_DRAG_ENDED -> dragEvents.onDragEnded(event)
            DragEvent.ACTION_DROP -> dragEvents.onDrop(event, activity)
        }
        return@setOnDragListener true
    }
}
