package com.module.playways.battle.room

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
import com.common.core.userinfo.model.UserInfoModel
import com.common.flutter.boost.FlutterBoostController
import com.common.log.DebugLogView
import com.common.log.MyLog
import com.common.utils.FragmentUtils
import com.common.utils.U
import com.common.view.ex.ExConstraintLayout
import com.component.busilib.constans.GameModeType
import com.component.busilib.view.GameEffectBgView
import com.component.dialog.ClubCardDialogView
import com.component.dialog.ConfirmDialog
import com.component.dialog.PersonInfoDialog
import com.component.person.event.SendGiftByPersonCardEvent
import com.component.person.event.ShowPersonCardEvent
import com.component.report.fragment.QuickFeedbackFragment
import com.dialog.view.TipsDialogView
import com.module.RouterConstants
import com.module.home.IHomeService
import com.module.playways.IPlaywaysModeService
import com.module.playways.R
import com.module.playways.battle.match.model.JoinBattleRoomRspModel
import com.module.playways.battle.room.bottom.BattleBottomContainerView
import com.module.playways.battle.room.model.BattleRoundInfoModel
import com.module.playways.battle.room.presenter.BattleCorePresenter
import com.module.playways.battle.room.top.BattleTopContentView
import com.module.playways.battle.room.top.BattleTopOpView
import com.module.playways.battle.room.ui.BattleWidgetAnimationController
import com.module.playways.battle.room.ui.IBattleRoomView
import com.module.playways.battle.room.view.*
import com.module.playways.grab.room.inter.IGrabVipView
import com.module.playways.grab.room.presenter.VipEnterPresenter
import com.module.playways.grab.room.view.GrabChangeRoomTransitionView
import com.module.playways.grab.room.view.GrabScoreTipsView
import com.module.playways.grab.room.view.VIPEnterView
import com.module.playways.grab.room.voicemsg.VoiceRecordTipsView
import com.module.playways.grab.room.voicemsg.VoiceRecordUiController
import com.module.playways.listener.AnimationListener
import com.module.playways.room.data.H
import com.module.playways.room.gift.event.BuyGiftEvent
import com.module.playways.room.gift.event.ShowHalfRechargeFragmentEvent
import com.module.playways.room.gift.view.ContinueSendView
import com.module.playways.room.gift.view.GiftDisplayView
import com.module.playways.room.gift.view.GiftPanelView
import com.module.playways.room.room.comment.CommentView
import com.module.playways.room.room.comment.listener.CommentViewItemListener
import com.module.playways.room.room.gift.GiftBigAnimationViewGroup
import com.module.playways.room.room.gift.GiftBigContinuousView
import com.module.playways.room.room.gift.GiftContinueViewGroup
import com.module.playways.room.room.gift.GiftOverlayAnimationViewGroup
import com.module.playways.room.room.view.BottomContainerView
import com.module.playways.room.room.view.InputContainerView
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


@Route(path = RouterConstants.ACTIVITY_BATTLE_ROOM)
class BattleRoomActivity : BaseActivity(), IBattleRoomView, IGrabVipView {

    companion object {
        val playerTag = "BattleRoomActivity"
    }

    private fun ensureActivityTop() {
        // 销毁其他的除排麦房页面所有界面
        for (i in U.getActivityUtils().activityList.size - 1 downTo 0) {
            val activity = U.getActivityUtils().activityList[i]
            if (activity === this) {
                continue
            }
//            if (activity is RelayHomeActivity) {
//                continue
//            }
            if (U.getActivityUtils().isHomeActivity(activity)) {
                continue
            }

            activity.finish()
        }
    }

    /**
     * 存起该房间一些状态信息
     */
    internal var mRoomData = BattleRoomData()
    private lateinit var mCorePresenter: BattleCorePresenter
    //基础ui组件
    internal lateinit var mInputContainerView: InputContainerView
    internal lateinit var mBottomContainerView: BattleBottomContainerView
    internal lateinit var mVoiceRecordTipsView: VoiceRecordTipsView
    internal lateinit var mCommentView: CommentView
    internal lateinit var mGiftPanelView: GiftPanelView
    internal lateinit var mContinueSendView: ContinueSendView
    internal lateinit var mTopOpView: BattleTopOpView
    internal lateinit var mTopContentView: BattleTopContentView

