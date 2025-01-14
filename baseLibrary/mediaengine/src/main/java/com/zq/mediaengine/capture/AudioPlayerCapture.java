package com.zq.mediaengine.capture;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.zq.mediaengine.filter.audio.AudioBufSrcPin;
import com.zq.mediaengine.filter.audio.AudioFilterMgt;
import com.zq.mediaengine.filter.audio.AudioResampleFilter;
import com.zq.mediaengine.filter.audio.AudioSLPlayer;
import com.zq.mediaengine.filter.audio.AudioTrackPlayer;
import com.zq.mediaengine.filter.audio.IPcmPlayer;
import com.zq.mediaengine.framework.AVConst;
import com.zq.mediaengine.framework.AudioBufFormat;
import com.zq.mediaengine.framework.AudioBufFrame;
import com.zq.mediaengine.framework.SinkPin;
import com.zq.mediaengine.framework.SrcPin;
import com.zq.mediaengine.util.StcMgt;
import com.zq.mediaengine.util.audio.AudioUtil;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

/**
 * Audio player capture.
 */
public class AudioPlayerCapture {
    private final static String TAG = "AudioPlayerCapture";
    private final static boolean VERBOSE = false;

    /**
     * The constant AUDIO_PLAYER_TYPE_AUDIOTRACK.
     */
    public final static int AUDIO_PLAYER_TYPE_AUDIOTRACK = 0;
    /**
     * The constant AUDIO_PLAYER_TYPE_OPENSLES.
     */
    public final static int AUDIO_PLAYER_TYPE_OPENSLES = 1;

    /**
     * The constant STATE_IDLE.
     */
    public static final int STATE_IDLE = AudioFileCapture.STATE_IDLE;
    /**
     * The constant STATE_PREPARING.
     */
    public static final int STATE_PREPARING = AudioFileCapture.STATE_PREPARING;
    /**
     * The constant STATE_STARTED.
     */
    public static final int STATE_STARTED = AudioFileCapture.STATE_STARTED;
    /**
     * The constant STATE_STOPPING.
     */
    public static final int STATE_STOPPING = AudioFileCapture.STATE_STOPPING;

    /**
     * The constant ERROR_UNKNOWN.
     */
    public static final int ERROR_UNKNOWN = AudioFileCapture.ERROR_UNKNOWN;
    /**
     * The constant ERROR_IO.
     */
    public static final int ERROR_IO = AudioFileCapture.ERROR_IO;
    /**
     * The constant ERROR_UNSUPPORTED.
     */
    public static final int ERROR_UNSUPPORTED = AudioFileCapture.ERROR_UNSUPPORTED;

    private SrcPin<AudioBufFrame> mSrcPin;
    private AudioResampleFilter mAudioResampleFilter;
    private AudioFilterMgt mAudioFilterMgt;
    private AudioFileCapture mAudioFileCapture;

    private Context mContext;
    private IPcmPlayer mPcmPlayer;
    private StcMgt mStcMgt;
    private AudioBufFormat mOutFormat;
    private int mAudioPlayerType = AUDIO_PLAYER_TYPE_AUDIOTRACK;
    private boolean mEnableLowLatency = false;
    private boolean mPlayerTypeChanged = false;
    private boolean mMute = false;
    private float mPlayoutVolume = 1.0f;
    private boolean mMuteChanged = false;
    private boolean mVolumeChanged = false;

    private Handler mMainHandler;
    private int mLoopCount;
    private int mLoopedCount;

    // 动态切换播放器时的播放位置修正参数
    private long mSwitchPositionOffset;

    // 播放完成事件精确测量时的超时保护
    private long mCompletionCheckDelayed;

    // 混音的延迟测试
    private boolean mEnableLatencyTest;
    private long mStartTime;

    private OnPreparedListener mOnPreparedListener;
    private OnFirstAudioFrameDecodedListener mOnFirstAudioFrameDecodedListener;
    private OnCompletionListener mOnCompletionListener;
    private OnErrorListener mOnErrorListener;

    /**
     * The interface on media prepared listener.
     */
    public interface OnPreparedListener {
        /**
         * On prepared.
         *
         * @param audioPlayerCapture the AudioPlayerCapture instance
         */
        void onPrepared(AudioPlayerCapture audioPlayerCapture);
    }

