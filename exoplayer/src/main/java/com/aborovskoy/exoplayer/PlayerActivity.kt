package com.aborovskoy.exoplayer

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.Player.DefaultEventListener
import com.google.android.exoplayer2.audio.AudioRendererEventListener
import com.google.android.exoplayer2.decoder.DecoderCounters
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.dash.DashChunkSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelection
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoRendererEventListener
import kotlinx.android.synthetic.main.activity_player.*

private val TAG = PlayerActivity::class.java.simpleName

class PlayerActivity : AppCompatActivity() {


    // bandwidth meter to measure and estimate bandwidth
    private val BANDWIDTH_METER = DefaultBandwidthMeter()

    private var player: SimpleExoPlayer? = null

    private var playbackPosition: Long = 0
    private var currentWindow = 0
    private var playWhenReady = true
    private var componentListener: ComponentListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        componentListener = ComponentListener()
    }

    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            initPlayer()
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUi()
        if (Util.SDK_INT <= 23 || player == null) {
            initPlayer()
        }
    }

    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            releasePlayer()
        }
    }

    private fun initPlayer() {
        if (player == null) { // a factory to create an AdaptiveVideoTrackSelection
            val adaptiveTrackSelectionFactory: TrackSelection.Factory =
                AdaptiveTrackSelection.Factory(BANDWIDTH_METER)
            player = ExoPlayerFactory.newSimpleInstance(
                DefaultRenderersFactory(this),
                DefaultTrackSelector(adaptiveTrackSelectionFactory),
                DefaultLoadControl()
            )
            videoView.player = player
            player?.apply {
                addListener(componentListener)
                addAudioDebugListener(componentListener)
                addVideoDebugListener(componentListener)
                this.playWhenReady = playWhenReady
                seekTo(currentWindow, playbackPosition)
            }
        }

        val uri = Uri.parse(getString(R.string.media_url_dash))
        val mediaSource = buildMediaSource(uri)
        player?.prepare(mediaSource, true, true)
    }

    private fun releasePlayer() {
        player?.let {
            playbackPosition = it.currentPosition
            currentWindow = it.currentWindowIndex
            playWhenReady = it.playWhenReady
            it.removeListener(componentListener)
            it.removeAudioDebugListener(componentListener)
            it.removeVideoDebugListener(componentListener)
            it.release()
        }
        player = null
    }

    private fun buildMediaSource(uri: Uri): MediaSource {
        val manifestDataSourceFactory: DataSource.Factory =
            DefaultHttpDataSourceFactory("exoplayer-codelab")
        val dashChunkSourceFactory: DashChunkSource.Factory = DefaultDashChunkSource.Factory(
            DefaultHttpDataSourceFactory("exoplayer-codelab", BANDWIDTH_METER)
        )
        return DashMediaSource.Factory(
            dashChunkSourceFactory,
            manifestDataSourceFactory
        ).createMediaSource(uri)
    }


    @SuppressLint("InlinedApi")
    private fun hideSystemUi() {
        videoView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
    }

    private class ComponentListener : DefaultEventListener(),
        AudioRendererEventListener, VideoRendererEventListener {
        override fun onPlayerStateChanged(
            playWhenReady: Boolean,
            playbackState: Int
        ) {
            val stateString: String = when (playbackState) {
                Player.STATE_IDLE -> "Player.STATE_IDLE      -"
                Player.STATE_BUFFERING -> "Player.STATE_BUFFERING -"
                Player.STATE_READY -> "Player.STATE_READY     -"
                Player.STATE_ENDED -> "Player.STATE_ENDED     -"
                else -> "UNKNOWN_STATE          -"
            }
            Log.d(
                TAG,
                "changed state to $stateString playWhenReady: $playWhenReady"
            )
        }

        override fun onAudioEnabled(counters: DecoderCounters) {
            Log.d(TAG, "onAudioEnabled")
        }

        override fun onAudioSessionId(audioSessionId: Int) {
            Log.d(TAG, "onAudioSessionId -> audioSessionId: $audioSessionId")
        }

        override fun onAudioDecoderInitialized(
            decoderName: String,
            initializedTimestampMs: Long,
            initializationDurationMs: Long
        ) {
            Log.d(
                TAG, "onAudioDecoderInitialized -> decoderName: " + decoderName +
                        "; initializedTimestampMs: " + initializedTimestampMs +
                        "; initializationDurationMs: " + initializationDurationMs
            )
        }

        override fun onAudioInputFormatChanged(format: Format) {
            Log.d(TAG, "onAudioInputFormatChanged")
        }

        override fun onAudioSinkUnderrun(
            bufferSize: Int,
            bufferSizeMs: Long,
            elapsedSinceLastFeedMs: Long
        ) {
            Log.d(
                TAG, "onAudioSinkUnderrun -> bufferSize: " + bufferSize +
                        "; bufferSizeMs: " + bufferSizeMs +
                        "; elapsedSinceLastFeedMs: " + elapsedSinceLastFeedMs
            )
        }

        override fun onAudioDisabled(counters: DecoderCounters) {
            Log.d(TAG, "onAudioDisabled")
        }

        override fun onVideoEnabled(counters: DecoderCounters) {
            Log.d(TAG, "onVideoEnabled")
        }

        override fun onVideoDecoderInitialized(
            decoderName: String,
            initializedTimestampMs: Long,
            initializationDurationMs: Long
        ) {
            Log.d(
                TAG, "onVideoDecoderInitialized -> decoderName: " + decoderName +
                        "; initializedTimestampMs: " + initializedTimestampMs +
                        "; initializationDurationMs: " + initializationDurationMs
            )
        }

        override fun onVideoInputFormatChanged(format: Format) {
            Log.d(TAG, "onVideoInputFormatChanged")
        }

        override fun onDroppedFrames(count: Int, elapsedMs: Long) {
            Log.d(
                TAG,
                "onDroppedFrames -> count: $count; elapsedMs: $elapsedMs"
            )
        }

        override fun onVideoSizeChanged(
            width: Int,
            height: Int,
            unappliedRotationDegrees: Int,
            pixelWidthHeightRatio: Float
        ) {
            Log.d(
                TAG, "onVideoSizeChanged -> width: " + width + "; height: " + height +
                        "; unappliedRotationDegrees: " + unappliedRotationDegrees +
                        "; pixelWidthHeightRatio: " + pixelWidthHeightRatio
            )
        }

        override fun onRenderedFirstFrame(surface: Surface) {
            Log.d(TAG, "onRenderedFirstFrame")
        }

        override fun onVideoDisabled(counters: DecoderCounters) {
            Log.d(TAG, "onVideoDisabled")
        }
    }
}
