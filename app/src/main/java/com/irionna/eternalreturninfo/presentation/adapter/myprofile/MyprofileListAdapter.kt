package com.irionna.eternalreturninfo.presentation.adapter.myprofile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.databinding.MyprofileItemRecyclerviewBinding
import com.irionna.eternalreturninfo.retrofit.CharacterStats
import com.irionna.eternalreturninfo.util.CharacterPicker

class MyprofileListAdapter(val dataList:MutableList<CharacterStats>, val characterList:Array<String>) :RecyclerView.Adapter<MyprofileListAdapter.Holder>()  {

    interface ItemClick{
        fun onClick(view: View, position: Int)
    }
    var itemClick: ItemClick? = null

    inner class Holder(val binding:MyprofileItemRecyclerviewBinding): RecyclerView.ViewHolder(binding.root){
        val image = binding.myprofilelistCharacterImg
        val usages = binding.myprofileUsagesTv
        val character = binding.myprofilelistMycharacterTv
        val top3 = binding.myprofilelistTvTop3
        val avRank = binding.myprofilelistTvAverageRank
        val maxKill = binding.myprofilelistTvMaxKill

    }
    // 사진, 이름, 주소, 타이틀, 내용
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = MyprofileItemRecyclerviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }


    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = dataList[position]
        // 얼굴 이미지(face_N). 없는 번호는 기본 이미지
        CharacterPicker.loadFace(holder.image, item.characterCode)

        holder.usages.text = dataList[position].usages.toString()+"게임"
        holder.character.text = characterList.getOrNull(item.characterCode) ?: "#${item.characterCode}"
        holder.top3.text = dataList[position].top3.toString()
        holder.avRank.text = "#"+dataList[position].averageRank.toString()
        holder.maxKill.text = dataList[position].maxKillings.toString()+"킬"

    }

    /** 전적 카드에서 선택한 시즌·모드가 바뀔 때 목록을 통째로 바꾼다. */
    fun update(list: List<CharacterStats>) {
        dataList.clear()
        dataList.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return dataList.size
    }


}