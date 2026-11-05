package com.irionna.eternalreturninfo.presentation.fragment.myprofile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.databinding.MyprofileRecordFragmentBinding
import com.irionna.eternalreturninfo.retrofit.BoardSingletone
import androidx.lifecycle.lifecycleScope
import com.irionna.eternalreturninfo.presentation.adapter.myprofile.MyprofileListAdapter
import com.irionna.eternalreturninfo.util.StatsPanel

class MyprofileRecordFragment : Fragment() {
    companion object {
        fun newInstance() = MyprofileRecordFragment()
    }

    private var _binding: MyprofileRecordFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MyprofileRecordFragmentBinding.inflate(inflater, container, false)
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

    private fun initView() {
        val chars = resources.getStringArray(R.array.characterName)
        val adapter = MyprofileListAdapter(mutableListOf(), chars)
        binding.myprofileCharacterRv.layoutManager = LinearLayoutManager(requireContext())
        binding.myprofileCharacterRv.adapter = adapter

        // 전적 카드에서 시즌·모드를 바꾸면 아래 실험체 목록도 그 기록으로 바뀐다
        StatsPanel(
            binding.myprofileStats,
            requireActivity(),
            viewLifecycleOwner.lifecycleScope
        ) { stats ->
            val list = stats?.characterStats.orEmpty().sortedByDescending { it.usages }.take(10)
            adapter.update(list)
        }.load {
            StatsPanel.waitForNickname { BoardSingletone.LoginUser().name }
        }
    }
}
