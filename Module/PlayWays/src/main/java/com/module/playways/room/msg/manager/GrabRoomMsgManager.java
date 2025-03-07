package com.module.playways.room.msg.manager;

import android.text.TextUtils;

import com.common.core.myinfo.MyUserInfoManager;
import com.common.log.MyLog;
import com.module.playways.room.msg.BasePushInfo;
import com.module.playways.room.msg.event.AccBeginEvent;
import com.module.playways.room.msg.event.AppSwapEvent;
import com.module.playways.room.msg.event.AudioMsgEvent;
import com.module.playways.room.msg.event.CommentMsgEvent;
import com.module.playways.room.msg.event.DynamicEmojiMsgEvent;
import com.module.playways.room.msg.event.ExitGameEvent;
import com.module.playways.room.msg.event.GiftPresentEvent;
import com.module.playways.room.msg.event.JoinNoticeEvent;
import com.module.playways.room.msg.event.MachineScoreEvent;
import com.module.playways.room.msg.event.PkBurstLightMsgEvent;
import com.module.playways.room.msg.event.PkLightOffMsgEvent;
import com.module.playways.room.msg.event.QChangeMusicTagEvent;
import com.module.playways.room.msg.event.QChangeRoomNameEvent;
import com.module.playways.room.msg.event.QChoGiveUpEvent;
import com.module.playways.room.msg.event.QCoinChangeEvent;
import com.module.playways.room.msg.event.QExitGameMsgEvent;
import com.module.playways.room.msg.event.QGameBeginEvent;
import com.module.playways.room.msg.event.QGetSingChanceMsgEvent;
import com.module.playways.room.msg.event.QJoinActionEvent;
import com.module.playways.room.msg.event.QJoinNoticeEvent;
import com.module.playways.room.msg.event.QKickUserReqEvent;
import com.module.playways.room.msg.event.QKickUserResultEvent;
import com.module.playways.room.msg.event.QLightBurstMsgEvent;
import com.module.playways.room.msg.event.QLightOffMsgEvent;
import com.module.playways.room.msg.event.QNoPassSingMsgEvent;
import com.module.playways.room.msg.event.QPkInnerRoundOverEvent;
import com.module.playways.room.msg.event.QRoundAndGameOverMsgEvent;
import com.module.playways.room.msg.event.QRoundOverMsgEvent;
import com.module.playways.room.msg.event.QSyncStatusMsgEvent;
import com.module.playways.room.msg.event.QWantSingChanceMsgEvent;
import com.module.playways.room.msg.event.SpecialEmojiMsgEvent;
import com.module.playways.room.msg.event.VoteResultEvent;
import com.module.playways.room.msg.filter.PushMsgFilter;
import com.zq.live.proto.GrabRoom.AppSwapMsg;
import com.zq.live.proto.GrabRoom.AudioMsg;
import com.zq.live.proto.GrabRoom.CommentMsg;
import com.zq.live.proto.GrabRoom.DynamicEmojiMsg;
import com.zq.live.proto.GrabRoom.ERoomMsgType;
import com.zq.live.proto.GrabRoom.ExitGameAfterPlayMsg;
import com.zq.live.proto.GrabRoom.ExitGameBeforePlayMsg;
import com.zq.live.proto.GrabRoom.ExitGameOutRoundMsg;
import com.zq.live.proto.GrabRoom.GPrensentGiftMsg;
import com.zq.live.proto.GrabRoom.JoinActionMsg;
import com.zq.live.proto.GrabRoom.JoinNoticeMsg;
import com.zq.live.proto.GrabRoom.MachineScore;
import com.zq.live.proto.GrabRoom.PKBLightMsg;
import com.zq.live.proto.GrabRoom.PKMLightMsg;
import com.zq.live.proto.GrabRoom.QBLightMsg;
import com.zq.live.proto.GrabRoom.QCHOGiveUpMsg;
import com.zq.live.proto.GrabRoom.QChangeMusicTag;
import com.zq.live.proto.GrabRoom.QChangeRoomName;
import com.zq.live.proto.GrabRoom.QCoinChangeMsg;
import com.zq.live.proto.GrabRoom.QExitGameMsg;
import com.zq.live.proto.GrabRoom.QGameBeginMsg;
import com.zq.live.proto.GrabRoom.QGetSingChanceMsg;
import com.zq.live.proto.GrabRoom.QJoinActionMsg;
import com.zq.live.proto.GrabRoom.QJoinNoticeMsg;
import com.zq.live.proto.GrabRoom.QKickUserRequestMsg;
import com.zq.live.proto.GrabRoom.QKickUserResultMsg;
import com.zq.live.proto.GrabRoom.QMLightMsg;
import com.zq.live.proto.GrabRoom.QNoPassSingMsg;
import com.zq.live.proto.GrabRoom.QRoundAndGameOverMsg;
import com.zq.live.proto.GrabRoom.QRoundOverMsg;
import com.zq.live.proto.GrabRoom.QSPKInnerRoundOverMsg;
import com.zq.live.proto.GrabRoom.QSyncStatusMsg;
import com.zq.live.proto.GrabRoom.QWantSingChanceMsg;
import com.zq.live.proto.GrabRoom.ReadyNoticeMsg;
import com.zq.live.proto.GrabRoom.RoomMsg;
import com.zq.live.proto.GrabRoom.RoundAndGameOverMsg;
import com.zq.live.proto.GrabRoom.RoundOverMsg;
import com.zq.live.proto.GrabRoom.SpecialEmojiMsg;
import com.zq.live.proto.GrabRoom.SyncStatusMsg;
import com.zq.live.proto.GrabRoom.VoteResultMsg;

