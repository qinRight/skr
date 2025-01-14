package com.module.playways.mic.room

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import android.view.*
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.common.base.BaseActivity
import com.common.base.FragmentDataListener
import com.common.core.myinfo.MyUserInfo
import com.common.core.myinfo.MyUserInfoManager
import com.common.core.permission.SkrAudioPermission
import com.common.core.userinfo.UserInfoManager
import com.common.core.userinfo.model.UserInfoModel
import com.common.core.view.setAnimateDebounceViewClickListener
import com.common.core.view.setDebounceViewClickListener
import com.common.log.DebugLogView
import com.common.log.MyLog
import com.common.utils.FragmentUtils
import com.common.utils.SpanUtils
import com.common.utils.U
import com.common.view.ex.ExTextView
import com.component.busilib.constans.GameModeType
import com.component.busilib.view.GameEffectBgView
import com.component.dialog.ConfirmDialog
import com.component.dialog.PersonInfoDialog
import com.component.person.event.ShowPersonCardEvent
import com.component.report.fragment.QuickFeedbackFragment
import com.component.toast.CommonToastView
import com.dialog.view.TipsDialogView
import com.module.RouterConstants
import com.module.home.IHomeService
import com.module.playways.IPlaywaysModeService
import com.module.playways.R
import com.module.playways.RoomDataUtils
import com.module.playways.grab.room.inter.IGrabVipView
import com.module.playways.grab.room.presenter.VipEnterPresenter
import com.module.playways.grab.room.view.GrabGiveupView
import com.module.playways.grab.room.view.GrabScoreTipsView
import com.module.playways.grab.room.view.VIPEnterView
import com.module.playways.grab.room.view.control.OthersSingCardView
import com.module.playways.grab.room.view.control.RoundOverCardView
import com.module.playways.grab.room.view.control.SelfSingCardView
import com.module.playways.grab.room.voicemsg.VoiceRecordTipsView
import com.module.playways.grab.room.voicemsg.VoiceRecordUiController
import com.module.playways.listener.AnimationListener
import com.module.playways.listener.SVGAListener
import com.module.playways.mic.home.MicHomeActivity
import com.module.playways.mic.match.model.JoinMicRoomRspModel
import com.module.playways.mic.room.bottom.MicBottomContainerView
import com.module.playways.mic.room.event.MicHomeOwnerChangeEvent
import com.module.playways.mic.room.event.MicWantInviteEvent
import com.module.playways.mic.room.model.MicPlayerInfoModel
import com.module.playways.mic.room.model.MicRoundInfoModel
import com.module.playways.mic.room.model.RoomInviteMusicModel
import com.module.playways.mic.room.presenter.MicCorePresenter
import com.module.playways.mic.room.seat.MicSeatView
import com.module.playways.mic.room.top.MicTopContentView
import com.module.playways.mic.room.top.MicTopOpView
import com.module.playways.mic.room.top.RoomInviteView
import com.module.playways.mic.room.ui.IMicRoomView
import com.module.playways.mic.room.ui.MicWidgetAnimationController
import com.module.playways.mic.room.view.MicInputContainerView
import com.module.playways.mic.room.view.MicSettingView
import com.module.playways.mic.room.view.MicTurnInfoCardView
import com.module.playways.mic.room.view.MicVoiceControlPanelView
import com.module.playways.mic.room.view.control.MicSingBeginTipsCardView
import com.module.playways.room.data.H
import com.module.playways.room.gift.event.BuyGiftEvent
import com.module.playways.room.gift.event.ShowHalfRechargeFragmentEvent
import com.module.playways.room.gift.model.NormalGift
import com.module.playways.room.gift.view.ContinueSendView
import com.module.playways.room.gift.view.GiftDisplayView
import com.module.playways.room.gift.view.GiftPanelView
import com.module.playways.room.room.comment.CommentView
import com.module.playways.room.room.comment.fly.FlyCommentView
import com.module.playways.room.room.comment.listener.CommentViewItemListener
import com.module.playways.room.room.comment.model.CommentModel
import com.module.playways.room.room.comment.model.CommentSysModel
import com.module.playways.room.room.event.PretendCommentMsgEvent
import com.module.playways.room.room.gift.GiftBigAnimationViewGroup
import com.module.playways.room.room.gift.GiftBigContinuousView
import com.module.playways.room.room.gift.GiftContinueViewGroup
import com.module.playways.room.room.gift.GiftOverlayAnimationViewGroup
import com.module.playways.room.room.view.BottomContainerView
import com.module.playways.songmanager.SongManagerActivity
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder
import com.zq.live.proto.Common.StandPlayType
import com.zq.live.proto.MicRoom.EMRoundOverReason
import com.zq.live.proto.MicRoom.EMRoundStatus
import com.zq.live.proto.MicRoom.MAddMusicMsg
import com.zq.live.proto.MicRoom.MReqAddMusicMsg
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

