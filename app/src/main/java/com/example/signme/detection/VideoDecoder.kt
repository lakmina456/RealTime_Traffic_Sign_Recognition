package com.example.signme.detection

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri

class VideoDecoder(private val context: Context, private val videoPath: Int) {

    private val retriever = MediaMetadataRetriever()

    init {
        val uri = Uri.parse("android.resource://${context.packageName}/$videoPath")
        retriever.setDataSource(context, uri)
    }

    fun getFrameAtTime(timeUs: Long): Bitmap? {
        return retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
    }

    fun getDuration(): Long {
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        return duration?.toLongOrNull() ?: 0L
    }

    fun release() {
        retriever.release()
    }
}
