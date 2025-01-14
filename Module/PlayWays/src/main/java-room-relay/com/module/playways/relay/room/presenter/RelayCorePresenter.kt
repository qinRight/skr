package com.module.playways.relay.room.presenter

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.common.core.account.UserAccountManager
import com.common.core.myinfo.MyUserInfoManager
import com.common.jiguang.JiGuangPush
import com.common.log.DebugLogView
import com.common.log.MyLog
import com.common.mvp.RxLifeCyclePresenter
import com.common.notification.event.CNRelayEnterFromRoomInviteNotifyEvent
import com.common.rxretrofit.*
import com.common.statistics.StatisticsAdapter
import com.common.utils.ActivityUtils
import com.common.utils.U
import com.component.lyrics.LyricAndAccMatchManager
import com.component.lyrics.utils.SongResUtils
import com.component.notification.PartyPeerAccStatusEvent
import com.engine.EngineEvent
import com.engine.Params
import com.module.ModuleServiceManager
import com.module.common.ICallback
import com.module.msg.CustomMsgType
import com.module.playways.pretendHeadSetSystemMsg
import com.module.playways.relay.match.model.JoinRelayRoomRspModel
import com.module.playways.relay.room.RelayRoomActivity
import com.module.playways.relay.room.RelayRoomData
import com.module.playways.relay.room.RelayRoomServerApi
import com.module.playways.relay.room.event.RelayRoundChangeEvent
import com.module.playways.relay.room.event.RelayRoundStatusChangeEvent
import com.module.playways.relay.room.model.RelayRoundInfoModel
import com.module.playways.relay.room.ui.IRelayRoomView
import com.module.playways.room.gift.event.GiftBrushMsgEvent
import com.module.playways.room.gift.event.UpdateCoinEvent
import com.module.playways.room.gift.event.UpdateMeiliEvent
import com.module.playways.room.msg.event.GiftPresentEvent
import com.module.playways.room.msg.event.MachineScoreEvent
import com.module.playways.room.msg.filter.PushMsgFilter
import com.module.playways.room.msg.manager.RelayRoomMsgManager
import com.module.playways.room.room.comment.model.CommentSysModel
import com.module.playways.room.room.event.PretendCommentMsgEvent
import com.module.playways.room.room.score.MachineScoreItem
import com.zq.live.proto.Common.ESex
import com.zq.live.proto.Common.UserInfo
import com.zq.live.proto.GrabRoom.EMsgPosType
import com.zq.live.proto.GrabRoom.ERoomMsgType
import com.zq.live.proto.GrabRoom.MachineScore
import com.zq.live.proto.GrabRoom.RoomMsg
import com.zq.live.proto.RelayRoom.*
import com.zq.mediaengine.kit.ZqEngineKit
import kotlinx.coroutines.*
import okhttp3.MediaType
import okhttp3.RequestBody
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.Exception
import kotlin.math.abs


class RelayCorePresenter(var mRoomData: RelayRoomData, var roomView: IRelayRoomView) : RxLifeCyclePresenter() {

    companion object {

        internal val MSG_ENSURE_IN_RC_ROOM = 9// 确保在融云的聊天室，保证融云的长链接

        internal val MSG_LAUNER_MUSIC = 21 // 到时间了 启动伴奏播放

        internal val MSG_TURN_CHANGE = 22 // 到时间了 轮次切换

        internal val MSG_TURN_CHANGE_PREPARE = 23 // 快到时间了 准备切换

        internal val MSG_MY_ACC_LOADING_FAILED = 24 // 我的伴奏下载失败

        internal val MSG_TURN_PRECHANGE_VOLUME = 25 // 对轮次切换，提前进行音量调整
    }

    internal var mAbsenTimes = 0

    internal var mRoomServerApi = ApiManager.getInstance().createService(RelayRoomServerApi::class.java)

    internal var mDestroyed = false

