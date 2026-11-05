package com.irionna.eternalreturninfo.presentation.activity.chat

import android.content.Context
import com.irionna.eternalreturninfo.util.setContentViewEdgeToEdge
import com.irionna.eternalreturninfo.util.BlockDialog
import com.irionna.eternalreturninfo.data.repository.BlockedUsers
import android.view.Gravity
import android.view.View
import android.widget.Toast
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import com.skydoves.powermenu.CircularEffect
import com.skydoves.powermenu.MenuAnimation
import com.skydoves.powermenu.OnMenuItemClickListener
import com.skydoves.powermenu.PowerMenu
import com.skydoves.powermenu.PowerMenuItem
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.databinding.ChatActivityBinding
import com.irionna.eternalreturninfo.data.model.ERModel
import com.irionna.eternalreturninfo.presentation.adapter.chat.ChatAdapter
import com.irionna.eternalreturninfo.presentation.viewmodel.ChatViewModel
import com.irionna.eternalreturninfo.presentation.viewmodel.ChatViewModelFactory
import com.irionna.eternalreturninfo.util.Constants.Companion.EXTRA_ER_MODEL
import com.google.firebase.auth.FirebaseAuth

class ChatActivity : AppCompatActivity() {

    companion object {
        fun newIntent(
            context: Context,
            erModel: ERModel
        ): Intent {
            val intent = Intent(context, ChatActivity::class.java).apply {
                putExtra(EXTRA_ER_MODEL, erModel)
            }
            return intent
        }
    }

    // 뷰바인딩
    private lateinit var binding: ChatActivityBinding

    // 파이어베이스 auth 초기화
    private val auth by lazy {
        FirebaseAuth.getInstance()
    }

    // data
    private val data by lazy {
        intent.getParcelableExtra<ERModel>(EXTRA_ER_MODEL)
    }

    // viewModel
    private val viewModel: ChatViewModel by lazy {
        ViewModelProvider(this, ChatViewModelFactory())[ChatViewModel::class.java]
    }


    private val chatAdapter by lazy {
        ChatAdapter(
            onClickItem = { position ->
                val imm =
                    this.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.chatMsgEt.windowToken, 0)
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ChatActivityBinding.inflate(layoutInflater)
        setContentViewEdgeToEdge(binding.root)

        initView()
        initModel()
    }

    private fun initView() = with(binding) {
        // 리사이클러뷰 초기화
        chatRecycler.adapter = chatAdapter
        chatRecycler.layoutManager = LinearLayoutManager(this@ChatActivity)

        // 툴바에 채팅상대 이름 출력하기
        chatToolbarTitle.text = data?.name

        // 상대가 탈퇴한 경우: 대화 기록은 볼 수 있지만 더 보낼 수 없다.
        if (data?.withdrawn == true) {
            chatMsgEt.isEnabled = false
            chatSendBtn.isEnabled = false
            chatMsgEt.setText("")
            chatMsgEt.setHint(R.string.chat_withdrawn_user)
            chatMsgEt.setHintTextColor(Color.GRAY)
        }

        // 상단 메뉴: 상대 차단/차단 해제 (탈퇴한 상대에게는 필요 없음)
        if (data?.withdrawn == true) {
            chatMenuBtn.visibility = View.GONE
        } else {
            chatMenuBtn.setOnClickListener { showBlockMenu(it) }
        }

        // 뒤로가기 클릭 시 채팅방에서 빠져나옴
        chatBackBtn.setOnClickListener{
            viewModel.disconnectFirebase()
            finish()
        }
    }

    // 입력창의 기본 힌트와 색 (차단을 풀면 되돌린다)
    private var defaultHint: CharSequence? = null
    private var defaultHintColors: ColorStateList? = null

    // 내가 차단한 상대면 입력창을 잠그고 회색 안내를 보여 준다 (탈퇴한 상대와 같은 방식)
    private fun applyBlockState() = with(binding) {
        if (data?.withdrawn == true) return@with
        if (defaultHint == null) {
            defaultHint = chatMsgEt.hint
            defaultHintColors = chatMsgEt.hintTextColors
        }
        if (BlockedUsers.isBlocked(data?.uid)) {
            chatMsgEt.isEnabled = false
            chatSendBtn.isEnabled = false
            chatMsgEt.setText("")
            chatMsgEt.setHint(R.string.chat_blocked_user)
            chatMsgEt.setHintTextColor(Color.GRAY)
        } else {
            chatMsgEt.isEnabled = true
            chatSendBtn.isEnabled = true
            chatMsgEt.hint = defaultHint
            defaultHintColors?.let { chatMsgEt.setHintTextColor(it) }
        }
    }

    private fun showBlockMenu(anchor: View) {
        val targetUid = data?.uid ?: return
        val blocked = BlockedUsers.isBlocked(targetUid)
        val menu = PowerMenu.Builder(this)
            .addItem(PowerMenuItem(getString(if (blocked) R.string.menu_unblock_user else R.string.menu_block_user)))
            .setMenuRadius(20f)
            .setTextSize(18)
            .setWidth(330)
            .setTextGravity(Gravity.CENTER)
            .setTextColor(ContextCompat.getColor(this, R.color.white))
            .setMenuColor(ContextCompat.getColor(this, R.color.darkgray))
            .setSelectedMenuColor(ContextCompat.getColor(this, R.color.black))
            .setCircularEffect(CircularEffect.BODY)
            .setAnimation(MenuAnimation.SHOWUP_TOP_RIGHT)
            .build()
        menu.setOnMenuItemClickListener(object : OnMenuItemClickListener<PowerMenuItem> {
            override fun onItemClick(position: Int, item: PowerMenuItem) {
                menu.dismiss()
                if (blocked) {
                    BlockedUsers.unblock(targetUid) { ok ->
                        Toast.makeText(
                            this@ChatActivity,
                            if (ok) R.string.unblock_done else R.string.unblock_failed,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    BlockDialog.show(this@ChatActivity, targetUid, data?.name)
                }
            }
        })
        menu.showAsDropDown(anchor)
    }

    private fun initModel() = with(viewModel) {
        BlockedUsers.blocked.observe(this@ChatActivity) { applyBlockState() }
        applyBlockState()
        sendFailed.observe(this@ChatActivity) {
            Toast.makeText(this@ChatActivity, R.string.chat_send_failed, Toast.LENGTH_SHORT).show()
        }
        list.observe(this@ChatActivity){
            chatAdapter.submitList(it)
        }
        message.observe(this@ChatActivity){
            binding.chatMsgEt.setText(it)
        }
        notify.observe(this@ChatActivity){
            chatAdapter.notifyDataSetChanged()
        }
        scroll.observe(this@ChatActivity){
            binding.chatRecycler.scrollToPosition(getListSize() - 1)
        }

        binding.chatSendBtn.setOnClickListener {
            if (data?.withdrawn == true) return@setOnClickListener
            saveChat(data?.uid.toString(), auth.uid.toString(), binding.chatMsgEt.text.toString())
        }

        loadChat(data?.name.toString(), data?.uid.toString(), auth.uid.toString())
    }
}