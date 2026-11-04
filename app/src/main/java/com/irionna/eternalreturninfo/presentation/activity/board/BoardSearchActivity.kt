package com.irionna.eternalreturninfo.presentation.activity.board

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.databinding.BoardSearchActivityBinding
import com.irionna.eternalreturninfo.data.model.BoardModel
import com.irionna.eternalreturninfo.retrofit.FBRef
import com.irionna.eternalreturninfo.presentation.adapter.board.BoardRecyclerViewAdapter
import com.irionna.eternalreturninfo.presentation.viewmodel.BoardListViewModel
import com.irionna.eternalreturninfo.presentation.viewmodel.BoardListViewModelFactory
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener

class BoardSearchActivity : AppCompatActivity() {

    private lateinit var binding: BoardSearchActivityBinding

    private val boardViewModel by lazy {
        ViewModelProvider(this, BoardListViewModelFactory()).get(BoardListViewModel::class.java)
    }

    private val listAdapter by lazy {
        BoardRecyclerViewAdapter()
    }

    private var postCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BoardSearchActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initView()
        initModel()
    }

    private fun initView() = with(binding) {

        boardSearchRv.adapter = listAdapter
        boardSearchRv.layoutManager = LinearLayoutManager(this@BoardSearchActivity)

        boardSearchIbBack.setOnClickListener {
            finish()
        }

        boardSearchIbClear.setOnClickListener {
            boardSearchEtSearch.setText("")
        }

        boardSearchEtSearch.setOnFocusChangeListener { view, b ->

            if(b){
                boardSearchEtSearch.setBackgroundResource(R.drawable.shape_board_search_clicked)
                boardSearchIbClear.visibility = View.VISIBLE

            } else {
                boardSearchEtSearch.setBackgroundResource(R.drawable.shape_board_search)
                boardSearchIbClear.visibility = View.INVISIBLE
            }

        }

        boardSearchEtSearch.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                postCount = 0
                boardViewModel.clearSearchBoard()
                val searchText = boardSearchEtSearch.text.toString()
                Search(searchText)

                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(boardSearchEtSearch.windowToken, 0)
            }
            false
        }



        listAdapter.setOnItemClickListener(object : BoardRecyclerViewAdapter.OnItemClickListener{
            override fun onItemClick(boardItem: BoardModel) {

                FBRef.postRef.child(boardItem.id).addListenerForSingleValueEvent(object :
                    ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.exists()) {

                            val board = dataSnapshot.getValue(BoardModel::class.java)

                            val views = board?.views?.plus(1)
                            FBRef.postRef.child(boardItem.id).child("views").setValue(views)
                            val intent = Intent(this@BoardSearchActivity, BoardPostActivity::class.java)
                            intent.putExtra("ID", boardItem.id)
                            startActivity(intent)

                        } else {
                            val intent = Intent(this@BoardSearchActivity, BoardDeletedActivity::class.java)
                            startActivity(intent)
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // 데이터 읽기 실패 처리
                    }
                })
            }
        })

    }

    private fun initModel() = with(binding) {
        boardViewModel.searchBoardList.observe(this@BoardSearchActivity){ searchBoardList ->
            val newBoardList = searchBoardList.reversed()
            listAdapter.submitList(newBoardList.toMutableList())
        }
    }

    fun Search(searchText: String)= with(binding){

        val query = FBRef.postRef.orderByChild("title").startAt(searchText).endAt(searchText + "\uf8ff")
        val query2 = FBRef.postRef.orderByChild("content").startAt(searchText).endAt(searchText + "\uf8ff")

        SearchFirebase(query)
        SearchFirebase(query2)

        if(postCount == 0){
            boardSearchTvResult.visibility = View.VISIBLE
            boardSearchTvResult.text = "검색 결과가 없습니다."
        }

    }

    fun SearchFirebase(query: Query)= with(binding){
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                boardSearchTvPostCount.visibility = View.VISIBLE
                postCount = postCount + dataSnapshot.childrenCount.toInt()
                boardSearchTvPostCount.text = postCount.toString() + " Post"

                if(dataSnapshot.exists()){
                    boardSearchTvResult.visibility = View.INVISIBLE

                    for (snapshot in dataSnapshot.children) {
                        val searchBoard = snapshot.getValue(BoardModel::class.java)
                        if (searchBoard != null) {
                            boardViewModel.addSearchBoard(searchBoard)
                        }
                    }
                }else{
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // 검색이 실패한 경우
            }
        })
    }

}