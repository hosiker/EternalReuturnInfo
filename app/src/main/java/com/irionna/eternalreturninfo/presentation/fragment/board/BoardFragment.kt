package com.irionna.eternalreturninfo.presentation.fragment.board

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.irionna.eternalreturninfo.databinding.BoardFragmentBinding
import com.irionna.eternalreturninfo.data.model.BoardModel
import com.irionna.eternalreturninfo.retrofit.FBRef
import com.irionna.eternalreturninfo.presentation.activity.board.BoardAddActivity
import com.irionna.eternalreturninfo.presentation.activity.board.BoardSearchActivity
import com.irionna.eternalreturninfo.presentation.adapter.board.BoardViewPagerAdapter
import com.irionna.eternalreturninfo.presentation.viewmodel.BoardListViewModel
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.irionna.eternalreturninfo.data.model.ReportModel


class BoardFragment : Fragment() {
    companion object {
        fun newInstance() = BoardFragment()
    }

    private var _binding: BoardFragmentBinding? = null
    private val binding get() = _binding!!

    private val boardViewModel: BoardListViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BoardFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun initView() = with(binding) {

        val addBoardLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val board = result.data?.getParcelableExtra("board", BoardModel::class.java)
                        if (board != null) {
                            refresh()
                        }
                    } else {
                        val board = result.data?.getParcelableExtra<BoardModel>("board")
                        if (board != null) {
                            refresh()
                        }
                    }

                }else{

                }
            }

        val adapter = BoardViewPagerAdapter(requireActivity())
        boardViewpager.adapter = adapter

        TabLayoutMediator(boardTabLayout, boardViewpager) { tab, position ->
            tab.setText(adapter.getTitle(position))
        }.attach()

        boardFab.setOnClickListener {
            val intent = Intent(requireContext(), BoardAddActivity::class.java)
            addBoardLauncher.launch(intent)
        }

        boardIbProfile.setOnClickListener {
            val intent = Intent(requireContext(), BoardSearchActivity::class.java)
            startActivity(intent)
        }

        boardSwipeRefreshLayout.setDistanceToTriggerSync(600)

        boardSwipeRefreshLayout.setOnRefreshListener {

            refresh()
            boardSwipeRefreshLayout.isRefreshing = false
        }

    }

    fun refresh(){
        // Firebase.database.getReference("post")
        FBRef.postRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val unsortedBoardList = mutableListOf<BoardModel>()
                for (data in snapshot.children) {
                    val board = data.getValue<BoardModel>()

                    if (board != null && board!!.report.values.none { r -> r.uid == FBRef.auth.uid })  {
                        unsortedBoardList.add(board)
                    }
                }

                boardViewModel.initBoard(unsortedBoardList)

            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("error", "firebase data loading failed")
            }
        })
    }


}