import org.greenrobot.eventbus.EventBus;

import java.util.HashSet;

/**
 * 处理所有的RoomMsg
 */
public class GrabRoomMsgManager extends BaseMsgManager<ERoomMsgType, RoomMsg> {

    public final String TAG = "GrabRoomMsgManager";

    private static class ChatRoomMsgAdapterHolder {
        private static final GrabRoomMsgManager INSTANCE = new GrabRoomMsgManager();
    }

    private GrabRoomMsgManager() {

    }

    public static final GrabRoomMsgManager getInstance() {
        return ChatRoomMsgAdapterHolder.INSTANCE;
    }

    /**
     * 处理消息分发
     *
     * @param msg
     */
    public void processRoomMsg(RoomMsg msg) {
        boolean canGo = true;  //是否放行的flag
        for (PushMsgFilter filter : mPushMsgFilterList) {
            canGo = filter.doFilter(msg);
            if (!canGo) {
                MyLog.d(TAG, "processRoomMsg " + msg + "被拦截");
                return;
            }
        }

        processRoomMsg(msg.getMsgType(), msg);
    }

    public void processRoomMsg(ERoomMsgType messageType, RoomMsg msg) {
        BasePushInfo basePushInfo = BasePushInfo.parse(msg);
        MyLog.d(TAG, "processRoomMsg messageType=" + messageType+" timeMs=" + basePushInfo.getTimeMs() );
        if (messageType == ERoomMsgType.RM_COMMENT) {
            processRMComment(basePushInfo, msg.getCommentMsg());
        } else if (messageType == ERoomMsgType.RM_SPECIAL_EMOJI) {
            processRMSpecialEmoji(basePushInfo, msg.getSpecialEmojiMsg());
        } else if (messageType == ERoomMsgType.RM_DYNAMIC_EMOJI) {
            processRMDynamicEmoji(basePushInfo, msg.getDynamicemojiMsg());
        } else if (messageType == ERoomMsgType.RM_AUDIO_MSG) {
            processRMAudio(basePushInfo, msg.getAudioMsg());
        } else if (messageType == ERoomMsgType.RM_JOIN_ACTION) {
            processJoinActionMsg(basePushInfo, msg.getJoinActionMsg());
        } else if (messageType == ERoomMsgType.RM_JOIN_NOTICE) {
            processJoinNoticeMsg(basePushInfo, msg.getJoinNoticeMsg());
        } else if (messageType == ERoomMsgType.RM_READY_NOTICE) {
            processReadyNoticeMsg(basePushInfo, msg.getReadyNoticeMsg());
        } else if (messageType == ERoomMsgType.RM_ROUND_OVER) {
            processRoundOverMsg(basePushInfo, msg.getRoundOverMsg());
        } else if (messageType == ERoomMsgType.RM_ROUND_AND_GAME_OVER) {
            processRoundAndGameOverMsg(basePushInfo, msg.getRoundAndGameOverMsg());
        } else if (messageType == ERoomMsgType.RM_APP_SWAP) {
            processAppSwapMsg(basePushInfo, msg.getAppSwapMsg());
        } else if (messageType == ERoomMsgType.RM_SYNC_STATUS) {
            processSyncStatusMsg(basePushInfo, msg.getSyncStatusMsg());
        } else if (messageType == ERoomMsgType.RM_EXIT_GAME_BEFORE_PLAY) {
            processExitGameBeforePlay(basePushInfo, msg.getExitGameBeforePlayMsg());
        } else if (messageType == ERoomMsgType.RM_EXIT_GAME_AFTER_PLAY) {
            processExitGameAfterPlay(basePushInfo, msg.getExitGameAfterPlayMsg());
        } else if (messageType == ERoomMsgType.RM_EXIT_GAME_OUT_ROUND) {
            processExitGameOutRound(basePushInfo, msg.getExitGameOutRoundMsg());
        } else if (messageType == ERoomMsgType.RM_VOTE_RESULT) {
            processVoteResult(basePushInfo, msg.getVoteResultMsg());
        } else if (messageType == ERoomMsgType.RM_ROUND_MACHINE_SCORE) {
            processMachineScore(basePushInfo, msg.getMachineScore());
        } else if (messageType == ERoomMsgType.RM_ROUND_ACC_BEGIN) {
            processAccBeigin(basePushInfo);
        } else if (messageType == ERoomMsgType.RM_Q_WANT_SING_CHANCE) {
            processQWantSingChanceMsg(basePushInfo, msg.getQWantSingChanceMsg());
        } else if (messageType == ERoomMsgType.RM_Q_GET_SING_CHANCE) {
            processQGetSingChanceMsg(basePushInfo, msg.getQGetSingChanceMsg());
        } else if (messageType == ERoomMsgType.RM_Q_SYNC_STATUS) {
            processQSyncStatusMsg(basePushInfo, msg.getQSyncStatusMsg());
        } else if (messageType == ERoomMsgType.RM_Q_ROUND_OVER) {
            processQRoundOverMsg(basePushInfo, msg.getQRoundOverMsg());
        } else if (messageType == ERoomMsgType.RM_Q_ROUND_AND_GAME_OVER) {
            processQRoundAndGameOverMsg(basePushInfo, msg.getQRoundAndGameOverMsg());
        } else if (messageType == ERoomMsgType.RM_Q_NO_PASS_SING) {
            processQNoPassSingMsg(basePushInfo, msg.getQNoPassSingMsg());
        } else if (messageType == ERoomMsgType.RM_Q_EXIT_GAME) {
            processQExitGameMsg(basePushInfo, msg.getQExitGameMsg());
        } else if (messageType == ERoomMsgType.RM_PK_BLIGHT) {
            processPkBurstLightMsg(basePushInfo, msg.getPkBLightMsg());
        } else if (messageType == ERoomMsgType.RM_PK_MLIGHT) {
            processPkLightOffMsg(basePushInfo, msg.getPkMLightMsg());
        } else if (messageType == ERoomMsgType.RM_Q_BLIGHT) {
            processGrabLightBurstMsg(basePushInfo, msg.getQBLightMsg());
        } else if (messageType == ERoomMsgType.RM_Q_MLIGHT) {
            processGrabLightOffMsg(basePushInfo, msg.getQMLightMsg());
        } else if (messageType == ERoomMsgType.RM_Q_JOIN_NOTICE) {
            processGrabJoinNoticeMsg(basePushInfo, msg.getQJoinNoticeMsg());
        } else if (messageType == ERoomMsgType.RM_Q_JOIN_ACTION) {
            processGrabJoinActionMsg(basePushInfo, msg.getQJoinActionMsg());
        } else if (messageType == ERoomMsgType.RM_Q_KICK_USER_REQUEST) {
            processGrabKickRequest(basePushInfo, msg.getQKickUserRequestMsg());
        } else if (messageType == ERoomMsgType.RM_Q_KICK_USER_RESULT) {
            processGrabKickResult(basePushInfo, msg.getQKickUserResultMsg());
        } else if (messageType == ERoomMsgType.RM_Q_GAME_BEGIN) {
            processGrabGameBegin(basePushInfo, msg.getQGameBeginMsg());
        } else if (messageType == ERoomMsgType.RM_Q_COIN_CHANGE) {
            processGrabCoinChange(basePushInfo, msg.getQCoinChangeMsg());
        } else if (messageType == ERoomMsgType.RM_Q_CHANGE_MUSIC_TAG) {
            processChangeMusicTag(basePushInfo, msg.getQChangeMusicTag());
        } else if (messageType == ERoomMsgType.RM_Q_CHO_GIVEUP) {
            processGrabChoGiveUp(basePushInfo, msg.getQCHOGiveUpMsg());
        } else if (messageType == ERoomMsgType.RM_Q_PK_INNER_ROUND_OVER) {
            processGrabPkRoundOver(basePushInfo, msg.getQSPKInnerRoundOverMsg());
        } else if (messageType == ERoomMsgType.RM_Q_CHANGE_ROOM_NAME) {
            processGrabChangeRoomName(basePushInfo, msg.getQChangeRoomName());
        } else if (messageType == ERoomMsgType.RM_G_PRESENT_GIFT) {
            processSendGiftInfo(basePushInfo, msg.getGPrensentGiftMsg());
        }
    }

