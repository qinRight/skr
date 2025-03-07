package com.module.posts.detail.view

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Message
import android.support.constraint.ConstraintLayout
import android.support.constraint.Group
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.alibaba.android.arouter.launcher.ARouter
import com.common.core.view.setDebounceViewClickListener
import com.common.emoji.EmotionKeyboard
import com.common.emoji.LQREmotionKit
import com.common.statistics.StatisticsAdapter
import com.common.utils.U
import com.common.view.AnimateClickListener
import com.common.view.DebounceViewClickListener
import com.common.view.ex.ExImageView
import com.common.view.ex.NoLeakEditText
import com.common.view.recyclerview.DiffAdapter
import com.component.busilib.event.FeedSongMakeSucessEvent
import com.dialog.view.TipsDialogView
import com.imagebrowse.ImageBrowseView
import com.imagebrowse.big.BigImageBrowseFragment
import com.imagebrowse.big.DefaultImageBrowserLoader
import com.module.RouterConstants
import com.module.posts.R
import com.module.posts.detail.adapter.PostsReplayImgAdapter
import com.module.posts.detail.event.PostsCommentBoardEvent
import com.module.posts.detail.view.PostsVoiceRecordView.Companion.STATUS_RECORDING
import com.module.posts.detail.view.PostsVoiceRecordView.Companion.STATUS_RECORD_OK
import com.module.posts.detail.view.PostsVoiceRecordView.Companion.STATUS_RECORD_PLAYING
import com.respicker.ResPicker
import com.respicker.activity.ResPickerActivity
import com.respicker.model.ImageItem
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.Serializable

class PostsInputContainerView : ConstraintLayout, EmotionKeyboard.BoardStatusListener {
    internal var mEmotionKeyboard: EmotionKeyboard? = null

    protected var mEtContent: NoLeakEditText? = null
    internal var mPlaceHolderView: ViewGroup? = null
    protected var mSendMsgBtn: View? = null
    lateinit var tupianIv: ExImageView
    lateinit var yuyinIv: ExImageView
    lateinit var kgeIv: ExImageView
    lateinit var imgRecyclerView: RecyclerView
    lateinit var postsVoiceRecordView: PostsVoiceRecordView
    lateinit var postsKgeRecordView: PostsKgeRecordView
    lateinit var topArea: View
    lateinit var mImageTid: ExImageView
    lateinit var mKgeTid: ExImageView
    lateinit var mAudioTid: ExImageView

    lateinit var selectImgGroup: Group

    lateinit var postsReplayImgAdapter: PostsReplayImgAdapter
    protected var mHasPretend = false
    protected var mForceHide = false
    var mSendCallBack: ((ReplyModel, Any?) -> Unit)? = null
    var toStopPlayCall: (() -> Unit)? = null

    //这个输入框消失的时候调用
    var mHideCallBack: ((SHOW_TYPE) -> Unit)? = null

    var showType: SHOW_TYPE = SHOW_TYPE.NUL

    var mExtra: Any? = null

    var replyModel = ReplyModel()

