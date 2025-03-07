package com.module.playways.race.room.presenter

import android.os.Handler
import android.os.Message
import android.support.annotation.CallSuper
import android.text.TextUtils
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.common.core.account.UserAccountManager
import com.common.core.myinfo.MyUserInfoManager
import com.common.core.userinfo.model.UserInfoModel
import com.common.jiguang.JiGuangPush
import com.common.log.DebugLogView
import com.common.log.MyLog
import com.common.mvp.RxLifeCyclePresenter
import com.common.rxretrofit.ApiManager
import com.common.rxretrofit.subscribe
import com.common.statistics.StatisticsAdapter
import com.common.utils.ActivityUtils
import com.common.utils.SpanUtils
import com.common.utils.U
import com.common.videocache.MediaCacheManager
import com.component.busilib.constans.GameModeType
import com.component.lyrics.LyricAndAccMatchManager
import com.component.lyrics.utils.SongResUtils
import com.engine.EngineEvent
import com.engine.Params
import com.engine.arccloud.RecognizeConfig
import com.module.ModuleServiceManager
import com.module.common.ICallback
import com.module.playways.BuildConfig
import com.module.playways.grab.room.event.SwitchRoomEvent
import com.module.playways.pretendHeadSetSystemMsg
import com.module.playways.race.RaceRoomServerApi
import com.module.playways.race.match.model.JoinRaceRoomRspModel
import com.module.playways.race.room.RaceRoomData
import com.module.playways.race.room.event.*
import com.module.playways.race.room.model.*
import com.module.playways.race.room.ui.IRaceRoomView
import com.module.playways.room.gift.event.GiftBrushMsgEvent
import com.module.playways.room.gift.event.UpdateCoinEvent
import com.module.playways.room.gift.event.UpdateMeiliEvent
import com.module.playways.room.msg.event.GiftPresentEvent
import com.module.playways.room.msg.filter.PushMsgFilter
import com.module.playways.room.msg.manager.RaceRoomMsgManager
import com.module.playways.room.room.comment.model.CommentModel
import com.module.playways.room.room.comment.model.CommentSysModel
import com.module.playways.room.room.comment.model.CommentTextModel
import com.module.playways.room.room.event.PretendCommentMsgEvent
import com.module.playways.room.song.model.SongModel
import com.module.playways.songmanager.event.MuteAllVoiceEvent
import com.zq.live.proto.RaceRoom.*
import com.zq.mediaengine.kit.ZqEngineKit
import kotlinx.coroutines.*
import okhttp3.MediaType
import okhttp3.RequestBody
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.Runnable
import java.util.*

class RaceCorePresenter(var mRoomData: RaceRoomData, var mIRaceRoomView: IRaceRoomView) : RxLifeCyclePresenter() {

    val raceRoomServerApi = ApiManager.getInstance().createService(RaceRoomServerApi::class.java)

    internal val MSG_ENSURE_IN_RC_ROOM = 9// 确保在融云的聊天室，保证融云的长链接

    internal val MSG_ENSURE_SWITCH_BROADCAST_SUCCESS = 21 // 确保用户切换成主播成功，防止引擎不回调的保护

    internal var mUiHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                MSG_ENSURE_IN_RC_ROOM -> {
                    DebugLogView.println(TAG, "handleMessage 长时间没收到push，重新进入融云房间容错")
                    ModuleServiceManager.getInstance().msgService.leaveChatRoom(mRoomData.gameId.toString() + "")
                    joinRcRoom(0)
                    ensureInRcRoom()
                }
                MSG_ENSURE_SWITCH_BROADCAST_SUCCESS -> {
                    onChangeBroadcastSuccess()
                }
            }
        }
    }

    internal var mPushMsgFilter: PushMsgFilter<*> = PushMsgFilter<RaceRoomMsg> { msg ->
        val b = msg != null && msg.roomID == mRoomData.gameId
        b
    }

    init {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        //添加房间消息过滤器
        RaceRoomMsgManager.addFilter(mPushMsgFilter)
        pretendEnterRoomMsg()
        pretendHeadSetSystemMsg(mRoomData.gameType)

        joinRoomAndInit(true)
    }

    private fun pretendEnterRoomMsg() {
        val commentSysModel = CommentSysModel(GameModeType.GAME_MODE_RACE, "欢迎加入撕歌排位赛，撕歌倡导文明竞演、理性投票，如遇恶意玩家请点击头像进行举报")
        EventBus.getDefault().post(PretendCommentMsgEvent(commentSysModel))
    }