    // 评论消息
    public void processRMComment(BasePushInfo info, CommentMsg commentMsg) {
        if (commentMsg == null) {
            MyLog.e(TAG, "processRMComment" + " commentMsg == null");
            return;
        }

        String text = commentMsg.getText();
        if (!TextUtils.isEmpty(text)) {
            EventBus.getDefault().post(new CommentMsgEvent(info, CommentMsgEvent.MSG_TYPE_RECE, commentMsg));
        }
    }

    // 特殊表情消息
    public void processRMSpecialEmoji(BasePushInfo info, SpecialEmojiMsg specialEmojiMsg) {
        if (specialEmojiMsg == null) {
            MyLog.e(TAG, "processRMSpecialEmoji" + " specialEmojiMsg == null");
            return;
        }

        SpecialEmojiMsgEvent specialEmojiMsgEvent = new SpecialEmojiMsgEvent(info);
        specialEmojiMsgEvent.emojiType = specialEmojiMsg.getEmojiType();
        specialEmojiMsgEvent.count = specialEmojiMsg.getCount();
        specialEmojiMsgEvent.action = specialEmojiMsg.getEmojiAction();
        specialEmojiMsgEvent.coutinueId = specialEmojiMsg.getContinueId();

        EventBus.getDefault().post(specialEmojiMsgEvent);
    }

