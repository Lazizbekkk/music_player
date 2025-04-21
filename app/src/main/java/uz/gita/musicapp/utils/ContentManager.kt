package uz.gita.musicapp.utils

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import uz.gita.musicapp.data.MusicData


    private val ls = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.DURATION
    )

    fun Context.getMusicByCursor(): Flow<Cursor> = flow {
        val cursor: Cursor = contentResolver
            .query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, ls,
                MediaStore.Audio.Media.IS_MUSIC + "!=0",
                null, null
            ) ?: return@flow

        emit(cursor)
    }

    fun Cursor.getMusicDataByPosition(position: Int): MusicData {
        this.moveToPosition(position)
        return MusicData(
            this.getInt(0),
            this.getString(1),
            this.getString(2),
            this.getString(3),
            this.getLong(4),
        )
    }
