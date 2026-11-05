package com.irionna.eternalreturninfo.presentation.fragment.main

import android.content.Intent
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import android.widget.TextView
import android.graphics.Typeface
import com.irionna.eternalreturninfo.R
import android.widget.Toast
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.EditorInfo
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
        mainViewModel.loadVideos(requireContext())

        initStatsSearch()
        initVideoTabs()
    }

    /** 전적 검색: 입력한 게임 닉네임의 전적을 시즌·모드별로 보여준다 (내 전적은 프로필 화면에서). */
    private fun initStatsSearch() {
        val panel = StatsPanel(binding.mainStatsPanel, requireActivity(), viewLifecycleOwner.lifecycleScope)

        fun search() {
            val q = binding.mainSearchEt.text.toString().trim()
            if (q.isEmpty()) {
                Toast.makeText(requireContext(), R.string.main_search_empty, Toast.LENGTH_SHORT).show()
                return
            }
            // 키보드를 내리고 결과 카드를 보여준다
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.mainSearchEt.windowToken, 0)
            binding.mainSearchEt.clearFocus()
            binding.mainSearchGuide.visibility = View.GONE
            binding.mainStatsPanel.root.visibility = View.VISIBLE
            panel.load { q }
        }

        binding.mainSearchBtn.setOnClickListener { search() }
        binding.mainSearchEt.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search()
                true
            } else false
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

    // 게임 방송: 공식 채널 / 스트리머 탭
    private var officialVideos: List<VideoModel> = emptyList()
    private var streamerVideos: List<VideoModel> = emptyList()
    private var showStreamer = false

    private fun showVideos() {
        videoListAdapter.submitList((if (showStreamer) streamerVideos else officialVideos).toList())
        styleVideoTab(binding.mainSegOfficial, !showStreamer)
        styleVideoTab(binding.mainSegStreamer, showStreamer)
    }

    private fun styleVideoTab(tab: TextView, selected: Boolean) {
        if (selected) {
            tab.setBackgroundResource(R.drawable.shape_seg_on)
            tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_mint))
            tab.setTypeface(null, Typeface.BOLD)
        } else {
            tab.background = null
            tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            tab.setTypeface(null, Typeface.NORMAL)
        }
    }

    private fun initVideoTabs() {
        binding.mainSegOfficial.setOnClickListener {
            showStreamer = false
            showVideos()
        }
        binding.mainSegStreamer.setOnClickListener {
            showStreamer = true
            mainViewModel.loadStreamerVideos(requireContext()) // 처음 열 때만 실제로 불러온다
            showVideos()
        }
        showVideos()
    }

    private fun initModel() = with(mainViewModel){
        noticeList.observe(viewLifecycleOwner){
            noticeListAdapter.submitList(it)
        }

        videoList.observe(viewLifecycleOwner){
            officialVideos = it
            showVideos()
        }

        streamerList.observe(viewLifecycleOwner){
            streamerVideos = it
            showVideos()
        }


    }


}
