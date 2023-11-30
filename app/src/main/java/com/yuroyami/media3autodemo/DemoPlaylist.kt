package com.yuroyami.media3autodemo

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

object DemoPlaylist {

    val demoitems = listOf(
        demoItem(""),
        demoItem(""),
        demoItem(""),
        demoItem(""),
        demoItem(""),
        demoItem(""),
        demoItem("")
    )

    fun demoItem(uri: String) = MediaItem.Builder()
            .setMediaId(uri)
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build()
            )
            .build()
}