    protected var mUiHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (msg.what == 100) {
                mHasPretend = true
            }
        }
    }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    protected fun init() {
        View.inflate(context, R.layout.posts_input_container_view_layout, this)
        initInputView()
    }

    fun setETHint(str: String) {
        mEtContent?.hint = str
    }

    /**
     * 输入面板相关view的初始化
     */
    protected fun initInputView() {
        LQREmotionKit.tryInit(U.app())

        mEtContent = this.findViewById<View>(R.id.etContent) as NoLeakEditText
        mPlaceHolderView = this.findViewById(R.id.place_holder_view)
        mSendMsgBtn = this.findViewById(R.id.send_msg_btn)
        tupianIv = this.findViewById(R.id.tupian_iv)
        yuyinIv = this.findViewById(R.id.yuyin_iv)
        kgeIv = this.findViewById(R.id.kge_iv)
        topArea = this.findViewById(R.id.topArea)
        mImageTid = findViewById(R.id.image_tid)
        mKgeTid = findViewById(R.id.kge_tid)
        mAudioTid = findViewById(R.id.audio_tid)

        imgRecyclerView = rootView.findViewById(R.id.recycler_view)
        postsVoiceRecordView = PostsVoiceRecordView(rootView.findViewById(R.id.posts_voice_record_view_stub))
        postsKgeRecordView = PostsKgeRecordView(rootView.findViewById(R.id.posts_kge_record_view_stub))
        selectImgGroup = rootView.findViewById(R.id.select_img_group)

        postsReplayImgAdapter = PostsReplayImgAdapter()
        imgRecyclerView.adapter = postsReplayImgAdapter
        imgRecyclerView.layoutManager = LinearLayoutManager(context).also {
            it.setOrientation(LinearLayoutManager.HORIZONTAL)
        }

        mEtContent?.setDebounceViewClickListener {
            showTid()
        }

        mEtContent?.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                showTid()
            }
        }

        postsReplayImgAdapter?.delClickListener = { m, pos ->
            // 不能直接用pos  notifyItemRemoved 会导致holder 里的 pos 不变
            postsReplayImgAdapter.dataList.removeAt(pos)
            postsReplayImgAdapter.notifyItemRemoved(pos)
            postsReplayImgAdapter.notifyItemRangeChanged(0,postsReplayImgAdapter.dataList.size, DiffAdapter.REFRESH_POS)
            if (postsReplayImgAdapter.dataList.isEmpty()) {
                imgRecyclerView.visibility = View.GONE
            }
        }
        postsReplayImgAdapter.imgClickListener = { _, pos ->

            BigImageBrowseFragment.open(true, context as FragmentActivity, object : DefaultImageBrowserLoader<ImageItem>() {
                override fun init() {

                }

                override fun load(imageBrowseView: ImageBrowseView, position: Int, item: ImageItem) {
                    imageBrowseView.load(item.path)
                }

                override fun getInitCurrentItemPostion(): Int {
                    return pos
                }

                override fun getInitList(): List<ImageItem>? {
                    return postsReplayImgAdapter.dataList
                }
            })
        }

        initEmotionKeyboard()

        tupianIv.setDebounceViewClickListener {
            goAddImagePage()
        }

        yuyinIv.setDebounceViewClickListener {
            goAddVoicePage()
        }

        kgeIv.setDebounceViewClickListener {
            goAddKgePage()
        }
        mSendMsgBtn?.setOnClickListener(object : DebounceViewClickListener() {
            override fun clickValid(v: View) {
                /**
                 * 这里点击按钮发送，要判断是否有录音以及图片
                 */
                if (postsVoiceRecordView.realView?.visibility == View.VISIBLE
                        && postsVoiceRecordView.status >= PostsVoiceRecordView.STATUS_RECORD_OK) {
                    // 相当于点击了声音的ok面板
                    postsVoiceRecordView.okIv.performClick()
                } else if (imgRecyclerView.visibility == View.VISIBLE) {
                    // 发送图片和文字
                    replyModel.imgLocalPathList.clear()
                    replyModel.imgLocalPathList.addAll(postsReplayImgAdapter.dataList)
                    replyModel.contentStr = mEtContent?.text.toString()
                    mSendCallBack?.invoke(replyModel, mExtra)
                } else {
                    // 只有文字或歌曲
                    replyModel.contentStr = mEtContent?.text.toString()
                    mSendCallBack?.invoke(replyModel, mExtra)
                }
            }
        })

        topArea?.setOnClickListener {
            hideSoftInput()
        }

        setOnClickListener {}

        postsVoiceRecordView.recordOkListener = { path, duration ->
            replyModel.recordVoicePath = path
            replyModel.recordDurationMs = duration
        }
        postsVoiceRecordView.okClickListener = { path, duration ->

            replyModel.contentStr = mEtContent?.text.toString()
            mSendCallBack?.invoke(replyModel, mExtra)
        }

        postsVoiceRecordView.okToStopPlayListener = {
            toStopPlayCall?.invoke()
        }

        postsKgeRecordView.selectSongClickListener = {
            ARouter.getInstance().build(RouterConstants.ACTIVITY_FEEDS_SONG_MANAGE)
                    .withInt("from", 9)
                    .navigation()
        }

        postsKgeRecordView.okToStopPlayListener = {
            toStopPlayCall?.invoke()
        }

        postsKgeRecordView.okClickListener = {
            replyModel.contentStr = mEtContent?.text.toString()
            mSendCallBack?.invoke(replyModel, mExtra)
        }

        postsVoiceRecordView?.mResetCall = {
            replyModel.reset()
        }

        postsKgeRecordView?.mResetCall = {
            replyModel.reset()
        }
    }

    var tipsDialogView: TipsDialogView? = null

    private fun goAddVoicePage() {
        val hasPic = postsReplayImgAdapter.dataList.isNotEmpty()
        val hasSong = replyModel.songId > 0
        val hasAudio = replyModel.recordVoicePath?.isNotEmpty() == true
        if (hasPic || hasSong) {
            var tips: String? = null
            if (hasPic) {
                tips = "上传语音将清空图片,是否继续"
            } else if (hasSong) {
                tips = "上传语音将清空歌曲,是否继续"
            }
            //如果已经录入语音
            tipsDialogView = TipsDialogView.Builder(context)
                    .setMessageTip(tips)
                    .setConfirmTip("继续")
                    .setCancelTip("取消")
                    .setCancelBtnClickListener(object : AnimateClickListener() {
                        override fun click(view: View?) {
                            tipsDialogView?.dismiss()
                        }
                    })
                    .setConfirmBtnClickListener(object : AnimateClickListener() {
                        override fun click(view: View?) {
                            resetData(SHOW_TYPE.KEG,SHOW_TYPE.IMG)
                            tipsDialogView?.dismiss(false)
                            goAddVoicePage()
                        }
                    })
                    .build()
            visibility = View.VISIBLE
            mEmotionKeyboard?.hideSoftInput()
            tipsDialogView?.showByDialog()
        } else {
            showAudioRecordView()
            visibility = View.VISIBLE
        }
    }

    private fun goAddKgePage() {
        val hasPic = postsReplayImgAdapter.dataList.isNotEmpty()
        val hasAudio = replyModel.recordVoicePath?.isNotEmpty() == true

        if (hasPic || hasAudio) {
            var tips: String? = null
            if (hasPic) {
                tips = "上传歌曲将清空图片,是否继续"
            } else if (hasAudio) {
                tips = "上传歌曲将清空语音,是否继续"
            }
            //如果已经录入语音
            tipsDialogView = TipsDialogView.Builder(context)
                    .setMessageTip(tips)
                    .setConfirmTip("继续")
                    .setCancelTip("取消")
                    .setCancelBtnClickListener(object : AnimateClickListener() {
                        override fun click(view: View?) {
                            tipsDialogView?.dismiss()
                        }
                    })
                    .setConfirmBtnClickListener(object : AnimateClickListener() {
                        override fun click(view: View?) {
                            resetData(SHOW_TYPE.AUDIO,SHOW_TYPE.IMG)
                            tipsDialogView?.dismiss(false)
                            goAddKgePage()
                        }
                    })
                    .build()
            visibility = View.VISIBLE
            mEmotionKeyboard?.hideSoftInput()
            tipsDialogView?.showByDialog()
        } else {
            showKgeRecordView()
            visibility = View.VISIBLE

            if (postsVoiceRecordView.status == STATUS_RECORDING) {
                resetData(SHOW_TYPE.AUDIO)
            }
        }
    }

    private fun goAddImagePage() {
        val hasAudio = replyModel.recordVoicePath?.isNotEmpty() == true
        val hasSong = replyModel.songId > 0

        if (hasSong || hasAudio) {
            var tips: String? = null
            if (hasSong) {
                tips = "上传图片将清空歌曲,是否继续"
            } else if (hasAudio) {
                tips = "上传图片将清空语音,是否继续"
            }
            //如果已经录入语音
            tipsDialogView = TipsDialogView.Builder(context)
                    .setMessageTip(tips)
                    .setConfirmTip("继续")
                    .setCancelTip("取消")
                    .setCancelBtnClickListener(object : AnimateClickListener() {
                        override fun click(view: View?) {
                            tipsDialogView?.dismiss()
                        }
                    })
                    .setConfirmBtnClickListener(object : AnimateClickListener() {
                        override fun click(view: View?) {
                            resetData(SHOW_TYPE.AUDIO,SHOW_TYPE.KEG)
                            tipsDialogView?.dismiss(false)
                            goAddImagePage()
                        }
                    })
                    .build()
            visibility = View.VISIBLE
            mEmotionKeyboard?.hideSoftInput()
            tipsDialogView?.showByDialog()
        } else {
            mEmotionKeyboard?.hideSoftInput()
            ResPicker.getInstance().params = ResPicker.newParamsBuilder()
                    .setMultiMode(true)
                    .setShowCamera(true)
                    .setIncludeGif(true)
                    .setCrop(false)
                    .setSelectLimit(9)
                    .build()
            ResPickerActivity.open(context as Activity, ArrayList<ImageItem>(postsReplayImgAdapter?.dataList))
            postsVoiceRecordView.setVisibility(View.GONE)
            postsKgeRecordView.setVisibility(View.GONE)
            mPlaceHolderView?.getLayoutParams()?.height = 0

            if (postsVoiceRecordView.status == STATUS_RECORDING) {
                resetData(SHOW_TYPE.AUDIO)
            }
            visibility = View.VISIBLE
        }
    }

    fun onSelectImgOk(selectedImageList: java.util.ArrayList<ImageItem>) {
        postsReplayImgAdapter.dataList.clear()
        postsReplayImgAdapter.dataList.addAll(selectedImageList)
        postsReplayImgAdapter.notifyDataSetChanged()
        if (postsReplayImgAdapter.dataList.isNotEmpty()) {
            showImageSelectView()
        }
    }

    private fun resetData(vararg types:SHOW_TYPE){
        for(type in types){
            if(type == SHOW_TYPE.KEG){
                // 重置k歌相关
                replyModel.songId = 0
                postsKgeRecordView.reset()
            }else if(type == SHOW_TYPE.AUDIO){
                // 重置语音相关
                postsVoiceRecordView.reset()
                replyModel.resetVoice()
            }else if(type == SHOW_TYPE.IMG){
                // 重置图片相关
                postsReplayImgAdapter.dataList.clear()
                postsReplayImgAdapter.notifyDataSetChanged()
                replyModel.imgUploadMap.clear()
            }else if(type==SHOW_TYPE.KEY_BOARD){
                replyModel.contentStr=""
                mEtContent?.setText("")
            }
        }
    }

    private fun showKgeRecordView() {
        showType = SHOW_TYPE.KEG
        postsKgeRecordView.setVisibility(View.VISIBLE)

        postsVoiceRecordView.setVisibility(View.GONE)
        selectImgGroup.visibility = View.GONE
        mEmotionKeyboard?.hideSoftInput()
        postsKgeRecordView.realView?.getLayoutParams()?.height = U.getDisplayUtils().dip2px(260f)
    }

    private fun showImageSelectView() {
        showType = SHOW_TYPE.IMG
        postsKgeRecordView.setVisibility(View.GONE)

        selectImgGroup.visibility = View.VISIBLE
        postsVoiceRecordView.setVisibility(View.GONE)

        mPlaceHolderView?.getLayoutParams()?.height = 0
        mPlaceHolderView?.setLayoutParams(mPlaceHolderView?.getLayoutParams())
    }

    private fun showAudioRecordView() {
        showType = SHOW_TYPE.AUDIO
        postsKgeRecordView.setVisibility(View.GONE)
        selectImgGroup.visibility = View.GONE
        postsVoiceRecordView.setVisibility(View.VISIBLE)
        mEmotionKeyboard?.hideSoftInput()
        postsVoiceRecordView.realView?.getLayoutParams()?.height = U.getDisplayUtils().dip2px(260f)
    }

    private fun initEmotionKeyboard() {
        mEmotionKeyboard = EmotionKeyboard.with(context as Activity)
        mEmotionKeyboard?.bindToPlaceHodlerView(mPlaceHolderView)
        mEmotionKeyboard?.bindToEditText(mEtContent)
        mEmotionKeyboard?.setBoardStatusListener(this)
        mEmotionKeyboard?.setEmotionLayout(findViewById(R.id.elEmotion))
    }

    override fun onBoardShow() {
        EventBus.getDefault().post(PostsCommentBoardEvent(true))
        visibility = View.VISIBLE
        showTid()
    }

    override fun onBoardHide() {
        if (showType == SHOW_TYPE.KEY_BOARD || mForceHide) {
            //当前是键盘状态，需要收起键盘，reset
            invokeHideCall()
            EventBus.getDefault().post(PostsCommentBoardEvent(false))
            visibility = View.GONE
            mEtContent?.hint = ""
        }

        if (showType == SHOW_TYPE.IMG) {
            mPlaceHolderView?.getLayoutParams()?.height = 0
            mPlaceHolderView?.setLayoutParams(mPlaceHolderView?.getLayoutParams())
        }

        mForceHide = false

        mImageTid.visibility = View.GONE
        mKgeTid.visibility = View.GONE
        mAudioTid.visibility = View.GONE
    }

    fun showSoftInput(type: SHOW_TYPE, model: Any?) {
        StatisticsAdapter.recordCountEvent("posts", "comment_click", null)
        mExtra = model
        if (type == SHOW_TYPE.KEY_BOARD) {
            if (showType == SHOW_TYPE.NUL) {
                showType = SHOW_TYPE.KEY_BOARD
            }
            mEmotionKeyboard?.showSoftInput()
            showTid()
        } else if (type == SHOW_TYPE.IMG) {
            goAddImagePage()
            mEmotionKeyboard?.hideSoftInput()
        } else if (type == SHOW_TYPE.AUDIO) {
            goAddVoicePage()
        } else if (type == SHOW_TYPE.KEG) {
            goAddKgePage()
        }
    }

    fun showTid() {
        mImageTid.visibility = View.GONE
        mKgeTid.visibility = View.GONE
        mAudioTid.visibility = View.GONE

        if (showType == SHOW_TYPE.AUDIO) {
            if (postsVoiceRecordView.status == STATUS_RECORD_OK) {
                mAudioTid.visibility = View.VISIBLE
                return
            }
        }

        if (showType == SHOW_TYPE.KEG) {
            if (postsKgeRecordView.status == postsKgeRecordView.STATUS_RECORD_OK) {
                mKgeTid.visibility = View.VISIBLE
                return
            }
        }

        if (showType == SHOW_TYPE.IMG) {
            if (postsReplayImgAdapter.dataList.size > 0) {
                mImageTid.visibility = View.VISIBLE
                return
            }
        }
    }

    fun hideSoftInput() {
        invokeHideCall()
        mForceHide = true
        mEmotionKeyboard?.hideSoftInput()
        if (showType == SHOW_TYPE.AUDIO) {
            if (postsVoiceRecordView.status == STATUS_RECORDING) {
                postsVoiceRecordView.reset()
            } else if (postsVoiceRecordView.status == STATUS_RECORD_PLAYING) {
                postsVoiceRecordView.stop()
            }
        }

        if (showType == SHOW_TYPE.KEG) {
            if (postsKgeRecordView.status == postsKgeRecordView.STATUS_RECORD_PLAYING) {
                postsKgeRecordView.stop()
            }
        }

    }

    private fun invokeHideCall() {
        if (showType == SHOW_TYPE.AUDIO) {
            if (postsVoiceRecordView.status == STATUS_RECORD_OK) {
                mHideCallBack?.invoke(showType)
                return
            }
        }

        if (showType == SHOW_TYPE.KEG) {
            if (postsKgeRecordView.status == postsKgeRecordView.STATUS_RECORD_OK) {
                mHideCallBack?.invoke(showType)
                return
            }
        }

        if (showType == SHOW_TYPE.IMG) {
            if (postsReplayImgAdapter.dataList.size > 0) {
                mHideCallBack?.invoke(showType)
                return
            }
        }
        mHideCallBack?.invoke(SHOW_TYPE.NUL)
    }

    fun onBackPressed(): Boolean {
        if (mEmotionKeyboard!!.isEmotionShown) {
            mEmotionKeyboard?.hideEmotionLayout(false)
            return true
        }
        return false
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mEmotionKeyboard?.destroy()
        postsKgeRecordView.reset()
        postsVoiceRecordView.reset()
        mUiHandler.removeCallbacksAndMessages(null)
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: FeedSongMakeSucessEvent) {
        /**
         * 只有顶部的activity 才能接受到这个时间
         * 防止二级评论回复时 一级评论也有k歌数据
         */
        val size = U.getActivityUtils().activityList.size
        for(i in (size-1) downTo 0){
            if(U.getActivityUtils().activityList[i] == context){
                replyModel.songId = event.songId ?: 0
                postsKgeRecordView.recordOk(event.localPath, event.duration!!)
                break
            }
        }
    }

    fun onCommentSuccess() {
        replyModel.reset()
        resetData(SHOW_TYPE.KEG,SHOW_TYPE.AUDIO,SHOW_TYPE.IMG,SHOW_TYPE.KEY_BOARD)
        mHideCallBack?.invoke(SHOW_TYPE.NUL)
    }

    enum class SHOW_TYPE {
        NUL, KEY_BOARD, IMG, AUDIO, KEG
    }
}


class ReplyModel : Serializable {
    fun resetVoice() {
        recordVoiceUrl = null
        recordVoicePath = null
        recordDurationMs = 0 // 毫秒
    }

    fun reset() {
        resetVoice()
        contentStr = ""
        imgUploadMap.clear()
        imgLocalPathList.clear()
        songId = 0
    }

    var contentStr: String = ""
    val imgUploadMap = LinkedHashMap<String, String>() // 本地路径->服务器url
    val imgLocalPathList = ArrayList<ImageItem>() // 本地路径列表
    var recordVoiceUrl: String? = null
    var recordVoicePath: String? = null
    var recordDurationMs: Int = 0 // 毫秒
    var songId = 0 // feeds的歌曲
}