    /**
     * The interface on fist audio frame decoded listener.
     */
    public interface OnFirstAudioFrameDecodedListener {
        /**
         * On prepared.
         *
         * @param audioFileCapture the AudioFileCapture instance
         * @param time time in ms when first audio frame decoded
         */
        void onFirstAudioFrameDecoded(AudioPlayerCapture audioFileCapture, long time);
    }

    /**
     * The interface On completion listener.
     */
    public interface OnCompletionListener {
        /**
         * On completion.
         *
         * @param audioPlayerCapture the AudioPlayerCapture instance
         */
        void onCompletion(AudioPlayerCapture audioPlayerCapture);
    }

    /**
     * The interface on audio play error listener.
     */
    public interface OnErrorListener {
        /**
         * On error.
         *
         * @param audioPlayerCapture the AudioPlayerCapture intance
         * @param type               the error type
         * @param msg                the extra msg
         */
        void onError(AudioPlayerCapture audioPlayerCapture, int type, long msg);
    }

    /**
     * Instantiates a new Audio player capture.
     *
     * @param context the context
     */
    public AudioPlayerCapture(Context context) {
        mContext = context;
        mSrcPin = new AudioBufSrcPin();
        mStcMgt = new StcMgt();
        mMainHandler = new Handler(Looper.getMainLooper());
        mAudioFileCapture = new AudioFileCapture(context);
        setupListeners();

        mAudioResampleFilter = new AudioResampleFilter();
        mAudioFilterMgt = new AudioFilterMgt();
        SinkPin<AudioBufFrame> playerSinkPin = new SinkPin<AudioBufFrame>() {
            AudioBufFormat mPcmOutFormat = null;

            @Override
            public void onFormatChanged(Object format) {
                mOutFormat = (AudioBufFormat) format;

                // 动态切换OpenSL/AudioTrack
                if (mPcmPlayer != null) {
                    mPcmPlayer.stop();
                    mPcmPlayer.release();
                    mPcmPlayer = null;
                }

                // player switch position correction
                mSwitchPositionOffset = mAudioFileCapture.getPosition() - mAudioFileCapture.getBasePosition();
                Log.d(TAG, "mSwitchPositionOffset: " + mSwitchPositionOffset);

                // open pcm player
                if (mAudioPlayerType == AUDIO_PLAYER_TYPE_OPENSLES) {
                    mPcmPlayer = new AudioSLPlayer();
                } else {
                    mPcmPlayer = new AudioTrackPlayer();
                }
                int atomSize = AudioUtil.getNativeBufferSize(mContext, mOutFormat.sampleRate);
                // use big fifo size to avoid distortion caused by GC
                int err = mPcmPlayer.config(mOutFormat.sampleFormat, mOutFormat.sampleRate,
                        mOutFormat.channels, atomSize, 300);
                if (err < 0) {
                    postError(ERROR_UNKNOWN, 0);
                }
                mPcmPlayer.setMute(mMute);
                mPcmPlayer.setVolume(mPlayoutVolume);
                mPcmPlayer.start();

                mPcmOutFormat = new AudioBufFormat(mOutFormat);
                mPcmOutFormat.nativeModule = mEnableLowLatency ? mPcmPlayer.getNativeInstance() : 0;
                mSrcPin.onFormatChanged(mPcmOutFormat);
            }

            @Override
            public void onFrameAvailable(AudioBufFrame frame) {
                handlePlayerTypeChanged();
                if (frame.buf != null && frame.buf.limit() > 0) {
                    if (mMuteChanged) {
                        mMuteChanged = false;
                        mPcmPlayer.setMute(mMute);
                    }
                    if (mVolumeChanged) {
                        mVolumeChanged = false;
                        mPcmPlayer.setVolume(mPlayoutVolume);
                    }

                    if (mEnableLatencyTest) {
                        ByteBuffer buffer = frame.buf;
                        long now = System.nanoTime() / 1000;
                        ShortBuffer shortBuffer = buffer.asShortBuffer();

                        // clear buffer
                        for (int i = 0; i < shortBuffer.limit(); i++) {
                            shortBuffer.put(i, (short) 0);
                        }

                        // trigger impulse
                        if ((now - mStartTime) >= 5000000L) {
                            mStartTime = now;
                            for (int i = 0; i < 4; i++) {
                                shortBuffer.put(i, Short.MAX_VALUE);
                            }
                        }
                        shortBuffer.rewind();
                    }

                    // write audio data in blocking mode
                    mPcmPlayer.write(frame.buf);

                    // ignore flag frames which should not be delivered
                    AudioBufFrame outFrame = new AudioBufFrame(frame);
                    outFrame.format = mPcmOutFormat;
                    mSrcPin.onFrameAvailable(outFrame);

                    // 更新时钟
                    long position = mAudioFileCapture.getBasePosition() + mSwitchPositionOffset + mPcmPlayer.getPosition();
                    mStcMgt.updateStc(position, true);

                    if (VERBOSE) {
                        long pos = mAudioFileCapture.getPosition();
                        Log.i(TAG, "pos: " + pos + " position: " + position);
                    }
                }
            }
        };
        // 默认不做resample
        mAudioResampleFilter.setOutFormat(new AudioBufFormat(-1, -1, -1));
        mAudioFileCapture.getSrcPin().connect(mAudioResampleFilter.getSinkPin());
        mAudioResampleFilter.getSrcPin().connect(mAudioFilterMgt.getSinkPin());
        mAudioFilterMgt.getSrcPin().connect(playerSinkPin);
    }