//    private fun pretendHeadSetSystemMsg() {
//        val stringBuilder = SpanUtils()
//                .append(" 温馨提示：佩戴耳机能获得最佳演唱效果").setForegroundColor(CommentModel.RANK_SYSTEM_COLOR)
//                .create()
//        val commentSysModel = CommentSysModel(mRoomData.gameType, stringBuilder)
//        EventBus.getDefault().post(PretendCommentMsgEvent(commentSysModel))
//    }

    /**
     * 加入引擎房间
     * 加入融云房间
     * 系统消息弹幕
     */
    private fun joinRoomAndInit(first: Boolean) {
        DebugLogView.println(TAG, "joinRoomAndInit" + " first=" + first + ", gameId is " + mRoomData.gameId)

        if (mRoomData.gameId > 0) {
            var reInit = false
            if (first) {
                reInit = true
            }
            if (reInit) {
                val params = Params.getFromPref().apply {
                    scene = Params.Scene.grab
                    isEnableAudio = true
                    isEnableVideo = false
                }
                ZqEngineKit.getInstance().init("raceroom", params)
            }
            ZqEngineKit.getInstance().joinRoom(mRoomData.gameId.toString(), UserAccountManager.uuidAsLong.toInt(), false, mRoomData.agoraToken)
            // 不发送本地音频, 会造成第一次抢没声音
            ZqEngineKit.getInstance().muteLocalAudioStream(true)
        }
        joinRcRoom(-1)
        if (mRoomData.gameId > 0) {
            for (playerInfoModel in mRoomData.getPlayerAndWaiterInfoList()) {
                if (!playerInfoModel.isOnline) {
                    continue
                }
                pretendEnterRoom(playerInfoModel)
            }
//            pretendRoomNameSystemMsg(mRoomData.getRoomName(), CommentSysModel.TYPE_ENTER_ROOM)
        }
        startHeartbeat()
        startSyncRaceStatus()
    }

    private fun joinRcRoom(deep: Int) {
        if (deep > 4) {
            MyLog.d(TAG, "加入融云房间，重试5次仍然失败，放弃")
            return
        }
        if (mRoomData.gameId > 0) {
            ModuleServiceManager.getInstance().msgService.joinChatRoom(mRoomData.gameId.toString(), -1, object : ICallback {

                override fun onSucess(obj: Any?) {
                    DebugLogView.println(TAG, "加入融云房间成功")
                }

                override fun onFailed(obj: Any?, errcode: Int, message: String?) {
                    MyLog.d(TAG, "加入融云房间失败， msg is $message, errcode is $errcode")
                    joinRcRoom(deep + 1)
                }
            })
            if (deep == -1) {
                /**
                 * 说明是初始化时那次加入房间，这时加入极光房间做个备份，使用tag的方案
                 */
                JiGuangPush.joinSkrRoomId(mRoomData.gameId.toString())
            }
        }
    }

    private fun ensureInRcRoom() {
        mUiHandler.removeMessages(MSG_ENSURE_IN_RC_ROOM)
        mUiHandler.sendEmptyMessageDelayed(MSG_ENSURE_IN_RC_ROOM, (30 * 1000).toLong())
    }

    /**
     * 开场动画播放完毕
     */
    fun onOpeningAnimationOver() {
        mRoomData.checkRoundInEachMode()
        ensureInRcRoom()
    }


    /**
     * 相当于告知服务器，我不抢
     */
    fun sendIntroOver() {
        launch {
            val map = mutableMapOf(
                    "roomID" to mRoomData.gameId,
                    "roundSeq" to mRoomData.realRoundSeq
            )
            val body = RequestBody.create(MediaType.parse(ApiManager.APPLICATION_JSON), JSON.toJSONString(map))
            val result = subscribe { raceRoomServerApi.introOver(body) }
            if (result.errno == 0) {

            } else {

            }
        }
    }

    /**
     * 选择歌曲
     * /** 报名，选择歌曲
     * {
    "itemID": 0,
    "curRoundSeq": 0,
    "curRoundStatus": "ERRS_UNKNOWN",
    "roomID": 0,
    "wantSingType": "ERWST_DEFAULT"
    }
    */
     */
    fun wantSingChance(itemID: Int, songModel: SongModel?) {
        var wantSingType = ERWantSingType.ERWST_DEFAULT.value

        if (mRoomData.isAccEnable && (songModel?.acc?.isNotBlank() == true)) {
            wantSingType = ERWantSingType.ERWST_ACCOMPANY.value
        }

        launch {
            val map = mutableMapOf(
                    "curRoundSeq" to mRoomData.realRoundSeq,
                    "curRoundStatus" to mRoomData.realRoundInfo?.status,
                    "itemID" to itemID,
                    "itemType" to 1,
                    "roomID" to mRoomData.gameId,
                    "wantSingType" to wantSingType
            )
            val body = RequestBody.create(MediaType.parse(ApiManager.APPLICATION_JSON), JSON.toJSONString(map))
            val result = subscribe { raceRoomServerApi.singMakeChoice(body) }
            if (result.errno == 0) {
                mRoomData.hasSignUpSelf = true
                EventBus.getDefault().post(RaceWantSingChanceEvent(songModel!!))
                //mRoomData?.realRoundInfo?.addWantSingChange(itemID, MyUserInfoManager.getInstance().uid.toInt())
            } else {
                MyLog.w(TAG, "wantSingChance errno is " + result.errno)
            }
        }
    }

    /**
     * 爆灯&投票
     */
    fun sendBLight(callback: ((isSucess: Boolean) -> Unit)?) {
        launch {
            val map = mutableMapOf(
                    "roomID" to mRoomData.gameId,
                    "roundSeq" to mRoomData.realRoundSeq,
                    "subRoundSeq" to mRoomData.realRoundInfo?.subRoundSeq
            )

            val roundSeq = mRoomData.realRoundSeq
            val subRoundSeq = mRoomData.realRoundInfo?.subRoundSeq ?: 0
            val userID = mRoomData?.realRoundInfo?.getSingerIdNow()

            val body = RequestBody.create(MediaType.parse(ApiManager.APPLICATION_JSON), JSON.toJSONString(map))
            val result = subscribe { raceRoomServerApi.bLight(body) }
            if (result.errno == 0) {
                userID?.let {
                    mRoomData?.realRoundInfo?.unfakeSetForMe?.add(it)
                    EventBus.getDefault().post(RaceBlightByMeEvent(it))
                }

                if (roundSeq == mRoomData.realRoundSeq && subRoundSeq == mRoomData.realRoundInfo?.subRoundSeq) {
                    callback?.invoke(true)
                }
            } else {
                if (result.errno == 8412159) {
                    // 已经投过票
                    U.getToastUtil().showShort(result.errmsg)
                    callback?.invoke(true)
                } else {
                    callback?.invoke(false)
                }
            }
        }
    }

    /**
     * 放弃演唱
     */
    fun giveupSing(callback: ((isSucess: Boolean) -> Unit)?) {
        launch {
            val map = mutableMapOf(
                    "roomID" to mRoomData.gameId,
                    "roundSeq" to mRoomData.realRoundSeq,
                    "subRoundSeq" to mRoomData.realRoundInfo?.subRoundSeq
            )
            val body = RequestBody.create(MediaType.parse(ApiManager.APPLICATION_JSON), JSON.toJSONString(map))
            val result = subscribe { raceRoomServerApi.giveup(body) }
            if (result.errno == 0) {
                callback?.invoke(true)
            } else {
                callback?.invoke(false)
            }
        }
    }

    /**
     * 主动告诉服务器我演唱完毕
     */
    fun sendSingComplete(from: String) {
        MyLog.d(TAG, "sendSingComplete from = $from")
        launch {
            val map = mutableMapOf(
                    "roomID" to mRoomData.gameId,
                    "roundSeq" to mRoomData.realRoundSeq,
                    "subRoundSeq" to mRoomData.realRoundInfo?.subRoundSeq
            )
            val body = RequestBody.create(MediaType.parse(ApiManager.APPLICATION_JSON), JSON.toJSONString(map))
            val result = subscribe { raceRoomServerApi.roundOver(body) }
            if (result.errno == 0) {

            } else {

            }
        }
    }

    fun exitRoom(from: String) {
        MyLog.d("RaceCorePresenter", "exitRoom from = $from")
        mRoomData.isHasExitGame = true
        GlobalScope.launch {
            val map = mutableMapOf(
                    "roomID" to mRoomData.gameId
            )
            val body = RequestBody.create(MediaType.parse(ApiManager.APPLICATION_JSON), JSON.toJSONString(map))
            subscribe {
                if (mRoomData.audience) {
                    raceRoomServerApi.audienceExitRoom(body)
                } else {
                    raceRoomServerApi.exitRoom(body)
                }
            }
        }
    }

    private fun goResultPage(lastRound: RaceRoundInfoModel) {
        exitRoom("goResultPage")
        mIRaceRoomView.goResultPage(lastRound)
    }

    var heartbeatJob: Job? = null

    private fun startHeartbeat() {
        //观众不需要心跳
        if (mRoomData.audience == false) {
            heartbeatJob?.cancel()
            heartbeatJob = launch {
                while (true) {
                    val map = mutableMapOf(
                            "roomID" to mRoomData.gameId,
                            "userID" to MyUserInfoManager.uid
                    )
                    val body = RequestBody.create(MediaType.parse(ApiManager.APPLICATION_JSON), JSON.toJSONString(map))
                    val result = subscribe { raceRoomServerApi.heartbeat(body) }
                    if (result.errno == 0) {

                    } else {

                    }
                    delay(60 * 1000)
                }
            }
        }
    }

    /**
     * 如果确定是自己唱了,预先可以做的操作
     */
    private fun preOpWhenSelfRound() {
        var needAcc = false
        val songModel = mRoomData?.realRoundInfo?.getSongModelNow()
        if (mRoomData.isAccEnable && mRoomData?.realRoundInfo?.isAccRoundNow() == true) {
            needAcc = true
        }

        val p = ZqEngineKit.getInstance().params
        p.isGrabSingNoAcc = !needAcc
        if (!ZqEngineKit.getInstance().params.isAnchor) {
            ZqEngineKit.getInstance().setClientRole(true)
            ZqEngineKit.getInstance().muteLocalAudioStream(false)
            if (needAcc) {
                // 如果需要播放伴奏，一定要在角色切换成功才能播
                mUiHandler.removeMessages(MSG_ENSURE_SWITCH_BROADCAST_SUCCESS)
                mUiHandler.sendEmptyMessageDelayed(MSG_ENSURE_SWITCH_BROADCAST_SUCCESS, 2000)
            }
        } else {
            // 如果是房主,不在这里 解禁，会录进去音效的声音 延后一些再解开
            mUiHandler.postDelayed({
                ZqEngineKit.getInstance().muteLocalAudioStream(false)
                onChangeBroadcastSuccess()
            }, 500)
        }

//        songModel?.let {
//            // 开始acr打分
//            if (ScoreConfig.isAcrEnable()) {
//                if (needAcc) {
//                    ZqEngineKit.getInstance().startRecognize(RecognizeConfig.newBuilder()
//                            .setSongName(it.itemName)
//                            .setArtist(it.owner)
//                            .setMode(RecognizeConfig.MODE_MANUAL)
//                            .build())
//                } else {
//                    if (needScore) {
//                        // 清唱还需要打分，那就只用 acr 自动打分
//                        ZqEngineKit.getInstance().startRecognize(RecognizeConfig.newBuilder()
//                                .setSongName(it.itemName)
//                                .setArtist(it.owner)
//                                .setMode(RecognizeConfig.MODE_AUTO)
//                                .setAutoTimes(3)
//                                .setMResultListener { result, list, targetSongInfo, lineNo ->
//                                    var mAcrScore = 0
//                                    if (targetSongInfo != null) {
//                                        mAcrScore = (targetSongInfo.score * 100).toInt()
//                                    }
//                                    EventBus.getDefault().post(LyricAndAccMatchManager.ScoreResultEvent("preOpWhenSelfRound", -1, mAcrScore, 0))
//                                }
//                                .build())
//                    } else {
//
//                    }
//                }
//            }
//        }
    }

    private fun closeEngine() {
        if (mRoomData.gameId > 0) {
            ZqEngineKit.getInstance().stopAudioMixing()
            ZqEngineKit.getInstance().stopAudioRecording()
            if (ZqEngineKit.getInstance().params.isAnchor) {
                ZqEngineKit.getInstance().setClientRole(false)
            }
        }
    }

    private var mSwitchRooming = false

    fun changeRoomForAudience() {
        if (mSwitchRooming) {
            U.getToastUtil().showShort("切换中")
            return
        }
        //        if(true){
        //            stopGuide();
        //            mRoomData.setRealRoundInfo(null);
        //            mIGrabView.hideAllCardView();
        //            joinRoomAndInit(false);
        //            ZqEngineKit.getInstance().unbindAllRemoteVideo();
        //            mRoomData.checkRoundInEachMode();
        //            mIGrabView.onChangeRoomResult(true, null);
        //            mIGrabView.dimissKickDialog();
        //            return;
        //        }
        mSwitchRooming = true
        launch {
            val map = mutableMapOf("roomID" to mRoomData.gameId)
            val body = RequestBody.create(MediaType.parse(ApiManager.APPLICATION_JSON), JSON.toJSONString(map))
            val result = subscribe {
                raceRoomServerApi.audienceChangeRoom(body)
            }
            if (result.errno == 0) {
                val rspModel = JSON.parseObject(result.data!!.toJSONString(), JoinRaceRoomRspModel::class.java)
                onChangeRoomSuccess(rspModel)
            } else {
                mIRaceRoomView.onChangeRoomResult(false, result.errmsg)
            }
            mSwitchRooming = false
        }
    }

    private fun onChangeRoomSuccess(rspModel: JoinRaceRoomRspModel?) {
        MyLog.d(TAG, "onChangeRoomSuccess joinGrabRoomRspModel=$rspModel")
        if (rspModel != null) {
            EventBus.getDefault().post(SwitchRoomEvent())
            mRoomData.loadFromRsp(rspModel)
            joinRoomAndInit(false)
            mIRaceRoomView.onChangeRoomResult(true, null)
            mRoomData.checkRoundInEachMode()
        }
    }


    /**
     * 轮次切换事件

     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: RaceRoundChangeEvent) {
        MyLog.d(TAG, "onRaceRoundChangeEvent lastRound = ${event.lastRound}")
        MyLog.d(TAG, "onRaceRoundChangeEvent thisRound = ${event.thisRound}")
        if (event.lastRound != null) {
            DebugLogView.println(TAG, "上一轮结果 overReason = ${event.lastRound?.overReason} " +
                    "subReason1 = ${event.lastRound?.subRoundInfo.getOrNull(0)?.overReason} " +
                    "subReason2 = ${event.lastRound?.subRoundInfo.getOrNull(1)?.overReason} " +
                    "票数 ${event.lastRound?.scores.getOrNull(0)?.bLightCnt}:${event.lastRound?.scores.getOrNull(1)?.bLightCnt} " +
                    "win ${event.lastRound?.scores.getOrNull(0)?.winType}:${event.lastRound?.scores.getOrNull(1)?.winType}")

        }
        DebugLogView.println(TAG, "新一轮 roundSeq=${event.thisRound?.roundSeq}")
        processStatusChange(1, event.lastRound, event.thisRound)
    }

    /**
     * 轮次内 状态切换事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: RaceRoundStatusChangeEvent) {
        MyLog.d(TAG, "onRaceRoundStatusChangeEvent = $event")
        processStatusChange(2, null, event.thisRound)
    }

    /**
     * 轮次内 演唱阶段子轮次切换事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: RaceSubRoundChangeEvent) {
        MyLog.d(TAG, "onRaceSubRoundChangeEvent = $event")
        processStatusChange(3, null, event.thisRound)
    }

    private fun processStatusChange(from: Int, lastRound: RaceRoundInfoModel?, thisRound: RaceRoundInfoModel?) {
        DebugLogView.println(TAG, "状态更新 from = $from, status = ${thisRound?.status} subRoundSeq = ${thisRound?.subRoundSeq}")
        mUiHandler.removeMessages(MSG_ENSURE_SWITCH_BROADCAST_SUCCESS)
        closeEngine()
        ZqEngineKit.getInstance().stopRecognize()
        if (thisRound == null) {
            MyLog.d(TAG, "this round is null")
            // 游戏结束了
            mIRaceRoomView.gameOver(lastRound)
            if (mRoomData.audience) {
                U.getToastUtil().showShort("排位房间已解散")
            }
            return
        }

        if (thisRound?.status == ERaceRoundStatus.ERRS_WAITING.value) {
            if (lastRound != null) {
                // 有上一轮，等待中要飞过来
                mIRaceRoomView.showRoundOver(lastRound) {
                    //如果我是上一轮的演唱者，要退出房间
                    if (lastRound.isSingerByUserId(MyUserInfoManager.uid.toInt())) {
                        goResultPage(lastRound)
                    } else {
                        mIRaceRoomView.showWaiting(true)
                    }
                }
            } else {
                mIRaceRoomView.showWaiting(false)
            }
        } else if (thisRound?.status == ERaceRoundStatus.ERRS_CHOCING.value) {
            if (lastRound != null) {
                // 有上一轮，等待中要飞过来
                mIRaceRoomView.showRoundOver(lastRound) {
                    //如果我是上一轮的演唱者，要退出房间
                    if (lastRound.isSingerByUserId(MyUserInfoManager.uid.toInt())) {
                        goResultPage(lastRound)
                    } else {
                        mIRaceRoomView.showChoiceView(true)
                    }
                }
            } else {
                mIRaceRoomView.showChoiceView(false)
            }
        } else if (thisRound?.status == ERaceRoundStatus.ERRS_ONGOINE.value) {
            if (thisRound?.subRoundSeq == 1) {
                val runnable = {
                    tryDownloadAccIfSelfSing()
                    // 变为演唱阶段，第一轮
                    val subRound1 = thisRound.subRoundInfo.get(0)
                    if (subRound1.userID == MyUserInfoManager.uid.toInt()) {
                        mIRaceRoomView.singBySelfFirstRound(subRound1.choiceDetail?.commonMusic)
                        preOpWhenSelfRound()
                    } else {
                        mIRaceRoomView.singByOtherFirstRound(subRound1.choiceDetail?.commonMusic, mRoomData.getPlayerOrWaiterInfo(subRound1.userID))
                    }
                }

                if (lastRound != null) {
                    // 说明是直接上一轮跳到本轮的演唱阶段了
                    mIRaceRoomView.showRoundOver(lastRound) {
                        //如果我是上一轮的演唱者，要退出房间
                        if (lastRound.isSingerByUserId(MyUserInfoManager.uid.toInt())) {
                            goResultPage(lastRound)
                        } else {
                            // 走匹配动画
                            mIRaceRoomView.showMatchAnimationView {
                                // 动画走完执行唱歌相关
                                runnable.invoke()
                            }
                        }
                    }
                } else {
                    if (mRoomData.realRoundInfo?.enterStatus == ERaceRoundStatus.ERRS_ONGOINE.value) {
                        //中途进来的
                        runnable.invoke()
                    } else {
                        // 正常从报名阶段 到 演唱阶段
                        // 走匹配动画
                        mIRaceRoomView.showMatchAnimationView {
                            // 动画走完执行唱歌相关
                            runnable.invoke()
                        }
                    }
                }
            } else if (thisRound?.subRoundSeq == 2) {
                // 变为演唱阶段，第二轮
                val subRound2 = thisRound.subRoundInfo.get(1)
                if (subRound2.userID == MyUserInfoManager.uid.toInt()) {
                    mIRaceRoomView.singBySelfSecondRound(subRound2.choiceDetail?.commonMusic)
                    preOpWhenSelfRound()
                } else {
                    mIRaceRoomView.singByOtherSecondRound(subRound2.choiceDetail?.commonMusic, mRoomData.getPlayerOrWaiterInfo(subRound2.userID))
                }
            }
        } else if (thisRound?.status == ERaceRoundStatus.ERRS_END.value) {
            // 结束
//            mIRaceRoomView.roundOver(thisRound?.overReason)
        }
    }

    private fun tryDownloadAccIfSelfSing() {
        /**
         * 有的网络伴奏在线播有问题，比如公司网络，这里尝试提前下载一下伴奏
         * 下载第一轮伴奏来不及，在第一轮下载第二轮伴奏才差不多
         */
        var accUrl: String? = null
