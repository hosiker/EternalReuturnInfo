package com.irionna.eternalreturninfo.presentation.activity.block

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.data.model.ERModel
import com.irionna.eternalreturninfo.data.repository.BlockedUsers
import com.irionna.eternalreturninfo.databinding.BlockedUserItemBinding
import com.irionna.eternalreturninfo.databinding.BlockedUsersActivityBinding
import com.irionna.eternalreturninfo.retrofit.FBRef
import com.irionna.eternalreturninfo.util.setContentViewEdgeToEdge

/** 내가 차단한 사용자 목록과 차단 해제 화면 (프로필 > 설정 > 차단 목록). */
class BlockedUsersActivity : AppCompatActivity() {

    private lateinit var binding: BlockedUsersActivityBinding

    // uid -> 표시 이름 (빈 문자열은 불러오는 중)
    private val names = mutableMapOf<String, String>()

    private val listAdapter = BlockedUserAdapter { uid ->
        BlockedUsers.unblock(uid) { ok ->
            Toast.makeText(
                this,
                if (ok) R.string.unblock_done else R.string.unblock_failed,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BlockedUsersActivityBinding.inflate(layoutInflater)
        setContentViewEdgeToEdge(binding.root)

        binding.blockedBackBtn.setOnClickListener { finish() }
        binding.blockedRecycler.layoutManager = LinearLayoutManager(this)
        binding.blockedRecycler.adapter = listAdapter

        BlockedUsers.blocked.observe(this) { render(it) }
    }

    private fun render(uids: Set<String>) {
        val items = uids.map { Item(it, names[it].orEmpty()) }.sortedBy { it.name }
        listAdapter.submitList(items)
        binding.blockedEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE

        uids.filter { it !in names }.forEach { loadName(it) }
    }

    private fun loadName(uid: String) {
        names[uid] = ""
        FBRef.userRef.child(uid).get()
            .addOnSuccessListener { snapshot ->
                val user = snapshot.getValue(ERModel::class.java)
                names[uid] = when {
                    user?.withdrawn == true -> getString(R.string.withdrawn_user_name)
                    !user?.name.isNullOrBlank() -> user?.name.orEmpty()
                    else -> getString(R.string.blocked_unknown_user)
                }
                BlockedUsers.blocked.value?.let { render(it) }
            }
            .addOnFailureListener {
                names[uid] = getString(R.string.blocked_unknown_user)
                BlockedUsers.blocked.value?.let { render(it) }
            }
    }

    data class Item(val uid: String, val name: String)

    private class BlockedUserAdapter(
        private val onUnblock: (String) -> Unit
    ) : ListAdapter<Item, BlockedUserAdapter.ViewHolder>(object : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item) = oldItem.uid == newItem.uid
        override fun areContentsTheSame(oldItem: Item, newItem: Item) = oldItem == newItem
    }) {
        class ViewHolder(val binding: BlockedUserItemBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(BlockedUserItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = getItem(position)
            holder.binding.blockedUserName.text = item.name
            holder.binding.blockedUnblockBtn.setOnClickListener { onUnblock(item.uid) }
        }
    }
}