    // 动态表情消息
    public void processRMDynamicEmoji(BasePushInfo info, DynamicEmojiMsg dynamicEmojiMsg) {
        if (dynamicEmojiMsg == null) {
            MyLog.e(TAG, "processRMDynamicEmoji" + " dynamicEmojiMsg == null");
            return;
        }

        EventBus.getDefault().post(new DynamicEmojiMsgEvent(info, DynamicEmojiMsgEvent.MSG_TYPE_RECE, dynamicEmojiMsg));
    }

    private void processRMAudio(BasePushInfo info, AudioMsg audioMsg) {
        if (audioMsg == null) {
            MyLog.e(TAG, "processRMAudio" + " info=" + info + " audioMsg = null");
        }

        EventBus.getDefault().post(new AudioMsgEvent(info, AudioMsgEvent.MSG_TYPE_RECE, audioMsg));
    }


    //////////////////////////////////


    private void processSendGiftInfo(BasePushInfo basePushInfo, GPrensentGiftMsg gPrensentGiftMsg) {
        if (gPrensentGiftMsg != null) {
            if (gPrensentGiftMsg.getSendUserInfo().getUserID() != MyUserInfoManager.INSTANCE.getUid()) {
                GiftPresentEvent giftPresentEvent = new GiftPresentEvent(basePushInfo, gPrensentGiftMsg);
                EventBus.getDefault().post(giftPresentEvent);
            } else {
                MyLog.d(TAG, "processSendGiftInfo" + "自己发的，无需理会");
            }
        } else {
            MyLog.w(TAG, "processSendGiftInfo" + " info=" + basePushInfo + " gPrensentGiftMsg = null");
        }
    }

    private void processGrabChangeRoomName(BasePushInfo basePushInfo, QChangeRoomName qChangeRoomName) {
        if (qChangeRoomName != null) {
            QChangeRoomNameEvent changeRoomNameEvent = new QChangeRoomNameEvent(basePushInfo, qChangeRoomName);
            EventBus.getDefault().post(changeRoomNameEvent);
        } else {
            MyLog.w(TAG, "processGrabChangeRoomName" + " info=" + basePushInfo + " qChangeRoomName = null");
        }
    }

    private void processGrabPkRoundOver(BasePushInfo basePushInfo, QSPKInnerRoundOverMsg qspkInnerRoundOverMsg) {
        if (qspkInnerRoundOverMsg != null) {
            QPkInnerRoundOverEvent qPkInnerRoundOverEvent = new QPkInnerRoundOverEvent(basePushInfo, qspkInnerRoundOverMsg);
            EventBus.getDefault().post(qPkInnerRoundOverEvent);
        } else {
            MyLog.w(TAG, "processGrabPkRoundOver" + " info=" + basePushInfo + " qspkInnerRoundOverMsg = null");
        }
    }

    private void processGrabChoGiveUp(BasePushInfo basePushInfo, QCHOGiveUpMsg qchoGiveUpMsg) {
        if (qchoGiveUpMsg != null) {
            QChoGiveUpEvent joinActionEvent = new QChoGiveUpEvent(basePushInfo, qchoGiveUpMsg);
            EventBus.getDefault().post(joinActionEvent);
        } else {
            MyLog.w(TAG, "processGrabChoGiveUp" + " info=" + basePushInfo + " qchoGiveUpMsg = null");
        }
    }

    //加入游戏指令消息
    private void processJoinActionMsg(BasePushInfo info, JoinActionMsg joinActionMsg) {
//        if (joinActionMsg != null) {
//            JoinActionEvent joinActionEvent = new JoinActionEvent(info, joinActionMsg);
//            EventBus.getDefault().post(joinActionEvent);
//        } else {
//            MyLog.w(TAG, "processJoinActionMsg" + " info=" + info + " joinActionMsg = null");
//        }
    }

