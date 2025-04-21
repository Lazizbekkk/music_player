package uz.gita.musicapp.ui.screen

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import uz.gita.musicapp.R
import uz.gita.musicapp.data.ControllerEnums
import uz.gita.musicapp.data.MusicData
import uz.gita.musicapp.databinding.ScreenPlayBinding
import uz.gita.musicapp.servise.MyService
import uz.gita.musicapp.utils.AppManager
import uz.gita.musicapp.utils.setChangeProgress

class PlayScreen : Fragment(R.layout.screen_play) {
    private val binding by viewBinding(ScreenPlayBinding::bind)

    @SuppressLint("DefaultLocale")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonNext.setOnClickListener { startMyService(ControllerEnums.NEXT) }
        binding.buttonPrev.setOnClickListener { startMyService(ControllerEnums.PREV) }
        binding.buttonManage.setOnClickListener { startMyService(ControllerEnums.MANAGE) }

        requireActivity().window?.statusBarColor =
            ContextCompat.getColor(requireContext(), R.color.white)


        AppManager.playMusicLiveData.observe(viewLifecycleOwner, playMusicObserver)
        AppManager.isPlayingMusicLiveData.observe(viewLifecycleOwner, isPlayingObserver)
        AppManager.currentTimeLiveData.observe(viewLifecycleOwner, currentTimeObserver)

        binding.back.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.seekBar.setChangeProgress { progress, fromUser ->
            if (fromUser) {
                AppManager.currentTime = progress.toLong()
                startMyService(ControllerEnums.SEEK)

                val totalMilliseconds: Long = progress.toLong()
                val seconds: Long = totalMilliseconds / 1000 % 60
                val minutes: Long = totalMilliseconds / 1000 / 60
                val formattedTime: String = String.format("%02d:%02d", minutes, seconds)

                binding.currentTime.text = formattedTime
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private val playMusicObserver = Observer<MusicData> {
        binding.textMusicName.text = it.title
        binding.textArtistName.text = it.artist

        binding.currentTime.text = AppManager.currentTime.toString()
        binding.totalTime.text = AppManager.duration.toString()
        binding.seekBar.max = AppManager.duration


        val totalMilliseconds: Long = AppManager.fullTime
        val totalSeconds: Long = totalMilliseconds / 1000
        val minutes: Long = totalSeconds / 60
        val seconds: Long = totalSeconds % 60
        val formattedTime: String = String.format("%02d:%02d", minutes, seconds)

        binding.totalTime.text = formattedTime


    }

    @SuppressLint("DefaultLocale")
    private val currentTimeObserver = Observer<Long> {
        binding.seekBar.progress = it.toInt()

        val totalMilliseconds: Long = it
        val seconds: Long = totalMilliseconds / 1000 % 60
        val minutes: Long = totalMilliseconds / 1000 / 60
        val formattedTime: String = String.format("%02d:%02d", minutes, seconds)
        binding.currentTime.text = formattedTime
    }
    private val isPlayingObserver = Observer<Boolean> {
        if (it) binding.buttonManage.setImageResource(R.drawable.ic_pause)
        else binding.buttonManage.setImageResource(R.drawable.ic_play)
    }

    private fun startMyService(action: ControllerEnums) {
        val intent = Intent(requireActivity(), MyService::class.java)
        intent.putExtra("COMMAND", action)
        if (Build.VERSION.SDK_INT >= 26) {
            requireActivity().startForegroundService(intent)
        } else {
            requireActivity().startService(intent)
        }
    }
}