    private Runnable mCheckCompletionRunnable = new Runnable() {
        @Override
        public void run() {
            if (mAudioFileCapture.getState() != AudioFileCapture.STATE_STARTED) {
                return;
            }
            long tm = mAudioFileCapture.getPosition() - mAudioFileCapture.getBasePosition() - mSwitchPositionOffset;
            long pos = mPcmPlayer.getPosition();
            long delay = tm - pos;
            Log.d(TAG, "check completion: " + tm + " - " + pos + " = " + delay);

            // 等待超过600ms时也发送播放完成事件
            if (delay < 10 || mCompletionCheckDelayed >= 600) {
                mCompletionCheckDelayed = 0;
                if (mLoopCount < 0 || ++mLoopedCount < mLoopCount) {
                    seek(0);
                } else {
                    postOnCompletion();
                }
            } else {
                mCompletionCheckDelayed += delay;
                mAudioFileCapture.getWorkHandler().postDelayed(mCheckCompletionRunnable, delay);
            }
        }
    };

    private void setupListeners() {
        // onPrepared
        mAudioFileCapture.setOnPreparedListener(new AudioFileCapture.OnPreparedListener() {
            @Override
            public void onPrepared(AudioFileCapture audioFileCapture) {
                mAudioFileCapture.start();
                if (mOnPreparedListener != null) {
                    mOnPreparedListener.onPrepared(AudioPlayerCapture.this);
                }
            }
        });
        // onFirstFrameDecoded
        mAudioFileCapture.setOnFirstAudioFrameDecodedListener(new AudioFileCapture.OnFirstAudioFrameDecodedListener() {
            @Override
            public void onFirstAudioFrameDecoded(AudioFileCapture audioFileCapture, long time) {
                if (mOnFirstAudioFrameDecodedListener != null) {
                    mOnFirstAudioFrameDecodedListener.onFirstAudioFrameDecoded(AudioPlayerCapture.this, time);
                }
            }
        });
        // onCompletion
        mAudioFileCapture.setOnCompletionListener(new AudioFileCapture.OnCompletionListener() {
            @Override
            public void onCompletion(AudioFileCapture audioFileCapture) {
                mCompletionCheckDelayed = 0;
                mAudioFileCapture.getWorkHandler().post(mCheckCompletionRunnable);
            }
        });
        // onError
        mAudioFileCapture.setOnErrorListener(new AudioFileCapture.OnErrorListener() {
            @Override
            public void onError(AudioFileCapture audioFileCapture, int type, long msg) {
                if (mOnErrorListener != null) {
                    mOnErrorListener.onError(AudioPlayerCapture.this, type, msg);
                }
            }
        });
    }

