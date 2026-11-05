package com.irionna.eternalreturninfo.presentation.fragment.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import com.irionna.eternalreturninfo.databinding.MainFragmentBinding
import androidx.lifecycle.lifecycleScope
import com.irionna.eternalreturninfo.retrofit.BoardSingletone
import com.irionna.eternalreturninfo.util.StatsPanel
import com.irionna.eternalreturninfo.data.model.Notice
import com.irionna.eternalreturninfo.data.model.VideoModel
import com.irionna.eternalreturninfo.presentation.activity.main.WebView
import com.irionna.eternalreturninfo.presentation.adapter.main.NoticeBannerListAdapter
import com.irionna.eternalreturninfo.presentation.adapter.main.VideoListAdapter
import com.irionna.eternalreturninfo.presentation.viewmodel.MainListViewModel

class MainFragment : Fragment() {
    companion object {
        fun newInstance() = MainFragment()
    }

    private var _binding: MainFragmentBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainListViewModel by activityViewModels()

    private val noticeListAdapter by lazy {
        NoticeBannerListAdapter()
    }

    private val videoListAdapter by lazy {
        VideoListAdapter()
    }



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MainFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.mainRvYoutube.adapter = videoListAdapter
        binding.mainRvYoutube.layoutManager = LinearLayoutManager(requireContext())

        binding.mainBannerNotice.adapter = noticeListAdapter
        val bannerVideoLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.mainBannerNotice.layoutManager = bannerVideoLayoutManager

        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.mainBannerNotice)

        val listener = SnapPagerScrollListener(
            snapHelper,
            SnapPagerScrollListener.ON_SCROLL,
            true,
            object : SnapPagerScrollListener.OnChangeListener {
                override fun onSnapped(position: Int) {
                    // position을 받아서 이벤트 처리
                }
            }
        )
        binding.mainBannerNotice.addOnScrollListener(listener)
        binding.mainBannerNotice.addItemDecoration(LinePagerIndicatorDecoration())

        initView()
        initModel()

        // 전적 카드: 시즌·모드 선택 가능 (프로필·듀오 카드와 같은 GameStats 사용)
        StatsPanel(binding.constraintLayout6, requireActivity(), viewLifecycleOwner.lifecycleScope).load {
            StatsPanel.waitForNickname { BoardSingletone.LoginUser().name }
        }
    }



    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
    private fun initView() = with(binding){


        noticeListAdapter.setOnItemClickListener(object : NoticeBannerListAdapter.OnItemClickListener{
            override fun onItemClick(item: Notice, position:Int) {
                val intent = Intent(requireContext(), WebView::class.java)
                intent.putExtra("url", item.url)
                startActivity(intent)
            }
        })

        videoListAdapter.setOnItemClickListener(object : VideoListAdapter.OnItemClickListener{
            override fun onItemClick(item: VideoModel, position: Int) {
                Log.d("item.id", item.id.toString())
                val videoUrl = "https://www.youtube.com/watch?v=${item.id}"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
                intent.setPackage("com.google.android.youtube")
                startActivity(intent)
            }

        })

        mainBtnSeeMore.setOnClickListener {
            val intent = Intent(requireContext(), WebView::class.java)
            intent.putExtra("url", "https://dak.gg/er")
            startActivity(intent)
        }

    }

    private fun initModel() = with(mainViewModel){
        noticeList.observe(viewLifecycleOwner){
            noticeListAdapter.submitList(it)
        }

        videoList.observe(viewLifecycleOwner){
            videoListAdapter.submitList(it)
        }


    }


}
