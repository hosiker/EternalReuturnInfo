package com.irionna.eternalreturninfo.presentation.fragment.myprofile

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.irionna.eternalreturninfo.databinding.MyprofileBoardRvFragmentBinding
import com.irionna.eternalreturninfo.data.model.BoardModel
import com.irionna.eternalreturninfo.retrofit.BoardSingletone
import com.irionna.eternalreturninfo.retrofit.FBRef
import com.irionna.eternalreturninfo.presentation.activity.board.BoardDeletedActivity
import com.irionna.eternalreturninfo.presentation.activity.board.BoardPostActivity
import com.irionna.eternalreturninfo.presentation.adapter.board.BoardRecyclerViewAdapter
import com.irionna.eternalreturninfo.presentation.viewmodel.BoardListViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class MyProfileBoardFragment : Fragment() {

    companion object {
        fun newInstance() = MyProfileBoardFragment()
    }

    private val boardViewModel: BoardListViewModel by activityViewModels()

    private var _binding: MyprofileBoardRvFragmentBinding? = null
    private val binding get() = _binding!!

    private val listAdapter by lazy {
        BoardRecyclerViewAdapter()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MyprofileBoardRvFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initModel()

    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun initView() = with(binding) {

        val loadBoardLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    result.data?.getParcelableExtra("updateBoard", BoardModel::class.java)?.let { updateBoard ->
                        boardViewModel.updateBoard(updateBoard)
                    }
                } else {
                    result.data?.getParcelableExtra<BoardModel>("updateBoard")?.let { updateBoard ->
                        boardViewModel.updateBoard(updateBoard)
                    }
                }

            }else{

            }
        }

        boardNoticeRv.adapter = listAdapter
        boardNoticeRv.layoutManager = LinearLayoutManager(requireContext())

        listAdapter.setOnItemClickListener(object : BoardRecyclerViewAdapter.OnItemClickListener{
            override fun onItemClick(boardItem: BoardModel) {

                FBRef.postRef.child(boardItem.id).addListenerForSingleValueEvent(object :
                    ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (!dataSnapshot.exists()) {
                            val intent = Intent(requireContext(), BoardDeletedActivity::class.java)
                            startActivity(intent)
                        } else {
                            // 조회수는 글 화면이 열릴 때 올린다
                            val intent = Intent(requireContext(), BoardPostActivity::class.java)
                            intent.putExtra("ID", boardItem.id)
                            loadBoardLauncher.launch(intent)
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.d("error", "firebase data loading failed")
                    }
                })

            }
        })
    }

    private fun initModel() = with(boardViewModel) {
        boardList.observe(viewLifecycleOwner) {

            val user = BoardSingletone.LoginUser()
            val userBoard = it.filter { it.author == user.uid }.toMutableList()

            listAdapter.submitList(
                userBoard.reversed()
            )
        }
    }

}