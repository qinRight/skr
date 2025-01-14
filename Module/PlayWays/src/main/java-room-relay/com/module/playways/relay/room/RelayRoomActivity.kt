package com.module.playways.relay.room

import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.constraint.ConstraintLayout
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.alibaba.fastjson.JSON
import com.common.anim.svga.SvgaParserAdapter
import com.common.base.BaseActivity
import com.common.base.FragmentDataListener
import com.common.core.kouling.SkrKouLingUtils
import com.common.core.kouling.api.KouLingServerApi
import com.common.core.myinfo.MyUserInfo
import com.common.core.myinfo.MyUserInfoManager
import com.common.core.permission.SkrAudioPermission
import com.common.core.userinfo.model.UserInfoModel
import com.common.core.view.setAnimateDebounceViewClickListener
import com.common.core.view.setDebounceViewClickListener
import com.common.flutter.boost.FlutterBoostController
import com.common.log.DebugLogView
import com.common.log.MyLog
import com.common.rxretrofit.ApiManager
import com.common.rxretrofit.ApiResult
import com.common.utils.FragmentUtils
import com.common.utils.U
import com.common.view.DiffuseView
import com.component.busilib.constans.GameModeType
import com.component.busilib.view.GameEffectBgView
import com.component.dialog.PersonInfoDialog
import com.component.person.event.ShowPersonCardEvent
import com.component.report.fragment.QuickFeedbackFragment
import com.dialog.view.TipsDialogView
import com.module.RouterConstants
import com.module.home.IHomeService
import com.module.playways.BaseRoomData
import com.module.playways.R
import com.module.playways.grab.room.inter.IGrabVipView
import com.module.playways.grab.room.invite.IInviteCallBack
import com.module.playways.grab.room.invite.fragment.InviteFriendFragment2
import com.module.playways.grab.room.presenter.VipEnterPresenter
import com.module.playways.grab.room.view.GrabScoreTipsView
import com.module.playways.grab.room.view.RelayEnergyView
import com.module.playways.grab.room.view.VIPEnterView
import com.module.playways.grab.room.view.normal.NormalOthersSingCardView
import com.module.playways.grab.room.voicemsg.VoiceRecordTipsView
import com.module.playways.grab.room.voicemsg.VoiceRecordUiController
import com.module.playways.mic.room.model.RoomInviteMusicModel
import com.module.playways.mic.room.top.RoomInviteView
import com.module.playways.party.room.PartyRoomServerApi
import com.module.playways.relay.match.RelayHomeActivity
import com.module.playways.relay.match.model.JoinRelayRoomRspModel
import com.module.playways.relay.room.bottom.RelayBottomContainerView
import com.module.playways.relay.room.event.RelayLockChangeEvent
import com.module.playways.relay.room.model.RelayRoundInfoModel
import com.module.playways.relay.room.presenter.RelayCorePresenter
import com.module.playways.relay.room.top.RelayContinueSingView
import com.module.playways.relay.room.top.RelayTopContentView
import com.module.playways.relay.room.top.RelayTopOpView
import com.module.playways.relay.room.ui.IRelayRoomView
import com.module.playways.relay.room.ui.RelayWidgetAnimationController
import com.module.playways.relay.room.view.RelaySingCardView
import com.module.playways.relay.room.view.RelayVoiceControlPanelView
import com.module.playways.room.data.H
import com.module.playways.room.gift.event.BuyGiftEvent
import com.module.playways.room.gift.event.ShowHalfRechargeFragmentEvent
import com.module.playways.room.gift.model.NormalGift
import com.module.playways.room.gift.view.ContinueSendView
import com.module.playways.room.gift.view.GiftDisplayView
import com.module.playways.room.gift.view.GiftPanelView
import com.module.playways.room.room.comment.fly.FlyCommentView
import com.module.playways.room.room.gift.GiftBigAnimationViewGroup
import com.module.playways.room.room.gift.GiftBigContinuousView
import com.module.playways.room.room.gift.GiftContinueViewGroup
import com.module.playways.room.room.gift.GiftOverlayAnimationViewGroup
import com.module.playways.room.room.view.BottomContainerView
import com.module.playways.room.room.view.InputContainerView
import com.module.playways.room.song.model.SongModel
import com.module.playways.songmanager.SongManagerActivity
import com.module.playways.view.ZanView
import com.opensource.svgaplayer.*
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder
import com.zq.live.proto.RelayRoom.ERRoundStatus
import com.zq.live.proto.RelayRoom.RAddMusicMsg
import com.zq.live.proto.RelayRoom.RReqAddMusicMsg
import io.reactivex.Observable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.RequestBody
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.set


