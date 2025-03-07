package com.module.playways.mic.room.top

import android.os.Handler
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.common.core.account.UserAccountManager
import com.common.view.DebounceViewClickListener
import com.common.view.ex.ExImageView
import com.common.view.ex.ExTextView
import com.component.busilib.view.AvatarView
import com.component.busilib.view.VoiceChartView
import com.component.person.event.ShowPersonCardEvent
import com.module.playways.R
import com.module.playways.mic.room.MicRoomData
import com.module.playways.mic.room.model.MicPlayerInfoModel
import com.zq.live.proto.MicRoom.EMUserRole
import org.greenrobot.eventbus.EventBus

class MicTopContentAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var mDataList = ArrayList<MicPlayerInfoModel>()
    var mRoomData: MicRoomData? = null

    var maxUserCount = 0
        set(value) {
            if (value > 0) {
                field = value
            }
        }
    val handler = Handler()

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is MicAvatarTopViewHolder) {
            holder.bindData(position, if (position < mDataList.size) mDataList[position] else null)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.mic_top_avatar_item_layout, parent, false)
        return MicAvatarTopViewHolder(view)
    }

    override fun getItemCount(): Int {
        if (mDataList.size >= maxUserCount) {
            return mDataList.size
        } else {
            return maxUserCount
        }
    }

    inner class MicAvatarTopViewHolder(item: View) : RecyclerView.ViewHolder(item) {

        var mPostion = 0
        var mModel: MicPlayerInfoModel? = null

        var circleBgIv: ExImageView
        var avatarIv: AvatarView
        var waitingTv: ExTextView
        var voiceChartView: VoiceChartView
        var homeownerIv: ImageView
        var emptyIv: ImageView

        init {
            item.setOnClickListener(object : DebounceViewClickListener() {
                override fun clickValid(v: View?) {
                    mModel?.let {
                        if (it.userID != UserAccountManager.SYSTEM_ID && it.userID != UserAccountManager.SYSTEM_GRAB_ID && it.userID != UserAccountManager.SYSTEM_RANK_AI) {
                            EventBus.getDefault().post(ShowPersonCardEvent(it.userInfo.userId))
                        }
                    }
                }
            })

            circleBgIv = itemView.findViewById(R.id.circle_bg_iv)
            avatarIv = itemView.findViewById(R.id.avatar_iv)
            waitingTv = itemView.findViewById(R.id.waiting_tv)
            voiceChartView = itemView.findViewById(R.id.voice_chart_view)
            homeownerIv = itemView.findViewById(R.id.homeowner_iv)
            emptyIv = item.findViewById(R.id.empty_iv)
        }

        fun bindData(position: Int, model: MicPlayerInfoModel?) {
            this.mPostion = position
            this.mModel = model

            if (mModel == null) {
                circleBgIv.visibility = View.GONE
                avatarIv.visibility = View.GONE
                waitingTv.visibility = View.GONE
                voiceChartView.visibility = View.GONE
                homeownerIv.visibility = View.GONE
                emptyIv.visibility = View.VISIBLE
                return
            } else {
                avatarIv.visibility = View.VISIBLE
                emptyIv.visibility = View.GONE
            }

            if (model!!.isNextSing && !model!!.isCurSing) {
                waitingTv.visibility = View.VISIBLE
            } else {
                waitingTv.visibility = View.GONE
            }

            if (model!!.isCurSing) {
                circleBgIv.visibility = View.VISIBLE
                voiceChartView.visibility = View.VISIBLE
                waitingTv.visibility = View.GONE
                voiceChartView.start()
            } else {
                circleBgIv.visibility = View.GONE
                voiceChartView.visibility = View.GONE
                voiceChartView.stop()
            }

            if (model?.role == EMUserRole.MQUR_ROOM_OWNER.value || model?.userID == mRoomData?.ownerId) {
                if (mRoomData?.ownerId != model.userID) {
                    handler.post {
                        // 如果不post  这里会同步导致列表刷新，会有崩溃
                        mRoomData?.ownerId = model.userID
                    }
                }
                homeownerIv.visibility = View.VISIBLE
            } else {
                homeownerIv.visibility = View.GONE
            }

            mModel?.userInfo?.let {
                avatarIv.bindData(it)
            }
        }
    }

    fun destroy() {
        handler.removeCallbacksAndMessages(null)
    }
}