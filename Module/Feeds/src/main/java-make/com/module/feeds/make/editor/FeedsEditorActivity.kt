package com.module.feeds.make.editor

import android.app.Activity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.common.base.BaseActivity
import com.common.core.avatar.AvatarUtils
import com.common.core.myinfo.MyUserInfoManager
import com.common.image.fresco.BaseImageView
import com.common.log.MyLog
import com.common.rx.RxRetryAssist
import com.common.utils.U
import com.common.view.DebounceViewClickListener
import com.common.view.ex.ExImageView
import com.common.view.titlebar.CommonTitleBar
import com.component.lyrics.LyricsManager
import com.component.lyrics.utils.SongResUtils
import com.component.lyrics.widget.ManyLyricsView
import com.engine.EngineEvent
import com.module.RouterConstants
import com.module.feeds.R
import com.module.feeds.detail.view.AutoScrollLyricView
import com.module.feeds.watch.view.FeedsRecordAnimationView
import com.module.feeds.make.FeedsMakeModel
import com.module.feeds.make.view.FeedsEditorVoiceControlPanelView
import com.module.feeds.make.view.VocalAlignControlPannelView
import com.trello.rxlifecycle2.android.ActivityEvent
import com.zq.mediaengine.kit.ZqAudioEditorKit
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


@Route(path = RouterConstants.ACTIVITY_FEEDS_EDITOR)
class FeedsEditorActivity : BaseActivity() {

    var mTitleBar: CommonTitleBar? = null
    var mPlayBtn: ExImageView? = null
    var mSeekBar: SeekBar? = null
    var mAvatarBg: BaseImageView? = null
    var mRadioView: FeedsRecordAnimationView? = null
    var mManyLyricsView: ManyLyricsView? = null
    var mAutoScrollLyricView: AutoScrollLyricView? = null
    var mVoiceControlView: FeedsEditorVoiceControlPanelView? = null
    var mVaControlView: VocalAlignControlPannelView? = null
    var mMenuBg: View? = null
    var mRenshengIv: ExImageView? = null
    var mEffectIv: ExImageView? = null
    var mResetIv: ExImageView? = null
    var mPublishIv: ExImageView? = null

    var mFeedsMakeModel: FeedsMakeModel? = null

    val mZqAudioEditorKit = ZqAudioEditorKit(U.app())

    var mPlayProgressJob: Job? = null

    var mComposeProgressbarVG: ViewGroup? = null

    var mComposeProgressTipsTv: TextView? = null

    override fun initView(savedInstanceState: Bundle?): Int {
        return R.layout.feeds_editor_activity_layout
    }

