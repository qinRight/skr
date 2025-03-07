package com.module.posts.watch

import android.os.Bundle
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.alibaba.android.arouter.launcher.ARouter
import com.common.base.BaseFragment
import com.module.posts.R
import com.common.base.INVISIBLE_REASON_IN_VIEWPAGER
import com.common.base.INVISIBLE_REASON_TO_DESKTOP
import com.common.base.INVISIBLE_REASON_TO_OTHER_ACTIVITY
import com.common.core.view.setDebounceViewClickListener
import com.common.log.MyLog
import com.common.statistics.StatisticsAdapter
import com.common.utils.U
import com.common.utils.dp
import com.common.view.DebounceViewClickListener
import com.common.view.ex.ExTextView
import com.common.view.titlebar.CommonTitleBar
import com.common.view.viewpager.NestViewPager
import com.common.view.viewpager.SlidingTabLayout
import com.component.busilib.event.PostsWatchTabRefreshEvent
import com.module.RouterConstants
import com.module.posts.statistics.PostsStatistics
import com.module.posts.watch.view.BasePostsWatchView.Companion.TYPE_POST_FOLLOW
import com.module.posts.watch.view.FollowPostsWatchView
import com.module.posts.watch.view.LastPostsWatchView
import com.module.posts.watch.view.RecommendPostsWatchView
import kotlinx.android.synthetic.main.posts_watch_fragment_layout.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import kotlin.properties.Delegates

class PostsWatchFragment : BaseFragment() {

    private var title: CommonTitleBar? = null
    private var divider: View? = null
    private var postsPublishIv: ImageView? = null
    private var postsTopicIv: ImageView? = null
    private var postsTab: SlidingTabLayout? = null
    private var postsVp: NestViewPager? = null

    private var tabPagerAdapter: PagerAdapter? = null

    val followPostsWatchView: FollowPostsWatchView by lazy { FollowPostsWatchView(this.activity!!,TYPE_POST_FOLLOW)}
    val recommendPostsWatchView: RecommendPostsWatchView by lazy { RecommendPostsWatchView(this.activity!!) }
    val lastPostsWatchView: LastPostsWatchView by lazy { LastPostsWatchView(this.activity!!) }

    val initPostion = 1  // 默认推荐
    // 保持 init Postion 一致
    var mPagerPosition: Int by Delegates.observable(initPostion, { _, oldPositon, newPosition ->
        // 为了解决滑动卡顿
        launch(Dispatchers.Main) {
            when (oldPositon) {
                0 -> {
                    followPostsWatchView.unselected(UNSELECT_REASON_SLIDE_OUT)
                }
                1 -> {
                    recommendPostsWatchView.unselected(UNSELECT_REASON_SLIDE_OUT)
                }
                2 -> {
                    lastPostsWatchView.unselected(UNSELECT_REASON_SLIDE_OUT)
                }
            }
            onViewSelected(newPosition)
        }
    })

    override fun initView(): Int {
        return R.layout.posts_watch_fragment_layout
    }

    override fun initData(savedInstanceState: Bundle?) {
        title = rootView.findViewById(R.id.title)
        divider = rootView.findViewById(R.id.divider)
        postsTab = rootView.findViewById(R.id.posts_tab)
        postsPublishIv = rootView.findViewById(R.id.posts_publish_iv)
        postsTopicIv = rootView.findViewById(R.id.posts_topic_iv)
        postsVp = rootView.findViewById(R.id.posts_vp)

        postsPublishIv?.setDebounceViewClickListener {
            StatisticsAdapter.recordCountEvent("posts", "publish_click", null)
            ARouter.getInstance()
                    .build(RouterConstants.ACTIVITY_POSTS_PUBLISH)
                    .navigation()
        }

        postsTopicIv?.setDebounceViewClickListener {
            StatisticsAdapter.recordCountEvent("posts", "topic_all_click", null)
            ARouter.getInstance()
                    .build(RouterConstants.ACTIVITY_POSTS_TOPIC_SELECT)
                    .withInt("from", 1)
                    .navigation()
        }

        postsTab?.apply {
            setCustomTabView(R.layout.posts_tab_view_layout, R.id.tab_tv)
            setDistributeMode(SlidingTabLayout.DISTRIBUTE_MODE_NONE)
            setIndicatorAnimationMode(SlidingTabLayout.ANI_MODE_NORMAL)
            setTitleSize(14f)
            setSelectedTitleSize(24f)
            setIndicatorWidth(16f.dp())
            setSelectedIndicatorColors(U.getColor(R.color.transparent))
            setSelectedIndicatorThickness(4f.dp().toFloat())
            setIndicatorCornorRadius(2f.dp().toFloat())
        }

        tabPagerAdapter = object : PagerAdapter() {

            override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
                MyLog.d(TAG, "destroyItem container=$container position=$position object=$`object`")
                container.removeView(`object` as View)
            }

            override fun instantiateItem(container: ViewGroup, position: Int): Any {
                MyLog.d(TAG, "instantiateItem container=$container position=$position")
                var view: View? = when (position) {
                    0 -> followPostsWatchView
                    1 -> recommendPostsWatchView
                    2 -> lastPostsWatchView
                    else -> null
                }
                if (container.indexOfChild(view) == -1) {
                    container.addView(view)
                }
                return view!!
            }

            override fun getItemPosition(`object`: Any): Int {
                return PagerAdapter.POSITION_NONE
            }

            override fun getCount(): Int {
                return 3
            }

            override fun getPageTitle(position: Int): CharSequence? {
                return when (position) {
                    0 -> "关注"
                    1 -> "推荐"
                    2 -> "最新"
                    else -> super.getPageTitle(position)
                }
            }

            override fun isViewFromObject(view: View, `object`: Any): Boolean {
                return view === `object`
            }
        }