    internal var mUiHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                MSG_ENSURE_IN_RC_ROOM -> {
                    MyLog.d(TAG, "handleMessage 长时间没收到push，重新进入融云房间容错")
                    ModuleServiceManager.getInstance().msgService.leaveChatRoom(mRoomData.gameId.toString() + "")
                    joinRcRoom(0)
                    ensureInRcRoom()
                }
                MSG_LAUNER_MUSIC -> {
                    realSingBegin("MSG_LAUNER_MUSIC")
                }
                MSG_TURN_CHANGE -> {
                    turnChange()
                }
                MSG_TURN_CHANGE_PREPARE -> {
                    turnChangePrepare()
                }
                MSG_TURN_PRECHANGE_VOLUME -> {
                    turnPreChangeVolume(msg.obj as Boolean)
                }
                MSG_MY_ACC_LOADING_FAILED -> {
                    if (mRoomData.realRoundInfo?.roundSeq == msg.arg1) {
                        MyLog.i(TAG, "我的伴奏下载失败了，通知对端")
                        sendAccStatusToPeer(false)
                        if (mRoomData?.realRoundInfo?.peerAccLoadingOk == true) {
                            MyLog.i(TAG, "对端伴奏良好播放歌词")
                            realSingBegin("MSG_MY_ACC_LOADING_FAILED")
                        }
                    }
                }
            }
        }
    }

    internal var mPushMsgFilter: PushMsgFilter<*> = PushMsgFilter<RelayRoomMsg> { msg ->
        msg != null && msg.roomID == mRoomData.gameId
    }

    init {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        RelayRoomMsgManager.addFilter(mPushMsgFilter)
        joinRoomAndInit(true)
        startSyncGameStatus()
        pretendHeadSetSystemMsg(mRoomData.gameType)
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
        MyLog.w(TAG, "joinRoomAndInit" + " first=" + first + ", gameId is " + mRoomData.gameId)
        mAbsenTimes = 0

        if (mRoomData.gameId > 0) {
            var reInit = false
            if (first) {
                reInit = true
            }
            if (reInit) {
                val params = Params.getFromPref()
                //            params.setStyleEnum(Params.AudioEffect.none);
                params.scene = Params.Scene.doubleChat
                params.isEnableAudio = true
                ZqEngineKit.getInstance().init("relayroom", params)
            }
            ZqEngineKit.getInstance().joinRoom(mRoomData.gameId.toString(), UserAccountManager.uuidAsLong.toInt(), true, mRoomData.agoraToken)
            // 不发送本地音频, 会造成第一次抢没声音
            //ZqEngineKit.getInstance().muteLocalAudioStream(true)
        } else {
            MyLog.e(TAG, "房间号不合法 mRoomData.gameId=" + mRoomData.gameId)
        }
        joinRcRoom(-1)
        if (mRoomData.gameId > 0) {
//            for (playerInfoModel in mRoomData.getPlayerAndWaiterInfoList()) {
//                if (!playerInfoModel.isOnline) {
//                    continue
//                }
//                pretendEnterRoom(playerInfoModel)
//            }
//            pretendRoomNameSystemMsg("双人接唱", CommentSysModel.TYPE_ENTER_ROOM)
        }
        startHeartbeat()
        startSyncGameStatus()
    }

    private fun queryPeerAppVersion() {
        if ((mRoomData?.peerUser?.userID?:0) > 0 && mRoomData?.peerAppVersionCode <= 0) {
            val map = HashMap<String, Any>()
            map["userIDs"] = listOf(mRoomData?.peerUser?.userID, MyUserInfoManager.uid.toInt())

            val body = RequestBody.create(MediaType.parse(ApiManager.APPLICATION_JSON), JSON.toJSONString(map))
            launch {
                val result = subscribe { mRoomServerApi.queryAppVersion(body) }
                if (result.errno == 0) {
                    try {
                        val versionArray = result.data.getJSONArray("versions")
                        val peerVer = versionArray.getJSONObject(0).getIntValue("versionCode")
                        val myVer = versionArray.getJSONObject(1).getIntValue("versionCode")
                        MyLog.i(TAG, "Got version from server, peer:${peerVer} my:${myVer}")
                        if (myVer == U.getAppInfoUtils().versionCode) {
                            mRoomData?.peerAppVersionCode = peerVer
                        }
                    } catch (e:Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun isEnablePreChangeVolume(): Boolean {
        return mRoomData?.peerAppVersionCode >= 3009000
    }

//    fun changeMatchState(isChecked: Boolean) {
//        launch {
//            val map = mutableMapOf(
//                    "roomID" to mRoomData?.gameId,
//                    "matchStatus" to (if (isChecked) 2 else 1)
//            )
//
//            val body = RequestBody.create(MediaType.parse(ApiManager.APPLICATION_JSON), JSON.toJSONString(map))
//            val result = subscribe(RequestControl("$TAG changeMatchState", ControlType.CancelLast)) {
//                mRoomServerApi.changeMatchStatus(body)
//            }
//
//            if (result.errno == 0) {
//                if (isChecked) {
////                    val commentSysModel = CommentSysModel(GameModeType.GAME_MODE_RACE, "房主已将房间设置为 不允许用户匹配进入")
////                    EventBus.getDefault().post(PretendCommentMsgEvent(commentSysModel))
//                } else {
////                    val commentSysModel = CommentSysModel(GameModeType.GAME_MODE_RACE, "房主已将房间设置为 允许用户匹配进入")
////                    EventBus.getDefault().post(PretendCommentMsgEvent(commentSysModel))
//                }
//            } else {
//                U.getToastUtil().showShort(result.errmsg)
//            }
//        }
//    }

    private fun joinRcRoom(deep: Int) {
        if (deep > 4) {
            MyLog.d(TAG, "加入融云房间，重试5次仍然失败，放弃")
            return
        }
        if (mRoomData.gameId > 0) {
            ModuleServiceManager.getInstance().msgService.joinChatRoom(mRoomData.gameId.toString(), -1, object : ICallback {
                override fun onSucess(obj: Any?) {
                    MyLog.d(TAG, "加入融云房间成功")
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

    private fun pretendSystemMsg(text: String) {
        val commentSysModel = CommentSysModel(mRoomData.gameType, text)
        EventBus.getDefault().post(PretendCommentMsgEvent(commentSysModel))
    }

    private fun pretendRoomNameSystemMsg(roomName: String?, type: Int) {
        val commentSysModel = CommentSysModel(roomName ?: "", type)
        EventBus.getDefault().post(PretendCommentMsgEvent(commentSysModel))
    }

    /**
     * 由ui层告知
     * 开场动画结束
     */
    fun onOpeningAnimationOver() {
        // 开始触发触发轮次变化
        mRoomData.checkRoundInEachMode()
        ensureInRcRoom()
        userStatisticForIntimacy()
    }

    /**
     * 如果确定是自己唱了,预先可以做的操作
     */
    private fun preOpWhenSelfRound() {
        queryPeerAppVersion()
        var progress = mRoomData.getSingCurPosition()
        DebugLogView.println(TAG, "preOpWhenSelfRound progress=$progress")
        if (progress == Long.MAX_VALUE) {
            MyLog.e(TAG, "当前播放进度非法")
            return
        }
        ZqEngineKit.getInstance().adjustAudioMixingPublishVolume(0, false)
        ZqEngineKit.getInstance().adjustAudioMixingPlayoutVolume(0, false)

//        val accFile = SongResUtils.getAccFileByUrl(mRoomData?.realRoundInfo?.music?.acc)
        val midiFile = SongResUtils.getMIDIFileByUrl(mRoomData?.realRoundInfo?.music?.midi)
        if (midiFile != null && !midiFile.exists()) {
            U.getHttpUtils().downloadFileAsync(mRoomData?.realRoundInfo?.music?.midi, midiFile, true, null)
        }
//        if (accFile?.exists() == true) {
//            DebugLogView.println(TAG, "preOpWhenSelfRound 伴奏文件本地存在${accFile.path}")
//            ZqEngineKit.getInstance().startAudioMixing(MyUserInfoManager.uid.toInt(), accFile?.path, midiFile?.path, 0, 1)
//        } else {
//            DebugLogView.println(TAG, "preOpWhenSelfRound 伴奏文件本地不存在${accFile.path}")
            ZqEngineKit.getInstance().startAudioMixing(MyUserInfoManager.uid.toInt(), mRoomData?.realRoundInfo?.music?.accWithCdnInfosJson, midiFile?.path, 0, 1)
//        }
        mUiHandler.removeMessages(MSG_MY_ACC_LOADING_FAILED)
        var msg = mUiHandler.obtainMessage(MSG_MY_ACC_LOADING_FAILED)
        msg.arg1 = mRoomData.realRoundInfo?.roundSeq ?: 0
        mUiHandler.sendMessageDelayed(msg, 10 * 1000)
    }

    private fun realSingBegin(from: String) {
        DebugLogView.println(TAG, "realSingBegin 开始伴奏 progress=${mRoomData?.getSingCurPosition()} from=${from}")
        ZqEngineKit.getInstance().resumeAudioMixing()
        if (mRoomData.isSingByMeNow()) {
            DebugLogView.println(TAG, "realSingBegin 当前是我唱 开启音量")
            mRoomData.lastSingerID = MyUserInfoManager.uid.toInt()
            ZqEngineKit.getInstance().adjustAudioMixingPublishVolume(RelayRoomData.MUSIC_PUBLISH_VOLUME, false)
            ZqEngineKit.getInstance().adjustAudioMixingPlayoutVolume(ZqEngineKit.getInstance().params.playbackSignalVolume, false)
        } else {
            DebugLogView.println(TAG, "realSingBegin 当前不是我唱 关闭音量")
            mRoomData.lastSingerID = mRoomData.peerUser?.userID
            ZqEngineKit.getInstance().adjustAudioMixingPublishVolume(0, false)
            ZqEngineKit.getInstance().adjustAudioMixingPlayoutVolume(0, false)
        }
        launcherNextTurn()
        roomView.singBegin()
    }

    private fun turnChangePrepare() {
        roomView.turnMyChangePrepare()
    }

    private fun fadeVolume(isPlayout: Boolean, isFadeIn: Boolean) {
        val duration = 1000L
        val fromVolume: Int
        val toVolume:Int

        if (isFadeIn) {
            fromVolume = 0
            toVolume = if (isPlayout) {
                ZqEngineKit.getInstance().params.audioMixingPlayoutVolume
            } else {
                RelayRoomData.MUSIC_PUBLISH_VOLUME
            }
        } else {
            toVolume = 0
            fromVolume = if (isPlayout) {
                ZqEngineKit.getInstance().params.audioMixingPlayoutVolume
            } else {
                RelayRoomData.MUSIC_PUBLISH_VOLUME
            }
        }

        val animation = ValueAnimator.ofInt(fromVolume, toVolume)
        animation.addUpdateListener {
            val v = it.animatedValue as Int
            if (isPlayout) {
                ZqEngineKit.getInstance().adjustAudioMixingPlayoutVolume(v, false)
            } else {
                ZqEngineKit.getInstance().adjustAudioMixingPublishVolume(v, false)
            }
        }
        animation.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {
            }

            override fun onAnimationEnd(animation: Animator?) {
            }

            override fun onAnimationCancel(animation: Animator?) {
                if (isPlayout) {
                    ZqEngineKit.getInstance().adjustAudioMixingPlayoutVolume(toVolume, false)
                } else {
                    ZqEngineKit.getInstance().adjustAudioMixingPublishVolume(toVolume, false)
                }
            }

            override fun onAnimationStart(animation: Animator?) {
                if (isPlayout) {
                    ZqEngineKit.getInstance().adjustAudioMixingPlayoutVolume(toVolume, false)
                } else {
                    ZqEngineKit.getInstance().adjustAudioMixingPublishVolume(toVolume, false)
                }
            }
        })
        animation.duration = duration
        animation.start()
    }

    private fun turnChange() {
        DebugLogView.println(TAG, "turnChange 开始轮换 progress=${mRoomData?.getSingCurPosition()}")
        if (mRoomData.isSingByMeNow()) {
            DebugLogView.println(TAG, "turnChange 当前是我唱 开启音量")
            if (mRoomData?.lastSingerID == mRoomData.peerUser?.userID) {
                // 确实有切换，声音渐变处理
                if (isEnablePreChangeVolume()) {
                    // 到切换时间点再调高远端音量
                    fadeVolume(isPlayout = false, isFadeIn = true)
                } else {
                    fadeVolume(isPlayout = true, isFadeIn = true)
                    fadeVolume(isPlayout = false, isFadeIn = true)
                }
            } else {
                ZqEngineKit.getInstance().adjustAudioMixingPublishVolume(RelayRoomData.MUSIC_PUBLISH_VOLUME, false)
                ZqEngineKit.getInstance().adjustAudioMixingPlayoutVolume(ZqEngineKit.getInstance().params.playbackSignalVolume, false)
            }
            mRoomData.lastSingerID = MyUserInfoManager.uid.toInt()
        } else {
            DebugLogView.println(TAG, "turnChange 当前不是我唱 关闭音量")
            if (mRoomData.realRoundInfo?.peerAccLoadingOk == true) {
                if (mRoomData?.lastSingerID == MyUserInfoManager.uid.toInt()) {
                    // 确实有切换，声音渐变处理
                    if (isEnablePreChangeVolume()) {
                        // 到切换时间点再调低本地音量
                        fadeVolume(isPlayout = true, isFadeIn = false)
                    } else {
                        fadeVolume(isPlayout = true, isFadeIn = false)
                        fadeVolume(isPlayout = false, isFadeIn = false)
                    }
                } else {
                    ZqEngineKit.getInstance().adjustAudioMixingPublishVolume(0, false)
                    ZqEngineKit.getInstance().adjustAudioMixingPlayoutVolume(0, false)
                }
            } else {
                DebugLogView.println(TAG, "对端的伴奏坏了，我不做处理")
            }
            mRoomData.lastSingerID = mRoomData.peerUser?.userID
        }
        launcherNextTurn()
        roomView.turnChange()
    }

    private fun launcherNextTurn() {
        // 算出下一次轮次切换的时间
        val nextTs = mRoomData.getNextTurnChangeTs()
        if (nextTs > 0) {
            DebugLogView.println(TAG, "${nextTs}ms 后进行轮次切换")
            val nextIsMyTurn = mRoomData.getSingerIdNow() == mRoomData.peerUser?.userID
            if (nextIsMyTurn) {
                mUiHandler.removeMessages(MSG_TURN_CHANGE_PREPARE)
                mUiHandler.sendEmptyMessageDelayed(MSG_TURN_CHANGE_PREPARE, nextTs - 3000)
            }
            if (isEnablePreChangeVolume()) {
                mUiHandler.removeMessages(MSG_TURN_PRECHANGE_VOLUME)
                val msg = mUiHandler.obtainMessage(MSG_TURN_PRECHANGE_VOLUME, nextIsMyTurn)
                var delayTime = nextTs - 2000
//                // 发送端再提前200ms降低音量（传输延迟）
//                if (!nextIsMyTurn) delayTime -= 200
                mUiHandler.sendMessageDelayed(msg, delayTime)
            }
            mUiHandler.removeMessages(MSG_TURN_CHANGE)
            mUiHandler.sendEmptyMessageDelayed(MSG_TURN_CHANGE, nextTs)
        } else {
            DebugLogView.println(TAG, "${nextTs} 没有轮次切换了")
        }
    }

    private fun turnPreChangeVolume(nextIsMyTurn: Boolean) {
        if (!isEnablePreChangeVolume()) {
            return
        }
        if (nextIsMyTurn) {
            DebugLogView.println(TAG, "马上到我了，提前调高本地伴奏音量")
            fadeVolume(isPlayout = true, isFadeIn = true)
        } else {
            if (mRoomData.realRoundInfo?.peerAccLoadingOk == true) {
                DebugLogView.println(TAG, "马上该对端了，提前调低远端伴奏音量")
                fadeVolume(isPlayout = false, isFadeIn = false)
            } else {
                DebugLogView.println(TAG, "马上该对端了，对端的伴奏坏了，我不做处理")
            }
        }
    }

    private fun preOpWhenOtherRound() {
    }

    /**
     * 真正打开引擎开始演唱
     */
    fun beginSing() {
        // 打开引擎，变为主播
        // 需要上传音频伪装成机器人
//        if (mRoomData.realRoundInfo?.isNormalRound == true) {
//            /**
//             * 个人标签声音
//             */
//            val fileName = String.format(PERSON_LABEL_SAVE_PATH_FROMAT, mRoomData.gameId, mRoomData.realRoundInfo?.roundSeq)
//            val savePath = U.getAppInfoUtils().getFilePathInSubDir("grab_save", fileName)
//            ZqEngineKit.getInstance().startAudioRecording(savePath, false)
//        }
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
//                mExoPlayer!!.setMuteAudio(true)
//            }
        } else {
            // 如果打开静音
//            if (mExoPlayer != null) {
//                mExoPlayer!!.setMuteAudio(false)
//            }
        }
    }


    override fun destroy() {
        MyLog.d(TAG, "destroy begin")
        super.destroy()
        mDestroyed = true
        Params.save2Pref(ZqEngineKit.getInstance().params)
        if (!mRoomData.isHasExitGame) {
            exitRoom("destroy")
        }
        cancelSyncGameStatus()
        heartbeatJob?.cancel()
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        ZqEngineKit.getInstance().destroy("relayroom")
        mUiHandler.removeCallbacksAndMessages(null)
        RelayRoomMsgManager.removeFilter(mPushMsgFilter)
        ModuleServiceManager.getInstance().msgService.leaveChatRoom(mRoomData.gameId.toString())
        JiGuangPush.exitSkrRoomId(mRoomData.gameId.toString())
        MyLog.d(TAG, "destroy over")
    }

    /**
     * 上报轮次结束信息
     */
    fun sendRoundOverInfo() {
        if (mRoomData?.realRoundInfo?.hasSendRoundOverInfo == false) {
            MyLog.w(TAG, "上报我的演唱结束")
            mRoomData?.realRoundInfo?.hasSendRoundOverInfo = true
            val map = HashMap<String, Any>()
            map["roomID"] = mRoomData.gameId
            map["roundSeq"] = mRoomData.realRoundInfo?.roundSeq ?: 0

            val body = RequestBody.create(MediaType.parse(ApiManager.APPLICATION_JSON), JSON.toJSONString(map))
            launch {
                var result = subscribe { mRoomServerApi.sendRoundOver(body) }
                if (result.errno == 0) {
                    MyLog.w(TAG, "演唱结束上报成功 traceid is " + result.traceId)
                } else {
                    MyLog.w(TAG, "演唱结束上报失败 traceid is " + result.traceId)
                }
            }
        } else {
            MyLog.w(TAG, "已经上报过演唱结束")
        }
    }


    /**
     * 放弃演唱接口
     */
    fun giveUpSing(okCallback: (() -> Unit)?) {
        MyLog.w(TAG, "我放弃演唱")
        val map = HashMap<String, Any?>()
        map["roomID"] = mRoomData.gameId
        map["roundSeq"] = mRoomData.realRoundInfo?.roundSeq ?: 0
        val body = RequestBody.create(MediaType.parse(ApiManager.APPLICATION_JSON), JSON.toJSONString(map))
        launch {
            var result = subscribe(RequestControl("giveUpSing", ControlType.CancelThis)) {
                mRoomServerApi.giveUpSing(body)
            }
            if (result.errno == 0) {
                //closeEngine()
                okCallback?.invoke()
                MyLog.w(TAG, "放弃演唱上报成功 traceid is " + result.traceId)
            } else {
                MyLog.w(TAG, "放弃演唱上报失败 traceid is " + result.traceId)
            }
        }
    }

    /**
     * 退出房间
     */
    fun exitRoom(from: String) {
        MyLog.w(TAG, "exitRoom from=$from")
        val map = HashMap<String, Any>()
        map["roomID"] = mRoomData.gameId
        mRoomData.isHasExitGame = true
        val body = RequestBody.create(MediaType.parse(ApiManager.APPLICATION_JSON), JSON.toJSONString(map))
        // 不想 destroy 时被取消
        GlobalScope.launch {
            var result = subscribe { mRoomServerApi.exitRoom(body) }
            if (result.errno == 0) {
                val msg = result.data.getString("needAlertMsg")
                if(!TextUtils.isEmpty(msg)){
                    U.getToastUtil().showLong(msg)
                }
            }
        }
    }

    /**
     * 将本人的伴奏状态传给对端
     */
    fun sendAccStatusToPeer(ok: Boolean) {
        val msgService = ModuleServiceManager.getInstance().msgService
        var js = JSONObject()
        js.put("userID", MyUserInfoManager.uid.toInt())
        js.put("roundSeq", mRoomData.realRoundInfo?.roundSeq)
        js.put("accLoadingOk", ok)
        msgService?.sendSpecialDebugMessage("${mRoomData?.peerUser?.userID}", 3, js.toJSONString(), object : ICallback {
            override fun onSucess(obj: Any) {
                //U.getToastUtil().showLong("请求成功,稍等看该用户是否有返回")
            }

            override fun onFailed(obj: Any, errcode: Int, message: String) {
                //U.getToastUtil().showLong("请求失败")
            }
        })
    }

    var heartbeatJob: Job? = null

    private fun startHeartbeat() {
//        heartbeatJob?.cancel()
//        heartbeatJob = launch {
//            while (true) {
//                val map = mutableMapOf(
//                        "roomID" to mRoomData.gameId,
//                        "userID" to MyUserInfoManager.uid
//                )
//                val body = RequestBody.create(MediaType.parse(ApiManager.APPLICATION_JSON), JSON.toJSONString(map))
//                val result = subscribe { mRoomServerApi.heartbeat(body) }
//                if (result.errno == 0) {
//
//                } else {
//
//                }
//                delay(60 * 1000)
//            }
//        }
    }

    private fun cancelSyncGameStatus() {
        syncJob?.cancel()
    }

    var syncJob: Job? = null

    private fun startSyncGameStatus() {
        if (mRoomData.isIsGameFinish) {
            MyLog.w(TAG, "游戏结束了，还特么Sync")
            return
        }

        if (!mRoomData.isPersonArrive()) {
            MyLog.w(TAG, "另一个人还没进来")
            return
        }

        syncJob?.cancel()
        syncJob = launch {
            while (true) {
                delay(10 * 1000)
                val result = subscribe { mRoomServerApi.syncStatus(mRoomData.gameId.toLong()) }
                if (result.errno == 0) {
                    val gameOverTimeMs = result.data.getLongValue("gameOverTimeMs")
                    if (gameOverTimeMs > 0) {
                        mRoomData.gameOverTs = gameOverTimeMs
                        DebugLogView.println(TAG, "gameOverTimeMs=${gameOverTimeMs} 游戏结束时间>0 ，游戏结束，退出房间")
                        // 游戏结束了，停服了
                        mRoomData.expectRoundInfo = null
                        mRoomData.checkRoundInEachMode()
                    } else {
//                        val syncStatusTimeMs = result.data.getLongValue("syncStatusTimeMs")
//                        if (syncStatusTimeMs > mRoomData.lastSyncTs) {
//                            mRoomData.lastSyncTs = syncStatusTimeMs
                        val roundInfo = JSON.parseObject(result.data.getString("currentRound"), RelayRoundInfoModel::class.java)
                        processSyncResult(roundInfo)
//                        }
                    }
                } else {

                }
            }
        }
    }


    private fun processSyncResult(roundInfo: RelayRoundInfoModel) {
        if (roundInfo.roundSeq == mRoomData.realRoundSeq) {
            mRoomData.realRoundInfo?.tryUpdateRoundInfoModel(roundInfo, true)
        } else if (roundInfo.roundSeq > mRoomData.realRoundSeq) {
            MyLog.w(TAG, "sync 回来的轮次大，要替换 roundInfo 了")
            // 主轮次结束
            launch {
                mRoomData.expectRoundInfo = roundInfo
                mRoomData.checkRoundInEachMode()
            }
        }
    }

    /**
     * 为了方便服务器亲密度结算
     */
    private fun userStatisticForIntimacy() {
//        launch {
//            while (true) {
//                delay(10 * 60 * 1000)
//                val l1 = java.util.ArrayList<Int>()
//                for (m in mRoomData.getPlayerAndWaiterInfoList()) {
//                    if (m.userID != MyUserInfoManager.uid.toInt()) {
//                        if (mRoomData.preUserIDsSnapShots.contains(m.userID)) {
//                            l1.add(m.userID)
//                        }
//                    }
//                }
//                if (l1.isNotEmpty()) {
//                    val map = java.util.HashMap<String, Any>()
//                    map["gameID"] = mRoomData.gameId
//                    map["mode"] = GameModeType.GAME_MODE_GRAB
//                    val ts = System.currentTimeMillis()
//                    map["timeMs"] = ts
//                    map["sign"] = U.getMD5Utils().MD5_32("skrer|" + MyUserInfoManager.uid + "|" + ts)
//                    map["userID"] = MyUserInfoManager.uid
//                    map["preUserIDs"] = l1
//                    val body = RequestBody.create(MediaType.parse(ApiManager.APPLICATION_JSON), JSON.toJSONString(map))
//                    val result = subscribe { mRoomServerApi.userStatistic(body) }
//                    if (result.errno == 0) {
//
//                    }
//                } else {
//                    break
//                }
//            }
//        }
    }


    /**
     * 轮次切换事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: RelayRoundChangeEvent) {
        MyLog.d(TAG, "RelayRoundChangeEvent = $event")
        processStatusChange(1, event.lastRound, event.newRound)
    }

    /**
     * 轮次内状态更新
     *
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: RelayRoundStatusChangeEvent) {
        MyLog.d(TAG, "RelayRoundStatusChangeEvent =$event")
        processStatusChange(2, null, event.roundInfo)
    }

    private fun processStatusChange(from: Int, lastRound: RelayRoundInfoModel?, thisRound: RelayRoundInfoModel?) {
        DebugLogView.println(TAG, "processStatusChange from=$from roundSeq=${thisRound?.roundSeq} statusNow=${thisRound?.status}")
        // 轮次变化尝试更新头像
//        mUiHandler.removeMessages(MSG_ENSURE_SWITCH_BROADCAST_SUCCESS)
        closeEngine()
        ZqEngineKit.getInstance().stopRecognize()
        if (thisRound == null) {
            // 游戏结束了
            roomView.gameOver("thisRound == null")
            return
        }
        if (thisRound.status == ERRoundStatus.RRS_INTRO.value) {
            // 等待阶段
            roomView.showRoundOver(lastRound) {
                roomView.showWaiting()
            }

        } else if (thisRound.isSingStatus) {

            roomView.showRoundOver(lastRound) {
                // 演唱阶段
                val size = U.getActivityUtils().activityList.size
                var needTips = false
                for (i in size - 1 downTo 0) {
                    val activity = U.getActivityUtils().activityList[i]
                    if (activity is RelayRoomActivity) {
                        break
                    } else {
                        activity.finish()
                        needTips = true
                    }
                }
                if (needTips) {
                    U.getToastUtil().showLong("你的演唱开始了")
                }
                roomView.singPrepare(lastRound) {
                    preOpWhenSelfRound()
                }
            }
        } else if (thisRound.status == ERRoundStatus.RRS_END.value) {

        }
    }

    private fun closeEngine() {
        if (mRoomData.gameId > 0) {
            ZqEngineKit.getInstance().stopAudioMixing()
            mUiHandler.removeMessages(MSG_TURN_PRECHANGE_VOLUME)
            mUiHandler.removeMessages(MSG_TURN_CHANGE)
            mUiHandler.removeMessages(MSG_TURN_CHANGE_PREPARE)
            mUiHandler.removeMessages(MSG_LAUNER_MUSIC)
            mUiHandler.removeMessages(MSG_MY_ACC_LOADING_FAILED)
//            ZqEngineKit.getInstance().stopAudioRecording()
//            if (ZqEngineKit.getInstance().params.isAnchor) {
//                ZqEngineKit.getInstance().setClientRole(false)
//            }
        }
    }

    /**
     * 引擎相关事件
     *
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.POSTING)
    fun onEvent(event: EngineEvent) {
        if (event.getType() == EngineEvent.TYPE_MUSIC_PLAY_STATE_CHANGE) {
            var state = event.obj as EngineEvent.MusicStateChange
            if (state.isPlayOk && mRoomData.realRoundInfo?.accLoadingOk == false) {
                mUiHandler.removeMessages(MSG_MY_ACC_LOADING_FAILED)
                sendAccStatusToPeer(true)
                mRoomData.realRoundInfo?.accLoadingOk = true
                var progress = mRoomData.getSingCurPosition()
                DebugLogView.println(TAG, "伴奏加载ok progress=${progress} shiftTs=" + mRoomData.getShiftTsForRelay())
                if (progress != Long.MAX_VALUE) {
                    if (progress > 0) {
                        DebugLogView.println(TAG, "EngineEvent 超时上车了")
                        ZqEngineKit.getInstance().setAudioMixingPosition(progress.toInt())
                        mUiHandler.post {
                            realSingBegin("超时上车")
                        }
                    } else {
                        DebugLogView.println(TAG, "EngineEvent 先暂停 ${-progress}ms后 resume")
                        ZqEngineKit.getInstance().pauseAudioMixing()
                        mUiHandler.removeMessages(MSG_LAUNER_MUSIC)
                        mUiHandler.sendEmptyMessageDelayed(MSG_LAUNER_MUSIC, -progress)
                    }
                } else {
                    MyLog.e(TAG, "当前播放进度非法2")
                }
            }
        } else if (event.getType() == EngineEvent.TYPE_MUSIC_PLAY_FINISH) {
            DebugLogView.println(TAG, "伴奏播放完毕")
            sendRoundOverInfo()
        } else if (event.getType() == EngineEvent.TYPE_MUSIC_PLAY_TIME_FLY_LISTENER) {
            //DebugLogView.println(TAG, "伴奏播放进度")
            val timeInfo = event.getObj() as EngineEvent.MixMusicTimeInfo
            //这个是唱的时间，先在按长度算时间
            var progress = mRoomData.getSingCurPosition()
            val shift = progress - timeInfo.current
            //DebugLogView.println(TAG, "当前伴奏与预定时间的偏移为${shift}")
            if (abs(shift) > 1000 && progress >= 0) {
                DebugLogView.println(TAG, "当前伴奏与预定时间的偏移过大 为${shift}")
                // 伴奏对齐，重新发送轮次切换
                ZqEngineKit.getInstance().setAudioMixingPosition(mRoomData.getSingCurPosition().toInt())
                launcherNextTurn()
            }
            if (mRoomData.hasOverThisRound()) {
                sendRoundOverInfo()
            }
        } else if (event.getType() == EngineEvent.TYPE_USER_ROLE_CHANGE) {
//            val roleChangeInfo = event.getObj<EngineEvent.RoleChangeInfo>()
//            if (roleChangeInfo.newRole == 1) {
//                val roundInfoModel = mRoomData.realRoundInfo
//                if (roundInfoModel != null && roundInfoModel.singBySelf()) {
//                    MyLog.d(TAG, "演唱环节切换主播成功")
//                    onChangeBroadcastSuccess()
//                }
//            }
        } else if (event.getType() == EngineEvent.TYPE_USER_MUTE_AUDIO) {
            //            UserStatus userStatus = event.getUserStatus();
            //            if (userStatus != null) {
            //                MyLog.d(TAG, "有人mute变化 uid=" + userStatus.getUserId());
            //                if (userStatus.getUserId() == mRoomData.getOwnerId()) {
            //                    if (mRoomData.isOwner()) {
            //                        MyLog.d(TAG, "自己就是房主，忽略");
            //                    } else {
            //                        if (!userStatus.isAudioMute()) {
            //                            MyLog.d(TAG, "房主解开mute，如果检测到房主说话，音量就衰减");
            //                            weakVolume(1000);
            //                        } else {
            //                            MyLog.d(TAG, "房主mute了，恢复音量");
            //                            mUiHandler.removeMessages(MSG_RECOVER_VOLUME);
            //                            mUiHandler.sendEmptyMessage(MSG_RECOVER_VOLUME);
            //                        }
            //                    }
            //                }
            //            }
        } else if (event.getType() == EngineEvent.TYPE_USER_AUDIO_VOLUME_INDICATION) {
            //            List<EngineEvent.UserVolumeInfo> list = event.getObj();
            //            for (EngineEvent.UserVolumeInfo uv : list) {
            //                //    MyLog.d(TAG, "UserVolumeInfo uv=" + uv);
            //                if (uv != null) {
            //                    int uid = uv.getUid();
            //                    if (uid == 0) {
            //                        uid = (int) MyUserInfoManager.getInstance().getUid();
            //                    }
            //                    if (mRoomData != null
            //                            && uid == mRoomData.getOwnerId()
            //                            && uv.getVolume() > 40
            //                            && !mRoomData.isOwner()) {
            //                        MyLog.d(TAG, "房主在说话");
            //                        weakVolume(1000);
            //                    }
            //                }
            //            }
        } else {
            // 可以考虑监听下房主的说话提示 做下容错
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: PartyPeerAccStatusEvent) {
        if (event.userID == mRoomData.peerUser?.userID && event.roundSeq == mRoomData.realRoundInfo?.roundSeq) {
            MyLog.i(TAG, "对方伴奏情况ok=${event.accLoadingOk}")
            if (mRoomData.realRoundInfo?.peerAccLoadingOk == true && event.accLoadingOk == false) {
                MyLog.i(TAG, "收到对端伴奏播放失败的事件，替对方播伴奏")
                mRoomData.realRoundInfo?.peerAccLoadingOk = false
                ZqEngineKit.getInstance().adjustAudioMixingPublishVolume(RelayRoomData.MUSIC_PUBLISH_VOLUME, false)
                ZqEngineKit.getInstance().adjustAudioMixingPlayoutVolume(ZqEngineKit.getInstance().params.playbackSignalVolume, false)
            } else if (mRoomData.realRoundInfo?.peerAccLoadingOk == false && event.accLoadingOk == true) {
                mRoomData.realRoundInfo?.peerAccLoadingOk = true
                if (mRoomData.isSingByMeNow()) {
                    ZqEngineKit.getInstance().adjustAudioMixingPublishVolume(RelayRoomData.MUSIC_PUBLISH_VOLUME, false)
                    ZqEngineKit.getInstance().adjustAudioMixingPlayoutVolume(ZqEngineKit.getInstance().params.playbackSignalVolume, false)
                } else {
                    ZqEngineKit.getInstance().adjustAudioMixingPublishVolume(0, false)
                    ZqEngineKit.getInstance().adjustAudioMixingPlayoutVolume(0, false)
                }

            }
        }
    }

//    /**
//     * 成功切换为主播
//     */
//    private fun onChangeBroadcastSuccess() {
//        MyLog.d(TAG, "onChangeBroadcastSuccess 我的演唱环节")
//        mUiHandler.removeMessages(MSG_ENSURE_SWITCH_BROADCAST_SUCCESS)
//        mUiHandler.post(Runnable {
//            var songModel: SongModel? = mRoomData.realRoundInfo?.music
//            if (songModel == null) {
//                return@Runnable
//            }
//            // 开始开始混伴奏，开始解除引擎mute
//            val accFile = SongResUtils.getAccFileByUrl(songModel?.acc)
//            // midi不需要在这下，只要下好，native就会解析，打分就能恢复
//            val midiFile = SongResUtils.getMIDIFileByUrl(songModel?.midi)
//            MyLog.d(TAG, "onChangeBroadcastSuccess 我的演唱环节 info=${songModel.toSimpleString()} acc=${songModel.acc} midi=${songModel.midi} ")
//
//            // 下载midi
//            if (midiFile != null && !midiFile.exists()) {
//                MyLog.d(TAG, "onChangeBroadcastSuccess 下载midi文件 url=${songModel.midi} => local=${midiFile.path}")
//                U.getHttpUtils().downloadFileAsync(songModel.midi, midiFile, true, null)
//            }
//
//            //  播放伴奏
//            val songBeginTs = songModel.beginMs
//            if (accFile != null && accFile.exists()) {
//                // 伴奏文件存在
//                ZqEngineKit.getInstance().startAudioMixing(MyUserInfoManager.uid.toInt(), accFile.absolutePath, midiFile.absolutePath, songBeginTs.toLong(), false, false, 1)
//            } else {
//                ZqEngineKit.getInstance().startAudioMixing(MyUserInfoManager.uid.toInt(), songModel.acc, midiFile.absolutePath, songBeginTs.toLong(), false, false, 1)
//            }
//
//        })
//    }

//    @Subscribe(threadMode = ThreadMode.MAIN)
//    fun onEvent(event: MAddMusicMsg) {
//        MyLog.d(TAG, "MAddMusicMsg event=$event")
//        roomView.showSongCount(event.musicCnt)
//    }
//
//    @Subscribe(threadMode = ThreadMode.MAIN)
//    fun onEvent(event: MDelMusicMsg) {
//        MyLog.d(TAG, "MAddMusicMsg event=$event")
//        roomView.showSongCount(event.musicCnt)
//    }

    /**
     * 轮次变化
     *
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: RNextRoundMsg) {
        MyLog.w(TAG, "收到服务器的某一个人轮次结束的push currentRound:${event.currentRound}")
        MyLog.w(TAG, "收到服务器的某一个人轮次结束的push nextRound:${event.nextRound}")
        ensureInRcRoom()
//        roomView.showSongCount(event.musicCnt)
        var currentRound = RelayRoundInfoModel.parseFromRoundInfo(event.currentRound)
        var nextRound = RelayRoundInfoModel.parseFromRoundInfo(event.nextRound)
        if (nextRound.roundSeq > (mRoomData.expectRoundInfo?.roundSeq ?: 0)) {
            // 游戏轮次结束
            // 轮次确实比当前的高，可以切换
            MyLog.w(TAG, "nextRound.roundSeq=${nextRound.roundSeq} 轮次确实比当前的高，可以切换")
            mRoomData.expectRoundInfo = nextRound
            mRoomData.checkRoundInEachMode()
        } else {
            MyLog.w(TAG, "轮次比当前轮次还小,直接忽略 当前轮次:" + mRoomData.expectRoundInfo?.roundSeq
                    + " push轮次:" + event.currentRound.roundSeq)
        }
    }


    // TODO sync
    /**
     * 同步
     *
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: RSyncMsg) {
        if (!mRoomData.isPersonArrive()) {
            fetchRelayRoom()
            return
        }

        ensureInRcRoom()
        MyLog.w(TAG, "收到服务器 sync push更新状态 ,event=$event")
        var thisRound = RelayRoundInfoModel.parseFromRoundInfo(event.currentRound)
        if (event.enableNoLimitDuration) {
            mRoomData.unLockMe = true
            mRoomData.unLockPeer = true
        }
        // 延迟10秒sync ，一旦启动sync 间隔 5秒 sync 一次
        startSyncGameStatus()
        processSyncResult(thisRound)
    }

    /**
     * 发出邀请之后轮询检查对方的进房情况，因为push有可能会丢
     */
    private fun fetchRelayRoom() {
        ApiMethods.subscribe(mRoomServerApi.getRelayInviteEnterResult(), object : ApiObserver<ApiResult>() {
            override fun process(obj: ApiResult?) {
                if (obj?.errno == 0 && obj.data.getBooleanValue("hasInvitedRoom")) {
                    val joinRelayRoomRspModel = JSON.parseObject(obj.data.toJSONString(), JoinRelayRoomRspModel::class.java)
                    startGameByEnterInvite(joinRelayRoomRspModel)
                }
            }
        }, this)
    }

//    @Subscribe
//    fun onEvent(event: QChangeRoomNameEvent) {
//        MyLog.d(TAG, "onEvent QChangeRoomNameEvent !!改变房间名 $event")
//        if (mRoomData.gameId == event.info.roomID) {
//            pretendRoomNameSystemMsg(event.newName, CommentSysModel.TYPE_MODIFY_ROOM_NAME)
//        }
//    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: ActivityUtils.ForeOrBackgroundChange) {
        MyLog.w(TAG, if (event.foreground) "切换到前台" else "切换到后台")
//        if (event.foreground) {
//            muteAllRemoteAudioStreams(mRoomData.isMute, false)
//        } else {
//            muteAllRemoteAudioStreams(true, false)
//        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: RUnlockMsg) {
        ensureInRcRoom()
        MyLog.w(TAG, "event=$event")
        for (us in event.userLockInfoList) {
            if (us.userID == MyUserInfoManager.uid.toInt()) {
                mRoomData.unLockMe = !us.hasLock
            } else if (us.userID == mRoomData.peerUser?.userID) {
                mRoomData.unLockPeer = !us.hasLock
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: RGameOverMsg) {
        ensureInRcRoom()
        roomView.gameOver("RGameOverMsg")
    }


//    /**
//     * 录制小游戏事件，防止录进去背景音
//     *
//     * @param event
//     */
//    @Subscribe(threadMode = ThreadMode.MAIN)
//    fun onEvent(event: MuteAllVoiceEvent) {
//        MyLog.d(TAG, "onEvent event=$event")
//        if (event.begin) {
//            muteAllRemoteAudioStreams(true, false)
//        } else {
//            muteAllRemoteAudioStreams(mRoomData.isMute, false)
//        }
//    }


    /*打分相关*/

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: MachineScoreEvent) {
        MyLog.d(TAG, "onEvent MachineScoreEvent = $event")
        //收到其他人的机器打分消息，比较复杂，暂时简单点，轮次正确就直接展示
        if (mRoomData.getSingerIdNow() == event.userId) {
            DebugLogView.println(TAG, "对手 score=${event.score}")
            roomView.receiveScoreEvent(event.score)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: LyricAndAccMatchManager.ScoreResultEvent) {
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

    private fun processScore(score: Int, line: Int) {
        if (score < 0) {
            return
        }
        if (!mRoomData.isSingByMeNow()) {
            DebugLogView.println(TAG, "不是自己唱 score=${score}")
            return
        }
        val machineScoreItem = MachineScoreItem()
        machineScoreItem.score = score
        // 这有时是个耗时操作
        //        long ts = ZqEngineKit.getInstance().getAudioMixingCurrentPosition();
        val ts: Long = -1
        machineScoreItem.ts = ts
        machineScoreItem.no = line
        // 打分信息传输给其他人
        sendScoreToOthers(machineScoreItem)
        DebugLogView.println(TAG, "自己 score=${score}")
        roomView.receiveScoreEvent(score)
        //打分传给服务器
        val now = mRoomData.realRoundInfo
        if (now != null) {
            if (mRoomData.isSingByMeNow()) {
                sendScoreToServer(score, line)
            }
        }
    }

    /**
     * 将自己的分数传给其他人
     *
     * @param machineScoreItem
     */
    private fun sendScoreToOthers(machineScoreItem: MachineScoreItem) {
        // 后续加个优化，如果房间里两人都是机器人就不加了
        val msgService = ModuleServiceManager.getInstance().msgService
        if (msgService != null) {
            val ts = System.currentTimeMillis()
            val senderInfo = UserInfo.Builder()
                    .setUserID(MyUserInfoManager.uid.toInt())
                    .setNickName(MyUserInfoManager.nickName)
                    .setAvatar(MyUserInfoManager.avatar)
                    .setSex(ESex.fromValue(MyUserInfoManager.sex))
                    .setDescription("")
                    .setIsSystem(false)
                    .build()

            val now = mRoomData.realRoundInfo
            if (now != null && now.music != null) {
                val roomMsg = RoomMsg.Builder()
                        .setTimeMs(ts)
                        .setMsgType(ERoomMsgType.RM_ROUND_MACHINE_SCORE)
                        .setRoomID(mRoomData.gameId)
                        .setNo(ts)
                        .setPosType(EMsgPosType.EPT_UNKNOWN)
                        .setSender(senderInfo)
                        .setMachineScore(MachineScore.Builder()
                                .setUserID(MyUserInfoManager.uid.toInt())
                                .setNo(machineScoreItem.no)
                                .setScore(machineScoreItem.score)
                                .setItemID(now?.music?.itemID)
//                                .setLineNum(mRoomData.songLineNum)
                                .build()
                        )
                        .build()
                val contnet = U.getBase64Utils().encode(roomMsg.toByteArray())
                msgService.sendChatRoomMessage(mRoomData.gameId.toString(), CustomMsgType.MSG_TYPE_ROOM, contnet, null)
            }
        }
    }

    /**
     * 单句打分上报,合唱模式
     *
     * @param score
     * @param line
     */
    private fun sendScoreToServer(score: Int, line: Int) {
        val map = java.util.HashMap<String, Any>()
        val infoModel = mRoomData.realRoundInfo ?: return
        map["userID"] = MyUserInfoManager.uid

        var itemID = 0
        if (infoModel.music != null) {
            itemID = infoModel?.music?.itemID ?: 0
        }

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
        map["segmentCnt"] = (mRoomData.realRoundInfo?.music?.relaySegments?.size ?: 0) + 1
        map["sentenceCnt"] = mRoomData.sentenceCnt

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
        val body = RequestBody.create(MediaType.parse(ApiManager.APPLICATION_JSON), JSON.toJSONString(map))
        launch {
            var result = subscribe {
                mRoomServerApi.sendPkPerSegmentResult(body)
            }
            if (result.errno == 0) {
                // TODO: 2018/12/13  当前postman返回的为空 待补充
                MyLog.w(TAG, "单句打分上报成功")
            } else {
                MyLog.w(TAG, "单句打分上报失败" + result.errno)
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
                        mRoomData.setHzCount(property.hongZuanBalance, property.lastChangeMs)
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
                StatisticsAdapter.recordCountEvent("relay", "game_getflower", null)
            } else {
                StatisticsAdapter.recordCountEvent("relay", "game_getgift", null)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    fun onEvent(cnRelayEnterFromRoomInviteNotifyEvent: CNRelayEnterFromRoomInviteNotifyEvent) {
        MyLog.d(TAG, "onEvent cnRelayEnterFromRoomInviteNotifyEvent=$cnRelayEnterFromRoomInviteNotifyEvent")
        if (!mRoomData.isPersonArrive() && mRoomData.gameId == cnRelayEnterFromRoomInviteNotifyEvent.relayRoomEnterMsg.roomID) {
            val rsp = JoinRelayRoomRspModel.parseFromPB(cnRelayEnterFromRoomInviteNotifyEvent.relayRoomEnterMsg)
            startGameByEnterInvite(rsp)
        }
    }

    private fun startGameByEnterInvite(rsp: JoinRelayRoomRspModel) {
        MyLog.d(TAG, "startGameByEnterInvite rsp = $rsp")
        val relayRoomData = RelayRoomData()
        relayRoomData.loadFromRsp(rsp)
        relayRoomData.enterType = RelayRoomData.EnterType.INVITE

        launch(Dispatchers.Main) {
            mRoomData.peerUser = relayRoomData.peerUser
            mRoomData.myEffectModel = relayRoomData.myEffectModel
            mRoomData.peerEffectModel = relayRoomData.peerEffectModel

            roomView.startGameByInvite()
        }

        //如果是邀请进来的需要在这里开启sync
        startSyncGameStatus()
    }

    fun sendUnlock() {
        StatisticsAdapter.recordCountEvent("chorus", "unlock", null)
        MyLog.w(TAG, "解锁爱心")
        val map = HashMap<String, Any>()
        map["roomID"] = mRoomData.gameId

        val body = RequestBody.create(MediaType.parse(ApiManager.APPLICATION_JSON), JSON.toJSONString(map))
        launch {
            var result = subscribe { mRoomServerApi.sendUnlock(body) }
            if (result.errno == 0) {
                val ja = result.data.getJSONArray("userLockInfo")

                for (i in 0 until ja.size) {
                    var userID = ja.getJSONObject(i).getIntValue("userID")
                    var hasLock = ja.getJSONObject(i).getBooleanValue("hasLock")
                    if (userID == MyUserInfoManager.uid.toInt()) {
                        mRoomData.unLockMe = !hasLock
                    } else if (userID == mRoomData.peerUser?.userID) {
                        mRoomData.unLockPeer = !hasLock
                    }
                }
            }
        }
    }

//    @Subscribe(threadMode = ThreadMode.MAIN)
//    fun onEvent(event: MCancelMusic) {
//        MyLog.d(TAG, "onEvent MCancelMusic=$event")
//        pretendSystemMsg(event.cancelMusicMsg)
//    }


}