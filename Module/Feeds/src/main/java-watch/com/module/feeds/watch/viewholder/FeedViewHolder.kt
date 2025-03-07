package com.module.feeds.watch.viewholder

import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.common.core.avatar.AvatarUtils
import com.common.log.MyLog
import com.common.player.SinglePlayer
import com.common.statistics.StatisticsAdapter
import com.common.utils.SpanUtils
import com.common.utils.U
import com.common.utils.dp
import com.common.view.AnimateClickListener
import com.common.view.DebounceViewClickListener
import com.common.view.ex.ExConstraintLayout
import com.component.person.utils.StringFromatUtils
import com.facebook.drawee.view.SimpleDraweeView
import com.module.feeds.R
import com.module.feeds.detail.view.AutoScrollLyricView
import com.module.feeds.detail.view.FeedsManyLyricView
import com.module.feeds.watch.listener.FeedsListener
import com.module.feeds.watch.model.FeedsWatchModel
import com.module.feeds.watch.view.FeedsRecordAnimationView
import com.module.feeds.watch.watchview.FeedLikeView


open class FeedViewHolder(var rootView: View, var listener: FeedsListener?) : RecyclerView.ViewHolder(rootView) {

    private val mMoreIv: ImageView = itemView.findViewById(R.id.more_iv)
    private val mTagArea: ExConstraintLayout = itemView.findViewById(R.id.tag_area)
    private val mTagTv: TextView = itemView.findViewById(R.id.tag_tv)
    private val mContentTv: TextView = itemView.findViewById(R.id.content_tv)
    val mSongAreaBg: SimpleDraweeView = itemView.findViewById(R.id.song_area_bg)
    val mRecordView: FeedsRecordAnimationView = itemView.findViewById(R.id.record_view)
    val mShareTag: TextView = itemView.findViewById(R.id.share_tag)

    val mLikeNumTv: TextView = itemView.findViewById(R.id.like_num_tv)
    val mPlayNumTv: TextView = itemView.findViewById(R.id.play_num_tv)
    val mCollectIconTv: TextView = itemView.findViewById(R.id.collect_icon_tv)
    val mDebugTv: TextView = itemView.findViewById(R.id.debug_tv)
    val feedAutoScrollLyricView = AutoScrollLyricView(itemView.findViewById(R.id.auto_scroll_lyric_view_layout_viewstub), true)
    val feedWatchManyLyricView = FeedsManyLyricView(itemView.findViewById(R.id.feeds_watch_many_lyric_layout_viewstub), true)

    val mTvLikes: FeedLikeView = itemView.findViewById(R.id.tv_likes)
    val mTvLikesDivider: View = itemView.findViewById(R.id.tv_likes_divider)

    val mFeedsClickView: View = itemView.findViewById(R.id.feeds_click_view)

    var mPosition: Int = 0
    var model: FeedsWatchModel? = null

    init {
        mMoreIv.setOnClickListener(object : DebounceViewClickListener() {
            override fun clickValid(view: View?) {
                listener?.onClickMoreListener(mPosition, model)
            }
        })

        mLikeNumTv.setOnClickListener(object : AnimateClickListener() {
            override fun click(v: View?) {
                listener?.onClickLikeListener(mPosition, model)
            }
        })

        mRecordView.setOnClickListener(object : DebounceViewClickListener() {
            override fun clickValid(v: View?) {
                listener?.onClickCDListener(mPosition, model)
            }
        })

        mTagArea.setOnClickListener(object : AnimateClickListener() {
            override fun click(v: View?) {
                StatisticsAdapter.recordCountEvent("music_recommend", "rank", null)
                listener?.onclickRankListener(model)
            }
        })

        itemView.setOnClickListener(object : DebounceViewClickListener() {
            override fun clickValid(v: View?) {
                listener?.onClickDetailListener(mPosition, model)
            }
        })

        mFeedsClickView.setOnClickListener(object : DebounceViewClickListener() {
            override fun clickValid(v: View?) {
                listener?.onClickDetailListener(mPosition, model)
            }
        })

        mCollectIconTv.setOnClickListener(object : AnimateClickListener() {
            override fun click(v: View?) {
                listener?.onClickCollectListener(mPosition, model)
            }
        })

        mTvLikes.onClickNameListener = {
            listener?.onClickNameListener(it)
        }


        if (MyLog.isDebugLogOpen()) {
            mDebugTv.visibility = View.VISIBLE
        } else {
            mDebugTv.visibility = View.GONE
        }
    }

