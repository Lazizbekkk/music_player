package uz.gita.musicapp.utils

import android.database.Cursor
import androidx.lifecycle.MutableLiveData
import uz.gita.musicapp.data.MusicData

object AppManager {
    var selectMusicPos = -1
    var cursor: Cursor? = null
    var isChanged = false
    var currentTime = 0L
    var fullTime = 0L
    var duration = 0
    val currentTimeLiveData = MutableLiveData<Long>()
    val playMusicLiveData = MutableLiveData<MusicData>()
    val isPlayingMusicLiveData = MutableLiveData<Boolean>()
}