    internal lateinit var mBattleBeginView: BattleBeginTipsView
    internal lateinit var mBattleRoundOverCardView: BattleRoundOverCardView
    internal lateinit var mBattleGameOverCardView: BattleGameOverCardView

    internal lateinit var mBattleGiveUpView: BattleGiveUpView
    internal lateinit var mBattleGrabView: BattleGrabView
    internal lateinit var mBattlePropsCardView: BattlePropsCardView
    internal lateinit var mBattleOtherSingCardView: BattleOtherSingCardView
    internal lateinit var mBattleSelfSingLyricView: BattleSelfSingLyricView
    internal lateinit var mBattleSongGuideView: BattleSongGuideView

    internal lateinit var mScoreTipsView: GrabScoreTipsView

    internal lateinit var mGameEffectBgView: GameEffectBgView

    var mChangeRoomTransitionView: GrabChangeRoomTransitionView? = null

    private var mVIPEnterView: VIPEnterView? = null
    // 都是dialogplus
    private var mMainActContainer: ExConstraintLayout? = null
    private var mPersonInfoDialog: PersonInfoDialog? = null
    private var mGameRuleDialog: DialogPlus? = null
    private var mTipsDialogView: TipsDialogView? = null
    private var mVoiceControlPanelView: BattleVoiceControlPanelView? = null
    private var mConfirmDialog: ConfirmDialog? = null
    private var mClubCardDialogView: ClubCardDialogView? = null

    private var mVipEnterPresenter: VipEnterPresenter? = null

    lateinit var mVoiceRecordUiController: VoiceRecordUiController

    val mWidgetAnimationController = BattleWidgetAnimationController(this)

    internal var mSkrAudioPermission = SkrAudioPermission()

    val SP_KEY_HOST_TIP_TIMES = "sp_key_host_tips_show_times"
//    val REMOVE_HOST_OP_TIP_MSG = 0x01     // 主持人操作提示
//    val CHECK_GO_MIC_TIP_MSG = 0x02  // 去上麦或者换房间的提示