        postsTab?.setOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

            }

            override fun onPageSelected(position: Int) {
                postsTab?.notifyDataChange()
                mPagerPosition = position
            }

            override fun onPageScrollStateChanged(state: Int) {

            }
        })

        postsVp?.adapter = tabPagerAdapter
        postsTab?.setViewPager(postsVp)
        tabPagerAdapter?.notifyDataSetChanged()
        postsVp?.setCurrentItem(initPostion, false)
    }

    fun onViewSelected(pos: Int) {
        if (!this.fragmentVisible) {
            return
        }
        when (pos) {
            0 -> followPostsWatchView.selected()
            1 -> recommendPostsWatchView.selected()
            2 -> lastPostsWatchView.selected()
        }
    }

    var beginTs = System.currentTimeMillis()
    override fun onFragmentVisible() {
        beginTs = System.currentTimeMillis()
        super.onFragmentVisible()
        StatisticsAdapter.recordCountEvent("posts", "tab_expose", null)
        postsVp?.currentItem?.let { onViewSelected(it) }
    }

    override fun onFragmentInvisible(reason: Int) {
        StatisticsAdapter.recordCalculateEvent("posts", "stay_duration", System.currentTimeMillis() - beginTs, null)
        super.onFragmentInvisible(reason)
        MyLog.d(TAG, "onFragmentInvisible reason=$reason")
        var r = UNSELECT_REASON_SLIDE_OUT
        // 滑走导致的不可见  返回桌面 返回桌面
        when (reason) {
            INVISIBLE_REASON_IN_VIEWPAGER -> r = UNSELECT_REASON_TO_OTHER_TAB
            INVISIBLE_REASON_TO_OTHER_ACTIVITY -> r = UNSELECT_REASON_TO_OTHER_ACTIVITY
            INVISIBLE_REASON_TO_DESKTOP -> r = UNSELECT_REASON_TO_DESKTOP
        }
        when (mPagerPosition) {
            0 -> followPostsWatchView.unselected(r)
            1 -> recommendPostsWatchView.unselected(r)
            2 -> lastPostsWatchView.unselected(r)
        }

        if (reason != INVISIBLE_REASON_TO_OTHER_ACTIVITY) {
            // 滑走导致的不可见
            PostsStatistics.tryUpload(true)
        } else {

        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: PostsWatchTabRefreshEvent) {
        // 首页重复点击了神曲，自动刷新一下吧
        when {
            postsVp?.currentItem == 0 -> // 关注自动刷新
                followPostsWatchView.autoRefresh()
            postsVp?.currentItem == 1 -> // 推荐自动刷新
                recommendPostsWatchView.autoRefresh()
            postsVp?.currentItem == 2 -> // 最新的自动刷新
                lastPostsWatchView.autoRefresh()
        }
    }

    override fun isBlackStatusBarText(): Boolean {
        return true
    }

    override fun useEventBus(): Boolean {
        return true
    }

    override fun destroy() {
        super.destroy()
        followPostsWatchView.destory()
        recommendPostsWatchView.destory()
        lastPostsWatchView.destory()
    }
}

const val UNSELECT_REASON_SLIDE_OUT = 1 // tab滑走
const val UNSELECT_REASON_TO_DESKTOP = 2  // 到桌面
const val UNSELECT_REASON_TO_OTHER_ACTIVITY = 3 // 到别的activity
const val UNSELECT_REASON_TO_OTHER_TAB = 4//  feed tab滑走
