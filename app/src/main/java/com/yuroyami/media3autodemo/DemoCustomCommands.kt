package com.yuroyami.media3autodemo

import android.os.Bundle
import androidx.media3.session.SessionCommand

val CUSTOM_COM_PLAY_ITEM = SessionCommand("demo_play_item", Bundle())

val CUSTOM_COM_PLAYLIST_REMOVE = SessionCommand("demo_playlist_remove", Bundle())

val CUSTOM_COM_PLAYLIST_CLEAR = SessionCommand("demo_playlist_clear", Bundle())

