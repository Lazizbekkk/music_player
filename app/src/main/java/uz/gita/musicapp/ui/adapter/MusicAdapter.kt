package uz.gita.musicapp.ui.adapter

import android.annotation.SuppressLint
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import uz.gita.musicapp.databinding.ItemMusicBinding
import uz.gita.musicapp.utils.AppManager
import uz.gita.musicapp.utils.getMusicDataByPosition

class MusicAdapter : RecyclerView.Adapter<MusicAdapter.VH>() {
    var cursor: Cursor? = null
    private var clickedMusic: ((Int) -> Unit)? = null
    var index = 0;
    var colorItem = 0;
    var lottieAnimationView: LottieAnimationView? = null

    inner class VH(val binding: ItemMusicBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                clickedMusic?.invoke(adapterPosition)
            }

        }

        @SuppressLint("NotifyDataSetChanged")
        fun bind() {
            lottieAnimationView = binding.myLottie
            cursor?.getMusicDataByPosition(adapterPosition).let {
                binding.myLottie.pauseAnimation()
                binding.textMusicName.text = it?.title
                binding.textArtistName.text = it?.artist
            }
            binding.root.setOnClickListener {

                val clickedPosition = adapterPosition
                if (colorItem != 0) {
                    colorItem = adapterPosition
                    lottieAnimationView!!.playAnimation()
                    notifyDataSetChanged()
                }
                clickedMusic?.invoke(clickedPosition)

                index = clickedPosition
                this@MusicAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemMusicBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int = cursor?.count ?: 0

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind()

        if (!AppManager.isChanged) {
            holder.binding.myLottie.visibility = View.INVISIBLE
            return
        }
        if (colorItem != position) {
            holder.binding.myLottie.pauseAnimation()
        }
        if (position == AppManager.selectMusicPos && position == colorItem) {
            lottieAnimationView!!.visibility = View.VISIBLE
            holder.binding.myLottie.playAnimation()

        } else if (position == AppManager.selectMusicPos) {
            lottieAnimationView!!.visibility = View.VISIBLE
            holder.binding.myLottie.pauseAnimation()
        } else {
            lottieAnimationView!!.visibility = View.INVISIBLE
            holder.binding.myLottie.pauseAnimation()
        }


    }

    fun onClickedMusic(block: (Int) -> Unit) {
        this.clickedMusic = block
    }


    fun getColor(index: Int): Int {
        this.index = index
        return this.index;
    }


    @SuppressLint("NotifyDataSetChanged")
    fun getColorAnimation(position: Int) {
        if (lottieAnimationView != null) {
            if (position == AppManager.selectMusicPos) {
                lottieAnimationView!!.visibility = View.VISIBLE
                lottieAnimationView!!.playAnimation()
            } else {
                lottieAnimationView!!.visibility = View.INVISIBLE
                lottieAnimationView!!.pauseAnimation()
            }
            notifyDataSetChanged()
        }
    }

}