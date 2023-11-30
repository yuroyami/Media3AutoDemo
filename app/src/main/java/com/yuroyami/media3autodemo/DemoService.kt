package com.yuroyami.media3autodemo

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.os.Bundle
import android.util.Log
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionResult.RESULT_SUCCESS
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DemoService : MediaLibraryService() {

    lateinit var player: Player
    private var mediaSession: MediaLibrarySession? = null

    private val serviceIOScope = CoroutineScope(Dispatchers.IO)
    private val serviceMainScope = CoroutineScope(Dispatchers.Main)

    /** The hierarchy in this demo is simple. There is a root invisible item that has one child item
     * which is the playlist node item. The playlist node item contains the visible media items in the app.
     *
     * - ROOT-NODE
     * ----- PLAYLIST (appears as a tab in Android Auto)
     *      ---item1 (appears as list items in the app and Android auto)
     *      ---item2 (idem)
     */
    /** Content styling constants */
    companion object {
        private const val TAG = "DemoService"
        const val nodeROOT = "ROOT"
        const val nodePLAYLIST = "PLAYLIST"
    }

    val rootItem = MediaItem.Builder()
        .setMediaId(nodeROOT)
        .setMediaMetadata(
            MediaMetadata.Builder().setIsBrowsable(true).setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED).setTitle(nodeROOT).build()
        )
        .build()

    val childItem = MediaItem.Builder()
        .setMediaId(nodePLAYLIST)
        .setMediaMetadata(
            MediaMetadata.Builder().setIsBrowsable(true).setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS).setTitle(nodePLAYLIST).build()
        )
        .build()

    var actualTracks = mutableListOf<MediaItem>().also {
        it.addAll(DemoPlaylist.demoitems)
    }

    override fun onCreate() {
        super.onCreate()

        /** Building ExoPlayer to use FFmpeg Audio Renderer and also enable fast-seeking */
        player = ExoPlayer.Builder(applicationContext)
            .setHandleAudioBecomingNoisy(true) /* Prevent annoying noise when changing devices */
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .build()

        player.repeatMode = Player.REPEAT_MODE_ALL

        /** Creating our MediaLibrarySession */
        mediaSession = with(MediaLibrarySession.Builder(
            this, player, object : MediaLibrarySession.Callback {
                override fun onAddMediaItems(mediaSession: MediaSession, controller: MediaSession.ControllerInfo, mediaItems: MutableList<MediaItem>): ListenableFuture<MutableList<MediaItem>> {

                    val finalItems = mediaItems.map {
                        it.buildUpon().setUri(it.mediaId).build()
                    }.toMutableList()

                    return Futures.immediateFuture(finalItems)
                }

                override fun onGetLibraryRoot(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, params: LibraryParams?): ListenableFuture<LibraryResult<MediaItem>> {
                    return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
                }

                override fun onGetChildren(
                    session: MediaLibrarySession, browser: MediaSession.ControllerInfo,
                    parentId: String, page: Int, pageSize: Int, params: LibraryParams?
                ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                    return Futures.immediateFuture(
                        LibraryResult.ofItemList(
                            when (parentId) {
                                nodeROOT -> listOf(childItem)
                                nodePLAYLIST -> actualTracks
                                else -> ImmutableList.of()
                            },
                            LibraryParams.Builder().build()
                        )
                    )
                }

                override fun onSubscribe(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, parentId: String, params: LibraryParams?): ListenableFuture<LibraryResult<Void>> {
                    session.notifyChildrenChanged(
                        parentId,
                        when (parentId) {
                            nodeROOT -> 1
                            nodePLAYLIST -> actualTracks.size
                            else -> 0
                        },
                        params
                    )
                    return Futures.immediateFuture(LibraryResult.ofVoid())
                }

                /* Explicitly permitting custom commands */
                override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
                    val superCall = super.onConnect(session, controller)

                    val sessionComs = superCall.availableSessionCommands
                        .buildUpon()
                        .add(CUSTOM_COM_PLAY_ITEM) //Command executed when an item is requested to play
                        .add(CUSTOM_COM_PLAYLIST_REMOVE) //Command used when removing items from playlist
                        .add(CUSTOM_COM_PLAYLIST_CLEAR) //Command used when clearing all items from playlist
                        .build()

                    return MediaSession.ConnectionResult.accept(sessionComs, superCall.availablePlayerCommands)
                }

                /* Handling custom commands */
                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand,
                    args: Bundle
                ): ListenableFuture<SessionResult> {

                    /** When the controller tries to add an item to the playlist */
                    if (customCommand == CUSTOM_COM_PLAY_ITEM) {

                        args.getString("id")?.let { mediaid ->
                            val i = actualTracks.indexOfFirst { it.mediaId == mediaid }
                            player.setMediaItems(actualTracks, i, 0)
                            player.prepare()
                            player.play()
                            return Futures.immediateFuture(SessionResult(RESULT_SUCCESS))

                        }
                    }

                    /** When the controller tries to remove an item from the playlist */
                    if (customCommand == CUSTOM_COM_PLAYLIST_REMOVE) {
                        (args.getString("id", player.currentMediaItem?.mediaId ?: ""))?.let { mediaid ->
                            actualTracks.firstOrNull { it.mediaId == mediaid }?.let { itemToRemove ->
                                actualTracks.remove(itemToRemove)

                                serviceIOScope.launch {
                                    /** notifying UI-end that the playlist has been modified */
                                    mediaSession?.notifyChildrenChanged(nodePLAYLIST, actualTracks.size, null)
                                }

                                return Futures.immediateFuture(SessionResult(RESULT_SUCCESS))
                            }
                        }
                    }

                    /** When the controller tries to clear the playlist */
                    if (customCommand == CUSTOM_COM_PLAYLIST_CLEAR) {
                        actualTracks.clear()
                        mediaSession?.notifyChildrenChanged(nodePLAYLIST, 0, null)
                        return Futures.immediateFuture(SessionResult(RESULT_SUCCESS))
                    }

                    return super.onCustomCommand(session, controller, customCommand, args)
                }
            }
        )) {
            setId(packageName)
            packageManager?.getLaunchIntentForPackage(packageName)?.let { sessionIntent ->
                setSessionActivity(
                    PendingIntent.getActivity(
                        /* context= */ this@DemoService,
                        /* requestCode= */ 0,
                        sessionIntent,
                        FLAG_IMMUTABLE
                    )
                )
            }
            build()
        }

        /** Listening to some player events */
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
//
//                Log.e(TAG, mediaItem?.mediaId ?: "Empty Media Id")
//
//                val index = actualTracks.indexOfFirst { it.mediaId == mediaItem?.mediaId }
//                player.setMediaItems(actualTracks, index, 0)
//                player.prepare()
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, error.stackTraceToString())
            }
        })
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}