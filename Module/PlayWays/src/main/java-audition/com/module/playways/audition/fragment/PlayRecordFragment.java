package com.module.playways.audition.fragment;


import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.common.base.BaseFragment;
import com.common.core.myinfo.MyUserInfoManager;
import com.common.core.userinfo.UserInfoServerApi;
import com.common.log.MyLog;
import com.common.player.ExoPlayer;
import com.common.player.IPlayer;
import com.common.player.PlayerCallbackAdapter;
import com.common.rxretrofit.ApiManager;
import com.common.rxretrofit.ApiMethods;
import com.common.rxretrofit.ApiObserver;
import com.common.rxretrofit.ApiResult;
import com.common.upload.UploadCallback;
import com.common.upload.UploadParams;
import com.common.upload.UploadTask;
import com.common.utils.ActivityUtils;
import com.common.utils.U;
import com.common.view.DebounceViewClickListener;
import com.common.view.ex.ExTextView;
import com.component.busilib.SkrConfig;
import com.component.busilib.view.SkrProgressView;
import com.component.dialog.ShareWorksDialog;
import com.component.lyrics.LyricsManager;
import com.component.lyrics.LyricsReader;
import com.component.lyrics.widget.AbstractLrcView;
import com.component.lyrics.widget.ManyLyricsView;
import com.component.person.producation.model.ProducationModel;
import com.module.playways.R;
import com.module.playways.room.song.model.SongModel;
import com.trello.rxlifecycle2.android.FragmentEvent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashMap;

import okhttp3.MediaType;
import okhttp3.RequestBody;

import static com.component.lyrics.widget.AbstractLrcView.LRCPLAYERSTATUS_PLAY;

public class PlayRecordFragment extends BaseFragment {

    TextView mTvName;
    LinearLayout mBottomContainer;
    RelativeLayout mBackArea;
    RelativeLayout mOptArea;
    ExTextView mOptTv;
    RelativeLayout mResetArea;
    RelativeLayout mSaveShareArea;
    ExTextView mSaveShareTv;
    ManyLyricsView mManyLyricsView;

    SongModel mSongModel;

    Handler mUiHanlder;

    IPlayer mPlayer;

    boolean mIsPlay = false;

    String mPath;  // 文件路径
    String mUrl;   // 文件上传Url
    int mDuration;  // 作品时长
    int mWorksId;   // 作品id

    ShareWorksDialog mShareWorksDialog;
    SkrProgressView mProgressView;

    boolean mIsOnComplete = false;

    @Override
    public int initView() {
        return R.layout.play_record_fragment_layout;
    }

