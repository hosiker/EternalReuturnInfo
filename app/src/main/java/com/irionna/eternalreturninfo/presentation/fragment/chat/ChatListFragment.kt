package com.irionna.eternalreturninfo.presentation.fragment.chat

import com.irionna.eternalreturninfo.util.ProfileImage
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import coil.api.load
import com.irionna.eternalreturninfo.databinding.ChatListFragmentBinding
import com.irionna.eternalreturninfo.presentation.activity.chat.ChatActivity
import com.irionna.eternalreturninfo.presentation.adapter.chat.ChatListAdapter
import com.irionna.eternalreturninfo.presentation.viewmodel.ChatListViewModel
import com.irionna.eternalreturninfo.presentation.viewmodel.ChatListViewModelFactory

class ChatListFragment : Fragment() {

    companion object {
        fun newInstance() = ChatListFragment()
    }

    private var _binding: ChatListFragmentBinding? = null
    private val binding get() = _binding!!

    private val chatListAdapter by lazy {
        ChatListAdapter(
            onClickItem = { position, item ->
                startActivity(
                    ChatActivity.newIntent(
                        requireContext(),
                        item
                    )
                )
            }
        )
    }

    private val viewModel: ChatListViewModel by lazy {
        ViewModelProvider(this, ChatListViewModelFactory())[ChatListViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ChatListFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initViewModel()
    }

    private fun initView() = with(binding) {
        chatListRecyclerview.adapter = chatListAdapter
        chatListRecyclerview.layoutManager = LinearLayoutManager(context)
        chatListRecyclerview.itemAnimator = null
    }

    private fun initViewModel() = with(viewModel) {
        loadProfilePicture()

        list.observe(viewLifecycleOwner) {
            chatListAdapter.submitList(it)
        }

        profilePictureUrl.observe(viewLifecycleOwner) {
            ProfileImage.load(binding.chatListMyProfilePicture, it)
        }

        profileName.observe(viewLifecycleOwner) {
            binding.chatListTitle.text = it
        }

        addChatList()
    }

    override fun onResume() {
        super.onResume()
        initViewModel()
        chatListAdapter.notifyDataSetChanged()
    }

    override fun onStop() {
        super.onStop()
        viewModel.disconnectFirebase()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}