//        if (mRoomData?.realRoundInfo?.subRoundInfo?.getOrNull(0)?.userID == MyUserInfoManager.getInstance().uid.toInt()) {
//            // 第一轮是自己唱
//            if (mRoomData?.realRoundInfo?.isAccRoundBySubRoundSeq(1) == true) {
//                // 第一轮是伴奏演唱
//                mRoomData?.realRoundInfo?.subRoundInfo?.getOrNull(0)?.choiceID?.let {
//                    val songModel = mRoomData?.realRoundInfo?.getSongModelByChoiceId(it)
//                    songModel?.acc?.let {
//                        accUrl = it
//                    }
//                }
//            }
//        }
        if (mRoomData?.realRoundInfo?.subRoundInfo?.getOrNull(1)?.userID == MyUserInfoManager.uid.toInt()) {
            // 第一轮是自己唱
            if (mRoomData?.realRoundInfo?.isAccRoundBySubRoundSeq(2) == true) {
                // 第一轮是伴奏演唱
                mRoomData?.realRoundInfo?.subRoundInfo?.getOrNull(1)?.choiceDetail?.commonMusic?.let { songModel ->
                    songModel?.acc?.let {
                        accUrl = it
                    }
                }
            }
        }
        accUrl?.let {
            MediaCacheManager.preCache(it)
        }

    }

    /**
     * 用户选择了某个歌曲
     */
    @Subscribe(threadMode = ThreadMode.POSTING)
    fun onEvent(event: RWantSingChanceMsg) {
        MyLog.d(TAG, "onEvent event = ${event}")
        ensureInRcRoom()
        if (event.roundSeq == mRoomData.realRoundSeq) {
            //mRoomData?.realRoundInfo?.addWantSingChange(event.pb.choiceID, event.pb.userID)
        }
    }

    /**
     * 用户得到了演唱机会
     */
    @Subscribe(threadMode = ThreadMode.POSTING)
    fun onEvent(event: RGetSingChanceMsg) {
        DebugLogView.println(TAG, "RGetSingChanceEvent 确定演唱者 ${event.currentRound.subRoundInfoList.getOrNull(0)?.userID} pk ${event.currentRound.subRoundInfoList.getOrNull(1)?.userID}")
        ensureInRcRoom()
        val roundInfoModel = parseFromRoundInfoPB(event.currentRound)
        if (roundInfoModel.roundSeq == mRoomData.realRoundSeq) {
            // 轮次符合，子轮次信息应该都有了
            mRoomData.realRoundInfo?.tryUpdateRoundInfoModel(roundInfoModel, true)
        }
    }

    /**
     * 有人加入了房间
     */
    @Subscribe(threadMode = ThreadMode.POSTING)
    fun onEvent(event: RJoinNoticeMsg) {
        DebugLogView.println(TAG, "RJoinNoticeEvent ${event.userInfo.userID} 加入房间 角色为${event.role}")
        ensureInRcRoom()
        val racePlayerInfoModel = RacePlayerInfoModel()
        racePlayerInfoModel.userInfo = UserInfoModel.parseFromPB(event.userInfo)
        racePlayerInfoModel.role = event.role.value
        racePlayerInfoModel.fakeUserInfo = FakeUserInfoModel.parseFromPB(event.fakeUserInfo)
        if (event.role != ERUserRole.ERUR_AUDIENCE) {
            mRoomData.realRoundInfo?.joinUser(racePlayerInfoModel)
            if (event.newRoundBegin) {
                // 游戏开始了
                if (mRoomData.realRoundInfo?.status == ERaceRoundStatus.ERRS_WAITING.value) {
                    mRoomData.realRoundInfo?.updateStatus(true, ERaceRoundStatus.ERRS_CHOCING.value)
                }
            }
        } else {
            mRoomData.realRoundInfo?.let {
                it.audienceUserCnt++
                EventBus.getDefault().post(UpdateAudienceCountEvent(it.audienceUserCnt))
            }
        }
        if (event.userInfo.userID != MyUserInfoManager.uid.toInt()) {
            // 进房的是自己 不处理
            pretendEnterRoom(racePlayerInfoModel)
        }
        mIRaceRoomView.joinNotice(UserInfoModel.parseFromPB(event.userInfo))
    }

    /**
     * 有人退出了房间
     */
    @Subscribe(threadMode = ThreadMode.POSTING)
    fun onEvent(event: RExitGameMsg) {
        DebugLogView.println(TAG, "RExitGameEvent ${event.userID} 退出房间")
        ensureInRcRoom()
        if (event.role != ERUserRole.ERUR_AUDIENCE) {
            mRoomData.realRoundInfo?.exitUser(event.userID)
        } else {
            mRoomData.realRoundInfo?.let {
                if (it.audienceUserCnt > 0) {
                    it.audienceUserCnt--
                    EventBus.getDefault().post(UpdateAudienceCountEvent(it.audienceUserCnt))
                }
            }
        }
    }

    /**
     * 用户爆灯投票
     */
    @Subscribe(threadMode = ThreadMode.POSTING)
    fun onEvent(event: RBLightMsg) {
        DebugLogView.println(TAG, "RBLightEvent ${event.userID} 爆灯")
        ensureInRcRoom()
        MyLog.d(TAG, "onEvent event = $event")
        if (event.roundSeq == mRoomData.realRoundSeq) {
            mRoomData.realRoundInfo?.addBLightUser(true, event.userID, event.subRoundSeq, event.bLightCnt)
            if (event.userID == UserAccountManager.SYSTEM_GRAB_ID ||
                    event.userID == UserAccountManager.SYSTEM_RANK_AI) {
                //TODO  投票打分提示
                pretendVote(mRoomData.getPlayerOrWaiterInfo(event.userID), mRoomData.getPlayerOrWaiterInfo(mRoomData.realRoundInfo?.getSingerIdNow()))
            }
        }
    }

    private fun pretendVote(userInfoModel: UserInfoModel?, singer: UserInfoModel?) {
        if (userInfoModel != null && singer != null) {
            val commentModel = CommentTextModel()
            commentModel.avatarColor = CommentModel.AVATAR_COLOR
            commentModel.userInfo = userInfoModel
            commentModel.fakeUserInfo = mRoomData.getFakeInfo(userInfoModel.userId)
            if (commentModel.fakeUserInfo == null) {
                // 观众，那我们构造一个fakeUserInfo
                val fakeUserInfoModel = FakeUserInfoModel().apply {
                    nickName = "【观众】${commentModel.userInfo?.nicknameRemark}"
                }
                commentModel.fakeUserInfo = fakeUserInfoModel
            }
            commentModel.isFake = mRoomData.isFakeForMe(userInfoModel.userId)

            val nameBuilder = SpanUtils()
                    .append((if (!TextUtils.isEmpty(commentModel.fakeUserInfo?.nickName))
                        commentModel.fakeUserInfo?.nickName else userInfoModel.nicknameRemark) + " ").setForegroundColor(CommentModel.GRAB_NAME_COLOR)
                    .create()
            commentModel.nameBuilder = nameBuilder

            val stringBuilder = SpanUtils()
                    .append("给").setForegroundColor(CommentModel.GRAB_TEXT_COLOR)
                    .append(if (TextUtils.isEmpty(mRoomData.getFakeInfo(singer.userId)?.nickName)) {
                        mRoomData.getFakeInfo(singer.userId)?.nickName
                    } else {
                        singer.nicknameRemark
                    } + "").setForegroundColor(CommentModel.GRAB_NAME_COLOR)
                    .append("投了一票").setForegroundColor(CommentModel.GRAB_TEXT_COLOR)
                    .create()
            commentModel.stringBuilder = stringBuilder

            EventBus.getDefault().post(PretendCommentMsgEvent(commentModel))
        }
    }

    /**
     * 轮次结束
     */
    @Subscribe(threadMode = ThreadMode.POSTING)
    fun onEvent(event: RaceRoundOverMsg) {
        //MyLog.d(TAG, "onEvent RaceRoundOverMsg = $event")
        ensureInRcRoom()
        if (event.overType == ERoundOverType.EROT_MAIN_ROUND_OVER) {
            DebugLogView.println(TAG, "RRoundOverEvent 主轮次结束 reason=${event.currentRound.overReason}")
            // 主轮次结束
            val curRoundInfo = parseFromRoundInfoPB(event.currentRound)
            val nextRoundInfo = parseFromRoundInfoPB(event.nextRound)
//            event.pb.gamesList.forEach {
//                nextRoundInfo.games.add(parseFromGameInfoPB(it))
//            }
//            mRoomData.couldChoiceGames.clear()
//            event.pb.couldChoiceGamesList?.forEach {
//                mRoomData.couldChoiceGames.add(parseFromGameInfoPB(it))
//            }
            if (curRoundInfo.roundSeq == mRoomData.realRoundSeq) {
                mRoomData.realRoundInfo?.tryUpdateRoundInfoModel(curRoundInfo, false)
                mRoomData.expectRoundInfo = nextRoundInfo
                mRoomData.checkRoundInEachMode()
            }
            // 第二轮次结束原因
            if (curRoundInfo.subRoundInfo.getOrNull(1)?.overReason == ESubRoundOverReason.ESROR_SELF_GIVE_UP.value) {
                pretendGiveUp(mRoomData.getPlayerOrWaiterInfo(curRoundInfo.subRoundInfo.getOrNull(1)?.userID))
            }
        } else if (event.overType == ERoundOverType.EROT_SUB_ROUND_OVER) {
            DebugLogView.println(TAG, "RRoundOverEvent 子轮次结束 reason=${event.currentRound.subRoundInfoList.getOrNull(0)?.overReason}")
            val curRoundInfo = parseFromRoundInfoPB(event.currentRound)
            mRoomData.realRoundInfo?.tryUpdateRoundInfoModel(curRoundInfo, true)
            // 第一轮次结束原因
            if (event.currentRound.subRoundInfoList.getOrNull(0)?.overReason?.value == ESubRoundOverReason.ESROR_SELF_GIVE_UP.value) {
                pretendGiveUp(mRoomData.getPlayerOrWaiterInfo(event.currentRound.subRoundInfoList.getOrNull(0)?.userID))
            }
        }
    }

    private fun pretendGiveUp(userInfoModel: UserInfoModel?) {
        if (userInfoModel != null) {
            val commentModel = CommentTextModel()
            commentModel.avatarColor = CommentModel.AVATAR_COLOR
            commentModel.userInfo = userInfoModel
            commentModel.fakeUserInfo = mRoomData.getFakeInfo(userInfoModel.userId)
            commentModel.isFake = mRoomData.isFakeForMe(userInfoModel.userId)
            if (commentModel.fakeUserInfo == null) {
                // 观众，那我们构造一个fakeUserInfo
                val fakeUserInfoModel = FakeUserInfoModel().apply {
                    nickName = "【观众】${commentModel.userInfo?.nicknameRemark}"
                }
                commentModel.fakeUserInfo = fakeUserInfoModel
            }

            var name = userInfoModel.nicknameRemark
            if (!TextUtils.isEmpty(commentModel.fakeUserInfo?.nickName)) {
                name = commentModel.fakeUserInfo?.nickName
            }
            val nameBuilder = SpanUtils()
                    .append("$name ").setForegroundColor(CommentModel.GRAB_NAME_COLOR)
                    .create()
            commentModel.nameBuilder = nameBuilder
            val stringBuilder = SpanUtils()
                    .append("$name ").setForegroundColor(CommentModel.GRAB_NAME_COLOR)
                    .append("不唱了").setForegroundColor(CommentModel.GRAB_TEXT_COLOR).create()
            commentModel.stringBuilder = stringBuilder
            EventBus.getDefault().post(PretendCommentMsgEvent(commentModel))
        }
    }

    private fun pretendEnterRoom(playerInfoModel: RacePlayerInfoModel) {
        if (playerInfoModel.role == ERUserRole.ERUR_PLAY_USER.value
                || playerInfoModel.role == ERUserRole.ERUR_WAIT_USER.value) {
            val commentModel = CommentTextModel()
            commentModel.userInfo = playerInfoModel.userInfo
            commentModel.fakeUserInfo = mRoomData.getFakeInfo(playerInfoModel.userID)
            commentModel.isFake = mRoomData.isFakeForMe(playerInfoModel.userID)
            commentModel.avatarColor = CommentModel.AVATAR_COLOR

            var name: String?
            if (playerInfoModel.role == ERUserRole.ERUR_AUDIENCE.value) {
                name = playerInfoModel.userInfo.nicknameRemark
            } else {
                name = playerInfoModel.fakeUserInfo?.nickName
            }
            val nameBuilder = SpanUtils()
                    .append("$name ").setForegroundColor(CommentModel.GRAB_NAME_COLOR)
                    .create()
            commentModel.nameBuilder = nameBuilder

            val spanUtils = SpanUtils()
                    .append("加入了房间").setForegroundColor(CommentModel.GRAB_TEXT_COLOR)
            if (BuildConfig.DEBUG) {
                spanUtils.append(" 角色为" + playerInfoModel.role)
                        .append(" 在线状态为" + playerInfoModel.isOnline)
            }
            commentModel.stringBuilder = spanUtils.create()
            EventBus.getDefault().post(PretendCommentMsgEvent(commentModel))
        }
    }


    var syncJob: Job? = null

    private fun startSyncRaceStatus() {
        syncJob?.cancel()
        syncJob = launch {
            while (true) {
                delay(10 * 1000)
                val result = subscribe { raceRoomServerApi.syncStatus(mRoomData.gameId.toLong()) }
                if (result.errno == 0) {
                    val gameOverTimeMs = result.data.getLong("gameOverTimeMs")
                    if (gameOverTimeMs > 0) {
                        DebugLogView.println(TAG, "gameOverTimeMs=${gameOverTimeMs} 游戏结束时间>0 ，游戏结束，退出房间")
                        // 游戏结束了，停服了
                        mRoomData.expectRoundInfo = null
                        mRoomData.checkRoundInEachMode()
                    } else {
                        val syncStatusTimeMs = result.data.getLong("syncStatusTimeMs")
                        if (syncStatusTimeMs > mRoomData.lastSyncTs) {
                            mRoomData.lastSyncTs = syncStatusTimeMs
                            val raceRoundInfoModel = JSON.parseObject(result.data.getString("currentRound"), RaceRoundInfoModel::class.java)
                            processSyncResult(raceRoundInfoModel)
                        }
                    }
                } else {

                }
            }
        }
    }

    /**
     * 收到服务器的push sync
     */
    @Subscribe(threadMode = ThreadMode.POSTING)
    fun onEvent(event: RSyncStatusMsg) {
        DebugLogView.println(TAG, "RSyncStatusEvent")
        ensureInRcRoom()
        syncJob?.cancel()
        startSyncRaceStatus()
        if (event.syncStatusTimeMs > mRoomData.lastSyncTs) {
            mRoomData.lastSyncTs = event.syncStatusTimeMs
            val raceRoundInfoModel = parseFromRoundInfoPB(event.currentRound)
            processSyncResult(raceRoundInfoModel)
        }
    }

    private fun processSyncResult(raceRoundInfoModel: RaceRoundInfoModel) {
        if (raceRoundInfoModel.roundSeq == mRoomData.realRoundSeq) {
            mRoomData.realRoundInfo?.tryUpdateRoundInfoModel(raceRoundInfoModel, true)
        } else if (raceRoundInfoModel.roundSeq > mRoomData.realRoundSeq) {
            MyLog.w(TAG, "sync 回来的轮次大，要替换 roundInfo 了")
            // 主轮次结束
            launch {
                if (raceRoundInfoModel.status == ERaceRoundStatus.ERRS_ONGOINE.value) {
                    var valid = true
                    raceRoundInfoModel.subRoundInfo?.getOrNull(0)?.choiceDetail?.commonMusic?.itemName?.let {
                        valid = !it.isEmpty()
                    }
                    if (!valid) {
                        // 没有有效数据，请求服务器拉取
                        val itemID1 = raceRoundInfoModel.subRoundInfo?.getOrNull(0)?.choiceDetail?.commonMusic?.itemID
                                ?: 0
                        val itemID2 = raceRoundInfoModel.subRoundInfo?.getOrNull(1)?.choiceDetail?.commonMusic?.itemID
                                ?: 0
                        if (itemID1 > 0 && itemID2 > 0) {
                            val l1 = ArrayList<JSONObject>()
                            // 点击
                            val ob1 = JSONObject()
                            ob1["itemID"] = itemID1
                            ob1["itemType"] = raceRoundInfoModel.subRoundInfo?.getOrNull(0)?.choiceDetail?.roundGameType
                            l1.add(ob1)

                            val ob2 = JSONObject()
                            ob2["itemID"] = itemID2
                            ob2["itemType"] = raceRoundInfoModel.subRoundInfo?.getOrNull(1)?.choiceDetail?.roundGameType
                            l1.add(ob2)
                            val mutableSet1 = mapOf(
                                    "choiceItems" to l1
                            )
                            val body = RequestBody.create(MediaType.parse(ApiManager.APPLICATION_JSON), JSON.toJSONString(mutableSet1))
                            // 拉取一下歌曲详情
                            val result = subscribe { raceRoomServerApi.getGameChoiceItemDetail(body) }
                            if (result.errno == 0) {
                                val choiceItems = JSON.parseArray(result.data.getString("choiceDetails"), RaceGamePlayInfo::class.java)
                                raceRoundInfoModel.subRoundInfo?.getOrNull(0)?.choiceDetail = choiceItems.getOrNull(0)
                                raceRoundInfoModel.subRoundInfo?.getOrNull(1)?.choiceDetail = choiceItems.getOrNull(1)
                            } else {

                            }
                        }
                    }
                }
                mRoomData.expectRoundInfo = raceRoundInfoModel
                mRoomData.checkRoundInEachMode()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    fun onEvent(giftPresentEvent: GiftPresentEvent) {
        MyLog.d(TAG, "onEvent giftPresentEvent=$giftPresentEvent")
        EventBus.getDefault().post(GiftBrushMsgEvent(giftPresentEvent.mGPrensentGiftMsgModel))

        if (giftPresentEvent.mGPrensentGiftMsgModel.propertyModelList != null) {
            for (property in giftPresentEvent.mGPrensentGiftMsgModel.propertyModelList) {
                if (property.userID.toLong() == MyUserInfoManager.uid) {
                    if (property.coinBalance != -1f) {
                        UpdateCoinEvent.sendEvent(property.coinBalance.toInt(), property.lastChangeMs)
                    }
                    if (property.hongZuanBalance != -1f) {
                        //mRoomData.setHzCount(property.hongZuanBalance, property.lastChangeMs)
                    }
                }
                if (property.curRoundSeqMeiliTotal > 0) {
                    // 他人的只关心魅力值的变化
                    EventBus.getDefault().post(UpdateMeiliEvent(property.userID, property.curRoundSeqMeiliTotal.toInt(), property.lastChangeMs))
                }
            }
        }

        if (giftPresentEvent.mGPrensentGiftMsgModel.receiveUserInfo.userId.toLong() == MyUserInfoManager.uid) {
            if (giftPresentEvent.mGPrensentGiftMsgModel.giftInfo.price <= 0) {
                StatisticsAdapter.recordCountEvent("race", "game_getflower", null)
            } else {
                StatisticsAdapter.recordCountEvent("race", "game_getgift", null)
            }
        }
    }

    /**
     * 游戏切后台或切回来
     *
     * @param out 切出去
     * @param in  切回来
     */
    fun swapGame(out: Boolean, inB: Boolean) {
    }

    fun muteAllRemoteAudioStreams(mute: Boolean, fromUser: Boolean) {
        if (fromUser) {
            mRoomData.isMute = mute
        }
        ZqEngineKit.getInstance().muteAllRemoteAudioStreams(mute)
        // 如果是机器人的话
        if (mute) {
            // 如果是静音
//            if (mExoPlayer != null) {
//                mExoPlayer.setMuteAudio(true)
//            }
        } else {
            // 如果打开静音
//            if (mExoPlayer != null) {
//                mExoPlayer.setMuteAudio(false)
//            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: ActivityUtils.ForeOrBackgroundChange) {
        MyLog.w(TAG, if (event.foreground) "切换到前台" else "切换到后台")
        swapGame(!event.foreground, event.foreground)
        if (event.foreground) {
            muteAllRemoteAudioStreams(mRoomData.isMute, false)
        } else {
            muteAllRemoteAudioStreams(true, false)
        }
    }

    /**
     * 引擎相关事件
     *
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.POSTING)
    fun onEvent(event: EngineEvent) {
        if (event.getType() == EngineEvent.TYPE_USER_ROLE_CHANGE) {
            val roleChangeInfo = event.getObj<EngineEvent.RoleChangeInfo>()
            if (roleChangeInfo.newRole == 1) {
                val roundInfoModel = mRoomData.realRoundInfo
                if (roundInfoModel != null && roundInfoModel.isSingerNowBySelf()) {
                    MyLog.d(TAG, "演唱环节切换主播成功")
                    onChangeBroadcastSuccess()
                }
            }
        } else {
            // 可以考虑监听下房主的说话提示 做下容错
        }
    }

    /**
     * 录制小游戏事件，防止录进去背景音
     *
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: MuteAllVoiceEvent) {
        MyLog.d(TAG, "onEvent event=$event")
        if (event.begin) {
            muteAllRemoteAudioStreams(true, false)
        } else {
            muteAllRemoteAudioStreams(mRoomData.isMute, false)
        }
    }

    /**
     * 成功切换为主播
     */
    private fun onChangeBroadcastSuccess() {
        mUiHandler.removeMessages(MSG_ENSURE_SWITCH_BROADCAST_SUCCESS)
        mUiHandler.post(Runnable {
            if (mRoomData.realRoundInfo?.isSingerNowBySelf() == false) {
                MyLog.d(TAG, "onChangeBroadcastSuccess,但已经不是你的轮次了，cancel")
                return@Runnable
            }
            var songModel = mRoomData?.realRoundInfo?.getSongModelNow()
            if (songModel == null) {
                return@Runnable
            }
            // 开始开始混伴奏，开始解除引擎mute
//            val accFile = SongResUtils.getAccFileByUrl(songModel.acc)
            // midi不需要在这下，只要下好，native就会解析，打分就能恢复
            val midiFile = SongResUtils.getMIDIFileByUrl(songModel.midi)
            MyLog.d(TAG, "onChangeBroadcastSuccess 我的演唱环节 info=${songModel.toSimpleString()} acc=${songModel.acc} midi=${songModel.midi} accRound=${mRoomData?.realRoundInfo?.isAccRoundNow()} mRoomData.isAccEnable=${mRoomData.isAccEnable}")
            val needAcc = mRoomData.isAccEnable && (mRoomData?.realRoundInfo?.isAccRoundNow() == true)
            if (needAcc) {
                // 下载midi
                if (midiFile != null && !midiFile.exists()) {
                    MyLog.d(TAG, "onChangeBroadcastSuccess 下载midi文件 url=${songModel.midi} => local=${midiFile.path}")
                    U.getHttpUtils().downloadFileAsync(songModel.midi, midiFile, true, null)
                }

                //  播放伴奏
                val songBeginTs = songModel.beginMs
//                if (accFile != null && accFile.exists()) {
//                    // 伴奏文件存在
//                    ZqEngineKit.getInstance().startAudioMixing(MyUserInfoManager.uid.toInt(), accFile.absolutePath, midiFile.absolutePath, songBeginTs.toLong(), 1)
//                } else {
                    ZqEngineKit.getInstance().startAudioMixing(MyUserInfoManager.uid.toInt(), songModel.accWithCdnInfosJson, midiFile.absolutePath, songBeginTs.toLong(), 1)
//                }
            }
            // 启动acr打分识别
            if (needAcc) {
                //有伴奏模式，手动开启acc
                ZqEngineKit.getInstance().startRecognize(RecognizeConfig.newBuilder()
                        .setSongName(songModel.itemName)
                        .setArtist(songModel.owner)
                        .setMode(RecognizeConfig.MODE_MANUAL)
                        .build())
            } else {
                // 清唱还需要打分，那就只用 acr 打分
                ZqEngineKit.getInstance().startRecognize(RecognizeConfig.newBuilder()
                        .setSongName(songModel.itemName)
                        .setArtist(songModel.owner)
                        .setMode(RecognizeConfig.MODE_AUTO)
                        .setAutoTimes(4)
                        .setMResultListener { result, list, targetSongInfo, lineNo ->
                            var mAcrScore = 0
                            if (targetSongInfo != null) {
                                mAcrScore = (targetSongInfo.score * 100).toInt()
                            }
                            EventBus.getDefault().post(LyricAndAccMatchManager.ScoreResultEvent("onChangeBroadcastSuccess", -1, mAcrScore, 0))
                        }
                        .build())
            }
        })
    }

    /*打分相关*/

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: LyricAndAccMatchManager.ScoreResultEvent) {
        MyLog.d(TAG, "onEvent 得分 event = $event")
        val line = event.line
        val acrScore = event.acrScore
        val melpScore = event.melpScore
        val from = event.from
        if (acrScore > melpScore) {
            processScore(acrScore, line)
        } else {
            processScore(melpScore, line)
        }
    }

    internal fun processScore(score: Int, line: Int) {
        if (score < 0) {
            return
        }
        //打分传给服务器
        if (mRoomData.realRoundInfo?.isSingerNowBySelf() == true) {
            sendScoreToServer(score, line)
        }
    }

    /**
     * 单句打分上报,在排位赛模式上报
     *
     * @param score
     * @param line
     */
    private fun sendScoreToServer(score: Int, line: Int) {
        //score = (int) (Math.mRandom()*100);
        val map = HashMap<String, Any>()
        val infoModel = mRoomData.realRoundInfo ?: return
        map["userID"] = MyUserInfoManager.uid

        var itemID = mRoomData.realRoundInfo?.getSingerIdNow() ?: 0

        map["itemID"] = itemID
        map["score"] = score
        map["no"] = line
        map["gameID"] = mRoomData.gameId
        map["mainLevel"] = 0
        map["singSecond"] = 0
        val roundSeq = infoModel.roundSeq
        map["roundSeq"] = roundSeq
        val nowTs = System.currentTimeMillis()
        map["timeMs"] = nowTs


        val sb = StringBuilder()
        sb.append("skrer")
                .append("|").append(MyUserInfoManager.uid)
                .append("|").append(itemID)
                .append("|").append(score)
                .append("|").append(line)
                .append("|").append(mRoomData.gameId)
                .append("|").append(0)
                .append("|").append(0)
                .append("|").append(roundSeq)
                .append("|").append(nowTs)
        map["sign"] = U.getMD5Utils().MD5_32(sb.toString())
        launch {
            val body = RequestBody.create(MediaType.parse(ApiManager.APPLICATION_JSON), JSON.toJSONString(map))
            val result = subscribe { raceRoomServerApi.sendPkPerSegmentResult(body) }
            if (result.errno == 0) {
                // TODO: 2018/12/13  当前postman返回的为空 待补充
                MyLog.w(TAG, "单句打分上报成功")
            } else {
                MyLog.w(TAG, "单句打分上报失败" + result.errno)
            }
        }
    }

    @CallSuper
    override fun destroy() {
        super.destroy()
        if (!mRoomData.isHasExitGame) {
            exitRoom("destroy")
        }
        mUiHandler.removeCallbacksAndMessages(null)
        Params.save2Pref(ZqEngineKit.getInstance().params)
        ZqEngineKit.getInstance().destroy("raceroom")
        RaceRoomMsgManager.removeFilter(mPushMsgFilter)
        ModuleServiceManager.getInstance().msgService.leaveChatRoom(mRoomData.gameId.toString())
        JiGuangPush.exitSkrRoomId(mRoomData.gameId.toString())
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }

}