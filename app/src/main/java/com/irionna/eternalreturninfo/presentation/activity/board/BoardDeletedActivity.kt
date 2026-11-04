package com.irionna.eternalreturninfo.presentation.activity.board

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import coil.load
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.databinding.BoardDeletedActivityBinding

class BoardDeletedActivity : AppCompatActivity() {

    private lateinit var binding: BoardDeletedActivityBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BoardDeletedActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.boardDeletedIv.load(R.drawable.ic_board_alert)

        binding.boardDeletedBtnOk.setOnClickListener {
            finish()
        }

        binding.boardDeletedBtnBack.setOnClickListener {
            finish()
        }

    }
}