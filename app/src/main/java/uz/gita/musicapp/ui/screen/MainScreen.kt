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
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import uz.gita.musicapp.R
import uz.gita.musicapp.data.ControllerEnums
import uz.gita.musicapp.data.MusicData
import uz.gita.musicapp.databinding.ScreenMainBinding
import uz.gita.musicapp.servise.MyService
import uz.gita.musicapp.ui.adapter.MusicAdapter
import uz.gita.musicapp.utils.AppManager

class MainScreen : Fragment(R.layout.screen_main) {
    private val adapter = MusicAdapter()
    private val binding by viewBinding(ScreenMainBinding::bind)


    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.rvList.adapter = adapter
        binding.rvList.layoutManager = LinearLayoutManager(requireContext())
        adapter.cursor = AppManager.cursor

        requireActivity().window?.statusBarColor =
            ContextCompat.getColor(requireContext(), R.color.white)






        binding.bottomPart.setOnClickListener {
            findNavController().navigate(R.id.action_mainScreen_to_playScreen)
        }

        binding.buttonNextScreen.setOnClickListener { startMyService(ControllerEnums.NEXT) }
        binding.buttonPrevScreen.setOnClickListener { startMyService(ControllerEnums.PREV) }
        binding.buttonManageScreen.setOnClickListener {
            startMyService(ControllerEnums.MANAGE)
        }

        AppManager.playMusicLiveData.observe(viewLifecycleOwner, musicPlayScreen)
        AppManager.isPlayingMusicLiveData.observe(viewLifecycleOwner, isPlayMusicObserver)


        if (AppManager.selectMusicPos == -1) {
            AppManager.selectMusicPos = 0;
            startMyService(ControllerEnums.POS)
        }

        adapter.onClickedMusic {
            AppManager.selectMusicPos = it
            startMyService(ControllerEnums.PLAY)
            adapter.getColor(it)
            adapter.notifyDataSetChanged()
            AppManager.isChanged = true


        }
    }

    private fun startMyService(action: ControllerEnums) {
        val intent = Intent(requireContext(), MyService::class.java)
        intent.putExtra("COMMAND", action)
        if (Build.VERSION.SDK_INT >= 26) {
            requireActivity().startForegroundService(intent)
        } else requireActivity().startService(intent)
    }


    private val musicPlayScreen = Observer<MusicData> {
        binding.apply {
            textMusicNameScreen.text = it.title
            textArtistNameScreen.text = it.artist
            AppManager.duration = it.duration!!.toInt()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private val isPlayMusicObserver = Observer<Boolean> {
        if (it) {
            binding.buttonManageScreen.setImageResource(R.drawable.ic_pause)
            adapter.lottieAnimationView!!.playAnimation()
            adapter.colorItem = AppManager.selectMusicPos
            adapter.getColor(AppManager.selectMusicPos)
            adapter.notifyDataSetChanged()
            AppManager.isChanged = true
        } else {
            binding.buttonManageScreen.setImageResource(R.drawable.ic_play)
            adapter.getColor(AppManager.selectMusicPos)
            adapter.getColorAnimation(AppManager.selectMusicPos)
            adapter.notifyDataSetChanged()
            adapter.colorItem = -1
            adapter.lottieAnimationView!!.pauseAnimation()
            adapter.notifyDataSetChanged()
        }
    }
}