    //加入游戏通知消息
    private void processJoinNoticeMsg(BasePushInfo info, JoinNoticeMsg joinNoticeMsg) {
        if (joinNoticeMsg != null) {
            JoinNoticeEvent joinNoticeEvent = new JoinNoticeEvent(info, joinNoticeMsg);
            EventBus.getDefault().post(joinNoticeEvent);
        } else {
            MyLog.w(TAG, "processJoinNoticeMsg" + " info=" + info + " joinNoticeMsg = null");
        }
    }

    //准备游戏通知消息
    private void processReadyNoticeMsg(BasePushInfo info, ReadyNoticeMsg readyNoticeMsg) {
//        if (readyNoticeMsg != null) {
//            ReadyNoticeEvent readyNoticeEvent = new ReadyNoticeEvent(info, readyNoticeMsg);
//            EventBus.getDefault().post(readyNoticeEvent);
//        } else {
//            MyLog.w(TAG, "processReadyNoticeMsg" + " info=" + info + " readyNoticeMsg = null");
//        }
    }

//    //准备并开始游戏通知消息
//    private void processReadyAndStartNoticeMsg(BasePushInfo info, ReadyAndStartNoticeMsg readyAndStartNoticeMsg) {
//        if (readyAndStartNoticeMsg == null) {
//            MyLog.d(TAG, "processReadyAndStartNoticeMsg" + " readyAndStartNoticeMsg == null");
//            return;
//        }
//
//        int readyUserID = readyAndStartNoticeMsg.getReadyUserID();   //准备用户ID
//        long readyTimeMs = readyAndStartNoticeMsg.getReadyTimeMs();  //准备的毫秒时间戳
//        long startTimeMS = readyAndStartNoticeMsg.getStartTimeMS();  //开始的毫秒时间戳
//        int firstUserID = readyAndStartNoticeMsg.getFirstUserID();   //第一个用户ID
//        int firstMusicID = readyAndStartNoticeMsg.getFirstMusicID(); //第一首歌曲ID
//
//        EventBus.getDefault().post(new ReadyAndStartNoticeEvent(info, readyUserID, readyTimeMs, startTimeMS, firstUserID, firstMusicID));
//
//    }

    //游戏轮次结束通知消息
    private void processRoundOverMsg(BasePushInfo info, RoundOverMsg roundOverMsgr) {
//        if (roundOverMsgr != null) {
//            RoundOverEvent roundOverEvent = new RoundOverEvent(info, roundOverMsgr);
//            EventBus.getDefault().post(roundOverEvent);
//        } else {
//            MyLog.w(TAG, "processRoundOverMsg" + " info=" + info + " roundOverMsgr = null");
//        }
    }

    //轮次和游戏结束通知消息
    private void processRoundAndGameOverMsg(BasePushInfo info, RoundAndGameOverMsg roundAndGameOverMsg) {
//        if (roundAndGameOverMsg != null) {
//            RoundAndGameOverEvent roundAndGameOverEvent = new RoundAndGameOverEvent(info, roundAndGameOverMsg);
//            EventBus.getDefault().post(roundAndGameOverEvent);
//        } else {
//            MyLog.w(TAG, "processRoundAndGameOverMsg" + " info=" + info + " roundAndGameOverMsg = null");
//        }
    }

    //app进程后台通知
    private void processAppSwapMsg(BasePushInfo info, AppSwapMsg appSwapMsg) {
        if (appSwapMsg != null) {
            AppSwapEvent appSwapEvent = new AppSwapEvent(info, appSwapMsg);
            EventBus.getDefault().post(appSwapEvent);
        } else {
            MyLog.w(TAG, "processAppSwapMsg" + " info=" + info + " appSwapMsg = null");
        }
    }

    //状态同步信令
    private void processSyncStatusMsg(BasePushInfo info, SyncStatusMsg syncStatusMsg) {
//        if (syncStatusMsg != null) {
//            SyncStatusEvent syncStatusEvent = new SyncStatusEvent(info, syncStatusMsg);
//            EventBus.getDefault().post(syncStatusEvent);
//        } else {
//            MyLog.w(TAG, "processSyncStatusMsg" + " info=" + info + " syncStatusMsg = null");
//        }
    }

    //退出游戏通知, 游戏开始前
    private void processExitGameBeforePlay(BasePushInfo info, ExitGameBeforePlayMsg exitGameBeforePlayMsg) {
        if (exitGameBeforePlayMsg != null) {
            ExitGameEvent exitGameEvent = new ExitGameEvent(info, exitGameBeforePlayMsg);
            EventBus.getDefault().post(exitGameEvent);
        } else {
            MyLog.w(TAG, "processExitGameBeforePlay" + " basePushInfo=" + info + " exitGameBeforePlayMsg = null");
        }
    }

