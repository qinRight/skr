package com.zq.engine.avstatistics;


import com.zq.engine.avstatistics.datastruct.ILogItem;
import com.zq.engine.avstatistics.datastruct.SAgora;
import com.zq.engine.avstatistics.datastruct.SAgoraUserEvent;
import com.zq.engine.avstatistics.datastruct.Skr;

import java.util.ArrayList;
import java.util.List;

import io.agora.rtc.IRtcEngineEventHandler;

import static com.zq.engine.avstatistics.datastruct.SAgoraUserEvent.EVENT_TYPE_onAudioRouteChanged;
import static io.agora.rtc.Constants.AUDIO_ROUTE_HEADSET;
import static io.agora.rtc.Constants.AUDIO_ROUTE_HEADSETBLUETOOTH;
import static io.agora.rtc.Constants.AUDIO_ROUTE_SPEAKERPHONE;

public class SDataHolderEx {
    private final static String TAG = "[SLS]SDataHolderEx";

    private List<ILogItem> mItemList = new ArrayList<ILogItem>();

    public SDataHolderEx() {

    }

    int numRtcStats = 0;

    synchronized public void addRtcStats(IRtcEngineEventHandler.RtcStats s) {
        if (numRtcStats <= 2) {
            SAgora.SRTCStats n = new SAgora.SRTCStats();
            n.timeStamp = System.currentTimeMillis();
            n.cpuAppUsage = s.cpuAppUsage;
            n.cpuTotalUsage = s.cpuTotalUsage;
            n.totalDuration = s.totalDuration;
            n.txBytes = s.txBytes;
            n.rxBytes = s.rxBytes;
            n.txKBitRate = s.txKBitRate;
            n.rxKBitRate = s.rxKBitRate;
            n.txAudioKBitRate = s.txAudioKBitRate;
            n.rxAudioKBitRate = s.rxAudioKBitRate;
            n.users = s.users;
            n.lastmileDelay = s.lastmileDelay;
            n.txPacketLossRate = s.txPacketLossRate;
            n.rxPacketLossRate = s.rxPacketLossRate;
            mItemList.add(n);
            numRtcStats++;
        }
        return;
    }

    int numLocalVideoStats = 0;

    synchronized public void addLocalVideoStats(IRtcEngineEventHandler.LocalVideoStats s) {
        if (numLocalVideoStats <= 2) {
            SAgora.SLocalVideoStats n = new SAgora.SLocalVideoStats();
            n.timeStamp = System.currentTimeMillis();
            n.sentBitrate = s.sentBitrate;
            n.sentFrameRate = s.sentFrameRate;
            n.encoderOutputFrameRate = s.encoderOutputFrameRate;
            n.rendererOutputFrameRate = s.rendererOutputFrameRate;
            n.targetBitrate = s.targetBitrate;
            n.targetFrameRate = s.targetFrameRate;
            n.qualityAdaptIndication = s.qualityAdaptIndication;
            mItemList.add(n);
            numLocalVideoStats++;
        }
        return;
    }

    int numRemoteAudioStats = 0;

    synchronized public void addRemoteAudioStats(IRtcEngineEventHandler.RemoteAudioStats s) {
        if (numRemoteAudioStats <= 2) {
            SAgora.SRemoteAudioStats n = new SAgora.SRemoteAudioStats();
            n.timeStamp = System.currentTimeMillis();
            n.uid = s.uid;
            n.quality = s.quality;
//        n.strQuality =  transNetQuality(s.quality);
            n.networkTransportDelay = s.networkTransportDelay;
            n.jitterBufferDelay = s.jitterBufferDelay;
            n.audioLossRate = s.audioLossRate;
            mItemList.add(n);
            numRemoteAudioStats++;
        }
        return;
    }

    int numRemoteVideoStats = 0;

    synchronized public void addRemoteVideoStats(IRtcEngineEventHandler.RemoteVideoStats s) {
        if (numRemoteVideoStats <= 2) {
            SAgora.SRemoteVideoStats n = new SAgora.SRemoteVideoStats();
            n.timeStamp = System.currentTimeMillis();
            n.uid = s.uid;
            n.width = s.width;
            n.height = s.height;
            n.receivedBitrate = s.receivedBitrate;
            n.decoderOutputFrameRate = s.decoderOutputFrameRate;
            n.rendererOutputFrameRate = s.rendererOutputFrameRate;
            n.rxStreamType = s.rxStreamType;
            mItemList.add(n);
            numRemoteVideoStats++;
        }
        return;
    }

    int numNetQualityStats = 0;

    synchronized public void addNetQualityStats(int uid, int txQuality, int rxQuality) {
        if (numNetQualityStats <= 2) {
            SAgora.SNetworkQuality n = new SAgora.SNetworkQuality();
            n.timeStamp = System.currentTimeMillis();
            n.uid = uid;
            n.txQuality = txQuality;
            n.rxQuality = rxQuality;
            mItemList.add(n);
            numNetQualityStats++;
        }
        return;
    }

    int numRemoteAudioTransStats = 0;

    synchronized public void addRemoteAudioTransStats(int uid, int delay, int lost, int rxKBitRate) {
        if (numRemoteAudioTransStats <= 2) {
            SAgora.SRemoteAudioTransportStats n = new SAgora.SRemoteAudioTransportStats();
            n.timeStamp = System.currentTimeMillis();
            n.uid = uid;
            n.delay = delay;
            n.lost = lost;
            n.rxKBitRate = rxKBitRate;
            mItemList.add(n);
            numRemoteAudioTransStats++;
        }
        return;
    }

