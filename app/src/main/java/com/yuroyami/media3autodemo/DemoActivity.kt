package com.yuroyami.media3autodemo

import android.content.ComponentName
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.SessionToken
import com.yuroyami.media3autodemo.DemoService.Companion.nodePLAYLIST
import com.yuroyami.media3autodemo.DemoService.Companion.nodeROOT
import com.yuroyami.media3autodemo.ui.theme.Media3AutoDemoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

class DemoActivity : ComponentActivity() {
    private var mediaBrowser: MediaBrowser? = null

    private val items = mutableStateListOf<MediaItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* Screen not to be dimmed or turned off */
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        /* Initializing and launching Media3 components */
        setupMedia3()

        setContent {
            Media3AutoDemoTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val itemsRem = remember { items }

                    LazyColumn(Modifier.fillMaxWidth()) {
                        items(itemsRem) { item ->

                            ListItem(
                                headlineContent = { Text(item.mediaMetadata.title.toString() ) },
                                supportingContent = { Text(item.mediaId, fontSize = 9.sp) },
                                trailingContent = {
                                    Column {
                                        IconButton(onClick = {
                                            mediaBrowser?.sendCustomCommand(
                                                CUSTOM_COM_PLAY_ITEM,
                                                Bundle().apply { putString("id", item.mediaId) }
                                            )
                                        }) {
                                            Icon(Icons.Filled.PlayArrow, "")
                                        }

                                        IconButton(onClick = {
                                            mediaBrowser?.sendCustomCommand(
                                                CUSTOM_COM_PLAYLIST_REMOVE,
                                                Bundle().apply { putString("id", item.mediaId) }
                                            )
                                        }) {
                                            Icon(Icons.Filled.Delete, "")
                                        }
                                    }
                                }
                            )

                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    private fun setupMedia3() {
        /** Initializing MediaController */
        lifecycleScope.launch(Dispatchers.Main) {
            /* Getting a session token that defines our underlying service */
            val sessionToken = SessionToken(
                this@DemoActivity,
                ComponentName(this@DemoActivity, DemoService::class.java)
            )

            /* initializing the media browser using the session token, the service will launch if it hasn't already */
            mediaBrowser = MediaBrowser.Builder(this@DemoActivity, sessionToken)
                .setListener(object : MediaBrowser.Listener {
                    override fun onChildrenChanged(
                        browser: MediaBrowser, parentId: String,
                        itemCount: Int, params: MediaLibraryService.LibraryParams?
                    ) {
                        super.onChildrenChanged(browser, parentId, itemCount, params)

                        val actualItemCount = if (itemCount < 1) 1 else itemCount

                        /** Restoring playlist on app startup (if service is running) */
                        lifecycleScope.launch(Dispatchers.Main) {
                            when (parentId) {
                                nodePLAYLIST -> {
                                    items.clear()

                                    val playlist = browser
                                        .getChildren(nodePLAYLIST, 0, actualItemCount, params)
                                        .await().value ?: emptyList()

                                    items.addAll(playlist)

                                }

                                else -> {}
                            }
                        }
                    }
                })
                .buildAsync().await()

            mediaBrowser?.getLibraryRoot(null)?.await()
            mediaBrowser?.subscribe(nodeROOT, null)?.await()
            mediaBrowser?.subscribe(nodePLAYLIST, null)?.await()
        }
    }
}