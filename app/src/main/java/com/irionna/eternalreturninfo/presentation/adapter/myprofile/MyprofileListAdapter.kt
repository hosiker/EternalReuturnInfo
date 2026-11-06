package com.irionna.eternalreturninfo.presentation.adapter.myprofile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.databinding.MyprofileItemRecyclerviewBinding
import com.irionna.eternalreturninfo.retrofit.CharacterStats
import com.irionna.eternalreturninfo.util.CharacterPicker
import com.irionna.eternalreturninfo.util.Characters
import com.irionna.eternalreturninfo.util.InitialAvatar

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
        // 서버가 준 게임 번호를 앱 목록 위치로 바꿔 얼굴 이미지(face_N)를 보여준다. 앱에 이미지가 없는 신규 실험체는 첫 글자 아바타
        val ctx = holder.image.context
        val name = Characters.nameOf(ctx, item.characterCode)
        val appIndex = Characters.appIndexOf(item.characterCode)
        if (appIndex != null) CharacterPicker.loadFace(holder.image, appIndex) else holder.image.setImageDrawable(InitialAvatar.drawable(ctx, name))

        holder.usages.text = dataList[position].usages.toString()+"게임"
        holder.character.text = name
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