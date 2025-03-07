package com.module.feeds.make.editor

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.view.Gravity
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.common.base.BaseActivity
import com.common.core.avatar.AvatarUtils
import com.common.core.myinfo.MyUserInfoManager
import com.common.image.fresco.BaseImageView
import com.common.log.MyLog
import com.common.utils.U
import com.common.view.DebounceViewClickListener
import com.common.view.ex.ExImageView
import com.common.view.titlebar.CommonTitleBar
import com.component.busilib.event.FeedSongMakeSucessEvent
import com.component.busilib.view.SkrProgressView
import com.component.lyrics.LyricsManager
import com.component.lyrics.LyricsReader
import com.component.lyrics.utils.SongResUtils
import com.component.lyrics.widget.AbstractLrcView.LRCPLAYERSTATUS_PLAY
import com.component.lyrics.widget.ManyLyricsView
import com.component.lyrics.widget.TxtLyricScrollView
import com.dialog.view.TipsDialogView
import com.module.RouterConstants
import com.module.club.IClubModuleService
import com.module.feeds.R
import com.module.feeds.make.*
import com.module.feeds.make.make.FeedsMakeActivity
import com.module.feeds.make.publish.FeedsPublishActivity
import com.module.feeds.make.view.FeedsEditorVoiceControlPanelView
import com.module.feeds.make.view.VocalAlignControlPannelView
import com.module.feeds.songmanage.activity.FeedSongManagerActivity
import com.module.feeds.songmanage.activity.FeedSongSearchActivity
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder
import com.zq.mediaengine.kit.ZqAudioEditorKit
import com.zq.mediaengine.kit.ZqEngineKit
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus


@Route(path = RouterConstants.ACTIVITY_FEEDS_EDITOR)
class FeedsEditorActivity : BaseActivity() {
    lateinit var rootView: ConstraintLayout
    lateinit var titleBar: CommonTitleBar
    lateinit var playBtnContainer: ConstraintLayout
    lateinit var playBtn: ExImageView
    lateinit var seekBar: SeekBar
    lateinit var nowTsTv: TextView
    lateinit var totalTsTv: TextView
    lateinit var cdContainer: View
    lateinit var cdBg: ImageView
    lateinit var cdAvatar: BaseImageView
    lateinit var txtLyricsView: TxtLyricScrollView
    var manyLyricsView: ManyLyricsView? = null
    lateinit var renshengIv: ExImageView
    lateinit var effectIv: ExImageView
    lateinit var resetIv: ExImageView
    lateinit var progressView: SkrProgressView

    var mFeedsMakeModel: FeedsMakeModel? = null

    val mZqAudioEditorKit = ZqAudioEditorKit(U.app())

    var mPlayProgressJob: Job? = null
    var cdRotateAnimator: ObjectAnimator? = null

    var tipsDialogView: TipsDialogView? = null

    var mFrom: Int? = null

    var familyID: Int? = null

    val voiceControlPanelViewDialog by lazy {
        val view = FeedsEditorVoiceControlPanelView(this).apply {
            this.mZqAudioEditorKit = this@FeedsEditorActivity.mZqAudioEditorKit
            if (mFeedsMakeModel?.withBgm == true) {
                this.mPeopleVoiceIndex = 1
            } else {
                this.mPeopleVoiceIndex = 0
            }
            bindData()
        }
        DialogPlus.newDialog(this)
                .setContentHolder(ViewHolder(view))
                .setContentBackgroundResource(R.color.transparent)
                .setOverlayBackgroundResource(R.color.black_trans_50)
                .setExpanded(false)
                .setCancelable(true)
                .setGravity(Gravity.BOTTOM)
                .create()
    }

    val vocalAlignControlPannelViewDialog by lazy {
        val view = VocalAlignControlPannelView(this).apply {
            this.audioEditorKit = this@FeedsEditorActivity.mZqAudioEditorKit
            bindData()
        }
        DialogPlus.newDialog(this)
                .setContentHolder(ViewHolder(view))
                .setContentBackgroundResource(R.color.transparent)
                .setOverlayBackgroundResource(R.color.black_trans_50)
                .setExpanded(false)
                .setCancelable(true)
                .setGravity(Gravity.BOTTOM)
                .create()
    }

