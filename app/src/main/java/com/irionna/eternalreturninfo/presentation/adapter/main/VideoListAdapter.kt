package com.irionna.eternalreturninfo.presentation.adapter.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.irionna.eternalreturninfo.databinding.MainRvVideoBinding
import com.irionna.eternalreturninfo.data.model.VideoModel


class VideoListAdapter :
    ListAdapter<VideoModel, VideoListAdapter.ViewHolder>(
        object : DiffUtil.ItemCallback<VideoModel>() {
            override fun areItemsTheSame(
                oldVideoItem: VideoModel,
                newVideoItem: VideoModel
            ): Boolean {
                return oldVideoItem.id == newVideoItem.id
            }

            override fun areContentsTheSame(
                oldVideoItem: VideoModel,
                newVideoItem: VideoModel
            ): Boolean {
                return oldVideoItem == newVideoItem
            }
        }) {

    interface OnItemClickListener {
        fun onItemClick(item: VideoModel, position:Int)
    }

    private var onItemClickListener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            MainRvVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ViewHolder(
        private val binding: MainRvVideoBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: VideoModel) {
            with(binding) {

                mainRvYoutubeThumnail.load(item.thumbnail)
                mainRvYoutubeTitle.text = item.title.toString()
                mainRvShortBadge.visibility = if (item.isShort) android.view.View.VISIBLE else android.view.View.GONE
                mainRvYoutubeThumnail.contentDescription =
                    root.context.getString(com.irionna.eternalreturninfo.R.string.main_video_thumb_desc, item.title.orEmpty())

                itemView.setOnClickListener {
                    onItemClickListener?.onItemClick(item, adapterPosition)
                }
            }
        }
    }
}