package com.module.playways.race.room.view

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import com.common.log.MyLog
import com.common.view.ex.ExConstraintLayout
import com.common.view.ex.ExImageView
import com.module.playways.R
import com.module.playways.race.room.RaceRoomData
import com.module.playways.race.room.model.RaceRoundInfoModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RaceSelectSongView : ExConstraintLayout {
    val mTag = "RaceSelectSongView"
    var bg: ImageView
    private val progressBg: ExImageView
    private val progressBar: ProgressBar
    private val forthSongItem: RaceSelectSongItemView
    private val firstSongItem: RaceSelectSongItemView
    private val secondSongItem: RaceSelectSongItemView
    private val thirdSongItem: RaceSelectSongItemView
    private val itemList: ArrayList<RaceSelectSongItemView> = ArrayList()
    var animator: ValueAnimator? = null
    var mRoomData: RaceRoomData? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        View.inflate(context, R.layout.race_select_song_layout, this)
        bg = findViewById(R.id.bg)
        progressBg = findViewById(R.id.progress_bg)
        progressBar = findViewById(R.id.progress_bar)
        firstSongItem = findViewById(R.id.first_song_item)
        secondSongItem = findViewById(R.id.second_song_item)
        thirdSongItem = findViewById(R.id.third_song_item)
        forthSongItem = findViewById(R.id.forth_song_item)
        itemList.add(firstSongItem)
        itemList.add(secondSongItem)
        itemList.add(thirdSongItem)
        itemList.add(forthSongItem)
        progressBar.max = 360
        progressBar.progress = 0

        launch {
            delay(1000)
            startCountDown()
            firstSongItem.startSelectedAnimation()

            delay(5000)
            firstSongItem.reset()
        }
    }

    fun setRoomData(roomData: RaceRoomData) {
        mRoomData = roomData
        firstSongItem.roomData = roomData
        secondSongItem.roomData = roomData
        thirdSongItem.roomData = roomData
        forthSongItem.roomData = roomData
    }

    fun setSongName() {
        mRoomData?.let {
            val info = it.realRoundInfo as RaceRoundInfoModel
            info?.let {
                for (i in 0 until it.games.size) {
                    if (i < 4) {
                        itemList[i].setSong(it.games[i])
                    }
                }
            }
        }
    }

    fun updateSelectState() {
        mRoomData?.let {
            val info = it.realRoundInfo as RaceRoundInfoModel
            info?.let {
                for (i in 0 until it.gamesChoiceMap.size) {
                    if (i < 4) {
                        itemList[i].bindData(it.gamesChoiceMap[i])
                    }
                }
            }
        }
    }

    fun startCountDown() {
        MyLog.d(mTag, "startCountDown")
        animator = ValueAnimator.ofInt(0, 360)
        animator?.duration = 6000
        animator?.addUpdateListener {
            progressBar.progress = it.animatedValue as Int
        }
        animator?.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}