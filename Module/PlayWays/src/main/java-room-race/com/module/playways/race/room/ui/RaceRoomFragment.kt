package com.module.playways.race.room.ui

import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewStub
import com.alibaba.android.arouter.launcher.ARouter
import com.common.base.BaseFragment
import com.common.base.FragmentDataListener
import com.common.core.myinfo.MyUserInfo
import com.common.core.myinfo.MyUserInfoManager
import com.common.core.userinfo.model.UserInfoModel
import com.common.log.DebugLogView
import com.common.log.MyLog
import com.common.statistics.StatisticsAdapter
import com.common.utils.FragmentUtils
import com.common.utils.U
import com.component.busilib.view.GameEffectBgView
import com.component.dialog.PersonInfoDialog
import com.component.person.event.ShowPersonCardEvent
import com.component.person.event.ShowReportEvent
import com.component.report.fragment.QuickFeedbackFragment
import com.dialog.view.TipsDialogView
import com.module.RouterConstants
import com.module.home.IHomeService
import com.module.playways.R
import com.module.playways.RoomDataUtils
import com.module.playways.grab.room.inter.IGrabVipView
import com.module.playways.grab.room.presenter.VipEnterPresenter
import com.module.playways.grab.room.view.GrabChangeRoomTransitionView
import com.module.playways.grab.room.view.VIPEnterView
import com.module.playways.grab.room.voicemsg.VoiceRecordTipsView
import com.module.playways.grab.room.voicemsg.VoiceRecordUiController
import com.module.playways.listener.AnimationListener
import com.module.playways.race.room.RaceRoomData
import com.module.playways.race.room.bottom.RaceBottomContainerView
import com.module.playways.race.room.event.RaceScoreChangeEvent
import com.module.playways.race.room.event.RaceWantSingChanceEvent
import com.module.playways.race.room.model.RaceRoundInfoModel
import com.module.playways.race.room.presenter.RaceCorePresenter
import com.module.playways.race.room.view.*
import com.module.playways.race.room.view.actor.RaceActorPanelView
import com.module.playways.race.room.view.matchview.RaceMatchView
import com.module.playways.race.room.view.topContent.RaceTopContentView
import com.module.playways.room.gift.event.BuyGiftEvent
import com.module.playways.room.gift.event.ShowHalfRechargeFragmentEvent
import com.module.playways.room.gift.model.NormalGift
import com.module.playways.room.gift.view.ContinueSendView
import com.module.playways.room.gift.view.GiftDisplayView
import com.module.playways.room.gift.view.GiftPanelView
import com.module.playways.room.room.comment.CommentView
import com.module.playways.room.room.comment.fly.FlyCommentView
import com.module.playways.room.room.comment.listener.CommentViewItemListener
import com.module.playways.room.room.gift.GiftBigAnimationViewGroup
import com.module.playways.room.room.gift.GiftContinueViewGroup
import com.module.playways.room.room.gift.GiftOverlayAnimationViewGroup
import com.module.playways.room.room.view.BottomContainerView
import com.module.playways.room.song.model.SongModel
import com.module.playways.songmanager.SongManagerActivity
import com.module.playways.songmanager.event.AddSongEvent
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder
import com.zq.live.proto.RaceRoom.ERUserRole
import com.zq.live.proto.RaceRoom.ERaceRoundOverReason
import com.zq.live.proto.RaceRoom.ERaceRoundStatus
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class RaceRoomFragment : BaseFragment(), IRaceRoomView, IGrabVipView {

    internal lateinit var mCorePresenter: RaceCorePresenter

    private lateinit var mInputContainerView: RaceInputContainerView
    private lateinit var mBottomContainerView: RaceBottomContainerView
    private lateinit var mVoiceRecordTipsView: VoiceRecordTipsView
    private lateinit var mCommentView: CommentView
    private var mFlyCommentView: FlyCommentView? = null

    internal lateinit var mGiftPanelView: GiftPanelView
    internal lateinit var mContinueSendView: ContinueSendView
    internal lateinit var mRaceTopOpView: RaceTopOpView
    internal lateinit var mRaceTopContentView: RaceTopContentView
    internal lateinit var mRaceTopVsView: RaceTopVsView

    internal lateinit var mRaceRightOpView: RaceRightOpView

    //    private lateinit var mRaceSelectSongView: RaceSelectSongView   // 选歌
    private lateinit var mRaceWaitingCardView: RaceWaitingCardView   // 等待中
    private lateinit var mRaceWantingSignUpCardView: RaceWantingSignUpCardView   // 正在报名卡片
    private lateinit var mRaceTurnInfoCardView: RaceTurnInfoCardView  // 下一局
    private lateinit var mRaceMatchView: RaceMatchView
    private lateinit var mRaceSelfSingLyricView: RaceSelfSingLyricView  // 自己唱
    private lateinit var mRaceOtherSingCardView: RaceOtherSingCardView   // 别人唱
    private lateinit var mRaceNoSingCardView: RaceNoSingerCardView    // 无人响应
    private lateinit var mRaceMiddleResultView: RaceMiddleResultView   // 比赛结果
    private var mRacePagerSelectSongView: RacePagerSelectSongView? = null
    private var mSignUpView: RaceSignUpBtnView? = null
    lateinit var mGameEffectBgView: GameEffectBgView

    internal var mVIPEnterView: VIPEnterView? = null

    // 都是dialogplus
    private var mRaceActorPanelView: RaceActorPanelView? = null  //参与的人
    private var mPersonInfoDialog: PersonInfoDialog? = null
    private var mRaceVoiceControlPanelView: RaceVoiceControlPanelView? = null
    private var mGameRuleDialog: DialogPlus? = null
    private var mTipsDialogView: TipsDialogView? = null

    internal var mVipEnterPresenter: VipEnterPresenter? = null

    lateinit var mVoiceRecordUiController: VoiceRecordUiController
    val mRaceWidgetAnimationController = RaceWidgetAnimationController(this)

    lateinit var mGrabChangeRoomTransitionView: GrabChangeRoomTransitionView
    internal var mBeginChangeRoomTs: Long = 0

    val mUiHanlder = object : Handler() {
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
        }
    }

    private fun hideAllSceneView() {
        mRaceSelfSingLyricView?.setVisibility(View.GONE)
        mRaceOtherSingCardView.setVisibility(View.GONE)
        mRaceNoSingCardView?.visibility = View.GONE
        mRaceMiddleResultView?.visibility = View.GONE
//        mRaceSelectSongView?.visibility = View.GONE
        mRaceWaitingCardView?.visibility = View.GONE
        mRaceTurnInfoCardView?.visibility = View.GONE
        mRaceMatchView?.visibility = View.GONE
        mRaceWantingSignUpCardView?.visibility = View.GONE
        hideFlyCommentView()
        mGameEffectBgView.hideBg()
    }

    var mRoomData: RaceRoomData = RaceRoomData()
    override fun initView(): Int {
        return R.layout.race_room_fragment_layout
    }

    override fun onFragmentVisible() {
        super.onFragmentVisible()
        if (mPersonInfoDialog?.isShowing == true) {
            mPersonInfoDialog?.refreshHomepage()
        }
    }

    override fun initData(savedInstanceState: Bundle?) {
        mCorePresenter = RaceCorePresenter(mRoomData, this)
        addPresent(mCorePresenter)
        mVipEnterPresenter = VipEnterPresenter(this, mRoomData)
        addPresent(mVipEnterPresenter)
        // 请保证从下面的view往上面的view开始初始化
        rootView.setOnTouchListener { v, event ->
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
//        initSelectSongView()
        initResultView()
        initBgEffectView()

        initSingSenceView()
        initRightView()
        initVipEnterView()
        initWantingSignUpCardView()
        initSelectPagerView()
        initRaceMatchView()
        initSignUpView()
        initChangeRoomTransitionView()
        mCorePresenter.onOpeningAnimationOver()

        mUiHanlder.postDelayed(Runnable {
            mRaceWidgetAnimationController.close()
        }, 500)
        if (MyLog.isDebugLogOpen()) {
            val viewStub = rootView.findViewById<ViewStub>(R.id.debug_log_view_stub)
            val debugLogView = DebugLogView(viewStub)
            debugLogView.tryInflate()
        }

        if (U.getPreferenceUtils().getSettingBoolean("is_first_enter_raceroom", true)) {
            U.getPreferenceUtils().setSettingBoolean("is_first_enter_raceroom", false)
            showGameRuleDialog()
        }

        MyUserInfoManager.myUserInfo?.let {
            if (it.ranking != null) {
                mVipEnterPresenter?.addNotice(MyUserInfo.toUserInfoModel(it))
            }
        }
    }

    private fun initSignUpView() {
        mSignUpView = rootView.findViewById(R.id.sign_up_view)
        if (!mRoomData.audience) {
            mSignUpView?.setType(RaceSignUpBtnView.SignUpType.SIGN_UP_START)
            mSignUpView?.roomData = mRoomData
            mSignUpView?.clickSignUpBtn = {
                mRacePagerSelectSongView?.showView()
            }
        }
    }

    private fun initChangeRoomTransitionView() {
        mGrabChangeRoomTransitionView = GrabChangeRoomTransitionView(rootView.findViewById(R.id.change_room_transition_view))
        mGrabChangeRoomTransitionView.setVisibility(View.GONE)
    }

    private fun initWantingSignUpCardView() {
        mRaceWantingSignUpCardView = rootView.findViewById(R.id.race_wanting_signup_view)
    }


    private fun initSelectPagerView() {
        mRacePagerSelectSongView = rootView.findViewById(R.id.select_pager_view)
        if (!mRoomData.audience) {
            mRacePagerSelectSongView?.mRoomData = mRoomData

            mRacePagerSelectSongView?.mSignUpMethod = { itemID, model ->
                mCorePresenter.wantSingChance(itemID, model?.commonMusic)
            }

            mRacePagerSelectSongView?.mSongManageMethod = {
                SongManagerActivity.open(activity, mRoomData)
            }

            mRacePagerSelectSongView?.showView()
        }
    }

    private fun initRaceMatchView() {
        mRaceMatchView = rootView.findViewById(R.id.race_match_view)
        mRaceMatchView.roomData = mRoomData
    }

    private fun initResultView() {
        mRaceNoSingCardView = rootView.findViewById(R.id.race_nosinger_result_view)
        mRaceMiddleResultView = rootView.findViewById(R.id.race_middle_result_view)
        mRaceMiddleResultView.setRaceRoomData(mRoomData)
    }


//    private fun initSelectSongView() {
//        mRaceSelectSongView = rootView.findViewById(R.id.race_select_song_view)
//        mRaceSelectSongView.setRoomData(mRoomData) { choiceID, seq ->
//            mCorePresenter.wantSingChance(choiceID, seq)
//        }
//        mRaceSelectSongView.visibility = View.GONE
//    }

    private fun initRightView() {
        mRaceRightOpView = rootView.findViewById(R.id.race_right_op_view)
        mRaceRightOpView.setListener(object : RightOpListener {
            override fun onClickGiveUp() {
                // 放弃演唱
                mCorePresenter.giveupSing {
                    if (it) {
                        mRaceRightOpView.showGiveUp(true)
                    } else {
                        MyLog.e(TAG, "onClickGiveUp 请求失败了")
                    }
                }
            }

            override fun onClickVote() {
                // 投票
                mCorePresenter.sendBLight { isSucess ->
                    if (isSucess) {
                        mRaceRightOpView.showVote(true)
                    } else {
                        MyLog.e(TAG, "onClickVote 请求失败了")
                    }
                }
            }
        })
    }

    private fun initVipEnterView() {
        mVIPEnterView = VIPEnterView(rootView.findViewById(R.id.vip_enter_view_stub))
    }

    private fun initSingSenceView() {
        mRaceSelfSingLyricView = RaceSelfSingLyricView(rootView.findViewById(R.id.race_self_sing_lyric_view_stub) as ViewStub, mRoomData)
        mRaceOtherSingCardView = RaceOtherSingCardView(rootView.findViewById(R.id.race_other_sing_lyric_view_stub) as ViewStub, mRoomData)
    }

    private fun initBgEffectView() {
        mGameEffectBgView = GameEffectBgView(rootView.findViewById(R.id.game_effect_bg_view_layout_viewStub))
    }

    private fun initInputView() {
        mInputContainerView = rootView.findViewById(R.id.input_container_view)
        mInputContainerView.setRoomData(mRoomData)
    }

    private fun initTurnSenceView() {
        mRaceWaitingCardView = rootView.findViewById(R.id.race_wait_card_view)
        mRaceWaitingCardView.visibility = View.GONE
        mRaceTurnInfoCardView = rootView.findViewById(R.id.race_turn_card_view)
        mRaceTurnInfoCardView.visibility = View.GONE
    }

    private fun initBottomView() {
        run {
            val voiceStub = rootView.findViewById<ViewStub>(R.id.voice_record_tip_view_stub)
            mVoiceRecordTipsView = VoiceRecordTipsView(voiceStub)
        }
//        mBottomBgVp = rootView.findViewById<ViewGroup>(R.id.bottom_bg_vp)
//        val lp = mBottomBgVp.getLayoutParams() as RelativeLayout.LayoutParams
//        /**
//         * 按比例适配手机
//         */
//        lp.height = U.getDisplayUtils().screenHeight * 284 / 667

        mBottomContainerView = rootView.findViewById(R.id.bottom_container_view)
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
                showGiftPanelView()
            }

            override fun onClickFlower() {
                buyFlowerFromOuter()
            }
        })
    }

    private fun showGiftPanelView() {
        mContinueSendView.setVisibility(View.GONE)
        if (mRoomData.realRoundInfo?.status == ERaceRoundStatus.ERRS_ONGOINE.value) {
            RoomDataUtils.getPlayerInfoById(mRoomData!!, mRoomData!!.realRoundInfo!!.subRoundInfo[mRoomData!!.realRoundInfo!!.subRoundSeq - 1].userID)?.let {
                if (it.userInfo.userId != MyUserInfoManager.uid.toInt()) {
                    mGiftPanelView.show(it.userInfo)
                } else {
                    U.getToastUtil().showShort("只能给正在演唱的其他选手送礼哦～")
                }
            }
        } else {
            U.getToastUtil().showShort("只能给正在演唱的其他选手送礼哦～")
        }
    }

    private fun buyFlowerFromOuter() {
        if (mRoomData.realRoundInfo?.status == ERaceRoundStatus.ERRS_ONGOINE.value) {
            RoomDataUtils.getPlayerInfoById(mRoomData!!, mRoomData!!.realRoundInfo!!.subRoundInfo[mRoomData!!.realRoundInfo!!.subRoundSeq - 1].userID)?.let {
                if (it.userInfo.userId != MyUserInfoManager.uid.toInt()) {
                    EventBus.getDefault().post(BuyGiftEvent(NormalGift.getFlower(), it.userInfo))
                } else {
                    U.getToastUtil().showShort("只能给正在演唱的其他选手送礼哦～")
                }
            }
        } else {
            U.getToastUtil().showShort("只能给正在演唱的其他选手送礼哦～")
        }
    }

    private fun initTopView() {
        mRaceTopOpView = rootView.findViewById(R.id.race_top_op_view)
        mRaceTopOpView.setRoomData(mRoomData)
        mRaceTopOpView.setListener(object : RaceTopOpView.Listener {
            override fun onClickVoiceAudition() {
                // 调音面板
                U.getKeyBoardUtils().hideSoftInputKeyBoard(activity)
                dismissDialog()
                if (mRaceVoiceControlPanelView == null) {
                    mRaceVoiceControlPanelView = RaceVoiceControlPanelView(context!!)
                    mRaceVoiceControlPanelView?.setRoomData(mRoomData)
                }
                mRaceVoiceControlPanelView?.showByDialog()
            }

            override fun onClickFeedBack() {
                U.getFragmentUtils().addFragment(
                        FragmentUtils.newAddParamsBuilder(activity, QuickFeedbackFragment::class.java)
                                .setAddToBackStack(true)
                                .setHasAnimation(true)
                                .addDataBeforeAdd(0, QuickFeedbackFragment.FROM_RACE_ROOM)
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
                U.getKeyBoardUtils().hideSoftInputKeyBoard(activity)
                showGameRuleDialog()
            }

            override fun onClickChangeRoom() {
                StatisticsAdapter.recordCountEvent("rank", "changeroom", null)
                mBeginChangeRoomTs = System.currentTimeMillis()
                mGrabChangeRoomTransitionView.setVisibility(View.VISIBLE)
                mCorePresenter?.changeRoomForAudience()
            }
        })


        mRaceTopContentView = rootView.findViewById(R.id.race_top_content_view)
        mRaceTopContentView.setRoomData(mRoomData)

        mRaceTopContentView.setListener(object : RaceTopContentView.Listener {
            override fun clickMore() {
                dismissDialog()
                mRaceActorPanelView = RaceActorPanelView(this@RaceRoomFragment, mRoomData)
                mRaceActorPanelView?.showByDialog()
            }

            override fun clickArrow(open: Boolean) {
                if (open) {
                    mRaceWidgetAnimationController.open()
                } else {
                    mRaceWidgetAnimationController.close()
                }
            }
        })
//        mPracticeFlagIv = rootView.findViewById<ExImageView>(R.id.practice_flag_iv)
        // 加上状态栏的高度
        val statusBarHeight = U.getStatusBarUtil().getStatusBarHeight(context)

//        run {
//            val topLayoutParams = mRaceTopOpView.getLayoutParams() as ConstraintLayout.LayoutParams
//            topLayoutParams.topMargin = statusBarHeight + topLayoutParams.topMargin
//        }
//        run {
//            val topLayoutParams = mRaceTopContentView.getLayoutParams() as ConstraintLayout.LayoutParams
//            topLayoutParams.topMargin = statusBarHeight + topLayoutParams.topMargin
//        }

        mRaceTopVsView = rootView.findViewById(R.id.race_top_vs_view)
        mRaceTopVsView.setRaceRoomData(mRoomData)
    }

    private fun showGameRuleDialog() {
        dismissDialog()
        mGameRuleDialog = DialogPlus.newDialog(context!!)
                .setContentHolder(ViewHolder(R.layout.race_game_rule_view_layout))
                .setContentBackgroundResource(R.color.transparent)
                .setOverlayBackgroundResource(R.color.black_trans_50)
                .setMargin(U.getDisplayUtils().dip2px(16f), -1, U.getDisplayUtils().dip2px(16f), -1)
                .setExpanded(false)
                .setGravity(Gravity.CENTER)
                .create()
        mGameRuleDialog?.show()
    }

    private fun initCommentView() {
        mCommentView = rootView.findViewById(R.id.comment_view)
        mCommentView.setListener(CommentViewItemListener { userId ->
            showPersonInfoView(userId)
        })
        mCommentView.roomData = mRoomData
        mVoiceRecordUiController = VoiceRecordUiController(mBottomContainerView.mVoiceRecordBtn!!, mVoiceRecordTipsView, mCommentView)
        mFlyCommentView = rootView.findViewById(R.id.fly_comment_view)
        mFlyCommentView?.roomData = mRoomData
    }

    private fun initGiftPanelView() {
        mGiftPanelView = rootView.findViewById<View>(R.id.gift_panel_view) as GiftPanelView
        mGiftPanelView.setRoomData(mRoomData)
        mContinueSendView = rootView.findViewById<View>(R.id.continue_send_view) as ContinueSendView
        mContinueSendView.mScene = ContinueSendView.EGameScene.GS_Race
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
        val giftContinueViewGroup = rootView.findViewById<GiftContinueViewGroup>(R.id.gift_continue_vg)
        giftContinueViewGroup.setRoomData(mRoomData)
        val giftOverlayAnimationViewGroup = rootView.findViewById<GiftOverlayAnimationViewGroup>(R.id.gift_overlay_animation_vg)
        giftOverlayAnimationViewGroup.setRoomData(mRoomData)
        val giftBigAnimationViewGroup = rootView.findViewById<GiftBigAnimationViewGroup>(R.id.gift_big_animation_vg)
        giftBigAnimationViewGroup.setRoomData(mRoomData)
        val giftBigContinueView = rootView.findViewById<RaceGiftBigContinuousView>(R.id.gift_big_continue_view)
        giftBigContinueView.mRoomData = mRoomData
        giftBigAnimationViewGroup.setGiftBigContinuousView(giftBigContinueView)
        //mDengBigAnimation = rootView.findViewById<View>(R.id.deng_big_animation) as GrabDengBigAnimationView
    }

    override fun startEnterAnimation(playerInfoModel: UserInfoModel, finishCall: () -> Unit) {
        val model: UserInfoModel = playerInfoModel.clone() as UserInfoModel
        model.nickname = RoomDataUtils.getRaceDisplayNickName(mRoomData, playerInfoModel)
        model.avatar = RoomDataUtils.getRaceDisplayAvatar(mRoomData, playerInfoModel)
        mVIPEnterView?.enter(model, finishCall, false)
    }

    private fun showPersonInfoView(userID: Int) {
        if (!U.getNetworkUtils().hasNetwork()) {
            U.getToastUtil().showShort("网络异常，请检查网络后重试!")
            return
        }
        dismissDialog()
        mInputContainerView.hideSoftInput()
        mPersonInfoDialog = PersonInfoDialog.Builder(activity, QuickFeedbackFragment.FROM_RACE_ROOM, userID, false, false)
                .setRoomID(mRoomData.gameId)
                .build()
        mPersonInfoDialog?.show()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: RaceWantSingChanceEvent) {
        mSignUpView?.setType(RaceSignUpBtnView.SignUpType.SIGN_UP_FINISH)
        mRacePagerSelectSongView?.hideView()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: ShowPersonCardEvent) {
        if (!mRoomData.isFakeForMe(event.uid)) {
            showPersonInfoView(event.uid)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: ShowReportEvent) {
        // 展示是否举报的弹窗
        dismissDialog()
        mTipsDialogView = TipsDialogView.Builder(context)
                .setMessageTip("是否举报ta？")
                .setConfirmTip("举报")
                .setCancelTip("取消")
                .setConfirmBtnClickListener {
                    mTipsDialogView?.dismiss(false)

                    U.getFragmentUtils().addFragment(
                            FragmentUtils.newAddParamsBuilder(activity, QuickFeedbackFragment::class.java)
                                    .setAddToBackStack(true)
                                    .setHasAnimation(true)
                                    .addDataBeforeAdd(0, QuickFeedbackFragment.FROM_RACE_ROOM)
                                    .addDataBeforeAdd(1, QuickFeedbackFragment.REPORT)
                                    .addDataBeforeAdd(2, event.uid)
                                    .setEnterAnim(com.component.busilib.R.anim.slide_in_bottom)
                                    .setExitAnim(com.component.busilib.R.anim.slide_out_bottom)
                                    .build())

                }
                .setCancelBtnClickListener {
                    mTipsDialogView?.dismiss()
                }
                .build()
        mTipsDialogView?.showByDialog()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: RaceScoreChangeEvent) {
        mRaceTopVsView.updateData()
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
    fun onEvent(event: AddSongEvent) {
        mCorePresenter.wantSingChance(event.songModel.itemID, event.songModel)
    }

    override fun singBySelfFirstRound(songModel: SongModel?) {
        MyLog.d(TAG, "singBySelfFirstRound songModel = ${songModel?.toSimpleString()}")
        hideAllSceneView()
        mRaceTopVsView.visibility = View.VISIBLE
        mRaceTopVsView.startVs()
        mRaceTopVsView.startSingBySelf {
            hideAllSceneView()
            mRaceRightOpView.showGiveUp(false)
            //            mRaceTopVsView.visibility = View.GONE
            mRaceSelfSingLyricView.startFly {
                mCorePresenter.sendSingComplete("singBySelfFirstRound")
            }
            mRaceSelfSingLyricView.setVisibility(View.VISIBLE)

            mRaceOtherSingCardView.bindData()
            mRaceOtherSingCardView.setVisibility(View.VISIBLE)

            showFlyCommentView()
            playBgEffect(1)
        }

        hideSignUpUI(true)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: ShowHalfRechargeFragmentEvent) {
        val channelService = ARouter.getInstance().build(RouterConstants.SERVICE_HOME).navigation() as IHomeService
        val baseFragmentClass = channelService.getData(2, null) as Class<android.support.v4.app.Fragment>
        U.getFragmentUtils().addFragment(
                FragmentUtils.newAddParamsBuilder(activity, baseFragmentClass)
                        .setEnterAnim(R.anim.slide_in_bottom)
                        .setExitAnim(R.anim.slide_out_bottom)
                        .setAddToBackStack(true)
                        .setFragmentDataListener(object : FragmentDataListener {
                            override fun onFragmentResult(requestCode: Int, resultCode: Int, bundle: Bundle?, obj: Any?) {
                                //充值成功
                                if (requestCode == 100 && resultCode == 0) {
                                    mGiftPanelView.updateZS()
                                    showGiftPanelView()
                                }
                            }
                        })
                        .setHasAnimation(true)
                        .build())
    }

    override fun singByOtherFirstRound(songModel: SongModel?, userModel: UserInfoModel?) {
        MyLog.d(TAG, "singByOtherFirstRound songModel = ${songModel?.toSimpleString()}, userModel = ${userModel?.toSimpleString()}")
        hideAllSceneView()
        mRaceTopVsView.visibility = View.VISIBLE
        mRoomData.realRoundInfo?.let {
            if (it.enterStatus == ERaceRoundStatus.ERRS_ONGOINE.value && it.enterSubRoundSeq == it.subRoundSeq) {
                MyLog.d(TAG, "singByOtherFirstRound 中途进来的, $it")
                //中途进来的
                mRaceTopVsView.bindData()
                mRaceTopVsView?.raceTopVsIv.visibility = View.VISIBLE
                showRightVote()
                mRaceOtherSingCardView.bindData()
                hideAllSceneView()
                mRaceOtherSingCardView.setVisibility(View.VISIBLE)
                playBgEffect(1)
            } else {
                mRaceTopVsView.startVs()
                mRaceTopVsView.startSingByOther(1) {
                    showRightVote()
                    mRaceOtherSingCardView.bindData()
                    hideAllSceneView()
                    mRaceOtherSingCardView.setVisibility(View.VISIBLE)
                    playBgEffect(1)
                }
            }
        }

        hideSignUpUI(mRoomData.realRoundInfo?.isSingerByUserId(MyUserInfoManager.uid.toInt()) == true)
    }

    private fun hideSignUpUI(hide: Boolean) {
        if (hide) {
            mRacePagerSelectSongView?.hideView()
            mSignUpView?.visibility = View.GONE
        }
    }

    private fun showRightVote() {
        if (mRoomData.realRoundInfo?.isSingerByUserId(MyUserInfoManager.uid.toInt()) == true) {
            MyLog.d(TAG, "showRightVote 是演唱者")
            mRaceRightOpView.visibility = View.GONE
        } else {
            if (mRoomData.audience) {
                if (mRoomData.realRoundInfo?.enterStatus == ERaceRoundStatus.ERRS_ONGOINE.value) {
                    // 中途进来不展示
                    mRaceRightOpView.visibility = View.GONE
                } else {
                    mRaceRightOpView.showVote(false)
                }
            } else {
                val role = mRoomData.getPlayerOrWaiterInfoModel(MyUserInfoManager.uid.toInt())?.role
                if (role == ERUserRole.ERUR_PLAY_USER.value) {
                    MyLog.d(TAG, "showRightVote 当前身份是play")
                    mRaceRightOpView.showVote(false)
                } else {
                    MyLog.d(TAG, "showRightVote 当前身份role=$role")
                    mRaceRightOpView.visibility = View.GONE
                }
            }
        }
    }

    override fun singBySelfSecondRound(songModel: SongModel?) {
        MyLog.d(TAG, "singBySelfSecondRound songModel = ${songModel?.toSimpleString()}")
        hideAllSceneView()
        mRaceRightOpView.visibility = View.GONE
        mRaceTopVsView.visibility = View.VISIBLE
        mRaceTopVsView.bindData()
        mRaceTopVsView.startSingBySelf {
            hideAllSceneView()
            mRaceRightOpView.showGiveUp(false)
            mRaceSelfSingLyricView.startFly {
                mCorePresenter.sendSingComplete("singBySelfSecondRound")
            }
            mRaceSelfSingLyricView.setVisibility(View.VISIBLE)

            mRaceOtherSingCardView.bindData()
            mRaceOtherSingCardView.setVisibility(View.VISIBLE)

            showFlyCommentView()
            playBgEffect(2)
        }
        hideSignUpUI(true)
    }

    override fun singByOtherSecondRound(songModel: SongModel?, userModel: UserInfoModel?) {
        MyLog.d(TAG, "singByOtherSecondRound songModel = ${songModel?.toSimpleString()}, userModel = ${userModel?.toSimpleString()}")
        hideAllSceneView()
        mRaceRightOpView.visibility = View.GONE
        mRaceTopVsView.visibility = View.VISIBLE
        mRoomData.realRoundInfo?.let {
            if (it.enterStatus == ERaceRoundStatus.ERRS_ONGOINE.value && it.enterSubRoundSeq == it.subRoundSeq) {
                showRightVote()
                mRaceTopVsView?.raceTopVsIv.visibility = View.VISIBLE
                mRaceTopVsView.bindData()
                mRaceOtherSingCardView.bindData()
                hideAllSceneView()
                mRaceOtherSingCardView.setVisibility(View.VISIBLE)
                playBgEffect(2)
            } else {
                mRaceTopVsView.startSingByOther(2) {
                    showRightVote()
                    mRaceOtherSingCardView.bindData()
                    hideAllSceneView()
                    mRaceOtherSingCardView.setVisibility(View.VISIBLE)
                    playBgEffect(2)
                }
            }
        }

        hideSignUpUI(mRoomData.realRoundInfo?.isSingerByUserId(MyUserInfoManager.uid.toInt()) == true)
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

    private fun playBgEffect(seq: Int) {
        val now = mRoomData!!.realRoundInfo
        if (seq == 1) {
            if (now?.showInfos != null && now?.showInfos.size >= 1) {
                mGameEffectBgView.showBgEffect(now?.showInfos[0].effectModel)
            } else {
                mGameEffectBgView.hideBg()
            }
        } else if (seq == 2) {
            if (now?.showInfos != null && now?.showInfos.size >= 2) {
                mGameEffectBgView.showBgEffect(now?.showInfos[1].effectModel)
            } else {
                mGameEffectBgView.hideBg()
            }
        }
    }

    override fun showRoundOver(lastRoundInfo: RaceRoundInfoModel, continueOp: (() -> Unit)?) {
        MyLog.d(TAG, "showRoundOver lastRoundInfo = $lastRoundInfo, continueOp = $continueOp")
        mRaceRightOpView.visibility = View.GONE
        mRaceTopVsView.visibility = View.GONE
        if (lastRoundInfo.overReason == ERaceRoundOverReason.ERROR_NO_ONE_SING.value ||
                lastRoundInfo.overReason == ERaceRoundOverReason.ERROR_NOT_ENOUTH_PLAYER.value) {
            // 无人应战
            hideAllSceneView()
            mRaceNoSingCardView.visibility = View.VISIBLE
            mRaceNoSingCardView.showAnimation(object : AnimationListener {
                override fun onFinish() {
                    continueOp?.invoke()
                }
            })
        } else if (lastRoundInfo.overReason == ERaceRoundOverReason.ERROR_UNKNOWN.value) {
            hideAllSceneView()
            continueOp?.invoke()
        } else {
            hideAllSceneView()
            mRaceMiddleResultView.visibility = View.VISIBLE
            mRaceMiddleResultView.showResult(lastRoundInfo) {
                continueOp?.invoke()
            }
        }
    }

    override fun showWaiting(showAnimation: Boolean) {
        MyLog.d(TAG, "showWaiting showAnimation = $showAnimation")
        mRaceRightOpView.visibility = View.GONE
        hideAllSceneView()
        mRaceWaitingCardView.visibility = View.VISIBLE
        if (showAnimation) {
            mRaceWaitingCardView.animationEnter()
        } else {
            mRaceWaitingCardView.visibility = View.VISIBLE
        }
    }

    override fun showChoiceView(showNextRound: Boolean) {
        MyLog.d(TAG, "showChoiceView showNextRound = $showNextRound")
        mRaceRightOpView.visibility = View.GONE
        if (showNextRound) {
            hideAllSceneView()
            mRaceTurnInfoCardView.visibility = View.VISIBLE
            // 下一首动画
            mRaceTurnInfoCardView.showAnimation(object : AnimationListener {
                override fun onFinish() {
                    showSelectSongView()
                }
            })
        } else {
            showSelectSongView()
        }
    }

    override fun showMatchAnimationView(overListener: () -> Unit) {
        MyLog.d(TAG, "showMatchAnimationView")
        hideAllSceneView()
        mRaceMatchView.visibility = View.VISIBLE
        mRaceMatchView.bindData {
            overListener.invoke()
        }
    }

    /**
     * 直接弹出选歌面板
     */
    private fun showSelectSongView() {
        mRaceWantingSignUpCardView.showAnimation(object : AnimationListener {
            override fun onFinish() {

            }
        })
    }

    override fun joinNotice(playerInfoModel: UserInfoModel?) {
        playerInfoModel?.let {
            if (it.userId != MyUserInfoManager.uid.toInt()) {
                mVipEnterPresenter?.addNotice(it)
            }
        }
    }

    override fun goResultPage(lastRound: RaceRoundInfoModel) {
        activity?.finish()
        ARouter.getInstance().build(RouterConstants.ACTIVITY_RACE_RESULT)
                .withInt("roomID", mRoomData.gameId)
                .withInt("roundSeq", lastRound.roundSeq)
                .navigation()
    }

    override fun useEventBus(): Boolean {
        return true
    }

    override fun setData(type: Int, data: Any?) {
        if (type == 0) {
            mRoomData = data as RaceRoomData
        }
    }

    override fun onBackPressed(): Boolean {
        if (mInputContainerView.onBackPressed()) {
            return true
        }
        if (mGiftPanelView.onBackPressed()) {
            return true
        }
        mRacePagerSelectSongView?.let {
            if (it.onBackPressed()) {
                return true
            }
        }

        dismissDialog()
        mTipsDialogView = TipsDialogView.Builder(context)
                .setMessageTip("确定要退出排位赛吗")
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

    override fun gameOver(lastRound: RaceRoundInfoModel?) {
        quitGame()
    }

    override fun onChangeRoomResult(success: Boolean, errMsg: String?) {
        val t = System.currentTimeMillis() - mBeginChangeRoomTs
        if (t > 1500) {
            mGrabChangeRoomTransitionView.setVisibility(View.GONE)
            if (!success) {
                U.getToastUtil().showShort(errMsg)
            }
        } else {
            mUiHanlder.postDelayed({
                if (!success) {
                    U.getToastUtil().showShort(errMsg)
                }
                mGrabChangeRoomTransitionView.setVisibility(View.GONE)
            }, 1500 - t)
        }
        if (success) {
//            initBgView()
//            hideAllCardView()
            hideAllSceneView()
            mRaceTopVsView.switchRoom()
            mVipEnterPresenter?.switchRoom()
            mVIPEnterView?.switchRoom()
            // 重新决定显示mic按钮
            mBottomContainerView?.setRoomData(mRoomData!!)
        }
    }

    fun quitGame() {
        mCorePresenter.exitRoom("quitGame")
        activity?.finish()

//        ARouter.getInstance().build(RouterConstants.ACTIVITY_RACE_RESULT)
//                .withInt("roomID", mRoomData.gameId)
//                .withInt("roundSeq", mRoomData.realRoundSeq)
//                .navigation()
    }

    override fun destroy() {
        super.destroy()
        dismissDialog()
        mFlyCommentView?.destory()
        mRaceActorPanelView?.destory()
        mGiftPanelView.destroy()
    }

    private fun dismissDialog() {
        mRaceActorPanelView?.dismiss(false)
        mPersonInfoDialog?.dismiss(false)
        mRaceVoiceControlPanelView?.dismiss(false)
        mGameRuleDialog?.dismiss(false)
        mTipsDialogView?.dismiss(false)
    }
}