@Route(path = RouterConstants.ACTIVITY_MIC_ROOM)
class MicRoomActivity : BaseActivity(), IMicRoomView, IGrabVipView {

    override fun ensureActivtyTop() {
        // 销毁其他的除排麦房页面所有界面
        for (i in U.getActivityUtils().activityList.size - 1 downTo 0) {
            val activity = U.getActivityUtils().activityList[i]
            if (activity === this) {
                continue
            }
            if (activity is MicHomeActivity) {
                continue
            }
            if (U.getActivityUtils().isHomeActivity(activity)) {
                continue
            }
            activity.finish()
        }
    }

    /**
     * 存起该房间一些状态信息
     */
    internal var mRoomData = MicRoomData()

    private lateinit var mCorePresenter: MicCorePresenter
    //基础ui组件
    internal lateinit var mInputContainerView: MicInputContainerView
    internal lateinit var mBottomContainerView: MicBottomContainerView
    internal lateinit var mVoiceRecordTipsView: VoiceRecordTipsView
    internal lateinit var mCommentView: CommentView
    private var mFlyCommentView: FlyCommentView? = null

    internal lateinit var mGiftPanelView: GiftPanelView
    internal lateinit var mContinueSendView: ContinueSendView
    internal lateinit var mTopOpView: MicTopOpView
    internal lateinit var mTopContentView: MicTopContentView

    internal var mMicInviteView: RoomInviteView? = null

    // 专场ui组件
    lateinit var mTurnInfoCardView: MicTurnInfoCardView  // 下一局
    lateinit var mOthersSingCardView: OthersSingCardView// 他人演唱卡片
    lateinit var mSelfSingCardView: SelfSingCardView // 自己演唱卡片
    lateinit var mSingBeginTipsCardView: MicSingBeginTipsCardView// 演唱开始提示
    lateinit var mRoundOverCardView: RoundOverCardView// 结果页
    lateinit var mGrabScoreTipsView: GrabScoreTipsView // 打分提示
    lateinit var mGameEffectBgView: GameEffectBgView

    lateinit var mAddSongIv: ExTextView
    private lateinit var mGiveUpView: GrabGiveupView

    private var mVIPEnterView: VIPEnterView? = null
    lateinit var mHasSelectSongNumTv: ExTextView
    private lateinit var mMicSeatView: MicSeatView

    // 都是dialogplus
    private var mPersonInfoDialog: PersonInfoDialog? = null
    private var mGrabKickDialog: ConfirmDialog? = null
    private var mVoiceControlPanelView: MicVoiceControlPanelView? = null
    private var mMicSettingView: MicSettingView? = null
    private var mGameRuleDialog: DialogPlus? = null
    private var mTipsDialogView: TipsDialogView? = null

    internal var mVipEnterPresenter: VipEnterPresenter? = null

    lateinit var mVoiceRecordUiController: VoiceRecordUiController
    val mWidgetAnimationController = MicWidgetAnimationController(this)
    internal var mSkrAudioPermission = SkrAudioPermission()