    //退出游戏通知，游戏开始后
    private void processExitGameAfterPlay(BasePushInfo info, ExitGameAfterPlayMsg exitGameAfterPlayMsg) {
        if (exitGameAfterPlayMsg != null) {
            ExitGameEvent exitGameEvent = new ExitGameEvent(info, exitGameAfterPlayMsg);
            EventBus.getDefault().post(exitGameEvent);
        } else {
            MyLog.w(TAG, "processExitGameAfterPlay" + " basePushInfo=" + info + " exitGameAfterPlayMsg = null");
        }
    }

    //退出游戏通知，游戏中非自己轮次
    private void processExitGameOutRound(BasePushInfo info, ExitGameOutRoundMsg exitGameOutRoundMsg) {
        if (exitGameOutRoundMsg != null) {
            ExitGameEvent exitGameEvent = new ExitGameEvent(info, exitGameOutRoundMsg);
            EventBus.getDefault().post(exitGameEvent);
        } else {
            MyLog.w(TAG, "processExitGameOutRound" + " basePushInfo=" + info + " exitGameOutRoundMsg = null");
        }
    }

    //游戏投票结果消息
    private void processVoteResult(BasePushInfo basePushInfo, VoteResultMsg voteResultMsg) {
        if (voteResultMsg != null) {
            VoteResultEvent voteResultEvent = new VoteResultEvent(basePushInfo, voteResultMsg);
            EventBus.getDefault().post(voteResultEvent);
        } else {
            MyLog.w(TAG, "processVoteResult" + " basePushInfo=" + basePushInfo + " voteResultMsg = null");
        }
    }

    // 处理机器打分
    private void processMachineScore(BasePushInfo basePushInfo, MachineScore machineScore) {
        if (machineScore != null) {
            MachineScoreEvent machineScoreEvent = new MachineScoreEvent(basePushInfo, machineScore);
            EventBus.getDefault().post(machineScoreEvent);
        } else {
            MyLog.w(TAG, "processMachineScore" + " basePushInfo=" + basePushInfo + " machineScore = null");
        }
    }

    private void processQWantSingChanceMsg(BasePushInfo basePushInfo, QWantSingChanceMsg qWantSingChanceMsg) {
        if (qWantSingChanceMsg != null) {
            QWantSingChanceMsgEvent machineScoreEvent = new QWantSingChanceMsgEvent(basePushInfo, qWantSingChanceMsg);
            EventBus.getDefault().post(machineScoreEvent);
        } else {
            MyLog.w(TAG, "processQWantSingChanceMsg" + " basePushInfo=" + basePushInfo + " qWantSingChanceMsg = null");
        }
    }

    private void processQGetSingChanceMsg(BasePushInfo basePushInfo, QGetSingChanceMsg qGetSingChanceMsg) {
        if (qGetSingChanceMsg != null) {
            QGetSingChanceMsgEvent machineScoreEvent = new QGetSingChanceMsgEvent(basePushInfo, qGetSingChanceMsg);
            EventBus.getDefault().post(machineScoreEvent);
        } else {
            MyLog.w(TAG, "processQGetSingChanceMsg" + " basePushInfo=" + basePushInfo + " qGetSingChanceMsg = null");
        }
    }

    private void processQSyncStatusMsg(BasePushInfo basePushInfo, QSyncStatusMsg qSyncStatusMsg) {
        if (qSyncStatusMsg != null) {
            QSyncStatusMsgEvent machineScoreEvent = new QSyncStatusMsgEvent(basePushInfo, qSyncStatusMsg);
            EventBus.getDefault().post(machineScoreEvent);
        } else {
            MyLog.w(TAG, "processQSyncStatusMsg" + " basePushInfo=" + basePushInfo + " qSyncStatusMsg = null");
        }
    }

    private void processQRoundOverMsg(BasePushInfo basePushInfo, QRoundOverMsg qRoundOverMsg) {
        if (qRoundOverMsg != null) {
            QRoundOverMsgEvent machineScoreEvent = new QRoundOverMsgEvent(basePushInfo, qRoundOverMsg);
            EventBus.getDefault().post(machineScoreEvent);
        } else {
            MyLog.w(TAG, "processQRoundOverMsg" + " basePushInfo=" + basePushInfo + " qRoundOverMsg = null");
        }
    }

    private void processQRoundAndGameOverMsg(BasePushInfo basePushInfo, QRoundAndGameOverMsg qRoundAndGameOverMsg) {
        if (qRoundAndGameOverMsg != null) {
            QRoundAndGameOverMsgEvent machineScoreEvent = new QRoundAndGameOverMsgEvent(basePushInfo, qRoundAndGameOverMsg);
            EventBus.getDefault().post(machineScoreEvent);
        } else {
            MyLog.w(TAG, "processQRoundAndGameOverMsg" + " basePushInfo=" + basePushInfo + " qRoundAndGameOverMsg = null");
        }
    }

