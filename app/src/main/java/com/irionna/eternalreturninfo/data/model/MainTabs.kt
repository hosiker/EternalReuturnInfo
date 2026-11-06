package com.irionna.eternalreturninfo.data.model

import androidx.fragment.app.Fragment

/**
 * 탭 하나: 화면을 만드는 방법과 제목.
 * 화면 객체를 미리 만들어 두고 돌려쓰면 ViewPager2 가 화면을 없앴다 다시 만들 때 같은 객체를 다른 자리에 끼워
 * 엉뚱한 탭이 보이는 문제가 생긴다. 그래서 필요할 때마다 새로 만들도록 만드는 함수([create])를 보관한다.
 */
data class MainTabs(
    val create: () -> Fragment,
    val titleRes: Int
)