    val mUiHanlder = object : Handler() {
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
        }
    }

    override fun initView(savedInstanceState: Bundle?): Int {
        return R.layout.mic_room_fragment_layout
    }

    override fun initData(savedInstanceState: Bundle?) {
        ensureActivtyTop()
        val joinRaceRoomRspModel = intent.getSerializableExtra("JoinMicRoomRspModel") as JoinMicRoomRspModel?
        joinRaceRoomRspModel?.let {
            mRoomData.loadFromRsp(it)
        }
        H.micRoomData = mRoomData
        H.setType(GameModeType.GAME_MODE_MIC, "MicRoomActivity")

        mCorePresenter = MicCorePresenter(mRoomData, this)
        addPresent(mCorePresenter)
        mVipEnterPresenter = VipEnterPresenter(this, mRoomData)
        addPresent(mVipEnterPresenter)
        // 请保证从下面的view往上面的view开始初始化
        findViewById<View>(R.id.main_act_container).setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                mInputContainerView.hideSoftInput()
            }
            false
        }
        initInputView()
        initBottomView()
        initCommentView()
        initGiftPanelView()
        initGiftDisplayView()
        initTopView()
        initTurnSenceView()
        initBgEffectView()

        initVipEnterView()
        initMicSeatView()

        mCorePresenter.onOpeningAnimationOver()

        mUiHanlder.postDelayed(Runnable {
            var openOpBarTimes = U.getPreferenceUtils().getSettingInt("key_open_op_bar_times", 0)
            if (openOpBarTimes < 2) {
                mWidgetAnimationController.open()
                openOpBarTimes++
                U.getPreferenceUtils().setSettingInt("key_open_op_bar_times", openOpBarTimes)
            } else {
                mWidgetAnimationController.close()
            }

        }, 500)
        if (MyLog.isDebugLogOpen()) {
            val viewStub = findViewById<ViewStub>(R.id.debug_log_view_stub)
            val debugLogView = DebugLogView(viewStub)
            debugLogView.tryInflate()
        }

        if (U.getPreferenceUtils().getSettingBoolean("is_first_enter_microom", true)) {
            U.getPreferenceUtils().setSettingBoolean("is_first_enter_microom", false)
            showGameRuleDialog()
        }

        MyUserInfoManager.myUserInfo?.let {
            if (it.ranking != null) {
                mVipEnterPresenter?.addNotice(MyUserInfo.toUserInfoModel(it))
            }
        }


        U.getStatusBarUtil().setTransparentBar(this, false)
    }

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        mSkrAudioPermission.onBackFromPermisionManagerMaybe(this)
        if (mPersonInfoDialog?.isShowing == true) {
            mPersonInfoDialog?.refreshHomepage()
        }
    }

    override fun destroy() {
        if (window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        super.destroy()
        mFlyCommentView?.destory()
        dismissDialog()
        mGiftPanelView?.destroy()
        mSelfSingCardView?.destroy()
        H.reset("MicRoomActivity")
    }

    override fun finish() {
        super.finish()
        MyLog.w(TAG, "finish")
    }

    override fun canSlide(): Boolean {
        return false
    }

    override fun resizeLayoutSelfWhenKeyboardShow(): Boolean {
        return true
    }


    private fun hideAllSceneView(exclude: Any?) {
        if (mSelfSingCardView != exclude) {
            mSelfSingCardView.setVisibility(View.GONE)
        }
        if (mOthersSingCardView != exclude) {
            mOthersSingCardView.setVisibility(View.GONE)
        }
        if (mTurnInfoCardView != exclude) {
            mTurnInfoCardView.visibility = View.GONE
        }
        if (mGiveUpView != exclude) {
            mGiveUpView.hideWithAnimation(false)
        }
        if (mRoundOverCardView != exclude) {
            mRoundOverCardView.setVisibility(View.GONE)
        }
        if (mSingBeginTipsCardView != exclude) {
            mSingBeginTipsCardView.setVisibility(View.GONE)
        }
        if (mGameEffectBgView != exclude) {
            mGameEffectBgView.hideBg()
        }
        hideFlyCommentView()
    }

    private fun initMicSeatView() {
        mHasSelectSongNumTv = findViewById(R.id.has_select_song_num_tv)
        mMicSeatView = MicSeatView(findViewById(R.id.mic_seat_view_layout_view_stub))
        mMicSeatView.hasSelectSongNumTv = mHasSelectSongNumTv
        mMicSeatView.mRoomData = mRoomData
        mHasSelectSongNumTv.setDebounceViewClickListener {
            mMicSeatView.show()
        }
    }

    private fun initVipEnterView() {
        mVIPEnterView = VIPEnterView(findViewById(R.id.vip_enter_view_stub))
    }

    private fun initInputView() {
        mInputContainerView = findViewById(R.id.input_container_view)
        mInputContainerView.setRoomData(mRoomData)
    }

    override fun invitedToOtherRoom() {
        mMicSettingView?.dismiss(false)
        mVoiceControlPanelView?.dismiss(false)
    }

    private fun initTurnSenceView() {
//        mWaitingCardView = findViewById(R.id.wait_card_view)
//        mWaitingCardView.visibility = View.GONE
        var rootView = findViewById<View>(R.id.main_act_container)
        // 下一首
        mTurnInfoCardView = findViewById(R.id.turn_card_view)
        mTurnInfoCardView.visibility = View.GONE
        // 演唱开始名片
        mSingBeginTipsCardView = MicSingBeginTipsCardView(rootView)
        // 自己演唱
        mSelfSingCardView = SelfSingCardView(rootView)
        mSelfSingCardView?.setListener {
            //            removeNoAccSrollTipsView()
//            removeGrabSelfSingTipView()
            mCorePresenter?.sendRoundOverInfo()
        }
        // 他人演唱
//        mSelfSingCardView?.setListener4FreeMic { mCorePresenter?.sendMyGrabOver("onSelfSingOver") }
        mOthersSingCardView = OthersSingCardView(rootView)
        // 结果页面
        mRoundOverCardView = RoundOverCardView(rootView)

        // 打分
        mGrabScoreTipsView = rootView.findViewById(R.id.grab_score_tips_view)
    }

    private fun initBottomView() {
        mGiveUpView = findViewById<GrabGiveupView>(R.id.give_up_view)
        mGiveUpView.mGiveUpListener = { _ ->
            mCorePresenter.giveUpSing {
                mGiveUpView.hideWithAnimation(true)
                if (mRoomData.realRoundInfo?.isChorusRound == true) {
                    mSelfSingCardView.setVisibility(View.GONE)
                    hideFlyCommentView()
                }
            }
        }
        mAddSongIv = findViewById(R.id.select_song_tv)
        mAddSongIv.setAnimateDebounceViewClickListener {
            mSkrAudioPermission.ensurePermission({
                SongManagerActivity.open(this, mRoomData)
            }, true)
        }

        run {
            val voiceStub = findViewById<ViewStub>(R.id.voice_record_tip_view_stub)
            mVoiceRecordTipsView = VoiceRecordTipsView(voiceStub)
        }
//        mBottomBgVp = findViewById<ViewGroup>(R.id.bottom_bg_vp)
//        val lp = mBottomBgVp.getLayoutParams() as RelativeLayout.LayoutParams
//        /**
//         * 按比例适配手机
//         */
//        lp.height = U.getDisplayUtils().screenHeight * 284 / 667

        mBottomContainerView = findViewById(R.id.bottom_container_view)
        mBottomContainerView.setRoomData(mRoomData)
        mBottomContainerView.setListener(object : BottomContainerView.Listener() {
            override fun showInputBtnClick() {
                dismissDialog()
                mInputContainerView.showSoftInput()
            }

            override fun clickRoomManagerBtn() {
                //                U.getFragmentUtils().addFragment(FragmentUtils.newAddParamsBuilder(GrabRoomFragment.this.getActivity(), OwnerManageFragment.class)
                //                        .setAddToBackStack(true)
                //                        .setHasAnimation(true)
                //                        .setEnterAnim(R.anim.slide_right_in)
                //                        .setExitAnim(R.anim.slide_right_out)
                //                        .addDataBeforeAdd(0, mRoomData)
                //                        .build());
//                SongManagerActivity.open(activity, mRoomData)
//                removeManageSongTipView()
            }

            override fun showGiftPanel() {
                mContinueSendView.setVisibility(View.GONE)
                showPanelView()
            }

            override fun onClickFlower() {
                buyFlowerFromOuter()
            }
        })
    }

    private fun initBgEffectView() {
        mGameEffectBgView = GameEffectBgView(findViewById(R.id.game_effect_bg_view_layout_viewStub))
    }

    private fun showPanelView() {
        if (mRoomData!!.realRoundInfo != null) {
            val now = mRoomData!!.realRoundInfo
            if (now != null) {
                if (now.isPKRound && now.status == EMRoundStatus.MRS_SPK_SECOND_PEER_SING.value) {
                    if (now.getsPkRoundInfoModels().size == 2) {
                        val userId = now.getsPkRoundInfoModels()[1].userID
                        mGiftPanelView?.show(RoomDataUtils.getPlayerInfoById(mRoomData!!, userId)?.userInfo)
                    } else {
                        mGiftPanelView?.show(RoomDataUtils.getPlayerInfoById(mRoomData!!, now.userID)?.userInfo)
                    }
                } else {
                    mGiftPanelView?.show(RoomDataUtils.getPlayerInfoById(mRoomData!!, now.userID)?.userInfo)
                }
            } else {
                mGiftPanelView?.show(null)
            }
        } else {
            mGiftPanelView?.show(null)
        }
    }

    private fun showFlyCommentView() {
        if (mFlyCommentView?.visibility != View.VISIBLE) {
            mFlyCommentView?.visibility = View.VISIBLE
        }
        if (mCommentView.visibility != View.INVISIBLE) {
            mCommentView.visibility = View.INVISIBLE
        }
    }

    private fun hideFlyCommentView() {
        if (mFlyCommentView?.visibility != View.GONE) {
            mFlyCommentView?.visibility = View.GONE
        }
        if (mCommentView.visibility != View.VISIBLE) {
            mCommentView.visibility = View.VISIBLE
        }
    }

    private fun buyFlowerFromOuter() {
        if (mRoomData!!.realRoundInfo != null) {
            val now = mRoomData!!.realRoundInfo
            if (now != null) {
                if (now.isPKRound && now.status == EMRoundStatus.MRS_SPK_SECOND_PEER_SING.value) {
                    if (now.getsPkRoundInfoModels().size == 2) {
                        val userId = now.getsPkRoundInfoModels()[1].userID
                        RoomDataUtils.getPlayerInfoById(mRoomData!!, userId)?.let {
                            EventBus.getDefault().post(BuyGiftEvent(NormalGift.getFlower(), it.userInfo))
                        }
                    } else {
                        RoomDataUtils.getPlayerInfoById(mRoomData!!, now.userID)?.let {
                            EventBus.getDefault().post(BuyGiftEvent(NormalGift.getFlower(), it.userInfo))
                        }
                    }
                } else if (now.isChorusRound) {
                    if (now.getChorusRoundInfoModels().size == 2) {
                        if (!now.getChorusRoundInfoModels()[0].isHasGiveUp) {
                            RoomDataUtils.getPlayerInfoById(mRoomData!!, now.getChorusRoundInfoModels()[0].userID)?.let {
                                EventBus.getDefault().post(BuyGiftEvent(NormalGift.getFlower(), it.userInfo))
                            }
                        } else {
                            RoomDataUtils.getPlayerInfoById(mRoomData!!, now.getChorusRoundInfoModels()[1].userID)?.let {
                                EventBus.getDefault().post(BuyGiftEvent(NormalGift.getFlower(), it.userInfo))
                            }
                        }
                    }
                } else {
                    val micPlayerInfoMode = RoomDataUtils.getPlayerInfoById(mRoomData!!, now.userID)
                    if (micPlayerInfoMode != null) {
                        EventBus.getDefault().post(BuyGiftEvent(NormalGift.getFlower(), micPlayerInfoMode.userInfo))
                    } else {
                        U.getToastUtil().showShort("只能给正在演唱的其他选手送礼哦～")
                    }
                }
            } else {
                U.getToastUtil().showShort("只能给正在演唱的其他选手送礼哦～")
            }
        } else {
            U.getToastUtil().showShort("只能给正在演唱的其他选手送礼哦～")
        }
    }

    private fun initTopView() {
        mTopOpView = findViewById(R.id.top_op_view)
        mTopOpView.setRoomData(mRoomData)
        mTopOpView.setListener(object : MicTopOpView.Listener {
            override fun onClickVoiceAudition() {
                // 调音面板
                U.getKeyBoardUtils().hideSoftInputKeyBoard(this@MicRoomActivity)
                dismissDialog()
                if (mVoiceControlPanelView == null) {
                    mVoiceControlPanelView = MicVoiceControlPanelView(this@MicRoomActivity)
                    mVoiceControlPanelView?.setRoomData(mRoomData)
                }
                mVoiceControlPanelView?.showByDialog()
            }

            override fun onClickFeedBack() {
                U.getFragmentUtils().addFragment(
                        FragmentUtils.newAddParamsBuilder(this@MicRoomActivity, QuickFeedbackFragment::class.java)
                                .setAddToBackStack(true)
                                .setHasAnimation(true)
                                .addDataBeforeAdd(0, QuickFeedbackFragment.FROM_MIC_ROOM)
                                .addDataBeforeAdd(1, QuickFeedbackFragment.FEED_BACK)
                                .addDataBeforeAdd(3, mRoomData.gameId)
                                .setEnterAnim(R.anim.slide_in_bottom)
                                .setExitAnim(R.anim.slide_out_bottom)
                                .build())
            }

            override fun closeBtnClick() {
                quitGame()
            }

            override fun onClickGameRule() {
                U.getKeyBoardUtils().hideSoftInputKeyBoard(this@MicRoomActivity)
                showGameRuleDialog()
            }

            override fun onClickSetting() {
                //设置界面
                if (mRoomData.isOwner) {
                    U.getKeyBoardUtils().hideSoftInputKeyBoard(this@MicRoomActivity)
                    dismissDialog()
                    if (mMicSettingView == null) {
                        mMicSettingView = MicSettingView(this@MicRoomActivity)
                        mMicSettingView?.mRoomData = mRoomData

                        mMicSettingView?.callUpdate = {
                            mCorePresenter?.changeMatchState(it)
                        }
                    }
                    mMicSettingView?.showByDialog()
                } else {
                    U.getToastUtil().showShort("只有房主能设置哦～")
                }
            }
        })


        mTopContentView = findViewById(R.id.top_content_view)
        mTopContentView.setRoomData(mRoomData)

        mTopContentView.setListener(object : MicTopContentView.Listener {
            override fun clickArrow(open: Boolean) {
                if (open) {
                    mWidgetAnimationController.open()
                } else {
                    mWidgetAnimationController.close()
                }
            }
        })

        mMicInviteView = RoomInviteView(findViewById(R.id.mic_invite_view_stub))
        mMicInviteView?.agreeInviteListener = {
            mSkrAudioPermission.ensurePermission({
                mMicInviteView?.agreeInvite()
            }, true)
        }
    }

    private fun showGameRuleDialog() {
        dismissDialog()
        mGameRuleDialog = DialogPlus.newDialog(this)
                .setContentHolder(ViewHolder(R.layout.mic_game_rule_view_layout))
                .setContentBackgroundResource(R.color.transparent)
                .setOverlayBackgroundResource(R.color.black_trans_50)
                .setMargin(U.getDisplayUtils().dip2px(16f), -1, U.getDisplayUtils().dip2px(16f), -1)
                .setExpanded(false)
                .setGravity(Gravity.CENTER)
                .create()
        mGameRuleDialog?.show()
    }

    private fun initCommentView() {
        mCommentView = findViewById(R.id.comment_view)
        mCommentView.setListener(CommentViewItemListener { userId ->
            showPersonInfoView(userId, null)
        })
        mCommentView.roomData = mRoomData
        mVoiceRecordUiController = VoiceRecordUiController(mBottomContainerView.mVoiceRecordBtn!!, mVoiceRecordTipsView, mCommentView)
        mFlyCommentView = findViewById(R.id.fly_comment_view)
        mFlyCommentView?.roomData = mRoomData
    }


    private fun initGiftPanelView() {
        mGiftPanelView = findViewById<View>(R.id.gift_panel_view) as GiftPanelView
        mGiftPanelView.setRoomData(mRoomData)
        mContinueSendView = findViewById<View>(R.id.continue_send_view) as ContinueSendView
        mContinueSendView.mScene = ContinueSendView.EGameScene.GS_Mic
        mContinueSendView.setRoomData(mRoomData)
        mContinueSendView.setObserver(object : ContinueSendView.OnVisibleStateListener {
            override fun onVisible(isVisible: Boolean) {
                mBottomContainerView.setOpVisible(!isVisible)
            }
        })
        mGiftPanelView.setIGetGiftCountDownListener(object : GiftDisplayView.IGetGiftCountDownListener {
            override fun getCountDownTs(): Long {
                //                return mGiftTimerPresenter.getCountDownSecond();
                return 0
            }
        })
    }

    private fun initGiftDisplayView() {
        val giftContinueViewGroup = findViewById<GiftContinueViewGroup>(R.id.gift_continue_vg)
        giftContinueViewGroup.setRoomData(mRoomData)
        val giftOverlayAnimationViewGroup = findViewById<GiftOverlayAnimationViewGroup>(R.id.gift_overlay_animation_vg)
        giftOverlayAnimationViewGroup.setRoomData(mRoomData)
        val giftBigAnimationViewGroup = findViewById<GiftBigAnimationViewGroup>(R.id.gift_big_animation_vg)
        giftBigAnimationViewGroup.setRoomData(mRoomData)
        val giftBigContinueView = findViewById<GiftBigContinuousView>(R.id.gift_big_continue_view)
        giftBigAnimationViewGroup.setGiftBigContinuousView(giftBigContinueView)
        //mDengBigAnimation = findViewById<View>(R.id.deng_big_animation) as GrabDengBigAnimationView
    }

    override fun receiveScoreEvent(score: Int) {
        mGrabScoreTipsView.updateScore(score, -1)
    }

    override fun showSongCount(count: Int) {
        mHasSelectSongNumTv?.text = "已点${count}首"
    }

    override fun startEnterAnimation(playerInfoModel: UserInfoModel, finishCall: () -> Unit) {
        mVIPEnterView?.enter(playerInfoModel, finishCall)
    }

    private fun showPersonInfoView(userID: Int, showKick: Boolean?) {
        if (!U.getNetworkUtils().hasNetwork()) {
            U.getToastUtil().showShort("网络异常，请检查网络后重试!")
            return
        }
        dismissDialog()
        mInputContainerView.hideSoftInput()
        val mShowKick = showKick != false
        mPersonInfoDialog = PersonInfoDialog.Builder(this, QuickFeedbackFragment.FROM_MIC_ROOM, userID, mShowKick, true)
                .setRoomID(mRoomData.gameId)
                .setInviteReplyListener { userInfoModel ->
                    val iRankingModeService = ARouter.getInstance().build(RouterConstants.SERVICE_RANKINGMODE).navigation() as IPlaywaysModeService
                    iRankingModeService.tryInviteToRelay(userInfoModel.userId, userInfoModel.isFriend, false)
                }
                .setKickListener { userInfoModel -> showKickConfirmDialog(userInfoModel) }
                .build()
        mPersonInfoDialog?.show()
    }

    // 确认踢人弹窗
    private fun showKickConfirmDialog(userInfoModel: UserInfoModel) {
        MyLog.d(TAG, "showKickConfirmDialog userInfoModel=$userInfoModel")
        dismissDialog()
        U.getKeyBoardUtils().hideSoftInputKeyBoard(this)
        if (!mRoomData.isOwner) {
            U.getToastUtil().showShort("只有房主才可以踢人")
            return
        }
        mGrabKickDialog = ConfirmDialog(U.getActivityUtils().topActivity, userInfoModel, ConfirmDialog.TYPE_OWNER_KICK_CONFIRM, 0)
        mGrabKickDialog?.setListener { userInfoModel ->
            // 发起踢人请求
            mCorePresenter?.reqKickUser(userInfoModel.userId)
        }
        mGrabKickDialog?.show()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: MReqAddMusicMsg) {
        // 请求合唱或者pk
        val micUserMusicModel = RoomInviteMusicModel.parseFromInfoPB(event.detail)
        if (micUserMusicModel.userID != MyUserInfoManager.uid.toInt()) {
            mMicInviteView?.showInvite(micUserMusicModel, mTopContentView.getViewLeft(micUserMusicModel.userID), true, GameModeType.GAME_MODE_MIC)
        } else {
            // 启一个任务去同步
            mMicInviteView?.startCheckSelfJob(micUserMusicModel)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: MicHomeOwnerChangeEvent) {
        RoomDataUtils.getPlayerInfoById(mRoomData, event.ownerId)?.let {
            val stringBuilder = SpanUtils()
                    .append("${UserInfoManager.getInstance().getRemarkName(event.ownerId, it.userInfo.nickname)}").setForegroundColor(Color.parseColor("#FFC15B"))
                    .append(" 已成为新的房主，房主可通过设置功能，更新房间属性").setForegroundColor(CommentModel.RANK_SYSTEM_COLOR)
                    .create()

            val commentSysModel = CommentSysModel(GameModeType.GAME_MODE_RACE, stringBuilder)
            EventBus.getDefault().post(PretendCommentMsgEvent(commentSysModel))
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: MAddMusicMsg) {
        // 合唱 pk 成功 点歌成功，要来判断是否当前合唱
        val userMusicModel = RoomInviteMusicModel.parseFromInfoPB(event.detail)
        if (userMusicModel.music?.playType == StandPlayType.PT_SPK_TYPE.value || userMusicModel.music?.playType == StandPlayType.PT_CHO_TYPE.value) {
            // 合唱或者pk
            mMicInviteView?.showInvite(userMusicModel, mTopContentView.getViewLeft(userMusicModel.userID), false, GameModeType.GAME_MODE_MIC)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: MicWantInviteEvent) {
//        ARouter.getInstance().build(RouterConstants.ACTIVITY_INVITE_FRIEND)
//                .withInt("from", GameModeType.GAME_MODE_MIC)
//                .withInt("roomId", mRoomData!!.gameId)
//                .navigation()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: ShowPersonCardEvent) {
        showPersonInfoView(event.uid, event.showKick)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: BuyGiftEvent) {
        if (event.receiver.userId != MyUserInfoManager.uid.toInt()) {
            mContinueSendView.startBuy(event.baseGift, event.receiver)
        } else {
            U.getToastUtil().showShort("只能给正在演唱的其他选手送礼哦～")
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: ShowHalfRechargeFragmentEvent) {
        val channelService = ARouter.getInstance().build(RouterConstants.SERVICE_HOME).navigation() as IHomeService
        val baseFragmentClass = channelService.getData(2, null) as Class<android.support.v4.app.Fragment>
        U.getFragmentUtils().addFragment(
                FragmentUtils.newAddParamsBuilder(this, baseFragmentClass)
                        .setEnterAnim(R.anim.slide_in_bottom)
                        .setExitAnim(R.anim.slide_out_bottom)
                        .setAddToBackStack(true)
                        .setFragmentDataListener(object : FragmentDataListener {
                            override fun onFragmentResult(requestCode: Int, resultCode: Int, bundle: Bundle?, obj: Any?) {
                                //充值成功
                                if (requestCode == 100 && resultCode == 0) {
                                    mGiftPanelView.updateZS()
                                    showPanelView()
                                }
                            }
                        })
                        .setHasAnimation(true)
                        .build())
    }

    override fun useEventBus(): Boolean {
        return true
    }

    override fun onBackPressedForActivity(): Boolean {
        if (mInputContainerView.onBackPressed()) {
            return true
        }

        if (mGiftPanelView.onBackPressed()) {
            return true
        }

        if (mMicSeatView.onBackPressed()) {
            return true
        }

        dismissDialog()
        mTipsDialogView = TipsDialogView.Builder(this)
                .setMessageTip("确定要退出小K房吗")
                .setConfirmTip("确定")
                .setCancelTip("取消")
                .setConfirmBtnClickListener {
                    mTipsDialogView?.dismiss(false)
                    quitGame()
                }
                .setCancelBtnClickListener {
                    mTipsDialogView?.dismiss()
                }
                .build()
        mTipsDialogView?.showByDialog()
        return true
    }


    fun quitGame() {
//        mCorePresenter.exitRoom("quitGame")
        finish()
    }

    private fun dismissDialog() {
//        mRaceActorPanelView?.dismiss(false)
        mPersonInfoDialog?.dismiss(false)
//        mRaceVoiceControlPanelView?.dismiss(false)
        mGameRuleDialog?.dismiss(false)
        mTipsDialogView?.dismiss(false)
        mGrabKickDialog?.dismiss(false)
    }

    override fun showWaiting() {
        hideAllSceneView(null)
    }

    override fun singBySelf(lastRoundInfo: MicRoundInfoModel?, singCardShowListener: () -> Unit) {
        hideAllSceneView(null)
        var step2 = {
            hideAllSceneView(null)
            mGrabScoreTipsView.reset()
            singCardShowListener.invoke()
            mSelfSingCardView.playLyric()
            mGiveUpView.delayShowGiveUpView(false)
            mOthersSingCardView.bindData()
            showFlyCommentView()
            playBgEffect()
        }

        var step1 = {
            //不是pk 第二轮 都显示 卡片
            if (mRoomData?.realRoundInfo?.status != EMRoundStatus.MRS_SPK_SECOND_PEER_SING.value) {
                mSingBeginTipsCardView.bindData(SVGAListener {
                    step2.invoke()
                })
            } else {
                step2.invoke()
            }
        }

        if (lastRoundInfo != null && lastRoundInfo.overReason != EMRoundOverReason.MROR_INTRO_OVER.value) {
            // 有上一局 肯定要显示下一首
            mTurnInfoCardView.showAnimation(object : AnimationListener {
                override fun onFinish() {
                    step1.invoke()
                }
            })
        } else {
            step1.invoke()
        }
    }

    private fun playBgEffect() {
        val now = mRoomData!!.realRoundInfo
        now?.let {
            if (now.isNormalRound) {
                if (now?.showInfos != null && now?.showInfos.size >= 1) {
                    mGameEffectBgView.showBgEffect(now?.showInfos[0].effectModel)
                }
            } else if (now.isChorusRound) {
                if (RoomDataUtils.isRoundSinger(it, MyUserInfoManager.uid)) {
                    //自己有演唱
                    if (now.chorusRoundInfoModels[0].userID.toLong() == MyUserInfoManager.uid) {
                        if (now?.showInfos != null && now?.showInfos.size >= 1) {
                            mGameEffectBgView.showBgEffect(now?.showInfos[0].effectModel)
                        }
                    } else {
                        if (now?.showInfos != null && now?.showInfos.size >= 2) {
                            mGameEffectBgView.showBgEffect(now?.showInfos[1].effectModel)
                        }
                    }
                } else {
                    //自己没有唱
                    for (effect in now?.showInfos) {
                        if (!TextUtils.isEmpty(effect.effectModel?.sourceURL)) {
                            mGameEffectBgView.showBgEffect(effect.effectModel)
                            break
                        }
                    }
                }
            } else if (now.isPKRound) {
                if (now.status == EMRoundStatus.MRS_SPK_FIRST_PEER_SING.value) {
                    if (now?.showInfos != null && now?.showInfos.size >= 1) {
                        mGameEffectBgView.showBgEffect(now?.showInfos[0].effectModel)
                    }
                } else if (now.status == EMRoundStatus.MRS_SPK_SECOND_PEER_SING.value) {
                    if (now?.showInfos != null && now?.showInfos.size >= 2) {
                        mGameEffectBgView.showBgEffect(now?.showInfos[1].effectModel)
                    }
                }
            } else {
                mGameEffectBgView.hideBg()
            }
        }
    }

    override fun singByOthers(lastRoundInfo: MicRoundInfoModel?) {
        hideAllSceneView(null)
        var step2 = {
            hideAllSceneView(null)
            mGrabScoreTipsView.reset()
            mOthersSingCardView.bindData()

            playBgEffect()
        }

        var step1 = {
            //不是pk 第二轮 都显示 卡片
            var b1 = mRoomData?.realRoundInfo?.status == EMRoundStatus.MRS_SPK_SECOND_PEER_SING.value
            var b2 = (mRoomData?.realRoundInfo?.isParticipant == false) && (mRoomData?.realRoundInfo?.isEnterInSingStatus == true)
            if (b2 || b1) {
                step2.invoke()
            } else {
                mSingBeginTipsCardView.bindData(SVGAListener {
                    step2.invoke()
                })
            }
        }

        if (lastRoundInfo != null && lastRoundInfo.overReason != EMRoundOverReason.MROR_INTRO_OVER.value) {
            // 有上一局 肯定要显示下一首
            mTurnInfoCardView.showAnimation(object : AnimationListener {
                override fun onFinish() {
                    step1.invoke()
                }
            })
        } else {
            step1.invoke()
        }
    }

    override fun joinNotice(model: MicPlayerInfoModel?) {
        model?.let {
            if (it.userID != MyUserInfoManager.myUserInfo?.userId?.toInt()) {
                mVipEnterPresenter?.addNotice(it.userInfo)
            }
        }
    }

    override fun showRoundOver(lastRoundInfo: MicRoundInfoModel?, continueOp: (() -> Unit)?) {
        hideAllSceneView(null)
        if (lastRoundInfo == null || lastRoundInfo?.overReason == EMRoundOverReason.MROR_INTRO_OVER.value) {
            // 等待阶段直接跳转 不走结果页
            continueOp?.invoke()
        } else {
            mRoundOverCardView.bindData(lastRoundInfo, SVGAListener {
                continueOp?.invoke()
            })
        }
    }

    override fun kickBySomeOne(isOwner: Boolean) {
        MyLog.d(TAG, "kickBySomeOne isOwner=$isOwner")
        //onGrabGameOver("kickBySomeOne");
        U.getToastUtil().showSkrCustomLong(CommonToastView.Builder(U.app())
                .setImage(R.drawable.touxiangshezhishibai_icon)
                .setText(if (isOwner) "房主将你请出了房间" else "超过半数玩家请你出房间，要友好文明游戏哦~")
                .build())
        mCorePresenter?.exitRoom("kickBySomeOne")
        finish()
    }

    override fun dismissKickDialog() {
    }

    override fun gameOver() {
        mCorePresenter.exitRoom("gameOver")
        finish()
    }


}