    open fun bindData(position: Int, watchModel: FeedsWatchModel) {
        this.mPosition = position
        this.model = watchModel

        watchModel.user?.let {
            AvatarUtils.loadAvatarByUrl(mSongAreaBg, AvatarUtils.newParamsBuilder(it.avatar)
                    .setCornerRadius(U.getDisplayUtils().dip2px(8f).toFloat())
                    .setBlur(true)
                    .build())
            mRecordView.setAvatar(it.avatar ?: "", watchModel.song?.needShareTag == true)
        }

        // 第一优先级显示 神曲分享
        if (watchModel.song?.needShareTag == true) {
            mTagArea.visibility = View.GONE
            if (watchModel.song?.needShareTag == true) {
                var singler = ""
                if (!TextUtils.isEmpty(watchModel.song?.songTpl?.singer)) {
                    singler = " 演唱/${watchModel.song?.songTpl?.singer}"
                }
                mShareTag.visibility = View.VISIBLE
                mShareTag.text = "#神曲分享#$singler"
            } else {
                mShareTag.visibility = View.GONE
            }
        } else {
            mShareTag.visibility = View.GONE
            if (watchModel.song?.needChallenge == true) {
                if (watchModel.rank != null) {
                    if (TextUtils.isEmpty(watchModel.rank?.rankDesc)) {
                        mTagArea.visibility = View.GONE
                    } else {
                        mTagTv.text = watchModel.rank?.rankDesc
                        mTagArea.visibility = View.VISIBLE
                    }
                } else {
                    mTagArea.visibility = View.GONE
                }
            } else {
                mTagArea.visibility = View.GONE
            }
        }

        refreshPlayNum()
        refreshLike()
        refreshCollects()

        var recomendTag = ""
        if (watchModel.song?.needRecommentTag == true) {
            recomendTag = "#小编推荐# "
        }
        var songTag = ""
        watchModel.song?.tags?.let {
            for (model in it) {
                model?.tagDesc.let { tagDesc ->
                    songTag = "$songTag#$tagDesc# "
                }
            }
        }
        val title = watchModel.song?.title ?: ""
        if (TextUtils.isEmpty(recomendTag) && TextUtils.isEmpty(songTag) && TextUtils.isEmpty(title)) {
            mContentTv.visibility = View.GONE
        } else {
            val stringBuilder = SpanUtils()
                    .append(recomendTag).setForegroundColor(U.getColor(R.color.black_trans_50))
                    .append(songTag).setForegroundColor(U.getColor(R.color.black_trans_50))
                    .append(title).setForegroundColor(U.getColor(R.color.black_trans_80))
                    .create()
            mContentTv.visibility = View.VISIBLE
            mContentTv.text = stringBuilder
        }

        // 加载带时间戳的歌词
        watchModel.song?.let {
            feedWatchManyLyricView.setSongModel(it, -1)
            feedAutoScrollLyricView.setSongModel(it, -1)
        }
        // 加载歌词
        if (!TextUtils.isEmpty(model?.song?.songTpl?.lrcTs) && model?.song?.songType == 1) {
            feedAutoScrollLyricView.setVisibility(View.GONE)
            feedWatchManyLyricView.setVisibility(View.VISIBLE)
            feedWatchManyLyricView.loadLyric()
        } else {
            feedWatchManyLyricView.setVisibility(View.GONE)
            feedAutoScrollLyricView.setVisibility(View.VISIBLE)
            feedAutoScrollLyricView.loadLyric()
        }
        tryBindDebugView()
    }