    /**
     * Gets src pin.
     *
     * @return the src pin
     */
    public SrcPin<AudioBufFrame> getSrcPin() {
        return mSrcPin;
    }

    /**
     * Get current state.
     *
     * @return current state
     */
    public int getState() {
        return mAudioFileCapture.getState();
    }

    /**
     * Gets audio filter mgt.
     *
     * @return the audio filter mgt
     */
    public AudioFilterMgt getAudioFilterMgt() {
        return mAudioFilterMgt;
    }

    /**
     * Set audio player type.
     *
     * @param type type in AUDIO_PLAYER_TYPE_AUDIOTRACK or AUDIO_PLAYER_TYPE_OPENSLES.
     */
    public void setAudioPlayerType(int type) {
        mPlayerTypeChanged |= (mAudioPlayerType != type);
        mAudioPlayerType = type;
    }

    /**
     * Set audio player should use low latency mode with type OPENSLES.
     */
    public void setEnableLowLatency(boolean enableLowLatency) {
        mPlayerTypeChanged |= (mEnableLowLatency != enableLowLatency);
        mEnableLowLatency = enableLowLatency;
    }

    /**
     * Set audio player output format
     *
     * @param format output format
     */
    public void setOutFormat(AudioBufFormat format) {
        if (format != null) {
            mAudioResampleFilter.setOutFormat(format);
        }
    }

    /**
     * Sets mute.
     *
     * @param mute true to mute, false to unmute
     */
    public void setMute(boolean mute) {
        mMuteChanged |= (mMute != mute);
        mMute = mute;
    }

    /**
     * Gets mute.
     *
     * @return the mute state
     */
    public boolean getMute() {
        return mMute;
    }

    /**
     * Sets volume.
     *
     * @param volume the volume, should be 0.0f-1.0f
     */
    public void setVolume(float volume) {
        Log.d(TAG, "setVolume: " + volume);
        mAudioFileCapture.setVolume(volume);
    }

    /**
     * Gets volume.
     *
     * @return the volume, should in 0.0f-1.0f
     */
    public float getVolume() {
        return mAudioFileCapture.getVolume();
    }

    public void setPlayoutVolume(float volume) {
        Log.d(TAG, "setPlayoutVolume: " + volume);
        mVolumeChanged |= (mPlayoutVolume != volume);
        mPlayoutVolume = volume;
    }

    public float getPlayoutVolume() {
        return mPlayoutVolume;
    }

    public void setEnableLatencyTest(boolean enableLatencyTest) {
        mEnableLatencyTest = enableLatencyTest;
    }

    public boolean getEnableLatencyTest() {
        return mEnableLatencyTest;
    }

    public int getRemainedLoopCount() {
        if (mLoopCount <= 0) {
            return mLoopCount;
        } else {
            return mLoopCount - mLoopedCount;
        }
    }

    /**
     * Sets on prepared listener.
     *
     * @param listener the listener
     */
    public void setOnPreparedListener(OnPreparedListener listener) {
        mOnPreparedListener = listener;
    }

    /**
     * Sets on first audio frame decoded listener.
     *
     * @param listener the listener
     */
    public void setOnFirstAudioFrameDecodedListener(OnFirstAudioFrameDecodedListener listener) {
        mOnFirstAudioFrameDecodedListener = listener;
    }

    /**
     * Sets on completion listener.
     *
     * @param listener the listener
     */
    public void setOnCompletionListener(OnCompletionListener listener) {
        mOnCompletionListener = listener;
    }

    /**
     * Sets on error listener.
     *
     * @param listener the listener
     */
    public void setOnErrorListener(OnErrorListener listener) {
        mOnErrorListener = listener;
    }

    /**
     * Start audio player.
     *
     * @param url the url.
     *            prefix "file://" for absolute path,
     *            and prefix "assets://" for resource in assets folder,
     *            also prefix "http://", "https://"  supported.
     */
    public void start(String url) {
        start(url, 1);
    }

    /**
     * Start audio player.
     *
     * @param url  the url.
     *             prefix "file://" for absolute path,
     *             and prefix "assets://" for resource in assets folder,
     *             also prefix "http://", "https://"  supported.
     * @param loopCount set loop count, <0 for infinity loop.
     */
    public void start(String url, int loopCount) {
        start(url, 0, -1, loopCount);
    }

