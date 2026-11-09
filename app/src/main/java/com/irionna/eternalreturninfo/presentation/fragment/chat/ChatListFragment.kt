package com.irionna.eternalreturninfo.presentation.fragment.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.data.repository.BlockedUsers
import com.irionna.eternalreturninfo.databinding.ChatListFragmentBinding
import com.irionna.eternalreturninfo.presentation.activity.chat.ChatActivity
import com.irionna.eternalreturninfo.presentation.activity.main.MainActivity
import com.irionna.eternalreturninfo.presentation.adapter.chat.ChatListAdapter
import com.irionna.eternalreturninfo.presentation.viewmodel.ChatListViewModel

/**
 * 채팅 목록. 화면이 보일 때(onResume)만 서버에 연결하고, 떠나면(onPause) 모두 닫는다.
 * 차단한 사용자와의 대화는 목록에서 숨긴다. 대화가 없으면 안내와 "듀오 찾기로 가기" 버튼을 보여 준다.
 */
class ChatListFragment : Fragment() {

    companion object {
        fun newInstance() = ChatListFragment()
        private const val TAB_DUO = 1
    }

    private var _binding: ChatListFragmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatListViewModel by viewModels()

    private val chatListAdapter by lazy {
        ChatListAdapter(onClickItem = { item ->
            startActivity(ChatActivity.newIntent(requireContext(), item.user))
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ChatListFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.chatListRecyclerview.adapter = chatListAdapter
        binding.chatListRecyclerview.layoutManager = LinearLayoutManager(requireContext())
        binding.chatListRecyclerview.itemAnimator = null

        binding.chatListGoDuo.setOnClickListener {
            (activity as? MainActivity)?.binding?.tabLayout?.getTabAt(TAB_DUO)?.select()
        }

        viewModel.rooms.observe(viewLifecycleOwner) { render() }
        viewModel.loaded.observe(viewLifecycleOwner) { render() }
        BlockedUsers.blocked.observe(viewLifecycleOwner) { render() }
    }

    private fun render() {
        val b = _binding ?: return
        val visible = viewModel.rooms.value.orEmpty().filterNot { BlockedUsers.isBlocked(it.user.uid) }
        chatListAdapter.submitList(visible)
        val loaded = viewModel.loaded.value == true
        b.chatListEmptyGroup.visibility = if (visible.isEmpty()) View.VISIBLE else View.GONE
        // 처음 확인이 끝나기 전에는 안내 문구만 "불러오는 중"으로, 버튼은 숨긴다
        b.chatListEmptyText.setText(if (loaded) R.string.chat_list_empty else R.string.chat_list_loading)
        b.chatListGoDuo.visibility = if (loaded) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        viewModel.start()
    }

    override fun onPause() {
        super.onPause()
        viewModel.stop()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