    private void processQNoPassSingMsg(BasePushInfo basePushInfo, QNoPassSingMsg qNoPassSingMsg) {
        if (qNoPassSingMsg != null) {
            QNoPassSingMsgEvent machineScoreEvent = new QNoPassSingMsgEvent(basePushInfo, qNoPassSingMsg);
            EventBus.getDefault().post(machineScoreEvent);
        } else {
            MyLog.w(TAG, "processQNoPassSingMsg" + " basePushInfo=" + basePushInfo + " qNoPassSingMsg = null");
        }
    }

    private void processQExitGameMsg(BasePushInfo basePushInfo, QExitGameMsg qExitGameMsg) {
        if (qExitGameMsg != null) {
            QExitGameMsgEvent machineScoreEvent = new QExitGameMsgEvent(basePushInfo, qExitGameMsg);
            EventBus.getDefault().post(machineScoreEvent);
        } else {
            MyLog.w(TAG, "processQExitGameMsg" + " basePushInfo=" + basePushInfo + " qExitGameMsg = null");
        }
    }

    private void processPkBurstLightMsg(BasePushInfo basePushInfo, PKBLightMsg qNoPassSingMsg) {
        if (qNoPassSingMsg != null) {
            PkBurstLightMsgEvent machineScoreEvent = new PkBurstLightMsgEvent(basePushInfo, qNoPassSingMsg);
            EventBus.getDefault().post(machineScoreEvent);
        } else {
            MyLog.w(TAG, "processPkBurstLightMsg" + " basePushInfo=" + basePushInfo + " qNoPassSingMsg = null");
        }
    }

    private void processPkLightOffMsg(BasePushInfo basePushInfo, PKMLightMsg qExitGameMsg) {
        if (qExitGameMsg != null) {
            PkLightOffMsgEvent machineScoreEvent = new PkLightOffMsgEvent(basePushInfo, qExitGameMsg);
            EventBus.getDefault().post(machineScoreEvent);
        } else {
            MyLog.w(TAG, "processPkLightOffMsg" + " basePushInfo=" + basePushInfo + " qExitGameMsg = null");
        }
    }


    private void processGrabLightOffMsg(BasePushInfo basePushInfo, QMLightMsg msg) {
        if (msg != null) {
            QLightOffMsgEvent event = new QLightOffMsgEvent(basePushInfo, msg);
            EventBus.getDefault().post(event);
        } else {
            MyLog.w(TAG, "processPkLightOffMsg" + " basePushInfo=" + basePushInfo + " qExitGameMsg = null");
        }
    }

    private void processGrabLightBurstMsg(BasePushInfo basePushInfo, QBLightMsg msg) {
        if (msg != null) {
            QLightBurstMsgEvent event = new QLightBurstMsgEvent(basePushInfo, msg);
            EventBus.getDefault().post(event);
        } else {
            MyLog.w(TAG, "processPkLightOffMsg" + " basePushInfo=" + basePushInfo + " qExitGameMsg = null");
        }
    }

    private void processGrabJoinNoticeMsg(BasePushInfo basePushInfo, QJoinNoticeMsg msg) {
        if (msg != null) {
            QJoinNoticeEvent event = new QJoinNoticeEvent(basePushInfo, msg);
            EventBus.getDefault().post(event);
        } else {
            MyLog.w(TAG, "processPkLightOffMsg" + " basePushInfo=" + basePushInfo + " qExitGameMsg = null");
        }
    }

    private void processGrabJoinActionMsg(BasePushInfo basePushInfo, QJoinActionMsg msg) {
        if (msg != null) {
            QJoinActionEvent event = new QJoinActionEvent(basePushInfo, msg);
            EventBus.getDefault().post(event);
        } else {
            MyLog.w(TAG, "processPkLightOffMsg" + " basePushInfo=" + basePushInfo + " qExitGameMsg = null");
        }
    }

    private void processAccBeigin(BasePushInfo basePushInfo) {
        if (basePushInfo != null) {
            AccBeginEvent machineScoreEvent = new AccBeginEvent(basePushInfo, basePushInfo.getSender().getUserID());
            EventBus.getDefault().post(machineScoreEvent);
        } else {
            MyLog.w(TAG, "processAccBeigin" + " basePushInfo = null ");
        }
    }

