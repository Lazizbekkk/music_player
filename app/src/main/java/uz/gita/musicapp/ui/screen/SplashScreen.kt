package uz.gita.musicapp.ui.screen

import android.Manifest
import android.Manifest.permission.POST_NOTIFICATIONS
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import uz.gita.musicapp.R
import uz.gita.musicapp.utils.AppManager
import uz.gita.musicapp.utils.getMusicByCursor
import uz.gita.musicapp.utils.goToSettings

@SuppressLint("CustomSplashScreen")
class SplashScreen : Fragment(R.layout.screen_splash) {
    private val requestPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val allGranted = permissions.all { it.value } 
        if (allGranted) {
            requireContext().getMusicByCursor().onEach {
                AppManager.cursor = it
                findNavController().navigate(R.id.action_splashScreen_to_mainScreen)
            }.launchIn(lifecycleScope)
        } else {
            requireActivity().goToSettings()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().window?.statusBarColor =
            ContextCompat.getColor(requireContext(), R.color.pink)



        object : CountDownTimer(1500, 900) {
            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                if (Build.VERSION.SDK_INT > 32) {
                    val permissions = arrayOf(
                        Manifest.permission.READ_MEDIA_AUDIO,)
                    requestPermissions.launch(permissions)
                } else {
                    val permissions = arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                    requestPermissions.launch(permissions)
                }
            }
        }.start()
    }
}