    // 刷新喜欢图标和数字和喜欢的人
    fun refreshLike() {
        var drawble = U.getDrawable(R.drawable.feed_like_black_icon)
        if (model?.isLiked == true) {
            drawble = U.getDrawable(R.drawable.feed_like_selected_icon)
        }
        drawble.setBounds(0, 0, 20.dp(), 18.dp())
        mLikeNumTv.setCompoundDrawables(drawble, null, null, null)
        mLikeNumTv.text = StringFromatUtils.formatTenThousand(model?.starCnt ?: 0)

        if (model?.feedLikeUserList.isNullOrEmpty()) {
            mTvLikesDivider.visibility = View.GONE
        } else {
            mTvLikesDivider.visibility = View.VISIBLE
        }
        mTvLikes.setLikeUsers(model?.feedLikeUserList, model?.starCnt ?: 0)
    }

    // 刷新播放次数
    fun refreshPlayNum() {
        mPlayNumTv.text = "${StringFromatUtils.formatTenThousand(model?.exposure ?: 0)} 播放"
    }

    // 刷新收藏状态
    open fun refreshCollects() {
        var drawble = U.getDrawable(R.drawable.feed_not_collection)
        if (model?.isCollected == true) {
            drawble = U.getDrawable(R.drawable.feed_collect_selected_icon)
        }
        drawble.setBounds(0, 0, 20.dp(), 18.dp())
        mCollectIconTv.setCompoundDrawables(drawble, null, null, null)
        //todo 先去掉收藏
        mCollectIconTv.visibility = View.GONE
    }


    open fun startPlay() {
        mRecordView.play(SinglePlayer.isBufferingOk)
    }

    fun playLyric() {
        // 播放歌词 不一定是从头开始播
        // 有可能从头播 也有可能继续播
        if (!TextUtils.isEmpty(model?.song?.songTpl?.lrcTs) && model?.song?.songType == 1) {
            feedAutoScrollLyricView.setVisibility(View.GONE)
            feedWatchManyLyricView.seekTo(model?.song?.playCurPos ?: 0)
            feedWatchManyLyricView.resume()
        } else {
            feedAutoScrollLyricView.seekTo(model?.song?.playCurPos ?: 0)
            feedAutoScrollLyricView.resume()
            feedWatchManyLyricView.setVisibility(View.GONE)
        }
        tryBindDebugView()
    }

    private fun tryBindDebugView() {
        if (MyLog.isDebugLogOpen()) {
            mDebugTv.text = "uid:${model?.user?.userId}\n" +
                    "feedId:${model?.feedID}\n" +
                    "songId:${model?.song?.songID}\n" +
                    "songtype:${model?.song?.songType}\n" +
                    "playDurMs:${model?.song?.playDurMs} \n" +
                    "${model?.song?.playCurPos}/${model?.song?.playDurMsFromPlayerForDebug}\n" +
                    "${model?.song?.playURL}"
        }
    }

    fun pauseWhenBuffering() {
        if (!TextUtils.isEmpty(model?.song?.songTpl?.lrcTs) && model?.song?.songType == 1) {
            feedWatchManyLyricView.pause()
        } else {
            feedAutoScrollLyricView.pause()
        }
        mRecordView.buffering()
    }

    fun resumeWhenBufferingEnd() {
        if (!TextUtils.isEmpty(model?.song?.songTpl?.lrcTs) && model?.song?.songType == 1) {
            feedWatchManyLyricView.resume()
        } else {
            feedAutoScrollLyricView.resume()
        }
        mRecordView.bufferEnd()
    }

    fun stopPlay(useAnimation: Boolean) {
        if (useAnimation) {
            mRecordView.pause()
        } else {
            mRecordView.pauseWithNoAnimation()
        }
        feedAutoScrollLyricView.pause()
        feedWatchManyLyricView.pause()
    }
}