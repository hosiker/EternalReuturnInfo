package com.irionna.eternalreturninfo.presentation.adapter.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.irionna.eternalreturninfo.databinding.MainRvBannerBinding
import com.irionna.eternalreturninfo.data.model.Notice


class NoticeBannerListAdapter :
    ListAdapter<Notice, NoticeBannerListAdapter.ViewHolder>(
        object : DiffUtil.ItemCallback<Notice>() {
            override fun areItemsTheSame(
                oldVideoItem: Notice,
                newVideoItem: Notice
            ): Boolean {
                return oldVideoItem.url == newVideoItem.url
            }

            override fun areContentsTheSame(
                oldVideoItem: Notice,
                newVideoItem: Notice
            ): Boolean {
                return oldVideoItem == newVideoItem
            }
        }) {

    interface OnItemClickListener {
        fun onItemClick(item: Notice, position:Int)
    }

    private var onItemClickListener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            MainRvBannerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ViewHolder(
        private val binding: MainRvBannerBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(noticeItem: Notice) {
            with(binding) {

                bannerImg.load(noticeItem.thumnail)
                bannerImg.contentDescription = noticeItem.title

                itemView.setOnClickListener {
                    onItemClickListener?.onItemClick(noticeItem, adapterPosition)
                }
            }
        }
    }
}