    @Override
    public void initData(@Nullable Bundle savedInstanceState) {
        mTvName = getRootView().findViewById(R.id.tv_name);
        mBottomContainer = getRootView().findViewById(R.id.bottom_container);
        mBackArea = getRootView().findViewById(R.id.back_area);
        mOptArea = getRootView().findViewById(R.id.opt_area);
        mOptTv = getRootView().findViewById(R.id.opt_tv);
        mResetArea = getRootView().findViewById(R.id.reset_area);
        mSaveShareArea = getRootView().findViewById(R.id.save_share_area);
        mSaveShareTv = getRootView().findViewById(R.id.save_share_tv);
        mManyLyricsView = getRootView().findViewById(R.id.many_lyrics_view);
        mProgressView = getRootView().findViewById(R.id.progress_view);

        mTvName.setText("《" + mSongModel.getItemName() + "》");
        mUiHanlder = new Handler();

        playLyrics(mSongModel);
        playRecord();
        mManyLyricsView.setAuthorName(mSongModel.getUploaderName());

        mBackArea.setOnClickListener(new DebounceViewClickListener() {
            @Override
            public void clickValid(View v) {
                // 返回选歌页面
                if (getActivity() != null) {
                    getActivity().finish();
                }
            }
        });

        mResetArea.setOnClickListener(new DebounceViewClickListener() {
            @Override
            public void clickValid(View v) {
                if (getFragmentDataListener() != null) {
                    getFragmentDataListener().onFragmentResult(0, 0, null, null);
                }
                U.getFragmentUtils().popFragment(PlayRecordFragment.this);
            }
        });

        mOptArea.setOnClickListener(new DebounceViewClickListener() {
            @Override
            public void clickValid(View v) {
                if (mIsPlay) {
                    // 暂停
                    if (mPlayer != null) {
                        mPlayer.pause();
                        mIsPlay = false;
                    }
                    mManyLyricsView.pause();
                    mOptTv.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.audition_bofang), null, null);
                    mOptTv.setText("播放");
                } else {
                    // 播放
                    mManyLyricsView.resume();
                    if (mIsOnComplete) {
                        mPlayer.startPlay(mPath);
                        mIsOnComplete = false;
                    } else {
                        mPlayer.resume();
                    }
                    mPlayer.resume();
                    mIsPlay = true;
                    mOptTv.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.audition_zanting), null, null);
                    mOptTv.setText("暂停");
                }
            }
        });

        mSaveShareArea.setOnClickListener(new DebounceViewClickListener() {
            @Override
            public void clickValid(View v) {
                if (!TextUtils.isEmpty(mUrl) && mWorksId > 0) {
                    if (SkrConfig.getInstance().worksShareOpen()) {
                        showShareDialog(false);
                    } else {

                    }
                } else {
                    mProgressView.setVisibility(View.VISIBLE);
                    saveWorksStep1();
                }
            }
        });

        if (SkrConfig.getInstance().worksShareOpen()) {

        } else {
            mSaveShareTv.setText("保存");
        }

    }

    /**
     * 播放录音
     */
    @Override
    public void setData(int type, @Nullable Object data) {
        super.setData(type, data);
        if (type == 0) {
            mSongModel = (SongModel) data;
        } else if (type == 1) {
            mPath = (String) data;
        }
    }

    LyricsReader mLyricsReader;

    private void playLyrics(SongModel songModel) {
        LyricsManager.INSTANCE
                .loadStandardLyric(songModel.getLyric())
                .compose(bindUntilEvent(FragmentEvent.DESTROY))
                .subscribe(lyricsReader -> {
                    MyLog.d(getTAG(), "playMusic, start play lyric");
                    mManyLyricsView.resetData();
                    mManyLyricsView.initLrcData();
                    lyricsReader.cut(songModel.getRankLrcBeginT(), songModel.getRankLrcEndT());
                    MyLog.d(getTAG(), "getRankLrcBeginT : " + songModel.getRankLrcBeginT());
                    mManyLyricsView.setLyricsReader(lyricsReader);
                    mLyricsReader = lyricsReader;
                    if (mManyLyricsView.getLrcStatus() == AbstractLrcView.LRCSTATUS_LRC && mManyLyricsView.getLrcPlayerStatus() != LRCPLAYERSTATUS_PLAY) {
                        mManyLyricsView.play(songModel.getBeginMs());
                        MyLog.d(getTAG(), "songModel.getBeginMs() : " + songModel.getBeginMs());
                    }
                }, throwable -> MyLog.e(throwable));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(ActivityUtils.ForeOrBackgroundChange event) {
        MyLog.w(getTAG(), event.foreground ? "切换到前台" : "切换到后台");
        if (!event.foreground && mIsPlay) {
            // 暂停
            if (mPlayer != null) {
                mPlayer.pause();
                mIsPlay = false;
            }
            mManyLyricsView.pause();
            mOptTv.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.audition_bofang), null, null);
            mOptTv.setText("播放");
        }
    }


    @Override
    public void onResume() {
        super.onResume();
    }


    @Override
    public void destroy() {
        super.destroy();
        mManyLyricsView.release();
        if (mPlayer != null) {
            mPlayer.release();
        }
        mUiHanlder.removeCallbacksAndMessages(null);
        if (mShareWorksDialog != null) {
            mShareWorksDialog.dismiss(false);
        }
    }

    @Override
    public boolean useEventBus() {
        return true;
    }

    /**
     * 播放录音
     */
    private void playRecord() {
        if (mPlayer != null) {
            mPlayer.reset();
        }
        if (mPlayer == null) {
            mPlayer = new ExoPlayer();
            mPlayer.setCallback(new PlayerCallbackAdapter() {
                @Override
                public void onCompletion() {
                    super.onCompletion();
                    mIsOnComplete = true;
                    mManyLyricsView.seekTo(mSongModel.getBeginMs());
                    mUiHanlder.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mManyLyricsView.pause();
                            mPlayer.pause();
                        }
                    }, 30);

                    mIsPlay = false;
                    mOptTv.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.audition_bofang), null, null);
                    mOptTv.setText("播放");
                }
            });
        }

        mIsPlay = true;
        mPlayer.startPlay(mPath);
    }

    private void saveWorksStep1() {
        UploadTask uploadTask = UploadParams.newBuilder(mPath)
                .setFileType(UploadParams.FileType.audioAi)
                .startUploadAsync(new UploadCallback() {

                    @Override
                    public void onProgressNotInUiThread(long currentSize, long totalSize) {

                    }

                    @Override
                    public void onSuccessNotInUiThread(String url) {
                        MyLog.d(getTAG(), "onSuccess" + " url=" + url);
                        mUrl = url;
                        saveWorksStep2();
                    }

                    @Override
                    public void onFailureNotInUiThread(String msg) {
                        U.getToastUtil().showShort("保存失败");
                        mProgressView.setVisibility(View.GONE);
                        mUrl = "";
                    }
                });
    }

    private void saveWorksStep2() {
        // TODO: 2019/5/22 上传服务器
        HashMap<String, Object> map = new HashMap<>();
        map.put("category", ProducationModel.TYPE_PRACTICE);
        if (mDuration <= 0) {
            // 这是个耗时操作
            mDuration = U.getMediaUtils().getDuration(mPath, 0);
        }
        // 单位毫秒
        map.put("duration", mDuration);
        map.put("songID", mSongModel.getItemID());
        map.put("worksURL", mUrl);
        RequestBody body = RequestBody.create(MediaType.parse(ApiManager.APPLICATION_JSON), JSON.toJSONString(map));

        UserInfoServerApi mUserInfoServerApi = ApiManager.getInstance().createService(UserInfoServerApi.class);
        ApiMethods.subscribe(mUserInfoServerApi.addWorks(body), new ApiObserver<ApiResult>() {
            @Override
            public void process(ApiResult result) {
                if (result.getErrno() == 0) {
                    mWorksId = result.getData().getIntValue("worksID");
                    if (SkrConfig.getInstance().worksShareOpen()) {
                        mSaveShareTv.setText("分享");
                        mSaveShareTv.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.audition_share, 0, 0);
                        showShareDialog(true);
                    } else {
                        mSaveShareTv.setText("已保存");
                        mSaveShareTv.setClickable(false);
                    }
                } else {
                    mWorksId = 0;
                    mProgressView.setVisibility(View.GONE);
                    U.getToastUtil().showShort("保存失败");
                }
            }

            @Override
            public void onError(Throwable e) {
                super.onError(e);
                mWorksId = 0;
                mProgressView.setVisibility(View.GONE);
                U.getToastUtil().showShort("保存失败");
            }

            @Override
            public void onNetworkError(ErrorType errorType) {
                super.onNetworkError(errorType);
                mWorksId = 0;
                mProgressView.setVisibility(View.GONE);
                U.getToastUtil().showShort("保存失败");

            }
        }, this);
    }

    private void showShareDialog(boolean containSaveTips) {
        mProgressView.setVisibility(View.GONE);
        if (mShareWorksDialog != null) {
            mShareWorksDialog.dismiss(false);
        }
        mShareWorksDialog = new ShareWorksDialog(getContext(), mSongModel.getItemName(), containSaveTips);
        mShareWorksDialog.setData((int) MyUserInfoManager.INSTANCE.getUid(), MyUserInfoManager.INSTANCE.getNickName(), MyUserInfoManager.INSTANCE.getAvatar()
                , mSongModel.getItemName(), mUrl, mWorksId);
        mShareWorksDialog.show();
    }

    @Override
    public boolean onBackPressed() {
        return true;
    }
}
