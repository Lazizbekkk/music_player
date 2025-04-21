package uz.gita.musicapp.servise

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import uz.gita.musicapp.MainActivity
import uz.gita.musicapp.R
import uz.gita.musicapp.data.ControllerEnums
import uz.gita.musicapp.data.MusicData
import uz.gita.musicapp.utils.AppManager
import uz.gita.musicapp.utils.getMusicDataByPosition
import java.io.File

class MyService : Service() {
    override fun onBind(p0: Intent?): IBinder? = null
    private val CHANNEL = "DEMO"
    private var _mediaPlayer: MediaPlayer? = null
    private val mediaPlayer get() = _mediaPlayer!!
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var job: Job? = null
    private var timer: CountDownTimer? = null
    private var mediaSession: MediaSessionCompat? = null

    override fun onCreate() {
        super.onCreate()
        _mediaPlayer = MediaPlayer()
        createMediaSession()
        createChannel()
        startMyService()
    }

    private fun startMyService() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Music player")
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCustomContentView(createRemoteView())
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .build()

        startForeground(1, notification)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel =
                NotificationChannel(CHANNEL, CHANNEL, NotificationManager.IMPORTANCE_DEFAULT)
            channel.setSound(null, null)
            val service = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            service.createNotificationChannel(channel)
        }
    }

    private fun createRemoteView(): RemoteViews {
        val view = RemoteViews(this.packageName, R.layout.my_remote_view)

        val musicData = AppManager.cursor?.getMusicDataByPosition(AppManager.selectMusicPos)

        if (musicData != null) {
            view.setTextViewText(R.id.textMusicName, musicData.title)
            view.setTextViewText(R.id.textArtistName, musicData.artist)
            if (mediaPlayer.isPlaying) {
                view.setImageViewResource(R.id.buttonManage, R.drawable.ic_pause)
            } else {
                view.setImageViewResource(R.id.buttonManage, R.drawable.ic_play)
            }
        } else {
            // Handle the case when musicData is null
            view.setTextViewText(R.id.textMusicName, "No music selected")
            view.setTextViewText(R.id.textArtistName, "Unknown artist")
            view.setImageViewResource(R.id.buttonManage, R.drawable.ic_play) // Default icon
        }

        // Set up the buttons' click listeners
        view.setOnClickPendingIntent(R.id.buttonPrev, createPendingIntent(ControllerEnums.PREV))
        view.setOnClickPendingIntent(R.id.buttonManage, createPendingIntent(ControllerEnums.MANAGE))
        view.setOnClickPendingIntent(R.id.buttonNext, createPendingIntent(ControllerEnums.NEXT))
        view.setOnClickPendingIntent(R.id.buttonCancel, createPendingIntent(ControllerEnums.CANCEL))

        return view
    }


    private fun createPendingIntent(action: ControllerEnums): PendingIntent {
        val intent = Intent(this, MyService::class.java)
        intent.putExtra("COMMAND", action)

        return PendingIntent.getService(this, action.amount, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val command = intent?.extras?.getSerializable("COMMAND") as ControllerEnums
        doneCommand(command)
        return START_NOT_STICKY
    }

    private fun doneCommand(command: ControllerEnums) {
        // Check if cursor is null or empty, or if selectMusicPos is out of bounds
        if (AppManager.cursor == null || AppManager.cursor!!.count == 0 || AppManager.selectMusicPos < 0 || AppManager.selectMusicPos >= AppManager.cursor!!.count) {
            // Handle the case where the cursor is null or selectMusicPos is out of bounds
            Log.e("MyService", "Invalid cursor or selectMusicPos")
            return
        }

        // Safely retrieve music data
        val data: MusicData = AppManager.cursor!!.getMusicDataByPosition(AppManager.selectMusicPos) ?: run {
            // Handle the case where musicData is null
            Log.e("MyService", "MusicData is null at position: ${AppManager.selectMusicPos}")
            return
        }

        when (command) {
            ControllerEnums.MANAGE -> {
                if (mediaPlayer.isPlaying) doneCommand(ControllerEnums.PAUSE)
                else doneCommand(ControllerEnums.PLAY)
            }

            ControllerEnums.PLAY -> {
                if (mediaPlayer.isPlaying) mediaPlayer.stop()
                _mediaPlayer = MediaPlayer.create(this, Uri.fromFile(File(data.data ?: "")))
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener {
                    doneCommand(ControllerEnums.NEXT)
                }
                AppManager.fullTime = data.duration!!
                mediaPlayer.seekTo(AppManager.currentTime.toInt())
                job?.let { it.cancel() }
                job = scope.launch {
                    changeProgress().collectLatest {
                        AppManager.currentTime = it
                        AppManager.currentTimeLiveData.postValue(it)
                        updateNotification()
                    }
                }

                AppManager.isPlayingMusicLiveData.value = true
                AppManager.playMusicLiveData.value = data
            }

            ControllerEnums.PAUSE -> {
                job?.let { it.cancel() }
                mediaPlayer.pause()
                AppManager.isPlayingMusicLiveData.value = false
                updateNotification()
            }

            ControllerEnums.NEXT -> {
                AppManager.currentTime = 0
                if (AppManager.selectMusicPos + 1 == AppManager.cursor!!.count) AppManager.selectMusicPos = 0
                else AppManager.selectMusicPos++
                doneCommand(ControllerEnums.PLAY)
            }

            ControllerEnums.PREV -> {
                AppManager.currentTime = 0
                if (AppManager.selectMusicPos == 0) AppManager.selectMusicPos = AppManager.cursor!!.count - 1
                else AppManager.selectMusicPos--
                doneCommand(ControllerEnums.PLAY)
            }

            ControllerEnums.CANCEL -> {
                mediaPlayer.stop()
                stopSelf()
            }

            ControllerEnums.POS -> {
                if (mediaPlayer.isPlaying) mediaPlayer.stop()
                mediaPlayer.setOnCompletionListener { doneCommand(ControllerEnums.NEXT) }

                job?.cancel()
                job = scope.launch {
                    changeProgress().collectLatest {
                        AppManager.currentTime = it
                        AppManager.currentTimeLiveData.postValue(it)
                    }
                }

                AppManager.playMusicLiveData.value = data
            }

            ControllerEnums.SEEK -> {
                val time = AppManager.duration / 100
                mediaPlayer.seekTo(AppManager.currentTime.toInt())
                job?.cancel()
                job = scope.launch {
                    changeProgress().collectLatest {
                        AppManager.currentTime = it
                        AppManager.currentTimeLiveData.postValue(it)
                    }
                }
                updateNotification()
            }
        }
    }


    private fun changeProgress(): Flow<Long> = flow {
        for (i in AppManager.currentTime until AppManager.fullTime step 500) {
            delay(500)
            emit(i)
        }
    }

    private fun updateNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        val musicData = AppManager.cursor!!.getMusicDataByPosition(AppManager.selectMusicPos)

        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Music player")
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(
                        0,
                        1,
                        2
                    ) // Indexes of buttons in your custom layout
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(createPendingIntent(ControllerEnums.CANCEL))
            )
            .setCustomContentView(createRemoteView())
            .build()


        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, musicData.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, musicData.title)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, musicData.duration!!.toLong())

        mediaSession?.setMetadata(metadataBuilder.build())

        startForeground(1, notification)
    }

    private fun createMediaSession() {
        mediaSession = MediaSessionCompat(baseContext, "My Music")

        mediaSession!!.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )
        mediaSession!!.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                super.onPlay()
                doneCommand(ControllerEnums.PLAY)
            }

            override fun onPause() {
                super.onPause()
                doneCommand(ControllerEnums.PAUSE)

            }

            override fun onSkipToNext() {
                super.onSkipToNext()
                doneCommand(ControllerEnums.NEXT)
            }

            override fun onSkipToPrevious() {
                super.onSkipToPrevious()
                doneCommand(ControllerEnums.PREV)
            }

            override fun onSeekTo(pos: Long) {
                super.onSeekTo(pos)
                AppManager.currentTime = pos
                doneCommand(ControllerEnums.SEEK)
            }
        })
        timer = object : CountDownTimer(Long.MAX_VALUE, 250) {
            override fun onTick(millisUntilFinished: Long) {
                updateMediaPlaybackState(AppManager.currentTime)
            }

            override fun onFinish() {
            }

        }.start()
    }

    private fun updateMediaPlaybackState(currentPos: Long) {
        val state =
            if (mediaPlayer.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val com = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY
                        or PlaybackStateCompat.ACTION_PLAY_PAUSE
                        or PlaybackStateCompat.ACTION_PAUSE
                        or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                        or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                        or PlaybackStateCompat.ACTION_SEEK_TO
            )
            .setState(
                state,
                currentPos,
                1f,
                SystemClock.elapsedRealtime()
            )
            .build()
        mediaSession!!.setPlaybackState(
            com
        )
    }


}