@Route(path = RouterConstants.ACTIVITY_RELAY_ROOM)
class RelayRoomActivity : BaseActivity(), IRelayRoomView, IGrabVipView {
    fun ensureActivtyTop() {
        // 销毁其他的除排麦房页面所有界面
        for (i in U.getActivityUtils().activityList.size - 1 downTo 0) {
            val activity = U.getActivityUtils().activityList[i]
            if (activity === this) {
                continue
            }
            if (activity is RelayHomeActivity) {
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
    internal var mRoomData = RelayRoomData()
    private lateinit var mCorePresenter: RelayCorePresenter
    //    internal var replyRoomInvitePresenter = DoubleRoomInvitePresenter()
    //基础ui组件
    internal lateinit var mInputContainerView: InputContainerView
    internal lateinit var mBottomContainerView: RelayBottomContainerView
    internal lateinit var mVoiceRecordTipsView: VoiceRecordTipsView
    //    internal lateinit var mCommentView: CommentView
    internal lateinit var mGiftPanelView: GiftPanelView
    internal lateinit var mContinueSendView: ContinueSendView
    internal lateinit var mTopOpView: RelayTopOpView
    internal lateinit var mTopContentView: RelayTopContentView
    internal lateinit var mExitIv: ImageView
    internal lateinit var mMainContainerView: ConstraintLayout

    internal lateinit var mGameEffectBgView: GameEffectBgView

    internal var mRoomInviteView: RoomInviteView? = null

    //var othersSingCardView:NormalOthersSingCardView?=null


    // 专场ui组件
//    lateinit var mTurnInfoCardView: MicTurnInfoCardView  // 下一局
    //    lateinit var mOthersSingCardView: OthersSingCardView// 他人演唱卡片
//    lateinit var mSelfSingCardView: SelfSingCardView // 自己演唱卡片
//    lateinit var mSingBeginTipsCardView: MicSingBeginTipsCardView// 演唱开始提示
//    lateinit var mRoundOverCardView: RoundOverCardView// 结果页
//    lateinit var mGrabScoreTipsView: GrabScoreTipsView // 打分提示

    lateinit var mAddSongIv: ImageView
    lateinit var mChangeSongIv: TextView
//    private lateinit var mGiveUpView: GrabGiveupView

    private var mVIPEnterView: VIPEnterView? = null
    private var mContinueSVGAImageView: SVGAImageView? = null
    private var mRelayEnergyView: RelayEnergyView? = null
    private var mDiffuseView: DiffuseView? = null
//    lateinit var mHasSelectSongNumTv: ExTextView

    // 都是dialogplus
    private var mPersonInfoDialog: PersonInfoDialog? = null
    //    private var mGrabKickDialog: ConfirmDialog? = null
    private var mVoiceControlPanelView: RelayVoiceControlPanelView? = null
    //    private var mMicSettingView: MicSettingView? = null
    private var mGameRuleDialog: DialogPlus? = null
    private var mTipsDialogView: TipsDialogView? = null
    private var mRelayContinueSingView: RelayContinueSingView? = null

    internal var mVipEnterPresenter: VipEnterPresenter? = null

    lateinit var mVoiceRecordUiController: VoiceRecordUiController
    val mWidgetAnimationController = RelayWidgetAnimationController(this)
    internal var mSkrAudioPermission = SkrAudioPermission()
    lateinit var relaySingCardView: RelaySingCardView

    var mFlyCommentView: FlyCommentView? = null

    lateinit var mGrabScoreTipsView: GrabScoreTipsView // 打分提示
    var mEnterTime = System.currentTimeMillis()

    val mUiHanlder = object : Handler() {
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
        }
    }

    override fun initView(savedInstanceState: Bundle?): Int {
        return R.layout.relay_room_fragment_layout
    }

    override fun initData(savedInstanceState: Bundle?) {
        ensureActivtyTop()
        H.setType(GameModeType.GAME_MODE_RELAY, "RelayRoomActivity")
        H.relayRoomData = mRoomData
        var joinRaceRoomRspModel = intent.getSerializableExtra("JoinRelayRoomRspModel") as JoinRelayRoomRspModel?
        if (joinRaceRoomRspModel == null) {
            // 构造假数据 用于测试
            var joinRaceRoomRspModel1 = JoinRelayRoomRspModel()
            joinRaceRoomRspModel1?.roomID = 10001
            joinRaceRoomRspModel1?.createTimeMs = (System.currentTimeMillis() / 30000) * 30000
            val list = ArrayList<UserInfoModel>()
            if (MyUserInfoManager.uid.toInt() == 1705476) {
                var userInfoModel = UserInfoModel()
                userInfoModel.userId = 1985618
                list.add(userInfoModel)
            } else {
                var userInfoModel = UserInfoModel()
                userInfoModel.userId = 1705476
                list.add(userInfoModel)
            }
            joinRaceRoomRspModel1.users = list
            var roundInfoModel = RelayRoundInfoModel()
            roundInfoModel.status = ERRoundStatus.RRS_SING.value
            roundInfoModel.singBeginMs = 30 * 1000
            roundInfoModel.userID = 1705476
            var music = SongModel()
            music.itemName = "告白气球"
            music.acc = "http://song-static.inframe.mobi/bgm/e3b214d337f1301420dad255230fe085.mp3"
            music.lyric = "http://song-static.inframe.mobi/lrc/4ee4ac0711c74d6f333fcac10c113239.zrce"
            music.beginMs = 0
            music.endMs = 4 * 60 * 1000
            music.relaySegments = arrayListOf(43 * 1000, 65 * 1000, 87 * 1000)
            roundInfoModel.music = music
            joinRaceRoomRspModel1.currentRound = roundInfoModel
            joinRaceRoomRspModel = joinRaceRoomRspModel1
        }
        joinRaceRoomRspModel?.let {
            mRoomData.loadFromRsp(it)
            MyLog.d(TAG, "initData mRoomData=$mRoomData")
        }
//        H.micRoomData = mRoomData
//        H.setType(GameModeType.GAME_MODE_MIC, "MicRoomActivity")

        mCorePresenter = RelayCorePresenter(mRoomData, this)
        addPresent(mCorePresenter)
        mVipEnterPresenter = VipEnterPresenter(this, mRoomData)
        addPresent(mVipEnterPresenter)
//        addPresent(replyRoomInvitePresenter)
        // 请保证从下面的view往上面的view开始初始化
        mMainContainerView = findViewById(R.id.main_act_container)

        mMainContainerView.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                mInputContainerView.hideSoftInput()
            }
            false
        }

        initBgEffectView()
        initInputView()
        initBottomView()
        initCommentView()
        initGiftPanelView()
        initGiftDisplayView()
        initTopView()
        initTurnSenceView()
        initRelayContinueSingView()

        initVipEnterView()
        initMicSeatView()
        initRelayEnergyView()

        BaseRoomData.syncServerTs()

        if (mRoomData.hasTimeLimit()) {
            mAddSongIv.visibility = View.VISIBLE
        }

        mCorePresenter.onOpeningAnimationOver()

        mUiHanlder.postDelayed({
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

        if (U.getPreferenceUtils().getSettingBoolean("is_first_enter_relay_room", true)) {
            U.getPreferenceUtils().setSettingBoolean("is_first_enter_relay_room", false)
            showGameRuleDialog()
        }

        MyUserInfoManager.myUserInfo?.let {
            if (it.ranking != null) {
                mVipEnterPresenter?.addNotice(MyUserInfo.toUserInfoModel(it))
            }
        }

        if (!mRoomData.hasTimeLimit()) {
            mRelayContinueSingView?.delayShowContinueView(mRoomData.configModel.unLockWaitTimeMs?.toLong())
        }

        U.getStatusBarUtil().setTransparentBar(this, false)
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
        mFlyCommentView?.destory()
        mGiftPanelView?.destroy()
        relaySingCardView?.destroy()
//        mSelfSingCardView?.destroy()
        H.reset("RelayRoomActivity")
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
//        if (mSelfSingCardView != exclude) {
//            mSelfSingCardView.setVisibility(View.GONE)
//        }
//        if (mOthersSingCardView != exclude) {
//            mOthersSingCardView.setVisibility(View.GONE)
//        }
//        if (mTurnInfoCardView != exclude) {
//            mTurnInfoCardView.visibility = View.GONE
//        }
//        if (mGiveUpView != exclude) {
//            mGiveUpView.hideWithAnimation(false)
//        }
//        if (mRoundOverCardView != exclude) {
//            mRoundOverCardView.setVisibility(View.GONE)
//        }
//        if (mSingBeginTipsCardView != exclude) {
//            mSingBeginTipsCardView.setVisibility(View.GONE)
//        }
    }

    private fun initMicSeatView() {
//        mHasSelectSongNumTv = findViewById(R.id.has_select_song_num_tv)
//        mMicSeatView = MicSeatView(findViewById(R.id.mic_seat_view_layout_view_stub))
//        mMicSeatView.hasSelectSongNumTv = mHasSelectSongNumTv
//        mMicSeatView.mRoomData = mRoomData
//        mHasSelectSongNumTv.setDebounceViewClickListener {
//            mMicSeatView.show()
//        }
    }

    private fun initRelayEnergyView() {
        mRelayEnergyView = findViewById(R.id.relay_energy_view)
        mDiffuseView = findViewById(R.id.wave_view)
        mRelayEnergyView?.diffuseView = mDiffuseView
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

    private fun initRelayContinueSingView() {
        mRelayContinueSingView = findViewById(R.id.relay_continue_sing_view)
        mRelayContinueSingView?.roomData = mRoomData
        mRelayContinueSingView?.mContinueListener = {
            mCorePresenter.sendUnlock()
        }
    }


    private fun initTurnSenceView() {
//        mWaitingCardView = findViewById(R.id.wait_card_view)
//        mWaitingCardView.visibility = View.GONE
        var rootView = findViewById<View>(R.id.main_act_container)
        // 下一首
//        mTurnInfoCardView = findViewById(R.id.turn_card_view)
//        mTurnInfoCardView.visibility = View.GONE
//         演唱开始名片
//        mSingBeginTipsCardView = MicSingBeginTipsCardView(rootView)
//        // 自己演唱
//        mSelfSingCardView = SelfSingCardView(rootView)
//        mSelfSingCardView?.setListener {
//            //            removeNoAccSrollTipsView()
////            removeGrabSelfSingTipView()
//            mCorePresenter?.sendRoundOverInfo()
//        }
//        // 他人演唱
////        mSelfSingCardView?.setListener4FreeMic { mCorePresenter?.sendMyGrabOver("onSelfSingOver") }
//        mOthersSingCardView = OthersSingCardView(rootView)
//        // 结果页面
//        mRoundOverCardView = RoundOverCardView(rootView)

        // 打分
//        mGrabScoreTipsView = rootView.findViewById(R.id.grab_score_tips_view)

        relaySingCardView = RelaySingCardView(rootView.findViewById(R.id.relay_sing_card_view_layout_stub))
        relaySingCardView.setVisibility(View.VISIBLE)
        relaySingCardView.roomData = mRoomData
        relaySingCardView.othersSingCardView = NormalOthersSingCardView(rootView.findViewById(R.id.normal_other_sing_card_view_stub))
        relaySingCardView.effectBgView = GameEffectBgView(rootView.findViewById(R.id.game_effect_bg_view_layout_viewStub))
        // 打分
        mGrabScoreTipsView = rootView.findViewById(R.id.grab_score_tips_view)
    }

    private fun initBottomView() {
//        mGiveUpView = findViewById<GrabGiveupView>(R.id.give_up_view)
//        mGiveUpView.mGiveUpListener = { _ ->
//            mCorePresenter.giveUpSing {
//                mGiveUpView.hideWithAnimation(true)
//            }
//        }
        mChangeSongIv = findViewById(R.id.change_song_tv)
        mChangeSongIv.setAnimateDebounceViewClickListener {
            mCorePresenter.giveUpSing { }
        }

        mAddSongIv = findViewById(R.id.select_song_tv)
        mAddSongIv.setAnimateDebounceViewClickListener {
            if (!mRoomData.isPersonArrive()) {
                U.getToastUtil().showShort("小伙伴还没到，暂不能点歌～")
                return@setAnimateDebounceViewClickListener
            }

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

    private fun showPanelView() {
        if (mRoomData.isPersonArrive()) {
            if (mRoomData!!.realRoundInfo != null) {
                val now = mRoomData!!.realRoundInfo
                if (now != null) {
                    mGiftPanelView?.show(mRoomData.peerUser?.userInfo)
                } else {
                    mGiftPanelView?.show(null)
                }
            } else {
                mGiftPanelView?.show(null)
            }
        } else {
//            U.getToastUtil().showShort("")
        }
    }

    private fun buyFlowerFromOuter() {
        if (mRoomData.isPersonArrive()) {
            EventBus.getDefault().post(BuyGiftEvent(NormalGift.getFlower(), mRoomData.peerUser?.userInfo))
        }
    }

    private fun initTopView() {
        mExitIv = findViewById(R.id.exit_iv)
        mExitIv.setDebounceViewClickListener {
            quitGame()
        }
        mTopOpView = findViewById(R.id.top_op_view)
        mTopOpView.listener = object : RelayTopOpView.Listener {
            override fun onClickGameRule() {
                U.getKeyBoardUtils().hideSoftInputKeyBoard(this@RelayRoomActivity)
                showGameRuleDialog()
            }

            override fun onClickFeedBack() {
                U.getFragmentUtils().addFragment(
                        FragmentUtils.newAddParamsBuilder(this@RelayRoomActivity, QuickFeedbackFragment::class.java)
                                .setAddToBackStack(true)
                                .setHasAnimation(true)
                                .addDataBeforeAdd(0, QuickFeedbackFragment.FROM_RELAY_ROOM)
                                .addDataBeforeAdd(1, QuickFeedbackFragment.FEED_BACK)
                                .addDataBeforeAdd(3, mRoomData.gameId)
                                .setEnterAnim(R.anim.slide_in_bottom)
                                .setExitAnim(R.anim.slide_out_bottom)
                                .build())
            }

            override fun onClickVoiceAudition() {
                // 调音面板
                U.getKeyBoardUtils().hideSoftInputKeyBoard(this@RelayRoomActivity)
                dismissDialog()
                if (mVoiceControlPanelView == null) {
                    mVoiceControlPanelView = RelayVoiceControlPanelView(this@RelayRoomActivity)
                    mVoiceControlPanelView?.setRoomData(mRoomData)
                }
                mVoiceControlPanelView?.showByDialog()
            }

        }
        mTopContentView = findViewById(R.id.top_content_view)
        mTopContentView.roomData = mRoomData
        mTopContentView.bindData()
        mTopContentView.listener = object : RelayTopContentView.Listener {
            override fun countDownOver() {
                // 时间到了 调退出
                gameOver("countDownOver")
            }

            override fun clickArrow(open: Boolean) {
                if (open) {
                    mWidgetAnimationController.open()
                } else {
                    mWidgetAnimationController.close()
                }
            }

            override fun clickLove() {
                mCorePresenter.sendUnlock()
            }

            override fun clickInvite() {
//                U.getFragmentUtils().addFragment(FragmentUtils.newAddParamsBuilder(this@RelayRoomActivity, InviteFriendFragment2::class.java)
//                        .setAddToBackStack(true)
//                        .setHasAnimation(true)
//                        .addDataBeforeAdd(0, GameModeType.GAME_MODE_RELAY)
//                        .addDataBeforeAdd(1, mRoomData!!.gameId)
//                        .addDataBeforeAdd(2, EMsgRoomMediaType.EMR_AUDIO.value)
//                        .build())


                U.getFragmentUtils().addFragment(FragmentUtils.newAddParamsBuilder(this@RelayRoomActivity, InviteFriendFragment2::class.java)
                        .setAddToBackStack(true)
                        .setHasAnimation(true)
                        .addDataBeforeAdd(0, object : IInviteCallBack {
                            override fun getFrom(): Int {
                                return GameModeType.GAME_MODE_RELAY
                            }

                            override fun getInviteDialogText(kouling: String?): String {
                                return SkrKouLingUtils.genJoinRelayRoomText(kouling)
                            }

                            override fun getShareTitle(): String {
                                return "合唱房间已开，就等你来玩"
                            }

                            override fun getShareDes(): String {
                                return "我在撕歌skr开了一个合唱房间，快来一起耍呀～"
                            }

                            override fun getInviteObservable(model: UserInfoModel): Observable<ApiResult> {
                                MyLog.d(TAG, "inviteRelayFriend roomID=${mRoomData?.gameId} model=$model")
                                val map = HashMap<String, Any>()
                                map["roomID"] = mRoomData?.gameId
                                map["inviteUserID"] = model.userId

                                val body = RequestBody.create(MediaType.parse(ApiManager.APPLICATION_JSON), JSON.toJSONString(map))
                                return ApiManager.getInstance().createService(PartyRoomServerApi::class.java).relayRoominvite(body)
                            }

                            override fun getRoomID(): Int {
                                return mRoomData?.gameId
                            }

                            override fun getKouLingTokenObservable(): Observable<ApiResult> {
                                val code = String.format("inframeskr://room/joinrelay?owner=%s&gameId=%s&ask=1", MyUserInfoManager.uid, mRoomData.gameId)
                                return ApiManager.getInstance().createService(KouLingServerApi::class.java).setTokenByCode(code)
                            }
                        })
                        .build())
            }
        }

        mRoomInviteView = RoomInviteView(findViewById(R.id.mic_invite_view_stub))
        mRoomInviteView?.agreeInviteListener = {
            mSkrAudioPermission.ensurePermission({
                mRoomInviteView?.agreeInvite()
            }, true)
        }
    }

    private fun showGameRuleDialog() {
        dismissDialog()
        mGameRuleDialog = DialogPlus.newDialog(this)
                .setContentHolder(ViewHolder(R.layout.relay_game_rule_view_layout))
                .setContentBackgroundResource(R.color.transparent)
                .setOverlayBackgroundResource(R.color.black_trans_50)
                .setMargin(U.getDisplayUtils().dip2px(16f), -1, U.getDisplayUtils().dip2px(16f), -1)
                .setExpanded(false)
                .setGravity(Gravity.CENTER)
                .create()
        mGameRuleDialog?.show()
    }

    private fun initCommentView() {
//        mCommentView = findViewById(R.id.comment_view)
//        mCommentView.setListener(CommentViewItemListener { userId ->
//            showPersonInfoView(userId)
//        })
//        mCommentView.roomData = mRoomData
//        mVoiceRecordUiController = VoiceRecordUiController(mBottomContainerView.mVoiceRecordBtn!!, mVoiceRecordTipsView, mCommentView)
        mFlyCommentView = findViewById(R.id.fly_comment_view)
        mFlyCommentView?.roomData = mRoomData
    }

    private fun initGiftPanelView() {
        mGiftPanelView = findViewById<View>(R.id.gift_panel_view) as GiftPanelView
        mGiftPanelView.setRoomData(mRoomData)
        mContinueSendView = findViewById<View>(R.id.continue_send_view) as ContinueSendView
        mContinueSendView.mScene = ContinueSendView.EGameScene.GS_Relay
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

    override fun startEnterAnimation(playerInfoModel: UserInfoModel, finishCall: () -> Unit) {
        mVIPEnterView?.enter(playerInfoModel, finishCall)
    }

    private fun showPersonInfoView(userID: Int) {
        if (!U.getNetworkUtils().hasNetwork()) {
            U.getToastUtil().showShort("网络异常，请检查网络后重试!")
            return
        }
        dismissDialog()
        mInputContainerView.hideSoftInput()
        mPersonInfoDialog = PersonInfoDialog.Builder(this, QuickFeedbackFragment.FROM_RELAY_ROOM, userID, false, false)
                .setRoomID(mRoomData.gameId)
                .build()
        mPersonInfoDialog?.show()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: RelayLockChangeEvent) {
        if (mRoomData?.unLockMe && mRoomData?.unLockPeer) {
            mChangeSongIv.visibility = View.VISIBLE
            mAddSongIv.visibility = View.VISIBLE

            val zanView = ZanView(this)
            mMainContainerView.addView(zanView, ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

            launch {
                delay(500)
                repeat(30) {
                    delay(80)
                    zanView.addZanXin(1)
                }
                delay(8000)
                if (!isFinishing && !isDestroyed) {
                    mMainContainerView.removeView(zanView)
                }
            }

            LayoutInflater.from(this).inflate(R.layout.relay_continue_burst, mMainContainerView)
            mContinueSVGAImageView = findViewById(R.id.continue_svga)
            playContinueAnimation()
        }
    }

    // 播放声纹动画
    private fun playContinueAnimation() {
        mContinueSVGAImageView?.visibility = View.VISIBLE
        mContinueSVGAImageView?.loops = 1

        SvgaParserAdapter.parse("relay_continue_sing.svga", object : SVGAParser.ParseCompletion {
            override fun onComplete(videoItem: SVGAVideoEntity) {
                val drawable = SVGADrawable(videoItem)
                mContinueSVGAImageView?.setImageDrawable(drawable)
                mContinueSVGAImageView?.startAnimation()
            }

            override fun onError() {

            }
        })

        mContinueSVGAImageView?.callback = object : SVGACallback {
            override fun onPause() {

            }

            override fun onFinished() {
                mContinueSVGAImageView?.callback = null
                mContinueSVGAImageView?.stopAnimation(true)
                mContinueSVGAImageView?.visibility = View.GONE

                if (!isFinishing && !isDestroyed) {
                    mMainContainerView.removeView(mContinueSVGAImageView)
                }
            }

            override fun onRepeat() {

            }

            override fun onStep(i: Int, v: Double) {

            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: RReqAddMusicMsg) {
        // 请求接唱
        val micUserMusicModel = RoomInviteMusicModel.parseFromInfoPB(event.detail)
        if (micUserMusicModel.userID != MyUserInfoManager.uid.toInt()) {
            if (U.getActivityUtils().topActivity is RelayRoomActivity) {
                // 顶部是接唱放的activity
            } else {
                U.getToastUtil().showShort("对方正在向你发起一首合唱，返回查看")
            }
            mRoomInviteView?.showInvite(micUserMusicModel, mTopContentView.getViewLeft(micUserMusicModel.userID), true, GameModeType.GAME_MODE_RELAY)
        } else {
            // 启一个任务去同步
            mRoomInviteView?.startCheckSelfJob(micUserMusicModel)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: RAddMusicMsg) {
        // 接唱发起请求的结果
        val userMusicModel = RoomInviteMusicModel.parseFromInfoPB(event.detail)
        mRoomInviteView?.showInvite(userMusicModel, mTopContentView.getViewLeft(userMusicModel.userID), false, GameModeType.GAME_MODE_RELAY)
    }

//    @Subscribe(threadMode = ThreadMode.MAIN)
//    fun onEvent(event: MicWantInviteEvent) {
//        ARouter.getInstance().build(RouterConstants.ACTIVITY_INVITE_FRIEND)
//                .withInt("from", InviteFriendFragment2.FROM_MIC_ROOM)
//                .withInt("roomId", mRoomData!!.gameId)
//                .navigation()
//    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: ShowPersonCardEvent) {
        showPersonInfoView(event.uid)
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

    override fun canGoPersonPage(): Boolean {
        return false
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
        if (mRoomData.isPersonArrive() && System.currentTimeMillis() - mEnterTime < 30000 && !mRoomData.isEnterFromInvite()) {
            mTipsDialogView = TipsDialogView.Builder(this)
                    .setMessageTip("频繁秒退将会对您作出惩罚，试试再唱一会儿吧～")
                    .setConfirmTip("再唱一会")
                    .setCancelTip("退出")
                    .setConfirmBtnClickListener {
                        mTipsDialogView?.dismiss(false)
                    }
                    .setCancelBtnClickListener {
                        mTipsDialogView?.dismiss(false)
                        gameOver("Dialog1")
                    }
                    .build()
        } else {
            mTipsDialogView = TipsDialogView.Builder(this)
                    .setMessageTip("确定要退出合唱吗")
                    .setConfirmTip("确定")
                    .setCancelTip("取消")
                    .setConfirmBtnClickListener {
                        mTipsDialogView?.dismiss(false)
                        gameOver("Dialog2")
                    }
                    .setCancelBtnClickListener {
                        mTipsDialogView?.dismiss(false)
                    }
                    .build()
        }
        mTipsDialogView?.showByDialog()
    }

    private fun dismissDialog() {
//        mRaceActorPanelView?.dismiss(false)
        mPersonInfoDialog?.dismiss(false)
//        mRaceVoiceControlPanelView?.dismiss(false)
        mGameRuleDialog?.dismiss(false)
        mTipsDialogView?.dismiss(false)
//        mGrabKickDialog?.dismiss(false)
    }

    override fun showWaiting() {
        hideAllSceneView(null)
        relaySingCardView.turnNoSong()
        mChangeSongIv.visibility = View.GONE
    }

    override fun singPrepare(lastRoundInfo: RelayRoundInfoModel?, singCardShowListener: () -> Unit) {
        hideAllSceneView(null)
        relaySingCardView.turnSingPrepare()

        if (!mRoomData.hasTimeLimit()) {
            mTopContentView.launchCountDown()
        }

        singCardShowListener.invoke()

        updateChangeSongIv()
    }

    override fun singBegin() {
        hideAllSceneView(null)
        relaySingCardView.turnSingBegin()

        updateChangeSongIv()
    }

    private fun updateChangeSongIv() {
        if (mRoomData.hasTimeLimit()) {
            if (mRoomData.realRoundInfo?.music != null) {
                mChangeSongIv.visibility = View.VISIBLE
            } else {
                mChangeSongIv.visibility = View.GONE
            }
        } else if (mRoomData?.unLockMe && mRoomData?.unLockPeer) {
            mChangeSongIv.visibility = View.VISIBLE
        }
    }

    override fun turnChange() {
        hideAllSceneView(null)
        relaySingCardView.turnSingTurnChange()
    }

    override fun showRoundOver(lastRoundInfo: RelayRoundInfoModel?, continueOp: (() -> Unit)?) {
        hideAllSceneView(null)
        continueOp?.invoke()
    }

    override fun gameOver(from: String) {
        MyLog.d(TAG, "gameOver from = $from")
        if (!mRoomData.isHasExitGame) {
            ensureActivtyTop()
            mCorePresenter.exitRoom("gameOver")

            if (mRoomData.isPersonArrive()) {
//                ARouter.getInstance().build(RouterConstants.ACTIVITY_RELAY_RESULT)
//                        .withSerializable("roomData", mRoomData)
//                        .navigation()
                FlutterBoostController.openFlutterPage(this, "RelayResultPage", hashMapOf(
                        "roomId" to mRoomData.gameId,
                        "targetId" to mRoomData.peerUser?.userID!!,
                        "targetAvatar" to mRoomData.peerUser?.userInfo?.avatar!!
                ))
            }

            finish()
        }
    }

    override fun turnMyChangePrepare() {
        relaySingCardView.turnMyChangePrepare()
    }

    override fun startGameByInvite() {
        mTopContentView.bindData()
        mAddSongIv.visibility = View.VISIBLE
    }

    override fun receiveScoreEvent(score: Int) {
        mGrabScoreTipsView.updateScore(score, -1)
    }

}