    int numRemoteVideoTransStata = 0;

    synchronized public void addRemoteVideoTransStata(int uid, int delay, int lost, int rxKBitRate) {
        if (numRemoteVideoTransStata <= 2) {
            SAgora.SRemoteVideoTransportStat n = new SAgora.SRemoteVideoTransportStat();
            n.timeStamp = System.currentTimeMillis();
            n.uid = uid;
            n.delay = delay;
            n.lost = lost;
            n.rxKBitRate = rxKBitRate;
            mItemList.add(n);
            numRemoteVideoTransStata++;
        }
        return;
    }

    //for user event
    synchronized public void addUserEvent(SAgoraUserEvent e) {
        if (null == e) return;
        mItemList.add(e);
        return;
    }

    int numPingInfo = 0;

    synchronized public void addPingInfo(Skr.PingInfo e) {
        if (null == e) return;
        if (numPingInfo <= 2) {
            e.ts = System.currentTimeMillis();
            mItemList.add(e);
            numPingInfo++;
        }
        return;
    }

    int numNetworkInfo = 0;

    synchronized public void addNetworkInfo(Skr.NetworkInfo e) {
        if (null == e) return;
        if (numNetworkInfo <= 2) {
            e.ts = System.currentTimeMillis();
            mItemList.add(e);
            numNetworkInfo++;
        }
        return;
    }

    // 伴奏播放
    int numPlayerStartInfo = 0;

    synchronized public void addPlayerStartInfo(Skr.PlayerStartInfo info) {
        if (info == null) return;
        if (numPlayerStartInfo <= 10) {
            info.ts = System.currentTimeMillis();
            mItemList.add(info);
            numPlayerStartInfo++;
        }
    }

    int numPlayerStopInfo = 0;

    synchronized public void addPlayerStopInfo(Skr.PlayerStopInfo info) {
        if (info == null) return;
        if (numPlayerStopInfo <= 10) {
            info.ts = System.currentTimeMillis();
            mItemList.add(info);
            numPlayerStopInfo++;
        }
    }

    int numAudioCaptureEvent = 0;

    synchronized public void addAudioCaptureEvent(Skr.AudioCaptureEvent event) {
        if (event == null) return;
        if (numAudioCaptureEvent <= 10) {
            event.ts = System.currentTimeMillis();
            mItemList.add(event);
            numAudioCaptureEvent++;
        }
    }

    public final static int AR_PHONE_SPEAKER = 1;
    public final static int AR_BLUETOOTH = 2;
    public final static int AR_HEADSET = 3;

    synchronized public void addAudioRoutine(int type) {//服用SAgoraUserEvent，一起统计
        SAgoraUserEvent n = new SAgoraUserEvent();
        n.ts = System.currentTimeMillis();
        n.type = EVENT_TYPE_onAudioRouteChanged;
        n.uid = -1;

        switch (type) {
            case AR_HEADSET:
                n.event = new SAgoraUserEvent.AudioRouting(AUDIO_ROUTE_HEADSET);
                break;
            case AR_BLUETOOTH:
                n.event = new SAgoraUserEvent.AudioRouting(AUDIO_ROUTE_HEADSETBLUETOOTH);
                break;
            case AR_PHONE_SPEAKER:
                n.event = new SAgoraUserEvent.AudioRouting(AUDIO_ROUTE_SPEAKERPHONE);
                break;
            default:
                return;
        }

        mItemList.add(n);

    }

    synchronized public void addJoinChannelAction(int joinRet, boolean hasServerConfig, boolean isExternalAudio, boolean isOpenSL, boolean enableAudioPreview) {
        SAgora.SJoinChannelAction n = new SAgora.SJoinChannelAction();
        n.ts = System.currentTimeMillis();
        n.ret = joinRet;

        n.hasServerConfig = hasServerConfig ? 1 : 0;
        n.isExternalAudio = isExternalAudio ? 1 : 0;
        n.isOpenSL = isOpenSL ? 1 : 0;
        n.audioPreview = enableAudioPreview ? 1 : 0;

        mItemList.add(n);
    }

    int numSAudioSamplingInfoGroup = 0;

    synchronized public void addSAudioSamplingInfoGroup(SAgora.SAudioSamplingInfoGroup e) {
        if (numSAudioSamplingInfoGroup <= 2) {
            if (0 == e.ts) {
                e.ts = System.currentTimeMillis();
            }
            mItemList.add(e);
            numSAudioSamplingInfoGroup++;
        }
        return;
    }


    synchronized public List<ILogItem> getItemListAndReset() {
        List<ILogItem> itemList = new ArrayList<>(mItemList);
        reset();
        return itemList;
    }

    private void reset() {
        mItemList.clear();
        numRtcStats = 0;
        numLocalVideoStats = 0;
        numRemoteAudioStats = 0;
        numRemoteVideoStats = 0;
        numNetQualityStats = 0;
        numRemoteAudioTransStats = 0;
        numRemoteVideoTransStata = 0;
        numPingInfo = 0;
        numNetworkInfo = 0;
        numSAudioSamplingInfoGroup = 0;
        numPlayerStartInfo = 0;
        numPlayerStopInfo = 0;
        numAudioCaptureEvent = 0;
    }


    private final static int LIST_COUNT_THRESHOLD = 20;

    synchronized public boolean need2Flush() {
        return mItemList.size() >= LIST_COUNT_THRESHOLD;
    }


}