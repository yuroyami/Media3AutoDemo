package com.yuroyami.media3autodemo

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

object DemoPlaylist {

    val demoitems = listOf(
        demoItem(
            "https://archive.org/download/lp_the-nocturnes_arthur-rubinstein-frdric-chopin/disc1/01.01.%20Op.%209%2C%20No.%201.mp3",
            "Chopin Nocturne Op.9 No.1"
        ),
        demoItem(
            "https://archive.org/download/lp_the-nocturnes_arthur-rubinstein-frdric-chopin/disc1/01.02.%20Op.%209%2C%20No.%202.mp3",
            "Chopin Nocturne Op.9 No.2"
        ),
        demoItem(
            "https://archive.org/download/lp_the-nocturnes_arthur-rubinstein-frdric-chopin/disc1/02.02.%20Op.%2027%2C%20No.%201.mp3",
            "Chopin Nocturne Op.27 No.1"
        ),
        demoItem(
            "https://archive.org/download/lp_the-nocturnes_arthur-rubinstein-frdric-chopin/disc2/03.03.%20Op.%2048%2C%20No.%201.mp3",
            "Chopin Nocturne Op.48 No.1"
        ),
        demoItem(
            "https://ia804608.us.archive.org/2/items/lp_the-nocturnes_arthur-rubinstein-frdric-chopin/disc2/03.04.%20Op.%2048%2C%20No.%202.mp3",
            "Chopin Nocturne Op.48 No.2"
        ),
        demoItem(
            "https://archive.org/download/lp_the-nocturnes_arthur-rubinstein-frdric-chopin/disc2/04.01.%20Op.%2055%2C%20No.%201.mp3",
            "Chopin Nocturne Op.55 No.1"
        ),
        demoItem(
            "https://archive.org/download/lp_chopin-waltzes-complete_witold-malcuzynsky/disc1/02.02.%20No.%2010%20In%20B%20Minor%2C%20Op.%2069%20No.%202%3B%20No.%2011%20In%20G%20Flat%20Major%2C%20Op.%2070%20No.%201.mp3",
            "Chopin Waltz Op.69 No.2"
        ),
        demoItem(
            "https://archive.org/download/lp_the-story-of-great-music-the-romantic_hector-berlioz-frdric-chopin-lizst-feli/disc1/02.03.%20Ballade%20No.%201%20In%20G%20Minor%2C%20Op.%2023.mp3",
            "Chopin Ballade No.1 (Op.23)"
        ),
    )

    fun demoItem(uri: String, title: String) = MediaItem.Builder()
        .setMediaId(uri)
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .setTitle(title)
                .build()
        )
        .build()
}