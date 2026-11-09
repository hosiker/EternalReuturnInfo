package com.irionna.eternalreturninfo.presentation.fragment.main

import com.irionna.eternalreturninfo.util.AppToast
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import android.widget.TextView
import android.graphics.Typeface
import com.irionna.eternalreturninfo.R
import android.widget.Toast
import com.irionna.eternalreturninfo.retrofit.BoardSingletone
import com.irionna.eternalreturninfo.util.CharacterPicker
import com.irionna.eternalreturninfo.util.Characters
import com.irionna.eternalreturninfo.util.InitialAvatar
import com.irionna.eternalreturninfo.util.RecentGamesSection
import com.irionna.eternalreturninfo.util.RecentSearches
import android.widget.LinearLayout
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
        private const val SORT_DATE = "date"
        private const val SORT_VIEWS = "viewCount"
        private const val TYPE_ALL = 0
        private const val TYPE_VIDEO = 1
        private const val TYPE_SHORT = 2
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
        mainViewModel.loadFreeCharacters(requireContext())

        initStatsSearch()
        initVideoTabs()
        initRefresh()
    }

    /** 전적 검색: 입력한 게임 닉네임의 전적을 시즌·모드별로 보여준다. 내 전적 칩과 최근 검색 칩(최대 5개)으로 바로 검색할 수 있다. */
    private fun initStatsSearch() {
        // 전적이 실제로 나온 닉네임만 최근 검색에 저장한다 (서버가 알려 준 정확한 철자로)
        val panel = StatsPanel(binding.mainStatsPanel, requireActivity(), viewLifecycleOwner.lifecycleScope) { stats ->
            val mine = BoardSingletone.LoginUser().name?.trim().orEmpty()
            // 내 닉네임은 [내 전적] 칩이 있으니 최근 검색에는 따로 남기지 않는다
            if (stats != null && stats.nickname.isNotBlank() && !stats.nickname.equals(mine, ignoreCase = true)) {
                RecentSearches.add(requireContext(), stats.nickname)
                refreshChips()
            }
        }
        searchPanel = panel
        // 전적 카드가 유저를 찾으면 같은 유저의 최근 3경기를 이어서 불러온다
        val recent = RecentGamesSection(binding.mainRecentGames, requireActivity(), viewLifecycleOwner.lifecycleScope, detailed = false, limit = 3)
        recentSection = recent
        panel.onUserResolved = { recent.load(it) }

        binding.mainSearchBtn.setOnClickListener { search(binding.mainSearchEt.text.toString()) }
        binding.mainSearchEt.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search(binding.mainSearchEt.text.toString())
                true
            } else false
        }
        refreshChips()
        // 내 닉네임은 로그인 직후 조금 늦게 도착한다. 도착하면 [내 전적] 칩을 다시 만든다.
        BoardSingletone.loginUserLive.observe(viewLifecycleOwner) { refreshChips() }
    }

    private var searchPanel: StatsPanel? = null
    private var recentSection: RecentGamesSection? = null

    private fun search(input: String) {
        val q = input.trim()
        if (q.isEmpty()) {
            AppToast.show(requireContext(), R.string.main_search_empty, AppToast.Kind.WARNING)
            return
        }
        binding.mainSearchEt.setText(q)
        // 키보드를 내리고 결과 카드를 보여준다
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.mainSearchEt.windowToken, 0)
        binding.mainSearchEt.clearFocus()
        binding.mainSearchGuide.visibility = View.GONE
        binding.mainStatsPanel.root.visibility = View.VISIBLE
        binding.mainRecentGames.root.visibility = View.VISIBLE
        recentSection?.reset()
        searchPanel?.load { q }
        // 결과 카드가 화면 안에 보이도록 스크롤한다
        binding.mainScroll.post { _binding?.mainScroll?.smoothScrollTo(0, binding.mainStatsContainer.top) }
    }

    /** 검색창 아래 칩: [내 전적] + 최근 검색 닉네임. 비어 있으면 줄 자체를 숨긴다. */
    private fun refreshChips() {
        val b = _binding ?: return
        val ctx = requireContext()
        val dp = resources.displayMetrics.density
        val row = b.mainChips
        row.removeAllViews()

        fun chip(label: String, accent: Boolean, onClick: () -> Unit): TextView = TextView(ctx).apply {
            text = label
            textSize = 13f
            gravity = android.view.Gravity.CENTER
            minHeight = (40 * dp).toInt()
            setPadding((14 * dp).toInt(), 0, (14 * dp).toInt(), 0)
            setBackgroundResource(R.drawable.shape_chip)
            setTextColor(ContextCompat.getColor(ctx, if (accent) R.color.brand_mint else R.color.white))
            setOnClickListener { onClick() }
        }

        fun gap() = View(ctx).apply { layoutParams = LinearLayout.LayoutParams((8 * dp).toInt(), 1) }

        val mine = BoardSingletone.LoginUser().name?.trim().orEmpty()
        if (mine.isNotEmpty()) {
            row.addView(chip(getString(R.string.main_chip_mine), true) { search(mine) })
        }
        RecentSearches.get(ctx).forEach { name ->
            if (row.childCount > 0) row.addView(gap())
            // 닉네임 칩 + 삭제(✕)
            val box = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setBackgroundResource(R.drawable.shape_chip)
            }
            box.addView(TextView(ctx).apply {
                text = name
                textSize = 13f
                gravity = android.view.Gravity.CENTER
                minHeight = (40 * dp).toInt()
                setPadding((14 * dp).toInt(), 0, (4 * dp).toInt(), 0)
                setTextColor(ContextCompat.getColor(ctx, R.color.white))
                setOnClickListener { search(name) }
            })
            box.addView(TextView(ctx).apply {
                text = "✕"
                textSize = 12f
                gravity = android.view.Gravity.CENTER
                minWidth = (36 * dp).toInt()
                minHeight = (40 * dp).toInt()
                setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
                contentDescription = getString(R.string.main_chip_remove_desc, name)
                setOnClickListener {
                    RecentSearches.remove(ctx, name)
                    refreshChips()
                }
            })
            row.addView(box)
        }
        b.mainChipsScroll.visibility = if (row.childCount > 0) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        // 로그인 직후에는 내 닉네임이 늦게 준비될 수 있어서 화면이 보일 때 칩을 다시 만든다
        refreshChips()
    }

    /**
     * 무료 실험체 줄. 서버가 주는 번호는 게임 번호라서 [Characters] 로 앱 목록 위치로 바꿔 얼굴을 보여준다.
     * 앱에 얼굴 이미지가 없는 신규 실험체는 이름 첫 글자 아바타 + 이름으로 보여준다. 목록이 비면 줄을 숨긴다.
     */
    private fun showFreeCharacters(codes: List<Int>) {
        val b = _binding ?: return
        val ctx = requireContext()
        val dp = resources.displayMetrics.density
        val row = b.mainFreeRow
        row.removeAllViews()

        codes.distinct().sorted().forEach { code ->
            val name = Characters.nameOf(ctx, code)
            val appIndex = Characters.appIndexOf(code)
            val item = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams((64 * dp).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            val img = de.hdodenhof.circleimageview.CircleImageView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams((56 * dp).toInt(), (56 * dp).toInt())
                scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                circleBackgroundColor = ContextCompat.getColor(ctx, R.color.surface_stroke)
                contentDescription = name
            }
            if (appIndex != null) CharacterPicker.loadFace(img, appIndex) else img.setImageDrawable(InitialAvatar.drawable(ctx, name))
            val label = TextView(ctx).apply {
                text = name
                textSize = 11f
                gravity = android.view.Gravity.CENTER
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(0, (5 * dp).toInt(), 0, 0)
                setTextColor(ContextCompat.getColor(ctx, R.color.white))
            }
            item.addView(img)
            item.addView(label)
            row.addView(item)
        }
        b.mainFreeContainer.visibility = if (row.childCount > 0) View.VISIBLE else View.GONE
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
                val videoUrl = "https://www.youtube.com/watch?v=${item.id}"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
                intent.setPackage("com.google.android.youtube")
                try {
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    // YouTube 앱이 없으면 브라우저 등 다른 앱으로 연다
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl)))
                    } catch (e2: ActivityNotFoundException) {
                        AppToast.show(requireContext(), getString(R.string.privacy_policy_open_failed, videoUrl), AppToast.Kind.ERROR)
                    }
                }
            }

        })

        mainBtnSeeMore.setOnClickListener {
            val intent = Intent(requireContext(), WebView::class.java)
            intent.putExtra("url", "https://dak.gg/er")
            startActivity(intent)
        }

    }

    // 게임 방송: 공식 채널 / 스트리머 탭 (스트리머는 최신순·조회순 검색 결과)
    private var officialVideos: List<VideoModel> = emptyList()
    private val streamerVideos = HashMap<String, List<VideoModel>>()
    private var showStreamer = false
    private var streamerSort = SORT_DATE

    // 영상 종류: 전체 / 영상 / 쇼츠 (쇼츠 여부는 영상 길이로 판단한다)
    private var typeFilter = TYPE_ALL

    private fun showVideos() {
        val source = if (showStreamer) streamerVideos[streamerSort].orEmpty() else officialVideos
        val list = when (typeFilter) {
            TYPE_VIDEO -> source.filterNot { it.isShort }
            TYPE_SHORT -> source.filter { it.isShort }
            else -> source
        }
        videoListAdapter.submitList(list.toList())
        styleVideoTab(binding.mainTypeAll, typeFilter == TYPE_ALL)
        styleVideoTab(binding.mainTypeVideo, typeFilter == TYPE_VIDEO)
        styleVideoTab(binding.mainTypeShort, typeFilter == TYPE_SHORT)
        binding.mainVideoEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        styleVideoTab(binding.mainSegOfficial, !showStreamer)
        styleVideoTab(binding.mainSegStreamer, showStreamer)
        binding.mainSortSeg.visibility = if (showStreamer) View.VISIBLE else View.GONE
        styleVideoTab(binding.mainSortLatest, streamerSort == SORT_DATE)
        styleVideoTab(binding.mainSortViews, streamerSort == SORT_VIEWS)
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

    /** 아래로 당겨서 새로고침. 로딩 표시는 잠깐만 보여준다. */
    private fun initRefresh() {
        binding.mainRefresh.setColorSchemeResources(R.color.brand_green)
        binding.mainRefresh.setProgressBackgroundColorSchemeResource(R.color.surface)
        binding.mainRefresh.setOnRefreshListener {
            mainViewModel.refreshHome(requireContext())
            binding.mainRefresh.postDelayed({
                _binding?.mainRefresh?.isRefreshing = false
            }, 1200)
        }
    }

    private fun initVideoTabs() {
        binding.mainSegOfficial.setOnClickListener {
            showStreamer = false
            showVideos()
        }
        binding.mainSegStreamer.setOnClickListener {
            showStreamer = true
            mainViewModel.loadStreamerVideos(requireContext(), streamerSort) // 처음 열 때만 실제로 불러온다
            showVideos()
        }
        binding.mainTypeAll.setOnClickListener { typeFilter = TYPE_ALL; showVideos() }
        binding.mainTypeVideo.setOnClickListener { typeFilter = TYPE_VIDEO; showVideos() }
        binding.mainTypeShort.setOnClickListener { typeFilter = TYPE_SHORT; showVideos() }
        binding.mainSortLatest.setOnClickListener { selectSort(SORT_DATE) }
        binding.mainSortViews.setOnClickListener { selectSort(SORT_VIEWS) }
        showVideos()
    }

    private fun selectSort(sort: String) {
        streamerSort = sort
        mainViewModel.loadStreamerVideos(requireContext(), sort)
        showVideos()
    }

    private fun initModel() = with(mainViewModel){
        noticeList.observe(viewLifecycleOwner){
            noticeListAdapter.submitList(it)
        }

        // 공지를 못 받았으면 배너 자리에 안내 문구
        freeCharacters.observe(viewLifecycleOwner){ codes -> showFreeCharacters(codes) }

        noticeFailed.observe(viewLifecycleOwner){ failed ->
            binding.mainBannerEmpty.visibility = if (failed) View.VISIBLE else View.GONE
        }

        videoList.observe(viewLifecycleOwner){
            officialVideos = it
            showVideos()
        }

        streamerList.observe(viewLifecycleOwner){ (sort, list) ->
            streamerVideos[sort] = list
            showVideos()
        }


    }


}
