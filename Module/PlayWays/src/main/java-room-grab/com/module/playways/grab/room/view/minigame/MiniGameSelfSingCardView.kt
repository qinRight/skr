package com.module.playways.grab.room.view.minigame

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewStub

import com.common.log.MyLog
import com.module.playways.R
import com.module.playways.grab.room.GrabRoomData
import com.module.playways.grab.room.model.GrabRoundInfoModel
import com.module.playways.grab.room.view.SingCountDownView2
import com.module.playways.grab.room.view.control.SelfSingCardView

/**
 * 小游戏自己视角的卡片
 */
class MiniGameSelfSingCardView(viewStub: ViewStub, roomData: GrabRoomData?) : BaseMiniGameSelfSingCardView(viewStub, roomData) {

    internal var mSingCountDownView: SingCountDownView2? = null

    override fun init(parentView: View) {
        super.init(parentView)
        mSingCountDownView = mParentView!!.findViewById(R.id.sing_count_down_view)
        mSingCountDownView!!.setListener(mOverListener)
    }

    override fun layoutDesc(): Int {
        return R.layout.grab_mini_game_self_sing_card_stub_layout
    }


    override fun playLyric(): Boolean {
        if (super.playLyric()) {
            val infoModel = mGrabRoomData?.realRoundInfo
            val totalTs = infoModel!!.singTotalMs
            mSingCountDownView!!.startPlay(0, totalTs, true)
            return true
        } else {
            return false
        }
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        if (visibility == View.GONE) {
            if (mSingCountDownView != null) {
                mSingCountDownView!!.reset()
            }
        }
    }
}