    override fun initView(savedInstanceState: Bundle?): Int {
        return R.layout.feeds_editor_activity_layout
    }

    override fun initData(savedInstanceState: Bundle?) {
        if (intent.hasExtra("from")) {
            mFrom = intent.getIntExtra("from", 0)
        }

        if (intent.hasExtra("familyID")) {
            familyID = intent.getIntExtra("familyID", 0)
        }

        mFeedsMakeModel = sFeedsMakeModelHolder
        sFeedsMakeModelHolder = null
        MyLog.d(TAG, "mFeedsMakeModel=$mFeedsMakeModel")
        if (mFeedsMakeModel == null) {
            finish()
            return
        }
        rootView = findViewById(R.id.root_view)
        titleBar = findViewById(R.id.title_bar)
        playBtnContainer = findViewById(R.id.play_btn_container)
        playBtn = findViewById(R.id.play_btn)
        seekBar = findViewById(R.id.seek_bar)
        nowTsTv = findViewById(R.id.now_ts_tv)
        totalTsTv = findViewById(R.id.total_ts_tv)
        cdContainer = findViewById(R.id.cd_container)
        cdBg = findViewById(R.id.cd_bg)
        cdAvatar = findViewById(R.id.cd_avatar)
        txtLyricsView = findViewById(R.id.txt_lyrics_view)
        manyLyricsView = findViewById(R.id.many_lyrics_view)
        renshengIv = findViewById(R.id.rensheng_iv)
        effectIv = findViewById(R.id.effect_iv)
        resetIv = findViewById(R.id.reset_iv)
        progressView = findViewById(R.id.progress_view)

        cdRotateAnimator = ObjectAnimator.ofFloat(cdContainer, View.ROTATION, 0f, 360f)
        cdRotateAnimator?.duration = 10000
        cdRotateAnimator?.interpolator = LinearInterpolator()
        cdRotateAnimator?.repeatCount = Animation.INFINITE

        titleBar?.centerTextView?.text = mFeedsMakeModel?.songModel?.getDisplayName()

        titleBar?.leftImageButton?.setOnClickListener(object : DebounceViewClickListener() {
            override fun clickValid(v: View?) {
                finishPage()
            }
        })

        playBtnContainer?.setOnClickListener {
            if (playBtn?.isSelected == true) {
                pausePreview()
            } else {
                resumePreview()
            }
        }
        val realDuration = (mFeedsMakeModel?.recordDuration?.toInt()
                ?: 0) - (mFeedsMakeModel?.firstLyricShiftTs ?: 0)
        seekBar?.max = realDuration
        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                mZqAudioEditorKit.seekTo(seekBar?.progress?.toLong() ?: 0)
            }

        })

        AvatarUtils.loadAvatarByUrl(cdAvatar, AvatarUtils.newParamsBuilder(MyUserInfoManager.avatar)
                .setCircle(true)
                .build())

        // 面板控制
        renshengIv?.setOnClickListener(object : DebounceViewClickListener() {
            override fun clickValid(v: View?) {
                if (mFeedsMakeModel?.withBgm == false) {
                    U.getToastUtil().showShort("清唱模式下无需人声对齐")
                    return
                }
                vocalAlignControlPannelViewDialog.show()
            }
        })

        effectIv?.setOnClickListener(object : DebounceViewClickListener() {
            override fun clickValid(v: View?) {
                voiceControlPanelViewDialog.show()
            }
        })

        resetIv?.setOnClickListener(object : DebounceViewClickListener() {
            override fun clickValid(v: View?) {
                tipsDialogView?.dismiss()
                tipsDialogView = TipsDialogView.Builder(this@FeedsEditorActivity)
                        .setMessageTip("确定要重新演唱么?")
                        .setCancelTip("取消")
                        .setCancelBtnClickListener {
                            tipsDialogView?.dismiss()
                        }
                        .setConfirmTip("确认")
                        .setConfirmBtnClickListener {
                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                        .build()
                tipsDialogView?.showByDialog()
            }
        })

        titleBar?.rightCustomView?.setOnClickListener(object : DebounceViewClickListener() {
            override fun clickValid(v: View?) {
                // 开始合成
                pausePreview()
                progressView.visibility = View.VISIBLE
                mZqAudioEditorKit.isEnableAutoComposeLevel = true
                mZqAudioEditorKit.autoComposeLevelDB = -3f
                mZqAudioEditorKit.startCompose()
            }
        })

        mZqAudioEditorKit.setOnErrorListener { what, msg1, msg2 ->
            MyLog.e(TAG, "onError what=$what msg1=$msg1 msg2=$msg2")
            launch {
                progressView.visibility = View.GONE
            }
        }

        mZqAudioEditorKit.setOnPreviewInfoListener(object : ZqAudioEditorKit.OnPreviewInfoListener {
            override fun onStarted() {
                MyLog.d(TAG, "onStarted")
            }

            override fun onCompletion() {
                MyLog.d(TAG, "preview onCompletion")
                //startPreview()
            }

            override fun onLoopCount(count: Int) {
                MyLog.d(TAG, "onLoopCount count:$count")
            }
        })

        mZqAudioEditorKit.setOnComposeInfoListener(object : ZqAudioEditorKit.OnComposeInfoListener {
            override fun onProgress(progress: Float, isAudioLevelPassing: Boolean) {
                launch {
                    val text = if (isAudioLevelPassing) "正在分析" else "正在合成"
                    progressView.setProgressText("$text ${(progress * 100).toInt()}%")
                }
            }

            override fun onAudioLevelPassCompletion() {
                MyLog.d(TAG, "onAudioLevelPassCompletion")
            }

            override fun onCompletion() {
                MyLog.d(TAG, "compose onCompletion")
                launch {
                    if (mFeedsMakeModel?.fromPosts == true) {
                        // 来自帖子发送请求获取songid 偷懒，直接调用Activity 的方法了
                        val feedsPublishActivity = FeedsPublishActivity()
                        feedsPublishActivity.mFeedsMakeModel = mFeedsMakeModel
                        progressView.setProgressText("生成歌曲")
                        feedsPublishActivity.step1 { result ->
                            progressView.visibility = View.GONE
                            if (result) {
                                EventBus.getDefault().post(FeedSongMakeSucessEvent(
                                        mFeedsMakeModel?.songModel?.songID,
                                        mFeedsMakeModel?.songModel?.songTpl?.getDisplaySongName(),
                                        mFeedsMakeModel?.composeSavePath,
                                        mFeedsMakeModel?.recordDuration?.toInt()
                                ))
                                for (ac in U.getActivityUtils().activityList) {
                                    if (ac is FeedSongManagerActivity) {
                                        ac.finish()
                                    }
                                    if (ac is FeedSongSearchActivity) {
                                        ac.finish()
                                    }
                                    if (ac is FeedsMakeActivity) {
                                        ac.finish()
                                    }
                                }
                                this@FeedsEditorActivity.finish()
                            } else {
                                U.getToastUtil().showShort("生成歌曲失败")
                            }
                        }
                    } else if (mFrom == FROM_CLUB_PAGE) {
                        progressView.setProgressDrwable(U.getDrawable(R.drawable.common_progress_complete_icon))
                        progressView.setProgressText("合成完成")
                        progressView.visibility = View.GONE
                        sFeedsMakeModelHolder = mFeedsMakeModel
                        ARouter.getInstance().build(RouterConstants.ACTIVITY_CLUB_UPLOAD_SONG)
                                .withInt("familyID", familyID!!)
                                .withInt("category", 1)
                                .navigation(this@FeedsEditorActivity, 9)

                        val iRankingModeService = ARouter.getInstance().build(RouterConstants.SERVICE_CLUB).navigation() as IClubModuleService
                        iRankingModeService.finishClubPostUpload()

                        finish()
                        for (ac in U.getActivityUtils().activityList) {
                            if (ac is FeedsMakeActivity) {
                                ac.finish()
                            }
                        }
                    } else {
                        progressView.setProgressDrwable(U.getDrawable(R.drawable.common_progress_complete_icon))
                        progressView.setProgressText("合成完成")
                        progressView.visibility = View.GONE
                        sFeedsMakeModelHolder = mFeedsMakeModel
                        ARouter.getInstance().build(RouterConstants.ACTIVITY_FEEDS_PUBLISH)
                                .navigation(this@FeedsEditorActivity, 9)
                        finish()
                        for (ac in U.getActivityUtils().activityList) {
                            if (ac is FeedsMakeActivity) {
                                ac.finish()
                            }
                        }
                    }

                }
            }
        })

        totalTsTv.text = U.getDateTimeUtils().formatVideoTime(realDuration.toLong())
        txtLyricsView.setDuration(realDuration)
        txtLyricsView?.visibility = View.GONE
        manyLyricsView?.visibility = View.GONE
        if (mFeedsMakeModel?.withBgm == true) {
            manyLyricsView?.visibility = View.VISIBLE
            LyricsManager
                    .loadStandardLyric(mFeedsMakeModel?.songModel?.songTpl?.lrcTs, -1)
                    .subscribe({ lyricsReader ->
                        createCustomZrce2ReaderByTxt(lyricsReader, mFeedsMakeModel?.songModel?.songTpl?.lrcTxtStr)
                        initManyLyricView(lyricsReader)
                    }, { throwable ->
                        MyLog.e(TAG, throwable)
                    })

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

                mZqAudioEditorKit.setDataSource(0, bgmFileJob.await().path, mFeedsMakeModel?.firstLyricShiftTs?.toLong()
                        ?: 0L, mFeedsMakeModel?.recordDuration
                        ?: -1)
                mZqAudioEditorKit.setDataSource(1, mFeedsMakeModel?.recordSavePath, mFeedsMakeModel?.recordOffsetTs
                        ?: 0L, -1)
                mZqAudioEditorKit.setInputVolume(0, ZqEngineKit.getInstance().params.audioMixingPlayoutVolume / 100.0f)
                mZqAudioEditorKit.setInputVolume(1, ZqEngineKit.getInstance().params.recordingSignalVolume / 100.0f)
                mZqAudioEditorKit.setAudioEffect(1, ZqEngineKit.getInstance().params.styleEnum.ordinal)
                mZqAudioEditorKit.setDelay(1, U.getPreferenceUtils().getSettingLong("feeds_voice_delay", 0))
                initWhenEngineReady()
            }
        } else {
            txtLyricsView?.visibility = View.VISIBLE
            mFeedsMakeModel?.songModel?.songTpl?.lrcTxtStr?.let {
                txtLyricsView.setLyrics(it)
            }
            mZqAudioEditorKit.setDataSource(0, mFeedsMakeModel?.recordSavePath, mFeedsMakeModel?.firstLyricShiftTs?.toLong()
                    ?: 0, mFeedsMakeModel?.recordDuration
                    ?: -1)
            mZqAudioEditorKit.setInputVolume(0, ZqEngineKit.getInstance().params.recordingSignalVolume / 100.0f)
            mZqAudioEditorKit.setAudioEffect(0, ZqEngineKit.getInstance().params.styleEnum.ordinal)
            initWhenEngineReady()
        }
    }

    private fun initWhenEngineReady() {
        MyLog.d(TAG, "initWhenEngineReady")
        mZqAudioEditorKit.outputPath = mFeedsMakeModel?.composeSavePath
//        mEffectIv?.isSelected = true
//        mVoiceControlView?.visibility = View.VISIBLE
        mZqAudioEditorKit.startPreview(-1)
        resumePreview()
    }

    private fun initManyLyricView(lyricsReader: LyricsReader) {
        manyLyricsView?.initLrcData()
        manyLyricsView?.lyricsReader = lyricsReader
        manyLyricsView?.seekTo(0)
        manyLyricsView?.pause()
    }

    private fun resumePreview() {
        MyLog.d(TAG, "startPreview")
        // 预览开始了
        mZqAudioEditorKit?.resumePreview()
        playBtn?.isSelected = true

        if (manyLyricsView?.visibility == View.VISIBLE) {
            manyLyricsView?.play(mZqAudioEditorKit.position.toInt())
        } else if (txtLyricsView.visibility == View.VISIBLE) {
            txtLyricsView?.play(mZqAudioEditorKit.position.toInt())
        }

        if (cdRotateAnimator?.isStarted == true) {
            cdRotateAnimator?.resume()
        } else {
            cdRotateAnimator?.start()
//            launch {
//                delay(1000)
//                if(cdContainer.width>0){
//                    cdContainer?.pivotX = cdContainer.width / 2f
//                    cdContainer?.pivotY = cdContainer.height / 2f
//
//                }
//            }
        }

        mPlayProgressJob?.cancel()
        mPlayProgressJob = launch {
            for (i in 1..1000) {
                nowTsTv.text = U.getDateTimeUtils().formatVideoTime(mZqAudioEditorKit.position)
                if (manyLyricsView?.visibility == View.VISIBLE) {
                    if (manyLyricsView?.getLrcPlayerStatus() != LRCPLAYERSTATUS_PLAY) {
                        manyLyricsView?.resume()
                    }
                    manyLyricsView?.seekTo(mZqAudioEditorKit.position.toInt())
                }
                if (txtLyricsView.visibility == View.VISIBLE) {
                    txtLyricsView?.play(mZqAudioEditorKit.position.toInt())
                }
                seekBar?.progress = mZqAudioEditorKit.position.toInt()
                delay(1000)
            }
        }
    }

    private fun pausePreview() {
        MyLog.d(TAG, "pausePreview")
        mZqAudioEditorKit.pausePreview()
        mPlayProgressJob?.cancel()
        playBtn?.isSelected = false
        manyLyricsView?.pause()
        txtLyricsView.pause()
        cdRotateAnimator?.pause()
    }

    override fun onResume() {
        super.onResume()

    }

    override fun onPause() {
        super.onPause()
    }

    override fun destroy() {
        super.destroy()
        manyLyricsView?.release()
        mZqAudioEditorKit.release()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 9) {
                // 从发布页返回
                mZqAudioEditorKit.startPreview(-1)
                resumePreview()
            }
        }
    }

    override fun onBackPressedForActivity(): Boolean {
        finishPage()
        return true
    }

    private fun finishPage() {
        if (mFeedsMakeModel?.hasChangeLyricOrSongNameThisTime == true) {
            val tipsDialogView = TipsDialogView.Builder(this@FeedsEditorActivity)
                    .setConfirmTip("保存")
                    .setCancelTip("直接退出")
                    .setCancelBtnClickListener {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                    .setMessageTip("是否将改编歌词保存到草稿箱?")
                    .setConfirmBtnClickListener {
                        launch {
                            val j = launch(Dispatchers.IO) {
                                mFeedsMakeModel?.let {
                                    FeedsMakeLocalApi.insert(it)
                                }
                            }
                            // 保存到草稿
                            // 保存到草稿
                            j.join()
                            if (mFeedsMakeModel?.songModel?.challengeID == 0L) {
                                U.getToastUtil().showShort("已存入翻唱草稿")
                            } else {
                                U.getToastUtil().showShort("已存入打榜草稿")
                            }
                            setResult(Activity.RESULT_CANCELED)
                            finish()
                        }
                    }
                    .build()
            tipsDialogView.showByDialog()
        } else {
            finish()
        }
    }
    //    @Subscribe(threadMode = ThreadMode.MAIN)
//    fun onEvent(event: EngineEvent) {
//        if (event.getType() == EngineEvent.TYPE_MUSIC_PLAY_FINISH) {
//        } else if (event.getType() == EngineEvent.TYPE_MUSIC_PLAY_START) {
//        }
//    }

    override fun canSlide(): Boolean {
        return false
    }

    override fun useEventBus(): Boolean {
        return false
    }
}
