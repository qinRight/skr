package com.module.playways.songmanager.fragment

import android.os.Bundle
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup

import com.common.base.BaseActivity
import com.common.base.BaseFragment
import com.common.base.FragmentDataListener
import com.common.log.MyLog
import com.common.utils.FragmentUtils
import com.common.utils.U
import com.common.view.DebounceViewClickListener
import com.common.view.ex.ExTextView
import com.common.view.titlebar.CommonTitleBar
import com.common.view.viewpager.SlidingTabLayout
import com.module.playways.R
import com.module.playways.doubleplay.DoubleRoomData
import com.module.playways.doubleplay.event.NoMusicEvent
import com.module.playways.doubleplay.model.DoubleCurSongInfoEvent
import com.module.playways.doubleplay.pushEvent.DoubleAddMusicEvent
import com.module.playways.doubleplay.pushEvent.DoubleDelMusicEvent
import com.module.playways.room.song.fragment.SearchSongFragment
import com.module.playways.room.song.model.SongModel
import com.module.playways.songmanager.SongManagerActivity
import com.module.playways.songmanager.event.AddSongEvent
import com.module.playways.songmanager.event.SongNumChangeEvent
import com.module.playways.songmanager.model.RecommendTagModel
import com.module.playways.songmanager.presenter.DoubleSongManagePresenter
import com.module.playways.songmanager.view.GrabExistSongManageView
import com.module.playways.songmanager.view.GrabSongWishView
import com.module.playways.songmanager.view.ISongManageView
import com.module.playways.songmanager.view.RecommendSongView

