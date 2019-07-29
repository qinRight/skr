package com.module.feeds.detail.fragment

import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CollapsingToolbarLayout
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import com.common.base.BaseFragment
import com.common.core.avatar.AvatarUtils
import com.common.core.share.SharePanel
import com.common.core.share.ShareType
import com.common.core.userinfo.UserInfoManager
import com.common.core.userinfo.event.RelationChangeEvent
import com.common.image.fresco.BaseImageView
import com.common.log.MyLog
import com.common.player.IPlayerCallback
import com.common.player.MyMediaPlayer
import com.common.player.event.PlayerEvent
import com.common.utils.U
import com.common.view.DebounceViewClickListener
import com.common.view.ex.ExImageView
import com.common.view.ex.ExTextView
import com.common.view.ex.drawable.DrawableCreator
import com.common.view.titlebar.CommonTitleBar
import com.module.feeds.watch.model.FeedsWatchModel
import com.component.dialog.FeedsMoreDialogView
import com.module.feeds.R
import com.module.feeds.detail.inter.IFeedsDetailView
import com.module.feeds.detail.model.FirstLevelCommentModel
import com.module.feeds.detail.presenter.FeedsDetailPresenter
import com.module.feeds.detail.view.FeedsCommentView
import com.module.feeds.detail.view.FeedsCommonLyricView
import com.module.feeds.detail.view.FeedsInputContainerView
import com.module.feeds.watch.view.FeedsRecordAnimationView
import com.umeng.socialize.UMShareListener
import com.umeng.socialize.bean.SHARE_MEDIA
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class FeedsDetailFragment : BaseFragment(), IFeedsDetailView {
    val mTag = "FeedsDetailFragment"
    var mContainer: LinearLayout? = null
    var mAppbar: AppBarLayout? = null
    var mContentLayout: CollapsingToolbarLayout? = null
    var mBlurBg: BaseImageView? = null
    var mXinIv: ExImageView? = null
    var mShareIv: ExImageView? = null
    var mBtnBack: ImageView? = null
    var mSongNameTv: ExTextView? = null
    var mMoreTv: ExImageView? = null
    var mControlTv: ExImageView? = null
    var mPassTimeTv: ExTextView? = null
    var mLastTimeTv: ExTextView? = null
    var mSeekBar: SeekBar? = null
    var mSingerIv: BaseImageView? = null
    var mNameTv: ExTextView? = null
    var mCommentTimeTv: ExTextView? = null
    var mFollowTv: ExTextView? = null
    var mMainCommentTv: ExTextView? = null
    var mCommentTv: ExTextView? = null
    var mXinNumTv: ExTextView? = null
    var mShareNumTv: ExTextView? = null
    var mToolbar: Toolbar? = null
    var mToolbarLayout: ConstraintLayout? = null
    var mFeedsCommonLyricView: FeedsCommonLyricView? = null
    var mFeedsCommentView: FeedsCommentView? = null
    var mRadioView: FeedsRecordAnimationView? = null
    var mCommonTitleBar: CommonTitleBar? = null
    var mFeedsDetailPresenter: FeedsDetailPresenter? = null
    var mMoreDialogPlus: FeedsMoreDialogView? = null

    var mIsSongStart = false

    var mFeedsInputContainerView: FeedsInputContainerView? = null

    var mFeedsWatchModel: FeedsWatchModel? = null

    var mResumeCall: (() -> Unit)? = null

    val mMyMediaPlayer: MyMediaPlayer by lazy {
        MyMediaPlayer().also {
            it.setMonitorProgress(true)
            it.setCallback(object : IPlayerCallback {
                override fun onPrepared() {
                    if (mControlTv!!.isSelected) {
                        if (!mFeedsCommonLyricView!!.isStart()) {
                            mFeedsCommonLyricView!!.playLyric()
                        } else {
                            mFeedsCommonLyricView!!.resume()
                        }
                    } else {
                        mMyMediaPlayer.pause()
                    }
                }

                override fun onCompletion() {
                    stopSong()
                }

                override fun onSeekComplete() {

                }

                override fun onVideoSizeChanged(width: Int, height: Int) {

                }

                override fun onError(what: Int, extra: Int) {
                    mFeedsCommonLyricView!!.pause()
                }

                override fun onInfo(what: Int, extra: Int) {

                }

                override fun onBufferingUpdate(mp: MediaPlayer?, percent: Int) {
                    MyLog.d(mTag, "onBufferingUpdate percent=$percent")
                    if (percent == 100) {
                        if (mp!!.isPlaying) {
                            mFeedsCommonLyricView!!.resume()
                        }
                    } else {
                        mFeedsCommonLyricView!!.pause()
                    }
                }
            })
        }
    }

    internal var isInitToolbar = false
    internal var mIsPlaying = false

    override fun initView(): Int {
        return R.layout.feeds_detail_fragment_layout
    }

    override fun initData(savedInstanceState: Bundle?) {
        if (mFeedsWatchModel == null) {
            activity?.finish()
            return
        }

        mContainer = rootView.findViewById(com.module.feeds.R.id.container)
        mCommonTitleBar = rootView.findViewById(com.module.feeds.R.id.titlebar)
        mAppbar = rootView.findViewById(com.module.feeds.R.id.appbar)
        mContentLayout = rootView.findViewById(com.module.feeds.R.id.content_layout)
        mBlurBg = rootView.findViewById(com.module.feeds.R.id.blur_bg)
        mBtnBack = rootView.findViewById(com.module.feeds.R.id.btn_back) as ImageView
        mSongNameTv = rootView.findViewById(com.module.feeds.R.id.song_name_tv)
        mMoreTv = rootView.findViewById(com.module.feeds.R.id.more_iv)
        mControlTv = rootView.findViewById(com.module.feeds.R.id.control_tv)
        mPassTimeTv = rootView.findViewById(com.module.feeds.R.id.pass_time_tv)
        mLastTimeTv = rootView.findViewById(com.module.feeds.R.id.last_time_tv)
        mSeekBar = rootView.findViewById(com.module.feeds.R.id.seek_bar)
        mSingerIv = rootView.findViewById(com.module.feeds.R.id.singer_iv)
        mNameTv = rootView.findViewById(com.module.feeds.R.id.name_tv)
        mCommentTimeTv = rootView.findViewById(com.module.feeds.R.id.comment_time_tv)
        mFollowTv = rootView.findViewById(com.module.feeds.R.id.follow_tv)
        mMainCommentTv = rootView.findViewById(com.module.feeds.R.id.main_comment_tv)
        mToolbar = rootView.findViewById(com.module.feeds.R.id.toolbar)
        mToolbarLayout = rootView.findViewById(com.module.feeds.R.id.toolbar_layout)
        mCommentTv = rootView.findViewById(com.module.feeds.R.id.comment_tv)
        mXinIv = rootView.findViewById(com.module.feeds.R.id.xin_iv)
        mXinNumTv = rootView.findViewById(com.module.feeds.R.id.xin_num_tv)
        mShareIv = rootView.findViewById(com.module.feeds.R.id.share_iv)
        mShareNumTv = rootView.findViewById(com.module.feeds.R.id.share_num_tv)
        mFeedsInputContainerView = rootView.findViewById(com.module.feeds.R.id.feeds_input_container_view)
        mRadioView = rootView.findViewById(com.module.feeds.R.id.radio_view)
        mFeedsCommonLyricView = FeedsCommonLyricView(rootView)
        mFeedsCommentView = rootView.findViewById(com.module.feeds.R.id.feedsCommentView)
        mFeedsDetailPresenter = FeedsDetailPresenter(this)
        addPresent(mFeedsDetailPresenter)

        mFeedsCommentView?.setFeedsID(mFeedsWatchModel!!)
        mFeedsWatchModel?.song?.songTpl?.songName?.let {
            mSongNameTv?.text = it
            mCommonTitleBar?.centerTextView?.text = "正在播放《${it}》"
        }

        mFeedsInputContainerView?.mSendCallBack = { s ->
            mFeedsDetailPresenter?.addComment(s, mFeedsWatchModel!!.feedID!!)
        }

        AvatarUtils.loadAvatarByUrl(mBlurBg, AvatarUtils.newParamsBuilder(mFeedsWatchModel?.user?.avatar)
                .setCircle(false)
                .setBlur(true)
                .build())

        mFeedsWatchModel?.song?.playDurMs?.let {
            mLastTimeTv?.text = U.getDateTimeUtils().formatTimeStringForDate(it.toLong(), "mm:ss")
        }

        mAppbar?.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            var srollLimit = appBarLayout.totalScrollRange - U.getDisplayUtils().dip2px(95f)
            if (U.getDeviceUtils().hasNotch(U.app())) {
                srollLimit = srollLimit - U.getStatusBarUtil().getStatusBarHeight(U.app())
            }
            if (verticalOffset == 0) {
                // 展开状态
                if (mToolbar?.getVisibility() != View.GONE) {
                    mToolbar?.setVisibility(View.GONE)
                }
            } else if (Math.abs(verticalOffset) >= srollLimit) {
                // 完全收缩状态
                if (mToolbar?.getVisibility() != View.VISIBLE) {
                    if (U.getDeviceUtils().hasNotch(U.app()) && !isInitToolbar) {
                        val params = mToolbarLayout?.getLayoutParams()
                        params!!.height = params!!.height + U.getStatusBarUtil().getStatusBarHeight(U.app())
                        mToolbarLayout?.setLayoutParams(params)
                        isInitToolbar = true
                    }
                    mToolbar?.setVisibility(View.VISIBLE)
                }
            } else {
                // TODO: 2019/4/8 过程中，可以加动画，先直接显示
                if (mToolbar?.getVisibility() != View.GONE) {
                    mToolbar?.setVisibility(View.GONE)
                }
            }
        }

        mCommonTitleBar?.leftTextView?.setDebounceViewClickListener {
            activity?.finish()
        }

        mBtnBack?.setDebounceViewClickListener {
            activity?.finish()
        }

        mShareIv?.setDebounceViewClickListener {
            val sharePanel = SharePanel(activity)
            sharePanel.setShareContent("http://res-static.inframe.mobi/common/skr-share.png")
            sharePanel.show(ShareType.IMAGE_RUL)
            sharePanel.setUMShareListener(object : UMShareListener {
                override fun onResult(p0: SHARE_MEDIA?) {

                }

                override fun onCancel(p0: SHARE_MEDIA?) {

                }

                override fun onError(p0: SHARE_MEDIA?, p1: Throwable?) {

                }

                override fun onStart(p0: SHARE_MEDIA?) {
                    mFeedsWatchModel?.shareCnt = mFeedsWatchModel?.shareCnt?.plus(1)
                    mShareNumTv?.text = mFeedsWatchModel?.shareCnt.toString()
                }
            })
        }

        mControlTv?.setDebounceViewClickListener {
            if (mIsPlaying) {
                pauseSong()
                mIsPlaying = false
                mControlTv?.isSelected = false
            } else {
                playSong()
                mIsPlaying = true
                mControlTv?.isSelected = true
            }
        }

        AvatarUtils.loadAvatarByUrl(mSingerIv, AvatarUtils.newParamsBuilder(mFeedsWatchModel?.user?.avatar)
                .setCircle(true)
                .build())

        mNameTv?.text = mFeedsWatchModel?.user?.nickname
        mFeedsWatchModel?.song?.createdAt?.let {
            mCommentTimeTv?.text = U.getDateTimeUtils().formatHumanableDateForSkrFeed(it, System.currentTimeMillis())
        }

        if (!TextUtils.isEmpty(mFeedsWatchModel?.song?.title)) {
            mMainCommentTv?.text = mFeedsWatchModel?.song?.title
            mMainCommentTv?.visibility = View.VISIBLE
        } else {
            mMainCommentTv?.visibility = View.GONE
        }

        mCommentTv?.setDebounceViewClickListener {
            mFeedsInputContainerView?.showSoftInput()
        }

        mXinIv?.setDebounceViewClickListener {
            mFeedsDetailPresenter?.likeFeeds(!mXinIv!!.isSelected, mFeedsWatchModel!!.feedID!!)
        }

        mFollowTv?.setDebounceViewClickListener {

        }

        mSeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mMyMediaPlayer.seekTo(progress.toLong())
                    mFeedsCommonLyricView?.seekTo(progress)
                    mPassTimeTv?.text = U.getDateTimeUtils().formatTimeStringForDate(progress.toLong(), "mm:ss")
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })

        mFeedsWatchModel?.user?.avatar?.let {
            mRadioView?.setAvatar(it)
        }
        mShareNumTv?.text = mFeedsWatchModel?.shareCnt.toString()
        mXinNumTv?.text = mFeedsWatchModel?.starCnt.toString()
        mFeedsCommentView?.feedsCommendAdapter?.mCommentNum = mFeedsWatchModel?.commentCnt!!

        mRadioView?.avatarContainer?.setDebounceViewClickListener {
            mControlTv?.callOnClick()
        }

        mFeedsCommonLyricView?.setSongModel(mFeedsWatchModel!!.song!!)
        playSong()

        mControlTv?.setDebounceViewClickListener {
            if (it!!.isSelected) {
                pauseSong()
            } else {
                resumeSong()
            }
        }

        mMoreTv?.setDebounceViewClickListener {
            showMoreOp()
        }

        mCommonTitleBar?.rightImageButton?.setDebounceViewClickListener {
            showMoreOp()
        }

        mFeedsDetailPresenter?.getRelation(mFeedsWatchModel!!.user!!.userID!!)
    }

    private fun showMoreOp() {
        mMoreDialogPlus?.dismiss()
        activity?.let {
            mMoreDialogPlus = FeedsMoreDialogView(it, FeedsMoreDialogView.FROM_COMMENT
                    , mFeedsWatchModel?.user?.userID ?: 0
                    , mFeedsWatchModel?.song?.songID ?: 0
                    , 0)
                    .apply {
                        setReply()
                        mFuncationTv.setOnClickListener(object : DebounceViewClickListener() {
                            override fun clickValid(v: View?) {
                                dismiss()
                                mFeedsInputContainerView?.showSoftInput()
                            }
                        })
                    }
            mMoreDialogPlus?.showByDialog()
        }
    }

    override fun addCommentSuccess(model: FirstLevelCommentModel) {
        mFeedsCommentView?.feedsCommendAdapter?.mCommentNum = mFeedsCommentView?.feedsCommendAdapter?.mCommentNum!!.plus(1)
        mFeedsWatchModel?.commentCnt = mFeedsCommentView?.feedsCommendAdapter?.mCommentNum!!
        mFeedsCommentView?.addSelfComment(model)
    }

    override fun likeFeed(like: Boolean) {
        mXinIv!!.isSelected = like
        if (like) {
            mXinNumTv!!.text = mXinNumTv!!.text.toString().toInt().plus(1).toString()
        } else {
            mXinNumTv!!.text = (mXinNumTv!!.text.toString().toInt() - 1).toString()
        }

    }

    override fun onResume() {
        super.onResume()
        mResumeCall?.invoke()
        mResumeCall = null
    }

    override fun onPause() {
        super.onPause()
        if (mControlTv!!.isSelected) {
            mMyMediaPlayer.pause()
            mResumeCall = {
                mMyMediaPlayer.resume()
            }
        }
    }

    override fun showRelation(isBlacked: Boolean, isFollow: Boolean, isFriend: Boolean) {
        if (isFriend) {
            isFriendState()
            mFollowTv?.setOnClickListener(null)
        } else if (isFollow) {
            isFollowState()
            mFollowTv?.setOnClickListener(null)
        } else {
            isStrangerState()
            mFollowTv?.setDebounceViewClickListener {
                UserInfoManager.getInstance().mateRelation(mFeedsWatchModel!!.user!!.userID!!, UserInfoManager.RA_BUILD, false, 0, null)
            }
        }
    }

    private fun playSong() {
        mMyMediaPlayer.reset()
        mControlTv?.isSelected = true
        mIsSongStart = true
        mRadioView?.play()
        mMyMediaPlayer.startPlay(mFeedsWatchModel?.song?.playURL)
    }

    private fun resumeSong() {
        mControlTv!!.isSelected = true
        mRadioView?.play()
        if (mIsSongStart) {
            mMyMediaPlayer.resume()
            mFeedsCommonLyricView?.resume()
        } else {
            playSong()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: PlayerEvent.TimeFly) {
        //歌曲还没加载到的时候这个会返回1毫秒，无意义，do not care
        if (event.totalDuration > 1000) {
            mPassTimeTv?.text = U.getDateTimeUtils().formatTimeStringForDate(event.curPostion, "mm:ss")
            mLastTimeTv?.text = U.getDateTimeUtils().formatTimeStringForDate(event.totalDuration - event.curPostion, "mm:ss")
            mSeekBar!!.max = event.totalDuration.toInt()
            mSeekBar!!.progress = event.curPostion.toInt()
            mFeedsCommonLyricView?.seekTo(event.curPostion.toInt())
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: RelationChangeEvent) {
        if (mFeedsWatchModel?.user?.userID == event.useId) {
            if (event.isFriend) {
                isFriendState()
                mFollowTv?.setOnClickListener(null)
            } else if (event.isFollow) {
                isFollowState()
                mFollowTv?.setOnClickListener(null)
            } else {
                isStrangerState()
            }
        }
    }

    fun isFriendState() {
        val followState = DrawableCreator.Builder()
                .setCornersRadius(U.getDisplayUtils().dip2px(20f).toFloat())
                .setSolidColor(U.getColor(R.color.white))
                .setStrokeColor(Color.parseColor("#AD6C00"))
                .setStrokeWidth(U.getDisplayUtils().dip2px(1f).toFloat())
                .build()

        mFollowTv?.text = "已互关"
        mFollowTv?.background = followState
        mFollowTv?.setTextColor(Color.parseColor("#AD6C00"))
    }

    fun isFollowState() {
        val followState = DrawableCreator.Builder()
                .setCornersRadius(U.getDisplayUtils().dip2px(20f).toFloat())
                .setSolidColor(U.getColor(R.color.white))
                .setStrokeColor(Color.parseColor("#AD6C00"))
                .setStrokeWidth(U.getDisplayUtils().dip2px(1f).toFloat())
                .build()

        mFollowTv?.text = "已关注"
        mFollowTv?.background = followState
        mFollowTv?.setTextColor(Color.parseColor("#AD6C00"))
    }

    fun isStrangerState() {
        val followState = DrawableCreator.Builder()
                .setCornersRadius(U.getDisplayUtils().dip2px(20f).toFloat())
                .setSolidColor(Color.parseColor("#FFC15B"))
                .build()

        mFollowTv?.text = "+关注"
        mFollowTv?.background = followState
        mFollowTv?.setTextColor(Color.parseColor("#AD6C00"))
    }

    private fun pauseSong() {
        mControlTv!!.isSelected = false
        mRadioView?.pause()
        mMyMediaPlayer.pause()
        mFeedsCommonLyricView?.pause()
    }

    private fun stopSong() {
        mIsSongStart = false
        mMyMediaPlayer.stop()
        mControlTv!!.isSelected = false
        mRadioView?.pause()
        mSeekBar!!.progress = 0
        mPassTimeTv?.text = "00:00"
        mFeedsWatchModel?.song?.playDurMs?.let {
            mLastTimeTv?.text = U.getDateTimeUtils().formatTimeStringForDate(it.toLong(), "mm:ss")
        }
        mFeedsCommonLyricView?.stop()
    }

    override fun setData(type: Int, data: Any?) {
        if (type == 0) {
            mFeedsWatchModel = data as FeedsWatchModel
        }
    }

    fun View.setDebounceViewClickListener(click: (view: View?) -> Unit) {
        this.setOnClickListener(object : DebounceViewClickListener() {
            override fun clickValid(v: View?) {
                click(v)
            }
        })
    }

    override fun isBlackStatusBarText(): Boolean = true

    override fun useEventBus(): Boolean {
        return true
    }

    override fun destroy() {
        super.destroy()
        mMyMediaPlayer.release()
        mFeedsCommonLyricView?.destroy()
    }
}