    override fun initData(savedInstanceState: Bundle?) {
        mFeedsMakeModel = intent.getSerializableExtra("feeds_make_model") as FeedsMakeModel?

        mTitleBar = findViewById(R.id.title_bar)
        mPlayBtn = findViewById(R.id.play_btn)
        mSeekBar = findViewById(R.id.seek_bar)
        mAvatarBg = findViewById(R.id.avatar_bg)
        mRadioView = findViewById(R.id.radio_view)
        mManyLyricsView = findViewById(R.id.many_lyrics_view)
        mAutoScrollLyricView = AutoScrollLyricView(findViewById(R.id.auto_scroll_lyric_view_layout_viewstub))
        mVoiceControlView = findViewById(R.id.voice_control_view)
        mVaControlView = findViewById(R.id.va_control_view)
        mMenuBg = findViewById(R.id.menu_bg)
        mRenshengIv = findViewById(R.id.rensheng_iv)
        mEffectIv = findViewById(R.id.effect_iv)
        mResetIv = findViewById(R.id.reset_iv)
        mPublishIv = findViewById(R.id.publish_iv)
        mComposeProgressbarVG = findViewById(R.id.compose_progressbar_vg)
        mComposeProgressTipsTv = findViewById(R.id.progress_tips_tv)
        mComposeProgressbarVG?.setOnClickListener {
            // 吃掉点击事件
        }

        mTitleBar?.centerTextView?.text = mFeedsMakeModel?.songModel?.songTpl?.songName
        mTitleBar?.leftImageButton?.setOnClickListener(object : DebounceViewClickListener() {
            override fun clickValid(v: View?) {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        })
        mTitleBar?.centerSubTextView?.text = U.getDateTimeUtils().formatVideoTime(mFeedsMakeModel?.recordDuration?.toLong()
                ?: 0L)

        mPlayBtn?.setOnClickListener(object : DebounceViewClickListener() {
            override fun clickValid(v: View?) {
                if (mPlayBtn?.isSelected == true) {
                    pausePreview()
                } else {
                    startPreview()
                }
            }
        })

        mSeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val d = mFeedsMakeModel?.recordDuration ?: 0
                val p = seekBar?.progress ?: 0
                mZqAudioEditorKit.seekTo(p * d / 100)
            }

        })

        AvatarUtils.loadAvatarByUrl(mAvatarBg, AvatarUtils.newParamsBuilder(MyUserInfoManager.getInstance().avatar).setBlur(true).build())
        mRadioView?.setAvatar(MyUserInfoManager.getInstance().avatar)
        mRadioView?.pause()

        if (!TextUtils.isEmpty(mFeedsMakeModel?.songModel?.songTpl?.lrcTs)) {
            LyricsManager.getLyricsManager(U.app())
                    .loadStandardLyric(mFeedsMakeModel?.songModel?.songTpl?.lrcTs)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .compose(this.bindUntilEvent(ActivityEvent.DESTROY))
                    .retryWhen(RxRetryAssist(3, "feed歌词下载失败"))
                    .subscribe({ lyricsReader ->
                        MyLog.w(TAG, "onEventMainThread " + "play")
                        mManyLyricsView?.visibility = View.VISIBLE
                        mManyLyricsView?.initLrcData()
                        mManyLyricsView?.lyricsReader = lyricsReader
                        mManyLyricsView?.seekto(0)
                        mManyLyricsView?.pause()
                    }, { throwable ->
                        MyLog.e(TAG, throwable)
                        MyLog.d(TAG, "歌词下载失败，采用不滚动方式播放歌词")
                    })
        } else {
            mFeedsMakeModel?.songModel?.let {
                mAutoScrollLyricView?.setSongModel(it)
            }
        }


        mVaControlView?.audioEditorKit = mZqAudioEditorKit

        // 面板控制
        mRenshengIv?.setOnClickListener(object : DebounceViewClickListener() {
            override fun clickValid(v: View?) {
                if (mFeedsMakeModel?.withBgm == false) {
                    U.getToastUtil().showShort("清唱模式下无需人声对齐")
                    return
                }
                mRenshengIv?.isSelected = true
                mEffectIv?.isSelected = false
                mVaControlView?.visibility = View.VISIBLE
                mVoiceControlView?.visibility = View.GONE
            }
        })

        mVoiceControlView?.mZqAudioEditorKit = mZqAudioEditorKit

        mEffectIv?.setOnClickListener(object : DebounceViewClickListener() {
            override fun clickValid(v: View?) {
                mRenshengIv?.isSelected = false
                mEffectIv?.isSelected = true
                mVaControlView?.visibility = View.GONE
                mVoiceControlView?.visibility = View.VISIBLE
            }
        })

        mResetIv?.setOnClickListener(object : DebounceViewClickListener() {
            override fun clickValid(v: View?) {
                setResult(Activity.RESULT_OK)
                finish()
            }
        })

        mPublishIv?.setOnClickListener(object : DebounceViewClickListener() {
            override fun clickValid(v: View?) {
                // 开始合成
                mComposeProgressbarVG?.visibility = View.VISIBLE
                mZqAudioEditorKit.startCompose()

                //TODO test
                ARouter.getInstance().build(RouterConstants.ACTIVITY_FEEDS_PUBLISH)
                        .withSerializable("feeds_make_model", mFeedsMakeModel)
                        .navigation()
            }
        })
        mZqAudioEditorKit.setOnPreviewInfoListener(object : ZqAudioEditorKit.OnPreviewInfoListener {
            override fun onStarted() {
            }

            override fun onCompletion() {
            }

            override fun onLoopCount(count: Int) {
            }
        })

        mZqAudioEditorKit.setOnComposeInfoListener(object : ZqAudioEditorKit.OnComposeInfoListener {
            override fun onProgress(progress: Float) {
                launch {
                    mComposeProgressTipsTv?.text = "${progress * 100}%"
                }
            }

            override fun onCompletion() {
                launch {
                    ARouter.getInstance().build(RouterConstants.ACTIVITY_FEEDS_PUBLISH)
                            .withSerializable("feeds_make_model", mFeedsMakeModel)
                            .navigation()
                }
            }
        })

        if (mFeedsMakeModel?.withBgm == true) {
            mVoiceControlView?.mPeopleVoiceIndex = 1
            runBlocking {
                val bgmFileJob = async(Dispatchers.IO) {
                    val file = SongResUtils.getAccFileByUrl(mFeedsMakeModel?.songModel?.songTpl?.bgm)
                    if (file?.exists() == true) {
                        MyLog.d(TAG, "伴奏存在")
                        file
                    } else {
                        for (i in 1..10) {
                            val r = U.getHttpUtils().downloadFileSync(mFeedsMakeModel?.songModel?.songTpl?.bgm, file, true, null)
                            if (r) {
                                MyLog.d(TAG, "伴奏下载成功")
                                break
                            } else {
                                delay(3000)
                            }
                        }
                        file
                    }
                }
                //播放音乐
                mZqAudioEditorKit.setDataSource(0, bgmFileJob.await().path, 0, -1)
                mZqAudioEditorKit.setDataSource(1, mFeedsMakeModel?.recordSavePath, 0, mFeedsMakeModel?.recordDuration
                        ?: -1)
                mZqAudioEditorKit.outputPath = mFeedsMakeModel?.composeSavePath
                startPreview()
                initWhenEngineReady()
            }
        } else {
            mVoiceControlView?.mPeopleVoiceIndex = 0
            mZqAudioEditorKit.setDataSource(0, mFeedsMakeModel?.recordSavePath, 0, mFeedsMakeModel?.recordDuration
                    ?: -1)
            mZqAudioEditorKit.outputPath = mFeedsMakeModel?.composeSavePath
            startPreview()
            initWhenEngineReady()
        }
    }

    private fun initWhenEngineReady() {
        mVoiceControlView?.bindData()
        mVaControlView?.bindData()

        mEffectIv?.isSelected = true
        mVoiceControlView?.visibility = View.VISIBLE
    }

    private fun startPreview() {
        mZqAudioEditorKit.startPreview(1)
        // 预览开始了
        mPlayBtn?.isSelected = true
        mRadioView?.play()
        mManyLyricsView?.play(mZqAudioEditorKit.position.toInt())
        mPlayProgressJob = launch {
            for (i in 1..1000) {
                mTitleBar?.centerSubTextView?.text = U.getDateTimeUtils().formatVideoTime(mZqAudioEditorKit.position)
                mManyLyricsView?.seekto(mZqAudioEditorKit.position.toInt())
                delay(1000)
            }
        }
    }

    private fun pausePreview() {
        mPlayProgressJob?.cancel()
        mPlayBtn?.isSelected = false
        mManyLyricsView?.pause()
        mRadioView?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mZqAudioEditorKit.release()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: EngineEvent) {
        if (event.getType() == EngineEvent.TYPE_MUSIC_PLAY_FINISH) {
        } else if (event.getType() == EngineEvent.TYPE_MUSIC_PLAY_START) {
        }
    }

    override fun canSlide(): Boolean {
        return false
    }

    override fun useEventBus(): Boolean {
        return true
    }
}