import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class DoubleSongManageFragment : BaseFragment(), ISongManageView {
    lateinit var mTitlebar: CommonTitleBar
    lateinit var mSearchSongIv: ExTextView
    lateinit var mTagTab: SlidingTabLayout
    lateinit var mViewpager: ViewPager

    lateinit var mPresenter: DoubleSongManagePresenter
    lateinit var mPagerAdapter: PagerAdapter

    private var mRoomData: DoubleRoomData? = null
    private var mTagModelList: List<RecommendTagModel>? = null

    override fun initView(): Int {
        return R.layout.double_song_manage_fragment_layout
    }

    override fun initData(savedInstanceState: Bundle?) {
        if (mRoomData == null) {
            activity?.finish()
            return
        }

        mTitlebar = rootView.findViewById(R.id.titlebar)
        mSearchSongIv = rootView.findViewById(R.id.search_song_iv)
        mTagTab = rootView.findViewById(R.id.tag_tab)
        mViewpager = rootView.findViewById(R.id.viewpager)

        mTitlebar.leftTextView.setOnClickListener(object : DebounceViewClickListener() {
            override fun clickValid(v: View) {
                if (activity is SongManagerActivity) {
                    if (activity != null) {
                        activity!!.finish()
                    }
                } else {
                    finish()
                }
            }
        })

        mTitlebar.rightTextView.setOnClickListener(object : DebounceViewClickListener() {
            override fun clickValid(v: View) {
                U.getFragmentUtils().addFragment(FragmentUtils.newAddParamsBuilder(context as BaseActivity?, DoubleExistSongManageFragment::class.java)
                        .setAddToBackStack(true)
                        .setHasAnimation(true)
                        .addDataBeforeAdd(0, mRoomData)
                        .build())
            }
        })

        mSearchSongIv.setOnClickListener(object : DebounceViewClickListener() {
            override fun clickValid(v: View) {
                U.getFragmentUtils().addFragment(FragmentUtils.newAddParamsBuilder(context as BaseActivity?, SearchSongFragment::class.java)
                        .setAddToBackStack(true)
                        .setHasAnimation(true)
                        .addDataBeforeAdd(0, SongManagerActivity.TYPE_FROM_DOUBLE)
                        .addDataBeforeAdd(1, false)
                        .setFragmentDataListener(object : FragmentDataListener {
                            override fun onFragmentResult(requestCode: Int, resultCode: Int, bundle: Bundle?, obj: Any?) {
                                if (requestCode == 0 && resultCode == 0 && obj != null) {
                                    val model = obj as SongModel
                                    MyLog.d(TAG, "onFragmentResult model=$model")
                                    EventBus.getDefault().post(AddSongEvent(model, SongManagerActivity.TYPE_FROM_DOUBLE))
                                }
                            }
                        })
                        .build())
            }
        })

        mPresenter = DoubleSongManagePresenter(this, mRoomData!!)
        addPresent(mPresenter)

        showRoomName(mRoomData?.localCombineRoomMusic?.music?.displaySongName)
        mPresenter.getRecommendTag()
        mPresenter.getAddMusicCnt()
    }

    override fun useEventBus(): Boolean {
        return true
    }

    override fun showRoomName(roomName: String?) {
        if (TextUtils.isEmpty(roomName)) {
            mTitlebar.centerTextView.text = "点歌"
        } else {
            mTitlebar.centerTextView.text = "正在演唱《${roomName}》"
        }
    }

    override fun showAddSongCnt(cnt: Int) {
        mTitlebar.rightTextView.text = "已点$cnt"
    }


    override fun showRecommendSong(recommendTagModelList: MutableList<RecommendTagModel>) {
        if (recommendTagModelList == null || recommendTagModelList.size == 0) {
            return
        }

        mTagModelList = recommendTagModelList
        mTagTab.setCustomTabView(R.layout.manage_song_tab, R.id.tab_tv)
        mTagTab.setSelectedIndicatorColors(U.getColor(R.color.white_trans_20))
        mTagTab.setDistributeMode(SlidingTabLayout.DISTRIBUTE_MODE_NONE)
        mTagTab.setIndicatorAnimationMode(SlidingTabLayout.ANI_MODE_NORMAL)
        mTagTab.setSelectedIndicatorThickness(U.getDisplayUtils().dip2px(24f).toFloat())
        mTagTab.setIndicatorCornorRadius(U.getDisplayUtils().dip2px(12f).toFloat())
        mPagerAdapter = object : PagerAdapter() {

            override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
                MyLog.d(TAG, "destroyItem container=$container position=$position object=$`object`")
                container.removeView(`object` as View)
            }

            override fun instantiateItem(container: ViewGroup, position: Int): Any {
                MyLog.d(TAG, "instantiateItem container=$container position=$position")
                return instantiateItemDouble(container, position, mTagModelList!!)
            }

            override fun getCount(): Int {
                return mTagModelList!!.size
            }

            override fun isViewFromObject(view: View, `object`: Any): Boolean {
                return view === `object`
            }

            override fun getPageTitle(position: Int): CharSequence? {
                return mTagModelList!![position].name
            }
        }

        mTagTab.setOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

            }

            override fun onPageSelected(position: Int) {
                val view = mViewpager.findViewWithTag<View>(position)
                if (view != null) {
                    if (view is RecommendSongView) {
                        view.tryLoad()
                    } else if (view is GrabSongWishView) {
                        view.tryLoad()
                    } else if (view is GrabExistSongManageView) {
                        view.tryLoad()
                    }
                }
            }

            override fun onPageScrollStateChanged(state: Int) {

            }
        })

        mViewpager.adapter = mPagerAdapter
        mTagTab.setViewPager(mViewpager)
        mPagerAdapter.notifyDataSetChanged()

        mViewpager.currentItem = 1

    }

    fun instantiateItemDouble(container: ViewGroup, position: Int, recommendTagModelList: List<RecommendTagModel>): Any {
        MyLog.d(TAG, "instantiateItem container=$container position=$position")
        val view: View
        val recommendTagModel = recommendTagModelList[position]
        val recommendSongView = RecommendSongView(activity!!, SongManagerActivity.TYPE_FROM_DOUBLE,
                false, mRoomData!!.gameId, recommendTagModel)
        recommendSongView.tag = position
        view = recommendSongView

        if (container.indexOfChild(view) == -1) {
            container.addView(view)
        }
        return view
    }

    override fun setData(type: Int, data: Any?) {
        super.setData(type, data)
        if (type == 0) {
            mRoomData = data as DoubleRoomData?
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: SongNumChangeEvent) {
        mTitlebar.rightTextView.text = "已点${event.songNum}"
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: DoubleCurSongInfoEvent) {
        showRoomName(event.name)
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: NoMusicEvent) {
        showRoomName("")
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: DoubleAddMusicEvent) {
        mPresenter.getAddMusicCnt()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: DoubleDelMusicEvent) {
        mPresenter.getAddMusicCnt()
    }

}