    /**
     * Start audio player.
     *
     * @param url  the url.
     *             prefix "file://" for absolute path,
     *             and prefix "assets://" for resource in assets folder,
     *             also prefix "http://", "https://"  supported.
     * @param offset 配置当前音频文件的实际播放开始时间，小于0时按0计算
     * @param end 配置当前音频文件的实际播放完成时间，小于0或者大于音频长度时，按音频长度计算
     * @param loopCount set loop count, <0 for infinity loop.
     */
    public void start(String url, long offset, long end, int loopCount) {
        mLoopCount = loopCount;
        mLoopedCount = 0;
        mSwitchPositionOffset = 0;
        mAudioFileCapture.setDataSource(url, offset, end);
        mAudioFileCapture.prepareAsync();
    }

    /**
     * Stop.
     */
    public void stop() {
        mAudioFileCapture.stop(new Runnable() {
            @Override
            public void run() {
                mPcmPlayer.stop();
                mStcMgt.reset();

                // stop后发送 DETACH_NATIVE_MODULE 事件
                AudioBufFrame frame = new AudioBufFrame(mOutFormat, null, 0);
                frame.flags |= AVConst.FLAG_DETACH_NATIVE_MODULE;
                mSrcPin.onFrameAvailable(frame);

                // 发送 DETACH_NATIVE_MODULE 事件后再销毁
                mPcmPlayer.release();
                mPcmPlayer = null;
            }
        });
    }

    /**
     * Pause.
     */
    public void pause() {
        mAudioFileCapture.pause(new Runnable() {
            @Override
            public void run() {
                mPcmPlayer.pause();
                mStcMgt.pause();
            }
        });
    }

    /**
     * Resume.
     */
    public void resume() {
        mAudioFileCapture.start(new Runnable() {
            @Override
            public void run() {
                mPcmPlayer.resume();
                mStcMgt.start();
            }
        });
    }

    /**
     * Seek.
     *
     * @param ms the time seek to, in miliseconds
     */
    public void seek(final long ms) {
        mAudioFileCapture.seek(ms, new Runnable() {
            @Override
            public void run() {
                mPcmPlayer.flush();
                mStcMgt.pause();
                mStcMgt.updateStc(ms);
            }
        });
    }

    /**
     * Release.
     */
    public void release() {
        Log.d(TAG, "release");
        // AudioFileCapture的release是异步的，这里先释放后面的模块
        mSrcPin.disconnect(true);

        stop();
        mAudioFileCapture.release();
    }

    /**
     * Gets duration.
     *
     * @return the duration in milisenconds
     */
    public long getDuration() {
        return mAudioFileCapture.getDuration();
    }

    /**
     * Gets position.
     *
     * @return current position in miliseconds
     */
    public long getPosition() {
        return mStcMgt.getCurrentStc();
    }

    private void postOnCompletion() {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mOnCompletionListener != null) {
                    mOnCompletionListener.onCompletion(AudioPlayerCapture.this);
                }
            }
        });
    }

    private void postError(final int err, final long msg) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mOnErrorListener != null) {
                    mOnErrorListener.onError(AudioPlayerCapture.this, err, msg);
                }
            }
        });
    }

    private void handlePlayerTypeChanged() {
        if (!mPlayerTypeChanged) {
            return;
        }
        // 支持动态切换audiotrack和opensles播放
        mPlayerTypeChanged = false;
        if ((mAudioPlayerType == AUDIO_PLAYER_TYPE_OPENSLES) ||
                (mAudioPlayerType == AUDIO_PLAYER_TYPE_AUDIOTRACK &&
                        mPcmPlayer instanceof AudioSLPlayer)) {
            if (mOutFormat != null) {
                // send detach event
                AudioBufFrame aFrame = new AudioBufFrame(mOutFormat, null, 0);
                aFrame.flags |= AVConst.FLAG_DETACH_NATIVE_MODULE;
                mSrcPin.onFrameAvailable(aFrame);
                mAudioFilterMgt.getSinkPin().onFormatChanged(mOutFormat);
            }
        }
    }
}