    val mUiHandler = object : Handler() {
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            when (msg?.what) {
//                REMOVE_HOST_OP_TIP_MSG -> {
//                    removeHostOpTips()
//                }
//                CHECK_GO_MIC_TIP_MSG -> {
//                    showGoMicTips()
//                }
            }
        }
    }

    override fun initView(savedInstanceState: Bundle?): Int {
        return R.layout.battle_room_fragment_layout
    }

    override fun initData(savedInstanceState: Bundle?) {
        ensureActivityTop()
        var rspModel = intent.getSerializableExtra("JoinBattleRoomRspModel") as JoinBattleRoomRspModel?
        rspModel?.let {
            mRoomData.loadFromRsp(it)
            MyLog.d(TAG, "initData mRoomData=$mRoomData")
        }
        H.battleRoomData = mRoomData
        H.setType(GameModeType.GAME_MODE_BATTLE, "BattleRoomActivity")

        mCorePresenter = BattleCorePresenter(mRoomData, this)
        addPresent(mCorePresenter)
        mVipEnterPresenter = VipEnterPresenter(this, mRoomData)
        addPresent(mVipEnterPresenter)
        // 请保证从下面的view往上面的view开始初始化
        mMainActContainer = findViewById(R.id.main_act_container)
        mMainActContainer?.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                mInputContainerView.hideSoftInput()
            }
            false
        }

        initBgEffectView()
        initTopView()
        initTurnChangeView()
        initInputView()
        initBottomView()
        initGiftPanelView()
        initGiftDisplayView()

        initRightOpView()
        initVipEnterView()
        initChangeRoomTransitionView()

        initCardView()
        initCommentView()

        initOpView()

        initScoreView()

        mCorePresenter.onOpeningAnimationOver()

        mUiHandler.postDelayed(Runnable {
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

        MyUserInfoManager.myUserInfo?.let {
            if (it.ranking != null) {
                mVipEnterPresenter?.addNotice(MyUserInfo.toUserInfoModel(it))
            }
        }
        U.getStatusBarUtil().setTransparentBar(this, false)
    }

    private fun initTurnChangeView() {
        // 对战开始，轮次结束和 游戏结束
        mBattleBeginView = this.findViewById(R.id.battle_begin_view)

        mBattleRoundOverCardView = BattleRoundOverCardView(this.findViewById<ViewStub>(R.id.battle_round_over_view_layour_viewStub))

        mBattleGameOverCardView = BattleGameOverCardView(this.findViewById<ViewStub>(R.id.battle_game_over_view_layour_viewStub))

    }

    private fun initCardView() {
        mBattleSongGuideView = BattleSongGuideView(findViewById(R.id.battle_song_guide_view_layout_viewStub), mRoomData)
        mBattleSelfSingLyricView = BattleSelfSingLyricView(findViewById(R.id.battle_self_sing_lyric_layout_viewStub), mRoomData)
        mBattleOtherSingCardView = BattleOtherSingCardView(findViewById(R.id.battle_other_sing_view_layout_viewStub), mRoomData)
    }

    private fun initOpView() {
        mBattleGrabView = BattleGrabView(findViewById(R.id.battle_grab_view_layout_viewStub), mRoomData)
        mBattlePropsCardView = BattlePropsCardView(findViewById(R.id.battle_props_card_view_layout_viewStub), mRoomData)
        mBattleGiveUpView = BattleGiveUpView(findViewById(R.id.battle_give_up_view_layout_viewStub), mRoomData)

        mBattleGiveUpView.clickGiveUpFuc = {
            mCorePresenter.giveUpSing {

            }
        }

        mBattleGrabView.clickGrabFuc = {
            mCorePresenter.grabSing()
        }

        mBattlePropsCardView.useSingCardFuc = {
            mCorePresenter.reqHelpSing()
        }

        mBattlePropsCardView.useSwitchSongCardFuc = {
            mCorePresenter.reqSwitchSing()
        }
    }

    private fun initChangeRoomTransitionView() {
        mChangeRoomTransitionView = GrabChangeRoomTransitionView(findViewById(R.id.change_room_transition_view))
        mChangeRoomTransitionView?.setVisibility(View.GONE)
    }

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        mSkrAudioPermission.onBackFromPermisionManagerMaybe(this)
    }

    override fun destroy() {
        if (window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        super.destroy()
        dismissDialog()
        mUiHandler.removeCallbacksAndMessages(null)
//        mBattleApplyPanelView?.destory()
//        mBattleMemberPanelView?.destory()
        mWidgetAnimationController.destroy()
//        mBottomWidgetAnimationController.destroy()
        mGiftPanelView?.destroy()
        H.reset("BattleRoomActivity")
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
        if (mBattleSongGuideView != exclude) {
            mBattleSongGuideView.hide()
        }

        if (mBattleSelfSingLyricView != exclude) {
            mBattleSelfSingLyricView.hide()
        }

        if (mBattleOtherSingCardView != exclude) {
            mBattleOtherSingCardView.hide()
        }

        if (mBattleGiveUpView != exclude) {
            mBattleGiveUpView.hide()
        }

        if (mBattleGrabView != exclude) {
            mBattleGrabView.hide()
        }

        if (mBattlePropsCardView != exclude) {
            mBattlePropsCardView.hide()
        }

    }

    private fun initRightOpView() {
    }

    private fun initScoreView() {
        mScoreTipsView = findViewById(R.id.battle_score_tips_view)
    }

    private fun initVipEnterView() {
        mVIPEnterView = VIPEnterView(findViewById(R.id.vip_enter_view_stub))
    }

    private fun initBgEffectView() {
        mGameEffectBgView = GameEffectBgView(findViewById(R.id.game_effect_bg_view_layout_viewStub))
    }

    private fun initInputView() {
        mInputContainerView = findViewById(R.id.input_container_view)
        mInputContainerView.setRoomData(mRoomData)
    }


    private fun initBottomView() {
        run {
            val voiceStub = findViewById<ViewStub>(R.id.voice_record_tip_view_stub)
            mVoiceRecordTipsView = VoiceRecordTipsView(voiceStub)
        }

        mBottomContainerView = findViewById(R.id.bottom_container_view)
        mBottomContainerView.setRoomData(mRoomData)
        mBottomContainerView.setListener(object : BottomContainerView.Listener() {
            override fun showInputBtnClick() {
                dismissDialog()
                mInputContainerView.showSoftInput()
            }

            override fun clickRoomManagerBtn() {

            }

            override fun showGiftPanel() {
                mContinueSendView.visibility = View.GONE
                showGiftPanelView()
            }

            override fun onClickFlower() {
                buyFlowerFromOuter()
            }
        })
    }

    private fun showGiftPanelView() {
        mGiftPanelView?.show(mRoomData.getFirstTeammate()?.userInfo)
    }

    private fun buyFlowerFromOuter() {
//        EventBus.getDefault().post(BuyGiftEvent(NormalGift.getFlower(), mRoomData.peerUser?.userInfo))
    }

    private fun initTopView() {
        mTopOpView = findViewById(R.id.top_op_view)
        mTopOpView.setListener(object : BattleTopOpView.Listener {

            override fun onClickGameRule() {
                U.getKeyBoardUtils().hideSoftInputKeyBoard(this@BattleRoomActivity)
                showGameRuleDialog()
            }

            override fun onClickFeedBack() {
                U.getFragmentUtils().addFragment(
                        FragmentUtils.newAddParamsBuilder(this@BattleRoomActivity, QuickFeedbackFragment::class.java)
                                .setAddToBackStack(true)
                                .setHasAnimation(true)
                                .addDataBeforeAdd(0, QuickFeedbackFragment.FROM_BATTLE_ROOM)
                                .addDataBeforeAdd(1, QuickFeedbackFragment.FEED_BACK)
                                .addDataBeforeAdd(3, mRoomData.gameId)
                                .setEnterAnim(R.anim.slide_in_bottom)
                                .setExitAnim(R.anim.slide_out_bottom)
                                .build())
            }

            override fun onClickVoiceAudition() {
                // 调音面板
                U.getKeyBoardUtils().hideSoftInputKeyBoard(this@BattleRoomActivity)
                dismissDialog()
                if (mVoiceControlPanelView == null) {
                    mVoiceControlPanelView = BattleVoiceControlPanelView(this@BattleRoomActivity)
                    mVoiceControlPanelView?.setRoomData(mRoomData)
                }
                mVoiceControlPanelView?.showByDialog()
            }

            override fun closeBtnClick() {
                quitGame()
            }
        })
        mTopOpView.bindData()
        mTopContentView = findViewById(R.id.top_content_view)
        mTopContentView.roomData = mRoomData
        mTopContentView.bindData()
        mTopContentView.listener = object : BattleTopContentView.Listener {

            override fun clickArrow(open: Boolean) {
                if (open) {
                    mWidgetAnimationController.open()
                } else {
                    mWidgetAnimationController.close()
                }
            }

        }
    }

    private fun showGameRuleDialog() {
        dismissDialog()
        mGameRuleDialog = DialogPlus.newDialog(this)
                .setContentHolder(ViewHolder(R.layout.battle_game_rule_view_layout))
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

        //设置评论区域高度为歌词卡片区域以外区域 减去剩余组件高度
        val lp = mCommentView.layoutParams
        lp.height = (U.getDisplayUtils().screenWidth * 200f / 667 ).toInt()

    }

    private fun initGiftPanelView() {
        mGiftPanelView = findViewById<View>(R.id.gift_panel_view) as GiftPanelView
        mGiftPanelView.setRoomData(mRoomData)
        mContinueSendView = findViewById<View>(R.id.continue_send_view) as ContinueSendView
        mContinueSendView.mScene = ContinueSendView.EGameScene.GS_Battle
        mContinueSendView.setRoomData(mRoomData)
        mContinueSendView.setObserver(object : ContinueSendView.OnVisibleStateListener {
            override fun onVisible(isVisible: Boolean) {
                mBottomContainerView.setOpVisible(!isVisible)
            }
        })
        mGiftPanelView.setIGetGiftCountDownListener(GiftDisplayView.IGetGiftCountDownListener {
            //                return mGiftTimerPresenter.getCountDownSecond();
            0
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

    override fun startEnterAnimation(playerInfoModel: UserInfoModel, finishCall: () -> Unit) {
        mVIPEnterView?.enter(playerInfoModel, finishCall)
    }

    private fun showPersonInfoView(userID: Int, isShowKick: Boolean?) {
        if (!U.getNetworkUtils().hasNetwork()) {
            U.getToastUtil().showShort("网络异常，请检查网络后重试!")
            return
        }
        dismissDialog()
        mInputContainerView.hideSoftInput()
        var showKick = false
//        if (isShowKick != null) {
//            showKick = isShowKick
//        } else if (mRoomData.myUserInfo?.isHost() == true) {
//            // 主持人 能踢所有人
//            showKick = mRoomData.getPlayerInfoById(userID)?.isHost() != true
//        } else if (mRoomData.myUserInfo?.isAdmin() == true) {
//            // 管理员能踢 嘉宾 观众
//            showKick = !(mRoomData.getPlayerInfoById(userID)?.isAdmin() == true || mRoomData.getPlayerInfoById(userID)?.isHost() == true)
//        }

        mPersonInfoDialog = PersonInfoDialog.Builder(this, QuickFeedbackFragment.FROM_BATTLE_ROOM, userID, showKick, true, true)
                .setRoomID(mRoomData.gameId)
                .setInviteReplyListener { userInfoModel ->
                    val iRankingModeService = ARouter.getInstance().build(RouterConstants.SERVICE_RANKINGMODE).navigation() as IPlaywaysModeService
                    iRankingModeService.tryInviteToRelay(userInfoModel.userId, userInfoModel.isFriend, false)
                }
                .setKickListener {
                    showKickConfirmDialog(it)
                }
                .build()
        mPersonInfoDialog?.show()
    }

    private fun showKickConfirmDialog(model: UserInfoModel) {
        dismissDialog()
        mConfirmDialog = ConfirmDialog(U.getActivityUtils().topActivity, model, ConfirmDialog.TYPE_OWNER_KICK_CONFIRM, 0)
        mConfirmDialog?.setListener { userInfoModel ->
            // 发起踢人请求
//            mCorePresenter.kickOut(userInfoModel.userId)
        }
        mConfirmDialog?.show()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: ShowPersonCardEvent) {
        showPersonInfoView(event.uid, event.showKick)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: SendGiftByPersonCardEvent) {
        val info = mRoomData.getPlayerInfoById(event.model.userId)

        if (info != null) {
            mGiftPanelView.show(info.userInfo)
        } else {
            mGiftPanelView.show(event.model)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: BuyGiftEvent) {
        if (event.receiver.userId != MyUserInfoManager.uid.toInt()) {
            mContinueSendView.startBuy(event.baseGift, event.receiver)
        } else {
            U.getToastUtil().showShort("只能给正在演唱的其他选手送礼哦～")
        }
    }

    private fun finishTopActivity(str: String) {
        var hasActivity = false
        for (i in U.getActivityUtils().activityList.size - 1 downTo 0) {
            val activity = U.getActivityUtils().activityList[i]
            if (activity is BattleRoomActivity) {
                break
            }
            if (U.getActivityUtils().isHomeActivity(activity)) {
                continue
            }
            activity.finish()
            hasActivity = true
        }

        if (!TextUtils.isEmpty(str) && hasActivity) {
            U.getToastUtil().showShort(str)
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
                                    showGiftPanelView()
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

//        if (mMicSeatView.onBackPressed()) {
//            return true
//        }

        quitGame()
        return true
    }


    private fun quitGame() {
        dismissDialog()
        mTipsDialogView = TipsDialogView.Builder(this)
                .setMessageTip("确定要退出对战吗")
                .setConfirmTip("确定")
                .setCancelTip("取消")
                .setConfirmBtnClickListener {
                    mTipsDialogView?.dismiss(false)
                    finish()
                }
                .setCancelBtnClickListener {
                    mTipsDialogView?.dismiss()
                }
                .build()
        mTipsDialogView?.showByDialog()
    }

    private fun dismissDialog() {
        mPersonInfoDialog?.dismiss(false)
        mGameRuleDialog?.dismiss(false)
        mTipsDialogView?.dismiss(false)
        mVoiceControlPanelView?.dismiss(false)
        mConfirmDialog?.dismiss(false)
        mClubCardDialogView?.dismiss(false)
    }

    /** 所有回调的用途见接口类 **/


    override fun showBeginTips(callback: () -> Unit) {
        mBattleBeginView.visibility = View.VISIBLE
        mBattleBeginView.showAnimation(object : AnimationListener {
            override fun onFinish() {
                callback.invoke()
            }
        })
    }

    override fun showIntro() {
        hideAllSceneView(null)
        mBattleSongGuideView.show()
        setSingCardViewHeight(mBattleSongGuideView)
        mTopContentView.bindData()
        if (mRoomData.realRoundInfo?.userID == MyUserInfoManager.uid.toInt()) {
            mBattleGrabView.show {
                mCorePresenter.overWait()
            }
            mBattlePropsCardView.show()
        }
    }

    private fun setSingCardViewHeight(view:BaseSceneView?){
        val lp = view?.realView?.layoutParams?:return
        lp.height = (U.getDisplayUtils().screenHeight * 273f / 667).toInt()
    }

    override fun showRoundOver(lastRound: BattleRoundInfoModel, callback: () -> Unit) {
        hideAllSceneView(null)
        mBattleRoundOverCardView.bindData(lastRound, object : AnimationListener {
            override fun onFinish() {
                callback.invoke()
            }
        })
    }

    private var helpSingDialogCountDownJob: Job? = null

    override fun useHelpSing() {
        /**
         * 队友请求你帮唱，这轮演唱者是你
         */
        var totalTime = 5
        if (mRoomData.realRoundInfo?.userID == MyUserInfoManager.uid.toInt()) {
            dismissDialog()
            var roundSeq = mRoomData.realRoundSeq
            mTipsDialogView = TipsDialogView.Builder(this)
                    .setMessageTip("队友请求帮唱${totalTime}s")
                    .setConfirmTip("开始演唱")
                    .setCancelTip("不会唱")
                    .setConfirmBtnClickListener {
                        mTipsDialogView?.dismiss(false)
                        helpSingDialogCountDownJob?.cancel()
                        mCorePresenter.rspHelpSing(roundSeq, 1)
                    }
                    .setCancelBtnClickListener {
                        mTipsDialogView?.dismiss()
                        helpSingDialogCountDownJob?.cancel()
                        mCorePresenter.rspHelpSing(roundSeq, 2)
                    }
                    .build()
            mTipsDialogView?.showByDialog()
            helpSingDialogCountDownJob = launch {
                for (i in 1..totalTime) {
                    mTipsDialogView?.mMessageTv?.text = "队友请求帮唱${totalTime - i + 1}s"
                    delay(1000)
                }
                mTipsDialogView?.dismiss()
            }
        } else {
            mBattleGrabView.hide()
            mBattlePropsCardView.hide()
        }
    }

    override fun receiveScoreEvent(score: Int, songLineNum: Int) {
        mScoreTipsView.updateScore(score, songLineNum, 5)
    }

    override fun showSelfSing() {
        hideAllSceneView(null)
        mBottomContainerView.unMuteBtn()
        mBattleSelfSingLyricView.show(0) {
            mCorePresenter.overSing()
        }
        setSingCardViewHeight(mBattleSelfSingLyricView)
        mBattleGiveUpView.show()
    }

    override fun showOtherSing() {
        hideAllSceneView(null)
        mBattleOtherSingCardView.show()
        setSingCardViewHeight(mBattleOtherSingCardView)

    }

    override fun gameOver(from: String) {
        MyLog.d(TAG, "gameOver from = $from")
        hideAllSceneView(null)
        mBattleGameOverCardView.bindData(object : AnimationListener {
            override fun onFinish() {
                FlutterBoostController.openFlutterPage(this@BattleRoomActivity, "BattleResultPage", hashMapOf(
                        "roomId" to mRoomData.gameId,
                        "targetNickname" to mRoomData.getFirstTeammate()?.userInfo?.nicknameRemark!!,
                        "targetAvatar" to mRoomData.getFirstTeammate()?.userInfo?.avatar!!
                ))
                finish()
            }
        })
        U.getToastUtil().showShort("游戏结束")
    }
}