    private void processGrabKickResult(BasePushInfo basePushInfo, QKickUserResultMsg qKickUserResultMsg) {
        if (basePushInfo != null && qKickUserResultMsg != null) {
            // 过滤，被踢人 也可以放在收事件的地方，但是觉得没有必要
            if (MyUserInfoManager.INSTANCE.getUid() == qKickUserResultMsg.getKickUserID()) {
                QKickUserResultEvent qKickUserResultEvent = new QKickUserResultEvent(basePushInfo, qKickUserResultMsg);
                EventBus.getDefault().post(qKickUserResultEvent);
                return;
            }
            // 过滤下, 所有投同意票
            if (qKickUserResultMsg.getGiveYesVoteUserIDsList() != null) {
                for (Integer integer : qKickUserResultMsg.getGiveYesVoteUserIDsList()) {
                    if (integer == MyUserInfoManager.INSTANCE.getUid()) {
                        QKickUserResultEvent qKickUserResultEvent = new QKickUserResultEvent(basePushInfo, qKickUserResultMsg);
                        EventBus.getDefault().post(qKickUserResultEvent);
                        return;
                    }
                }
            }

            // 过滤下, 所有投不同意票
            if (qKickUserResultMsg.getGiveNoVoteUserIDsList() != null) {
                for (Integer integer : qKickUserResultMsg.getGiveNoVoteUserIDsList()) {
                    if (integer == MyUserInfoManager.INSTANCE.getUid()) {
                        QKickUserResultEvent qKickUserResultEvent = new QKickUserResultEvent(basePushInfo, qKickUserResultMsg);
                        EventBus.getDefault().post(qKickUserResultEvent);
                        return;
                    }
                }
            }

            // 过滤下, 所有未知票
            if (qKickUserResultMsg.getGiveUnknownVoteUserIDsList() != null) {
                for (Integer integer : qKickUserResultMsg.getGiveUnknownVoteUserIDsList()) {
                    if (integer == MyUserInfoManager.INSTANCE.getUid()) {
                        QKickUserResultEvent qKickUserResultEvent = new QKickUserResultEvent(basePushInfo, qKickUserResultMsg);
                        EventBus.getDefault().post(qKickUserResultEvent);
                        return;
                    }
                }
            }
        } else {
            MyLog.w(TAG, "processGrabKickResult" + " basePushInfo = null or qKickUserSuccessMsg = null");
        }
    }

    private void processGrabKickRequest(BasePushInfo basePushInfo, QKickUserRequestMsg qKickUserRequestMsg) {
        if (basePushInfo != null && qKickUserRequestMsg != null) {
            // 过滤下,所有投票者
            if (qKickUserRequestMsg.getOtherOnlineUserIDsList() != null && qKickUserRequestMsg.getOtherOnlineUserIDsList().size() > 0) {
                for (Integer integer : qKickUserRequestMsg.getOtherOnlineUserIDsList()) {
                    if (integer == MyUserInfoManager.INSTANCE.getUid()) {
                        QKickUserReqEvent qKickUserReqEvent = new QKickUserReqEvent(basePushInfo, qKickUserRequestMsg);
                        EventBus.getDefault().post(qKickUserReqEvent);
                        return;
                    }
                }
            } else {
                MyLog.w(TAG, "processGrabKickRequest" + " OtherOnlineUserIDsList() = null");
            }
        } else {
            MyLog.w(TAG, "processGrabKickRequest" + " basePushInfo = null or qKickUserRequestMsg = null");
        }
    }

    private void processGrabGameBegin(BasePushInfo basePushInfo, QGameBeginMsg qGameBeginMsg) {
        if (basePushInfo != null && qGameBeginMsg != null) {
            // 过滤下,所有投票者
            QGameBeginEvent event = new QGameBeginEvent(basePushInfo, qGameBeginMsg);
            EventBus.getDefault().post(event);
        } else {
            MyLog.w(TAG, "processGrabKickRequest" + " basePushInfo = null or qKickUserRequestMsg = null");
        }
    }

    private void processGrabCoinChange(BasePushInfo basePushInfo, QCoinChangeMsg qCoinChangeMsg) {
        if (basePushInfo != null && qCoinChangeMsg != null) {
            QCoinChangeEvent event = new QCoinChangeEvent(basePushInfo, qCoinChangeMsg);
            EventBus.getDefault().post(event);
        } else {
            MyLog.w(TAG, "processGrabKickRequest" + " basePushInfo = null or qKickUserRequestMsg = null");
        }
    }

    private void processChangeMusicTag(BasePushInfo basePushInfo, QChangeMusicTag qChangeMusicTag) {
        if (basePushInfo != null && qChangeMusicTag != null) {
            QChangeMusicTagEvent event = new QChangeMusicTagEvent(basePushInfo, qChangeMusicTag);
            EventBus.getDefault().post(event);
        } else {
            MyLog.w(TAG, "processChangeMusicTag" + " basePushInfo = null or QChangeMusicTag = null");
        }
    }
}
